package com.youkeda.exercise.claw;

import com.youkeda.exercise.claw.command.CommandHandler;
import com.youkeda.exercise.claw.exception.ClawException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Claw Assistant CLI 主控制器
 *
 * 负责：控制台输入接收、命令分发、异常统一处理
 */
@Slf4j
@Component
public class Main {

    private final Map<String, CommandHandler> commandHandlers = new ConcurrentHashMap<>();
    private final List<CommandHandler> handlerList;

    public Main(List<CommandHandler> handlerList) {
        this.handlerList = handlerList;
    }

    @PostConstruct
    public void init() {
        registerCommands();
        startCli();
    }

    /**
     * 注册所有支持的命令到命令映射表
     */
    private void registerCommands() {
        for (CommandHandler handler : handlerList) {
            String name = handler.getClass().getSimpleName()
                    .replace("Command", "")
                    .toLowerCase();
            commandHandlers.put(name, handler);
        }
        log.info("CLI 命令注册完成，共 {} 个命令", commandHandlers.size());
    }

    /**
     * 在新线程中启动 CLI 事件循环
     */
    private void startCli() {
        Thread cliThread = new Thread(() -> {
            System.out.println("Claw Assistant 启动成功");
            System.out.println();

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("> ");
                    if (!scanner.hasNextLine()) {
                        break;
                    }
                    String input = scanner.nextLine().trim();

                    if (input.isEmpty()) {
                        continue;
                    }

                    // 退出处理
                    if ("exit".equalsIgnoreCase(input) || "quit".equalsIgnoreCase(input)) {
                        System.out.println("再见！");
                        break;
                    }

                    processCommand(input);
                }
            }

            log.info("CLI 线程退出");
        }, "cli-thread");
        cliThread.setDaemon(false);
        cliThread.start();
    }

    /**
     * 处理用户输入的命令
     */
    private void processCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String commandName = parts[0].toLowerCase();

        CommandHandler handler = commandHandlers.get(commandName);
        if (handler == null) {
            System.out.println("未知命令: " + commandName);
            System.out.println("输入 help 查看支持的命令");
            return;
        }

        try {
            handler.execute(parts);
        } catch (ClawException e) {
            System.out.println("错误：" + e.getMessage());
        }
    }
}
