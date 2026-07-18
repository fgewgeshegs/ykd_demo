package com.youkeda.exercise.claw.ai.llm;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 系统提示词提供者
 *
 * 从 resources/prompts/system-prompt.txt 读取系统提示词
 */
@Slf4j
@Component
public class SystemPromptProvider {

    private String systemPrompt;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/system-prompt.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                this.systemPrompt = reader.lines().collect(Collectors.joining("\n"));
            }
            log.info("系统提示词加载完成，共 {} 字符", systemPrompt.length());
        } catch (Exception e) {
            log.error("加载系统提示词失败", e);
            this.systemPrompt = "你是 Claw助手，一个智能AI助手。";
        }
    }

    /**
     * 获取系统提示词
     *
     * @return 系统提示词内容
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }
}
