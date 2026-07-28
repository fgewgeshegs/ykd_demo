package com.youkeda.exercise.claw.wechat.login;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.youkeda.exercise.claw.wechat.bot.BotSessionManager;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 微信 ClawBot 控制台 Dashboard。
 *
 * <p>永久 HTTP 服务，不再随登录流程销毁。
 * 左侧显示二维码/连接状态，右侧显示机器人状态与用户列表。
 *
 * <p>API 端点：
 * <ul>
 *   <li>{@code GET /login} — 控制台 HTML 页面</li>
 *   <li>{@code GET /login/status} — 登录过程状态 {@code LoginStatus}（向后兼容）</li>
 *   <li>{@code GET /api/bot/status} — 机器人连接状态 JSON</li>
 *   <li>{@code GET /api/users} — 微信用户列表 JSON</li>
 * </ul>
 */
public class LoginPageServer {

    private static final Logger log = LoggerFactory.getLogger(LoginPageServer.class);

    private final LoginStateManager stateManager;
    private final BotSessionManager botSessionManager;
    private final WechatUserManager wechatUserManager;

    private HttpServer server;
    private volatile int port = -1;

    public LoginPageServer(LoginStateManager stateManager,
                           BotSessionManager botSessionManager,
                           WechatUserManager wechatUserManager) {
        this.stateManager = stateManager;
        this.botSessionManager = botSessionManager;
        this.wechatUserManager = wechatUserManager;
    }

