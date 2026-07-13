package com.pr.review.reviewbot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PullRequestFile {

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("status")
    private String status;

    @JsonProperty("additions")
    private int additions;

    @JsonProperty("deletions")
    private int deletions;

    @JsonProperty("changes")
    private int changes;

    @JsonProperty("patch")
    private String patch;

    @JsonProperty("blob_url")
    private String blobUrl;
}
