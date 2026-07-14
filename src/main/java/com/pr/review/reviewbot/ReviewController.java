package com.pr.review.reviewbot;

import com.pr.review.reviewbot.diff.DiffParser;
import com.pr.review.reviewbot.github.GithubClient;
import com.pr.review.reviewbot.ollama.OllamaClient;
import com.pr.review.reviewbot.ollama.ReviewComment;
import com.pr.review.reviewbot.rag.CodeEmbedding;
import com.pr.review.reviewbot.rag.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final GithubClient gitHubClient;
    private final OllamaClient ollamaClient;
    private final RagService ragService;


    @GetMapping("/review")
    public Map<String, Object> review(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam int pr,
            @RequestParam(defaultValue = "info") String minSeverity) {

        String fullRepoName = owner + "/" + repo;

        List<Map<String, Object>> fileReviews = gitHubClient
                .fetchPRFiles(owner, repo, pr).stream()
                .filter(f -> f.getPatch() != null)
                .map(f -> {
                    List<CodeEmbedding> relevantFiles = ragService.findRelevantFiles(
                            fullRepoName,
                            f.getFilename() + "\n" + f.getPatch(),
                            5
                    );

                    // Build RAG context
                    String ragContext = ragService.buildContext(relevantFiles);


                    List<ReviewComment> comments = ollamaClient
                            .reviewCode(f.getFilename(), f.getPatch(), ragContext)
                            .stream()
                            .filter(c -> c.getComment() != null && !c.getComment().isBlank())
                            .filter(c -> severityLevel(c.getSeverity()) >= severityLevel(minSeverity))
                            .toList();

                    Map<String, Object> result = new HashMap<>();
                    result.put("filename",  f.getFilename());
                    result.put("status",    f.getStatus() != null ? f.getStatus() : "unknown");
                    result.put("additions", f.getAdditions());
                    result.put("deletions", f.getDeletions());
                    result.put("comments",  comments);
                    return result;
                })
                .filter(f -> !((List<?>) f.get("comments")).isEmpty())
                .toList();

        // Summary at the top level
        Map<String, Object> response = new HashMap<>();
        response.put("pr",           pr);
        response.put("repo",         owner + "/" + repo);
        response.put("minSeverity",  minSeverity);
        response.put("filesReviewed", fileReviews.size());
        response.put("totalComments", fileReviews.stream()
                .mapToInt(f -> ((List<?>) f.get("comments")).size())
                .sum());
        response.put("files", fileReviews);
        return response;
    }

    // info=0, warning=1, error=2
    private int severityLevel(String severity) {
        if (severity == null) return 0;
        return switch (severity.toLowerCase()) {
            case "warning" -> 1;
            case "error"   -> 2;
            default        -> 0;  // info or anything unexpected
        };
    }
}


