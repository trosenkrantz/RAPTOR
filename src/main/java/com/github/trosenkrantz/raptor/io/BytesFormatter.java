package com.github.trosenkrantz.raptor.io;

import java.io.ByteArrayOutputStream;
import java.util.HexFormat;

/**
 * Terms:
 * <ul>
 *     <li>Hex escaped string: Printable characters and control characters are as-is (e.g., line feed is a single character), and arbitrary bytes are four characters (e.g., byte 0 is \, x, 0, and 0). Used for in-memory processing.</li>
 *     <li>Fully escaped string: Printable characters are as-is, control characters are escaped (e.g., line feed is two characters, \ and n), and arbitrary bytes are five characters (e.g., byte 0 is \, \, x, 0, and 0). Used for I/O and user prompts. From user's point of view, this is known as the RAPTOR encoding.</li>
 *     <li>Fully escaped text string: A fully escaped string that is only printable characters and control characters. Used for I/O where we think the bytes are text.</li>
 *     <li>Fully escaped hex string: Is a fully escaped string, but all bytes are formatted as arbitrary bytes. Used for I/O where we think the bytes are arbitrary bytes.</li>
 * </ul>
 */
public class BytesFormatter {
    public static final String DEFAULT_FULLY_ESCAPED_STRING = "Hello, World!";
    private static final String REGEX_CLI_ARGUMENTS_TO_ESCAPE = "([!\"#$%&'()*:;<>?@\\[\\]^`{|}~\\[\\]])";

    public static String bytesToFullyEscapedString(byte[] input) {
        if (isText(input)) {
            return bytesToFullyEscapedTextString(input);
        } else {
            return bytesToFullyEscapedHexString(input);
        }
    }

    public static String bytesToFullyEscapedStringWithType(byte[] input) {
        if (isText(input)) {
            return "text: " + bytesToFullyEscapedTextString(input);
        } else {
            return "bytes: " + bytesToFullyEscapedHexString(input);
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

    public static String bytesToFullyEscapedTextString(byte[] input) {
        StringBuilder builder = new StringBuilder();

        int length = input.length;
        for (int i = 0; i < length; i++) {
            char currentChar = (char) input[i];

            builder.append(switch (currentChar) {
                case '\n' -> "\\n";
                case '\r' -> "\\r";
                case '\t' -> "\\t";
                case '\"' -> "\\\"";
                case '\\' -> {
                    // If this backslash could start a hex escape, force escaping the backslash
                    if (i + 3 < length
                            && input[i + 1] == 'x'
                            && isHex((char) input[i + 2])
                            && isHex((char) input[i + 3])) {
                        yield "\\\\x5c"; // Hex escape backslash
                    } else {
                        yield  "\\\\";
                    }
                }
                default -> String.valueOf(currentChar);
            });
        }
        return builder.toString();
    }

    public static String bytesToFullyEscapedHexString(byte[] bytes) {
        return HexFormat.of().withPrefix("\\\\x").formatHex(bytes);
    }

    public static byte[] fullyEscapedStringToBytes(String input) {
        return hexEscapedStringToBytes(fullyEscapedStringToHexEscapedString(input));
    }

    private static String fullyEscapedStringToHexEscapedString(String input) {
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

    public static byte[] hexEscapedStringToBytes(String hexString) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        int length = hexString.length();
        for (int i = 0; i < length; i++) {
            char c = hexString.charAt(i);

            if (c == '\\' && i + 3 < length && hexString.charAt(i + 1) == 'x' && isHex(hexString.charAt(i + 2)) && isHex(hexString.charAt(i + 3))) { // Match \xhh
                byteStream.write(Integer.parseInt(hexString.substring(i + 2, i + 4), 16)); // Parse as hex to byte
                i += 3; // Skip past hex
            } else { // Regular character
                byteStream.write((byte) c);
            }
        }

        return byteStream.toByteArray();
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    public static String escapeCliArgument(String argument) {
        // Escape special characters for Bash and .cmd usage
        argument = argument.replaceAll(REGEX_CLI_ARGUMENTS_TO_ESCAPE, "\\\\$1");


        if (argument.contains(" ")) {
            return "\"" + argument + "\""; // Wrap in quotes to handle spaces
        } else {
            return argument;
        }
    }

    public static String unescapeCliArgument(String argument) {
        // Remove the wrapping quotes
        if (argument.startsWith("\"") && argument.endsWith("\"")) {
            argument = argument.substring(1, argument.length() - 1);
        }

        // Unescape special characters
        return argument.replaceAll("\\\\" + REGEX_CLI_ARGUMENTS_TO_ESCAPE, "$1");
    }
}
