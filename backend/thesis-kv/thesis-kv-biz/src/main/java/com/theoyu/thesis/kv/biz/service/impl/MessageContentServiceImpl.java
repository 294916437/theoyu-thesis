package com.theoyu.thesis.kv.biz.service.impl;

import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.kv.biz.enums.ResponseCodeEnum;
import com.theoyu.thesis.kv.biz.model.entity.MessageContentPO;
import com.theoyu.thesis.kv.biz.model.repository.MessageContentRepository;
import com.theoyu.thesis.kv.biz.service.MessageContentService;
import com.theoyu.thesis.kv.dto.request.*;
import com.theoyu.thesis.kv.dto.response.FindMessageContentRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * 消息内容 Service 实现类
 */
@Service
public class MessageContentServiceImpl implements MessageContentService {

    @Resource
    private  MessageContentRepository messageContentRepository;


    @Override
    public Response<?> addMessageContent(AddMessageContentReqDTO addMessageContentReqDTO) {
        // 笔记 ID
        String uuid = addMessageContentReqDTO.getUuid();
        // 笔记内容
        String content = addMessageContentReqDTO.getContent();

        // 构建数据库实体类
        MessageContentPO nodeContent = MessageContentPO.builder()
                .id(UUID.fromString(uuid))
                .content(content)
                .build();

        // 插入数据
        messageContentRepository.save(nodeContent);

        return Response.success();
    }

    /**
     * 查询消息内容
     **/
    @Override
    public Response<FindMessageContentRspDTO> findMessageContent(FindMessageContentReqDTO reqDTO) {
        // 消息 ID
        String uuid = reqDTO.getUuid();
        // 根据消息 ID 查询消息内容
        Optional<MessageContentPO> optional =messageContentRepository.findById(UUID.fromString(uuid));

        // 若消息内容不存在
        if (optional.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.MESSAGE_CONTENT_NOT_FOUND);
        }

        MessageContentPO messageContentPO = optional.get();
        // 构建返参 DTO
        FindMessageContentRspDTO findMessageContentRspDTO = FindMessageContentRspDTO.builder()
                .uuid(messageContentPO.getId())
                .content(messageContentPO.getContent())
                .build();

        return Response.success(findMessageContentRspDTO);
    }

    @Override
    public Response<?> deleteMessageContent(DeleteMessageContentReqDTO deleteMessageContentReqDTO) {
        // 笔记 ID
        String uuid = deleteMessageContentReqDTO.getUuid();
        // 删除笔记内容
        messageContentRepository.deleteById(UUID.fromString(uuid));

        return Response.success();
    }


}