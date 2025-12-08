package com.theoyu.thesis.media.biz.model.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoomReqVO {
    /**
     * 房间标题
     */
    @NotBlank(message = "房间标题不能为空")
    private String title;

    /**
     * 房间类型: 1-即使会议, 2-预约会议
     */
    private Integer type = 1;

    /**
     *
     */
    private Long sfuNodeId;

    /**
     * 最大参与者数量
     */
    private Integer maxParticipants = 15;

    /**
     * 房间设置（JSON格式）
     * 例如: {"enableRecording": true, "allowedCodecs": ["opus", "VP8"]}
     */
    private String settings;
}
