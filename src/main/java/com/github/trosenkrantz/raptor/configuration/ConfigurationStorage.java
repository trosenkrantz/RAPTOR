package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ConfigurationStorage {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationStorage.class.getName());

    public static void offerSaveConfiguration(Configuration configuration) throws IOException {
        SaveConfigurationOptions chosenOption = ConsoleIo.askForOptions(SaveConfigurationOptions.class);
        switch (chosenOption) {
            case SAVE -> {
                saveConfiguration(configuration);
            }
            case SAVE_AND_OPEN -> {
                Path configurationFilePath = saveConfiguration(configuration);
                open(configurationFilePath);
            }
        }
    }

    private static Path saveConfiguration(Configuration configuration) throws IOException {
        String name = ConsoleIo.askForString("Name of configuration", value -> {
            String regex = "^[A-Za-z0-9][A-Za-z0-9 _-]{0,63}$";
            if (!Pattern.matches(regex, value)) {
                return Optional.of("Invalid name. It must have only letters (a to z, either case), digits, spaces, '-', and '_'. it must start with a letter or digit. It must have at most 64 characters.");
            }
            if (Files.exists(getConfigurationDirPath(value))) {
                return Optional.of("Configuration with this name already exists.");
            }
            return Optional.empty();
        });

        Path configurationDirPath = getConfigurationDirPath(name);

        Files.createDirectories(configurationDirPath); // Create parent dirs

        Path configurationFilePath = Files.writeString(configurationDirPath.resolve("config.json"), configuration.toJson());// Copy configuration

        // Copy shell script
        try (InputStream in = ConfigurationStorage.class.getResourceAsStream("/run-configs/run")) {
            if (in == null) {
                throw new IllegalStateException("run script not found in resources.");
            }

            Path scriptPath = configurationDirPath.resolve("run");
            Files.copy(in, scriptPath);
            scriptPath.toFile().setExecutable(true);
        }

        // Copy CMD script
        try (InputStream in = ConfigurationStorage.class.getResourceAsStream("/run-configs/run.cmd")) {
            if (in == null) {
                throw new IllegalStateException("run.cmd script not found in resources.");
            }

            Files.copy(in, configurationDirPath.resolve("run.cmd"));
        }

        return configurationFilePath;
    }

    private static Path getConfigurationDirPath(String name) {
        return Path.of("configs", name);
    }

    private static void open(Path path) throws IOException {
        if (!Desktop.isDesktopSupported()) {
            LOGGER.warning("Desktop is unsupported, cannot open configuration.");
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            LOGGER.warning("Desktop does not support open actions, cannot open configuration.");
            return;
        }

        desktop.open(path.toFile());
    }
}
