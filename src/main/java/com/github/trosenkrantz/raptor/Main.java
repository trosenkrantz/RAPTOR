package com.github.trosenkrantz.raptor;

import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.Collection;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            LoggingConfigurator.run();

            Collection<RaptorService> services = RaptorFactory.createServices();
            Configuration configuration = new Configuration(args);

            if (configuration.getString("service").isEmpty()) {
                configure(services, configuration);
            }
            LOGGER.info("Using arguments:" + System.lineSeparator() + configuration);

            String serviceKey = configuration.requireString("service");
            services.stream().filter(service -> service.getParameterKey().equals(serviceKey)).findAny().orElseThrow(() -> new IllegalArgumentException("Service " + serviceKey + " not found.")).run(configuration);

            promptUserToExit();
        } catch (AbortedException ignore) {
            // Exit immediately
        } catch (Throwable e) {
            ConsoleIo.writeException(e);
            promptUserToExit();
        }
    }

    private static void promptUserToExit() {
        ConsoleIo.write(System.lineSeparator() + "Type enter to terminate...");
        ConsoleIo.readLine();
    }

    private static void configure(Collection<RaptorService> services, Configuration configuration) {
        RaptorService raptorService = ConsoleIo.askFor(services.stream().map(service -> new PromptOption<>(service.getPromptValue(), service.getDescription(), service)).toList());

        configuration.setString("service", raptorService.getParameterKey());
        raptorService.configure(configuration);
    }
}