package com.youkeda.exercise.claw.intent;

/**
 * 意图分类器接口
 *
 * 根据用户文本消息判断用户意图
 */
public interface IntentClassifier {

    /**
     * 对用户消息进行意图分类
     *
     * @param message 用户消息文本
     * @return 意图分类结果
     */
    IntentResult classify(String message);
}