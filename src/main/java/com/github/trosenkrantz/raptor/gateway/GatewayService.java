package com.github.trosenkrantz.raptor.gateway;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.PromptOption;
import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.gateway.network.impairment.*;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class GatewayService implements RootService {
    private static final String PARAMETER_ENDPOINT = "endpoint";
    private static final Collection<EndpointService> ENDPOINT_SERVICES = EndpointServiceFactory.createServices();
    private static final List<PromptOption<EndpointService>> ENDPOINT_SERVICE_OPTIONS = ENDPOINT_SERVICES.stream().map(endpoint -> new PromptOption<>(endpoint.getPromptValue(), endpoint.getDescription(), endpoint)).toList();

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
        return "Gateway between two systems";
    }

    @Override
    public void configure(Configuration configuration) throws Exception {
        configureEndpoint(configuration, "A");
        configureEndpoint(configuration, "B");

        configureNetworkImpairment(configuration, "A to B");
        configureNetworkImpairment(configuration, "B to A");
    }

    private static void configureEndpoint(Configuration rootConfiguration, String endpointName) {
        ConsoleIo.writeLine("---- Configuring endpoint " + endpointName + " ----");

        EndpointService service = ConsoleIo.askForOptions(ENDPOINT_SERVICE_OPTIONS, false);
        Configuration endpointConfiguration = Configuration.empty();
        endpointConfiguration.setString(PARAMETER_ENDPOINT, service.getParameterKey());
        service.configureEndpoint(endpointConfiguration);

        rootConfiguration.setSubConfiguration(endpointName.toLowerCase(Locale.ROOT), endpointConfiguration);
    }

    private static void configureNetworkImpairment(Configuration rootConfiguration, String direction) {
        ConsoleIo.writeLine("---- Configuring network impairment " + direction + " ----");

        Configuration directionConfiguration = Configuration.empty();
        ConsoleIo.configureAdvancedSettings("Configure network impairment", List.of(LatencyFactory.SETTING, CorruptionFactory.SETTING, PacketLossFactory.SETTING, DuplicationFactory.SETTING), directionConfiguration);

        rootConfiguration.setSubConfiguration(direction.toLowerCase(Locale.ROOT).replaceAll(" ", "-"), directionConfiguration);
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        shouldFinish = new CountDownLatch(1);

        // As endpoint A might produce data before endpoint B is ready, we need to buffer the data
        DelayedConsumer<byte[]> fromAConsumer = new DelayedConsumer<>();
        DelayedConsumer<byte[]> fromBConsumer = new DelayedConsumer<>();

        Endpoint endpointA = createEndpoint(configuration.requireSubConfiguration("a"), fromAConsumer);
        Endpoint endpointB = createEndpoint(configuration.requireSubConfiguration("b"), fromBConsumer);

        Consumer<byte[]> impairmentAToB = createNetworkImpairment(configuration.getSubConfiguration("a-to-b").orElse(Configuration.empty()), endpointB);
        Consumer<byte[]> impairmentBToA = createNetworkImpairment(configuration.getSubConfiguration("b-to-a").orElse(Configuration.empty()), endpointA);

        // Now that endpoints and impairments are created, we can start processing the data, flushing the buffers
        fromAConsumer.setDelegate(impairmentAToB); // When receiving data from A, pass to impairment a-to-b
        fromBConsumer.setDelegate(impairmentBToA); // When receiving data from B, pass to impairment b-to-a

        Thread.ofVirtual().start(() -> {
            ConsoleIo.promptUserToExit();
            shouldFinish.countDown();
        });

        shouldFinish.await();
    }

    private Endpoint createEndpoint(Configuration endpointConfiguration, Consumer<byte[]> consumer) throws IOException {
        String endpointKey = endpointConfiguration.requireString(PARAMETER_ENDPOINT);
        EndpointService configuredEndpointService = ENDPOINT_SERVICES.stream().filter(service -> service.getParameterKey().equals(endpointKey)).findAny().orElseThrow(() -> new IllegalArgumentException("Service " + endpointKey + " not found."));
        return configuredEndpointService.createEndpoint(endpointConfiguration, consumer, () -> shouldFinish.countDown());
    }

    private Consumer<byte[]> createNetworkImpairment(Configuration impairmentConfiguration, Endpoint toEndpoint) {
        List<NetworkImpairmentFactory> factories = new ArrayList<>();

        LatencyFactory.SETTING.read(impairmentConfiguration).ifPresent(latency -> factories.add(new LatencyFactory(latency)));
        CorruptionFactory.SETTING.read(impairmentConfiguration).ifPresent(corruption -> factories.add(new CorruptionFactory(corruption)));
        PacketLossFactory.SETTING.read(impairmentConfiguration).ifPresent(packetLoss -> factories.add(new PacketLossFactory(packetLoss)));
        DuplicationFactory.SETTING.read(impairmentConfiguration).ifPresent(duplication -> factories.add(new DuplicationFactory(duplication)));

        // Each factory needs the next consumer to pass the data to, so combine them in reverse order
        Consumer<byte[]> result = toEndpoint::sendToExternalSystem;
        for (int i = factories.size() - 1; i >= 0; i--) {
            result = factories.get(i).create(result);
        }
        return result;
    }
}
