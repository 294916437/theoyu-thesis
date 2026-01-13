package com.theoyu.thesis.media.biz.model.vo;

import com.theoyu.thesis.user.dto.response.FindUserByIdRspDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetRoomDetailResVO {

    private Long roomId;

    private String roomNo;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer duration;

    private Integer status;

    private FindUserByIdRspDTO host;

    private Integer participantCount;

    private List<ParticipantListItemVO> participants;

    private RecordingInfo recording;

    private TranscriptInfo transcript;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordingInfo {
        private Boolean available;
        private String url;
        private Long size;
        private Integer duration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptInfo {
        private Boolean available;
        private String url;
    }
}