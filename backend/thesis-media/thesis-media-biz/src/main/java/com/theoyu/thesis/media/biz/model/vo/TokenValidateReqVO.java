package com.theoyu.thesis.media.biz.model.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * Token 验证请求 VO
 */
@Data
public class TokenValidateReqVO {

    @NotBlank(message = "访问令牌不能为空")
    private String token;
}
