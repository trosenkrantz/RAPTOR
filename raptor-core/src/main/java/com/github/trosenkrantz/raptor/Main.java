package com.github.trosenkrantz.raptor;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.configuration.ReloadableConfiguration;
import com.github.trosenkrantz.raptor.configuration.SaveConfigurationOptions;
import com.github.trosenkrantz.raptor.io.Ansi;
import com.github.trosenkrantz.raptor.configuration.ConfigurationStorage;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.io.LoggingConfigurator;
import com.github.trosenkrantz.raptor.io.RaptorPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Path createdConfigurationDir = null;
        try {
            LoggingConfigurator.initialise();
            Ansi.configure(args);

            Collection<RootService> services = RaptorServiceFactory.createServices();
            Optional<ReloadableConfiguration> loadedConfiguration = ConfigurationStorage.loadConfiguration();

            if (loadedConfiguration.isPresent()) {
                Path configPath = Path.of(ConfigurationStorage.CONFIGURATION_FILE_NAME);
                RaptorPaths.setBase(configPath.toAbsolutePath().normalize().getParent());
                try (ReloadableConfiguration configuration = loadedConfiguration.get()) { // Use configuration from file
                    run(configuration.configuration(), services);
                }
            } else {
                SaveConfigurationOptions chosenOption = ConsoleIo.askForOptions(SaveConfigurationOptions.class);
                Path configurationDir = null;
                if (chosenOption == SaveConfigurationOptions.SAVE_AND_OPEN) {
                    configurationDir = ConfigurationStorage.prepareConfigurationDir(ConfigurationStorage.askForConfigurationName());
                    RaptorPaths.setBase(configurationDir);
                    createdConfigurationDir = configurationDir;
                } else {
                    RaptorPaths.useCurrentDirectory();
                }

                Configuration configuration = configure(services); // Create new configuration

                if (chosenOption == SaveConfigurationOptions.SAVE_AND_OPEN) {
                    Path configurationFilePath = ConfigurationStorage.saveConfiguration(configuration, configurationDir);
                    ConfigurationStorage.open(configurationFilePath);
                } else {
                    LOGGER.info("Running configuration:" + System.lineSeparator() + configuration.toJson());
                    run(configuration, services);
                }
            }

            ConsoleIo.onExit();
        } catch (UserAbortedException ignore) {
            if (createdConfigurationDir != null) {
                try {
                    Files.deleteIfExists(createdConfigurationDir);
                } catch (IOException e) {
                    LOGGER.log(Level.FINE, "Could not remove unused configuration directory.", e);
                }
            }
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Error occurred.", e);
            ConsoleIo.onExit();
        }
    }

    private static Configuration configure(Collection<RootService> services) throws Exception {
        RootService rootService = ConsoleIo.askForOptions(services.stream().map(service -> new PromptOption<>(service.getPromptValue(), service.getDescription(), service)).toList(), true);

        Configuration configuration = Configuration.empty();
        configuration.setRaptorEncodedString("service", rootService.getParameterKey());
        rootService.configure(configuration);

        return configuration;
    }

    private static void run(Configuration configuration, Collection<RootService> services) throws Exception {
        String serviceKey = configuration.requireRaptorEncodedString("service");
        services.stream().filter(service -> service.getParameterKey().equals(serviceKey)).findAny().orElseThrow(() -> new IllegalArgumentException("Service " + serviceKey + " not found.")).run(configuration);
    }
}
