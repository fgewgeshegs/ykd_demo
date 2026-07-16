package org.example.wechatilink.controller;

import org.example.wechatilink.service.ILinkBotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Bot 管理 REST API。
 * <p>
 * 提供查看 Bot 状态、获取登录二维码、重启 Bot 等管理端点。
 * </p>
 */
@RestController
@RequestMapping("/bot")
public class BotController {

    private final ILinkBotService botService;

    public BotController(ILinkBotService botService) {
        this.botService = botService;
    }

    /**
     * 查看 Bot 运行状态。
     * GET /bot/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        boolean loggedIn = botService.isLoggedIn();
        String botId = null;
        String userId = null;
        if (loggedIn && botService.getLoginContext() != null) {
            botId = botService.getLoginContext().getBotId();
            userId = botService.getLoginContext().getUserId();
        }

        return ResponseEntity.ok(Map.of(
                "running", botService.isRunning(),
                "loggedIn", loggedIn,
                "botId", botId != null ? botId : "",
                "userId", userId != null ? userId : ""
        ));
    }

    /**
     * 获取当前登录二维码（仅未登录时有效）。
     * GET /bot/qrcode
     */
    @GetMapping("/qrcode")
    public ResponseEntity<Map<String, Object>> qrCode() {
        String qr = botService.getCurrentQrCode();
        if (qr == null) {
            return ResponseEntity.ok(Map.of(
                    "hasQrCode", false,
                    "message", botService.isLoggedIn()
                            ? "已登录，无需二维码"
                            : "二维码尚未生成，请等待 Bot 启动"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "hasQrCode", true,
                "qrCodeContent", qr
        ));
    }

    /**
     * 重启 Bot。
     * POST /bot/restart
     */
    @PostMapping("/restart")
    public ResponseEntity<Map<String, Object>> restart() {
        botService.restart();
        return ResponseEntity.ok(Map.of(
                "message", "Bot 正在重启...",
                "running", botService.isRunning()
        ));
    }
}
