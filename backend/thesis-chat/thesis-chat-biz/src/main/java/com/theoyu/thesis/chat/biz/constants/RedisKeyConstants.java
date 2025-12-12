package com.theoyu.thesis.chat.biz.constants;

public class RedisKeyConstants {
    /**
     * 会话 KEY 前缀
     * 格式: message:conversation:{conversationId}
     */
    public static final String CONVERSATION_CACHE_KEY = "message:conversation:";
    /**
     * 会话详情 KEY 前缀
     * 格式: message:conversation:detail:{conversationId}:{userId}
     * 说明: 包含参与者信息的完整会话详情，不同用户看到的详情可能不同（如未读数）
     */
    public static final String CONVERSATION_DETAIL_CACHE_KEY = "message:conversation:detail:";

    /**
     * 会话列表缓存 KEY
     * 格式: message:conversation:list:{userId}:{cursor}
     * 说明: 基于游标的分页缓存，cursor为空时表示首页
     */
    public static final String CONVERSATION_LIST_CACHE_KEY = "message:conversation:list:";
    /**
     * 消息缓存 KEY
     * 格式: message:msg:{messageId}
     */
    public static final String MESSAGE_CACHE_KEY = "message:msg:";

    /**
     * 会话消息列表缓存 KEY
     * 格式: message:conversation:messages:{conversationId}:cursor:{cursor}
     * 缓存最新的一页消息（cursor为null时）
     */
    public static final String CONVERSATION_MESSAGES_CACHE_KEY = "message:conversation:messages:";

    /**
     * 用户消息发送频率限制 Key 前缀
     * Key: message:rate:limit:{userId}
     */
    public static final String RATE_LIMIT_KEY = "message:rate:limit:";

    /**
     * 通话状态 Key 前缀
     * Key: call:status:{callId}
     * Value: JSON (包含参与者、开始时间等)
     */
    public static final String CALL_STATUS_PREFIX = "call:status:";

    /**
     * 用户在线状态 Key 前缀
     * Key: user:online:{userId}
     * Value: timestamp
     */
    public static final String USER_ONLINE_PREFIX = "user:online:";


    /**
     * 构建通话状态 Key
     */
    public static String buildCallStatusKey(String callId) {
        return CALL_STATUS_PREFIX + callId;
    }

    /**
     * 构建用户在线状态 Key
     */
    public static String buildUserOnlineKey(Long userId) {
        return USER_ONLINE_PREFIX + userId;
    }
    /**
     * 构建会话基本信息缓存 Key
     * 格式: message:conversation:{conversationId}
     */
    public static String buildConversationKey(Long conversationId) {
        return CONVERSATION_CACHE_KEY + conversationId;
    }

    /**
     * 构建会话详情缓存 Key
     * 格式: message:conversation:detail:{conversationId}:{userId}
     */
    public static String buildConversationDetailKey(Long conversationId, Long userId) {
        return CONVERSATION_DETAIL_CACHE_KEY + conversationId + ":" + userId;
    }

    /**
     * 构建会话列表缓存 Key
     * 格式: message:conversation:list:{userId}:{cursor}
     */
    public static String buildConversationListKey(Long userId, Long cursor) {
        return CONVERSATION_LIST_CACHE_KEY + userId + ":" + (cursor != null ? cursor : "latest");
    }

    /**
     * 构建消息缓存 Key
     * 格式: message:msg:{messageId}
     */
    public static String buildMessageKey(Long messageId) {
        return MESSAGE_CACHE_KEY + messageId;
    }

    /**
     * 构建会话消息列表缓存 Key（最新一页）
     * 格式: message:conversation:messages:{conversationId}:latest
     */
    public static String buildConversationMessagesKey(Long conversationId) {
        return CONVERSATION_MESSAGES_CACHE_KEY + conversationId + ":latest";
    }
    /**
     * 构建消息发送频率限制 Key
     * 格式: message:rate:limit:{userId}
     */
    public static String buildRateLimitKey(Long userId) {
        return RATE_LIMIT_KEY + userId;
    }


}
