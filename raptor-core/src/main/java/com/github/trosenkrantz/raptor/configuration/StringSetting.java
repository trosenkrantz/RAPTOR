package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.Optional;

public class StringSetting extends SettingBase<String> {
    private StringSetting(Builder builder) {
        super(builder);
    }

    @Override
    public Optional<String> read(Configuration configuration) {
        return configuration.getRaptorEncodedString(getParameterKey());
    }

    @Override
    public String valueToString(String value) {
        return value;
    }

    @Override
    public void configure(Configuration configuration) {
        String value;
        if (getDefaultValue().isEmpty()) {
            value = ConsoleIo.askForString(getDescription());
        } else {
            value = ConsoleIo.askForString(getDescription(), getDefaultValue().get());
        }

        configuration.setRaptorEncodedString(getParameterKey(), value);
    }

    public static class Builder extends SettingBase.Builder<String, Builder> {
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
