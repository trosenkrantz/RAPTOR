package com.github.trosenkrantz.raptor.io;

import java.util.function.Predicate;

/**
 * Used to wrap a predicate that declares throwing of a checked exceptions.
 * This wraps it into a predicate that throws a runtime exception.
 *
 * @param <T> Type of argument for the predicate
 */
@FunctionalInterface
public interface CheckedPredicate<T> {
    boolean test(T value) throws Exception;

    static <T> Predicate<T> wrap(CheckedPredicate<T> predicate) {
        return value -> {
            try {
                return predicate.test(value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}