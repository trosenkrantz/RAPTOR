package com.github.trosenkrantz.raptor.io;

import com.github.trosenkrantz.raptor.AbortedException;
import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.PromptOption;

import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConsoleIo {
    private static final Console console = System.console();

    private static boolean haveHadUserInteraction = false;

    public static void write(String message) {
        console.printf(message);
    }

    public static void writeLine() {
        write(System.lineSeparator());
    }

    public static void writeLine(String message) {
        write(message + System.lineSeparator());
    }

    public static void writeException(Throwable e) {
        e.printStackTrace(console.writer());
    }

    public static String readLine() {
        haveHadUserInteraction = true;
        String result = console.readLine();
        writeLine();
        return result;
    }

    private static <T extends Enum<T> & PromptEnum> List<PromptOption<T>> getPromptOptions(Class<T> enumClass) {
        return Stream.of(enumClass.getEnumConstants())
                .map(value -> new PromptOption<>(value.getPromptValue(), value.getDescription(), value))
                .collect(Collectors.toList());
    }

    public static <T extends Enum<T> & PromptEnum> T askForOptions(Class<T> enumClass) {
        return askForOptions(getPromptOptions(enumClass), null);
    }

    public static <T extends Enum<T> & PromptEnum> T askForOptions(Class<T> enumClass, T defaultValue) {
        return askForOptions(getPromptOptions(enumClass), new PromptOption<>(defaultValue.getPromptValue(), defaultValue.getDescription(), defaultValue));
    }

    public static <T> T askForOptions(List<PromptOption<T>> options) {
        return askForOptions(options, null);
    }

    public static <T> T askForOptions(List<PromptOption<T>> options, PromptOption<T> defaultValue) {
        while (true) {
            writeLine(
                    "Choose between" +
                            (defaultValue == null ? "" : " (default " + defaultValue.promptValue() + ")") +
                            " or (e) exit:" + System.lineSeparator() +
                            options.stream().map(option -> option.promptValue() + " - " + option.description()).collect(Collectors.joining(System.lineSeparator()))
            );

            String answer = readLine();
            if (answer.equals("e")) throw new AbortedException();
            if (defaultValue != null && answer.isEmpty()) return defaultValue.value();

            Optional<PromptOption<T>> result = options.stream().filter(option -> option.promptValue().equalsIgnoreCase(answer)).findAny();
            if (result.isPresent()) {
                return result.get().value();
            } else {
                writeLine("Unrecognised answer.");
            }
        }
    }

    public static int askForInt(String description, int defaultValue) {
        while (true) {
            write(description + " (default " + defaultValue + ") or (e) exit: ");

            String answer = readLine();
            if (answer.isEmpty()) return defaultValue;
            if (answer.equals("e")) throw new AbortedException();

            if (answer.matches("\\d+")) {
                return Integer.parseInt(answer);
            } else {
                writeLine("Answer must be an integer.");
            }
        }
    }

    public static String askForString(String description, String defaultValue) {
        return askForString(description, defaultValue, s -> Optional.empty());
    }

    public static String askForString(String description, Function<String, Optional<String>> validator) {
        return askForString(description, null, validator);
    }

    public static String askForString(String description, String defaultValue, Function<String, Optional<String>> validator) {
        while (true) {
            write(description + (defaultValue == null ? "" : " (default " + defaultValue + ")") + " or (e) exit: ");

            String answer = readLine();
            if (answer.equals("e")) throw new AbortedException();
            if (defaultValue != null && answer.isEmpty()) return defaultValue;

            Optional<String> error = validator.apply(answer);
            if (error.isEmpty()) {
                return answer;
            } else {
                writeLine(error.get());
            }
        }
    }

    public static String askForFile(String description) {
        return askForFile(description, null);
    }

    public static String askForFile(String description, String defaultPath) {
        while (true) {
            write(description + (defaultPath == null ? "" : " (default " + defaultPath + ")") + " or (e) exit: ");

            String answer = readLine();
            if (answer.equals("e")) throw new AbortedException();
            if (defaultPath != null && answer.isEmpty()) answer = defaultPath;

            Path path = Path.of(answer);
            if (Files.exists(path)) {
                return answer;
            } else {
                writeLine("Cannot find " + path + ".");
            }
        }
    }

    public static void promptUserToExit() {
        if (haveHadUserInteraction) { // Skip prompt if running as CLI
            write(System.lineSeparator() + "Type enter to terminate...");
            readLine();
        }
    }
}
