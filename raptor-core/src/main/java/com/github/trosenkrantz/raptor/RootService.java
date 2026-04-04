package com.github.trosenkrantz.raptor;

import com.github.trosenkrantz.raptor.configuration.Configuration;

/**
 * Object that can be run independently by RAPTOR.
 */
public interface RootService extends RaptorService {
    /**
     * Prompts the user to configure the service.
     *
     * @param configuration configuration to populate with the user's input.
     * @throws Exception if the configuration fails.
     */
    void configure(Configuration configuration) throws Exception;

    /**
     * Runs the service.
     *
     * @param configuration configuration specifying how to run the service.
     * @throws Exception if the service fails.
     */
    void run(Configuration configuration) throws Exception;
}
