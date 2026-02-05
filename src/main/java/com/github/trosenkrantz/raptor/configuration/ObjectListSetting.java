package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.AbortedException;
import com.github.trosenkrantz.raptor.io.Ansi;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ObjectListSetting<T> extends Setting<List<T>> {
    private final SettingGroup<T> group;

    public ObjectListSetting(Builder<T> builder) {
        super(builder);
        this.group = builder.group;
    }

    @Override
    public Optional<List<T>> read(Configuration configuration) {

        List<Configuration> items = configuration.getSubConfigurationArray(getParameterKey());

        if (items.isEmpty()) {
            return Optional.empty();
        }

        List<T> result = new ArrayList<>();

        for (Configuration itemCfg : items) {
            result.add(group.readAndRequire(itemCfg));
        }

        return Optional.of(result);
    }

    @Override
    public String valueToString(Configuration configuration) {
        return configurationToString(configuration.getSubConfigurationArray(getParameterKey()));
    }

    private String configurationToString(List<Configuration> itemConfigurations) {
        if (itemConfigurations.isEmpty()) {
            return Setting.EMPTY_VALUE_TO_STRING;
        }

        return itemConfigurations.stream()
                .map(group::valueToString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public void configure(Configuration configuration, List<T> currentValue) {
        List<Configuration> currentConfigurations = new ArrayList<>();

        while (true) {
            ConsoleIo.writeLine("Configuring " + getDescription() + ". Current value: " + System.lineSeparator() + configurationToString(currentConfigurations) + System.lineSeparator() + "Type " + Ansi.PROMPT.apply("a") + " to add or modify. Type " + Ansi.PROMPT.apply("enter") + " to continue. " + ConsoleIo.getExitString() + ":");
            String answer = ConsoleIo.readLine();

            switch (answer) {
                case "a" -> {
                    Configuration newConfiguration = group.configure();
                    currentConfigurations.add(newConfiguration);
                }
                case "" -> {
                    configuration.setSubConfigurationArray(getParameterKey(), currentConfigurations);
                    return;
                }
                case "e" -> throw new AbortedException();
            }
        }
    }

    public static class Builder<T>
            extends Setting.Builder<List<T>, Builder<T>> {

        private final SettingGroup<T> group;

        public Builder(
                String promptValue,
                String parameterKey,
                String name,
                String description,
                SettingGroup<T> group
        ) {
            super(promptValue, parameterKey, name, description);
            this.group = group;
        }

        @Override
        public Builder<T> self() {
            return this;
        }

        @Override
        public ObjectListSetting<T> build() {
            return new ObjectListSetting<>(this);
        }
    }
}
