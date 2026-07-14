package com.pr.review.reviewbot.github;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GithubReviewRequest {

    private String event = "comment";
    private String body;
    private List<InlineComment> comments;

    @Data
    @Builder
    public static class InlineComment {
        private String path;
        private int position;
        private String body;

        @JsonProperty("path")
        public String getPath() { return path; }

        @JsonProperty("position")
        public int getPosition() { return position; }

        @JsonProperty("body")
        public String getBody() { return body; }
    }


}
