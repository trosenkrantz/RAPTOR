package com.github.trosenkrantz.raptor;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.configuration.ReloadableConfiguration;
import com.github.trosenkrantz.raptor.configuration.SaveConfigurationOptions;
import com.github.trosenkrantz.raptor.io.Ansi;
import com.github.trosenkrantz.raptor.configuration.ConfigurationStorage;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.io.LoggingConfigurator;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            LoggingConfigurator.initialise();

            Collection<RootService> services = RaptorServiceFactory.createServices();
            Optional<ReloadableConfiguration> configurationFromSavedFile = ConfigurationStorage.loadConfiguration();
            Ansi.configure(args);

            if (configurationFromSavedFile.isPresent()) {
                try (ReloadableConfiguration configuration = configurationFromSavedFile.get()) { // Use configuration from file
                    run(configuration.configuration(), services);
                }
            } else {
                Configuration configuration = configure(services); // Create new configuration

                SaveConfigurationOptions chosenOption = ConsoleIo.askForOptions(SaveConfigurationOptions.class);
                switch (chosenOption) {
                    case RUN -> {
                        LOGGER.info("Running configuration:" + System.lineSeparator() + configuration.toJson());
                        run(configuration, services);
                    }
                    case SAVE_AND_OPEN -> {
                        Path configurationFilePath = ConfigurationStorage.saveConfiguration(configuration);
                        ConfigurationStorage.open(configurationFilePath);
                    }
                }
            }

            ConsoleIo.onExit();
        } catch (UserAbortedException ignore) {
            // Exit immediately
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