package com.pr.review.reviewbot.ollama;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbeddingRequest {

    private String model;
    private String prompt;
}
