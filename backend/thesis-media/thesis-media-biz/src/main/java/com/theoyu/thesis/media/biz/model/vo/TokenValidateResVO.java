package com.theoyu.thesis.media.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * Token 验证响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidateResVO {

    private Boolean valid;

    private String userId;

    private String username;
}
