package com.youkeda.exercise.claw.command;

import com.youkeda.exercise.claw.exception.ClawException;

/**
 * 命令处理器接口
 * 所有命令类都需要实现此接口
 */
public interface CommandHandler {

    /**
     * 执行命令
     *
     * @param args 命令参数数组
     * @throws ClawException 执行过程中发生的业务异常
     */
    void execute(String[] args) throws ClawException;
}
