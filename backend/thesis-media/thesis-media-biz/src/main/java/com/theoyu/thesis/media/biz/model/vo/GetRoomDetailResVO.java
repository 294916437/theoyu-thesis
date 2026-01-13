package com.theoyu.thesis.media.biz.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    private Integer duration;

    private Integer status;

    private HostInfo host;

    private Integer participantCount;

    private List<ParticipantInfo> participants;

    private RecordingInfo recording;

    private TranscriptInfo transcript;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HostInfo {
        private Long userId;
        private String userName;
        private String email;
        private String avatar;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo {
        private Long userId;
        private String userName;
        private String email;
        private String avatar;
        private Integer role;
        private Integer status;
        private Boolean audioMuted;
        private Boolean videoMuted;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime joinedAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime leftAt;
    }

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