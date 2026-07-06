package com.pr.review.reviewbot.github;


import com.pr.review.reviewbot.PullRequestFile;
import lombok.RequiredArgsConstructor;
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

    public List<PullRequestFile> fetchPRFiles(String owner, String repo, int prNumber){

    log.debug("Fetching PR files: {}/{} #{}", owner, repo, prNumber);

        return githubWebClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{pr}/files",
                        owner, repo, prNumber)
                .retrieve()
                .onStatus(status -> status.value() == 401,
                        r -> Mono.error(new RuntimeException("Invalid GitHub token — check application-local.properties")))
                .onStatus(status -> status.value() == 404,
                        r -> Mono.error(new RuntimeException("PR not found: " + prNumber)))
                .onStatus(status -> status.value() == 403,
                        r -> Mono.error(new RuntimeException("GitHub rate limit exceeded — wait 1 hour or use a token")))
                .bodyToFlux(PullRequestFile.class)
                .collectList()
                .block();
    }

}
