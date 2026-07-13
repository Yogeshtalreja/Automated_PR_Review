package com.pr.review.reviewbot.ollama;


import com.pr.review.reviewbot.config.OllamaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

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
                .format("json")
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

        if (response == null || response.getMessage() == null || response.getMessage().getContent() == null) {
            log.warn("Received empty Ollama response for file {}", filename);
            return List.of();
        }

        return parseComments(response.getMessage().getContent());
    }

    private List<ReviewComment> parseComments(String content) {
        try {
            String cleaned = content
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            // Handle single object instead of array
            if (cleaned.startsWith("{")) {
                log.debug("Model returned single object, wrapping in array");
                cleaned = "[" + cleaned + "]";
            }

            // Remove trailing commas before } or ] — common LLM mistake
            cleaned = cleaned
                    .replaceAll(",\\s*}", "}")
                    .replaceAll(",\\s*]", "]");

            return objectMapper.readValue(cleaned,
                    new TypeReference<List<ReviewComment>>() {});
        } catch (Exception e) {
            log.warn("Could not parse Ollama response as JSON array. Response starts with: {}",
                    content.substring(0, Math.min(content.length(), 200)));
            return List.of();
        }
    }

    private Optional<String> extractFirstJsonArray(String content) {
        int start = content.indexOf('[');
        if (start < 0) {
            return Optional.empty();
        }

        int depth = 0;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return Optional.of(content.substring(start, i + 1));
                }
            }
        }

        return Optional.empty();
    }
}
