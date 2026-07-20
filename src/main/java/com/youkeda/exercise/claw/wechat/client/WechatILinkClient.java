package com.youkeda.exercise.claw.wechat.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lth.wechat.ilink.ILinkClient;
import com.lth.wechat.ilink.LoginCredentials;
import com.lth.wechat.ilink.dto.login.LoginStatus;
import com.lth.wechat.ilink.dto.login.QrCodeInfo;
import com.lth.wechat.ilink.dto.message.ReceiveMessagesResult;
import com.lth.wechat.ilink.entity.config.BaseInfo;
import com.lth.wechat.ilink.entity.media.GetUploadUrlReq;
import com.lth.wechat.ilink.utils.MediaUtils;
import com.youkeda.exercise.claw.wechat.config.WechatProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * 微信 iLink 客户端封装
 *
 * 职责：
 * - SDK 初始化和登录
 * - 消息收发
 * - 媒体上传（含 SDK 回退机制）
 */
@Slf4j
@Component
public class WechatILinkClient {

    private final WechatProperties wechatProperties;

    private ILinkClient client;
    private LoginCredentials credentials;

    @Getter
    private volatile boolean loggedIn = false;

    private static final int MAX_LOGIN_RETRIES = 3;
    private static final int LOGIN_RETRY_INTERVAL_MS = 5000;

    /** 媒体类型：图片 */
    private static final int MEDIA_TYPE_IMAGE = 1;

    // ========== 自定义上传常量 ==========

    /** getUploadUrl API 路径 */
    private static final String GET_UPLOAD_URL_PATH = "/ilink/bot/getuploadurl";

    /** 默认 API 地址 */
    private static final String DEFAULT_API_BASE_URL = "https://ilinkai.weixin.qq.com";

    /** 渠道版本 */
    private static final String CHANNEL_VERSION = "1.0.0";

    private final SecureRandom secureRandom = new SecureRandom();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WechatILinkClient(WechatProperties wechatProperties) {
        this.wechatProperties = wechatProperties;
    }

    @PostConstruct
    public void init() {
        if (!wechatProperties.isEnabled()) {
            log.info("微信 iLink 功能未启用 (wechat.ilink.enabled=false)");
            return;
        }

        log.info("微信 iLink 客户端初始化开始");
        client = new ILinkClient();
        CompletableFuture.runAsync(this::login);
    }

    /**
     * 扫码登录流程
     */
    private void login() {
        try {
            // 1. 获取二维码
            QrCodeInfo qrCode = client.getQrCodeInfo();
            log.info("请扫码登录: imageUrl={}", qrCode.getImageUrl());
            log.info("二维码ID: {}", qrCode.getQrcodeId());

            // 2. 等待扫码登录（带重试）
            LoginStatus status = pollLoginWithRetry(qrCode.getQrcodeId());

            if (status == null || !status.isSuccess()) {
                log.error("微信登录失败，已重试{}次，服务已禁用", MAX_LOGIN_RETRIES);
                return;
            }

            // 3. 保存登录凭证
            credentials = new LoginCredentials(
                    status.getToken(),
                    status.getUserId(),
                    status.getApiBaseUrl()
            );
            loggedIn = true;
            log.info("微信登录成功，开始监听消息");

        } catch (Exception e) {
            log.error("微信登录过程中发生异常，服务已禁用", e);
        }
    }

