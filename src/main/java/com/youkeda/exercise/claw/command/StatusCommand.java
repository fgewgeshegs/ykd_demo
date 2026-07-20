package com.youkeda.exercise.claw.command;

import com.youkeda.exercise.claw.ClawException;
import org.springframework.stereotype.Component;

/**
 * status 命令
 * 显示程序运行状态
 */
@Component
public class StatusCommand implements CommandHandler {

    @Override
    public void execute(String[] args) throws ClawException {
        String javaVersion = System.getProperty("java.version");

        System.out.println("系统状态：");
        System.out.println("运行正常");
        System.out.println("Java版本：" + javaVersion);
    }
}
