package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.Configuration;

import java.util.Optional;

public abstract class SettingInstance<T> {
    public static String EMPTY_VALUE_TO_STRING = "N/A";

    private final Setting<T> setting;

    private T value; // Null means no value

    /**
     * @param setting setting
     * @param value value, may be null
     */
    public SettingInstance(Setting<T> setting, T value) {
        this.setting = setting;
        this.value = value;
    }

    public Setting<T> getSetting() {
        return setting;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    public abstract void configure(Configuration configuration);

    @Override
    public abstract String toString();
}
