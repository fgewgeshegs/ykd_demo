package com.youkeda.exercise.claw.wechat.client;

import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import com.youkeda.exercise.claw.wechat.login.BotInstance;
import com.youkeda.exercise.claw.wechat.login.BotManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 微信 iLink 客户端薄封装。
 * 委托 BotManager 管理 BotInstance，聚焦消息收发。
 */
@Component
public class WechatILinkClient {

    private static final Logger log = LoggerFactory.getLogger(WechatILinkClient.class);

    private final BotManager botManager;

    public WechatILinkClient(BotManager botManager) {
        this.botManager = botManager;
    }

    private BotInstance requireBot() {
        BotInstance bot = botManager.getPrimaryBot();
        if (bot == null || !bot.isLoggedIn()) {
            log.warn("Bot 未登录");
            return null;
        }
        return bot;
    }

    public boolean isLoggedIn() {
        BotInstance bot = botManager.getPrimaryBot();
        return bot != null && bot.isLoggedIn();
    }

    // ==================== 消息发送 ====================

    public void sendTextMessage(String toUserId, String text) {
        BotInstance bot = requireBot();
        if (bot == null) return;
        bot.sendText(toUserId, text);
        log.info("发送文本消息 | to={}", toUserId);
    }

    public void startTyping(String toUserId) {
        BotInstance bot = requireBot();
        if (bot == null) return;
        bot.startTyping(toUserId);
    }

    public void sendImageMessage(String toUserId, byte[] imageBytes, String textFallback) {
        BotInstance bot = requireBot();
        if (bot == null) return;
        bot.sendImage(toUserId, imageBytes, textFallback);
    }

    public void sendFileMessage(String toUserId, byte[] fileBytes, String fileName, String fileDescription) {
        BotInstance bot = requireBot();
        if (bot == null) return;
        bot.sendFile(toUserId, fileBytes, fileName, fileDescription);
    }

    // ==================== 消息接收 ====================

    public List<WeixinMessage> receiveMessages() {
        BotInstance bot = requireBot();
        if (bot == null) return Collections.emptyList();
        return bot.receiveMessages();
    }

    // ==================== 媒体下载 ====================

    public byte[] downloadMedia(String encryptQueryParam, String aesKey) {
        BotInstance bot = requireBot();
        if (bot == null) return null;
        return bot.downloadMedia(encryptQueryParam, aesKey);
    }

    public byte[] downloadMediaFromMessageItem(
            com.github.wechat.ilink.sdk.core.model.MessageItem item) {
        BotInstance bot = requireBot();
        if (bot == null) return null;
        return bot.downloadMediaFromMessageItem(item);
    }
}
