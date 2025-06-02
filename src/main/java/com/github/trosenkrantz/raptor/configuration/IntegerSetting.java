package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.Optional;

public class IntegerSetting extends Setting<Integer> {
    public IntegerSetting(String promptValue, String parameterKey, String description) {
        super(promptValue, parameterKey, description, null);
    }

    public IntegerSetting(String promptValue, String parameterKey, String description, Integer defaultValue) {
        super(promptValue, parameterKey, description, defaultValue);
    }

    @Override
    public Optional<Integer> read(Configuration configuration) {
        return configuration.getInt(getParameterKey());
    }

    @Override
    public String valueToString(Configuration configuration) {
        return read(configuration).map(Object::toString).orElse(Setting.EMPTY_VALUE_TO_STRING);
    }

    @Override
    public void configure(Configuration configuration) {
        configuration.setInt(
                getParameterKey(),
                read(configuration)
                        .map(integer -> ConsoleIo.askForInt(getDescription(), integer))
                        .orElseGet(() -> ConsoleIo.askForInt(getDescription()))
        );
    }
}
