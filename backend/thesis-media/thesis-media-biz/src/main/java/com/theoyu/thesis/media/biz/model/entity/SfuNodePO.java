package com.theoyu.thesis.media.biz.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SfuNodePO {
    private Long id;

    private String instanceId;

    private String ipAddress;

    private Integer grpcPort;

    private Integer httpPort;

    private String region;

    private Integer status;

    private Integer currentLoad;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private String grpcHost;


}