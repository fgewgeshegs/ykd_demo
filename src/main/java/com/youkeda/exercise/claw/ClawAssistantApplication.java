package com.youkeda.exercise.claw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.Charset;

/**
 * Claw Assistant Spring Boot 入口
 *
 * 自动扫描 com.youkeda.exercise.claw 包及其子包下的所有组件
 */
@SpringBootApplication
public class ClawAssistantApplication {

    public static void main(String[] args) {
        // 检测 JVM 默认编码：Windows 中文系统默认为 GBK，可能导致 ZIP 解压或文件读取乱码
        // 若当前编码非 UTF-8，建议添加 JVM 启动参数：-Dfile.encoding=UTF-8
        Charset defaultCharset = Charset.defaultCharset();
        if (!"UTF-8".equalsIgnoreCase(defaultCharset.name())) {
            System.out.println("⚠ 当前 JVM 默认编码为 " + defaultCharset.name()
                    + "，建议使用 -Dfile.encoding=UTF-8 启动以避免中文乱码");
        }
        System.out.println("Claw Assistant 启动中，默认编码: " + defaultCharset.name());

        SpringApplication.run(ClawAssistantApplication.class, args);
    }
}
