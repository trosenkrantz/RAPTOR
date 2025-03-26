package com.github.trosenkrantz.raptor.gateway;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.PromptOption;
import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class GatewayService implements RootService {
    public static final String PARAMETER_ENDPOINT = "endpoint";
    public static final Collection<EndpointService> ENDPOINT_SERVICES = EndpointServiceFactory.createServices();
    public static final List<PromptOption<EndpointService>> ENDPOINT_SERVICE_OPTIONS = ENDPOINT_SERVICES.stream().map(endpoint -> new PromptOption<>(endpoint.getPromptValue(), endpoint.getDescription(), endpoint)).toList();
    private Endpoint endpointA;
    private Endpoint endpointB;

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
        service.configure(endpointConfiguration);

        rootConfiguration.addWithPrefix(endpointName.toLowerCase(Locale.ROOT), endpointConfiguration);
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        // As endpoint A might produce data before endpoint B is ready, we need to buffer the data
        DelayedConsumer<byte[]> fromAConsumer = new DelayedConsumer<>();
        DelayedConsumer<byte[]> fromBConsumer = new DelayedConsumer<>();

        endpointA = createEndpoint(configuration.extractWithPrefix("a"), fromAConsumer);
        endpointB = createEndpoint(configuration.extractWithPrefix("b"), fromBConsumer);

        // Now that both endpoints are created, we can start processing the data, flushing the buffer
        fromAConsumer.setDelegate(this::acceptFromEndpointA);
        fromBConsumer.setDelegate(this::acceptFromEndpointB);
    }

    private static Endpoint createEndpoint(Configuration endpointConfiguration, Consumer<byte[]> consumer) throws Exception {
        String endpointAKey = endpointConfiguration.requireString(PARAMETER_ENDPOINT);
        return ENDPOINT_SERVICES.stream().filter(service -> service.getParameterKey().equals(endpointAKey)).findAny().orElseThrow(() -> new IllegalArgumentException("Service " + endpointAKey + " not found.")).createEndpoint(endpointConfiguration, consumer);
    }

    private void acceptFromEndpointA(byte[] data) {
        endpointB.accept(data);
    }

    private void acceptFromEndpointB(byte[] data) {
        endpointA.accept(data);
    }
}
