package com.github.trosenkrantz.raptor.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.trosenkrantz.raptor.io.JsonUtility;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

public class Configuration {
    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    private final ObjectMapper mapper;
    private final ObjectNode root;
    private final List<String> path; // logical prefix path

    public static Configuration empty() {
        ObjectMapper mapper = JsonUtility.buildMapper();
        return new Configuration(mapper, mapper.createObjectNode(), List.of());
    }

    public static Optional<Configuration> fromSavedFile() throws IOException {
        Path path = Path.of(ConfigurationStorage.CONFIGURATION_FILE_NAME);
        if (!Files.exists(path)) return Optional.empty();

        ObjectMapper mapper = JsonUtility.buildMapper();
        JsonNode node = mapper.readTree(path.toFile());
        if (!node.isObject()) {
            throw new IllegalArgumentException("Configuration file " + path.toAbsolutePath() + " is not a JSON object.");
        }

        LOGGER.info("Loaded configuration at: " + path.toAbsolutePath());

        return Optional.of(new Configuration(mapper, (ObjectNode) node, List.of()));
    }

    public static Configuration fromStream(final InputStream inputStream) throws IOException {
        ObjectMapper mapper = JsonUtility.buildMapper();

        JsonNode node = mapper.readTree(inputStream);
        if (!node.isObject()) {
            throw new IllegalArgumentException("Configuration file loaded from stream is not a JSON object.");
        }

        return new Configuration(mapper, (ObjectNode) node, List.of());
    }

    private Configuration(ObjectMapper mapper, ObjectNode root, List<String> path) {
        this.mapper = mapper;
        this.root = root;
        this.path = path;
    }

    private String pathToString(String key) {
        ArrayList<String> pathToLog = new ArrayList<>(path);
        pathToLog.add(key);
        return String.join("/", pathToLog);
    }

    public Configuration copy() {
        return new Configuration(mapper, root.deepCopy(), path);
    }

    public boolean hasParameter(String key) {
        return root.has(key);
    }

    /**
     * Get the top level keys in this configuration.
     *
     * @return keys in this configuration scope
     */
    public List<String> keys() {
        List<String> keys = new ArrayList<>();
        root.fieldNames().forEachRemaining(keys::add);
        return keys;
    }


    /* Sub-Configurations */

    /**
     * Add all parameters from a configuration to this configuration under a specified key.
     *
     * @param key           key to add
     * @param configuration configuration to copy from
     */
    public void setSubConfiguration(String key, Configuration configuration) {
        if (root.get(key) != null) {
            LOGGER.warning("Overwriting sub-configuration with path " + pathToString(key) + ".");
        }

        root.putObject(key).setAll(configuration.root);
    }

    /**
     * Extract the part of this configuration that have a given key.
     *
     * @param key key to filter with
     * @return a new configuration containing only the matching parameters
     */
    public Optional<Configuration> getSubConfiguration(String key) {
        List<String> nextPath = new ArrayList<>(path);
        nextPath.add(key);

        JsonNode nextNode = root.get(key);
        if (nextNode == null) {
            return Optional.empty();
        } else if (!nextNode.isObject()) {
            throw new IllegalArgumentException("Sub-configuration with path " + pathToString(key) + " is not a JSON object.");
        }

        return Optional.of(new Configuration(mapper, nextNode.deepCopy(), nextPath));
    }

    public Configuration requireSubConfiguration(String key) {
        return getSubConfiguration(key).orElseThrow(() -> new IllegalArgumentException("Sub-configuration with path " + pathToString(key) + " does not exist."));
    }

    public List<Configuration> getSubConfigurationArray(String key) {
        JsonNode node = root.get(key);
        if (node == null) {
            return List.of();
        }
        if (!node.isArray()) {
            throw new IllegalArgumentException("Parameter " + pathToString(key) + " is not a JSON array.");
        }

        List<Configuration> result = new ArrayList<>();

        for (JsonNode element : node) {
            if (!element.isObject()) {
                throw new IllegalArgumentException("Array element in " + pathToString(key) + " is not a JSON object.");
            }
            result.add(new Configuration(mapper, element.deepCopy(), path));
        }

        return result;
    }

    public void setSubConfigurationArray(String key, List<Configuration> values) {
        ArrayNode array = mapper.createArrayNode();

        for (Configuration cfg : values) {
            array.add(cfg.root);
        }

        root.set(key, array);
    }


    /* String */

    public Optional<String> getString(String key) {
        JsonNode node = root.get(key);
        if (node == null || !node.isTextual()) {
            return Optional.empty();
        }
        return Optional.of(node.asText());
    }

    public String requireString(String key) {
        return getString(key).orElseThrow(() -> new IllegalArgumentException("Parameter " + pathToString(key) + " not set."));
    }

    public void setString(String key, String value) {
        root.put(key, value);
    }

    /* Enum */

    public <E extends Enum<E>> E requireEnum(Class<E> enumClass) {
        return requireEnum(PascalCaseToCamelCase(enumClass.getSimpleName()), enumClass);
    }

    public <E extends Enum<E>> E requireEnum(String key, Class<E> enumClass) {
        return camelCaseToEnum(requireString(key), enumClass);
    }

    public <E extends Enum<E>> void setEnum(String key, E value) {
        setString(key, enumToCamelCase(value));
    }

    public <E extends Enum<E>> void setEnum(E value) {
        setEnum(PascalCaseToCamelCase(value.getDeclaringClass().getSimpleName()), value);
    }

    /* Int */

    public Optional<Integer> getInt(String key) {
        JsonNode node = root.get(key);
        if (node == null || !node.isInt()) {
            return Optional.empty();
        }
        return Optional.of(node.intValue());
    }

    public int requireInt(String key) {
        return getInt(key).orElseThrow(() -> new IllegalArgumentException("Parameter " + pathToString(key) + " not set"));
    }

    public void setInt(String key, Integer value) {
        root.put(key, value);
    }

    /* Double */

    public Optional<Double> getDouble(String key) {
        JsonNode node = root.get(key);
        if (node == null || !node.isNumber()) {
            return Optional.empty();
        }
        return Optional.of(node.doubleValue());
    }

    public void setDouble(String key, Double value) {
        root.put(key, value);
    }


    /* Complex objects */

    public <T> T getObject(String key, Class<T> clazz) {
        JsonNode node = root.get(key);
        if (node == null) {
            throw new IllegalArgumentException("Parameter " + pathToString(key) + " not set.");
        }

        try {
            return mapper.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize parameter " + pathToString(key) + " into " + clazz.getSimpleName() + ".", e);
        }
    }


    /* Utility */

    private static String enumToCamelCase(Enum<?> value) {
        String[] parts = value.name().toLowerCase(Locale.ROOT).split("_", -1);
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(parts[i].substring(0, 1).toUpperCase(Locale.ROOT));
            sb.append(parts[i].substring(1));
        }
        return sb.toString();
    }

    private static <E extends Enum<E>> E camelCaseToEnum(String value, Class<E> enumClass) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toUpperCase(c));
            } else {
                sb.append(Character.toUpperCase(c));
            }
        }
        return Enum.valueOf(enumClass, sb.toString());
    }

    private static String PascalCaseToCamelCase(String value) {
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }


    /* Other */

    public String toJson() {
        try {
            return mapper
                    .writer()
                    .with(new RaptorJsonPrinter())
                    .writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert configuration to JSON.", e);
        }
    }
}
