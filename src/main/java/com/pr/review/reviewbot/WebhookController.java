package com.pr.review.reviewbot;

import com.pr.review.reviewbot.config.GithubProperties;
import com.pr.review.reviewbot.config.HmacValidator;
import com.pr.review.reviewbot.github.PullRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhookController {

    private static final Set<String> TRIGGER_ACTIONS =
            Set.of("opened", "synchronize", "reopened");

    private final HmacValidator hmacValidator;
    private final GithubProperties gitHubProperties;
    private final ObjectMapper objectMapper;
    private final ReviewService reviewService;

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

        try {
            JsonNode json = objectMapper.readTree(payload);

            if ("ping".equals(event)) {
                log.info("✅ GitHub ping received");
                return ResponseEntity.ok("Pong!");
            }

            if ("pull_request".equals(event)) {
                String action  = json.path("action").asText();
                int prNumber   = json.path("number").asInt();
                String prTitle = json.path("pull_request").path("title").asText();
                String owner   = json.path("repository").path("owner").path("login").asText();
                String repo    = json.path("repository").path("name").asText();

                log.info("📨 PR #{} '{}' — action: {}", prNumber, prTitle, action);

                if (TRIGGER_ACTIONS.contains(action)) {
                    // Build the event and trigger async review
                    PullRequestEvent prEvent = PullRequestEvent.builder()
                            .owner(owner)
                            .repo(repo)
                            .prNumber(prNumber)
                            .prTitle(prTitle)
                            .action(action)
                            .build();

                    // This returns immediately — review runs in background
                    reviewService.processReview(prEvent);

                    log.info("🚀 Review triggered for PR #{}", prNumber);
                    return ResponseEntity.ok("Review started for PR #" + prNumber);
                }
                return ResponseEntity.ok("Action '" + action + "' ignored");
            }

            return ResponseEntity.ok("Event ignored: " + event);

        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

}
