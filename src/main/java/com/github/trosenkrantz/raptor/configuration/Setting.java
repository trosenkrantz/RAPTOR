package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.Configuration;

import java.util.Optional;

public abstract class Setting<T> {
    private final String promptValue;
    private final String description;
    private final String parameterKey;
    private final T defaultValue; // Null means no default value

    /**
     * Constructor
     * @param promptValue value to display to the user
     * @param parameterKey value to use a key in the configuration
     * @param description description
     * @param defaultValue may be null
     */
    public Setting(String promptValue, String parameterKey, String description, T defaultValue) {
        this.promptValue = promptValue;
        this.parameterKey = parameterKey;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public String getPromptValue() {
        return promptValue;
    }

    public String getParameterKey() {
        return parameterKey;
    }

    public String getDescription() {
        return description;
    }

    Optional<T> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public abstract SettingInstance<T> instantiateDefault();

    public abstract Optional<T> read(Configuration configuration);

    public T readAndRequire(Configuration configuration) {
        Optional<T> value = read(configuration);
        if (value.isEmpty()) {
            throw new IllegalStateException("Required setting " + getParameterKey() + " not set.");
        }
        return value.get();
    }
}