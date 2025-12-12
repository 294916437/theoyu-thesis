package com.theoyu.thesis.chat.biz.model.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetMessagesReqVO {
    /**
     * 游标（消息ID）
     * 为空时表示获取最新消息
     * 不为空时表示获取该消息之前的消息
     */
    private Long cursor;

    /**
     * 每页数量，默认10，最大50
     */
    @Min(value = 1, message = "每页数量最小为1")
    @Max(value = 50, message = "每页数量最大为50")
    private Integer limit = 10;
}