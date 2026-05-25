package com.theoyu.thesis.media.biz.enums;

import com.theoyu.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {
    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("MEDIA-10000", "系统错误，请稍后尝试..."),
    PARAM_NOT_VALID("MEDIA-10001", "参数错误"),
    // ==================== 房间相关 10xx ====================
    ROOM_NOT_FOUND("MEDIA-1001", "房间不存在"),
    ROOM_FULL("MEDIA-1002", "房间已满"),
    ROOM_CLOSED("MEDIA-1003", "房间已关闭"),
    ROOM_ACCESS_DENIED("MEDIA-1004", "无权限访问该房间"),
    ROOM_ALREADY_EXISTS("MEDIA-1005", "房间已存在"),
    ROOM_QUOTA_EXCEEDED("MEDIA-1006", "房间创建数量已达上限"),
    ROOM_NO_DUPLICATE("MEDIA-1007", "房间号已存在"),
    USER_NOT_IN_ROOM("MEDIA-1008", "用户不在房间内"),
    MESSAGE_SEND_FAILED("MEDIA-1009", "消息发送失败"),
    MESSAGE_CONTENT_EMPTY("MEDIA-1010", "消息内容为空"),
    ROOM_DETAIL_QUERY_FAILED("MEDIA-1011", "查询会议详情失败"),
    RECORDING_ALREADY_STARTED("MEDIA-1012", "该房间已在录制中"),
    RECORDING_NOT_FOUND("MEDIA-1013", "录制记录不存在"),
    RECORDING_NOT_ACTIVE("MEDIA-1014", "当前没有进行中的录制"),
    RECORDING_START_FAILED("MEDIA-1015", "启动录制失败，请稍后重试"),
    RECORDING_STOP_FAILED("MEDIA-1016", "停止录制失败，请稍后重试"),
    RECORDING_STATUS_QUERY_FAILED("MEDIA-1017", "查询录制状态失败"),
    RECORDING_PERMISSION_DENIED("MEDIA-1018", "无权操作录制，仅房主可操作"),
    ROOM_NOT_ACTIVE_FOR_RECORDING("MEDIA-1019", "会议未在进行中，无法录制"),
    ROOM_UPDATE_FAILED("MEDIA-1020", "进行中的会议不可编辑"),
    ROOM_DELETE_FAILED("MEDIA-1021", "进行中的会议不可删除"),

    // ==================== 用户相关 20xx ====================
    USER_NOT_FOUND("MEDIA-2001", "用户不存在"),
    TOKEN_INVALID("MEDIA-2002", "Token无效"),
    TOKEN_EXPIRED("MEDIA-2003", "Token已过期"),
    USER_ALREADY_IN_ROOM("MEDIA-2004", "用户已在房间中"),

    // ==================== 参与者相关 30xx ====================
    PARTICIPANT_NOT_FOUND("MEDIA-3001", "参与者不存在"),
    PARTICIPANT_ALREADY_EXISTS("MEDIA-3002", "参与者已存在"),

    // ==================== SFU 节点相关 40xx ====================
    SFU_NODE_NOT_FOUND("MEDIA-4001", "SFU节点不存在"),
    SFU_NODE_UNAVAILABLE("MEDIA-4002", "SFU节点不可用"),

    // ==================== 媒体相关 50xx ====================
    MEDIA_STATS_INVALID("MEDIA-5001", "媒体统计数据无效"),
    ;


    // 异常码，用于表示微服务和错误类型
    private final String errorCode;
    // 错误信息，展示错误的详细信息
    private final String errorMessage;


}


