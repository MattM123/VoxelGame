package com.marcuzzo;

import javafx.event.Event;
import javafx.event.EventType;

public class ChunkTransitionEvent extends Event {

    public static final EventType<ChunkTransitionEvent> CHUNK_TRANSITION = new EventType<>(Event.ANY, "CHUNK_TRANSITION");
    public ChunkTransitionEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }
}
