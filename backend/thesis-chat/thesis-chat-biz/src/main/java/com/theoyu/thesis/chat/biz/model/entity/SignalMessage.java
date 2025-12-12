package com.theoyu.thesis.chat.biz.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 信令消息基类
 */
@Data
public class SignalMessage implements Serializable {

    private String type;
    private String callId;
    private Long fromUserId;
    private Long toUserId;
    private Long timestamp;

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CallOffer extends SignalMessage {
        private Object offer;  // SDP Offer
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CallAnswer extends SignalMessage {
        private Object answer;  // SDP Answer
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class IceCandidate extends SignalMessage {
        private Object candidate;  // ICE 候选
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CallEnd extends SignalMessage {
        private String reason;
        private Long duration;
    }
}