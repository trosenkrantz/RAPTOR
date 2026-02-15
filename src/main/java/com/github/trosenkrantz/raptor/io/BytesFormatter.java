package com.github.trosenkrantz.raptor.io;

import com.github.trosenkrantz.raptor.auto.reply.StateMachine;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Intermediate encoding is used as an intermediate encoding, between bytes and JSON.
 * When using Jackson to produce a JSON string value from an intermediate encoded string, it produces a RAPTOR encoded string.
 * Rules:
 * <ul>
 *     <li>Printable characters are as-is.</li>
 *     <li>Control characters are as-is (e.g., line feed is a single character)</li>
 *     <li>Arbitrary bytes are four characters (e.g., byte 0 is \, x, 0, and 0). </li>
 * </ul>
 * <p>
 * RAPTOR encoding is used for user prompts and inputs.
 * It is a subset of JSON string values.
 * Rules:
 * <ul>
 *     <li>Printable characters are as-is</li>
 *     <li>Control characters are escaped (e.g., line feed is two characters, \ and n)</li>
 *     <li>Arbitrary bytes are each five characters (e.g., byte 0 is \, \, x, 0, and 0)</li>
 * </ul>
 * <p>
 * When converting from bytes to either intermediate or RAPTOR encoding, we guess if the bytes are intended as text or arbitrary bytes.
 * If we guess intend of arbitrary bytes, we treat all bytes as arbitrary bytes.
 * E.g., For bytes with hex values 00 and 48 (H in ASCII), we encode it as arbitrary bytes with string \\x00\x48.
 */
public class BytesFormatter {
    public static final String DEFAULT_FULLY_ESCAPED_STRING = "Hello, World!";

    private static final Logger LOGGER = Logger.getLogger(BytesFormatter.class.getName());

    public static String bytesToRaptorEncoding(byte[] input) {
        if (isText(input)) {
            return bytesToRaptorEncodedText(input);
        } else {
            return bytesToRaptorEncodedBytes(input);
        }
    }

    public static String bytesToRaptorEncodingWithType(byte[] input) {
        if (isText(input)) {
            return "text: " + bytesToRaptorEncodedText(input);
        } else {
            return "bytes: " + bytesToRaptorEncodedBytes(input);
        }
    }

    public static boolean isText(byte[] bytes) {
        for (byte b : bytes) {
            if (!isText(b)) return false;
        }
        return true;
    }

    public static String getType(byte[] bytes) {
        return isText(bytes) ? "text" : "bytes";
    }

    private static boolean isText(byte b) {
        if (b < 0x20) { // Is control char
            return b == 0x09 || b == 0x0A || b == 0x0D; // If tab, LF, or CR
        } else {
            return b != 0x7F; // If not DEL
        }
    }

    public static String bytesToRaptorEncodedText(byte[] input) {
        return intermediateEncodingToRaptorEncoded(bytesToIntermediateEncodedText(input));
    }

    public static String bytesToIntermediateEncoding(byte[] input) {
        if (isText(input)) {
            return bytesToIntermediateEncodedText(input);
        } else {
            return bytesToIntermediateEncodedBytes(input);
        }
    }

    public static String bytesToIntermediateEncodedText(byte[] input) {
        StringBuilder builder = new StringBuilder();

        int length = input.length;
        for (int i = 0; i < length; i++) {
            // For an edge case of four bytes that would encode as \, \, x, a hex character, and a hex character, that would decode to a single byte.
            // Instead, we process the backslash as an arbitrary byte with hex value 5c, which is a backslash in ASCII.
            if (input[i] == '\\'
                    && i + 3 < length
                    && input[i + 1] == 'x'
                    && isHex((char) input[i + 2])
                    && isHex((char) input[i + 3])) {
                builder.append("\\x5c");
            } else {
                builder.append((char) input[i]);
            }
        }
        return builder.toString();
    }

    public static String bytesToIntermediateEncodedBytes(byte[] input) {
        return HexFormat.of().withPrefix("\\x").formatHex(input);
    }

    public static String intermediateEncodingToRaptorEncoded(String input) {
        StringBuilder result = new StringBuilder();

        int length = input.length();
        for (int i = 0; i < length; i++) {
            char currentChar = input.charAt(i);

            result.append(switch (currentChar) {
                case '\n' -> "\\n";
                case '\r' -> "\\r";
                case '\t' -> "\\t";
                case '\"' -> "\\\"";
                case '\\' -> "\\\\";
                default -> String.valueOf(currentChar);
            });
        }

        return result.toString();
    }

    public static String bytesToRaptorEncodedBytes(byte[] input) {
        return HexFormat.of().withPrefix("\\\\x").formatHex(input);
    }

    public static byte[] raptorEncodingToBytes(String input) {
        return intermediateEncodingToBytes(raptorEncodingToIntermediateEncodedBytes(input));
    }

