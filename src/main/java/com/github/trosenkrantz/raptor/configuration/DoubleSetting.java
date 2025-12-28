package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.Optional;

public class DoubleSetting extends Setting<Double> {
    public DoubleSetting(Builder builder) {
        super(builder);
    }

    @Override
    public Optional<Double> read(Configuration configuration) {
        return configuration.getDouble(getParameterKey());
    }

    @Override
    public String valueToString(Configuration configuration) {
        return read(configuration).map(Object::toString).orElse(Setting.EMPTY_VALUE_TO_STRING);
    }

    @Override
    public void configure(Configuration configuration) {
        configuration.setDouble(
                getParameterKey(),
                readOrDefault(configuration)
                        .map(value -> ConsoleIo.askForDouble(getDescription(), value, getValidator()))
                        .orElseGet(() -> ConsoleIo.askForDouble(getDescription(), getValidator()))
        );
    }

    public static class Builder extends Setting.Builder<Double, DoubleSetting.Builder> {
        public Builder(String promptValue, String parameterKey, String name, String description) {
            super(promptValue, parameterKey, name, description);
        }

        @Override
        public DoubleSetting.Builder self() {
            return this;
        }

        @Override
        public DoubleSetting build() {
            return new DoubleSetting(this);
        }
    }
}
