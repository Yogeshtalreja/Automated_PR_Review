package com.pr.review.reviewbot.ollama;


import lombok.Data;

@Data
public class OllamaChatResponse {

    private String model;
    private Message message;
    private boolean done;

    @Data
    public static class Message {
        private String role;
        private String content;
    }

}
