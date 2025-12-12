package com.theoyu.thesis.chat.biz.rpc;

import com.theoyu.thesis.id.generator.api.IdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


@Component
public class IdGeneratorRpcService {
    @Resource
    private IdGeneratorFeignApi idGeneratorFeignApi;
    /**
     * 生成雪花算法 ID
     *
     * @return
     */
    public String getConversationSnowflakeId() {
        return idGeneratorFeignApi.getSnowflakeId("conversation");
    }
    public String getMessageSnowflakeId() {
        return idGeneratorFeignApi.getSnowflakeId("message");
    }

}
