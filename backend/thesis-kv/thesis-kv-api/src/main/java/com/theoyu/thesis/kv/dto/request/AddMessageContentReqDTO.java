package com.theoyu.thesis.kv.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddMessageContentReqDTO {
    @NotNull(message = "消息内容ID不能为空")
    private String uuid;

    @NotBlank(message = "消息内容不能为空")
    private String content;
}