    /**
     * 轮询登录状态（带重试）
     */
    private LoginStatus pollLoginWithRetry(String qrcodeId) {
        int retries = 0;
        while (retries < MAX_LOGIN_RETRIES) {
            try {
                while (true) {
                    Thread.sleep(wechatProperties.getLoginPollIntervalMs());
                    LoginStatus status = client.pollLoginStatus(qrcodeId);
                    log.info("登录状态: {}", status.getStatus());

                    if (status.isSuccess()) {
                        return status;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("登录轮询被中断");
                return null;
            } catch (Exception e) {
                retries++;
                log.warn("登录轮询异常 (第{}次)，{}ms后重试: {}",
                        retries, LOGIN_RETRY_INTERVAL_MS, e.getMessage());
                if (retries < MAX_LOGIN_RETRIES) {
                    try {
                        Thread.sleep(LOGIN_RETRY_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 发送文本消息
     */
    public void sendTextMessage(String toUserId, String contextToken, String text) {
        if (!loggedIn || credentials == null) {
            log.warn("微信未登录，无法发送消息");
            return;
        }
        try {
            client.sendTextMessage(credentials, toUserId, contextToken, text);
            log.info("发送回复 | to={} | text={}", toUserId, text);
        } catch (Exception e) {
            log.error("发送消息失败 | to={} | error={}", toUserId, e.getMessage());
        }
    }

    /**
     * 接收消息
     *
     * @param cursor 分页游标，首次传空字符串
     * @return 接收消息结果，异常时返回 null
     */
    public ReceiveMessagesResult receiveMessages(String cursor) {
        if (!loggedIn || credentials == null) {
            return null;
        }
        try {
            return client.receiveMessages(credentials, cursor);
        } catch (Exception e) {
            log.warn("接收消息异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 发送图片消息
     *
     * @param toUserId    目标用户 ID
     * @param contextToken 上下文 token
     * @param mediaInfo   已上传的媒体信息
     */
    public void sendImageMessage(String toUserId, String contextToken, ILinkClient.MediaInfo mediaInfo) {
        if (!loggedIn || credentials == null) {
            log.warn("微信未登录，无法发送图片消息");
            return;
        }
        try {
            client.sendImageMessage(credentials, toUserId, contextToken, mediaInfo);
            log.info("发送图片消息 | to={}", toUserId);
        } catch (Exception e) {
            log.error("发送图片消息失败 | to={} | error={}", toUserId, e.getMessage());
        }
    }

    /**
     * 从微信 CDN 下载媒体文件（图片、语音等）
     *
     * @param encryptQueryParam 加密查询参数
     * @param aesKey            解密密钥
     * @return 媒体文件字节数组，失败时返回 null
     */
    public byte[] downloadMedia(String encryptQueryParam, String aesKey) {
        if (!loggedIn || credentials == null) {
            log.warn("微信未登录，无法下载媒体");
            return null;
        }
        try {
            byte[] data = client.downloadMedia(encryptQueryParam, aesKey);
            log.info("媒体下载成功，大小={} bytes", data.length);
            return data;
        } catch (Exception e) {
            log.error("媒体下载失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 上传媒体文件到微信 CDN
     *
     * 先尝试 SDK 方式，SDK 因 GetUploadUrlResp 字段名解析问题失败时使用自定义实现。
     *
     * @param data     文件字节数组
     * @param fileName 文件名（如 "generated.png"）
     * @param toUserId 接收者用户 ID（用于 getUploadUrl 的 to_user_id 字段）
     * @return MediaInfo（含 encryptQueryParam / aesKey），失败时返回 null
     */
    public ILinkClient.MediaInfo uploadMedia(byte[] data, String fileName, String toUserId) {
        if (!loggedIn || credentials == null) {
            log.warn("微信未登录，无法上传媒体");
            return null;
        }

        // 先尝试 SDK 方式
        try {
            ILinkClient.MediaInfo mediaInfo = client.uploadMedia(credentials, MEDIA_TYPE_IMAGE, fileName, data);
            log.info("媒体上传成功 | file={} | size={}", fileName, data.length);
            return mediaInfo;
        } catch (Exception e) {
            log.warn("SDK 上传失败，尝试自定义上传 | file={} | size={} | error={}",
                    fileName, data.length, e.toString());
        }

        // SDK 失败时使用自定义上传
        try {
            return doUploadMedia(data, fileName, toUserId);
        } catch (Exception e) {
            log.error("自定义上传失败 | file={} | size={}", fileName, data.length, e);
            return null;
        }
    }

    /**
     * 上传媒体文件到微信 CDN（不指定接收者用户 ID，兼容旧调用）
     *
     * @deprecated 使用 {@link #uploadMedia(byte[], String, String)} 替代
     */
    @Deprecated
    public ILinkClient.MediaInfo uploadMedia(byte[] data, String fileName) {
        return uploadMedia(data, fileName, fileName);
    }

    // ======================================================================
    //  自定义媒体上传实现
    //
    //  流程：SDK MediaUtils 生成密钥 + AES 加密 → 自定义 getUploadUrl 获取 CDN 地址
    //       （JsonNode 解析解决 SDK GetUploadUrlResp 字段名 bug）→ MediaUtils CDN 上传
    // ======================================================================

    /**
     * 自定义媒体上传实现
     * <p>
     * 加密和 CDN 上传部分复用 SDK 的 {@link MediaUtils} 静态方法，
     * 仅绕过 SDK 中 GetUploadUrlResp 响应解析的 bug（upload_param 字段名不匹配）。
     */
    private ILinkClient.MediaInfo doUploadMedia(byte[] data, String fileName, String toUserId) throws Exception {
        // 1. SDK 生成密钥和加密数据
        String fileKey = MediaUtils.generateFileKey();
        byte[] aesKey = MediaUtils.generateAesKey();
        String rawMd5 = MediaUtils.calculateMd5(data);
        long rawSize = data.length;
        long encryptedSize = MediaUtils.calculateEncryptedSize(rawSize);
        byte[] encryptedData = MediaUtils.encryptAesEcb(data, aesKey);

        // 2. 构建 getUploadUrl 请求体（使用 SDK POJO 确保 @JsonProperty 注解生效）
        String requestBody = buildGetUploadUrlRequest(fileKey, toUserId, rawSize, rawMd5, encryptedSize, aesKey);

        // 3. 调用 getUploadUrl API
        String apiBaseUrl = credentials.getApiBaseUrl();
        if (apiBaseUrl == null) {
            apiBaseUrl = DEFAULT_API_BASE_URL;
        }

        String uploadParam = callGetUploadUrl(apiBaseUrl, requestBody);
        if (uploadParam == null) {
            return null;
        }

        // 4. SDK CDN 上传（复用 MediaUtils.uploadToCdn，确保上传格式完全一致）
        String xEncryptedParam = MediaUtils.uploadToCdn(encryptedData, uploadParam, fileKey);

        // 5. 构建 MediaInfo
        // 注意：SDK 的做法是将 AES key 的 hex 字符串的 UTF-8 字节再做 base64，
        // 不是直接对 raw key bytes 做 base64
        String aesKeyHex = MediaUtils.aesKeyToHex(aesKey);
        String base64AesKey = Base64.getEncoder().encodeToString(aesKeyHex.getBytes(StandardCharsets.UTF_8));
        return new ILinkClient.MediaInfo(xEncryptedParam, base64AesKey, encryptedSize);
    }

    /**
     * 构建 getUploadUrl 请求体 JSON
     *
     * 使用 SDK 的 GetUploadUrlReq POJO 序列化，确保 @JsonProperty(snake_case) 注解生效
     */
    private String buildGetUploadUrlRequest(String fileKey, String toUserId,
                                            long rawSize, String rawMd5,
                                            long encryptedSize, byte[] aesKey) {
        GetUploadUrlReq req = new GetUploadUrlReq();
        req.setFileKey(fileKey);
        req.setMediaType(MEDIA_TYPE_IMAGE);
        req.setToUserId(toUserId);
        req.setRawSize(rawSize);
        req.setRawFileMd5(rawMd5);
        req.setFileSize(encryptedSize);
        req.setNoNeedThumb(true);
        req.setAesKey(MediaUtils.aesKeyToHex(aesKey));

        BaseInfo baseInfo = new BaseInfo();
        baseInfo.setChannelVersion(CHANNEL_VERSION);
        req.setBaseInfo(baseInfo);

        try {
            return objectMapper.writeValueAsString(req);
        } catch (Exception e) {
            throw new RuntimeException("序列化 GetUploadUrlReq 失败", e);
        }
    }

    /**
     * 解析 getUploadUrl 响应中的 upload_param 字段
     * <p>
     * 兼容两种字段命名风格：
     * - upload_param（下划线，SDK GetUploadUrlResp 预期格式）
     * - uploadParam（驼峰，API 实际返回格式）
     */
    private String parseUploadParam(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        if (root.has("upload_param")) {
            JsonNode node = root.get("upload_param");
            if (node != null && !node.isNull()) {
                return node.asText();
            }
        }

        if (root.has("uploadParam")) {
            JsonNode node = root.get("uploadParam");
            if (node != null && !node.isNull()) {
                return node.asText();
            }
        }

        return null;
    }

    /**
     * 调用 getUploadUrl API 获取 CDN 上传参数
     */
    private String callGetUploadUrl(String apiBaseUrl, String requestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + GET_UPLOAD_URL_PATH))
                .header("Content-Type", "application/json")
                .header("AuthorizationType", "ilink_bot_token")
                .header("Authorization", "Bearer " + credentials.getBotToken())
                .header("X-WECHAT-UIN", generateXWechatUin())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        log.info("getUploadUrl 请求 URL={} | body={}", apiBaseUrl + GET_UPLOAD_URL_PATH, requestBody);

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        log.info("getUploadUrl 响应 code={} | body={}", response.statusCode(), response.body());

        // 兼容 upload_param 和 uploadParam 两种字段名
        String uploadParam = parseUploadParam(response.body());
        if (uploadParam == null) {
            log.warn("getUploadUrl 响应缺少上传参数: {}", response.body());
        }
        return uploadParam;
    }

    // ========== 加密工具方法 ==========

    /**
     * 生成 X-WECHAT-UIN 请求头
     * <p>
     * 算法：随机 32 位无符号整数转为字符串 → Base64 编码
     */
    private String generateXWechatUin() {
        long val = secureRandom.nextLong() & 0xFFFFFFFFL;
        return Base64.getEncoder().encodeToString(Long.toString(val).getBytes(StandardCharsets.UTF_8));
    }
}