    public static String raptorEncodingToIntermediateEncodedBytes(String input) {
        StringBuilder stringBuilder = new StringBuilder();

        int length = input.length();
        for (int i = 0; i < length; i++) {
            char currentChar = input.charAt(i);

            if (currentChar == '\\' && i + 1 < length) { // Match \.
                char nextChar = input.charAt(i + 1);
                stringBuilder.append(switch (nextChar) {
                    case 'n' -> "\n";
                    case 'r' -> "\r";
                    case 't' -> "\t";
                    case '\"' -> "\"";
                    case '\\' -> "\\";
                    default -> "\\" + nextChar;
                });
                i += 1; // Skip past nextChar
            } else { // Regular character
                stringBuilder.append(currentChar);
            }
        }

        return stringBuilder.toString();
    }

    public static byte[] intermediateEncodingToBytes(String input) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        int length = input.length();
        for (int i = 0; i < length; i++) {
            if (isIntermediateHexEncoding(input, i)) {
                byteStream.write(Integer.parseInt(input.substring(i + 2, i + 4), 16)); // Parse as hex to byte
                i += 3; // Skip past hex encoding
                continue;
            }

            if (isIntermediateCommandSubstitutionEncoding(input, i)) {
                int start = i + 3;
                Optional<Integer> end = getClosingParenthesisIndex(input, start);

                if (end.isPresent()) {
                    String command = input.substring(start, end.get());
                    byte[] stdout = executeCommand(command);
                    String outputString = new String(stdout, StandardCharsets.ISO_8859_1); // stdout should be RAPTOR encoding, which is ASCII, but use ISO 8859-1 to be more forgiving
                    String trimmedOutputString = outputString.replaceAll("\\r?\\n$", ""); // Many CLI commands append a newline in stdout, this is invalid in RAPTOR encoding, trimmed away to be more forgiving
                    byte[] commandBytes = raptorEncodingToBytes(trimmedOutputString); // Convert the RAPTOR encoded stdout to bytes
                    byteStream.write(commandBytes, 0, commandBytes.length);

                    i = end.get(); // Skip past the closing parenthesis
                    continue;
                }
            }

            // Regular character
            byteStream.write((byte) input.charAt(i));
        }

        return byteStream.toByteArray();
    }

    /**
     * Example: \h00
     *
     * @param input intermediate encoding
     * @param i     index
     * @return true iff hex encoding
     */
    private static boolean isIntermediateHexEncoding(String input, int i) {
        return input.charAt(i) == '\\' && i + 3 < input.length() && input.charAt(i + 1) == 'x' && isHex(input.charAt(i + 2)) && isHex(input.charAt(i + 3));
    }

    /**
     * Example: \$(date)
     *
     * @param input intermediate encoding
     * @param i     index
     * @return true iff command substitution encoding
     */
    private static boolean isIntermediateCommandSubstitutionEncoding(String input, int i) {
        return input.charAt(i) == '\\' && i + 4 < input.length() && input.charAt(i + 1) == '$' && input.charAt(i + 2) == '(';
    }

    private static Optional<Integer> getClosingParenthesisIndex(String input, int start) {
        int depth = 1;
        for (int i = start; i < input.length(); i++) {
            if (input.charAt(i) == '(') depth++;
            else if (input.charAt(i) == ')') depth--;
            if (depth == 0) return Optional.of(i);
        }
        return Optional.empty();
    }

    private static byte[] executeCommand(String command) {
        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        String[] shellConfig = isWindows ? new String[]{"cmd", "/c", command} : new String[]{"sh", "-c", command};

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Process process = new ProcessBuilder(shellConfig).start();

            // Start capturing streams in parallel to prevent deadlock
            Future<byte[]> stdoutFuture = executor.submit(() -> process.getInputStream().readAllBytes());
            Future<byte[]> stderrFuture = executor.submit(() -> process.getErrorStream().readAllBytes());

            try {
                byte[] stdout = stdoutFuture.get(1, TimeUnit.SECONDS); // Timeout to not block RAPTOR
                byte[] stderr = stderrFuture.get(100, TimeUnit.MILLISECONDS); // Expect stderr to end at most shortly after

                if (process.waitFor(100, TimeUnit.MILLISECONDS)) { // Expect process to terminate at most shortly after
                    if (stderr.length > 0) {
                        LOGGER.warning("Command " + command + " reported stderr: " + new String(stderr, StandardCharsets.UTF_8).trim()); // Use UTF-8 as that is usually the case for stderr, and out logging framework supports it
                    }

                    int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        LOGGER.severe("Command " + command + " existed with code " + exitCode + ". Skipping processing its output.");
                        return new byte[0];
                    }
                } else {
                    process.destroyForcibly();
                    LOGGER.severe("Command " + command + " did not terminate by itself. Killed it and skipping processing its output.");
                    return new byte[0];
                }

                return stdout;
            } catch (TimeoutException e) {
                process.destroyForcibly();
                LOGGER.severe("Command " + command + " did not terminate its standard streams. Killed it and skipping processing its output.");
                return new byte[0];
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed command " + command + ". ", e);
            return new byte[0];
        }
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }
}
