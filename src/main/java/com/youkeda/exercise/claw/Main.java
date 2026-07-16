package com.youkeda.exercise.claw;

import com.youkeda.exercise.claw.command.CommandHandler;
import com.youkeda.exercise.claw.command.HelpCommand;
import com.youkeda.exercise.claw.command.StatusCommand;
import com.youkeda.exercise.claw.command.VersionCommand;
import com.youkeda.exercise.claw.command.WeatherCommand;
import com.youkeda.exercise.claw.config.WeatherConfig;
import com.youkeda.exercise.claw.exception.ClawException;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Claw Assistant 主程序
 *
 * 命令行版本智能助手入口
 * 负责：程序启动、控制台输入接收、命令分发、异常统一处理
 */
public class Main {

    private final Map<String, CommandHandler> commandHandlers = new ConcurrentHashMap<>();
    private final WeatherConfig weatherConfig;
    private boolean running = true;

    public Main() {
        // 初始化配置
        this.weatherConfig = new WeatherConfig();
        // 注册命令
        registerCommands();
    }

    /**
     * 注册所有支持的命令到命令映射表
     */
    private void registerCommands() {
        commandHandlers.put("help", new HelpCommand());
        commandHandlers.put("version", new VersionCommand());
        commandHandlers.put("status", new StatusCommand());
        commandHandlers.put("weather", new WeatherCommand(weatherConfig));
    }

    /**
     * 启动 CLI 事件循环
     */
    public void start() {
        System.out.println("Claw Assistant 启动成功");
        System.out.println();

        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
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
    }

    /**
     * 处理用户输入的命令
     *
     * @param input 用户输入的命令行
     */
    private void processCommand(String input) {
        // 按空格分割命令和参数
        String[] parts = input.split("\\s+", 2);
        String commandName = parts[0].toLowerCase();

        // 查找命令处理器
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

    public static void main(String[] args) {
        try {
            Main app = new Main();
            app.start();
        } catch (Exception e) {
            System.err.println("程序启动失败: " + e.getMessage());
            System.exit(1);
        }
    }
}
