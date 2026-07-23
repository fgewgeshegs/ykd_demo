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
            "<title>微信扫码登录</title>\n" +
            "<style>\n" +
            CSS +
            "</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"container\">\n" +
            "  <div class=\"card\">\n" +
            "\n" +
            "    <!-- 等待扫码区域 -->\n" +
            "    <div id=\"scanArea\">\n" +
            "      <div id=\"qrcode\" class=\"qrcode-box\"></div>\n" +
            "      <div class=\"spinner\"></div>\n" +
            "      <p class=\"hint\">请使用微信扫码连接</p>\n" +
            "      <p class=\"sub-hint\">等待扫码中...</p>\n" +
            "    </div>\n" +
            "\n" +
            "    <!-- 结果区域（成功/失败共用） -->\n" +
            "    <div id=\"resultArea\" class=\"result-area\" style=\"display:none;\">\n" +
            "      <div id=\"resultIcon\" class=\"result-icon\"></div>\n" +
            "      <p id=\"resultText\" class=\"result-text\"></p>\n" +
            "      <button id=\"retryBtn\" class=\"retry-btn\" style=\"display:none;\" onclick=\"retry()\">重新扫码</button>\n" +
            "    </div>\n" +
            "\n" +
            "  </div>\n" +
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

    // ==================== CSS（内联） ====================

    private static final String CSS = """
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC",
                         "Microsoft YaHei", sans-serif;
            background: #f0f2f5;
            display: flex; justify-content: center; align-items: center;
            min-height: 100vh;
        }
        .container { width: 100%; max-width: 400px; padding: 24px; }
        .card {
            background: #fff; border-radius: 16px; padding: 40px 32px;
            box-shadow: 0 2px 16px rgba(0,0,0,0.08); text-align: center;
        }
        .qrcode-box {
            display: inline-block; padding: 12px; background: #fff;
            border: 2px solid #e8e8e8; border-radius: 12px; margin-bottom: 20px;
        }
        .qrcode-box canvas, .qrcode-box img {
            display: block; width: 220px; height: 220px;
        }
        .spinner {
            width: 32px; height: 32px; margin: 8px auto 16px;
            border: 3px solid #e0e0e0; border-top-color: #07c160;
            border-radius: 50%; animation: spin 0.8s linear infinite;
        }
        @keyframes spin { to { transform: rotate(360deg); } }
        .hint { font-size: 16px; color: #333; margin-bottom: 4px; }
        .sub-hint { font-size: 13px; color: #999; }

        /* 结果区域 */
        .result-area { padding-top: 20px; }
        .result-icon {
            width: 80px; height: 80px; margin: 0 auto 24px;
            border-radius: 50%; display: flex; align-items: center; justify-content: center;
            animation: popIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
        }
        @keyframes popIn {
            0% { transform: scale(0); opacity: 0; }
            100% { transform: scale(1); opacity: 1; }
        }
        .result-icon.success { background: #07c160; }
        .result-icon.fail { background: #fa5151; }
        .result-icon svg { width: 40px; height: 40px; }
        .result-text { font-size: 17px; color: #333; margin-bottom: 20px; }
        .retry-btn {
            padding: 10px 32px; font-size: 15px; color: #fff;
            background: #07c160; border: none; border-radius: 8px;
            cursor: pointer; transition: background 0.2s;
        }
        .retry-btn:hover { background: #06ad56; }
        """;

    // ==================== JS（内联） ====================

    // language=JavaScript
    private static final String JS = """
        let pollTimer = null;

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
                    '<p style="color:#999;">二维码加载失败，请<a href="' +
                    escapeHtml(qrUrl) + '" target="_blank" style="color:#07c160;">点击此处</a>打开扫码页面</p>';
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
                            showResult('success');
                            stopPolling();
                            // SUCCESS 3 秒后尝试关闭窗口
                            setTimeout(tryClose, 3000);
                        } else if (data.status === 'FAILED' || data.status === 'TIMEOUT') {
                            showResult('fail');
                            stopPolling();
                            // 失败 10 秒后尝试关闭窗口
                            setTimeout(tryClose, 10000);
                        }
                        // WAITING_SCAN 什么都不做，继续轮询
                    })
                    .catch(function() {
                        // 网络错误静默忽略，继续轮询
                    });
            }, 2000);
        }

        function stopPolling() {
            if (pollTimer) {
                clearInterval(pollTimer);
                pollTimer = null;
            }
        }

        function showResult(type) {
            document.getElementById('scanArea').style.display = 'none';
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
                text.textContent = '用户已成功连接';
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
            // 尝试关闭窗口（浏览器安全策略可能限制，静默失败即可）
            try { window.close(); } catch(e) {}
        }
        """;
}