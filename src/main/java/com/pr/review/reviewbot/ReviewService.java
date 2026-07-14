package com.pr.review.reviewbot;


import com.pr.review.reviewbot.github.GithubClient;
import com.pr.review.reviewbot.github.GithubReviewRequest;
import com.pr.review.reviewbot.github.PullRequestEvent;
import com.pr.review.reviewbot.ollama.OllamaClient;
import com.pr.review.reviewbot.ollama.ReviewComment;
import com.pr.review.reviewbot.rag.CodeEmbedding;
import com.pr.review.reviewbot.rag.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final GithubClient githubClient;
    private final OllamaClient ollamaClient;
    private final CommentMapper commentMapper;
    private final RagService ragService;


    @Async
    public void processReview(PullRequestEvent event) {
        log.info("🔍 Starting RAG-powered review for PR #{} - '{}'",
                event.getPrNumber(), event.getPrTitle());

        try {
            var files = githubClient.fetchPRFiles(
                    event.getOwner(),
                    event.getRepo(),
                    event.getPrNumber()
            );

            log.info("📁 Found {} changed files", files.size());

            List<GithubReviewRequest.InlineComment> allComments = files.stream()
                    .filter(f -> f.getPatch() != null)
                    .flatMap(f -> {
                        log.info("📝 Reviewing: {}", f.getFilename());

                        // Step 1 — Find relevant files from indexed codebase
                        List<CodeEmbedding> relevantFiles = ragService.findRelevantFiles(
                                event.getOwner() + "/" + event.getRepo(),
                                f.getFilename() + "\n" + f.getPatch(),
                                5  // top 5 most relevant files
                        );

                        // Step 2 — Build context string from relevant files
                        String ragContext = ragService.buildContext(relevantFiles);

                        log.info("   📚 Injecting {} relevant files as context",
                                relevantFiles.size());

                        // Step 3 — Review with full context
                        List<ReviewComment> comments =
                                ollamaClient.reviewCode(f.getFilename(), f.getPatch(), ragContext);

// Add this log
                        log.info("   💬 Got {} raw comments for {}", comments.size(), f.getFilename());

                        return comments.stream()
                                .filter(c -> {
                                    boolean valid = c.getComment() != null && !c.getComment().isBlank();
                                    if (!valid) log.debug("Filtered out comment with null/blank content");
                                    return valid;
                                })
                                .map(c -> commentMapper.toInlineComment(c, f))
                                .peek(opt -> {
                                    if (opt.isEmpty()) log.debug("CommentMapper returned empty for a comment");
                                })
                                .filter(Optional::isPresent)
                                .map(Optional::get);
                    })
                    .toList();

            if (allComments.isEmpty()) {
                log.info("✅ No issues found in PR #{}", event.getPrNumber());
                postGeneralComment(event, "✅ No issues found — looks good!");
                return;
            }

            long errors   = allComments.stream()
                    .filter(c -> c.getBody().contains("ERROR")).count();
            long warnings = allComments.stream()
                    .filter(c -> c.getBody().contains("WARNING")).count();

            String summary = String.format(
                    "🤖 **AI Code Review (RAG-powered)** — %d issue(s): %d 🔴 errors, %d 🟡 warnings",
                    allComments.size(), errors, warnings
            );

            GithubReviewRequest review = GithubReviewRequest.builder()
                    .event("COMMENT")
                    .body(summary)
                    .comments(allComments)
                    .build();

            githubClient.postReview(
                    event.getOwner(),
                    event.getRepo(),
                    event.getPrNumber(),
                    review
            );

            log.info("✅ RAG review posted to PR #{} — {} comments",
                    event.getPrNumber(), allComments.size());

        } catch (Exception e) {
            log.error("❌ Review failed for PR #{}: {}",
                    event.getPrNumber(), e.getMessage());
        }
    }

    private void postGeneralComment(PullRequestEvent event, String message) {
        GithubReviewRequest review = GithubReviewRequest.builder()
                .event("COMMENT")
                .body(message)
                .comments(List.of())
                .build();

        githubClient.postReview(
                event.getOwner(),
                event.getRepo(),
                event.getPrNumber(),
                review
        );
    }
}
