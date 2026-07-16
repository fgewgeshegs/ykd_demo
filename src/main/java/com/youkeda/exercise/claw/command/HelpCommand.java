package com.youkeda.exercise.claw.command;

import com.youkeda.exercise.claw.exception.ClawException;
import org.springframework.stereotype.Component;

/**
 * help 命令
 * 显示所有支持的命令列表
 */
@Component
public class HelpCommand implements CommandHandler {

    @Override
    public void execute(String[] args) throws ClawException {
        System.out.println("支持命令：");
        System.out.println();
        System.out.println("help      查看帮助");
        System.out.println("version   查看版本");
        System.out.println("status    查看程序状态");
        System.out.println("weather   查询天气");
    }
}
