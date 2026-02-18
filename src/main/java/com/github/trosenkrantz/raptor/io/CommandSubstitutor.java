package com.github.trosenkrantz.raptor.io;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.configuration.IntegerSetting;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandSubstitutor {
    private static final Logger LOGGER = Logger.getLogger(CommandSubstitutor.class.getName());

    private static final String PARAMETER_TIMEOUT = "commandSubstitutionTimeout";
    private static final String TIMEOUT_DESCRIPTION = "Timeout used for command substitutions in ms";

    public static final int DEFAULT_TIMEOUT = 1000;
    public static final IntegerSetting TIMEOUT_SETTING = new IntegerSetting.Builder("t", PARAMETER_TIMEOUT, "Command substitution timeout", TIMEOUT_DESCRIPTION)
            .defaultValue(DEFAULT_TIMEOUT)
            .validator(timeout -> {
                if (timeout <= 0) {
                    return Optional.of("Must be positive.");
                }
                return Optional.empty();
            })
            .build();

    /**
     * Executes a command.
     *
     * @param command command to execute
     * @param timeout timeout in ms
     * @return stdout of command, or empty if failed
     */
    public static byte[] executeCommand(String command, int timeout) {
        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        String[] shellConfig = isWindows ? new String[]{"cmd", "/c", command} : new String[]{"sh", "-c", command};

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Process process = new ProcessBuilder(shellConfig).start();

            // Start capturing streams in parallel to prevent deadlock
            Future<byte[]> stdoutFuture = executor.submit(() -> process.getInputStream().readAllBytes());
            Future<byte[]> stderrFuture = executor.submit(() -> process.getErrorStream().readAllBytes());

            try {
                byte[] stdout = stdoutFuture.get(timeout, TimeUnit.MILLISECONDS); // Timeout to not block RAPTOR
                byte[] stderr = stderrFuture.get(100, TimeUnit.MILLISECONDS); // Expect stderr to end at most shortly after

                if (process.waitFor(100, TimeUnit.MILLISECONDS)) { // Expect process to terminate at most shortly after
                    if (stderr.length > 0) {
                        LOGGER.warning("Command " + command + " reported stderr: " + new String(stderr, StandardCharsets.UTF_8).trim()); // Use UTF-8 as that is usually the case for stderr, and out logging framework supports it
                    }

                    int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        LOGGER.severe("Command " + command + " existed with code " + exitCode + ". Skipping processing its output.");
                        return new byte[0];
                    }
                } else {
                    process.destroyForcibly();
                    LOGGER.warning("Command " + command + " did not terminate by itself within 100 ms of closing its standard streams. Killed the process.");
                    return stdout;
                }

                return stdout;
            } catch (TimeoutException e) {
                process.destroyForcibly();
                LOGGER.severe("Command " + command + " did not end its standard streams within " + timeout + " ms. Killed the process and skipping processing its output.");
                return new byte[0];
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed command " + command + ". ", e);
            return new byte[0];
        }
    }
}
