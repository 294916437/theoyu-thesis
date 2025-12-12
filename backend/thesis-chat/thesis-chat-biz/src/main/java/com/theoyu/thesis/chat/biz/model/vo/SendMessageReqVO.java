package com.theoyu.thesis.chat.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageReqVO {
    /**
     * 消息类型：1-文本，2-图片，3-语音，4-视频，5-文件
     */
    @NotNull(message = "消息类型不能为空")
    private Integer messageType;
    
    /**
     * 文本内容
     */
    private String content;
    
    /**
     * 图片 URL 列表（消息类型为图片时必填）
     */
    private List<String> imgUris;
    
    /**
     * 视频 URL（消息类型为视频时必填）
     */
    private String videoUri;
}