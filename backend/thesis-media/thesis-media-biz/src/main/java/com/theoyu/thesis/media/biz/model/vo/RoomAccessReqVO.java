package com.theoyu.thesis.media.biz.model.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 房间访问请求 VO
 */
@Data
public class RoomAccessReqVO {

    @NotBlank(message = "房间ID不能为空")
    private String roomId;

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "访问令牌不能为空")
    private String token;
}

