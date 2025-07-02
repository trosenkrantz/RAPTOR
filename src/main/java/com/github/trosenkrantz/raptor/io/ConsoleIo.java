package com.github.trosenkrantz.raptor.io;

import com.github.trosenkrantz.raptor.AbortedException;
import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.PromptEnum;
import com.github.trosenkrantz.raptor.PromptOption;
import com.github.trosenkrantz.raptor.configuration.Setting;

import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConsoleIo {
    private static final Console console = System.console();

    private static boolean shouldPromptUserBeforeExit = false;

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
        shouldPromptUserBeforeExit = true;
        String result = console.readLine();
        writeLine();
        return result;
    }


    /* Enum */

    private static <T extends Enum<T> & PromptEnum> List<PromptOption<T>> getPromptOptions(Class<T> enumClass) {
        return Stream.of(enumClass.getEnumConstants())
                .map(value -> new PromptOption<>(value.getPromptValue(), value.getDescription(), value))
                .collect(Collectors.toList());
    }

    public static <T extends Enum<T> & PromptEnum> T askForOptions(Class<T> enumClass) {
        return askForOptions(getPromptOptions(enumClass), null, false);
    }

    public static <T extends Enum<T> & PromptEnum> T askForOptions(Class<T> enumClass, T defaultValue) {
        return askForOptions(getPromptOptions(enumClass), new PromptOption<>(defaultValue.getPromptValue(), defaultValue.getDescription(), defaultValue), false);
    }

    public static <T> T askForOptions(List<PromptOption<T>> options, boolean showAbout) {
        return askForOptions(options, null, showAbout);
    }

    public static <T> T askForOptions(List<PromptOption<T>> options, PromptOption<T> defaultValue, boolean showAbout) {
        while (true) {
            List<List<String>> rows = options.stream().map(option -> List.of(Ansi.PROMPT.apply(option.promptValue()), option.description())).collect(Collectors.toList());
            if (showAbout) rows.add(List.of(Ansi.PROMPT.apply("a"), Ansi.LESS_IMPORTANT.apply("About")));
            rows.add(List.of(Ansi.PROMPT.apply("e"), Ansi.LESS_IMPORTANT.apply("Exit")));

            String defaultString = defaultValue == null ? "" : " (default " + defaultValue.promptValue() + ")";
            writeLine("Choose between" + defaultString + ":" + System.lineSeparator() + TableFormatter.format(rows));

            String answer = readLine();
            if (showAbout && answer.equals("a")) {
                AboutWriter.write();
                continue;
            }
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


    /* Int */

    public static int askForInt(String description) {
        return askForInt(description, v -> Optional.empty());
    }

    public static int askForInt(String description, int defaultValue) {
        return askForInt(description, defaultValue, v -> Optional.empty());
    }

    public static int askForInt(String description, int defaultValue, Validator<Integer> validator) {
        return askForOptionalInt(description, String.valueOf(defaultValue), validator).orElse(defaultValue);
    }

    public static int askForInt(String description, Validator<Integer> validator) {
        while (true) {
            write(description + " or type " + Ansi.PROMPT.apply("e") + " to exit: ");

            String answer = readLine();
            if (answer.equals("e")) throw new AbortedException();

            if (!answer.matches("^-?\\d+$")) {
                writeLine("Answer must be an integer.", Ansi.ERROR);
                continue;
            }
            int intAnswer = Integer.parseInt(answer);

            Optional<String> error = validator.validate(intAnswer);
            if (error.isPresent()) {
                writeLine(error.get(), Ansi.ERROR);
            } else {
                return intAnswer;
            }
        }
    }

    public static Optional<Integer> askForOptionalInt(String description, String defaultDescription, Validator<Integer> validator) {
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

            Optional<String> error = validator.validate(intAnswer);
            if (error.isPresent()) {
                writeLine(error.get());
            } else {
                return Optional.of(intAnswer);
            }
        }
    }


    /* Double */

    public static Double askForDouble(String description, Validator<Double> validator) {
        return askForDouble(description, null, validator);
    }

    public static Double askForDouble(String description, Double defaultValue, Validator<Double> validator) {
        while (true) {
            write(description + (defaultValue == null ? "" : " (default " + defaultValue + ")") + " or type " + Ansi.PROMPT.apply("e") + " exit: ");

            String answer = readLine();
            if (answer.equals("e")) throw new AbortedException();
            if (defaultValue != null && answer.isEmpty()) return defaultValue;

            if (!answer.matches("^-?\\d+(\\.\\d+)?$")) {
                writeLine("Answer must be a number, potentially with a decimal separator.", Ansi.ERROR);
                continue;
            }
            double doubleAnswer = Double.parseDouble(answer);

            Optional<String> error = validator.validate(doubleAnswer);
            if (error.isEmpty()) {
                return doubleAnswer;
            } else {
                writeLine(error.get(), Ansi.ERROR);
            }
        }
    }


    /* String */

    public static String askForString(String description, String defaultValue) {
        return askForString(description, defaultValue, v -> Optional.empty());
    }

    public static String askForString(String description, Validator<String> validator) {
        return askForString(description, null, validator);
    }

    public static String askForString(String description, String defaultValue, Validator<String> validator) {
        while (true) {
            write(description + (defaultValue == null ? "" : " (default " + defaultValue + ")") + " or type " + Ansi.PROMPT.apply("e") + " exit: ");

            String answer = readLine();
            if (answer.equals("e")) throw new AbortedException();
            if (defaultValue != null && answer.isEmpty()) return defaultValue;

            Optional<String> error = validator.validate(answer);
            if (error.isEmpty()) {
                return answer;
            } else {
                writeLine(error.get(), Ansi.ERROR);
            }
        }
    }


    /* File */

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


    /* Advanced settings */

    public static void configureAdvancedSettings(String description, List<Setting<?>> settings, Configuration configuration) {
        while (true) {
            List<List<String>> rows = settings.stream().map(setting -> List.of(
                    Ansi.PROMPT.apply(setting.getPromptValue()),
                    setting.getName(),
                    setting.valueToString(configuration)
            )).toList();
            writeLine(description + " or type " + Ansi.PROMPT.apply("enter") + " to continue or " + Ansi.PROMPT.apply("e") + " to exit: " + System.lineSeparator() + TableFormatter.format(rows));

            String answer = readLine();
            if (answer.isEmpty()) return; // User chosen to continue
            if (answer.equals("e")) throw new AbortedException();

            Optional<Setting<?>> result = settings.stream().filter(setting -> setting.getPromptValue().equalsIgnoreCase(answer)).findAny();
            if (result.isPresent()) {
                result.get().configure(configuration); // And do not return to allow for additional settings afterwards
            } else {
                writeLine("Unrecognised answer.", Ansi.ERROR);
            }
        }
    }


    /* Other */

    public static void onExit() {
        if (shouldPromptUserBeforeExit) { // Skip prompt if running as CLI
            promptUserToExit();
        }
    }

    public static void promptUserToExit() {
        writeLine(System.lineSeparator() + "Type " + Ansi.PROMPT.apply("enter") + " to terminate...");
        ConsoleIo.readLine();

        shouldPromptUserBeforeExit = false; // Reset to avoid double prompt
    }
}
