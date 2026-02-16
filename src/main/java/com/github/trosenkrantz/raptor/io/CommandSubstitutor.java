package com.github.trosenkrantz.raptor.io;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.configuration.IntegerSetting;

import java.util.Optional;

public class CommandSubstitutor {
    private static final String PARAMETER_TIMEOUT = "commandSubstitutionTimeout";
    private static final String TIMEOUT_DESCRIPTION = "Timeout used for command substitutions in ms";

    public static final int DEFAULT_TIMEOUT = 1000;
    public static final IntegerSetting TIMEOUT_SETTING = new IntegerSetting.Builder("t", PARAMETER_TIMEOUT, "Command substitution timeout", TIMEOUT_DESCRIPTION)
            .defaultValue(DEFAULT_TIMEOUT).build();

    public static void configureTimeout(Configuration configuration) {
        configuration.setInt(PARAMETER_TIMEOUT, ConsoleIo.askForInt(TIMEOUT_DESCRIPTION, DEFAULT_TIMEOUT, CommandSubstitutor::validateTimeout));
    }

    private static Optional<String> validateTimeout(Integer timeout) {
        if (timeout <= 0) {
            return Optional.of("Must be positive.");
        }
        return Optional.empty();
    }

    public static int requireTimeout(Configuration configuration) {
        return configuration.requireInt(PARAMETER_TIMEOUT);
    }
}
