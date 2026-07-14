package com.pr.review.reviewbot.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ollama")
@Getter
@Setter
public class OllamaProperties {

    private String baseUrl;
    private String model;
    private String embeddingModel;

}
