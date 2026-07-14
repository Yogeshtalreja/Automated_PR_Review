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


    public List<ReviewComment> reviewCode(
            String filename, String patch, String ragContext) {

        log.info("Sending {} to Ollama with RAG context", filename);

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
                                .content(promptBuilder.buildUserPrompt(
                                        filename, patch, ragContext))
                                .build()
                ))
                .build();

        OllamaChatResponse response = ollamaWebClient.post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaChatResponse.class)
                .block();

        log.info("Ollam Request {} with RAG context", request);
        log.info("Ollam Response {} with RAG context", response);


        return parseComments(response.getMessage().getContent());
    }

    private List<ReviewComment> parseComments(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        try {
            // Step 1 — Remove markdown backticks
            String cleaned = content
                    .replaceAll("(?s)```json", "")
                    .replaceAll("```", "")
                    .trim();

            // Step 2 — Fix trailing commas before } or ]
            cleaned = cleaned
                    .replaceAll(",\\s*}", "}")
                    .replaceAll(",\\s*]", "]");

            // Step 3 — Merge multiple arrays into one
            cleaned = cleaned
                    .replaceAll("\\]\\s*\\[", ",")
                    .trim();

            // Step 4 — Wrap single object in array
            if (cleaned.startsWith("{")) {
                cleaned = "[" + cleaned + "]";
            }

            // Step 5 — Ensure starts with [ and ends with ]
            if (!cleaned.startsWith("[")) {
                int start = cleaned.indexOf("[");
                if (start != -1) cleaned = cleaned.substring(start);
            }
            if (!cleaned.endsWith("]")) {
                int end = cleaned.lastIndexOf("]");
                if (end != -1) cleaned = cleaned.substring(0, end + 1);
            }

            log.debug("Cleaned JSON: {}", cleaned);

            List<ReviewComment> result = objectMapper.readValue(cleaned,
                    new TypeReference<List<ReviewComment>>() {});

            log.debug("Parsed {} comments successfully", result.size());
            return result;

        } catch (Exception e) {
            log.warn("Parse failed: {} | Raw content: {}",
                    e.getMessage(),
                    content.substring(0, Math.min(content.length(), 300)));
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

    public List<Float> generateEmbedding(String text) {
        EmbeddingRequest request = EmbeddingRequest.builder()
                .model(ollamaProperties.getEmbeddingModel())
                .prompt(text)
                .build();

        EmbeddingResponse response = ollamaWebClient.post()
                .uri("/api/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();

        return response.getEmbedding();
    }

}
