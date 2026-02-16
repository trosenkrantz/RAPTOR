package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.Optional;

public class EnumSetting<T extends Enum<T> & ConfigurableEnum & PromptEnum> extends Setting<T> {
    private final Class<T> enumClass;

    public EnumSetting(Builder<T> builder) {
        super(builder);
        this.enumClass = builder.enumClass;
    }

    @Override
    public Optional<T> read(Configuration configuration) {
        return configuration.getEnum(enumClass);
    }

    @Override
    public String valueToString(Configuration configuration) {
        return read(configuration).map(Object::toString).orElse(Setting.EMPTY_VALUE_TO_STRING);
    }

    @Override
    public void configure(Configuration configuration, T currentValue) {
        T value;
        if (currentValue == null) {
            value = ConsoleIo.askForOptions(getDescription(), enumClass);
        } else {
            value = ConsoleIo.askForOptions(enumClass, currentValue);
        }

        configuration.setEnum(value);
    }

    public static class Builder<T extends Enum<T> & ConfigurableEnum & PromptEnum> extends Setting.Builder<T, EnumSetting.Builder<T>> {
        private final Class<T> enumClass;

        public Builder(String promptValue, String parameterKey, String name, String description, Class<T> enumClass) {
            super(promptValue, parameterKey, name, description);
            this.enumClass = enumClass;
        }

        @Override
        public Builder<T> self() {
            return this;
        }

        @Override
        public EnumSetting<T> build() {
            return new EnumSetting<T>(this);
        }
    }
}
