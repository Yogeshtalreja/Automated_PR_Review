package com.pr.review.reviewbot;

import com.pr.review.reviewbot.diff.DiffParser;
import com.pr.review.reviewbot.github.GithubClient;
import com.pr.review.reviewbot.ollama.OllamaClient;
import com.pr.review.reviewbot.ollama.ReviewComment;
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
@CrossOrigin
public class ReviewController {

    private final GithubClient githubClient;
    private final OllamaClient ollamaClient;

    @GetMapping("/review")
    public List<Map<String, Object>> review(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam int pr) {

        return githubClient.fetchPRFiles(owner, repo, pr).stream()
                .filter(f -> f.getPatch() != null)
                .map(f -> {

                    List<ReviewComment> comments = ollamaClient.reviewCode(f.getFileName(), f.getPatch());

                    Map<String, Object> result = new HashMap<>();
                    result.put("filename",  f.getFileName());
                    result.put("status",    f.getStatus() != null ? f.getStatus() : "unknown");
                    result.put("additions", f.getAdditions());
                    result.put("deletions", f.getDeletions());
                    result.put("lines",     DiffParser.parse(f.getPatch()));
                    result.put("comments", comments);
                    return result;
                })
                .toList();
    }

}
