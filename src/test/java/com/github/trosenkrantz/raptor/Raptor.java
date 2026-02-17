package com.github.trosenkrantz.raptor;

import com.github.dockerjava.api.async.ResultCallback;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.Transferable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Raptor extends GenericContainer<Raptor> {
    private static final long TIMEOUT_MS = 8000L;
    private static final long EXPECT_INTERVAL_MS = 10L; // Same as WaitingConsumer

    private final List<String> stdoutLines = new ArrayList<>();
    private final RaptorNetwork network;

    private boolean shouldBeRunning = false;

    public Raptor(final RaptorNetwork network) {
        super("raptor:latest");

        this.network = network;
        this.network.addContainer(this);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withTty(true);
            cmd.withStdinOpen(true);
        });
        withLogConsumer(outputFrame -> { // TODO Can we use getLogs?
            if (outputFrame.getType() == OutputFrame.OutputType.STDOUT) {
                String line = outputFrame.getUtf8String();
                synchronized (this) {
                    stdoutLines.add(line);
                }
            }
        });
    }

    @Override
    public void start() {
        super.start();
        shouldBeRunning = true;
    }

    /**
     * Expects that any output line contains all the expected phrases.
     * This method continues to check the output until the timeout is reached.
     *
     * @param expectedPhrases the phrases to check for
     */
    public void expectAnyOutputLineContains(String... expectedPhrases) {
        List<String> expectedPhrases2 = Arrays.stream(expectedPhrases).map(String::toLowerCase).toList();

        expectOutputLines(actualLines -> actualLines.stream().anyMatch(lineContainsAllPhrases(expectedPhrases2)));
    }

    private static Predicate<String> lineContainsAllPhrases(List<String> expectedPhrases) {
        return actualLine -> expectedPhrases.stream().allMatch(actualLine::contains);
    }

    /**
     * Expects an exact number of output lines each contain all the expected phrases.
     *
     * @param expectedNumber  the expected number of output lines to match
     * @param expectedPhrases the phrases to check for
     */
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
                network.getContainers().stream()
                        .filter(container -> !equals(container))
                        .map(Raptor::getOutput)
                        .map(output -> System.lineSeparator() + "Another RAPTOR's output:" + System.lineSeparator() + output)
                        .collect(Collectors.joining()));
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

    public void setConfiguration(String json) {
        copyFileToContainer(Transferable.of(json), "/app/config.json");
    }

    public Raptor runConfiguration(String json) throws IOException {
        setConfiguration(json);
        writeLineToStdIn("./raptor");

        return this;
    }

    @Override
    public void stop() {
        try {
            if (shouldBeRunning) { // Might be stopped already, in which case we already ran this and cannot access log files anymore
                assertLogFiles();
                shouldBeRunning = false;
            }
        } finally {
            super.stop();
        }
    }

    private void assertLogFiles() {
        String stdout;
        try {
            stdout = execInContainer("ls", "-1", "/app/logs").getStdout().trim();
        } catch (IOException | InterruptedException e) {
            Assertions.fail("Failed listing log files.", e);
            return;
        }
        if (stdout.isEmpty()) Assertions.fail("No log files found.");

        List<String> logFilenames = stdout.lines()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .filter(name -> !name.endsWith(".lck"))
                .toList();

        Assertions.assertFalse(logFilenames.isEmpty(), "No log files not found.");

        for (String fileName : logFilenames) {
            String content = copyFileFromContainer("/app/logs/" + fileName, inputStream -> new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));

            Assertions.assertTrue(content.contains("INFO"), "Log file " + fileName + " has no INFO entry:" + System.lineSeparator() + content);
            Assertions.assertFalse(content.contains("SEVERE"), "Log file " + fileName + " has a SEVERE entry:" + System.lineSeparator() + content);
            Assertions.assertFalse(content.contains("WARNING"), "Log file " + fileName + " has a WARNING entry:" + System.lineSeparator() + content);
        }
    }
}
