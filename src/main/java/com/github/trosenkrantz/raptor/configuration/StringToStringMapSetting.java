package com.github.trosenkrantz.raptor.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.trosenkrantz.raptor.AbortedException;
import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.io.Ansi;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StringToStringMapSetting extends Setting<Map<String, String>> {
    private static final Logger LOGGER = Logger.getLogger(StringToStringMapSetting.class.getName());

    private StringToStringMapSetting(Builder builder) {
        super(builder);
    }

    @Override
    public Optional<Map<String, String>> read(Configuration configuration) {
        Optional<String> rawValue = configuration.getString(getParameterKey());
        if (rawValue.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ObjectMapper().readValue(rawValue.get(), new TypeReference<>() {
            }));
        } catch (JsonProcessingException e) {
            LOGGER.warning("Failed reading JSON. " + e.getMessage() + ". Ignoring the JSON value.");
            return Optional.empty();
        }
    }

    @Override
    public String valueToString(Configuration configuration) {
        return read(configuration).map(StringToStringMapSetting::valueToString).orElse(Setting.EMPTY_VALUE_TO_STRING);
    }

    private static String valueToString(Map<String, String> map) {
        return map.entrySet().stream().map(
                entry -> entry.getKey() + ": " + entry.getValue()
        ).collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public void configure(Configuration configuration) {
        Map<String, String> current = readOrDefault(configuration).orElse(new HashMap<>());

        while (true) {
            ConsoleIo.writeLine("Configuring " + getDescription() + ". Current value: " + System.lineSeparator() + valueToString(current) + System.lineSeparator() + "Type " + Ansi.PROMPT.apply("a") + " to add or modify. Type " + Ansi.PROMPT.apply("enter") + " to continue. " + ConsoleIo.getExitString() + ":");
            String answer = ConsoleIo.readLine();

            if (answer.equals("a")) {
                String key = ConsoleIo.askForString("Key to configure");
                String value = ConsoleIo.askForString("Value for " + key);
                current.put(key, value);
                continue;
            }
            if (answer.isEmpty()) { // User chosen to continue
                try {
                    configuration.setString(getParameterKey(), new ObjectMapper().writeValueAsString(current));
                } catch (JsonProcessingException e) {
                    LOGGER.warning("Failed writing JSON. " + e.getMessage());
                }
                return;
            }
            if (answer.equals("e")) throw new AbortedException();

            ConsoleIo.writeLine("Unrecognised answer.", Ansi.ERROR);
        }
    }

    public static class Builder extends Setting.Builder<Map<String, String>, Builder> {
        public Builder(String promptValue, String parameterKey, String name, String description) {
            super(promptValue, parameterKey, name, description);
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

