package com.youkeda.exercise.claw.agent.skill;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class DocumentChunker {

    private static final int DEFAULT_CHUNK_SIZE = 2000;
    private static final int DEFAULT_OVERLAP = 250;

    public List<Chunk> chunkMarkdown(String text, String documentId, String source) {
        if (text == null || text.isBlank()) return List.of();

        List<Chunk> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        String currentHeading = "";
        int chunkIndex = 0;
        int charCount = 0;

        for (String line : text.split("\n")) {
            if (line.matches("^#{2,3}\\s.+")) {
                if (currentChunk.length() > 0) {
                    chunks.add(new Chunk(documentId, chunkIndex++, currentChunk.toString().trim(), source, currentHeading));
                }
                currentHeading = line.replaceAll("^#+\\s*", "").trim();
                currentChunk = new StringBuilder();
                currentChunk.append(line).append("\n");
                charCount = line.length();
            } else {
                currentChunk.append(line).append("\n");
                charCount += line.length() + 1;
                if (charCount > DEFAULT_CHUNK_SIZE) {
                    chunks.add(new Chunk(documentId, chunkIndex++, currentChunk.toString().trim(), source, currentHeading));
                    String current = currentChunk.toString();
                    int overlapStart = Math.max(0, current.length() - DEFAULT_OVERLAP);
                    int lastNewline = current.lastIndexOf('\n', overlapStart + DEFAULT_OVERLAP);
                    currentChunk = lastNewline > 0 ? new StringBuilder(current.substring(lastNewline + 1)) : new StringBuilder();
                    charCount = currentChunk.length();
                }
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(new Chunk(documentId, chunkIndex, currentChunk.toString().trim(), source, currentHeading));
        }
        return chunks;
    }

    public List<Chunk> chunkPlainText(String text, String documentId, String source) {
        if (text == null || text.isBlank()) return List.of();

        List<Chunk> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int charCount = 0;

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;
            if (charCount + trimmed.length() > DEFAULT_CHUNK_SIZE && currentChunk.length() > 0) {
                chunks.add(new Chunk(documentId, chunkIndex++, currentChunk.toString().trim(), source, ""));
                currentChunk = new StringBuilder();
                charCount = 0;
            }
            currentChunk.append(trimmed).append("\n\n");
            charCount += trimmed.length() + 2;
        }

        if (currentChunk.length() > 0) {
            chunks.add(new Chunk(documentId, chunkIndex, currentChunk.toString().trim(), source, ""));
        }
        return chunks;
    }

    public record Chunk(String documentId, int chunkIndex, String content, String source, String heading) {}
}
