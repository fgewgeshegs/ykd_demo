package org.example.wechatilink.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * iLink Bot 配置属性，映射 application.yaml 中的 ilink.bot 前缀。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ilink.bot")
public class ILinkConfigProperties {

    /** 连接超时（毫秒），默认 35 秒适配长轮询 */
    private int connectTimeoutMs = 35000;

    /** 读取超时（毫秒） */
    private int readTimeoutMs = 35000;

    /** 写入超时（毫秒） */
    private int writeTimeoutMs = 35000;

    /** HTTP 最大重试次数 */
    private int httpMaxRetries = 3;

    /** 重试基础延迟（毫秒） */
    private int retryBaseDelayMs = 1000;

    /** 重试最大延迟（毫秒） */
    private int retryMaxDelayMs = 10000;

    /** 是否启用心跳 */
    private boolean heartbeatEnabled = true;

    /** 心跳间隔（毫秒） */
    private int heartbeatIntervalMs = 30000;

    /** 消息轮询间隔（毫秒），两次 getUpdates 之间的等待 */
    private long pollIntervalMs = 1000;

    /** 是否在应用启动时自动启动 Bot，默认 true */
    private boolean autoStart = true;

    /** 天气查询城市（wttr.in 格式），默认 Beijing */
    private String weatherCity = "Beijing";

    /**
     * 固定回复内容模板。
     * 支持占位符: {text}=用户消息, {time}=当前时间, {weather}=天气
     * 例如: "你说了: {text}，这是自动回复 🤖"
     */
    private String replyTemplate = "收到你的消息啦！\n"
            + "你说：「{text}」\n\n"
            + "⏰ 现在时间：{time}\n"
            + "🌤️ 今日天气：{weather}\n\n"
            + "🤖 这是 iLink Bot 的自动回复~";

    /** 首次关注/打招呼的欢迎语 */
    private String welcomeMessage = "👋 你好！我是基于微信 iLink 协议的 Bot。\n\n"
            + "目前我处于固定回复模式，会对你发的每条消息做出相同的回应。\n"
            + "发送任意消息试试吧！";

    // ==================== AI 配置 ====================

    /** 是否启用 AI 回复（false 则使用固定模板） */
    private boolean aiEnabled = true;

    /** AI API Key */
    private String aiApiKey = "";

    /** AI API Base URL */
    private String aiBaseUrl = "https://api.siliconflow.cn/v1";

    /** AI 模型名称 */
    private String aiModel = "deepseek-ai/DeepSeek-V3";

    /** AI 系统提示词 */
    private String aiSystemPrompt = "你是一个友好的微信助手，请用简洁自然的中文回复。";

    /** AI 回复最大 token 数 */
    private int aiMaxTokens = 1024;
}
