package com.theoyu.thesis.media.biz.grpc;

import com.theoyu.thesis.media.biz.grpc.proto.control.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * SFU Node.js gRPC 客户端
 * 用于调用 Node.js 提供的控制服务 (如录制)
 *
 * @author theoyu
 */
@Slf4j
@Service
public class SFUGrpcClient {

    /**
     * 注入 gRPC 客户端存根
     * 名称需要与配置文件中的 client 名称一致: sfu-node-service
     */
    @GrpcClient("sfu-node-service")
    private SFUControlServiceGrpc.SFUControlServiceBlockingStub blockingStub;

    // ==================== 录制相关接口 ====================

    /**
     * 启动录制
     *
     * @param roomId 房间ID
     * @param hostId 主持人ID
     * @param config 录制配置
     * @return 录制响应
     */
    public StartRecordingResponse startRecording(String roomId, String hostId, RecordingConfig config) {
        log.info("[gRPC-Client] 调用 Node.js 启动录制 - roomId: {}, hostId: {}", roomId, hostId);

        try {
            StartRecordingRequest request = StartRecordingRequest.newBuilder()
                    .setRoomId(roomId)
                    .setHostId(hostId)
                    .setConfig(config)
                    .build();

            StartRecordingResponse response = blockingStub.startRecording(request);

            log.info("[gRPC-Client] 启动录制成功 - success: {}, hostId: {}",
                    response.getSuccess(), response.getHostId());
            return response;

        } catch (StatusRuntimeException e) {
            log.error("[gRPC-Client] 启动录制失败 - status: {}, message: {}",
                    e.getStatus(), e.getMessage());
            throw new RuntimeException("启动录制失败: " + e.getMessage(), e);
        }
    }

    /**
     * 停止录制
     *
     * @param roomId 房间ID
     * @param hostId 录制ID
     * @return 停止录制响应
     */
    public StopRecordingResponse stopRecording(String roomId, String hostId) {
        log.info("[gRPC-Client] 调用 Node.js 停止录制 - roomId: {}, hostId: {}", roomId, hostId);

        try {
            StopRecordingRequest request = StopRecordingRequest.newBuilder()
                    .setRoomId(roomId)
                    .setHostId(hostId)
                    .build();

            StopRecordingResponse response = blockingStub.stopRecording(request);

            log.info("[gRPC-Client] 停止录制成功 - success: {}, fileUrl: {}",
                    response.getSuccess(), response.getFileUrl());
            return response;

        } catch (StatusRuntimeException e) {
            log.error("[gRPC-Client] 停止录制失败 - status: {}, message: {}",
                    e.getStatus(), e.getMessage());
            throw new RuntimeException("停止录制失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取录制状态
     *
     * @param roomId 房间ID
     * @param hostId 录制ID
     * @return 录制状态响应
     */
    public RecordingStatusResponse getRecordingStatus(String roomId, String hostId) {
        log.info("[gRPC-Client] 查询录制状态 - roomId: {}, hostId: {}", roomId, hostId);

        try {
            RecordingStatusRequest request = RecordingStatusRequest.newBuilder()
                    .setRoomId(roomId)
                    .setHostId(hostId)
                    .build();

            RecordingStatusResponse response = blockingStub.getRecordingStatus(request);

            log.info("[gRPC-Client] 录制状态查询成功 - isRecording: {}, duration: {}s, fileSize: {}bytes",
                    response.getIsRecording(), response.getDurationSeconds(), response.getFileSizeBytes());
            return response;

        } catch (StatusRuntimeException e) {
            log.error("[gRPC-Client] 查询录制状态失败 - status: {}, message: {}",
                    e.getStatus(), e.getMessage());
            throw new RuntimeException("查询录制状态失败: " + e.getMessage(), e);
        }
    }
}