package com.github.trosenkrantz.raptor.io;

import com.github.trosenkrantz.raptor.Configuration;

public enum Ansi {
    ERROR("\u001B[31m"), // Red
    PROMPT("\u001B[36m"), // Cyan
    LESS_IMPORTANT("\u001B[90m"); // Grey

    private static final String RESET = "\u001B[0m";

    private static boolean enabled;

    private final String code;

    Ansi(String code) {
        this.code = code;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void configure(Configuration configuration) {
        enabled = configuration.getString("no-ansi").isEmpty();
    }

    public String apply(String message) {
        if (enabled) {
            return this.code + message + RESET;
        } else {
            return message;
        }
    }
}