    /** 启动 HTTP 服务，返回实际绑定端口 */
    public int start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/login", this::handleLogin);
        server.createContext("/login/status", this::handleStatus);
        server.createContext("/api/bot/status", this::handleBotStatus);
        server.createContext("/api/users", this::handleUsers);
        server.setExecutor(null);
        server.start();
        port = server.getAddress().getPort();
        log.info("ClawBot 控制台已启动 → http://127.0.0.1:{}/login", port);
        return port;
    }

    public int getPort() { return port; }

    public void stop() {
        if (server != null) {
            server.stop(0);
            log.info("控制台服务已关闭");
        }
    }

    // ==================== 路由处理 ====================

    /** GET /login — 控制台 HTML */
    private void handleLogin(HttpExchange exchange) throws IOException {
        String qrUrl = stateManager.getQrUrl();
        String html = buildDashboardHtml(qrUrl);
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** GET /login/status — 登录过程状态（向后兼容） */
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

    /** GET /api/bot/status — 机器人连接状态 */
    private void handleBotStatus(HttpExchange exchange) throws IOException {
        String status = botSessionManager.getLastStatus();
        if (status == null) status = "NOT_STARTED";
        String loginTime = botSessionManager.getLastLoginTime();
        String error = botSessionManager.getLastError();

        StringBuilder json = new StringBuilder();
        json.append("{\"status\":\"").append(jsonEscape(status)).append("\"");
        if (loginTime != null) {
            json.append(",\"loginTime\":\"").append(jsonEscape(loginTime)).append("\"");
        }
        if (error != null) {
            json.append(",\"error\":\"").append(jsonEscape(error)).append("\"");
        }
        json.append("}");

        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** GET /api/users — 微信用户列表 */
    private void handleUsers(HttpExchange exchange) throws IOException {
        List<WechatUserManager.UserRecord> users = wechatUserManager.getAllUsers();
        int total = users.size();

        StringBuilder json = new StringBuilder();
        json.append("{\"total\":").append(total).append(",\"users\":[");
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) json.append(",");
            WechatUserManager.UserRecord u = users.get(i);
            json.append("{");
            json.append("\"userId\":\"").append(jsonEscape(u.userId())).append("\"");
            json.append(",\"nickname\":").append(u.nickname() != null ? "\"" + jsonEscape(u.nickname()) + "\"" : "null");
            json.append(",\"avatarUrl\":").append(u.avatarUrl() != null ? "\"" + jsonEscape(u.avatarUrl()) + "\"" : "null");
            json.append(",\"lastActiveTime\":\"").append(jsonEscape(u.lastActiveTime())).append("\"");
            json.append(",\"firstActiveTime\":\"").append(jsonEscape(u.firstActiveTime())).append("\"");
            json.append(",\"interactionCount\":").append(u.interactionCount());
            json.append("}");
        }
        json.append("]}");

        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ==================== HTML ====================

    private String buildDashboardHtml(String qrUrl) {
        String rawQrUrl = qrUrl != null ? qrUrl : "";
        return "<!DOCTYPE html>\n" +
            "<html lang=\"zh-CN\">\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "<title>ClawBot 控制台</title>\n" +
            "<style>\n" +
            CSS +
            "</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"app\">\n" +
            "  <!-- 顶部导航 -->\n" +
            "  <header class=\"topbar\">\n" +
            "    <div class=\"topbar-left\">\n" +
            "      <span class=\"logo\">" + LOGO_SVG + "</span>\n" +
            "      <span class=\"title\">ClawBot 控制台</span>\n" +
            "      <span class=\"badge\" id=\"botBadge\">等待连接</span>\n" +
            "    </div>\n" +
            "    <div class=\"topbar-right\">\n" +
            "      <span class=\"update-time\" id=\"updateTime\">—</span>\n" +
            "    </div>\n" +
            "  </header>\n" +
            "\n" +
            "  <main class=\"main\">\n" +
            "    <!-- 左侧面板：扫码/连接 -->\n" +
            "    <section class=\"panel panel-left\" id=\"scanPanel\">\n" +
            "      <div class=\"panel-title\">\n" +
            "        <span class=\"panel-icon\">" + ICON_WECHAT + "</span>\n" +
            "        微信连接\n" +
            "      </div>\n" +
            "      <div class=\"scan-area\" id=\"scanArea\">\n" +
            "        <div class=\"qrcode-box\" id=\"qrcode\"></div>\n" +
            "        <div class=\"spinner\" id=\"spinner\"></div>\n" +
            "        <p class=\"state-hint\" id=\"stateHint\">等待扫码中...</p>\n" +
            "        <p class=\"state-sub\" id=\"stateSub\">请使用微信扫描二维码连接机器人</p>\n" +
            "      </div>\n" +
            "      <!-- 登录结果 -->\n" +
            "      <div class=\"result-area\" id=\"resultArea\" style=\"display:none;\">\n" +
            "        <div class=\"result-icon\" id=\"resultIcon\"></div>\n" +
            "        <p class=\"result-text\" id=\"resultText\"></p>\n" +
            "      </div>\n" +
            "    </section>\n" +
            "\n" +
            "    <!-- 右侧面板：控制台数据 -->\n" +
            "    <section class=\"panel panel-right\">\n" +
            "      <!-- 机器人状态卡片 -->\n" +
            "      <div class=\"card card-status\">\n" +
            "        <div class=\"card-header\">\n" +
            "          <span class=\"card-icon\">" + ICON_BOT + "</span>\n" +
            "          机器人状态\n" +
            "        </div>\n" +
            "        <div class=\"card-body\">\n" +
            "          <div class=\"status-row\">\n" +
            "            <span class=\"label\">状态</span>\n" +
            "            <span class=\"status-indicator\" id=\"botStatusIndicator\">\n" +
            "              <span class=\"dot dot-gray\"></span> 未启动\n" +
            "            </span>\n" +
            "          </div>\n" +
            "          <div class=\"status-row\">\n" +
            "            <span class=\"label\">最近登录</span>\n" +
            "            <span class=\"value\" id=\"botLoginTime\">—</span>\n" +
            "          </div>\n" +
            "          <div class=\"status-row\" id=\"botErrorRow\" style=\"display:none;\">\n" +
            "            <span class=\"label\">错误信息</span>\n" +
            "            <span class=\"value error-text\" id=\"botError\"></span>\n" +
            "          </div>\n" +
            "        </div>\n" +
            "      </div>\n" +
            "\n" +
            "      <!-- 用户统计卡片 -->\n" +
            "      <div class=\"card card-users\">\n" +
            "        <div class=\"card-header\">\n" +
            "          <span class=\"card-icon\">" + ICON_USERS + "</span>\n" +
            "          在线用户\n" +
            "        </div>\n" +
            "        <div class=\"card-body\">\n" +
            "          <div class=\"user-count-wrap\">\n" +
            "            <span class=\"user-count-num\" id=\"userCount\">0</span>\n" +
            "            <span class=\"user-count-label\">位用户</span>\n" +
            "          </div>\n" +
            "        </div>\n" +
            "      </div>\n" +
            "\n" +
            "      <!-- 用户列表卡片 -->\n" +
            "      <div class=\"card card-list\">\n" +
            "        <div class=\"card-header\">\n" +
            "          <span class=\"card-icon\">" + ICON_LIST + "</span>\n" +
            "          用户列表\n" +
            "        </div>\n" +
            "        <div class=\"card-body\">\n" +
            "          <div class=\"user-table\" id=\"userTable\">\n" +
            "            <div class=\"table-header\">\n" +
            "              <span class=\"col-user\">用户</span>\n" +
            "              <span class=\"col-id\">ID</span>\n" +
            "              <span class=\"col-active\">最近活跃</span>\n" +
            "              <span class=\"col-count\">消息</span>\n" +
            "            </div>\n" +
            "            <div class=\"table-body\" id=\"userTableBody\">\n" +
            "              <div class=\"table-empty\">暂无用户数据</div>\n" +
            "            </div>\n" +
            "          </div>\n" +
            "        </div>\n" +
            "      </div>\n" +
            "    </section>\n" +
            "  </main>\n" +
            "</div>\n" +
            "\n" +
            "<script>\n" +
            "const QR_URL = " + jsonString(qrUrl) + ";\n" +
            JS +
            "</script>\n" +
            "</body>\n" +
            "</html>";
    }

    // ==================== SVG 图标 ====================

    private static final String LOGO_SVG =
        "<svg viewBox=\"0 0 28 28\" width=\"28\" height=\"28\" fill=\"none\">" +
        "  <rect width=\"28\" height=\"28\" rx=\"8\" fill=\"#07C160\"/>" +
        "  <path d=\"M7 14c0-3.86 3.13-7 7-7s7 3.14 7 7-3.13 7-7 7a6.96 6.96 0 0 1-3.4-.9L7 20l1.1-3.2A6.9 6.9 0 0 1 7 14z\" fill=\"white\" opacity=\".95\"/>" +
        "  <circle cx=\"14\" cy=\"14\" r=\"2\" fill=\"#07C160\"/>" +
        "</svg>";

    private static final String ICON_WECHAT =
        "<svg viewBox=\"0 0 20 20\" width=\"18\" height=\"18\" fill=\"none\">" +
        "  <path d=\"M5.5 8a5.5 5.5 0 0 1 9.9-3.3A6 6 0 0 1 18 10.5c0 1.3-.4 2.5-1.1 3.5l.6 1.8-2-.7a6.5 6.5 0 0 1-2 .4c-.3 0-.6 0-.9-.1A5.5 5.5 0 0 1 5.5 8z\" fill=\"#07C160\" opacity=\".15\"/>" +
        "  <path d=\"M4 9.5a4.5 4.5 0 0 1 8.2-2.7 5 5 0 0 1 2.3 4.2c0 1-.3 2-.8 2.8l.5 1.5-1.7-.6a5.3 5.3 0 0 1-1.6.3c-.2 0-.5 0-.7-.1A4.5 4.5 0 0 1 4 9.5z\" fill=\"#07C160\"/>" +
        "  <circle cx=\"7\" cy=\"9.5\" r=\"1\" fill=\"white\"/>" +
        "  <circle cx=\"11\" cy=\"9.5\" r=\"1\" fill=\"white\"/>" +
        "</svg>";

    private static final String ICON_BOT =
        "<svg viewBox=\"0 0 20 20\" width=\"18\" height=\"18\" fill=\"none\">" +
        "  <rect x=\"3\" y=\"5\" width=\"14\" height=\"11\" rx=\"3\" fill=\"#6366F1\" opacity=\".15\"/>" +
        "  <rect x=\"4\" y=\"6\" width=\"12\" height=\"9\" rx=\"2\" fill=\"#6366F1\"/>" +
        "  <circle cx=\"7.5\" cy=\"10.5\" r=\"1.2\" fill=\"white\"/>" +
        "  <circle cx=\"12.5\" cy=\"10.5\" r=\"1.2\" fill=\"white\"/>" +
        "  <path d=\"M7 14a3 3 0 0 1 6 0\" stroke=\"white\" stroke-width=\".8\" stroke-linecap=\"round\"/>" +
        "</svg>";

    private static final String ICON_USERS =
        "<svg viewBox=\"0 0 20 20\" width=\"18\" height=\"18\" fill=\"none\">" +
        "  <circle cx=\"7\" cy=\"7\" r=\"3\" fill=\"#F59E0B\" opacity=\".15\"/>" +
        "  <path d=\"M2 16c0-2.76 2.24-5 5-5s5 2.24 5 5\" stroke=\"#F59E0B\" stroke-width=\"1.2\" stroke-linecap=\"round\"/>" +
        "  <circle cx=\"7\" cy=\"7\" r=\"2.5\" fill=\"#F59E0B\"/>" +
        "  <circle cx=\"14\" cy=\"8\" r=\"2\" fill=\"#6366F1\" opacity=\".15\"/>" +
        "  <circle cx=\"14\" cy=\"8\" r=\"1.6\" fill=\"#6366F1\"/>" +
        "  <path d=\"M11 17c0-2.2 1.34-4 3-4s3 1.8 3 4\" stroke=\"#6366F1\" stroke-width=\"1.2\" stroke-linecap=\"round\"/>" +
        "</svg>";

    private static final String ICON_LIST =
        "<svg viewBox=\"0 0 20 20\" width=\"18\" height=\"18\" fill=\"none\">" +
        "  <rect x=\"4\" y=\"3\" width=\"12\" height=\"14\" rx=\"2\" fill=\"#10B981\" opacity=\".1\"/>" +
        "  <path d=\"M4 7h12M4 10h12M4 13h8\" stroke=\"#10B981\" stroke-width=\"1.2\" stroke-linecap=\"round\"/>" +
        "</svg>";

    // ==================== CSS ====================

    private static final String CSS = """
        * { margin:0; padding:0; box-sizing:border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC",
                         "Microsoft YaHei", sans-serif;
            background: linear-gradient(135deg, #f0f4ff 0%, #f5f0ff 50%, #ecfdf5 100%);
            min-height: 100vh;
            color: #1e293b;
        }

        /* ===== 顶部栏 ===== */
        .topbar {
            display: flex; align-items: center; justify-content: space-between;
            padding: 12px 28px;
            background: rgba(255,255,255,0.8);
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            border-bottom: 1px solid rgba(226,232,240,0.8);
            position: sticky; top: 0; z-index: 100;
        }
        .topbar-left { display: flex; align-items: center; gap: 12px; }
        .topbar-left .logo { display: flex; align-items: center; }
        .topbar-left .title {
            font-size: 17px; font-weight: 600; color: #0f172a;
            letter-spacing: -0.2px;
        }
        .badge {
            display: inline-flex; align-items: center; gap: 5px;
            padding: 3px 10px; border-radius: 20px;
            font-size: 12px; font-weight: 500;
            background: #f1f5f9; color: #64748b;
            border: 1px solid #e2e8f0;
        }
        .badge:before {
            content: ''; width: 6px; height: 6px;
            border-radius: 50%; display: inline-block;
            background: #94a3b8;
        }
        .badge.connected { background: #f0fdf4; color: #16a34a; border-color: #bbf7d0; }
        .badge.connected:before { background: #22c55e; }
        .badge.waiting { background: #fffbeb; color: #d97706; border-color: #fde68a; }
        .badge.waiting:before { background: #f59e0b; }
        .badge.failed { background: #fef2f2; color: #dc2626; border-color: #fecaca; }
        .badge.failed:before { background: #ef4444; }
        .topbar-right { font-size: 12px; color: #94a3b8; }

        /* ===== 主布局 ===== */
        .main {
            display: grid;
            grid-template-columns: 340px 1fr;
            gap: 24px;
            padding: 24px 28px;
            max-width: 1200px;
            margin: 0 auto;
        }

        /* ===== 面板 ===== */
        .panel {
            background: rgba(255,255,255,0.75);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid rgba(226,232,240,0.7);
            border-radius: 16px;
            padding: 24px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 8px 24px rgba(0,0,0,0.04);
        }
        .panel-title {
            display: flex; align-items: center; gap: 8px;
            font-size: 14px; font-weight: 600; color: #334155;
            margin-bottom: 20px;
        }
        .panel-icon { display: flex; }

        /* ===== 左侧扫码区 ===== */
        .scan-area { text-align: center; padding: 8px 0; }
        .qrcode-box {
            display: inline-block; padding: 12px;
            background: #fff; border: 2px solid #eef2f6;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.04);
            margin-bottom: 16px;
            transition: opacity 0.4s ease, transform 0.4s ease;
        }
        .qrcode-box canvas, .qrcode-box img {
            display: block; width: 200px; height: 200px;
        }
        .qrcode-box.fade-out {
            opacity: 0; transform: scale(0.95);
        }
        .spinner {
            width: 24px; height: 24px; margin: 0 auto 14px;
            border: 3px solid #e8ecf1; border-top-color: #6366F1;
            border-radius: 50%;
            animation: spin 0.7s linear infinite;
        }
        @keyframes spin { to { transform: rotate(360deg); } }
        .state-hint { font-size: 15px; color: #1e293b; font-weight: 500; margin-bottom: 4px; }
        .state-sub { font-size: 12px; color: #94a3b8; }

        /* ===== 登录结果 ===== */
        .result-area { text-align: center; padding: 40px 0; }
        .result-icon {
            width: 72px; height: 72px; margin: 0 auto 20px;
            border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            animation: popIn 0.45s cubic-bezier(0.175, 0.885, 0.32, 1.275);
        }
        @keyframes popIn {
            0% { transform: scale(0); opacity: 0; }
            100% { transform: scale(1); opacity: 1; }
        }
        .result-icon.success { background: linear-gradient(135deg, #22c55e, #16a34a); }
        .result-icon.fail { background: linear-gradient(135deg, #ef4444, #dc2626); }
        .result-icon svg { width: 36px; height: 36px; }
        .result-text { font-size: 16px; color: #1e293b; font-weight: 500; }

        /* ===== 右侧卡片 ===== */
        .panel-right { display: flex; flex-direction: column; gap: 16px; }
        .card {
            background: white;
            border: 1px solid #e2e8f0;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 1px 2px rgba(0,0,0,0.03);
            transition: box-shadow 0.2s;
        }
        .card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
        .card-header {
            display: flex; align-items: center; gap: 8px;
            padding: 14px 18px;
            font-size: 13px; font-weight: 600; color: #475569;
            background: #f8fafc;
            border-bottom: 1px solid #e2e8f0;
        }
        .card-icon { display: flex; }
        .card-body { padding: 18px; }

        /* ===== 状态行 ===== */
        .status-row {
            display: flex; justify-content: space-between; align-items: center;
            padding: 8px 0;
            font-size: 13px;
        }
        .status-row + .status-row { border-top: 1px solid #f1f5f9; }
        .status-row .label { color: #94a3b8; }
        .status-row .value { color: #334155; font-weight: 500; }
        .status-indicator { display: flex; align-items: center; gap: 6px; font-weight: 500; }
        .dot {
            width: 8px; height: 8px; border-radius: 50%; display: inline-block;
            transition: background 0.3s;
        }
        .dot-green { background: #22c55e; box-shadow: 0 0 6px rgba(34,197,94,0.4); }
        .dot-yellow { background: #f59e0b; box-shadow: 0 0 6px rgba(245,158,11,0.4); }
        .dot-red { background: #ef4444; box-shadow: 0 0 6px rgba(239,68,68,0.4); }
        .dot-gray { background: #94a3b8; }
        .error-text { color: #dc2626 !important; font-size: 12px; word-break: break-all; }

        /* ===== 用户计数 ===== */
        .user-count-wrap { text-align: center; padding: 12px 0; }
        .user-count-num {
            font-size: 42px; font-weight: 700;
            color: #6366F1;
            letter-spacing: -1px;
            line-height: 1;
            transition: all 0.3s ease;
        }
        .user-count-label {
            display: block; font-size: 13px; color: #94a3b8;
            margin-top: 6px;
        }

        /* ===== 用户列表 ===== */
        .user-table { font-size: 13px; }
        .table-header {
            display: grid;
            grid-template-columns: 1fr 1.2fr 1fr 60px;
            gap: 8px;
            padding: 8px 4px;
            color: #94a3b8;
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            border-bottom: 1px solid #f1f5f9;
        }
        .table-body { min-height: 60px; }
        .table-row {
            display: grid;
            grid-template-columns: 1fr 1.2fr 1fr 60px;
            gap: 8px;
            padding: 10px 4px;
            align-items: center;
            border-bottom: 1px solid #f8fafc;
            transition: background 0.15s;
        }
        .table-row:hover { background: #f8fafc; }
        .table-row:last-child { border-bottom: none; }
        .table-empty {
            text-align: center; color: #cbd5e1; padding: 24px 0;
            font-size: 13px;
        }
        .user-avatar {
            width: 28px; height: 28px; border-radius: 50%;
            background: #e2e8f0;
            display: inline-flex; align-items: center; justify-content: center;
            font-size: 12px; font-weight: 600; color: #94a3b8;
            flex-shrink: 0;
        }
        .user-info { display: flex; align-items: center; gap: 8px; }
        .user-name {
            font-weight: 500; color: #334155;
            white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
            max-width: 90px;
        }
        .userId-text {
            color: #94a3b8; font-size: 11px; font-family: monospace;
            white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
        }
        .active-time { color: #64748b; font-size: 12px; }
        .msg-count {
            font-weight: 600; color: #6366F1;
            text-align: center;
        }
        .status-cell { display: flex; align-items: center; gap: 4px; }
        .status-dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; }

        /* ===== 动画: 新用户加入 ===== */
        .table-row.new-user {
            animation: highlightRow 1.5s ease-out;
        }
        @keyframes highlightRow {
            0% { background: #eef2ff; }
            100% { background: transparent; }
        }

        /* ===== 数字跳动 ===== */
        .count-bump {
            animation: countBump 0.3s ease-out;
        }
        @keyframes countBump {
            0% { transform: scale(1.15); }
            100% { transform: scale(1); }
        }

        /* ===== 响应式 ===== */
        @media (max-width: 800px) {
            .main {
                grid-template-columns: 1fr;
                padding: 16px;
            }
            .topbar { padding: 10px 16px; }
            .panel-left { order: 1; }
            .panel-right { order: 2; }
        }
        """;

    // ==================== JavaScript ====================

    private static final String JS = """
        let pollTimer = null;
        let dashboardTimer = null;
        let scannedOnce = false;
        let knownUserIds = new Set();

        // ===== 初始化 =====
        function initLogin(qrUrl) {
            if (qrUrl && qrUrl.length > 0) {
                generateQR(qrUrl);
            }
            // 登录状态轮询
            startPolling();
            // 控制台数据轮询
            startDashboardPolling();
        }

        // ===== 二维码 =====
        function generateQR(qrUrl) {
            var box = document.getElementById('qrcode');
            box.classList.remove('fade-out');
            var script = document.createElement('script');
            script.src = 'https://cdn.jsdelivr.net/npm/qrcodejs@1.0.0/qrcode.min.js';
            script.onload = function() {
                box.innerHTML = '';
                new QRCode(box, {
                    text: qrUrl,
                    width: 200,
                    height: 200,
                    colorDark: '#1e293b',
                    colorLight: '#ffffff',
                    correctLevel: QRCode.CorrectLevel.M
                });
            };
            script.onerror = function() {
                box.innerHTML = '<p style="color:#94a3b8;padding:50px 0;">二维码加载失败</p>';
            };
            document.head.appendChild(script);
        }

        // ===== 登录状态轮询 =====
        function startPolling() {
            pollTimer = setInterval(function() {
                fetch('/login/status')
                    .then(function(r) { return r.json(); })
                    .then(function(data) {
                        var hint = document.getElementById('stateHint');
                        var sub = document.getElementById('stateSub');
                        var spinner = document.getElementById('spinner');
                        var badge = document.getElementById('botBadge');

                        if (data.status === 'SUCCESS') {
                            hint.textContent = '登录成功';
                            sub.textContent = '机器人已连接';
                            if (spinner) spinner.style.display = 'none';
                            setBadge(badge, 'connected', '已连接');
                            showResult('success');

                            // 3秒后重置扫码区域（显示已连接状态）
                            setTimeout(function() {
                                var area = document.getElementById('scanArea');
                                var result = document.getElementById('resultArea');
                                // 保留二维码位置显示"已连接"
                                result.style.display = 'block';
                                area.style.display = 'block';
                                var qrBox = document.getElementById('qrcode');
                                qrBox.classList.add('fade-out');
                                hint.textContent = '\\u2713 已连接';
                                sub.textContent = '机器人正常运行中';
                                if (spinner) spinner.style.display = 'none';
                            }, 3000);
                        } else if (data.status === 'FAILED' || data.status === 'TIMEOUT') {
                            hint.textContent = '登录失败';
                            sub.textContent = '请检查网络后重启';
                            if (spinner) spinner.style.display = 'none';
                            setBadge(badge, 'failed', '连接失败');
                            showResult('fail');
                        } else if (data.status === 'SCANNED' && !scannedOnce) {
                            scannedOnce = true;
                            hint.textContent = '正在验证身份...';
                            sub.textContent = '请稍候';
                            setBadge(badge, 'waiting', '验证中');
                        } else if (data.status === 'WAITING_SCAN') {
                            setBadge(badge, 'waiting', '等待扫码');
                        }
                    });
            }, 2000);
        }

        // ===== 控制台数据轮询（每 3 秒） =====
        function startDashboardPolling() {
            dashboardTimer = setInterval(function() {
                fetchBotStatus();
                fetchUsers();
            }, 3000);
            // 立即执行一次
            fetchBotStatus();
            fetchUsers();
        }

        // ===== 机器人状态 =====
        function fetchBotStatus() {
            fetch('/api/bot/status')
                .then(function(r) { return r.json(); })
                .then(function(data) {
                    var indicator = document.getElementById('botStatusIndicator');
                    var timeEl = document.getElementById('botLoginTime');
                    var errorRow = document.getElementById('botErrorRow');
                    var errorEl = document.getElementById('botError');
                    var badge = document.getElementById('botBadge');

                    if (data.status === 'CONNECTED') {
                        indicator.innerHTML = '<span class="dot dot-green"></span> \\u2713 已连接';
                        timeEl.textContent = data.loginTime || '—';
                        errorRow.style.display = 'none';
                        setBadge(badge, 'connected', '已连接');
                    } else if (data.status === 'FAILED') {
                        indicator.innerHTML = '<span class="dot dot-red"></span> \\u2716 登录失败';
                        timeEl.textContent = data.loginTime || '—';
                        if (data.error) {
                            errorRow.style.display = 'flex';
                            errorEl.textContent = data.error;
                        }
                        setBadge(badge, 'failed', '连接失败');
                    } else {
                        indicator.innerHTML = '<span class="dot dot-gray"></span> \\u25CB 未启动';
                        timeEl.textContent = '—';
                        errorRow.style.display = 'none';
                        setBadge(badge, 'waiting', '等待连接');
                    }
                    document.getElementById('updateTime').textContent =
                        new Date().toLocaleTimeString();
                });
        }

        // ===== 用户列表 =====
        function fetchUsers() {
            fetch('/api/users')
                .then(function(r) { return r.json(); })
                .then(function(data) {
                    var countEl = document.getElementById('userCount');
                    var oldCount = parseInt(countEl.textContent);
                    var newCount = data.total || 0;

                    // 更新计数
                    countEl.textContent = newCount;
                    if (newCount > oldCount) {
                        countEl.classList.remove('count-bump');
                        void countEl.offsetWidth;
                        countEl.classList.add('count-bump');
                    }

                    var tbody = document.getElementById('userTableBody');
                    if (!data.users || data.users.length === 0) {
                        tbody.innerHTML = '<div class="table-empty">暂无用户数据</div>';
                        return;
                    }

                    var newIds = new Set();
                    var html = '';
                    for (var i = 0; i < data.users.length; i++) {
                        var u = data.users[i];
                        newIds.add(u.userId);
                        var isNew = !knownUserIds.has(u.userId);
                        var rowClass = 'table-row' + (isNew ? ' new-user' : '');

                        var userIdShort = u.userId.length > 12
                            ? u.userId.substring(0, 6) + '...' + u.userId.slice(-4)
                            : u.userId;

                        var displayName = u.nickname || '微信用户';
                        var avatarLetter = displayName.charAt(0);

                        // 计算活跃状态
                        var activeLabel = formatActiveTime(u.lastActiveTime);
                        var isActive = isRecentlyActive(u.lastActiveTime);

                        html += '<div class="' + rowClass + '">';
                        html += '  <div class="user-info">';
                        html += '    <span class="user-avatar">' + escapeHtml(avatarLetter) + '</span>';
                        html += '    <span class="user-name">' + escapeHtml(displayName) + '</span>';
                        html += '  </div>';
                        html += '  <span class="userId-text">' + escapeHtml(userIdShort) + '</span>';
                        html += '  <span class="active-time"><span class="status-dot" style="background:' +
                            (isActive ? '#22c55e' : '#94a3b8') + ';margin-right:4px;"></span>' +
                            escapeHtml(activeLabel) + '</span>';
                        html += '  <span class="msg-count">' + u.interactionCount + '</span>';
                        html += '</div>';
                    }
                    knownUserIds = newIds;
                    tbody.innerHTML = html;
                });
        }

        // ===== 工具函数 =====

        function isRecentlyActive(timeStr) {
            if (!timeStr) return false;
            try {
                var then = new Date(timeStr.replace(' ', 'T'));
                var now = new Date();
                return (now - then) < 30 * 60 * 1000; // 30分钟内算活跃
            } catch(e) { return false; }
        }

        function formatActiveTime(timeStr) {
            if (!timeStr) return '未知';
            try {
                var then = new Date(timeStr.replace(' ', 'T'));
                var now = new Date();
                var diffMs = now - then;
                var diffMin = Math.floor(diffMs / 60000);
                if (diffMin < 1) return '刚刚';
                if (diffMin < 60) return diffMin + '分钟前';
                var diffHour = Math.floor(diffMin / 60);
                if (diffHour < 24) return diffHour + '小时前';
                var diffDay = Math.floor(diffHour / 24);
                if (diffDay < 7) return diffDay + '天前';
                return timeStr.split(' ')[0];
            } catch(e) { return timeStr; }
        }

        function setBadge(el, cls, text) {
            el.className = 'badge ' + cls;
            el.textContent = text;
        }

        function escapeHtml(s) {
            if (!s) return '';
            return s.replace(/&/g,'&amp;').replace(/"/g,'&quot;')
                    .replace(/</g,'&lt;').replace(/>/g,'&gt;');
        }

        function showResult(type) {
            var scanArea = document.getElementById('scanArea');
            var resultArea = document.getElementById('resultArea');
            resultArea.style.display = 'block';

            var icon = document.getElementById('resultIcon');
            var text = document.getElementById('resultText');

            if (type === 'success') {
                icon.className = 'result-icon success';
                icon.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="white" ' +
                    'stroke-width="3" stroke-linecap="round" stroke-linejoin="round">' +
                    '<polyline points="20 6 9 17 4 12"></polyline></svg>';
                text.textContent = '连接成功';
            } else {
                icon.className = 'result-icon fail';
                icon.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="white" ' +
                    'stroke-width="3" stroke-linecap="round" stroke-linejoin="round">' +
                    '<line x1="18" y1="6" x2="6" y2="18"></line>' +
                    '<line x1="6" y1="6" x2="18" y2="18"></line></svg>';
                text.textContent = '连接失败，请重启应用';
            }
            stopPolling();
        }

        // ===== 清理 =====
        function stopPolling() {
            if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
        }

        // 启动
        initLogin(QR_URL);
        """;

    // ==================== 工具方法 ====================

    private static String jsonString(String s) {
        if (s == null) return "null";
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

    private static String jsonEscape(String s) {
        return jsonString(s).replaceAll("^\"|\"$", "");
    }
}
