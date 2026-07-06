package com.pr.review.reviewbot.ollama;


import com.pr.review.reviewbot.config.OllamaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Service
public class OllamaClient {


    private final WebClient ollamaWebClient;
    private final OllamaProperties ollamaProperties;
    private final ObjectMapper objectMapper;
    private final PromptBuilder promptBuilder;

    public OllamaClient(@Qualifier("ollamaWebClient") WebClient ollamaWebClient,
                        OllamaProperties ollamaProperties,
                        ObjectMapper objectMapper,
                        PromptBuilder promptBuilder) {
        this.ollamaWebClient = ollamaWebClient;
        this.ollamaProperties = ollamaProperties;
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
    }


    public List<ReviewComment> reviewCode(String filename, String patch) {
        log.debug("Sending diff to Ollama for review");

        OllamaChatRequest request = OllamaChatRequest.builder()
                .model(ollamaProperties.getModel())
                .stream(false)
                .messages(List.of(
                        OllamaChatRequest.Message.builder()
                                .role("system")
                                .content(promptBuilder.buildSystemPrompt())
                                .build(),
                        OllamaChatRequest.Message.builder()
                                .role("user")
                                .content(promptBuilder.buildUserPrompt(filename, patch))
                                .build()
                ))
                .build();

        OllamaChatResponse response = ollamaWebClient.post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaChatResponse.class)
                .block();

        return parseComments(response.getMessage().getContent());
    }

    private List<ReviewComment> parseComments(String content) {
        try {
            // Clean up in case model adds markdown backticks despite instructions
            String cleaned = content
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();
            return objectMapper.readValue(cleaned,
                    new TypeReference<List<ReviewComment>>() {});
        } catch (Exception e) {
            log.warn("Could not parse Ollama response as JSON: {}", content);
            return List.of(); // return empty list rather than crashing
        }
    }

    }
