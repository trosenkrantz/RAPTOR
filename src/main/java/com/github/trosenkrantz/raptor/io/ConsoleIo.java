package com.github.trosenkrantz.raptor.io;

import com.github.trosenkrantz.raptor.AbortedException;
import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.PromptOption;
import com.github.trosenkrantz.raptor.configuration.Setting;
import com.github.trosenkrantz.raptor.configuration.SettingInstance;

import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    private static void write(String message, Ansi colour) {
        write(colour.apply(message));
    }

    public static void writeLine() {
        write(System.lineSeparator());
    }

    public static void writeLine(String message) {
        write(message + System.lineSeparator());
    }

    public static void writeLine(String message, Ansi colour) {
        write(message + System.lineSeparator(), colour);
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
            List<List<String>> rows = options.stream().map(option -> List.of(Ansi.PROMPT.apply(option.promptValue()), option.description())).collect(Collectors.toList());
            rows.add(List.of(Ansi.PROMPT.apply("e"), Ansi.EXIT.apply("Exit")));

            String defaultString = defaultValue == null ? "" : " (default " + defaultValue.promptValue() + ")";
            writeLine("Choose between" + defaultString + ":" + System.lineSeparator() + TableFormatter.format(rows));

            String answer = readLine();
            if (answer.equals("e")) throw new AbortedException();
            if (defaultValue != null && answer.isEmpty()) return defaultValue.value();

            Optional<PromptOption<T>> result = options.stream().filter(option -> option.promptValue().equalsIgnoreCase(answer)).findAny();
            if (result.isPresent()) {
                return result.get().value();
            } else {
                writeLine("Unrecognised answer.", Ansi.ERROR);
            }
        }
    }

    public static int askForInt(String description) {
        return askForInt(description, v -> Optional.empty());
    }

    public static int askForInt(String description, int defaultValue) {
        return askForInt(description, defaultValue, v -> Optional.empty());
    }

    public static int askForInt(String description, int defaultValue, Function<Integer, Optional<String>> validator) {
        return askForOptionalInt(description, String.valueOf(defaultValue), validator).orElse(defaultValue);
    }

    public static int askForInt(String description, Function<Integer, Optional<String>> validator) {
        while (true) {
            write(description + " or type " + Ansi.PROMPT.apply("e") + " to exit: ");

            String answer = readLine();
            if (answer.equals("e")) throw new AbortedException();

            if (!answer.matches("^-?\\d+$")) {
                writeLine("Answer must be an integer.", Ansi.ERROR);
                continue;
            }
            int intAnswer = Integer.parseInt(answer);

            Optional<String> error = validator.apply(intAnswer);
            if (error.isPresent()) {
                writeLine(error.get());
            } else {
                return intAnswer;
            }
        }
    }

    public static Optional<Integer> askForOptionalInt(String description, String defaultDescription, Function<Integer, Optional<String>> validator) {
        while (true) {
            write(description + (defaultDescription == null ? "" : " (default " + defaultDescription + ")") + " or type " + Ansi.PROMPT.apply("e") + " to exit: ");

            String answer = readLine();
            if (answer.isEmpty()) return Optional.empty();
            if (answer.equals("e")) throw new AbortedException();

            if (!answer.matches("^-?\\d+$")) {
                writeLine("Answer must be an integer.", Ansi.ERROR);
                continue;
            }
            int intAnswer = Integer.parseInt(answer);

            Optional<String> error = validator.apply(intAnswer);
            if (error.isPresent()) {
                writeLine(error.get());
            } else {
                return Optional.of(intAnswer);
            }
        }
    }

    public static String askForString(String description, String defaultValue) {
        return askForString(description, defaultValue, v -> Optional.empty());
    }

    public static String askForString(String description, Function<String, Optional<String>> validator) {
        return askForString(description, null, validator);
    }

    public static String askForString(String description, String defaultValue, Function<String, Optional<String>> validator) {
        while (true) {
            write(description + (defaultValue == null ? "" : " (default " + defaultValue + ")") + " or type " + Ansi.PROMPT.apply("e") + " exit: ");

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
            write(description + (defaultPath == null ? "" : " (default " + defaultPath + ")") + " or type " + Ansi.PROMPT.apply("e") + " to exit: ");

            String answer = readLine();
            if (answer.equals("e")) throw new AbortedException();
            if (defaultPath != null && answer.isEmpty()) answer = defaultPath;

            Path path = Path.of(answer);
            if (Files.exists(path)) {
                return answer;
            } else {
                writeLine("Cannot find " + path + ".", Ansi.ERROR);
            }
        }
    }

    public static void configureAdvancedSettings(String description, List<Setting<?>> settings, Configuration configuration) {
        // Instantiate all settings
        List<SettingInstance<?>> settingInstances = new ArrayList<>();
        settings.forEach(setting -> settingInstances.add(setting.instantiateDefault()));

        while (true) {
            List<List<String>> rows = settingInstances.stream().map(instance -> List.of(
                    Ansi.PROMPT.apply(instance.getSetting().getPromptValue()),
                    instance.getSetting().getDescription(),
                    instance.toString()
            )).toList();
            writeLine(description + " or type " + Ansi.PROMPT.apply("enter") + " to continue or " + Ansi.PROMPT.apply("e") + " to exit: " + System.lineSeparator() + TableFormatter.format(rows));

            String answer = readLine();
            if (answer.isEmpty()) return; // User chosen to continue
            if (answer.equals("e")) throw new AbortedException();

            Optional<SettingInstance<?>> result = settingInstances.stream().filter(instance -> instance.getSetting().getPromptValue().equalsIgnoreCase(answer)).findAny();
            if (result.isPresent()) {
                result.get().configure(configuration); // And do not return to allow for additional settings afterwards
            } else {
                writeLine("Unrecognised answer.", Ansi.ERROR);
            }
        }
    }

    public static void onExit() {
        if (haveHadUserInteraction) { // Skip prompt if running as CLI
            promptUserToExit();
        }
    }

    public static void promptUserToExit() {
        write(System.lineSeparator() + "Type " + Ansi.PROMPT.apply("enter") + " to terminate...");
        ConsoleIo.readLine();

        haveHadUserInteraction = false; // Reset to avoid double prompt
    }
}
