package com.github.trosenkrantz.raptor.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class RaptorPathsTest {
    @AfterEach
    void resetBase() {
        RaptorPaths.useCurrentDirectory();
    }

    @Test
    void resolvesRelativePathAgainstBase(@TempDir Path configDir) {
        RaptorPaths.setBase(configDir);

        Assertions.assertEquals(configDir.resolve("keystore.p12"), RaptorPaths.resolve("keystore.p12"));
    }

    @Test
    void leavesAbsolutePathUnchanged(@TempDir Path configDir) {
        RaptorPaths.setBase(configDir);
        Path absolute = configDir.resolve("certs/key.p12");

        Assertions.assertEquals(absolute, RaptorPaths.resolve(absolute.toString()));
    }

    @Test
    void normalizesResolvedPath(@TempDir Path configDir) {
        RaptorPaths.setBase(configDir);

        Assertions.assertEquals(configDir.resolve("keystore.p12"), RaptorPaths.resolve("certs/../keystore.p12"));
    }

    @Test
    void usesCurrentDirectoryByDefault(@TempDir Path unused) {
        RaptorPaths.useCurrentDirectory();

        Assertions.assertEquals(Path.of("").toAbsolutePath().normalize().resolve("file"), RaptorPaths.resolve("file"));
    }
}
