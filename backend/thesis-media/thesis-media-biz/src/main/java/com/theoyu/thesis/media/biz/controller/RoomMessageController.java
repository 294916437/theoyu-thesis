package com.theoyu.thesis.media.biz.controller;

import com.theoyu.thesis.media.biz.service.RoomMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/room/message")
@Slf4j
public class RoomMessageController {
    @Resource
    private RoomMessageService roomMessageService;
}
