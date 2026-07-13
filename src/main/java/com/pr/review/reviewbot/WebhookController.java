package com.pr.review.reviewbot;

import com.pr.review.reviewbot.config.GithubProperties;
import com.pr.review.reviewbot.config.HmacValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final HmacValidator hmacValidator;
    private final GithubProperties gitHubProperties;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
        @RequestHeader(value = "X-GitHub-Event", defaultValue = "unknown") String event,
        @RequestHeader(value = "X-Hub-Signature-256", defaultValue = "none") String signature,
        @RequestBody String payload
    ) {

        if (!hmacValidator.isValid(payload, signature, gitHubProperties.getWebhookSecret())) {
            log.warn("❌ Invalid webhook signature — request rejected");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid signature");
        }

        log.info("✅ Signature valid — processing event: {}", event);

        log.info("Received webhook event: {}", event);
        log.info("Webhook payload: {}", payload);
        return ResponseEntity.ok("Webhook received successfully");
    }

}
