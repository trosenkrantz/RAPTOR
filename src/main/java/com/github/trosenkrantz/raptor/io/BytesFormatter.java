package com.github.trosenkrantz.raptor.io;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class BytesFormatter {
    public static String format(byte[] input) {
        if (isText(input)) {
            return "text: " + new String(input, StandardCharsets.UTF_8);
        } else {
            return "bytes: " + bytesToHex(input);
        }
    }

    private static boolean isText(byte[] bytes) {
        for (byte b : bytes) {
            if (!isText(b)) return false;
        }
        return true;
    }

    private static boolean isText(byte b) {
        if (b < 0x20) { // Is control char
            return b == 0x09 || b == 0x0A || b == 0x0D; // If tab, LF, or CR
        } else {
            return b != 0x7F; // If not DEL
        }
    }

    // Method to convert byte array to hexadecimal representation
    private static String bytesToHex(byte[] bytes) {
        HexFormat format = HexFormat.ofDelimiter(" ");
        return format.formatHex(bytes);
    }

    public static byte[] escapedHexStringToBytes(String hexString) {
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
