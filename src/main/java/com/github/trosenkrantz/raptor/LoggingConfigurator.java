package com.github.trosenkrantz.raptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class LoggingConfigurator {
    public static void run() throws IOException {
        Logger rootLogger = Logger.getLogger(""); // Get the root logger (global logger for all classes)

        rootLogger.getHandlers()[0].setLevel(Level.OFF); // Turn off the default console logger
        rootLogger.addHandler(getFileHandler());
        rootLogger.addHandler(getConsoleHandler());

        rootLogger.setLevel(Level.ALL);
    }

    private static FileHandler getFileHandler() throws IOException {
        Path path = Files.createDirectories(Path.of("logs"));
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").format(new Date()); // ISO 8601 format
        String logFileName = path.resolve("RAPTOR " + timestamp + ".log").toString();

        FileHandler fileHandler;
        fileHandler = new FileHandler(logFileName);
        fileHandler.setFormatter(new FileFormatter());
        fileHandler.setLevel(Level.ALL);
        return fileHandler;
    }

    private static Handler getConsoleHandler() {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new ConsoleFormatter());
        consoleHandler.setLevel(Level.ALL);
        return consoleHandler;
    }

    private static class FileFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format(
                    "%s: %s%n",
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()), // ISO 8601 format
                    record.getMessage()
            );
        }
    }

    private static class ConsoleFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return record.getMessage()
                    .replaceAll("\t", " ")
                    .replaceAll("\r\n", " ") // If Windows style CR LF, replace with a single space
                    .replaceAll("\r", " ")
                    .replaceAll("\n", " ")
                    + System.lineSeparator();
        }
    }
}
