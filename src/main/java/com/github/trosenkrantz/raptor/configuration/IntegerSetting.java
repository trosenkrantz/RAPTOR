package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.Optional;

public class IntegerSetting extends SettingBase<Integer> {
    private IntegerSetting(Builder builder) {
        super(builder);
    }

    @Override
    public Optional<Integer> read(Configuration configuration) {
        return configuration.getInt(getParameterKey());
    }

    @Override
    public String valueToString(Integer value) {
        return value.toString();
    }

    @Override
    public void configure(Configuration configuration) {
        int value;
        if (getDefaultValue().isEmpty()) {
            value = ConsoleIo.askForInt(getDescription(), getValidator());
        } else {
            value = ConsoleIo.askForInt(getDescription(), getDefaultValue().get(), getValidator());
        }

        configuration.setInt(getParameterKey(), value);
    }

    public static class Builder extends SettingBase.Builder<Integer, Builder> {
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
