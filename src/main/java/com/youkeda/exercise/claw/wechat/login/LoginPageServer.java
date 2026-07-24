package com.youkeda.exercise.claw.wechat.login;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * 登录状态可视化 HTTP 服务（临时，非 Spring Bean）。
 *
 * 仅绑定 127.0.0.1，生命周期由 WechatILinkClient 控制。
 * 零外部依赖，JDK 内置 HttpServer。
 */
public class LoginPageServer {

    private static final Logger log = LoggerFactory.getLogger(LoginPageServer.class);

    private final LoginStateManager stateManager;
    private HttpServer server;
    private int port;

    public LoginPageServer(LoginStateManager stateManager) {
        this.stateManager = stateManager;
    }

    /** 启动 HTTP 服务，返回实际绑定的端口 */
    public int start() throws IOException {
        // InetSocketAddress 不指定 port=0，操作系统自动分配空闲端口
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/login", this::handleLogin);
        server.createContext("/login/status", this::handleStatus);
        server.setExecutor(null); // 使用默认单线程 executor
        server.start();
        port = server.getAddress().getPort();
        log.info("登录页面服务已启动 → http://127.0.0.1:{}/login", port);
        return port;
    }

    /** 获取端口（start 之前返回 -1） */
    public int getPort() {
        return port;
    }

    /** 关闭 HTTP 服务 */
    public void stop() {
        if (server != null) {
            server.stop(0);
            log.info("登录页面服务已关闭");
        }
    }

    // ==================== 路由处理 ====================

