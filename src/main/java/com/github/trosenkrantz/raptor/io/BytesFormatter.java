package com.github.trosenkrantz.raptor.io;

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
}
