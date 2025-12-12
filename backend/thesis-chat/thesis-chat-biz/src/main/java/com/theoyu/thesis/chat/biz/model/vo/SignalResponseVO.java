package com.theoyu.thesis.chat.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 信令响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalResponseVO {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private Object data;

    public static SignalResponseVO success() {
        return SignalResponseVO.builder()
                .success(true)
                .message("操作成功")
                .build();
    }

    public static SignalResponseVO success(Object data) {
        return SignalResponseVO.builder()
                .success(true)
                .message("操作成功")
                .data(data)
                .build();
    }

    public static SignalResponseVO error(String message) {
        return SignalResponseVO.builder()
                .success(false)
                .message(message)
                .build();
    }
}