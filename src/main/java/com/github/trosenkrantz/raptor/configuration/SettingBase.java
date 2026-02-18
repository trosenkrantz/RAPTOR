package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.io.Validator;

import java.util.Optional;

public abstract class SettingBase<T> implements Setting<T> {
    public static String EMPTY_VALUE_TO_STRING = "N/A";

    private final String promptValue;
    private final String parameterKey;
    private final String name;
    private final String description;
    private final T defaultValue; // Null means no default value
    private final Validator<T> validator;

    public SettingBase(Builder<T, ?> builder) {
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

    public Optional<T> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public Validator<T> getValidator() {
        return validator;
    }

    @Override
    public T readAndRequireOrDefault(Configuration configuration) {
        Optional<T> value = read(configuration);
        if (value.isEmpty()) {
            if (defaultValue != null) return defaultValue;
            else throw new IllegalStateException("Required setting " + getParameterKey() + " not set.");
        }
        return value.get();
    }

//    @Override
//    public void configure(Configuration configuration) {
//        configure(configuration, read(configuration).or(this::getDefaultValue).orElse(null));
//    }
//
//    /**
//     * Prompts the user to configure this setting.
//     *
//     * @param configuration configuration to set this setting with
//     * @param currentValue  current value if any, otherwise the default value if any, otherwise null
//     */
//    protected abstract void configureWithDefault(Configuration configuration, T defaultValue);
//
//    protected abstract void configureWithoutDefault(Configuration configuration);

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

            this.validator = value -> Optional.empty();
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

        public abstract SettingBase<T> build();
    }
}