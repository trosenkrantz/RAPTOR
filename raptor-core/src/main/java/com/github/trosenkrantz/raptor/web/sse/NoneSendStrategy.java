package com.github.trosenkrantz.raptor.web.sse;

class NoneSendStrategy implements SseSendStrategy {
    @Override
    public void start(SseServer server, Runnable shutDownAction) {
        // Keep accepting subscribers without sending any events
    }
}
