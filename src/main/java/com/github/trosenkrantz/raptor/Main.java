package com.github.trosenkrantz.raptor;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.Ansi;
import com.github.trosenkrantz.raptor.configuration.ConfigurationStorage;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.io.LoggingConfigurator;

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
            Optional<Configuration> configurationFromCliArguments = Configuration.fromSavedFile();
            Ansi.configure(args);

            Configuration configuration;
            if (configurationFromCliArguments.isPresent()) {
                configuration = configurationFromCliArguments.get(); // Use configuration from arguments
            } else {
                configuration = configure(services); // Create new configuration
                LOGGER.info("Using configuration:" + System.lineSeparator() + configuration.toJson());
            }

            // Run
            String serviceKey = configuration.requireString("service");
            services.stream().filter(service -> service.getParameterKey().equals(serviceKey)).findAny().orElseThrow(() -> new IllegalArgumentException("Service " + serviceKey + " not found.")).run(configuration);

            // Offer saving configuration if new
            if (configurationFromCliArguments.isEmpty()) {
                ConfigurationStorage.offerSaveConfiguration(configuration);
            }

            ConsoleIo.onExit();
        } catch (AbortedException ignore) {
            // Exit immediately
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Error occurred.", e);
            ConsoleIo.onExit();
        }
    }

    private static Configuration configure(Collection<RootService> services) throws Exception {
        RootService rootService = ConsoleIo.askForOptions(services.stream().map(service -> new PromptOption<>(service.getPromptValue(), service.getDescription(), service)).toList(), true);

        Configuration configuration = Configuration.empty();
        configuration.setString("service", rootService.getParameterKey());
        rootService.configure(configuration);

        return configuration;
    }
}