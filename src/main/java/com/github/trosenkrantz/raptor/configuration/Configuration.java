package com.github.trosenkrantz.raptor.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
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
        ObjectMapper mapper = new ObjectMapper();
        return new Configuration(mapper, mapper.createObjectNode(), List.of());
    }

    public static Optional<Configuration> fromSavedFile() throws IOException {
        Path path = Path.of("config.json");
        if (!Files.exists(path)) return Optional.empty();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(path.toFile());
        if (!node.isObject()) {
            throw new IllegalArgumentException("Configuration file " + path.toAbsolutePath() + " is not a JSON object.");
        }

        return Optional.of(new Configuration(mapper, (ObjectNode) node, List.of()));
    }

//    public static Configuration fromPath(Path path) throws IOException {
//        File configFile = path.toFile();
//        if (!configFile.exists()) {
//            throw new IllegalArgumentException("Configuration file " + path.toAbsolutePath() + " does not exist.");
//        }
//
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode node = mapper.readTree(configFile);
//        if (!node.isObject()) {
//            throw new IllegalArgumentException("Configuration file " + path.toAbsolutePath() + " is not a JSON object.");
//        }
//
//        return new Configuration(mapper, (ObjectNode) node, List.of());
//    }

    private Configuration(ObjectMapper mapper, ObjectNode root, List<String> path) {
        this.mapper = mapper;
        this.root = root;
        this.path = path;
    }

    /**
     * Add all parameters from a configuration to this configuration prefixing the keys to add.
     * The keys of the added parameters will be {@code <prefix>-<key of original parameter}.
     *
     * @param prefix        prefix to add
     * @param configuration configuration to copy from
     */
    public void addWithPrefix(String prefix, Configuration configuration) { // TODO Rename to setSubConfiguration?
        if (root.get(prefix) != null) {
            LOGGER.warning("Overwriting sub-configuration with path " + pathToString(prefix) + ".");
        }

        root.putObject(prefix).setAll(configuration.root);
    }

    private String pathToString(String key) {
        ArrayList<String> pathToLog = new ArrayList<>(path);
        pathToLog.add(key);
        return String.join("/", pathToLog);
    }

    /**
     * Extract all parameters from this configuration that have a key starting with the given prefix.
     * The returned configuration will have the prefix and '-' removed from the keys.
     *
     * @param prefix prefix to filter with
     * @return a new configuration containing only the matching parameters
     */
    public Configuration extractWithPrefix(String prefix) { // TODO Rename to getSubConfiguration?
        List<String> nextPath = new ArrayList<>(path);
        nextPath.add(prefix);

        JsonNode nextNode = root.get(prefix);
        if (nextNode == null) {
            LOGGER.warning("Extracting empty sub-configuration with path " + pathToString(prefix) + ".");
            return new Configuration(mapper, mapper.createObjectNode(), nextPath);
        } else if (!nextNode.isObject()) {
            LOGGER.warning("Extracting sub-configuration with path " + pathToString(prefix) + " that is not an object.");
            return new Configuration(mapper, mapper.createObjectNode(), nextPath);
        }

        return new Configuration(mapper, nextNode.deepCopy(), nextPath);
    }

    public Configuration copy() {
        return new Configuration(mapper, root.deepCopy(), path);
    }

    public boolean hasParameter(String key) {
        return root.has(key);
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
        return getString(key).orElseThrow(() -> new IllegalArgumentException("Parameter " + pathToString(key) + " not set"));
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
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert configuration to JSON.", e);
        }
    }
}
