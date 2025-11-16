package org.javaibm.eventfeedbackanalyzer.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.javaibm.eventfeedbackanalyzer.client.RobertaClient;
import org.javaibm.eventfeedbackanalyzer.dto.EventResponseDTO;
import org.javaibm.eventfeedbackanalyzer.dto.FeedbackResponseDTO;
import org.javaibm.eventfeedbackanalyzer.dto.SentimentResultDTO;
import org.javaibm.eventfeedbackanalyzer.entity.Event;
import org.javaibm.eventfeedbackanalyzer.entity.Feedback;
import org.javaibm.eventfeedbackanalyzer.repository.h2.EventRepository;
import org.javaibm.eventfeedbackanalyzer.repository.h2.FeedbackRepository;
import org.javaibm.eventfeedbackanalyzer.service.EventService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.groupingBy;

@Slf4j
@Service
public class EventServiceImpl implements EventService {

    private final static String NO_EVENT_ERROR_MESSAGE = "No event with id found: {}";
    private final RobertaClient robertaClient;
    private final EventRepository eventRepository;
    private final FeedbackRepository feedbackRepository;

    EventServiceImpl(RobertaClient robertaClient, EventRepository eventRepository, FeedbackRepository feedbackRepository) {
        this.robertaClient = robertaClient;
        this.eventRepository = eventRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    public void createEvent(String title, String description) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        eventRepository.save(event);
    }

    @Override
    public List<EventResponseDTO> getEvents() {
        return eventRepository.findAll().stream()
                .map(event -> new EventResponseDTO(event.getTitle(), event.getDescription()))
                .toList();
    }

    @Override
    public void submitFeedback(Long eventId, String feedback) {
        try {
            Feedback feedback1 = new Feedback();
            Long timestamp = System.currentTimeMillis();
            feedback1.setContent(feedback);
            feedback1.setEvent(eventRepository.findById(eventId).orElseThrow());
            feedback1.setTimestamp(timestamp);
            feedbackRepository.save(feedback1);
        } catch (NoSuchElementException e) {
            log.error(NO_EVENT_ERROR_MESSAGE, eventId);
            throw new NoSuchElementException();
        }
    }

    @Override
    public Map<String, Double> getAnalysis(Long eventId) {
        try {
            List<Feedback> feedbacks = feedbackRepository.findByEvent_Id(eventId);
            List<List<SentimentResultDTO>> analyzedText = robertaClient.analyzeText(feedbacks)
                    .orElse(List.of());
            log.info("Analyzed text for event {}: {}", eventId, analyzedText);

            Map<String, Double> averages = analyzedText.stream()
                    .flatMap(List::stream)
                    .collect(groupingBy(
                            dto -> mapLabelToSentiment(dto.label()),
                            averagingDouble(SentimentResultDTO::score)
                    ));

            return averages;

        } catch (NoSuchElementException e) {
            log.error(NO_EVENT_ERROR_MESSAGE, eventId);
            throw new NoSuchElementException();
        }
    }

    private String mapLabelToSentiment(String label) {
        return switch (label.toUpperCase()) {
            case "LABEL_0" -> "NEGATIVE";
            case "LABEL_1" -> "NEUTRAL";
            case "LABEL_2" -> "POSITIVE";
            default -> label.toUpperCase();
        };
    }

    @Override
    public List<FeedbackResponseDTO> getFeedbacks() {
        return feedbackRepository.findAll().stream()
                .map(feedback -> new FeedbackResponseDTO(
                        feedback.getContent(),
                        feedback.getEvent(),
                        feedback.getTimestamp()))
                .toList();
    }

}
