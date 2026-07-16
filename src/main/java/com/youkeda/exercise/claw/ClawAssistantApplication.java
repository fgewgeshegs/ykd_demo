package com.youkeda.exercise.claw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Claw Assistant Spring Boot 入口
 *
 * 自动扫描 com.youkeda.exercise.claw 包及其子包下的所有组件
 */
@SpringBootApplication
public class ClawAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClawAssistantApplication.class, args);
    }
}
