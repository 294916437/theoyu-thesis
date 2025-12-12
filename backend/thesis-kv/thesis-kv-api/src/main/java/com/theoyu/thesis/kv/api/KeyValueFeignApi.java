package com.theoyu.thesis.kv.api;

import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.kv.constants.ApiConstants;
import com.theoyu.thesis.kv.dto.request.*;
import com.theoyu.thesis.kv.dto.response.*;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface KeyValueFeignApi {
    String PREFIX = "/kv";

    // ==================== Example相关 ====================
    
    @PostMapping(value = PREFIX + "/example/content/add")
    Response<?> addExampleContent(@RequestBody AddExampleContentReqDTO addExampleContentReqDTO);
    
    @PostMapping(value = PREFIX + "/example/content/find")
    Response<FindExampleContentRspDTO> findExampleContent(@RequestBody FindExampleContentReqDTO findExampleContentReqDTO);
    
    @PostMapping(value = PREFIX + "/example/content/delete")
    Response<?> deleteExampleContent(@RequestBody DeleteExampleContentReqDTO deleteExampleContentReqDTO);

    // ==================== Chat服务相关 ====================

    @PostMapping(PREFIX + "/message/content/add")
    Response<?> addMessageContent(@RequestBody AddMessageContentReqDTO reqDTO);

    @PostMapping(PREFIX + "/message/content/find")
    Response<FindMessageContentRspDTO> findMessageContent(@Valid @RequestBody FindMessageContentReqDTO reqDTO);

    @PostMapping(PREFIX + "/message/content/delete")
    Response<?> deleteMessageContent(@Valid @RequestBody DeleteMessageContentReqDTO reqDTO);


}