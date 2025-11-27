package com.theoyu.thesis.id.generator.biz.controller;

import com.theoyu.thesis.id.generator.biz.core.common.Result;
import com.theoyu.thesis.id.generator.biz.core.common.Status;
import com.theoyu.thesis.id.generator.biz.exception.LeafServerException;
import com.theoyu.thesis.id.generator.biz.exception.NoKeyException;
import com.theoyu.thesis.id.generator.biz.service.SegmentService;
import com.theoyu.thesis.id.generator.biz.service.SnowflakeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/id")
@Slf4j
public class LeafController {

    // 批量生成上限
    private static final int MAX_BATCH_SIZE = 100;

    @Resource
    private SegmentService segmentService;
    @Resource
    private SnowflakeService snowflakeService;

    @RequestMapping(value = "/segment/get/{key}")
    public String getSegmentId(@PathVariable("key") String key) {
        return get(key, segmentService.getId(key));
    }

    @RequestMapping(value = "/snowflake/get/{key}")
    public String getSnowflakeId(@PathVariable("key") String key) {
        return get(key, snowflakeService.getId(key));
    }

    /**
     * 批量生成雪花算法 ID
     *
     * @param key 业务标识
     * @param count 生成数量
     * @return ID列表，使用逗号分隔
     */
    @GetMapping(value = "/snowflake/batch/{key}")
    public List<String> getSnowflakeIds(@PathVariable("key") String key,
                                  @RequestParam(value = "count", defaultValue = "1") Integer count) {
        // 参数校验
        if (key == null || key.isEmpty()) {
            throw new NoKeyException();
        }

        if (count == null || count <= 0) {
            throw new IllegalArgumentException("生成数量必须大于0");
        }

        if (count > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("单次生成数量不能超过" + MAX_BATCH_SIZE);
        }

        // 返回逗号分隔的ID字符串，保持与原有接口风格一致
        return snowflakeService.getBatchIds(key, count);
    }

    private String get(@PathVariable("key") String key, Result id) {
        Result result;
        if (key == null || key.isEmpty()) {
            throw new NoKeyException();
        }
        result = id;
        if (result.getStatus().equals(Status.EXCEPTION)) {
            throw new LeafServerException(result.toString());
        }
        return String.valueOf(result.getId());
    }

}
