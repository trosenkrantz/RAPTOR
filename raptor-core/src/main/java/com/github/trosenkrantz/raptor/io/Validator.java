package com.github.trosenkrantz.raptor.io;

import java.util.Optional;

public interface Validator<T> {
    /**
     * Validates a value
     *
     * @param value the value
     * @return empty if valid, other a string describing how it is invalid
     */
    Optional<String> validate(T value);
}
