package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.UserAbortedException;
import com.github.trosenkrantz.raptor.io.Ansi;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ObjectListSetting<T> extends SettingBase<List<T>> {
    private final Setting<T> group;

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
            result.add(group.readAndRequireOrDefault(itemCfg));
        }

        return Optional.of(result);
    }

    private String configurationToString(List<Configuration> itemConfigurations) {
        if (itemConfigurations.isEmpty()) {
            return SettingBase.EMPTY_VALUE_TO_STRING;
        }

        return itemConfigurations.stream()
                .map(group::readAndRequireOrDefault)
                .map(group::valueToString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public String valueToString(List<T> value) {
        if (value.isEmpty()) {
            return SettingBase.EMPTY_VALUE_TO_STRING;
        }

        return value.stream()
                .map(group::valueToString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public void configure(Configuration configuration) {
        List<Configuration> currentConfigurations = new ArrayList<>();

        while (true) {
            ConsoleIo.writeLine("Configuring " + getDescription() + ". Current value: " + System.lineSeparator() + configurationToString(currentConfigurations) + System.lineSeparator() + "Type " + Ansi.PROMPT.apply("a") + " to add or modify. Type " + Ansi.PROMPT.apply("enter") + " to continue. " + ConsoleIo.getExitString() + ":");
            String answer = ConsoleIo.readLine();

            switch (answer) {
                case "a" -> {
                    Configuration newConfiguration = Configuration.empty();
                    group.configure(newConfiguration);
                    currentConfigurations.add(newConfiguration);
                }
                case "" -> {
                    configuration.setSubConfigurationArray(getParameterKey(), currentConfigurations);
                    return;
                }
                case "e" -> throw new UserAbortedException();
            }
        }
    }

    public static class Builder<T> extends SettingBase.Builder<List<T>, Builder<T>> {
        private final Setting<T> group;

        public Builder(String promptValue, String parameterKey, String name, String description, Setting<T> group) {
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
