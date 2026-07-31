package com.youkeda.exercise.claw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Claw Assistant Spring Boot 入口（兼容位置）
 *
 * <p>合并 czd-tools 后主类迁移到了 {@code application} 子包。
 * 此类保留原包路径，方便 IntelliJ 旧 Run Configuration 直接使用。
 *
 * <p>显式指定 {@code scanBasePackages} 确保扫描整个 {@code com.youkeda.exercise.claw} 树。
 *
 * @see com.youkeda.exercise.claw.application.ClawAssistantApplication
 */
@SpringBootApplication(scanBasePackages = "com.youkeda.exercise.claw")
@EnableScheduling
@ConfigurationPropertiesScan("com.youkeda.exercise.claw")
public class ClawAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClawAssistantApplication.class, args);
    }
}