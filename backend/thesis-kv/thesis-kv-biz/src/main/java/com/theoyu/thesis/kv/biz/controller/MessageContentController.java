package com.theoyu.thesis.kv.biz.controller;

import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.kv.biz.service.MessageContentService;
import com.theoyu.thesis.kv.dto.request.AddMessageContentReqDTO;
import com.theoyu.thesis.kv.dto.request.DeleteMessageContentReqDTO;
import com.theoyu.thesis.kv.dto.request.FindMessageContentReqDTO;
import com.theoyu.thesis.kv.dto.response.FindMessageContentRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kv")
@Slf4j
public class MessageContentController {
    @Resource
    private MessageContentService messageContentService;

    @PostMapping(value = "/message/content/add")
    public Response<?> addMessageContent(@Validated @RequestBody AddMessageContentReqDTO addMessageContentReqDTO) {
        return messageContentService.addMessageContent(addMessageContentReqDTO);
    }
    @PostMapping(value = "/message/content/find")
    public Response<FindMessageContentRspDTO> findMessageContent(@Validated @RequestBody FindMessageContentReqDTO findMessageContentReqDTO) {
        return messageContentService.findMessageContent(findMessageContentReqDTO);
    }
    @PostMapping(value = "/message/content/delete")
    public Response<?> deleteMessageContent(@Validated @RequestBody DeleteMessageContentReqDTO deleteMessageContentReqDTO) {
        return messageContentService.deleteMessageContent(deleteMessageContentReqDTO);
    }
}
