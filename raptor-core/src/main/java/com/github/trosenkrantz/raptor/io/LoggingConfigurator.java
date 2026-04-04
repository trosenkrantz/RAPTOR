package com.github.trosenkrantz.raptor.io;

import com.github.trosenkrantz.raptor.logging.FileFormatter;

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

    public static void initialise() throws IOException, URISyntaxException {
        Logger rootLogger = Logger.getLogger(""); // Get the root logger (global logger for all classes)

        rootLogger.getHandlers()[0].setLevel(Level.OFF); // Turn off the default console logger
        String logFileName = getLogFileName();
        rootLogger.addHandler(getFileHandler(logFileName));
        rootLogger.addHandler(getConsoleHandler());

        rootLogger.setLevel(Level.WARNING); // Only show warnings and above from libraries

        // Enable all logging for own application
        Logger myLogger = Logger.getLogger("com.github.trosenkrantz.raptor");
        myLogger.setLevel(Level.ALL);

        logVersion();
        ConsoleIo.writeLine("Logging to " + logFileName + "." + System.lineSeparator());
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
        fileHandler.setLevel(Level.ALL);
        return fileHandler;
    }

    private static Handler getConsoleHandler() {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new ConsoleFormatter());
        consoleHandler.setLevel(Level.INFO);
        return consoleHandler;
    }
}
