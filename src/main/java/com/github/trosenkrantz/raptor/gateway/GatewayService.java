package com.github.trosenkrantz.raptor.gateway;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.PromptOption;
import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.udp.UdpService;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GatewayService implements RootService {
    private static final Logger LOGGER = Logger.getLogger(UdpService.class.getName());

    private static final String PARAMETER_ENDPOINT = "endpoint";
    private static final Collection<EndpointService> ENDPOINT_SERVICES = EndpointServiceFactory.createServices();
    private static final List<PromptOption<EndpointService>> ENDPOINT_SERVICE_OPTIONS = ENDPOINT_SERVICES.stream().map(endpoint -> new PromptOption<>(endpoint.getPromptValue(), endpoint.getDescription(), endpoint)).toList();

    private Endpoint endpointA;
    private Endpoint endpointB;
    private CountDownLatch shouldFinish;

    @Override
    public String getPromptValue() {
        return "g";
    }

    @Override
    public String getParameterKey() {
        return "gateway";
    }

    @Override
    public String getDescription() {
        return "Gateway between two protocols (under development)";
    }

    @Override
    public void configure(Configuration configuration) throws Exception {
        configureEndpoint(configuration, "A");
        configureEndpoint(configuration, "B");
    }

    private static void configureEndpoint(Configuration rootConfiguration, String endpointName) throws Exception {
        ConsoleIo.writeLine("Select endpoint " + endpointName + ".");

        EndpointService service = ConsoleIo.askForOptions(ENDPOINT_SERVICE_OPTIONS);
        Configuration endpointConfiguration = new Configuration();
        endpointConfiguration.setString(PARAMETER_ENDPOINT, service.getParameterKey());
        service.configureEndpoint(endpointConfiguration);

        rootConfiguration.addWithPrefix(endpointName.toLowerCase(Locale.ROOT), endpointConfiguration);
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        shouldFinish = new CountDownLatch(1);

        // As endpoint A might produce data before endpoint B is ready, we need to buffer the data
        DelayedConsumer<byte[]> fromAConsumer = new DelayedConsumer<>();
        DelayedConsumer<byte[]> fromBConsumer = new DelayedConsumer<>();

        endpointA = createEndpoint(configuration.extractWithPrefix("a"), fromAConsumer);
        endpointB = createEndpoint(configuration.extractWithPrefix("b"), fromBConsumer);

        // Now that both endpoints are created, we can start processing the data, flushing the buffers
        fromAConsumer.setDelegate(this::acceptFromEndpointA);
        fromBConsumer.setDelegate(this::acceptFromEndpointB);

        shouldFinish.await();
    }

    private Endpoint createEndpoint(Configuration endpointConfiguration, Consumer<byte[]> consumer) throws IOException {
        String endpointKey = endpointConfiguration.requireString(PARAMETER_ENDPOINT);
        EndpointService configuredEndpointService = ENDPOINT_SERVICES.stream().filter(service -> service.getParameterKey().equals(endpointKey)).findAny().orElseThrow(() -> new IllegalArgumentException("Service " + endpointKey + " not found."));
        return configuredEndpointService.createEndpoint(endpointConfiguration, consumer, () -> shouldFinish.countDown());
    }

    private void acceptFromEndpointA(byte[] data) {
        try {
            endpointB.sendToExternalSystem(data);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to process data.", e);
            shouldFinish.countDown();
        }
    }

    private void acceptFromEndpointB(byte[] data) {
        try {
            endpointA.sendToExternalSystem(data);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to process data.", e);
            shouldFinish.countDown();
        }
    }
}
