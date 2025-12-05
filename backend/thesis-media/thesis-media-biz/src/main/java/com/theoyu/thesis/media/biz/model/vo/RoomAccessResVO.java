package com.theoyu.thesis.media.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 房间访问响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class RoomAccessResVO {

    private Boolean allowed;

    private String message;

    private RoomConfigVO config;
}

