package com.cpt22.HTTPServer;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Configuration class based on the SnakeYAML library
 */
public class Configuration {
    private Yaml confFile = new Yaml();
    private Map<String, Object> config = null;

    public Configuration() {
        load("./configuration/config.yaml");
    }

    private Configuration(Map<String, Object> section) {
        config = section;
    }

    public Configuration(String filename) {
        load(filename);
    }

    /**
     * Loads the YAML configuration file from the specified path.
     * @param path - path to configuration file
     */
    private void load(String path) {
        try {
            Path target = Paths.get(path);
            if (!Files.exists(target)) {
                Files.createDirectories(target.getParent());
                InputStream in = getClass().getResourceAsStream("/default_config.yaml");
                Files.copy(in, target);
            }
            config = confFile.load(new FileInputStream(new File(path)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetches a configuration section in the form of another configuration object. A section is a nested multi value YAML element
     * @param key - the key of the multi variable attribute in the YAML file
     * @return - A new configuration object containing a map of all of the elements in the configuration object.
     * @throws YAMLConfigurationException
     */
    public Configuration getSection(String key) throws YAMLConfigurationException {
        Object obj = config.get(key);
        if (obj instanceof Map) {
            return new Configuration((Map<String, Object>) obj);
        } else {
            throw new YAMLConfigurationException("Expected type Config Section for key '" + key);
        }
    }

    /**
     * Fetches a boolean at the specified key
     * @param key - the key of the boolean value to be fetched
     * @return - the boolean value of the YAML attribute
     * @throws YAMLConfigurationException - Thrown if the attribute is not found(null) or the value is not of the expected type.
     */
    public Boolean getBoolean(String key) throws YAMLConfigurationException {
        Object obj = config.get(key);
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else {
            throw new YAMLConfigurationException("Expected type Boolean for key '" + key + "' but found " + (obj != null ? obj.getClass().getName() : "null"));
        }
    }

    /**
     * Fetches a boolean at the specified key or returns the default value if the key is not found
     * @param key - the key of the boolean value to be fetched
     * @param def - the default value to be returned if the value is not found
     * @return - the boolean value of the YAML attribute or the default value
     */
    public Boolean getBoolean(String key, boolean def) {
        Object obj = config.get(key);
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else {
            System.err.println("Expected type Boolean for key '" + key + "' but found " + (obj != null ? obj.getClass().getName() : "null"));
            return def;
        }
    }

    /**
     * Fetches an integer at the specified key
     * @param key - the key of the integer value to be fetched
     * @return - the integer value of the YAML attribute
     * @throws YAMLConfigurationException - Thrown if the attribute is not found(null) or the value is not of the expected type.
     */
    public Integer getInt(String key) throws YAMLConfigurationException {
        Object obj = config.get(key);
        if (obj instanceof Integer) {
            return (Integer) config.get(key);
        } else {
            throw new YAMLConfigurationException("Expected type Integer for key '" + key + "' but found " + (obj != null ? obj.getClass().getName() : "null"));
        }
    }

    /**
     * Fetches an integer at the specified key or returns the default value if the key is not found
     * @param key - the key of the integer value to be fetched
     * @param def - the default value to be returned if the value is not found
     * @return - the integer value of the YAML attribute or the default value
     */
    public Integer getInt(String key, int def) {
        Object obj = config.get(key);
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else {
            System.err.println("Expected type Boolean for key '" + key + "' but found " + (obj != null ? obj.getClass().getName() : "null"));
            return def;
        }
    }

    /**
     * Fetches a String at the specified key
     * @param key - the key of the String value to be fetched
     * @return - the String value of the YAML attribute
     * @throws YAMLConfigurationException - Thrown if the attribute is not found(null) or the value is not of the expected type.
     */
    public String getString(String key) throws YAMLConfigurationException {
        Object obj = config.get(key);
        if (obj instanceof String) {
            return (String) obj;
        } else {
            throw new YAMLConfigurationException("Expected type String for key '" + key + "' but found " + (obj != null ? obj.getClass().getName() : "null"));
        }
    }

    /**
     * Fetches a String at the specified key or returns the default value if the key is not found
     * @param key - the key of the String value to be fetched
     * @param def - the default value to be returned if the value is not found
     * @return - the String value of the YAML attribute or the default value
     */
    public String getString(String key, String def) {
        Object obj = config.get(key);
        if (obj instanceof String) {
            return (String) obj;
        } else {
            System.err.println("Expected type Boolean for key '" + key + "' but found " + (obj != null ? obj.getClass().getName() : "null"));
            return def;
        }
    }
}
