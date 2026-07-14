package com.pr.review.reviewbot.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/index")
public class IndexController {

    private final CodeIndexer codeIndexer;

    @PostMapping
    public ResponseEntity<String> indexRepo(
            @RequestParam String repoName,
            @RequestParam String localPath) {

        new Thread(() -> {
            try {
                codeIndexer.indexRepository(repoName, localPath);
            } catch (Exception e) {
                log.error("Indexing failed: {}", e.getMessage());
            }
        }).start();

        return ResponseEntity.ok("Indexing started for: " + repoName);
    }
}
