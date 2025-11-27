package com.theoyu.thesis.media.biz.controller;

import com.theoyu.thesis.media.biz.service.MediaService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/media")
@Slf4j
public class MediaController {
    @Resource
    private MediaService mediaService;



}
