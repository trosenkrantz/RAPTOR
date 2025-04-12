package com.github.trosenkrantz.raptor;

import org.junit.jupiter.api.Assertions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public class Raptor extends GenericContainer<Raptor> {
    public Raptor(final Network network) {
        super("raptor:latest");

        withNetwork(network).withCreateContainerCmdModifier(cmd -> cmd.withTty(true));
    }

    @Override
    public Raptor withCommand(String cmd) {
        // The withCommand out of the box does not support quoted arguments, so we parse them ourselves
        return super.withCommand(parseArguments(cmd));
    }

    public static String[] parseArguments(String cliArgs) {
        List<String> argsList = new ArrayList<>();

        String regex = "--[\\w-]+=[^\\s\"]+|\"[^\"]*\"";

        var matcher = Pattern.compile(regex).matcher(cliArgs);
        while (matcher.find()) {
            argsList.add(matcher.group());
        }

        return argsList.toArray(new String[0]);
    }

    /**
     * Assert that this container have output all the given strings on a single output line.
     * This ignores casing.
     *
     * @param expected the expected strings to be found
     */
    public void expectOutputLineContains(String... expected) {
        WaitingConsumer consumer = new WaitingConsumer();
        followOutput(consumer, OutputFrame.OutputType.STDOUT);
        StringBuilder output = new StringBuilder();

        try {
            consumer.waitUntil(frame -> {
                String line = frame.getUtf8String();
                output.append(line);
                String lineLowerCase = line.toLowerCase(Locale.ROOT);
                return Arrays.stream(expected).allMatch(string -> lineLowerCase.contains(string.toLowerCase(Locale.ROOT)));
            }, 4, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assertions.fail("Timeout waiting for expected output. Output:" + System.lineSeparator() + output);
        }
    }

    public String getRaptorHostname() {
        return getContainerInfo().getConfig().getHostName();
    }

    public String getRaptorIpAddress() {
        return getContainerInfo().getNetworkSettings().getNetworks().get(((Network.NetworkImpl) getNetwork()).getName()).getIpAddress();
    }
}
