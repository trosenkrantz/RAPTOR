package com.github.trosenkrantz.raptor.io;

import java.util.Optional;

public interface Validator<T> {
    Optional<String> validate(T value);
}
