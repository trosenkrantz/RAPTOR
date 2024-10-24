package com.github.trosenkrantz.raptor.auto.reply;

import java.util.List;
import java.util.Map;

public record StateMachineConfiguration(String startState, Map<String, List<Transition>> states) {
}
