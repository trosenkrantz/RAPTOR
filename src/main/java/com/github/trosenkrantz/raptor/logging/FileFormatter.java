package com.github.trosenkrantz.raptor.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class FileFormatter extends Formatter {
    private static final DateTimeFormatter TIME_FORMATTER_RECORD = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder();

        builder.append(LocalDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault()).format(TIME_FORMATTER_RECORD));

        // Pad level, 7 characters is the max length (WARNING)
        String level = record.getLevel().getName();
        builder.append(String.format(" [%-7s] ", level));

        builder.append(record.getMessage());
        builder.append(System.lineSeparator());
        if (record.getThrown() != null) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            record.getThrown().printStackTrace(printWriter);
            builder.append(stringWriter);
        }

        return builder.toString();
    }
}
