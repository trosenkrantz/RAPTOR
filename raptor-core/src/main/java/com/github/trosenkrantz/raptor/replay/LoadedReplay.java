package com.github.trosenkrantz.raptor.replay;

/** A replay timeline and the mode in which to run it. */
public record LoadedReplay(ReplayTimeline timeline, ReplayMode mode) {
}
