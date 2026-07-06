package com.pr.review.reviewbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient gitHubWebClient(GithubProperties props) {
        return WebClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getToken())
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Bean
    public WebClient ollamaWebClient(OllamaProperties props) {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
