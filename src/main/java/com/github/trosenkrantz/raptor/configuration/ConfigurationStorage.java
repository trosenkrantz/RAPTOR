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
    public static final String CONFIGURATION_FILE_NAME = "config.json";

    private static final Logger LOGGER = Logger.getLogger(ConfigurationStorage.class.getName());

    public static Path saveConfiguration(Configuration configuration) throws IOException {
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

        Path configurationFilePath = Files.writeString(configurationDirPath.resolve(CONFIGURATION_FILE_NAME), configuration.toJson());
        ConsoleIo.writeLine("Saved configuration to: " + configurationFilePath.toAbsolutePath());

        // Copy shell script
        try (InputStream in = ConfigurationStorage.class.getResourceAsStream("/com/github/trosenkrantz/raptor/run")) {
            if (in == null) {
                throw new IllegalStateException("run script not found in resources.");
            }

            Path scriptPath = configurationDirPath.resolve("run");
            Files.copy(in, scriptPath);
            scriptPath.toFile().setExecutable(true);
        }

        // Copy CMD script
        try (InputStream in = ConfigurationStorage.class.getResourceAsStream("/com/github/trosenkrantz/raptor/run.cmd")) {
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

    public static void open(Path path) throws IOException {
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

    public static Optional<ReloadableConfiguration> loadConfiguration() throws IOException {
        Path path = Path.of(CONFIGURATION_FILE_NAME);
        if (!Files.exists(path)) return Optional.empty();

        FileWatcher fileWatcher = new FileWatcher(path);
        Configuration configuration = Configuration.fromSavedFile(path, fileWatcher);

        return Optional.of(new ReloadableConfiguration(configuration, fileWatcher));
    }
}
