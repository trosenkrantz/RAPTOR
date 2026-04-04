package com.github.trosenkrantz.raptor.io;

import java.util.List;
import java.util.regex.Pattern;

public class TableFormatter {
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

    private static String stripAnsi(String value) {
        return ANSI_PATTERN.matcher(value).replaceAll("");
    }

    private static int visibleLength(String value) {
        return stripAnsi(value).length();
    }

    private static String padRight(String value, int targetWidth) {
        return value + " ".repeat(Math.max(0, targetWidth - visibleLength(value)));
    }

    /**
     * Formats a table of strings.
     * @param rows List of rows, each a list of cells.
     * @return Aligned table as a String.
     */
    public static String format(List<List<String>> rows) {
        if (rows.isEmpty()) return "";

        int columnCount = rows.stream().mapToInt(List::size).max().orElse(0);
        int[] maxWidths = new int[columnCount];

        // Determine max width per column (ignoring ANSI codes)
        for (var row : rows) {
            for (int i = 0; i < row.size(); i++) {
                int visible = visibleLength(row.get(i));
                if (visible > maxWidths[i]) {
                    maxWidths[i] = visible;
                }
            }
        }

        // Build formatted string
        var builder = new StringBuilder();
        for (var row : rows) {
            for (int i = 0; i < columnCount; i++) {
                String cell = i < row.size() ? row.get(i) : "";
                builder.append(padRight(cell, maxWidths[i]));
                if (i < columnCount - 1) builder.append("  "); // spacing between columns
            }
            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }
}
