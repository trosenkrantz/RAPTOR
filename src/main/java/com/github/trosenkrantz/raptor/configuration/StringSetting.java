package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.Optional;

public class StringSetting extends Setting<String> {
    private StringSetting(Builder builder) {
        super(builder);
    }

    @Override
    public Optional<String> read(Configuration configuration) {
        return configuration.getString(getParameterKey());
    }

    @Override
    public String valueToString(Configuration configuration) {
        return read(configuration).orElse(Setting.EMPTY_VALUE_TO_STRING);
    }

    @Override
    public void configure(Configuration configuration, String currentValue) {
        String value;
        if (currentValue == null) {
            value = ConsoleIo.askForString(getDescription());
        } else {
            value = ConsoleIo.askForString(getDescription(), currentValue);
        }

        configuration.setString(getParameterKey(), value);
    }

    public static class Builder extends Setting.Builder<String, Builder> {
        public Builder(String promptValue, String parameterKey, String name, String description) {
            super(promptValue, parameterKey, name, description);
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public StringSetting build() {
            return new StringSetting(this);
        }
    }
}
