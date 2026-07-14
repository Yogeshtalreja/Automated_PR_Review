package com.pr.review.reviewbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ConfigurationProperties(prefix = "github")
public class GithubProperties {

    private String token;

    @Value("${github.api-base-url:https://api.github.com}")
    private String apiBaseUrl;

    @Value("${github.webhook.secret:}")
    private String webhookSecret;

}