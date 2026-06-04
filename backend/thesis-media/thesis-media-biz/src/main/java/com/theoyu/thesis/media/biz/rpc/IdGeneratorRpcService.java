package com.theoyu.thesis.media.biz.rpc;

import com.theoyu.thesis.id.generator.api.IdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class IdGeneratorRpcService {
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
     * Leaf 号段模式：SFU 节点 ID 业务标识
     */
    private static final String BIZ_TAG_SFU_NODE_ID = "leaf-segment-sfu-node-id";
    /**
     * 调用分布式 ID 生成服务生成房间ID
     *
     * @return
     */
    public String getRoomId() {
        return idGeneratorFeignApi.getSegmentId(BIZ_TAG_ROOM_ID);
    }
    /**
     * 调用分布式 ID 生成房间内消息ID
     *
     * @return
     */
    public String getRoomMsgId() {
        return idGeneratorFeignApi.getSnowflakeId(BIZ_TAG_ROOM_MSG_ID);
    }

    /**
     * 生成分布式 SFU 节点主键 ID
     */
    public String getSfuNodeId() {
        return idGeneratorFeignApi.getSegmentId(BIZ_TAG_SFU_NODE_ID);
    }

}
