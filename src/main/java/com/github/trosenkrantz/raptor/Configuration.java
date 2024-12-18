package com.github.trosenkrantz.raptor;

import com.github.trosenkrantz.raptor.io.BytesFormatter;

import java.util.*;
import java.util.stream.Collectors;

public class Configuration {
    private final List<StringParameter> stringParameters;

    public Configuration(String[] args) {
        stringParameters = Arrays.stream(args).map(arg -> {
            arg = BytesFormatter.unescapeCliArgument(arg);

            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Failed to parse argument " + arg + " due to missing -- prefix");
            }

            String[] parts = arg.substring(2).split("=", 2);
            return new StringParameter(parts[0], parts[1]);
        }).collect(Collectors.toCollection(ArrayList::new)); // Collect to mutable list
    }


    /* String */

    public Optional<String> getString(final String key) {
        return stringParameters.stream().filter(stringParameter -> stringParameter.key().equals(key)).findAny().map(StringParameter::value);
    }

    public String requireString(final String key) {
        return getString(key).orElseThrow(() -> new IllegalArgumentException("Parameter --" + key + " not set."));
    }

    public void setString(String key, String value) {
        stringParameters.add(new StringParameter(key, value));
    }


    /* Enum */

    private static <E extends Enum<E>> String extractParameterKeyFromEnumClass(Class<E> enumClass) {
        // Convert Pascal-case enum class name to kebab-case parameter key
        return enumClass.getSimpleName().replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }


    private static <E extends Enum<E>> String extractParameterKeyFromEnum(E value) {
        return extractParameterKeyFromEnumClass(value.getDeclaringClass());
    }

    private static <E extends Enum<E>> String convertEnumToParameterValue(E value) {
        return value.name().replaceAll("_", "-").toLowerCase();
    }

    private static String convertParameterValueToEnumName(String stringValue) {
        return stringValue.replaceAll("-", "_").toUpperCase();
    }

    public <E extends Enum<E>> Optional<E> getEnum(String parameter, Class<E> enumClass) {
        return getString(parameter).map(stringValue -> {
            String name = convertParameterValueToEnumName(stringValue);
            try {
                return Enum.valueOf(enumClass, name);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed getting parameter " + parameter + ". No enum constant " + enumClass.getCanonicalName() + "." + name + ".", e);
            }
        });
    }

    public <E extends Enum<E>> E requireEnum(Class<E> enumClass) {
        String parameter = extractParameterKeyFromEnumClass(enumClass);
        return getEnum(parameter, enumClass).orElseThrow(() -> new IllegalArgumentException("Parameter --" + parameter + " not set."));
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
        return stringParameters.stream()
                .map(stringParameter -> BytesFormatter.escapeCliArgument("--" + stringParameter.key() + "=" + stringParameter.value()))
                .collect(Collectors.joining(" ")
                );
    }
}
