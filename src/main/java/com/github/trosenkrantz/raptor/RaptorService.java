package com.github.trosenkrantz.raptor;

public interface RaptorService {
    String getPromptValue();

    String getParameterKey();

    String getDescription();

    void configure(Configuration configuration);

    void run(Configuration configuration);
}
