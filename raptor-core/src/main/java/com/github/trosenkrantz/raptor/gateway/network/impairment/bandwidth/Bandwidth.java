package com.github.trosenkrantz.raptor.gateway.network.impairment.bandwidth;

public record Bandwidth(int bitsPerSecond, int maxBurstDurationMillis, int queueDurationMillis) {
}
