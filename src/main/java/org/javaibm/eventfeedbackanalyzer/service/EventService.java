package org.javaibm.eventfeedbackanalyzer.service;

import org.javaibm.eventfeedbackanalyzer.dto.EventResponseDTO;
import org.javaibm.eventfeedbackanalyzer.dto.FeedbackResponseDTO;

import java.util.List;
import java.util.Map;

public interface EventService {
    void createEvent(String title, String description);
    List<EventResponseDTO> getEvents();
    void submitFeedback(Long eventId, String feedback);
    Map<String, Double> getAnalysis(Long eventId);
    List<FeedbackResponseDTO> getFeedbacks();
}
