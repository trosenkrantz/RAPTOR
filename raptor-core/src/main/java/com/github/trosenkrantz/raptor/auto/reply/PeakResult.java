package com.github.trosenkrantz.raptor.auto.reply;

import java.util.List;

public record PeakResult(boolean matched, Transition matchedTransition, List<String> captureGroups) {
}
