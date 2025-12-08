package com.theoyu.thesis.media.biz.model.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;
/**
 * 媒体统计上报请求 VO
 */
@Data

public class MediaStatsReqVO {

    @NotBlank(message = "房间ID不能为空")
    private String roomId;

    @NotBlank(message = "Peer ID不能为空")
    private String peerId;

    private Map<String, String> stats;
}
