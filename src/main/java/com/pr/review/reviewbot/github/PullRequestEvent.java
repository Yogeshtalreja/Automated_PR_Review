package com.pr.review.reviewbot.github;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PullRequestEvent {

    private String owner;
    private String repo;
    private int prNumber;
    private String prTitle;
    private String action;

}
