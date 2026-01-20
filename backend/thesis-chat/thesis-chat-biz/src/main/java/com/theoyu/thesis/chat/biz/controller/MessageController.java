package com.theoyu.thesis.chat.biz.controller;

import com.theoyu.framework.common.response.Response;
import com.theoyu.framework.logger.aspect.ApiOperationLog;
import com.theoyu.thesis.chat.biz.model.vo.GetMessagesReqVO;
import com.theoyu.thesis.chat.biz.model.vo.GetMessagesResVO;
import com.theoyu.thesis.chat.biz.model.vo.SendMessageReqVO;
import com.theoyu.thesis.chat.biz.service.MessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/message")
@Slf4j
public class MessageController {
    @Resource
    private MessageService messageService;
    
    /**
     * 发送消息
     */
    @PostMapping("/{id}/send")
    @ApiOperationLog(description = "发送消息")
    public Response<?> sendMessage(
            @PathVariable("id") Long id,
            @Validated @RequestBody SendMessageReqVO reqVO) {
        return messageService.sendMessage(id, reqVO);
    }
    /**
     * 获取消息列表（游标分页）
     */
    @PostMapping("/{id}/list")
    @ApiOperationLog(description = "获取消息列表")
    public Response<GetMessagesResVO> getMessages(
            @PathVariable("id") Long id,
            @Validated @RequestBody GetMessagesReqVO reqVO) {
        return messageService.getMessages(id, reqVO);
    }


}
