package com.theoyu.thesis.media.biz.service;

import com.theoyu.thesis.media.biz.model.entity.SfuNodePO;

/**
 * SFU 节点发现、同步与负载分配
 */
public interface SfuNodeService {

    /**
     * 从 Nacos 同步 sfu-server 实例到 MySQL
     */
    void syncNodesFromNacos();

    /**
     * 为房间分配或解析已绑定的 SFU 节点
     *
     * @param roomId 房间 ID
     * @return 可用节点；分配失败返回 null
     */
    SfuNodePO allocateNodeForRoom(Long roomId);

    /**
     * 根据节点 ID 获取节点
     */
    SfuNodePO getNodeById(Long nodeId);

    /**
     * 构建客户端 Socket.IO 连接地址
     */
    String buildSfuServerUrl(SfuNodePO node);

    /**
     * 递减 SFU 节点的负载（房间关闭时调用）
     * 
     * @param sfuNodeId SFU 节点 ID
     * @return 递减后的负载值
     */
    Integer decrementNodeLoad(Long sfuNodeId);
}
