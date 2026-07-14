package com.pr.review.reviewbot.ollama;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class PromptBuilder {

    private static final String CONTEXT_FILE = "CLAUDE_CONTEXT.md";

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        You are a senior Java engineer doing a code review.
        You must respond ONLY with a single valid JSON array.
        Do not split into multiple arrays.
        Do not include any explanation, markdown, backticks, or text outside the array.
        Start your response with [ and end with ].
        All comments must be inside ONE single array.
        Each item must have exactly these three fields:
        - "line": integer line number
        - "severity": exactly one of "info", "warning", or "error"
        - "comment": non-null string with your review comment
        Correct format example:
        [{"line":1,"severity":"warning","comment":"Missing null check"},{"line":5,"severity":"error","comment":"No @Transactional"}]
        If there are no issues return exactly: []
        
        %s
        """;

    public String buildSystemPrompt() {
        String context = loadProjectContext();
        return String.format(SYSTEM_PROMPT_TEMPLATE, context);
    }

    public String buildUserPrompt(String filename, String patch, String ragContext) {
        return """
                %s
                
                Now review the following changes in file: %s
                
                %s
                """.formatted(ragContext, filename, patch);
    }

    public String buildUserPrompt(String filename, String patch) {
        return buildUserPrompt(filename, patch, "");
    }


    private String loadProjectContext() {
        try {
            Path contextPath = Paths.get(CONTEXT_FILE);
            if (Files.exists(contextPath)) {
                String content = Files.readString(contextPath);
                log.debug("Loaded project context from {}", CONTEXT_FILE);
                return "## Project Conventions\n" + content;
            } else {
                log.warn("No {} found — reviews will have no project context",
                        CONTEXT_FILE);
                return "";
            }
        } catch (IOException e) {
            log.error("Failed to read {}: {}", CONTEXT_FILE, e.getMessage());
            return "";
        }
    }
}
