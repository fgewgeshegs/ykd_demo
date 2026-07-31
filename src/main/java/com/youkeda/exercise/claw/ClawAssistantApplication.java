package com.youkeda.exercise.claw;

import com.youkeda.exercise.claw.core.InstanceLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>启动时通过 {@link InstanceLockManager} 获取进程锁，保证同一目录下只有一个实例运行，
 * 避免多实例同时轮询微信消息导致重复回复。
 *
 * @see com.youkeda.exercise.claw.application.ClawAssistantApplication
 */
@SpringBootApplication(scanBasePackages = "com.youkeda.exercise.claw")
@EnableScheduling
@ConfigurationPropertiesScan("com.youkeda.exercise.claw")
public class ClawAssistantApplication {

    private static final Logger log = LoggerFactory.getLogger(ClawAssistantApplication.class);

    public static void main(String[] args) {
        // 单实例保护：获取失败说明已有实例在运行，直接退出，不启动任何 Spring/Bot 逻辑
        InstanceLockManager lockManager = new InstanceLockManager();
        if (!lockManager.acquireLock()) {
            System.out.println("已有 ClawAssistant 实例运行，当前实例退出。");
            System.exit(0);
            return;
        }
        // JVM 退出（含 Ctrl+C、正常退出）时释放锁；进程被 kill 时 OS 自动释放
        Runtime.getRuntime().addShutdownHook(
                new Thread(lockManager::releaseLock, "instance-lock-release"));

        SpringApplication.run(ClawAssistantApplication.class, args);

        log.info("ClawAssistant started. instance={}", InstanceLockManager.instanceId());
    }
}