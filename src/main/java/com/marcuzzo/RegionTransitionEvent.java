package com.marcuzzo;

import javafx.event.Event;
import javafx.event.EventType;

public class RegionTransitionEvent extends Event {
    public static final EventType<RegionTransitionEvent> REGION_TRANSITION = new EventType<>(Event.ANY, "REGION_TRANSITION");
    public RegionTransitionEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }
}
