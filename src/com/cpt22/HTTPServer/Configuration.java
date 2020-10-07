package com.cpt22.HTTPServer;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

public class Configuration {
    Yaml confFile = new Yaml();
    Map<String, Object> config = null;

    public Configuration() {
        load("./config.yaml");
    }

    private Configuration(Map<String, Object> section) {
        config = section;
    }

    public Configuration(String filename) {
        load(filename);
    }

    private void load(String path) {
        try {
            config = confFile.load(new FileInputStream(new File(path)));
        } catch (
                FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Configuration getSection(String key) throws YAMLConfigurationException {
        Object obj = config.get(key);
        if (obj != null && obj instanceof Map) {
            return new Configuration((Map<String, Object>) obj);
        } else {
            throw new YAMLConfigurationException("Expected type Config Section for key '" + key);
        }
    }

    public Boolean getBoolean(String key) throws YAMLConfigurationException {
        Object obj = config.get(key);
        if (obj != null && obj instanceof Boolean) {
            return (Boolean) obj;
        } else {
            throw new YAMLConfigurationException("Expected type Boolean for key '" + key + "' but found " + (obj != null ? obj.getClass().getName() : "null"));
        }
    }

    public Boolean getBoolean(String key, boolean def) throws YAMLConfigurationException {
        Object obj = config.get(key);
        if (obj != null && obj instanceof Boolean) {
            return (Boolean) obj;
        } else {
            System.err.println("Expected type Boolean for key '" + key + "' but found " + (obj != null ? obj.getClass().getName() : "null"));
            return def;
        }
    }

    public Integer getInt(String key) throws YAMLConfigurationException {
        Object obj = config.get(key);
        if (obj != null && obj instanceof Integer) {
            return (Integer) config.get(key);
        } else {
            throw new YAMLConfigurationException("Expected type Integer for key '" + key + "' but found " + (obj != null ? obj.getClass().getName() : "null"));
        }
    }

    public Integer getInt(String key, int def) throws YAMLConfigurationException {
        Object obj = config.get(key);
        if (obj != null && obj instanceof Integer) {
            return (Integer) obj;
        } else {
            System.err.println("Expected type Boolean for key '" + key + "' but found " + (obj != null ? obj.getClass().getName() : "null"));
            return def;
        }
    }

    public String getString(String key) throws YAMLConfigurationException {
        Object obj = config.get(key);
        if (obj != null && obj instanceof String) {
            return (String) obj;
        } else {
            throw new YAMLConfigurationException("Expected type String for key '" + key + "' but found " + (obj != null ? obj.getClass().getName() : "null"));
        }
    }
}
