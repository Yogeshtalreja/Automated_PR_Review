package com.pr.review.reviewbot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PullRequestFile {
    private String fileName;
    private String status;


    private int additions;
    private int deletions;
    private int changes;

    private String patch;

    @JsonProperty("blob_url")
    private String blobUrl;

}
