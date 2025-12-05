package com.theoyu.thesis.media.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 通用确认响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AckResVO {

    private Boolean success;

    private String message;
}
