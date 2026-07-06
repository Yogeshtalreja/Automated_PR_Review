package com.pr.review.reviewbot.ollama;


import lombok.Data;

@Data
public class ReviewComment {

    private int line;
    private String severity;
    private String comment;

}
