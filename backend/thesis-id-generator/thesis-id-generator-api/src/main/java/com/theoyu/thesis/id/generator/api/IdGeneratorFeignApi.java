package com.theoyu.thesis.id.generator.api;


import com.theoyu.thesis.id.generator.constants.ApiConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface IdGeneratorFeignApi {


    String PREFIX = "/id";

    @GetMapping(value = PREFIX + "/segment/get/{key}")
    String getSegmentId(@PathVariable("key") String key);

    @GetMapping(value = PREFIX + "/snowflake/get/{key}")
    String getSnowflakeId(@PathVariable("key") String key);

    @GetMapping("/id/snowflake/batch/{key}")
    String getSnowflakeIds(@PathVariable("key") String key,
                           @RequestParam("count") Integer count);
}
