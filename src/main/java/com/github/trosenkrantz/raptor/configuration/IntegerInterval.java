package com.github.trosenkrantz.raptor.configuration;

public record IntegerInterval(int min, int max) {
    @Override
    public String toString() {
        return min + " - " + max;
    }
}
