package com.github.trosenkrantz.raptor;

import com.github.trosenkrantz.raptor.io.BytesFormatter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A configuration of key-value parameters.
 * A parameter can represent either a flag (no value) or a primitive value.
 * When we need a complex value, we JSON to encode as a string.
 */
public class Configuration {
    public static final String DEFAULT_PREFIX = "--";
    public static final String KEY_SEPARATOR = "-";

    /**
     * Storage of parameters.
     * We store keys without the {@link #DEFAULT_PREFIX}.
     * We represent flags by a null value
     */
    private final Map<String, String> parameters;

    /**
     * Used as output to the user so they can tell the full path.
     * This includes the ´-´s.
     */
    private final String prefix;

    public Configuration() {
        this(new LinkedHashMap<>());
    }

    /**
     * Creates a new configuration from CLI arguments.
     * Each argument must be in the form --{@code key}={@code value}.
     *
     * @param args CLI arguments
     */
    public Configuration(String[] args) {
        this(cliArgumentsToParameters(args));
    }

    private static Map<String, String> cliArgumentsToParameters(String[] args) {
        Map<String, String> map = new LinkedHashMap<>();

        for (String raw : args) {
            String arg = BytesFormatter.unescapeCliArgument(raw);
            if (!arg.startsWith(DEFAULT_PREFIX)) throw new IllegalArgumentException("Failed to parse argument " + arg + " due to missing " + DEFAULT_PREFIX + " prefix");

            if (!arg.contains("=")) { // If flag
                map.putIfAbsent(arg.substring(2), null);
            } else { // If parameter with value
                String[] parts = arg.substring(2).split("=", 2);
                map.putIfAbsent(parts[0], parts[1]);
            }
        }

        return map;
    }

    private Configuration(final Map<String, String> parameters) {
        this(new LinkedHashMap<>(parameters), DEFAULT_PREFIX);
    }

    private Configuration(final Map<String, String> parameters, String prefix) {
        this.parameters = new LinkedHashMap<>(parameters);
        this.prefix = prefix;
    }

    /**
     * Add all parameters from a configuration to this configuration prefixing the keys to add.
     * The keys of the added parameters will be {@code <prefix>-<key of original parameter}.
     *
     * @param prefix        prefix to add
     * @param configuration configuration to copy from
     */
    public void addWithPrefix(final String prefix, final Configuration configuration) {
        configuration.parameters.forEach((key, value) -> this.parameters.put(prefix + KEY_SEPARATOR + key, value));
    }

    /**
     * Extract all parameters from this configuration that have a key starting with the given prefix.
     * The returned configuration will have the prefix and '-' removed from the keys.
     *
     * @param prefix prefix to filter with
     * @return a new configuration containing only the matching parameters
     */
    public Configuration extractWithPrefix(final String prefix) {
        return new Configuration(
                parameters.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(prefix + KEY_SEPARATOR))
                        .collect(Collectors.toMap(entry -> entry.getKey().substring(prefix.length() + 1), Map.Entry::getValue)),
                prefix + KEY_SEPARATOR
        );
    }

    public Configuration copy() {
        return new Configuration(parameters);
    }

    public boolean hasParameter(final String key) {
        return parameters.containsKey(key);
    }

    /* String */
    public Optional<String> getString(final String key) {
        return Optional.ofNullable(parameters.get(key));
    }

    public String requireString(final String key) {
        return getString(key).orElseThrow(() -> new IllegalArgumentException("Parameter " + prefix + key + " not set."));
    }

    public void setString(String key, String value) {
        parameters.put(key, value);
    }


    /* Enum */

    private static <E extends Enum<E>> String extractParameterKeyFromEnumClass(Class<E> enumClass) {
        // Convert Pascal-case enum class name to kebab-case parameter key
        return enumClass.getSimpleName().replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    }


    private static <E extends Enum<E>> String extractParameterKeyFromEnum(E value) {
        return extractParameterKeyFromEnumClass(value.getDeclaringClass());
    }

    private static <E extends Enum<E>> String convertEnumToParameterValue(E value) {
        return value.name().replaceAll("_", KEY_SEPARATOR).toLowerCase(Locale.ROOT);
    }

    private static String convertParameterValueToEnumName(String stringValue) {
        return stringValue.replaceAll(KEY_SEPARATOR, "_").toUpperCase(Locale.ROOT);
    }

    private <E extends Enum<E>> Optional<E> getEnum(String key, Class<E> enumClass) {
        return getString(key).map(stringValue -> {
            String name = convertParameterValueToEnumName(stringValue);
            try {
                return Enum.valueOf(enumClass, name);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed getting parameter " + key + ". No enum constant " + enumClass.getCanonicalName() + "." + name + ".", e);
            }
        });
    }

    public <E extends Enum<E>> E requireEnum(String key, Class<E> enumClass) {
        return getEnum(key, enumClass).orElseThrow(() -> new IllegalArgumentException("Parameter " + prefix + key + " not set."));
    }

    public <E extends Enum<E>> E requireEnum(Class<E> enumClass) {
        return requireEnum(extractParameterKeyFromEnumClass(enumClass), enumClass);
    }

    public <E extends Enum<E>> void setEnum(String key, E value) {
        setString(key, convertEnumToParameterValue(value));
    }

    public <E extends Enum<E>> void setEnum(E value) {
        setEnum(extractParameterKeyFromEnum(value), value);
    }

    /* Int */

    public Optional<Integer> getInt(final String key) {
        return getString(key).map(stringValue -> {
            try {
                return Integer.valueOf(stringValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Failed reading parameter " + key + ".", e);
            }
        });
    }

    public int requireInt(String key) {
        return getInt(key).orElseThrow(() -> new IllegalArgumentException("Parameter " + prefix + key + " not set."));
    }

    public void setInt(String key, Integer port) {
        setString(key, String.valueOf(port));
    }


    /* Double */

    public Optional<Double> getDouble(String key) {
        return getString(key).map(stringValue -> {
            try {
                return Double.valueOf(stringValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Failed reading parameter " + key + ".", e);
            }
        });
    }

    public void setDouble(String key, Double value) {
        setString(key, String.valueOf(value));
    }


    /* Other */

    @Override
    public String toString() {
        return parameters.entrySet().stream()
                .map(parameter -> BytesFormatter.escapeCliArgument(DEFAULT_PREFIX + parameterToString(parameter)))
                .collect(Collectors.joining(" "));
    }

    private static String parameterToString(Map.Entry<String, String> parameter) {
        if (parameter.getValue() == null) return parameter.getKey();
        else return parameter.getKey() + "=" + parameter.getValue();
    }
}
