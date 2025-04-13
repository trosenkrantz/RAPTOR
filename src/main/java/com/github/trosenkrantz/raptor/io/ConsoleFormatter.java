package com.github.trosenkrantz.raptor.io;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

class ConsoleFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder();

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
