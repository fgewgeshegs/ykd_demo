package com.youkeda.exercise.claw.command;

import com.youkeda.exercise.claw.exception.ClawException;
import org.springframework.stereotype.Component;

/**
 * version 命令
 * 显示当前程序版本
 */
@Component
public class VersionCommand implements CommandHandler {

    private static final String VERSION = "0.1.0";

    @Override
    public void execute(String[] args) throws ClawException {
        System.out.println("Claw Assistant v" + VERSION);
    }
}
