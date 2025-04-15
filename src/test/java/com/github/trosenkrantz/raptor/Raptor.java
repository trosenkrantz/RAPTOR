package com.github.trosenkrantz.raptor;

import com.github.dockerjava.api.async.ResultCallback;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Raptor extends GenericContainer<Raptor> {
    private static final long TIMEOUT_MS = 4000L;
    private static final long EXPECT_INTERVAL_MS = 10L; // Same as WaitingConsumer

    private final List<String> stdoutLines = new ArrayList<>();
    private final RaptorNetwork network;

    public Raptor(final RaptorNetwork network) {
        super("raptor:latest");

        this.network = network;
        this.network.addContainer(this);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withTty(true);
            cmd.withStdinOpen(true);
        });
        withLogConsumer(outputFrame -> {
            if (outputFrame.getType() == OutputFrame.OutputType.STDOUT) {
                String line = outputFrame.getUtf8String();
                synchronized (this) {
                    stdoutLines.add(line);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * <p>
     * This support multiple arguments in {@code cmd} string for easy copy paste.
     * This supports quoted arguments.
     *
     * @param cmd the commands
     * @return this
     */
    @Override
    public Raptor withCommand(String cmd) {
        // The withCommand out of the box does not support quoted arguments, so we parse them ourselves
        return super.withCommand(parseArguments(cmd));
    }

    public ExecResult execInContainer(String command) throws UnsupportedOperationException, IOException, InterruptedException {
        return super.execInContainer(parseArguments(command));
    }

    public static String[] parseArguments(String cliArgs) {
        List<String> argsList = new ArrayList<>();

        String regex = "\"([^\"]*)\"|(\\S+)";

        var matcher = Pattern.compile(regex).matcher(cliArgs);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                argsList.add(matcher.group(1)); // Double-quoted content without the quotes
            } else {
                argsList.add(matcher.group(2)); // Unquoted argument
            }
        }

        return argsList.toArray(new String[0]);
    }

    public void expectAnyOutputLineContains(String... expectedPhrases) {
        List<String> expectedPhrases2 = Arrays.stream(expectedPhrases).map(String::toLowerCase).toList();

        expectOutputLines(actualLines -> actualLines.stream().anyMatch(lineContainsAllPhrases(expectedPhrases2)));
    }

    private static Predicate<String> lineContainsAllPhrases(List<String> expectedPhrases) {
        return actualLine -> expectedPhrases.stream().allMatch(actualLine::contains);
    }

    public void expectNumberOfOutputLineContains(int expectedNumber, String... expectedPhrases) {
        List<String> expectedPhrases2 = Arrays.stream(expectedPhrases).map(String::toLowerCase).toList();

        expectOutputLines(actualLines -> {
            long linesMatch = actualLines.stream().filter(lineContainsAllPhrases(expectedPhrases2)).count();
            return linesMatch == expectedNumber;
        });
    }

    public void expectOutputLines(Predicate<List<String>> predicate) {
        List<String> capturedLines;

        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        do {
            synchronized (this) {
                capturedLines = new ArrayList<>(stdoutLines);
            }
            List<String> actualLines = capturedLines.stream().map(String::toLowerCase).toList();

            if (predicate.test(actualLines)) {
                return; // Success
            }

            try {
                Thread.sleep(EXPECT_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (System.currentTimeMillis() < deadline);


        Assertions.fail("Timeout waiting for expected outputs. Output:" + System.lineSeparator() +
                String.join("", capturedLines) + System.lineSeparator() +
                "Other RAPTORs output:" + System.lineSeparator() +
                String.join(System.lineSeparator(), network.getContainers().stream().filter(container -> !equals(container)).map(Raptor::getOutput).toList()));
    }

    public synchronized String getOutput() {
        return String.join("", stdoutLines);
    }

    public String getRaptorHostname() {
        return getContainerInfo().getConfig().getHostName();
    }

    public String getRaptorIpAddress() {
        return getContainerInfo().getNetworkSettings().getNetworks().get(((Network.NetworkImpl) getNetwork()).getName()).getIpAddress();
    }

    public void writeLineToStdIn(String message) throws IOException {
        try (PipedOutputStream out = new PipedOutputStream()) {
            PipedInputStream in = new PipedInputStream(out);

            DockerClientFactory.instance().client().attachContainerCmd(getContainerId())
                    .withFollowStream(true)
                    .withStdIn(in)
                    .exec(new ResultCallback.Adapter<>() {
                    });

            out.write((message + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    public Raptor runRaptor(String arguments) throws IOException {
        writeLineToStdIn("/app/raptor " + arguments);
        return this;
    }
}
