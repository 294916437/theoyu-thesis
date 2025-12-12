package com.theoyu.thesis.kv.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindMessageContentRspDTO {
    /**
     * 笔记内容 ID
     */
    private UUID uuid;

    /**
     * 笔记内容
     */
    private String content;
}
