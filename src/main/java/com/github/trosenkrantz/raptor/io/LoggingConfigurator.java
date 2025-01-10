package com.github.trosenkrantz.raptor.io;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.jar.JarFile;
import java.util.logging.*;

public class LoggingConfigurator {
    private static final Logger LOGGER = Logger.getLogger(LoggingConfigurator.class.getName());

    private static final DateTimeFormatter TIME_FORMATTER_FILE_NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");
    private static final DateTimeFormatter TIME_FORMATTER_RECORD = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static void initialise() throws IOException, URISyntaxException {
        Logger rootLogger = Logger.getLogger(""); // Get the root logger (global logger for all classes)

        rootLogger.getHandlers()[0].setLevel(Level.OFF); // Turn off the default console logger
        String logFileName = getLogFileName();
        rootLogger.addHandler(getFileHandler(logFileName));
        rootLogger.addHandler(getConsoleHandler());

        rootLogger.setLevel(Level.INFO);

        logVersion();
        ConsoleIo.writeLine("Logging to " + logFileName + ".");
    }

    private static String getLogFileName() throws IOException {
        Path logsPath = Files.createDirectories(Path.of("logs"));
        String timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(TIME_FORMATTER_FILE_NAME);
        return logsPath.resolve(timestamp + ".log").toAbsolutePath().toString();
    }

    private static void logVersion() throws IOException, URISyntaxException {
        try (JarFile jarFile = new JarFile(LoggingConfigurator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())) {
            LOGGER.info("Running RAPTOR version " + jarFile.getManifest().getMainAttributes().getValue("Implementation-Version") + ".");
        }
    }

    private static FileHandler getFileHandler(String logFileName) throws IOException {
        FileHandler fileHandler;
        fileHandler = new FileHandler(logFileName);
        fileHandler.setFormatter(new FileFormatter());
        fileHandler.setLevel(Level.INFO);
        return fileHandler;
    }

    private static Handler getConsoleHandler() {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new ConsoleFormatter());
        consoleHandler.setLevel(Level.INFO);
        return consoleHandler;
    }

    private static class FileFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format(
                    "%s: %s%n",
                    LocalDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault()).format(TIME_FORMATTER_RECORD),
                    record.getMessage()
            );
        }
    }

    private static class ConsoleFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return record.getMessage() + System.lineSeparator();
        }
    }
}
