package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.AbortedException;
import com.github.trosenkrantz.raptor.io.Ansi;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class StringToStringMapSetting extends Setting<Map<String, String>> {
    private StringToStringMapSetting(Builder builder) {
        super(builder);
    }

    @Override
    public Optional<Map<String, String>> read(Configuration configuration) {
        Optional<Configuration> optionalMapConfiguration = configuration.getSubConfiguration(getParameterKey());
        if (optionalMapConfiguration.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> result = new HashMap<>();
        Configuration mapConfiguration = optionalMapConfiguration.get();
        mapConfiguration.keys().forEach(key -> result.put(key, mapConfiguration.requireFullyEscapedString(key)));
        return Optional.of(result);
    }

    @Override
    public String valueToString(Configuration configuration) {
        return read(configuration).map(StringToStringMapSetting::valueToString).orElse(Setting.EMPTY_VALUE_TO_STRING);
    }

    private static String valueToString(Map<String, String> map) {
        if (map.isEmpty()) return Setting.EMPTY_VALUE_TO_STRING;

        return map.entrySet().stream().map(
                entry -> entry.getKey() + ": " + entry.getValue()
        ).collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public void configure(Configuration configuration, Map<String, String> currentValue) {
        while (true) {
            ConsoleIo.writeLine("Configuring " + getDescription() + ". Current value: " + System.lineSeparator() + valueToString(currentValue) + System.lineSeparator() + "Type " + Ansi.PROMPT.apply("a") + " to add or modify. Type " + Ansi.PROMPT.apply("enter") + " to continue. " + ConsoleIo.getExitString() + ":");
            String answer = ConsoleIo.readLine();

            switch (answer) {
                case "a" -> {
                    String key = ConsoleIo.askForString("Key to configure");
                    String value = ConsoleIo.askForString("Value for " + key);
                    currentValue.put(key, value);
                }
                case "" -> { // User chosen to finish configuring this setting
                    Configuration mapConfiguration = Configuration.empty();
                    currentValue.forEach(mapConfiguration::setFullyEscapedString);
                    configuration.setSubConfiguration(getParameterKey(), mapConfiguration);

                    return;
                }
                case "e" -> throw new AbortedException();
                default -> ConsoleIo.writeLine("Unrecognised answer.", Ansi.ERROR); // And do another iteration
            }
        }
    }

    public static class Builder extends Setting.Builder<Map<String, String>, Builder> {
        public Builder(String promptValue, String parameterKey, String name, String description) {
            super(promptValue, parameterKey, name, description);
            defaultValue(new HashMap<>());
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public StringToStringMapSetting build() {
            return new StringToStringMapSetting(this);
        }
    }
}

