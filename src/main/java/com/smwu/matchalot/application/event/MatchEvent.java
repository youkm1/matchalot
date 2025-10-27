package com.smwu.matchalot.application.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class MatchEvent extends ApplicationEvent {
    private final String userId;
    private final String eventType;
    private final Map<String, Object> data;

    public MatchEvent(Object source, String userId, String eventType, Map<String, Object> data) {
        super(source);
        this.userId = userId;
        this.eventType = eventType;
        this.data = data;
    }
}
