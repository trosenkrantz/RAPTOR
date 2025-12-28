package com.github.trosenkrantz.raptor.io;

import java.util.Arrays;

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

    public static void configure(String[] args) {
        enabled = !Arrays.asList(args).contains("--no-ansi");
    }

    public String apply(String message) {
        if (enabled) {
            return this.code + message + RESET;
        } else {
            return message;
        }
    }
}