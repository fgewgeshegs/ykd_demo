package com.youkeda.exercise.claw.intent;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 意图分类结果
 *
 * 封装分类后的意图类型
 */
@Data
@AllArgsConstructor
public class IntentResult {

    /**
     * 分类结果意图
     */
    private Intent intent;
}