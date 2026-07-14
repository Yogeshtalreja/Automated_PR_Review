package com.pr.review.reviewbot.rag;

import com.pr.review.reviewbot.ollama.OllamaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {
    private final CodeEmbeddingRepository repository;
    private final OllamaClient ollamaClient;
    private final JdbcTemplate jdbcTemplate;


    /**
     * Finds the most relevant files from the indexed codebase
     * based on semantic similarity to the changed file's diff.
     *
     * Why this works: if PaymentService.java is being changed,
     * its diff will be semantically similar to Order.java,
     * PaymentRepository.java etc. — pgvector finds them instantly.
     */
    public List<CodeEmbedding> findRelevantFiles(
            String repo, String diffContent, int topK) {

        log.debug("Searching for relevant files in repo: {}", repo);

        // Check if repo is indexed at all
        long count = repository.countByRepo(repo);
        if (count == 0) {
            log.warn("No indexed files found for repo: {} — skipping RAG", repo);
            return List.of();
        }

        // Generate embedding for the diff
        List<Float> queryEmbedding = ollamaClient.generateEmbedding(diffContent);

        // Convert to pgvector format: [0.1,0.2,0.3,...]
        String embeddingStr = queryEmbedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));

        // Use JdbcTemplate for native vector query
        String sql = """
                SELECT id, repo, filepath, content, created_at
                FROM code_embeddings
                WHERE repo = ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;

        try {
            List<CodeEmbedding> results = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> {
                        CodeEmbedding ce = new CodeEmbedding();
                        ce.setId(rs.getLong("id"));
                        ce.setRepo(rs.getString("repo"));
                        ce.setFilepath(rs.getString("filepath"));
                        ce.setContent(rs.getString("content"));
                        return ce;
                    },
                    repo, embeddingStr, topK
            );

            log.debug("Found {} relevant files", results.size());
            results.forEach(r -> log.debug("  → {}", r.getFilepath()));
            return results;

        } catch (Exception e) {
            log.error("Similarity search failed: {} — returning empty", e.getMessage());
            return List.of();
        }
    }

    /**
     * Builds a context string from relevant files to inject into the prompt.
     * Truncates each file to avoid exceeding Ollama's context window.
     */
    public String buildContext(List<CodeEmbedding> relevantFiles) {
        if (relevantFiles.isEmpty()) return "";

        StringBuilder context = new StringBuilder();
        context.append("--- RELEVANT PROJECT FILES FOR CONTEXT ---\n\n");

        for (CodeEmbedding file : relevantFiles) {
            context.append("// File: ").append(file.getFilepath()).append("\n");

            // Truncate each file to 1500 chars — enough for context, not too much
            String content = file.getContent().length() > 1500
                    ? file.getContent().substring(0, 1500) + "\n... (truncated)"
                    : file.getContent();

            context.append(content).append("\n\n");
        }

        context.append("--- END OF CONTEXT ---\n");
        return context.toString();
    }
}

