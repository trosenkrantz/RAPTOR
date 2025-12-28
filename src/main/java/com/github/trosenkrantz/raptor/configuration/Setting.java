package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.io.Validator;

import java.util.Optional;

/**
 * Something to be configured by a user as well as stored in and read from the CLI configuration.
 * @param <T> Type for what the settings model
 */
public abstract class Setting<T> {
    public static String EMPTY_VALUE_TO_STRING = "N/A";

    private final String promptValue;
    private final String parameterKey;
    private final String name;
    private final String description;
    private final T defaultValue; // Null means no default value
    private final Validator<T> validator;

    public Setting(Builder<T, ?> builder) {
        this.promptValue = builder.promptValue;
        this.parameterKey = builder.parameterKey;
        this.name = builder.name;
        this.description = builder.description;
        this.defaultValue = builder.defaultValue;
        this.validator = builder.validator;
    }

    public String getPromptValue() {
        return promptValue;
    }

    public String getParameterKey() {
        return parameterKey;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    Optional<T> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public Validator<T> getValidator() {
        return validator;
    }

    /**
     * Read the value for this setting from the configuration.
     * @param configuration Configuration to read from
     * @return Optional setting value, empty if not set
     */
    public abstract Optional<T> read(Configuration configuration);

    public T readAndRequire(Configuration configuration) {
        Optional<T> value = read(configuration);
        if (value.isEmpty()) {
            throw new IllegalStateException("Required setting " + getParameterKey() + " not set.");
        }
        return value.get();
    }

    protected Optional<T> readOrDefault(Configuration configuration) {
        return read(configuration).or(this::getDefaultValue);
    }

    /**
     * Used to display the value of this setting to the user.
     * @param configuration Configuration to read from
     * @return String representation of the setting value
     */
    public abstract String valueToString(Configuration configuration);

    public abstract void configure(Configuration configuration);

    public static abstract class Builder<T, B extends Builder<T, B>> {
        private final String promptValue;
        private final String parameterKey;
        private final String name;
        private final String description;
        private T defaultValue;
        private Validator<T> validator;

        protected Builder(String promptValue, String parameterKey, String name, String description) {
            this.promptValue = promptValue;
            this.parameterKey = parameterKey;
            this.name = name;
            this.description = description;
        }

        public B defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return self();
        }

        public B validator(Validator<T> validator) {
            this.validator = validator;
            return self();
        }

        public abstract B self();

        public abstract Setting<T> build();
    }
}