package com.github.trosenkrantz.raptor;

import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.io.LoggingConfigurator;

import java.util.Collection;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            LoggingConfigurator.initialise();

            Collection<RootService> services = RaptorServiceFactory.createServices();
            Configuration configuration = new Configuration(args);

            if (configuration.getString("service").isEmpty()) {
                configure(services, configuration);
            }
            LOGGER.info("Using arguments:" + System.lineSeparator() + configuration);

            String serviceKey = configuration.requireString("service");
            services.stream().filter(service -> service.getParameterKey().equals(serviceKey)).findAny().orElseThrow(() -> new IllegalArgumentException("Service " + serviceKey + " not found.")).run(configuration);

            ConsoleIo.onExit();
        } catch (AbortedException ignore) {
            // Exit immediately
        } catch (Throwable e) {
            ConsoleIo.writeException(e);
            ConsoleIo.onExit();
        }
    }

    private static void configure(Collection<RootService> services, Configuration configuration) throws Exception {
        RootService rootService = ConsoleIo.askForOptions(services.stream().map(service -> new PromptOption<>(service.getPromptValue(), service.getDescription(), service)).toList());

        configuration.setString("service", rootService.getParameterKey());
        rootService.configure(configuration);
    }
}