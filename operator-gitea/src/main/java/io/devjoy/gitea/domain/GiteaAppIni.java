package io.devjoy.gitea.domain;

import java.util.HashMap;
import java.util.Map;

public class GiteaAppIni { 

    public static class GiteaIniSection{
        private final Map<String, Object> properties = new HashMap<>();
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
            builder.append("[");
            builder.append(name);
            builder.append("]\n");
            properties.entrySet().stream().forEach(e -> 
            {
                builder.append(e.getKey());
                builder.append(" = ");
                builder.append(e.getValue());
                builder.append("\n");
            });
        }
    }

    private final Map<String, GiteaIniSection> sections = new HashMap<>();

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
        return builder.toString();
    }
    
}
