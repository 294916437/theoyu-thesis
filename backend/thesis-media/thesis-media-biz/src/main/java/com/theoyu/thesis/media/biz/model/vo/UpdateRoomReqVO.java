package com.theoyu.thesis.media.biz.model.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.Instant;

@Data
public class UpdateRoomReqVO {

    /**
     * 房间标题
     */
    @NotBlank(message = "房间标题不能为空")
    private String title;

    /**
     * 房间描述/设置（JSON格式），对应数据库 settings 字段
     * 例如: {"enableRecording": false, "enableWaitingRoom": false, "disableCamera": false, "allowedCodecs": ["opus", "VP8"]}
     */
    private String description;

    /**
     * 会议开始时间（前端传入 ISO 8601 UTC 格式，如 2026-04-18T01:00:00.000Z）
     */
    private Instant startTime;

    /**
     * 预计持续时间（分钟），用于计算 endTime
     */
    private Integer duration;

    /**
     * 最大参与者数量
     */
    @Min(value = 1, message = "最大参与者数量必须大于0")
    @Max(value = 100, message = "最大参与者数量不能超过100")
    private Integer maxParticipants;
}
