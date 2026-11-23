package com.github.trosenkrantz.raptor.io;

import java.nio.file.Path;

/** Resolves file paths from configuration values against the configuration base directory. */
public final class RaptorPaths {
    private static Path base;

    private RaptorPaths() {
    }

    public static void useCurrentDirectory() {
        base = Path.of("").toAbsolutePath().normalize();
    }

    public static void setBase(Path path) {
        base = path.toAbsolutePath().normalize();
    }

    static void clearBase() {
        base = null;
    }

    public static Path resolve(String path) {
        if (base == null) {
            throw new IllegalStateException("The configuration base directory has not been determined.");
        }
        Path parsed = Path.of(path);
        return parsed.isAbsolute() ? parsed.normalize() : base.resolve(parsed).normalize();
    }
}
