package com.github.trosenkrantz.raptor.configuration;

public record Interval(int min, int max) {
    public Interval {
        if (min > max) throw new IllegalArgumentException("min cannot be greater than max");
    }

    @Override
    public String toString() {
        return min + " - " + max;
    }
}
