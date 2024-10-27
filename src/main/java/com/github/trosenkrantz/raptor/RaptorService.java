package com.github.trosenkrantz.raptor;

import java.io.IOException;

public interface RaptorService {
    String getPromptValue();

    String getParameterKey();

    String getDescription();

    void configure(Configuration configuration);

    void run(Configuration configuration) throws IOException, InterruptedException;
}
