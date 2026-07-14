package com.pr.review.reviewbot.github;


import com.pr.review.reviewbot.PullRequestFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class GithubClient {

    private final WebClient githubWebClient;

    public GithubClient(@Qualifier("gitHubWebClient") WebClient githubWebClient) {
        this.githubWebClient = githubWebClient;
    }

    public List<PullRequestFile> fetchPRFiles(
            String owner, String repo, int prNumber) {

        log.info("Fetching PR files: {}/{} #{}", owner, repo, prNumber);

        List<PullRequestFile> files = githubWebClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{pr}/files",
                        owner, repo, prNumber)
                .retrieve()
                .onStatus(status -> status.value() == 401,
                        r -> Mono.error(new RuntimeException("Invalid GitHub token")))
                .onStatus(status -> status.value() == 404,
                        r -> Mono.error(new RuntimeException("PR not found: " + prNumber)))
                .onStatus(status -> status.value() == 403,
                        r -> Mono.error(new RuntimeException("GitHub rate limit exceeded")))
                .bodyToFlux(PullRequestFile.class)
                .collectList()
                .block();

        // Log first file to verify mapping
        if (files != null && !files.isEmpty()) {
            log.debug("First file mapped: filename={}, status={}",
                    files.get(0).getFilename(),
                    files.get(0).getStatus());
        }

        return files;
    }


    public void postReview(String owner, String repo, int prNumber, GithubReviewRequest review){

        try {
            githubWebClient.post()
                    .uri("/repos/{owner}/{repo}/pulls/{pr}/reviews",
                            owner, repo, prNumber)
                    .bodyValue(review)
                    .retrieve()
                    .onStatus(status -> status.value() == 422,
                            r -> Mono.error(new RuntimeException(
                                    "Invalid review — check diff positions")))
                    .bodyToMono(String.class)
                    .block();

            log.info("✅ Posted review to PR #{}", prNumber);
        } catch (Exception e) {
            log.error("❌ Failed to post review to PR #{}: {}", prNumber, e.getMessage());
        }

    }

}
