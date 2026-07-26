package com.theoyu.thesis.android.core.sfu

import android.content.Context
import com.theoyu.thesis.android.core.signaling.SocketIoClient
import com.theoyu.thesis.android.feature.main.RoomMediaState
import com.theoyu.thesis.android.feature.main.SfuConsumerState
import com.theoyu.thesis.android.feature.main.SfuMediaPhase
import com.theoyu.thesis.android.feature.main.SfuProducerState
import com.theoyu.thesis.android.feature.main.SfuTransportState

class RoomMediaEngine(
    context: Context,
    private val socketIoClient: SocketIoClient,
    private val onMediaStateChanged: (RoomMediaState) -> Unit,
    private val onMessageChanged: (String?) -> Unit,
    private val onLocalPreviewChanged: (Any?) -> Unit,
    private val onRemoteVideoTrackChanged: (peerId: String, track: Any?) -> Unit,
) {
    @Suppress("unused")
    private val appContext = context.applicationContext
    private var lastState: RoomMediaState = RoomMediaState()

    fun setSessionContext(roomId: String, userId: String, displayName: String) = Unit

    fun loadRouterCapabilities(routerCapabilitiesJson: String) {
        updateState(
            lastState.copy(
                phase = SfuMediaPhase.RouterReady,
                routerRtpCapabilitiesJson = routerCapabilitiesJson,
                error = null,
            ),
        )
    }

    fun attachTransports(send: SfuTransportState, recv: SfuTransportState) {
        updateState(
            lastState.copy(
                phase = SfuMediaPhase.TransportsReady,
                sendTransport = send,
                recvTransport = recv,
                error = null,
            ),
        )
    }

    fun startLocalPublish(audioEnabled: Boolean, videoEnabled: Boolean) {
        updateState(
            lastState.copy(
                phase = SfuMediaPhase.AwaitingMediaEngine,
                mediaEngineReady = false,
            ),
        )
        onMessageChanged("React Native 侧媒体引擎将使用 mediasoup-client 初始化音视频")
        onLocalPreviewChanged(null)
    }

    fun consumeExistingRemoteProducers(producers: List<SfuProducerState>) {
        updateState(lastState.copy(remoteProducers = lastState.remoteProducers.upsertProducers(producers)))
    }

    fun registerRemoteProducer(producer: SfuProducerState) {
        updateState(lastState.copy(remoteProducers = lastState.remoteProducers.upsertProducer(producer)))
    }

    fun consumeRemoteProducer(producer: SfuProducerState) {
        updateState(
            lastState.copy(
                remoteProducers = lastState.remoteProducers.upsertProducer(producer),
                consumers = lastState.consumers.upsertConsumer(
                    SfuConsumerState(
                        id = producer.id,
                        producerId = producer.id,
                        kind = producer.kind,
                        peerId = producer.peerId,
                        resumed = true,
                        producerPaused = producer.paused,
                    ),
                ),
            ),
        )
        onRemoteVideoTrackChanged(producer.peerId, null)
    }

    fun toggleAudio(enabled: Boolean) = Unit

    fun toggleVideo(enabled: Boolean) = Unit

    fun switchCamera() = Unit

    fun closeSession() {
        updateState(RoomMediaState())
        onLocalPreviewChanged(null)
    }

    fun release() {
        closeSession()
    }

    private fun updateState(state: RoomMediaState) {
        lastState = state
        onMediaStateChanged(state)
    }

    private fun List<SfuProducerState>.upsertProducers(producers: List<SfuProducerState>): List<SfuProducerState> =
        producers.fold(this) { current, producer -> current.upsertProducer(producer) }

    private fun List<SfuProducerState>.upsertProducer(producer: SfuProducerState): List<SfuProducerState> =
        if (producer.id.isBlank()) {
            this
        } else if (any { it.id == producer.id }) {
            map { current -> if (current.id == producer.id) producer else current }
        } else {
            this + producer
        }

    private fun List<SfuConsumerState>.upsertConsumer(consumer: SfuConsumerState): List<SfuConsumerState> =
        if (consumer.id.isBlank()) {
            this
        } else if (any { it.id == consumer.id }) {
            map { current -> if (current.id == consumer.id) consumer else current }
        } else {
            this + consumer
        }
}
