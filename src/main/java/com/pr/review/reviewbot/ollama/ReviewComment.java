package com.pr.review.reviewbot.ollama;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewComment {

    private int line;
    private String severity = "info";
    private String comment;

}
