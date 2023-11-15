package io.devjoy.gitea.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class GiteaAppIniTest {
    static OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
    static final String KEY_APP_INI = "app.ini";
    GiteaAppIni ini;
    

    @Test
    public void readSimpleFile() throws IOException {
        Secret secret = client
          .secrets()
          .load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/config-secret.yaml"))
          .item();
        String iniData = secret.getStringData().get(KEY_APP_INI);
        ini = new GiteaAppIni(iniData);
        assertEquals("${GITEA_INTERNAL_TOKEN}", ini.getSection("security").getProperty("INTERNAL_TOKEN"));
        assertEquals("\"asciidoc --out-file=- -\"", ini.getSection("markup.asciidoc").getProperty("RENDER_COMMAND"));
        assertEquals("postgres", ini.getSection("database").getProperty("DB_TYPE"));
        assertEquals("disable", ini.getSection("database").getProperty("SSL_MODE"));
        assertEquals("gitea",ini.getProperty("APP_NAME"));
        assertEquals("prod", ini.getProperty("RUN_MODE"));
        assertEquals("gitea", ini.getProperty("RUN_USER"));
        Path expectedIniPath = Paths.get("src/test/resources/ini/app.ini");
        String expectedIniData = Files.readString(expectedIniPath);
        assertEquals(expectedIniData, ini.toString());
    }

     @Test
    public void readAllSectionsFile() throws IOException {
        Path expectedIniPath = Paths.get("src/test/resources/ini/app3.ini");
        String expectedIniData = Files.readString(expectedIniPath);
        ini = new GiteaAppIni(expectedIniData);
        assertEquals("${GITEA_INTERNAL_TOKEN}", ini.getSection("security").getProperty("INTERNAL_TOKEN"));
        assertEquals("\"asciidoc --out-file=- -\"", ini.getSection("markup.asciidoc").getProperty("RENDER_COMMAND"));
        assertEquals("postgres", ini.getSection("database").getProperty("DB_TYPE"));
        assertEquals("disable", ini.getSection("database").getProperty("SSL_MODE"));
        assertEquals("Gitea: Git with a cup of tea under test",ini.getProperty("APP_NAME"));
        assertEquals("prod", ini.getProperty("RUN_MODE"));
        assertEquals("gitea", ini.getProperty("RUN_USER"));
        assertEquals("WIP:,[WIP]:,WIPT:", ini.getSection("repository.pull-request").getProperty("WORK_IN_PROGRESS_PREFIXES"));
        assertEquals(expectedIniData, ini.toString());
    }

    @Test
    public void changeFile() throws IOException {
        Secret secret = client
          .secrets()
          .load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/config-secret.yaml"))
          .item();
        String iniData = secret.getStringData().get(KEY_APP_INI);
        ini = new GiteaAppIni(iniData);
        ini.setProperty("RUN_MODE", "dev");
        assertEquals("dev", ini.getProperty("RUN_MODE"));
        ini.setProperty("WORK_PATH", "the-work-path");
        assertEquals("the-work-path", ini.getProperty("WORK_PATH"));
        ini.getSection("service").setProperty("NO_REPLY_ADDRESS","devjoy.example.org");
        assertEquals("devjoy.example.org", ini.getSection("service").getProperty("NO_REPLY_ADDRESS"));
        ini.getSection("service").setProperty("ENABLE_BASIC_AUTHENTICATION","true");
        assertEquals("true", ini.getSection("service").getProperty("ENABLE_BASIC_AUTHENTICATION"));
        ini.getSection("repository.pull-request").setProperty("WORK_IN_PROGRESS_PREFIXES", "WIP:,[WIP]:,WIPT:");
        assertEquals("WIP:,[WIP]:,WIPT:", ini.getSection("repository.pull-request").getProperty("WORK_IN_PROGRESS_PREFIXES"));
        Path expectedIniPath = Paths.get("src/test/resources/ini/app2.ini");
        String expectedIniData = Files.readString(expectedIniPath);
        assertEquals(expectedIniData, ini.toString());
    }
}
