package io.devjoy.gitea.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.util.StringUtil;

public class GiteaAppIni { 

    public static class GiteaIniSection{
        private final Map<String, Object> properties = new LinkedHashMap<>();
        private final String name;

        public GiteaIniSection(String name) {
            this.name = name;
        }

        public Object setProperty(String key, Object value) {
            return properties.put(key, value);
        } 
        
        public Object getProperty(String key) {
            return properties.get(key);
        } 

        private void appendAsString(StringBuilder builder) {
            if (notDefaultSection()) {
                builder.append("[");
                builder.append(name);
                builder.append("]\n");
            }
            properties.entrySet().stream().forEach(e -> 
            {
                builder.append(e.getKey());
                builder.append(" = ");
                builder.append(e.getValue());
                builder.append("\n");
            });
            builder.append("\n");
        }

        private boolean notDefaultSection() {
            return !StringUtil.isNullOrEmpty(name);
        }
    }
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationService.class);
    private static final Pattern PATTERN_SECTION = Pattern.compile("\\[.*\\]\n[^\\[]*");
    private static final Pattern PATTERN_DEFAULT = Pattern.compile("\\[.*\\]");
    private final Map<String, GiteaIniSection> sections = new LinkedHashMap<>();
   
    public GiteaAppIni() {

    }

    public GiteaAppIni(String appIniData) {   
        parseDefaultProperties(appIniData);
        parseSections(appIniData);
    }

    private void parseSections(String appIniData) {
        LOG.debug("Parsing sections");
        try(Scanner scanner = new Scanner(appIniData)) {
            scanner.findAll(PATTERN_SECTION).map(MatchResult::group).forEach(secData -> {
                Optional<GiteaIniSection> section = secData.lines().findFirst()
                    .map(s -> s.trim().substring(1, s.length() - 1))
                    .map(this::getSection);
                section.ifPresent(s -> {
                    secData.lines()
                        .skip(1)
                        .filter(l -> !l.trim().isEmpty())
                        .map(l -> {
                            int index = l.indexOf("=");
                            LOG.debug("Adding {} to section {}", l, s.name);
                            return new String[]{l.substring(0, index).trim(), l.substring(index + 1, l.length()).trim()};
                        })
                        .forEach(l -> s.setProperty(l[0], l[1]));
                });
                
            });
        }
    }

    private void parseDefaultProperties(String appIniData) {
        LOG.debug("Parsing default sections");
        try(Scanner scanner = new Scanner(appIniData)) {
            int sectionIndex = scanner.findAll(PATTERN_DEFAULT).map(MatchResult::start).findFirst().orElse(appIniData.length());
            appIniData.substring(0, sectionIndex).lines()
                .filter(l -> !l.trim().isEmpty())
                .map(l -> {
                            int index = l.indexOf("=");
                            LOG.debug("Adding {} to default section", l);
                            return new String[]{l.substring(0, index).trim(), l.substring(index + 1, l.length()).trim()};
                        })
                .forEach(l -> setProperty(l[0], l[1]));
        }
    }

    public GiteaIniSection getSection(String name) {
        GiteaIniSection section = sections.get(name);
        if (section == null) {
            section = new GiteaIniSection(name);
            sections.put(name, section);
        }
        return section;
    }

    public Object setProperty(String key, Object value) {
        return getSection("").setProperty(key, value);
    } 
    
    public Object getProperty(String key) {
        return getSection("").getProperty(key);
    } 

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        sections.values().stream().forEach(s -> s.appendAsString(builder));
        return builder.toString().trim();
    }
    
}
