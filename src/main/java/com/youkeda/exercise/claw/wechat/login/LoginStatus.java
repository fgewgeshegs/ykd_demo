package com.youkeda.exercise.claw.wechat.login;

/**
 * 微信扫码登录状态枚举
 *
 * 所有状态由 WechatILinkClient 根据真实 SDK 事件驱动更新，
 * 前端只读不写。
 */
public enum LoginStatus {

    /** 等待用户扫码 */
    WAITING_SCAN,

    /** 登录成功 */
    SUCCESS,

    /** 登录异常 */
    FAILED,

    /** 登录超时 */
    TIMEOUT
}