    /** GET /login — 返回自包含 HTML 页面 */
    private void handleLogin(HttpExchange exchange) throws IOException {
        String qrUrl = stateManager.getQrUrl();
        String html = buildHtml(qrUrl);
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** GET /login/status — 返回当前状态 JSON（不含 qrUrl） */
    private void handleStatus(HttpExchange exchange) throws IOException {
        LoginStatus status = stateManager.getStatus();
        String json = "{\"status\":\"" + (status != null ? status.name() : "WAITING_SCAN") + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ==================== HTML 模板 ====================

    /**
     * 构建自包含 HTML 页面。
     * qrUrl 使用 HTML 实体转义注入，防止 XSS。
     */
    private String buildHtml(String qrUrl) {
        // 注意：不使用 escapeHtml，避免 & -> &amp; 破坏二维码 URL
        // qrUrl 通过 JSON 字符串编码注入 <script> 标签，安全且不改变原始内容
        String rawQrUrl = qrUrl != null ? qrUrl : "";
        return "<!DOCTYPE html>\n" +
            "<html lang=\"zh-CN\">\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "<title>Claw 助手 · AI 团建规划</title>\n" +
            "<style>\n" +
            CSS +
            "</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"dot-grid\"></div>\n" +
            "<div class=\"travel-decor\">\n" +
            "  <div class=\"decor-flag\"></div>\n" +
            "  <div class=\"decor-trail\"></div>\n" +
            "  <div class=\"decor-dot1\"></div>\n" +
            "  <div class=\"decor-dot2\"></div>\n" +
            "  <div class=\"decor-peak\"></div>\n" +
            "</div>\n" +
            "<div class=\"wrapper\">\n" +
            "\n" +
            "  <!-- 品牌头部 -->\n" +
            "  <div class=\"brand\">\n" +
            "    <div class=\"brand-icon\">" + LOGO_SVG + "</div>\n" +
            "    <div class=\"brand-name\">Claw 助手</div>\n" +
            "    <div class=\"brand-sub\">让团建规划，从一个想法开始</div>\n" +
            "  </div>\n" +
            "\n" +
            "  <!-- 二维码卡片 -->\n" +
            "  <div class=\"card\">\n" +
            "    <div id=\"scanArea\">\n" +
            "      <div id=\"qrcode\" class=\"qrcode-box\"></div>\n" +
            "      <div class=\"spinner\" id=\"spinner\"></div>\n" +
            "      <p class=\"hint\">微信扫码连接</p>\n" +
            "      <p class=\"sub-hint\" id=\"statusHint\">等待扫码中...</p>\n" +
            "    </div>\n" +
            "    <!-- 结果区域 -->\n" +
            "    <div id=\"resultArea\" class=\"result-area\" style=\"display:none;\">\n" +
            "      <div id=\"resultIcon\" class=\"result-icon\"></div>\n" +
            "      <p id=\"resultText\" class=\"result-text\"></p>\n" +
            "      <button id=\"retryBtn\" class=\"retry-btn\" style=\"display:none;\" onclick=\"retry()\">重新扫码</button>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "\n" +
            "  <!-- 功能卡 -->\n" +
            "  <div class=\"features\" id=\"features\">\n" +
            "    <div class=\"feature-item\">\n" +
            "      <div class=\"feature-icon\">" + ICON_LOCATION + "</div>\n" +
            "      <div class=\"feature-title\">智能地点推荐</div>\n" +
            "      <div class=\"feature-desc\">AI 寻找团建好去处</div>\n" +
            "    </div>\n" +
            "    <div class=\"feature-item\">\n" +
            "      <div class=\"feature-icon\">" + ICON_ROUTE + "</div>\n" +
            "      <div class=\"feature-title\">路线规划</div>\n" +
            "      <div class=\"feature-desc\">自动规划交通方案</div>\n" +
            "    </div>\n" +
            "    <div class=\"feature-item\">\n" +
            "      <div class=\"feature-icon\">" + ICON_ITINERARY + "</div>\n" +
            "      <div class=\"feature-title\">行程生成</div>\n" +
            "      <div class=\"feature-desc\">生成完整团建计划</div>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "\n" +
            "  <!-- 底部 -->\n" +
            "  <div class=\"footer\">\n" +
            "    <div class=\"footer-line\">⚡ 由 AI 驱动 · 微信原生体验</div>\n" +
            "  </div>\n" +
            "\n" +
            "</div>\n" +
            "\n" +
            "<script>\n" +
            JS +
            "</script>\n" +
            // JSON 字符串编码注入 — 不会改变 & / ? / = 等 URL 关键字符
            "<script>\n" +
            "  const QR_URL = " + jsonString(qrUrl) + ";\n" +
            "  initLogin(QR_URL);\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>";
    }

    /** JSON 字符串编码：仅转义 JSON 特殊字符，不改 URL 结构字符（&、=、?） */
    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    // ==================== 图标 SVG（团建主题手绘风插画） ====================

    /** 智能地点推荐 — 山水旗标，暖色调 */
    private static final String ICON_LOCATION =
        "<svg viewBox=\"0 0 44 44\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">" +
        "  <circle cx=\"30\" cy=\"14\" r=\"10\" fill=\"#FDE68A\" opacity=\".7\"/>" +
        "  <rect x=\"7\" y=\"22\" width=\"30\" height=\"16\" rx=\"5\" fill=\"#E0E7FF\"/>" +
        "  <path d=\"M7 24 C7 22,37 22,37 24\" fill=\"#93C5FD\"/>" +
        "  <path d=\"M18 28 L18 38 M22 27 L22 37 M26 28 L26 38\" stroke=\"#93C5FD\" stroke-width=\"1.5\" stroke-linecap=\"round\"/>" +
        "  <path d=\"M8 20 L14 16 L22 22 L30 10\" stroke=\"#6366F1\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\" fill=\"none\"/>" +
        "  <circle cx=\"30\" cy=\"10\" r=\"2\" fill=\"#6366F1\"/>" +
        "  <path d=\"M8 20 L14 16 L22 22 L30 10\" stroke=\"#818CF8\" stroke-width=\"2.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\" fill=\"none\" opacity=\".5\"/>" +
        "  <path d=\"M36 13 L39 10 L36 7\" stroke=\"#F59E0B\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" fill=\"none\"/>" +
        "  <rect x=\"37\" y=\"5\" width=\"6\" height=\"8\" rx=\"1.5\" fill=\"#F59E0B\"/>" +
        "</svg>";

    /** 路线规划 — 蜿蜒路线 + 小巴车，年轻轻快 */
    private static final String ICON_ROUTE =
        "<svg viewBox=\"0 0 44 44\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">" +
        "  <circle cx=\"12\" cy=\"34\" r=\"6\" fill=\"#D1FAE5\" opacity=\".8\"/>" +
        "  <circle cx=\"34\" cy=\"14\" r=\"6\" fill=\"#FEE2E2\" opacity=\".8\"/>" +
        "  <path d=\"M12 34 C18 30,26 22,34 14\" stroke=\"#10B981\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-dasharray=\"3 3\"/>" +
        "  <path d=\"M12 34 C18 30,26 22,34 14\" stroke=\"#34D399\" stroke-width=\"2.5\" stroke-linecap=\"round\" fill=\"none\" opacity=\".4\"/>" +
        "  <rect x=\"18\" y=\"18\" width=\"12\" height=\"8\" rx=\"3\" fill=\"#EEF2FF\" stroke=\"#6366F1\" stroke-width=\"1.5\"/>" +
        "  <rect x=\"22\" y=\"20\" width=\"4\" height=\"4\" rx=\"1.5\" fill=\"#A5B4FC\"/>" +
        "  <circle cx=\"19.5\" cy=\"27.5\" r=\"2\" fill=\"#818CF8\" stroke=\"#6366F1\" stroke-width=\".8\"/>" +
        "  <circle cx=\"28.5\" cy=\"27.5\" r=\"2\" fill=\"#818CF8\" stroke=\"#6366F1\" stroke-width=\".8\"/>" +
        "  <path d=\"M8 33 L5 32 L7 28\" stroke=\"#F59E0B\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\" fill=\"none\"/>" +
        "</svg>";

    /** 行程生成 — 计划卡片叠层，有序温暖 */
    private static final String ICON_ITINERARY =
        "<svg viewBox=\"0 0 44 44\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">" +
        "  <rect x=\"10\" y=\"26\" width=\"24\" height=\"12\" rx=\"3\" fill=\"#FEF3C7\" stroke=\"#FCD34D\" stroke-width=\"1.2\" transform=\"rotate(-4 22 32)\"/>" +
        "  <line x1=\"14\" y1=\"29\" x2=\"28\" y2=\"28\" stroke=\"#F59E0B\" stroke-width=\"1\" stroke-linecap=\"round\"/>" +
        "  <line x1=\"14\" y1=\"32\" x2=\"24\" y2=\"31\" stroke=\"#FBBF24\" stroke-width=\"1\" stroke-linecap=\"round\"/>" +
        "  <line x1=\"14\" y1=\"35\" x2=\"22\" y2=\"34\" stroke=\"#FCD34D\" stroke-width=\"1\" stroke-linecap=\"round\"/>" +
        "  <rect x=\"8\" y=\"24\" width=\"24\" height=\"12\" rx=\"3\" fill=\"#E0E7FF\" stroke=\"#818CF8\" stroke-width=\"1.2\"/>" +
        "  <line x1=\"12\" y1=\"27\" x2=\"28\" y2=\"27\" stroke=\"#6366F1\" stroke-width=\"1\" stroke-linecap=\"round\"/>" +
        "  <line x1=\"12\" y1=\"30\" x2=\"26\" y2=\"30\" stroke=\"#818CF8\" stroke-width=\"1\" stroke-linecap=\"round\"/>" +
        "  <line x1=\"12\" y1=\"33\" x2=\"20\" y2=\"33\" stroke=\"#A5B4FC\" stroke-width=\"1\" stroke-linecap=\"round\"/>" +
        "  <circle cx=\"6\" cy=\"25\" r=\"2.5\" fill=\"#F9A8D4\"/>" +
        "  <rect x=\"4\" y=\"6\" width=\"20\" height=\"13\" rx=\"3\" fill=\"#FCE7F3\" stroke=\"#F9A8D4\" stroke-width=\"1.2\" transform=\"rotate(3 14 13)\"/>" +
        "  <line x1=\"7\" y1=\"10\" x2=\"19\" y2=\"9\" stroke=\"#EC4899\" stroke-width=\".8\" stroke-linecap=\"round\"/>" +
        "  <line x1=\"7\" y1=\"13\" x2=\"17\" y2=\"12\" stroke=\"#F472B6\" stroke-width=\".8\" stroke-linecap=\"round\"/>" +
        "  <line x1=\"7\" y1=\"16\" x2=\"14\" y2=\"15\" stroke=\"#F9A8D4\" stroke-width=\".8\" stroke-linecap=\"round\"/>" +
        "  <circle cx=\"18\" cy=\"7\" r=\"1.2\" fill=\"#EC4899\"/>" +
        "</svg>";

    // ==================== 品牌 Logo SVG ====================

    /** Claw 助手品牌 Logo — 团队人物 + 路线旗标，团建出游意象 */
    private static final String LOGO_SVG =
        "<svg viewBox=\"0 0 56 56\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">" +
        "  <rect width=\"56\" height=\"56\" rx=\"16\" fill=\"url(#logo-grad)\"/>" +
        "  <defs>" +
        "    <linearGradient id=\"logo-grad\" x1=\"0\" y1=\"0\" x2=\"56\" y2=\"56\">" +
        "      <stop offset=\"0%\" stop-color=\"#667EEA\"/>" +
        "      <stop offset=\"100%\" stop-color=\"#764BA2\"/>" +
        "    </linearGradient>" +
        "  </defs>" +
        /* 小山/丘陵 — 团建户外 */
        "  <ellipse cx=\"20\" cy=\"40\" rx=\"8\" ry=\"5\" fill=\"#C4B5FD\" opacity=\".6\"/>" +
        "  <ellipse cx=\"38\" cy=\"38\" rx=\"6\" ry=\"4\" fill=\"#A78BFA\" opacity=\".5\"/>" +
        /* 旗标 — 目的地 */
        "  <line x1=\"28\" y1=\"18\" x2=\"28\" y2=\"34\" stroke=\"#FDE68A\" stroke-width=\"1.5\" stroke-linecap=\"round\"/>" +
        "  <path d=\"M28 18 L37 22 L28 26\" fill=\"#F59E0B\" stroke=\"#FDE68A\" stroke-width=\".8\" stroke-linejoin=\"round\"/>" +
        /* 三个人物剪影 — 团队 */
        "  <circle cx=\"20\" cy=\"26\" r=\"3.5\" fill=\"#FDE68A\"/>" +
        "  <path d=\"M14 33 C14 28,26 28,26 33\" fill=\"#FDE68A\" opacity=\".9\"/>" +
        "  <circle cx=\"32\" cy=\"24\" r=\"3\" fill=\"#E0E7FF\"/>" +
        "  <path d=\"M27 31 C27 26.5,37 26.5,37 31\" fill=\"#E0E7FF\" opacity=\".9\"/>" +
        "  <circle cx=\"24\" cy=\"20\" r=\"2.5\" fill=\"#C4B5FD\"/>" +
        "  <path d=\"M19 27 C19 23,29 23,29 27\" fill=\"#C4B5FD\" opacity=\".9\"/>" +
        /* 蜿蜒路线 */
        "  <path d=\"M12 40 C18 36,22 38,28 34 C32 31,36 32,40 38\" stroke=\"#FDE68A\" stroke-width=\"1.2\" " +
        "    stroke-linecap=\"round\" fill=\"none\" opacity=\".7\"/>" +
        "</svg>";

    // ==================== CSS（内联） ====================

    private static final String CSS = """
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC",
                         "Microsoft YaHei", sans-serif;
            background: linear-gradient(135deg, #f5f0ff 0%, #e8f4fd 50%, #f0f7ff 100%);
            display: flex; justify-content: center; align-items: center;
            min-height: 100vh;
            position: relative;
            overflow-x: hidden;
        }
        /* 背景光晕 */
        body::before {
            content: '';
            position: fixed; top: -50%; left: -30%;
            width: 80%; height: 100%;
            background: radial-gradient(ellipse, rgba(139,92,246,0.06) 0%, transparent 70%);
            pointer-events: none; z-index: 0;
        }
        body::after {
            content: '';
            position: fixed; bottom: -40%; right: -20%;
            width: 70%; height: 80%;
            background: radial-gradient(ellipse, rgba(59,130,246,0.05) 0%, transparent 70%);
            pointer-events: none; z-index: 0;
        }
        /* 团建出游装饰元素 */
        .travel-decor {
            position: fixed; inset: 0; pointer-events: none; z-index: 0; overflow: hidden;
        }
        .travel-decor .decor-flag {
            position: absolute; top: 12%; left: 8%;
            width: 18px; height: 32px; opacity: 0.08;
            border-left: 2px solid #667eea;
        }
        .travel-decor .decor-flag::after {
            content: '';
            position: absolute; top: 2px; left: 2px;
            width: 0; height: 0;
            border-left: 7px solid transparent;
            border-right: 7px solid transparent;
            border-bottom: 10px solid #667eea;
        }
        .travel-decor .decor-trail {
            position: absolute; bottom: 18%; right: 10%; opacity: 0.06;
            width: 120px; height: 40px;
            border: 2px dashed #764ba2;
            border-radius: 50%;
            border-color: transparent transparent #764ba2 transparent;
            transform: rotate(-15deg);
        }
        .travel-decor .decor-dot1 {
            position: absolute; top: 25%; right: 14%; opacity: 0.07;
            width: 10px; height: 10px; border-radius: 50%; background: #f59e0b;
        }
        .travel-decor .decor-dot2 {
            position: absolute; top: 65%; left: 12%; opacity: 0.06;
            width: 8px; height: 8px; border-radius: 50%; background: #10b981;
        }
        .travel-decor .decor-peak {
            position: absolute; bottom: 22%; left: 6%; opacity: 0.05;
            width: 0; height: 0;
            border-left: 20px solid transparent;
            border-right: 20px solid transparent;
            border-bottom: 28px solid #667eea;
        }
        /* 点阵网格 */
        .dot-grid {
            position: fixed; inset: 0;
            background-image: radial-gradient(circle, rgba(148,163,184,0.12) 1px, transparent 1px);
            background-size: 28px 28px;
            pointer-events: none; z-index: 0;
        }
        .wrapper {
            position: relative; z-index: 1;
            width: 100%; max-width: 420px; padding: 24px;
        }

        /* ===== 品牌头部 ===== */
        .brand {
            text-align: center; margin-bottom: 28px;
        }
        .brand-icon {
            display: inline-flex; align-items: center; justify-content: center;
            width: 56px; height: 56px; margin-bottom: 14px;
        }
        .brand-icon svg {
            width: 56px; height: 56px; display: block;
        }
        .brand-name {
            font-size: 24px; font-weight: 700; color: #1a1a2e;
            letter-spacing: -0.3px;
        }
        .brand-sub {
            font-size: 14px; color: #8892a4; margin-top: 4px;
            font-weight: 400;
        }

        /* ===== 二维码卡片 ===== */
        .card {
            background: rgba(255,255,255,0.85);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border-radius: 20px; padding: 36px 28px 32px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.07), 0 1px 0 rgba(255,255,255,0.6) inset;
            border: 1px solid rgba(226,232,240,0.8);
            text-align: center;
            margin-bottom: 24px;
        }
        .qrcode-box {
            display: inline-block; padding: 14px;
            background: #fff; border: 2px solid #eef2f6;
            border-radius: 14px; margin-bottom: 16px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.04);
        }
        .qrcode-box canvas, .qrcode-box img {
            display: block; width: 220px; height: 220px;
        }
        .spinner {
            width: 28px; height: 28px; margin: 6px auto 12px;
            border: 3px solid #e8ecf1; border-top-color: #667eea;
            border-radius: 50%; animation: spin 0.7s linear infinite;
        }
        @keyframes spin { to { transform: rotate(360deg); } }
        .hint { font-size: 16px; color: #1a1a2e; font-weight: 500; margin-bottom: 2px; }
        .sub-hint { font-size: 13px; color: #94a3b8; margin-top: 2px; }
        .sub-hint.verified {
            color: #667eea; font-weight: 500;
            animation: pulse 1.5s ease-in-out infinite;
        }
        @keyframes pulse { 0%,100% { opacity:1; } 50% { opacity:0.5; } }

        /* ===== 功能卡 ===== */
        .features {
            display: flex; gap: 10px; margin-bottom: 24px;
        }
        .feature-item {
            flex: 1; text-align: center;
            background: rgba(255,255,255,0.72);
            backdrop-filter: blur(8px);
            -webkit-backdrop-filter: blur(8px);
            border: 1px solid rgba(226,232,240,0.7);
            border-radius: 14px; padding: 16px 8px;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        .feature-item:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102,126,234,0.1);
        }
        .feature-icon {
            width: 44px; height: 44px; margin: 0 auto 8px;
            display: flex; align-items: center; justify-content: center;
        }
        .feature-icon svg {
            width: 44px; height: 44px; display: block;
        }
        .feature-title { font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 2px; }
        .feature-desc { font-size: 11px; color: #94a3b8; line-height: 1.4; }

        /* ===== 底部 ===== */
        .footer {
            text-align: center; font-size: 12px; color: #b0b8c4;
        }
        .footer-line { margin-bottom: 4px; }
        .footer-line:last-child { opacity: 0.7; }

        /* ===== 结果区域 ===== */
        .result-area { padding-top: 12px; }
        .result-icon {
            width: 80px; height: 80px; margin: 0 auto 20px;
            border-radius: 50%; display: flex; align-items: center; justify-content: center;
            animation: popIn 0.45s cubic-bezier(0.175, 0.885, 0.32, 1.275);
        }
        @keyframes popIn {
            0% { transform: scale(0); opacity: 0; }
            100% { transform: scale(1); opacity: 1; }
        }
        .result-icon.success { background: linear-gradient(135deg, #07c160, #06ad56); }
        .result-icon.fail { background: linear-gradient(135deg, #fa5151, #e04848); }
        .result-icon svg { width: 40px; height: 40px; }
        .result-text {
            font-size: 17px; color: #1a1a2e; font-weight: 500; margin-bottom: 20px;
        }
        .retry-btn {
            padding: 10px 36px; font-size: 15px; color: #fff;
            background: linear-gradient(135deg, #667eea, #764ba2);
            border: none; border-radius: 10px;
            cursor: pointer; transition: transform 0.15s, box-shadow 0.15s;
            box-shadow: 0 4px 12px rgba(102,126,234,0.3);
        }
        .retry-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 18px rgba(102,126,234,0.4); }
        .retry-btn:active { transform: translateY(0); }

        /* ===== 响应式 ===== */
        @media (max-width: 480px) {
            .wrapper { padding: 16px; }
            .card { padding: 28px 18px 24px; border-radius: 16px; }
            .features { flex-direction: column; gap: 8px; }
            .feature-item { padding: 14px 12px; }
        }
        """;

    // ==================== JS（内联） ====================

    // language=JavaScript
    private static final String JS = """
        let pollTimer = null;
        let scannedOnce = false;

        function initLogin(qrUrl) {
            console.log("qrUrl length:", qrUrl.length);
            if (qrUrl && qrUrl.length > 0) {
                generateQR(qrUrl);
            }
            startPolling();
        }

        // 加载 qrcodejs CDN 并生成二维码
        function generateQR(qrUrl) {
            var script = document.createElement('script');
            script.src = 'https://cdn.jsdelivr.net/npm/qrcodejs@1.0.0/qrcode.min.js';
            script.onload = function() {
                document.getElementById('qrcode').innerHTML = '';
                new QRCode(document.getElementById('qrcode'), {
                    text: qrUrl,
                    width: 220,
                    height: 220,
                    colorDark: '#000000',
                    colorLight: '#ffffff',
                    correctLevel: QRCode.CorrectLevel.M
                });
            };
            script.onerror = function() {
                document.getElementById('qrcode').innerHTML =
                    '<p style="color:#94a3b8;">二维码加载失败，请<a href="' +
                    escapeHtml(qrUrl) + '" target="_blank" style="color:#667eea;">点击此处</a>打开扫码页面</p>';
            };
            document.head.appendChild(script);
        }

        function escapeHtml(s) {
            return s.replace(/&/g,'&amp;').replace(/"/g,'&quot;')
                    .replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/'/g,'&#39;');
        }

        // 轮询登录状态
        function startPolling() {
            pollTimer = setInterval(function() {
                fetch('/login/status')
                    .then(function(r) { return r.json(); })
                    .then(function(data) {
                        if (data.status === 'SUCCESS') {
                            updateHint('正在连接微信助手...', 'verified');
                            // 短暂延迟让用户看到连接中状态
                            setTimeout(function() {
                                showResult('success');
                                stopPolling();
                                setTimeout(tryClose, 3000);
                            }, 800);
                        } else if (data.status === 'FAILED' || data.status === 'TIMEOUT') {
                            showResult('fail');
                            stopPolling();
                            setTimeout(tryClose, 10000);
                        } else if (data.status === 'SCANNED' && !scannedOnce) {
                            scannedOnce = true;
                            updateHint('正在验证身份...', 'verified');
                        }
                        // WAITING_SCAN 继续轮询
                    })
                    .catch(function() {
                        // 网络错误静默忽略
                    });
            }, 2000);
        }

        function updateHint(text, className) {
            var hint = document.getElementById('statusHint');
            hint.textContent = text;
            hint.className = 'sub-hint';
            if (className) hint.classList.add(className);
        }

        function stopPolling() {
            if (pollTimer) {
                clearInterval(pollTimer);
                pollTimer = null;
            }
        }

        function showResult(type) {
            var spinner = document.getElementById('spinner');
            if (spinner) spinner.style.display = 'none';
            document.getElementById('scanArea').style.display = 'none';
            document.getElementById('features').style.display = 'none';
            var area = document.getElementById('resultArea');
            area.style.display = 'block';

            var icon = document.getElementById('resultIcon');
            var text = document.getElementById('resultText');
            var btn = document.getElementById('retryBtn');

            if (type === 'success') {
                icon.className = 'result-icon success';
                icon.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="white" ' +
                    'stroke-width="3" stroke-linecap="round" stroke-linejoin="round">' +
                    '<polyline points="20 6 9 17 4 12"></polyline></svg>';
                text.textContent = '连接成功，开始使用 🎉';
                btn.style.display = 'none';
            } else {
                icon.className = 'result-icon fail';
                icon.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="white" ' +
                    'stroke-width="3" stroke-linecap="round" stroke-linejoin="round">' +
                    '<line x1="18" y1="6" x2="6" y2="18"></line>' +
                    '<line x1="6" y1="6" x2="18" y2="18"></line></svg>';
                text.textContent = '连接失败，请重新扫码';
                btn.style.display = 'inline-block';
            }
        }

        function retry() {
            location.reload();
        }

        function tryClose() {
            try { window.close(); } catch(e) {}
        }
        """;
}