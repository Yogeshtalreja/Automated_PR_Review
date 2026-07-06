package com.pr.review;

import com.pr.review.reviewbot.config.GithubProperties;
import com.pr.review.reviewbot.config.OllamaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GithubProperties.class, OllamaProperties.class})
public class PrApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrApplication.class, args);
	}

}
