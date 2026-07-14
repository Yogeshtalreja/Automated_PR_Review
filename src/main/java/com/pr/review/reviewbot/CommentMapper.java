package com.pr.review.reviewbot;


import com.pr.review.reviewbot.diff.DiffLine;
import com.pr.review.reviewbot.diff.DiffParser;
import com.pr.review.reviewbot.github.GithubReviewRequest;
import com.pr.review.reviewbot.ollama.ReviewComment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CommentMapper {

    public Optional<GithubReviewRequest.InlineComment> toInlineComment(
            ReviewComment reviewComment,
            PullRequestFile file) {

        if (file.getPatch() == null) return Optional.empty();

        List<DiffLine> diffLines = DiffParser.parse(file.getPatch());

        // Log available line numbers for debugging
        log.debug("Looking for line {} in diff. Available lines: {}",
                reviewComment.getLine(),
                diffLines.stream()
                        .map(l -> l.fileLineNumber() + "(" + l.type() + ")")
                        .toList());

        Optional<DiffLine> matchedLine = diffLines.stream()
                .filter(l -> l.fileLineNumber() == reviewComment.getLine())
                .filter(l -> "ADDED".equals(l.type()) || "CONTEXT".equals(l.type()))
                .findFirst();

        if (matchedLine.isEmpty()) {
            matchedLine = diffLines.stream()
                    .filter(l -> "ADDED".equals(l.type()))
                    .reduce((first, second) -> second); // get last ADDED line
        }

        if (matchedLine.isEmpty()) {
            log.debug("No suitable line found for comment — dropping");
            return Optional.empty();
        }


        String emoji = switch (reviewComment.getSeverity().toLowerCase()) {
            case "error"   -> "🔴";
            case "warning" -> "🟡";
            default        -> "🔵";
        };

        return Optional.of(GithubReviewRequest.InlineComment.builder()
                .path(file.getFilename())
                .position(matchedLine.get().diffPosition())
                .body(emoji + " **" + reviewComment.getSeverity().toUpperCase()
                        + "**: " + reviewComment.getComment())
                .build());
    }

}
