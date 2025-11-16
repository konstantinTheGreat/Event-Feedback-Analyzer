package org.javaibm.eventfeedbackanalyzer.controller;

import org.javaibm.eventfeedbackanalyzer.dto.EventResponseDTO;
import org.javaibm.eventfeedbackanalyzer.dto.FeedbackResponseDTO;
import org.javaibm.eventfeedbackanalyzer.entity.Event;
import org.javaibm.eventfeedbackanalyzer.service.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventController.class)
class EventControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Test
    void testCreateEvent_returns200() throws Exception {
        mockMvc.perform(post("/events?title=A&description=B"))
                .andExpect(status().isOk())
                .andExpect(content().string("Event created"));
    }

    @Test
    void testGetEvents_returnsList() throws Exception {
        when(eventService.getEvents())
                .thenReturn(List.of(
                        new EventResponseDTO("A", "D1"),
                        new EventResponseDTO("B", "D2")
                ));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("A"))
                .andExpect(jsonPath("$[1].title").value("B"));
    }

    @Test
    void testGetFeedbacks_returnsList() throws Exception {
        Event e1 = new Event(1L, "Event1", "Desc1");
        Event e2 = new Event(2L, "Event2", "Desc2");

        when(eventService.getFeedbacks())
                .thenReturn(List.of(
                        new FeedbackResponseDTO("F1", e1, 1000L),
                        new FeedbackResponseDTO("F2", e2, 2000L)
                ));

        mockMvc.perform(get("/events/feedbacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("F1"))
                .andExpect(jsonPath("$[1].content").value("F2"))
                .andExpect(jsonPath("$[0].event.title").value("Event1"))
                .andExpect(jsonPath("$[1].event.title").value("Event2"));
    }

    @Test
    void testSubmitFeedback_success() throws Exception {
        mockMvc.perform(post("/events/1/feedback?feedback=Nice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Feedback submitted"));
    }

    @Test
    void testSubmitFeedback_eventNotFound_returns404() throws Exception {
        Mockito.doThrow(new NoSuchElementException())
                .when(eventService).submitFeedback(anyLong(), anyString());

        mockMvc.perform(post("/events/999/feedback?feedback=Hello"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAnalysis_returnsSummary() throws Exception {
        when(eventService.getAnalysis(1L))
                .thenReturn(Map.of("POSITIVE", 0.8, "NEGATIVE", 0.2));

        mockMvc.perform(get("/events/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.POSITIVE").value(0.8))
                .andExpect(jsonPath("$.NEGATIVE").value(0.2));
    }
}
