package com.pr.review.reviewbot.rag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CodeEmbeddingRepository extends JpaRepository<CodeEmbedding, Long> {


    CodeEmbedding findByRepoAndFilepath(String repo, String filepath);

    @Modifying
    @Transactional
    void deleteByRepo(String repo);

    // Count files for a repo — to check if indexed
    long countByRepo(String repo);

}
