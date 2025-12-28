package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.Optional;

public class IntegerSetting extends Setting<Integer> {
    private IntegerSetting(Builder builder) {
        super(builder);
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
                readOrDefault(configuration)
                        .map(value -> ConsoleIo.askForInt(getDescription(), value, getValidator()))
                        .orElseGet(() -> ConsoleIo.askForInt(getDescription()))
        );
    }

    public static class Builder extends Setting.Builder<Integer, Builder> {
        public Builder(String promptValue, String parameterKey, String name, String description) {
            super(promptValue, parameterKey, name, description);
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public IntegerSetting build() {
            return new IntegerSetting(this);
        }
    }
}
