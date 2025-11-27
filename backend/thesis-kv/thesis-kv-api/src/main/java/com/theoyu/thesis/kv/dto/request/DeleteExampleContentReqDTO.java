package com.theoyu.thesis.kv.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeleteExampleContentReqDTO {
    @NotBlank(message = "笔记内容 ID 不能为空")
    private String uuid;
}
