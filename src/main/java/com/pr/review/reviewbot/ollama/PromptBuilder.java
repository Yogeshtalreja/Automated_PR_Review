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
            You are a senior Java engineer doing a thorough code review.
            You must respond ONLY with a JSON array, no explanation, no markdown, no backticks.
            Each item in the array must have exactly these fields:
            - "line": the line number being commented on (int)
            - "severity": one of "info", "warning", or "error" (string)
            - "comment": your specific, actionable review comment (string)
                        
            Focus on:
            - Null pointer risks
            - Missing @Transactional where needed
            - Security issues
            - Violations of the project conventions below
            - Logic errors in the diff
                        
            If there are no issues, return an empty array: []
                        
            %s
            """;

    public String buildSystemPrompt() {
        String context = loadProjectContext();
        return String.format(SYSTEM_PROMPT_TEMPLATE, context);
    }

    public String buildUserPrompt(String filename, String patch) {
        return """
                Review the following changes in file: %s
                                
                %s
                """.formatted(filename, patch);
    }

    private String loadProjectContext() {
        try {
            Path contextPath = Paths.get(CONTEXT_FILE);
            if (Files.exists(contextPath)) {
                String content = Files.readString(contextPath);
                log.debug("Loaded project context from {}", CONTEXT_FILE);
                return "## Project Conventions\n" + content;
            } else {
                log.warn("No {} found in project root — reviews will have no project context", CONTEXT_FILE);
                return "";
            }
        } catch (IOException e) {
            log.error("Failed to read {}: {}", CONTEXT_FILE, e.getMessage());
            return "";
        }
    }

}
