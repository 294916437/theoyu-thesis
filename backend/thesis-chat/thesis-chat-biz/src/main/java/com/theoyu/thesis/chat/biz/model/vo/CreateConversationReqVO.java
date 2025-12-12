package com.theoyu.thesis.chat.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateConversationReqVO {
    @NotNull(message = "对方用户ID不能为空")
    private Long targetUserId;
}