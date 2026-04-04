package com.github.trosenkrantz.raptor.conversion;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.CommandSubstitutor;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

public class ConverterService implements RootService {
    private static final Logger LOGGER = Logger.getLogger(ConverterService.class.getName());
    private static final String PARAMETER_PATH = "path";

    @Override
    public String getPromptValue() {
        return "c";
    }

    @Override
    public String getParameterKey() {
        return "convert";
    }

    @Override
    public String getDescription() {
        return "Convert between RAPTOR encoding and files";
    }

    @Override
    public void configure(Configuration configuration) throws IOException {
        Action action = ConsoleIo.askForOptions(Action.class);
        configuration.setEnum(action);

        String path = switch (action) {
            case ENCODING -> {
                CommandSubstitutor.TIMEOUT_SETTING.configure(configuration);
                yield ConsoleIo.askForString("Absolute or relative path of file to write to", "." + File.separator + "file");
            }
            case FILE -> {
                String result = ConsoleIo.askForFile("Absolute or relative path of file to convert", "." + File.separator + "file");

                // Read file immediately to provide early feedback
                ConsoleIo.writeLine("Read file with " + Files.readAllBytes(Paths.get(result)).length + " bytes.");
                yield result;
            }
        };

        configuration.setRaptorEncodedString(PARAMETER_PATH, path);
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        switch (configuration.requireEnum(Action.class)) {
            case ENCODING -> {
                String encoding = ConsoleIo.askForString("RAPTOR encoding to convert to file", "\\\\x00Hello\\n");

                Path path = Path.of(configuration.requireRaptorEncodedString(PARAMETER_PATH));
                Files.createDirectories(path.getParent());
                byte[] bytes = BytesFormatter.raptorEncodingToBytes(encoding, CommandSubstitutor.TIMEOUT_SETTING.readAndRequireOrDefault(configuration));
                Files.write(
                        path,
                        bytes,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );

                LOGGER.info("Wrote " + bytes.length + (bytes.length == 1 ? " byte" : " bytes") + " to " + path + ".");
            }
            case FILE -> {
                LOGGER.info("In RAPTOR encoding:\n" + BytesFormatter.bytesToRaptorEncoding(Files.readAllBytes(Paths.get(configuration.requireRaptorEncodedString(PARAMETER_PATH)))));
            }
        }
    }
}
