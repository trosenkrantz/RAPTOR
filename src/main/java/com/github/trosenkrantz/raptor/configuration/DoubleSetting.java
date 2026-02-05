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
    public void configure(Configuration configuration, Double currentValue) {
        Double value;
        if (currentValue == null) {
            value = ConsoleIo.askForDouble(getDescription(), getValidator());
        } else {
            value = ConsoleIo.askForDouble(getDescription(), currentValue, getValidator());
        }

        configuration.setDouble(getParameterKey(), value);
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
