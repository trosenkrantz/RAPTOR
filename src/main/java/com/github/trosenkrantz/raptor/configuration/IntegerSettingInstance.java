package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

public class IntegerSettingInstance extends SettingInstance<Integer> {
    public IntegerSettingInstance(Setting<Integer> setting, Integer value) {
        super(setting, value);
    }

    @Override
    public void configure(Configuration configuration) {
        int newValue;

        if (getValue().isPresent()) {
            newValue = ConsoleIo.askForInt(getSetting().getDescription(), getValue().get());
        } else {
            newValue = ConsoleIo.askForInt(getSetting().getDescription());
        }

        configuration.setInt(getSetting().getParameterKey(), newValue);
    }

    @Override
    public String toString() {
        return getValue().map(Object::toString).orElse(SettingInstance.EMPTY_VALUE_TO_STRING);
    }
}
