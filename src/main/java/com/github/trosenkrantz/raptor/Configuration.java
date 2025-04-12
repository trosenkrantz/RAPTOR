package com.github.trosenkrantz.raptor;

import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.udp.EndpointMode;

import java.util.*;
import java.util.stream.Collectors;

public class Configuration {
    private final Map<String, String> parameters; // TODO Data structure to keep order of insertion?

    public Configuration() {
        parameters = new HashMap<>();
    }

    /**
     * Creates a new configuration from CLI arguments.
     * Each argument must be in the form --{@code key}={@code value}.
     *
     * @param args CLI arguments
     */
    public Configuration(String[] args) {
        parameters = Arrays.stream(args).map(arg -> {
            arg = BytesFormatter.unescapeCliArgument(arg);

            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Failed to parse argument " + arg + " due to missing -- prefix");
            }
            if (!arg.contains("=")) {
                throw new IllegalArgumentException("Failed to parse argument " + arg + " due to missing =");
            }

            return arg.substring(2).split("=", 2);
        }).collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
    }

    private Configuration(final Map<String, String> parameters) {
        this.parameters = new HashMap<>(parameters);
    }

    /**
     * Add all parameters from a configuration to this configuration prefixing the keys to add.
     * The keys of the added parameters will be {@code <prefix>-<key of original parameter}.
     *
     * @param prefix        prefix to add
     * @param configuration configuration to copy from
     */
    public void addWithPrefix(final String prefix, final Configuration configuration) {
        configuration.parameters.forEach((key, value) -> this.parameters.put(prefix + "-" + key, value));
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
                        .filter(entry -> entry.getKey().startsWith(prefix + "-"))
                        .collect(Collectors.toMap(entry -> entry.getKey().substring(prefix.length() + 1), Map.Entry::getValue))
        );
    }

    public Configuration copy() {
        return new Configuration(parameters);
    }

    /* String */
    public Optional<String> getString(final String key) {
        return Optional.ofNullable(parameters.get(key));
    }

    public String requireString(final String key) {
        return getString(key).orElseThrow(() -> new IllegalArgumentException("Parameter --" + key + " not set."));
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
        return value.name().replaceAll("_", "-").toLowerCase(Locale.ROOT);
    }

    private static String convertParameterValueToEnumName(String stringValue) {
        return stringValue.replaceAll("-", "_").toUpperCase(Locale.ROOT);
    }

    private <E extends Enum<E>> Optional<E> getEnum(String parameter, Class<E> enumClass) {
        return getString(parameter).map(stringValue -> {
            String name = convertParameterValueToEnumName(stringValue);
            try {
                return Enum.valueOf(enumClass, name);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed getting parameter " + parameter + ". No enum constant " + enumClass.getCanonicalName() + "." + name + ".", e);
            }
        });
    }

    public <E extends Enum<E>> E requireEnum(String parameter, Class<E> enumClass) {
        return getEnum(parameter, enumClass).orElseThrow(() -> new IllegalArgumentException("Parameter --" + parameter + " not set."));
    }

    public <E extends Enum<E>> E requireEnum(Class<E> enumClass) {
        return requireEnum(extractParameterKeyFromEnumClass(enumClass), enumClass);
    }

    public <E extends Enum<E>> void setEnum(String parameter, E value) {
        setString(parameter, convertEnumToParameterValue(value));
    }

    public <E extends Enum<E>> void setEnum(E value) {
        setEnum(extractParameterKeyFromEnum(value), value);
    }

    /* Int */

    public Optional<Integer> getInt(final String parameter) {
        return getString(parameter).map(stringValue -> {
            try {
                return Integer.valueOf(stringValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Failed reading parameter " + parameter + ".", e);
            }
        });
    }

    public int requireInt(String parameter) {
        return getInt(parameter).orElseThrow(() -> new IllegalArgumentException("Parameter --" + parameter + " not set."));
    }

    public void setInt(String key, Integer port) {
        setString(key, String.valueOf(port));
    }


    /* Other */

    @Override
    public String toString() {
        return parameters.entrySet().stream()
                .map(parameter -> BytesFormatter.escapeCliArgument("--" + parameter.getKey() + "=" + parameter.getValue()))
                .collect(Collectors.joining(" "));
    }
}
