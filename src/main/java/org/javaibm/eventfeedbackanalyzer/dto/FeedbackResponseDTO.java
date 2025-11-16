package org.javaibm.eventfeedbackanalyzer.dto;

import org.javaibm.eventfeedbackanalyzer.entity.Event;

public record FeedbackResponseDTO(String content, Event event, Long timestamp ) {
}
