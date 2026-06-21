package com.theoyu.thesis.auth.constants;

public class RedisKeyConstants {
    /**
     * 验证码 KEY 前缀
     */
    private static final String VERIFICATION_CODE_KEY_PREFIX  = "verification_code:";
    /**
     * 验证码发送频率限制 KEY 前缀
     */
    private static final String VERIFICATION_CODE_SEND_LIMIT_KEY_PREFIX = "verification_code_send_limit:";

    /**
     * 构建验证码完整的 KEY
     * @param phone 手机号
     * @return string
     */
    public static String buildVerificationCodeKey(String phone) {
        return VERIFICATION_CODE_KEY_PREFIX + phone;
    }

    /**
     * 构建验证码发送频率限制完整的 KEY
     * @param phone 手机号
     * @return string
     */
    public static String buildVerificationCodeSendLimitKey(String phone) {
        return VERIFICATION_CODE_SEND_LIMIT_KEY_PREFIX + phone;
    }
}
