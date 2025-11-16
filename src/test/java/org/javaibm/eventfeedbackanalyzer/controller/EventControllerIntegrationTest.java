package org.javaibm.eventfeedbackanalyzer.controller;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.javaibm.eventfeedbackanalyzer.dto.EventResponseDTO;
import org.javaibm.eventfeedbackanalyzer.dto.FeedbackResponseDTO;
import org.javaibm.eventfeedbackanalyzer.entity.Event;
import org.javaibm.eventfeedbackanalyzer.entity.Feedback;
import org.javaibm.eventfeedbackanalyzer.repository.h2.EventRepository;
import org.javaibm.eventfeedbackanalyzer.repository.h2.FeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WireMockTest(httpPort = 8090)
class EventControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @BeforeEach
    void setup() {
        feedbackRepository.deleteAll();
        eventRepository.deleteAll();

        stubFor(post(urlPathEqualTo("/models/cardiffnlp/twitter-roberta-base-sentiment"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  [
                                    {"label": "LABEL_0", "score": 0.1},
                                    {"label": "LABEL_1", "score": 0.2},
                                    {"label": "LABEL_2", "score": 0.7}
                                  ]
                                ]
                                """)));
    }

    @Test
    void testCreateEvent_shouldReturn200() {
        ResponseEntity<String> response =
                restTemplate.postForEntity("/events?title=A&description=B", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Event created");
        assertThat(eventRepository.findAll()).hasSize(1);
    }

    @Test
    void testCreateEvent_shouldPersist() {
        restTemplate.postForEntity("/events?title=X&description=Y", null, String.class);

        Event ev = eventRepository.findAll().getFirst();
        assertThat(ev.getTitle()).isEqualTo("X");
        assertThat(ev.getDescription()).isEqualTo("Y");
    }


    @Test
    void testGetEvents_emptyList() {
        ResponseEntity<EventResponseDTO[]> response =
                restTemplate.getForEntity("/events", EventResponseDTO[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void testGetEvents_listReturned() {
        eventRepository.save(new Event(null, "A", "D1"));
        eventRepository.save(new Event(null, "B", "D2"));

        ResponseEntity<EventResponseDTO[]> response =
                restTemplate.getForEntity("/events", EventResponseDTO[].class);

        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).extracting(EventResponseDTO::title)
                .containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void testGetFeedbacks_emptyList() {
        ResponseEntity<FeedbackResponseDTO[]> response =
                restTemplate.getForEntity("/events/feedbacks", FeedbackResponseDTO[].class);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void testGetFeedbacks_listReturned() {
        Event e1 = eventRepository.save(new Event(null, "E1", "D1"));
        Event e2 = eventRepository.save(new Event(null, "E2", "D2"));

        feedbackRepository.save(new Feedback(null, e1, "F1", 1000L));
        feedbackRepository.save(new Feedback(null, e2, "F2", 2000L));

        ResponseEntity<FeedbackResponseDTO[]> response =
                restTemplate.getForEntity("/events/feedbacks", FeedbackResponseDTO[].class);

        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).extracting(FeedbackResponseDTO::content)
                .containsExactlyInAnyOrder("F1", "F2");
    }

    @Test
    void testSubmitFeedback_success() {
        Event e = eventRepository.save(new Event(null, "E", "D"));

        ResponseEntity<String> response =
                restTemplate.postForEntity("/events/" + e.getId() + "/feedback?feedback=Nice", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Feedback submitted");
    }

    @Test
    void testSubmitFeedback_persists() {
        Event e = eventRepository.save(new Event(null, "E", "D"));

        restTemplate.postForEntity("/events/" + e.getId() + "/feedback?feedback=Nice", null, String.class);

        Feedback f = feedbackRepository.findAll().getFirst();
        assertThat(f.getContent()).isEqualTo("Nice");
        assertThat(f.getEvent().getId()).isEqualTo(e.getId());
        assertThat(f.getTimestamp()).isGreaterThan(0);
    }

    @Test
    void testSubmitFeedback_eventNotFound_returns404() {
        ResponseEntity<String> response =
                restTemplate.postForEntity("/events/999/feedback?feedback=Hello", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(feedbackRepository.findAll()).isEmpty();
    }

    @Test
    void testMultipleFeedbacks() {
        Event e = eventRepository.save(new Event(null, "E", "D"));

        restTemplate.postForEntity("/events/" + e.getId() + "/feedback?feedback=A", null, String.class);
        restTemplate.postForEntity("/events/" + e.getId() + "/feedback?feedback=B", null, String.class);
        restTemplate.postForEntity("/events/" + e.getId() + "/feedback?feedback=C", null, String.class);

        List<Feedback> list = feedbackRepository.findAll();
        assertThat(list).hasSize(3);
        assertThat(list).extracting(Feedback::getContent)
                .containsExactlyInAnyOrder("A", "B", "C");
    }
}
