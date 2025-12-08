package com.theoyu.thesis.media.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinRoomReqVO {

    /**
     * 房间ID
     */
    @NotNull(message = "房间ID不能为空")
    private Long roomId;
}