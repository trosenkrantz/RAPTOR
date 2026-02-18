package com.github.trosenkrantz.raptor.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.FileWatcher;
import com.github.trosenkrantz.raptor.io.JsonUtility;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Configuration {
    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    private final ObjectMapper mapper;
    private final List<String> jsonPath; // logical prefix path from origin configuration
    private final Path filePath;
    private final FileWatcher fileWatcher;

    private ObjectNode root;

    public static Configuration empty() {
        ObjectMapper mapper = JsonUtility.buildMapper();
        return new Configuration(mapper, mapper.createObjectNode(), List.of());
    }

    public static Configuration fromSavedFile(Path path, FileWatcher fileWatcher) throws IOException {
        ObjectMapper mapper = JsonUtility.buildMapper();
        JsonNode node = mapper.readTree(path.toFile());
        if (!node.isObject()) {
            throw new IllegalArgumentException("Configuration file " + path.toAbsolutePath() + " is not a JSON object.");
        }

        LOGGER.info("Loaded configuration at " + path.toAbsolutePath() + ".");

        return new Configuration(mapper, (ObjectNode) node, List.of(), path, fileWatcher);
    }

    public static Configuration fromStream(final InputStream inputStream) throws IOException {
        ObjectMapper mapper = JsonUtility.buildMapper();

        JsonNode node = mapper.readTree(inputStream);
        if (!node.isObject()) {
            throw new IllegalArgumentException("Configuration file loaded from stream is not a JSON object.");
        }

        return new Configuration(mapper, (ObjectNode) node, List.of());
    }

    private Configuration(ObjectMapper mapper, ObjectNode root, List<String> jsonPath) {
        this(mapper, root, jsonPath, null, null);
    }

    public Configuration(ObjectMapper mapper, ObjectNode root, List<String> jsonPath, Path filePath, FileWatcher fileWatcher) {
        this.mapper = mapper;
        this.root = root;
        this.jsonPath = jsonPath;
        this.filePath = filePath;
        this.fileWatcher = fileWatcher;
    }

    private String pathToString(String key) {
        ArrayList<String> pathToLog = new ArrayList<>(jsonPath);
        pathToLog.add(key);
        return String.join("/", pathToLog);
    }

    public Configuration copy() {
        return new Configuration(mapper, root.deepCopy(), jsonPath);
    }

    public boolean hasParameter(String key) {
        return root.has(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key));
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
        if (root.get(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key)) != null) {
            LOGGER.warning("Overwriting sub-configuration with path " + pathToString(key) + ".");
        }

        root.putObject(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key)).setAll(configuration.root);
    }

    /**
     * Extract the part of this configuration that have a given key.
     *
     * @param key key to filter with
     * @return a new configuration containing only the matching parameters
     */
    public Optional<Configuration> getSubConfiguration(String key) {
        List<String> nextPath = new ArrayList<>(jsonPath);
        nextPath.add(key);

        JsonNode nextNode = root.get(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key));
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
        JsonNode node = root.get(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key));
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
            result.add(new Configuration(mapper, element.deepCopy(), jsonPath));
        }

        return result;
    }

    public void setSubConfigurationArray(String key, List<Configuration> values) {
        ArrayNode array = mapper.createArrayNode();

        for (Configuration cfg : values) {
            array.add(cfg.root);
        }

        root.set(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key), array);
    }


    /* String */

    /**
     * Gets a string.
     *
     * @param key key, must be a RAPTOR encoded string, see {@link BytesFormatter}
     * @return the value as a RAPTOR encoded string if present, empty Optional otherwise
     */
    public Optional<String> getRaptorEncodedString(String key) {
        JsonNode node = root.get(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key));
        if (node == null || !node.isTextual()) {
            return Optional.empty();
        }
        return Optional.of(BytesFormatter.intermediateEncodingToRaptorEncoded(node.asText()));
    }

    /**
     * Gets a string, throwing an exception if absent.
     *
     * @param key key, must be a RAPTOR encoded escaped string, see {@link BytesFormatter}
     * @return the value as a RAPTOR encoded string
     */
    public String requireRaptorEncodedString(String key) {
        return getRaptorEncodedString(key).orElseThrow(() -> new IllegalArgumentException("Parameter " + pathToString(key) + " not set."));
    }

    /**
     * Sets a string.
     *
     * @param key   key, must be a RAPTOR encoded strings, see {@link BytesFormatter}
     * @param value value, must be a RAPTOR encoded strings, see {@link BytesFormatter}
     */
    public void setRaptorEncodedString(String key, String value) {
        // Jackson escapes strings, so we convert to hex escaped strings, matching unescaped JSON, to avoid double escaping
        root.put(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key), BytesFormatter.raptorEncodingToIntermediateEncodedBytes(value));
    }

    /* Enum and Configurable */

    public <E extends Enum<E> & ConfigurableEnum> Optional<E> getEnum(String key, Class<E> enumClass) {
        Optional<String> idInConfiguration = getRaptorEncodedString(key);
        if (idInConfiguration.isEmpty()) return Optional.empty();

        E[] enumConstants = enumClass.getEnumConstants();
        List<E> matchingEnumConstants = Arrays.stream(enumConstants).filter(id -> id.getConfigurationId().equals(idInConfiguration.get())).toList();

        if (matchingEnumConstants.isEmpty()) {
            List<String> idsInEnumConstants = Arrays.stream(enumConstants).map(ConfigurableEnum::getConfigurationId).toList();
            LOGGER.warning(idInConfiguration + " not found among [" + String.join(", ", idsInEnumConstants) + "]. Assuming unset.");
            return Optional.empty();
        }
        if (matchingEnumConstants.size() > 1) {
            LOGGER.warning("Multiple enum constants found for " + idInConfiguration + ". Picking the first.");
        }

        return Optional.of(matchingEnumConstants.getFirst());
    }

    public <E extends Enum<E> & ConfigurableEnum> Optional<E> getEnum(Class<E> enumClass) {
        return getEnum(PascalCaseToCamelCase(enumClass.getSimpleName()), enumClass);
    }

    public <E extends Enum<E> & ConfigurableEnum> E requireEnum(Class<E> enumClass) {
        return requireEnum(PascalCaseToCamelCase(enumClass.getSimpleName()), enumClass);
    }

    /**
     * Gets an enum constant, throwing an exception if absent.
     *
     * @param key       key, must be a RAPTOR encoded string, see {@link BytesFormatter}
     * @param enumClass enum class
     * @param <E>       enum type
     * @return enum constant
     */
    public <E extends Enum<E> & ConfigurableEnum> E requireEnum(String key, Class<E> enumClass) {
        String idInConfiguration = requireRaptorEncodedString(key);
        E[] enumConstants = enumClass.getEnumConstants();
        List<E> matchingEnumConstants = Arrays.stream(enumConstants).filter(id -> id.getConfigurationId().equals(idInConfiguration)).toList();

        if (matchingEnumConstants.isEmpty()) {
            List<String> idsInEnumConstants = Arrays.stream(enumConstants).map(ConfigurableEnum::getConfigurationId).toList();
            throw new IllegalArgumentException(idInConfiguration + " not found among [" + String.join(", ", idsInEnumConstants) + "].");
        }
        if (matchingEnumConstants.size() > 1) {
            LOGGER.warning("Multiple enum constants found for " + idInConfiguration + ". Picking the first.");
        }

        return matchingEnumConstants.getFirst();
    }

    public <E extends Enum<E> & ConfigurableEnum> void setEnum(E value) {
        setConfigurable(PascalCaseToCamelCase(value.getDeclaringClass().getSimpleName()), value);
    }

    public void setConfigurable(String key, ConfigurableEnum value) {
        setRaptorEncodedString(key, value.getConfigurationId());
    }


    /* Int */

    public Optional<Integer> getInt(String key) {
        JsonNode node = root.get(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key));
        if (node == null || !node.isInt()) {
            return Optional.empty();
        }
        return Optional.of(node.intValue());
    }

    public int requireInt(String key) {
        return getInt(key).orElseThrow(() -> new IllegalArgumentException("Parameter " + pathToString(key) + " not set"));
    }

    public void setInt(String key, Integer value) {
        root.put(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key), value);
    }

    /* Double */

    public Optional<Double> getDouble(String key) {
        JsonNode node = root.get(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key));
        if (node == null || !node.isNumber()) {
            return Optional.empty();
        }
        return Optional.of(node.doubleValue());
    }

    public void setDouble(String key, Double value) {
        root.put(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key), value);
    }


    /* Complex objects */

    public <T> T requireObject(String key, Class<T> clazz) {
        JsonNode node = root.get(BytesFormatter.raptorEncodingToIntermediateEncodedBytes(key));
        if (node == null) {
            throw new IllegalArgumentException("Parameter " + pathToString(key) + " not set.");
        }

        try {
            return mapper.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize parameter " + pathToString(key) + " into " + clazz.getSimpleName() + ".", e);
        }
    }

    public <T> boolean subscribeToObjectChangesIfSupported(String key, Class<T> clazz, Consumer<T> consumer) {
        if (fileWatcher == null || filePath == null) return false;

        try {
            fileWatcher.subscribe(() -> {
                JsonNode node;
                try {
                    node = mapper.readTree(filePath.toFile());
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to read configuration at " + filePath.toAbsolutePath() + ".", e);
                    return false;
                }
                if (!node.isObject()) {
                    LOGGER.log(Level.WARNING, "Failed processing configuration file " + filePath.toAbsolutePath() + ", it is not a JSON object.");
                    return false;
                }

                root = (ObjectNode) node;

                consumer.accept(requireObject(key, clazz));

                return true;
            });

            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed watching for configuration file changes at " + filePath.toAbsolutePath() + ".", e);
            return false;
        }
    }


    /* Utility */

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
