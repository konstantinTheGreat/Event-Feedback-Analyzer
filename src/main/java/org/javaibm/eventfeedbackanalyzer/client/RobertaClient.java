package org.javaibm.eventfeedbackanalyzer.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaibm.eventfeedbackanalyzer.dto.SentimentResultDTO;
import org.javaibm.eventfeedbackanalyzer.entity.Feedback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Component
public class RobertaClient {

    private final RestClient robertaRestClient;

    @Value("${roberta.api.key}")
    private String robertaApiKey;

    private static final String MODEL = "cardiffnlp/twitter-roberta-base-sentiment";

    public Optional<List<List<SentimentResultDTO>>> analyzeText(List<Feedback> feedbacks) {
        try {
            List<String> texts = feedbacks.stream()
                    .map(Feedback::getContent)
                    .toList();

            Map<String, Object> requestBody = Map.of(
                    "inputs", texts,
                    "parameters", Map.of("return_all_scores", true)
            );

            SentimentResultDTO[][] response = robertaRestClient.post()
                    .uri("/models/" + MODEL)
                    .header("Authorization", "Bearer " + robertaApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(SentimentResultDTO[][].class);

            if (response == null) {
                log.error("Roberta API returned null response");
                return Optional.empty();
            }

            List<List<SentimentResultDTO>> results =
                    Arrays.stream(response)
                            .map(Arrays::asList)
                            .toList();

            log.info("Roberta API response received successfully: {}", results);
            return Optional.of(results);
        } catch (Exception ex) {
            log.error("Error calling Roberta API: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
