package com.youkeda.exercise.claw.wechat.login;

/**
 * 登录状态持有者，纯状态管理，不持有任何业务回调。
 *
 * 线程安全：status 用 volatile 保证跨线程可见。
 * WechatILinkClient 的异步轮询线程写，LoginPageServer 的 HTTP 线程读。
 */
public class LoginStateManager {

    private volatile LoginStatus status;
    private volatile String qrUrl;

    /** 默认构造，后续通过 updateQrUrl / updateStatus 填充 */
    public LoginStateManager() {
    }

    public LoginStatus getStatus() {
        return status;
    }

    public void updateStatus(LoginStatus status) {
        this.status = status;
    }

    public String getQrUrl() {
        return qrUrl;
    }

    public void updateQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }
}