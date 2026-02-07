package com.github.trosenkrantz.raptor.io;

import java.io.ByteArrayOutputStream;
import java.util.HexFormat;

/**
 * Terms:
 * <ul>
 *     <li>Hex escaped string: Printable characters and control characters are as-is (e.g., line feed is a single character), and arbitrary bytes are four characters (e.g., byte 0 is \, x, 0, and 0). Used to store values as JSON. When using Jackson to produce JSON on a hex escaped string, it is a subset of JSON.</li>
 *     <li>Fully escaped text string: Printable characters are as-is, control characters are escaped (e.g., line feed is two characters, \ and n). Arbitrary bytes are not supported. Used for user prompts where we think the bytes are text.</li>
 *     <li>Fully escaped hex string: Each byte is five characters (e.g., byte 0 is \, \, x, 0, and 0). Used for user prompts where we think the bytes are arbitrary bytes.</li>
 *     <li>Fully escaped string: An umbrella term for either a fully escaped text string or fully escaped hex string.</li>
 * </ul>
 */
public class BytesFormatter {
    public static final String DEFAULT_FULLY_ESCAPED_STRING = "Hello, World!";

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
        return hexEscapedStringToFullyEscapedString(bytesToHexEscapedTextString(input));
    }

    public static String bytesToHexEscapedString(byte[] input) {
        if (isText(input)) {
            return bytesToHexEscapedTextString(input);
        } else {
            return bytesToHexEscapedHexString(input);
        }
    }

    public static String bytesToHexEscapedTextString(byte[] input) {
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

    public static String bytesToHexEscapedHexString(byte[] input) {
        return HexFormat.of().withPrefix("\\x").formatHex(input);
    }

    public static String hexEscapedStringToFullyEscapedString(String input) {
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

    public static String bytesToFullyEscapedHexString(byte[] input) {
        return HexFormat.of().withPrefix("\\\\x").formatHex(input);
    }

    public static byte[] fullyEscapedStringToBytes(String input) {
        return hexEscapedStringToBytes(fullyEscapedStringToHexEscapedString(input));
    }

    public static String fullyEscapedStringToHexEscapedString(String input) {
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
}
