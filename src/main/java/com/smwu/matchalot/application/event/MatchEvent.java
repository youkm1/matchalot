package com.smwu.matchalot.application.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MatchEvent extends ApplicationEvent {
    private final String userId;
    private final String eventType;
    private final Object data;

    public MatchEvent(Object source, String userId, String eventType, Object data) {
        super(source);
        this.userId = userId;
        this.eventType = eventType;
        this.data = data;
    }
}