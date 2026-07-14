package com.pr.review.reviewbot.rag;

import com.pr.review.reviewbot.ollama.OllamaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@RequiredArgsConstructor
@Service
@Slf4j
public class CodeIndexer {

    private final OllamaClient ollamaClient;
    private final CodeEmbeddingRepository repository;

    // File extensions worth indexing
    private static final List<String> INDEXABLE_EXTENSIONS =
            List.of(".java", ".xml", ".yml", ".yaml", ".properties");

    // Skip these folders — no useful code
    private static final List<String> SKIP_FOLDERS =
            List.of("build", "target", ".gradle", ".git", "node_modules");

    /**
     * Index an entire local repository.
     * Walks every file, generates embeddings, stores in pgvector.
     */
    public void indexRepository(String repoName, String localPath) throws IOException {
        log.info("🗂️ Starting indexing of repo: {} at {}", repoName, localPath);

        AtomicInteger indexed = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);

        Path rootPath = Paths.get(localPath);

        Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // Skip irrelevant folders
                String dirName = dir.getFileName().toString();
                if (SKIP_FOLDERS.contains(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.toString();

                // Only index relevant file types
                boolean shouldIndex = INDEXABLE_EXTENSIONS.stream()
                        .anyMatch(fileName::endsWith);

                if (!shouldIndex) {
                    return FileVisitResult.CONTINUE;
                }

                // Skip very large files — too much content for one embedding
                if (attrs.size() > 100_000) {
                    log.debug("Skipping large file: {}", fileName);
                    skipped.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }

                try {
                    String content = Files.readString(file);
                    String relativePath = rootPath.relativize(file).toString();

                    indexFile(repoName, relativePath, content);
                    indexed.incrementAndGet();

                    if (indexed.get() % 10 == 0) {
                        log.info("📄 Indexed {} files so far...", indexed.get());
                    }

                } catch (Exception e) {
                    log.warn("Failed to index {}: {}", fileName, e.getMessage());
                    skipped.incrementAndGet();
                }

                return FileVisitResult.CONTINUE;
            }
        });

        log.info("✅ Indexing complete — {} files indexed, {} skipped",
                indexed.get(), skipped.get());
    }

    /**
     * Index a single file — generates embedding and upserts into database.
     */
    public void indexFile(String repo, String filepath, String content) {
        // Truncate very long files — embedding models have token limits
        String truncated = content.length() > 8000
                ? content.substring(0, 8000)
                : content;

        // Generate embedding via Ollama
        List<Float> embeddingList = ollamaClient.generateEmbedding(truncated);

        // Convert to float array
        float[] embedding = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i);
        }

        // Upsert — update if exists, insert if new
        CodeEmbedding existing = repository.findByRepoAndFilepath(repo, filepath);

        if (existing != null) {
            existing.setContent(truncated);
            existing.setEmbedding(embedding);
            existing.setCreatedAt(LocalDateTime.now());
            repository.save(existing);
        } else {
            repository.save(CodeEmbedding.builder()
                    .repo(repo)
                    .filepath(filepath)
                    .content(truncated)
                    .embedding(embedding)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }
}
