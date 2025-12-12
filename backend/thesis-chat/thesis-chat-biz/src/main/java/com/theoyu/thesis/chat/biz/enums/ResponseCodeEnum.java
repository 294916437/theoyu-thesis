package com.theoyu.thesis.chat.biz.enums;

import com.theoyu.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {
    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("MESSAGE-10000", "系统错误，请稍后尝试..."),
    PARAM_NOT_VALID("MESSAGE-10001", "参数错误"),
    // ----------- 会话异常状态码 -----------c
    CONVERSATION_NOT_FOUND("MESSAGE-20001", "会话不存在"),
    CONVERSATION_ACCESS_DENIED("MESSAGE-20002", "无权访问该会话"),
    CONVERSATION_NOT_ACTIVE("MESSAGE-20003", "会话已失效"),
    CONVERSATION_ALREADY_EXISTS("MESSAGE-20004", "会话已存在"),
    CONVERSATION_TYPE_INVALID("MESSAGE-20005", "会话类型无效"),
    PARTICIPANT_COUNT_INVALID("MESSAGE-20006", "参与者数量不符合要求"),
    USER_NOT_FOUND("MESSAGE-20007", "用户不存在"),
    CONVERSATION_CREATE_FAILED("MESSAGE-20008", "创建会话失败"),
    CANNOT_CHAT_WITH_SELF("MESSAGE-20009", "不能和自己创建会话"),
    // ----------- 消息异常状态码 -----------
    MESSAGE_TYPE_INVALID("MESSAGE-30001", "消息类型无效"),
    MESSAGE_CONTENT_EMPTY("MESSAGE-30002", "消息内容不能为空"),
    MESSAGE_SEND_FAILED("MESSAGE-30003", "发送消息失败"),
    MESSAGE_TOO_FREQUENT("MESSAGE-30004", "发送消息过于频繁，请稍后再试"),
    MESSAGE_CONTENT_SAVE_FAILED("MESSAGE-30005", "保存消息内容失败"),
    MESSAGE_IMG_URIS_REQUIRED("MESSAGE-30006", "图片消息必须提供图片URL"),
    MESSAGE_VIDEO_URI_REQUIRED("MESSAGE-30007", "视频消息必须提供视频URL"),
    ;



    // 异常码，用于表示微服务和错误类型
    private final String errorCode;
    // 错误信息，展示错误的详细信息
    private final String errorMessage;


}


