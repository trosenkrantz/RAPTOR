package com.github.trosenkrantz.raptor.io;

import java.util.Optional;
import java.util.function.Function;

public class IpPortValidator {
    private IpPortValidator() {
    }

    public static final Function<Integer, Optional<String>> VALIDATOR = port -> {
        if (port < 0 || port > 65535) {
            return Optional.of("Must be between 0 and 65535.");
        }
        return Optional.empty();
    };
}
