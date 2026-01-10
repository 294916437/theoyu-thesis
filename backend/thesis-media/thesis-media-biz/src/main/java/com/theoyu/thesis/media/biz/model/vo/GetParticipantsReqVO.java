package com.theoyu.thesis.media.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GetParticipantsReqVO {
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    private Integer status; // 状态过滤: null-全部, 1-在线, 2-离线

    private long page = 1;

    private long size = 20;
}