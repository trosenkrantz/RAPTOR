package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.Optional;

public class DoubleSetting extends SettingBase<Double> {
    public DoubleSetting(Builder builder) {
        super(builder);
    }

    @Override
    public Optional<Double> read(Configuration configuration) {
        return configuration.getDouble(getParameterKey());
    }

    @Override
    public String valueToString(Double value) {
        return value.toString();
    }

    @Override
    public void configure(Configuration configuration) {
        Double value;
        if (getDefaultValue().isEmpty()) {
            value = ConsoleIo.askForDouble(getDescription(), getValidator());
        } else {
            value = ConsoleIo.askForDouble(getDescription(), getDefaultValue().get(), getValidator());
        }

        configuration.setDouble(getParameterKey(), value);
    }

    public static class Builder extends SettingBase.Builder<Double, DoubleSetting.Builder> {
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
