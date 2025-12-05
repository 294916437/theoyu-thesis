package com.theoyu.thesis.media.biz.model.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * 参与者事件请求 VO
 */
@Data
public class ParticipantEventReqVO {

    @NotBlank(message = "房间ID不能为空")
    private String roomId;

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private Long timestamp;
}
