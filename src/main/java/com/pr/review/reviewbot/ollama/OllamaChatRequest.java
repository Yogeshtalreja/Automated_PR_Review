package com.pr.review.reviewbot.ollama;


import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OllamaChatRequest {

    private String model;
    private String format;
    private boolean stream;
    private List<Message> messages;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}

