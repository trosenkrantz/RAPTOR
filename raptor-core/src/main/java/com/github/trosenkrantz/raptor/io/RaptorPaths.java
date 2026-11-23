package com.github.trosenkrantz.raptor.io;

import java.nio.file.Path;

/**
 * Resolves file paths that appear in configuration values. Relative paths are
 * anchored to the directory of the configuration being used, or to the current
 * working directory when no configuration is involved.
 */
public final class RaptorPaths {
    private static Path base = currentDirectory();

    private RaptorPaths() {
    }

    private static Path currentDirectory() {
        return Path.of("").toAbsolutePath().normalize();
    }

    public static void useCurrentDirectory() {
        base = currentDirectory();
    }

    public static void setBase(Path path) {
        base = path.toAbsolutePath().normalize();
    }

    public static Path base() {
        return base;
    }

    public static Path resolve(String path) {
        Path parsed = Path.of(path);
        return parsed.isAbsolute() ? parsed.normalize() : base.resolve(parsed).normalize();
    }
}
