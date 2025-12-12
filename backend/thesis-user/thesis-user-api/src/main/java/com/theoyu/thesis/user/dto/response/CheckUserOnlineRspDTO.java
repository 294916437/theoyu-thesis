package com.theoyu.thesis.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckUserOnlineRspDTO {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 是否在线
     */
    private Boolean online;

}