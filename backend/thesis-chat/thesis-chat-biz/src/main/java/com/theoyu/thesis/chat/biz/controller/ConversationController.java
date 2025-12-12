package com.theoyu.thesis.chat.biz.controller;

import com.theoyu.framework.common.response.Response;
import com.theoyu.framework.logger.aspect.ApiOperationLog;
import com.theoyu.thesis.chat.biz.model.vo.*;
import com.theoyu.thesis.chat.biz.service.ConversationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/conversation")
@Slf4j
public class ConversationController {
    
    @Resource
    private ConversationService conversationService;
    
    /**
     * 获取会话列表
     * POST方法，接收userId和cursor
     */
    @PostMapping("/list")
    @ApiOperationLog(description = "获取会话列表")
    public Response<ConversationListResVO> getConversationList(
            @Validated @RequestBody ConversationListReqVO reqVO) {
        return conversationService.getConversationList(reqVO);
    }
    
    /**
     * 获取会话详情
     * GET方法，使用路径参数
     */
    @GetMapping("/{id}")
    @ApiOperationLog(description = "获取会话详情")
    public Response<ConversationDetailResVO> getConversationDetail(
            @PathVariable("id") Long id) {
        return conversationService.getConversationDetail(id);
    }
    /**
     * 创建私聊会话
     */
    @PostMapping("/create")
    @ApiOperationLog(description = "创建私聊会话")
    public Response<CreateConversationResVO> createConversation(
            @Validated @RequestBody CreateConversationReqVO reqVO) {
        return conversationService.createConversation(reqVO);
    }
    /**
     * 退出/删除会话
     */
    @PutMapping("/{id}/leave")
    @ApiOperationLog(description = "退出/删除会话")
    public Response<?> leaveConversation(
            @PathVariable("id") Long id) {
        return conversationService.leaveConversation(id);
    }
}