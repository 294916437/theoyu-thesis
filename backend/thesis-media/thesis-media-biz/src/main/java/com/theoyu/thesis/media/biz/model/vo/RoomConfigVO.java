package com.theoyu.thesis.media.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
/**
 * 房间配置 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomConfigVO {

    private Integer maxParticipants;

    private Boolean enableRecording;

    private List<String> allowedCodecs;
}
