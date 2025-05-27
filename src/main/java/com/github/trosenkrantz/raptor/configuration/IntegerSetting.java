package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.Configuration;

import java.util.Optional;

public class IntegerSetting extends Setting<Integer> {
    public IntegerSetting(String promptValue, String parameterKey, String description) {
        super(promptValue, parameterKey, description, null);
    }

    public IntegerSetting(String promptValue, String parameterKey, String description, Integer defaultValue) {
        super(promptValue, parameterKey, description, defaultValue);
    }

    @Override
    public SettingInstance<Integer> instantiateDefault() {
        return new IntegerSettingInstance(this, getDefaultValue().orElse(null));
    }

    @Override
    public Optional<Integer> read(Configuration configuration) {
        return configuration.getInt(getParameterKey());
    }
}
