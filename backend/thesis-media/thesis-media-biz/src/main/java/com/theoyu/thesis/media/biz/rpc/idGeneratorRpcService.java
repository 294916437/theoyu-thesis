package com.theoyu.thesis.media.biz.rpc;

import com.theoyu.thesis.id.generator.api.IdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class idGeneratorRpcService {
    @Resource
    private IdGeneratorFeignApi idGeneratorFeignApi;

    /**
     * Leaf 号段模式：房间 ID 业务标识
     */
    private static final String BIZ_TAG_ROOM_ID = "leaf-segment-room-id";
    /**
     * Leaf 雪花模式：用户 ID 业务标识
     */
    private static final String BIZ_TAG_ROOM_MSG_ID = "leaf-segment-room-message-id";
    /**
     * 调用分布式 ID 生成服务生成用户应用 ID
     *
     * @return
     */
    public String getRoomId() {
        return idGeneratorFeignApi.getSegmentId(BIZ_TAG_ROOM_ID);
    }
    /**
     * 调用分布式 ID 生成服务用户 ID
     *
     * @return
     */
    public String getRoomMsgId() {
        return idGeneratorFeignApi.getSnowflakeId(BIZ_TAG_ROOM_MSG_ID);
    }

}
