package com.theoyu.thesis.chat.biz.rpc;

import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.kv.api.KeyValueFeignApi;
import com.theoyu.thesis.kv.dto.request.AddMessageContentReqDTO;
import com.theoyu.thesis.kv.dto.request.DeleteMessageContentReqDTO;
import com.theoyu.thesis.kv.dto.request.FindMessageContentReqDTO;
import com.theoyu.thesis.kv.dto.response.FindMessageContentRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class KVRpcService {

    @Resource
    private KeyValueFeignApi keyValueFeignApi;


    /**
     * 保存消息内容到 Cassandra
     *
     * @param content 消息文本内容
     * @return UUID（成功时）或 null（失败时）
     */
    public String saveMessageContent(String content) {
        String uuid = UUID.randomUUID().toString();

        AddMessageContentReqDTO addReqDTO = new AddMessageContentReqDTO();
        addReqDTO.setUuid(uuid);
        addReqDTO.setContent(content);

        Response<?> response = keyValueFeignApi.addMessageContent(addReqDTO);

        if (Objects.isNull(response) || !response.isSuccess()) {
            return null;
        }

        return uuid;
    }

    /**
     * 从 Cassandra 获取消息内容
     *
     * @param uuid 消息内容的 UUID
     * @return 消息文本内容（成功时）或 null（失败时）
     */
    public String getMessageContent(String uuid) {
        FindMessageContentReqDTO findReqDTO = new FindMessageContentReqDTO();
        findReqDTO.setUuid(uuid);

        Response<FindMessageContentRspDTO> response = keyValueFeignApi.findMessageContent(findReqDTO);

        if (Objects.isNull(response) || !response.isSuccess() || Objects.isNull(response.getData())) {
            return null;
        }

        return response.getData().getContent();
    }

    /**
     * 删除消息内容（用于消息撤回或删除）
     *
     * @param uuid 消息内容的 UUID
     * @return true-成功，false-失败
     */
    public boolean deleteMessageContent(String uuid) {
        DeleteMessageContentReqDTO deleteReqDTO = new DeleteMessageContentReqDTO();
        deleteReqDTO.setUuid(uuid);

        Response<?> response = keyValueFeignApi.deleteMessageContent(deleteReqDTO);

        if (Objects.isNull(response) || !response.isSuccess()) {
            return false;
        }

        return true;
    }

    /**
     * 批量获取消息内容
     * 用于查询消息列表时批量获取文本内容
     *
     * @param uuids UUID 列表
     * @return UUID -> 消息内容的映射
     */
    public Map<String, String> batchGetMessageContent(List<String> uuids) {
        Map<String, String> resultMap = new HashMap<>();

        if (Objects.isNull(uuids) || uuids.isEmpty()) {
            return resultMap;
        }

        // 并发查询提高性能
        uuids.parallelStream().forEach(uuid -> {
            String content = getMessageContent(uuid);
            if (content != null) {
                resultMap.put(uuid, content);
            }
        });

        return resultMap;
    }

}
