package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Function Calling 工具扩展接口
 *
 * 继承 Tool 接口，新增三个方法：
 * 1. 返回 OpenAI tools 格式的工具定义（JSON）
 * 2. 接收 JSON 参数执行工具，返回 JSON 结果
 *
 * 不支持 Function Calling 的工具无需实现此接口，原有 Tool 接口不变。
 */
public interface FunctionTool extends Tool {

    /**
     * 返回 OpenAI tools 格式的工具定义 JSON 字符串。
     *
     * 示例格式：
     * {
     *   "type": "function",
     *   "function": {
     *     "name": "get_weather",
     *     "description": "查询指定城市的当前天气信息",
     *     "parameters": {
     *       "type": "object",
     *       "properties": {
     *         "city": { "type": "string", "description": "城市名称" }
     *       },
     *       "required": ["city"]
     *     }
     *   }
     * }
     */
    String getToolDefinition();

    /**
     * 执行 Function Calling 调用
     *
     * @param arguments 大模型返回的 JSON 参数，如 {"city": "上海"}
     * @return JSON 格式的执行结果，如 {"city":"上海","weather":"晴","temperature":28}
     */
    String executeFunction(JsonNode arguments);
}
