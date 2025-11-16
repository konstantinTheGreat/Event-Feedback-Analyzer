package org.javaibm.eventfeedbackanalyzer.controller;

import lombok.AllArgsConstructor;
import org.javaibm.eventfeedbackanalyzer.dto.EventResponseDTO;
import org.javaibm.eventfeedbackanalyzer.dto.FeedbackResponseDTO;
import org.javaibm.eventfeedbackanalyzer.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController("/events")
public class EventController {
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestParam String title,
                                         @RequestParam String description) {
        eventService.createEvent(title, description);

        return ResponseEntity.ok("Event created");
    }

    @GetMapping
    public List<EventResponseDTO> getEvents() {
        return eventService.getEvents();
    }

    @GetMapping("/feedbacks")
    public List<FeedbackResponseDTO> getFeedbacks() {
        return eventService.getFeedbacks();
    }

    @PostMapping("/{eventId}/feedback")
    public ResponseEntity<?> submitFeedback(@PathVariable Long eventId, String feedback) {
        eventService.submitFeedback(eventId, feedback);
        return ResponseEntity.ok("Feedback submitted");
    }

    @GetMapping("/{eventId}/summary")
    public Map<String, Double> getAnalysis(@PathVariable long eventId) {
        return eventService.getAnalysis(eventId);
    }


}
