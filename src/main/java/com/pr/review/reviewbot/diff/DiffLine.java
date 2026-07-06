package com.pr.review.reviewbot.diff;

public record DiffLine(
        int fileLineNumber,   // actual line number in the NEW file
        int diffPosition,     // position within the patch — needed later for posting GitHub comments
        String type,          // ADDED | REMOVED | CONTEXT
        String content
) {
}
