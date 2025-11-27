package com.theoyu.thesis.kv.biz.controller;

import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.kv.biz.service.ExampleContentService;
import com.theoyu.thesis.kv.dto.request.AddExampleContentReqDTO;
import com.theoyu.thesis.kv.dto.request.DeleteExampleContentReqDTO;
import com.theoyu.thesis.kv.dto.request.FindExampleContentReqDTO;
import com.theoyu.thesis.kv.dto.response.FindExampleContentRspDTO;
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
public class NoteContentController {

    @Resource
    private ExampleContentService exampleContentService;

    @PostMapping(value = "/example/content/add")
    public Response<?> addNoteContent(@Validated @RequestBody AddExampleContentReqDTO addExampleContentReqDTO) {
        return exampleContentService.addExampleContent(addExampleContentReqDTO);
    }
    @PostMapping(value = "/example/content/find")
    public Response<FindExampleContentRspDTO> findNoteContent(@Validated @RequestBody FindExampleContentReqDTO findExampleContentReqDTO) {
        return exampleContentService.findExampleContent(findExampleContentReqDTO);
    }
    @PostMapping(value = "/example/content/delete")
    public Response<?> deleteNoteContent(@Validated @RequestBody DeleteExampleContentReqDTO deleteExampleContentReqDTO) {
        return exampleContentService.deleteExampleContent(deleteExampleContentReqDTO);
    }
}
