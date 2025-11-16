package org.javaibm.eventfeedbackanalyzer.service;

import org.javaibm.eventfeedbackanalyzer.client.RobertaClient;
import org.javaibm.eventfeedbackanalyzer.dto.EventResponseDTO;
import org.javaibm.eventfeedbackanalyzer.dto.FeedbackResponseDTO;
import org.javaibm.eventfeedbackanalyzer.dto.SentimentResultDTO;
import org.javaibm.eventfeedbackanalyzer.entity.Event;
import org.javaibm.eventfeedbackanalyzer.entity.Feedback;
import org.javaibm.eventfeedbackanalyzer.repository.h2.EventRepository;
import org.javaibm.eventfeedbackanalyzer.repository.h2.FeedbackRepository;
import org.javaibm.eventfeedbackanalyzer.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplUnitTest {

    @Mock
    private RobertaClient robertaClient;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event testEvent;
    private Feedback testFeedback;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setTitle("Test Event");
        testEvent.setDescription("Test Description");

        testFeedback = new Feedback();
        testFeedback.setId(1L);
        testFeedback.setContent("Great event!");
        testFeedback.setEvent(testEvent);
        testFeedback.setTimestamp(System.currentTimeMillis());
    }

    @Test
    void createEvent_shouldSaveEvent() {
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        eventService.createEvent("Test Event", "Test Description");

        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void getEvents_shouldReturnEmptyList_whenNoEvents() {
        when(eventRepository.findAll()).thenReturn(List.of());

        List<EventResponseDTO> result = eventService.getEvents();

        assertThat(result).isEmpty();
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    void getEvents_shouldReturnListOfEvents() {
        Event event1 = new Event();
        event1.setTitle("Event 1");
        event1.setDescription("Description 1");

        Event event2 = new Event();
        event2.setTitle("Event 2");
        event2.setDescription("Description 2");

        when(eventRepository.findAll()).thenReturn(List.of(event1, event2));

        List<EventResponseDTO> result = eventService.getEvents();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EventResponseDTO::title)
                .containsExactly("Event 1", "Event 2");
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    void submitFeedback_shouldSaveFeedback_whenEventExists() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(testFeedback);

        eventService.submitFeedback(1L, "Great event!");

        verify(eventRepository, times(1)).findById(1L);
        verify(feedbackRepository, times(1)).save(any(Feedback.class));
    }

    @Test
    void submitFeedback_shouldThrowException_whenEventNotFound() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.submitFeedback(999L, "Test feedback"))
                .isInstanceOf(NoSuchElementException.class);

        verify(eventRepository, times(1)).findById(999L);
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void getFeedbacks_shouldReturnEmptyList_whenNoFeedbacks() {
        when(feedbackRepository.findAll()).thenReturn(List.of());

        List<FeedbackResponseDTO> result = eventService.getFeedbacks();

        assertThat(result).isEmpty();
        verify(feedbackRepository, times(1)).findAll();
    }

    @Test
    void getFeedbacks_shouldReturnListOfFeedbacks() {
        Feedback feedback1 = new Feedback();
        feedback1.setContent("Great!");
        feedback1.setEvent(testEvent);
        feedback1.setTimestamp(123456L);

        Feedback feedback2 = new Feedback();
        feedback2.setContent("Awesome!");
        feedback2.setEvent(testEvent);
        feedback2.setTimestamp(789012L);

        when(feedbackRepository.findAll()).thenReturn(List.of(feedback1, feedback2));

        List<FeedbackResponseDTO> result = eventService.getFeedbacks();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(FeedbackResponseDTO::content)
                .containsExactly("Great!", "Awesome!");
        verify(feedbackRepository, times(1)).findAll();
    }

    @Test
    void getAnalysis_shouldReturnSentimentMap_whenFeedbacksExist() {
        List<SentimentResultDTO> sentimentResults = List.of(
                new SentimentResultDTO("LABEL_0", 0.1),
                new SentimentResultDTO("LABEL_1", 0.2),
                new SentimentResultDTO("LABEL_2", 0.7)
        );

        when(feedbackRepository.findByEvent_Id(1L)).thenReturn(List.of(testFeedback));
        when(robertaClient.analyzeText(anyList())).thenReturn(Optional.of(List.of(sentimentResults)));

        Map<String, Double> result = eventService.getAnalysis(1L);

        assertThat(result).isNotEmpty();
        assertThat(result).containsKeys("POSITIVE", "NEUTRAL", "NEGATIVE");
        assertThat(result.get("POSITIVE")).isEqualTo(0.7);
        assertThat(result.get("NEUTRAL")).isEqualTo(0.2);
        assertThat(result.get("NEGATIVE")).isEqualTo(0.1);

        verify(feedbackRepository, times(1)).findByEvent_Id(1L);
        verify(robertaClient, times(1)).analyzeText(anyList());
    }

    @Test
    void getAnalysis_shouldReturnEmptyMap_whenNoFeedbacks() {
        when(feedbackRepository.findByEvent_Id(1L)).thenReturn(List.of());
        when(robertaClient.analyzeText(anyList())).thenReturn(Optional.of(List.of()));

        Map<String, Double> result = eventService.getAnalysis(1L);

        assertThat(result).isEmpty();
        verify(feedbackRepository, times(1)).findByEvent_Id(1L);
        verify(robertaClient, times(1)).analyzeText(anyList());
    }

    @Test
    void getAnalysis_shouldReturnEmptyMap_whenRobertaClientReturnsEmpty() {
        when(feedbackRepository.findByEvent_Id(1L)).thenReturn(List.of(testFeedback));
        when(robertaClient.analyzeText(anyList())).thenReturn(Optional.empty());

        Map<String, Double> result = eventService.getAnalysis(1L);

        assertThat(result).isEmpty();
        verify(feedbackRepository, times(1)).findByEvent_Id(1L);
        verify(robertaClient, times(1)).analyzeText(anyList());
    }


    @Test
    void getAnalysis_shouldHandleUnknownLabels() {
        List<SentimentResultDTO> sentimentResults = List.of(
                new SentimentResultDTO("UNKNOWN_LABEL", 0.5),
                new SentimentResultDTO("LABEL_2", 0.7)
        );

        when(feedbackRepository.findByEvent_Id(1L)).thenReturn(List.of(testFeedback));
        when(robertaClient.analyzeText(anyList())).thenReturn(Optional.of(List.of(sentimentResults)));

        Map<String, Double> result = eventService.getAnalysis(1L);

        assertThat(result).containsKey("POSITIVE");
        assertThat(result).containsKey("UNKNOWN_LABEL");
        assertThat(result.get("POSITIVE")).isEqualTo(0.7);
        assertThat(result.get("UNKNOWN_LABEL")).isEqualTo(0.5);
    }

    @Test
    void submitFeedback_shouldSetTimestamp() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback saved = invocation.getArgument(0);
            assertThat(saved.getTimestamp()).isNotNull();
            assertThat(saved.getTimestamp()).isGreaterThan(0L);
            return saved;
        });

        eventService.submitFeedback(1L, "Great event!");

        verify(feedbackRepository, times(1)).save(any(Feedback.class));
    }

    @Test
    void createEvent_shouldSetTitleAndDescription() {
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            assertThat(saved.getTitle()).isEqualTo("My Event");
            assertThat(saved.getDescription()).isEqualTo("My Description");
            return saved;
        });

        eventService.createEvent("My Event", "My Description");

        verify(eventRepository, times(1)).save(any(Event.class));
    }
}