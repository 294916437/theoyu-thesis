package com.theoyu.thesis.android.core.sfu

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import com.theoyu.thesis.android.core.signaling.SocketIoClient
import com.theoyu.thesis.android.feature.main.RoomMediaState
import com.theoyu.thesis.android.feature.main.SfuConsumerState
import com.theoyu.thesis.android.feature.main.SfuMediaPhase
import com.theoyu.thesis.android.feature.main.SfuProducerState
import com.theoyu.thesis.android.feature.main.SfuTransportState
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.mediasoup.droid.Consumer
import org.mediasoup.droid.Device
import org.mediasoup.droid.Producer
import org.mediasoup.droid.RecvTransport
import org.mediasoup.droid.SendTransport
import org.mediasoup.droid.Transport
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpParameters
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

class RoomMediaEngine(
    context: Context,
    private val socketIoClient: SocketIoClient,
    private val onMediaStateChanged: (RoomMediaState) -> Unit,
    private val onMessageChanged: (String?) -> Unit,
    private val onLocalPreviewChanged: (VideoTrack?) -> Unit,
    private val onRemoteVideoTrackChanged: (peerId: String, track: VideoTrack?) -> Unit,
    private val onScreenShareChanged: (Boolean) -> Unit,
) {
    private val appContext = context.applicationContext

    init {
        WebRtcEnvironment.initialize(appContext)
    }

    private val device = Device()
    private val eglBase: EglBase = WebRtcEnvironment.eglBase
    private val peerConnectionFactory: PeerConnectionFactory
    private val audioDeviceModule: JavaAudioDeviceModule
    private val surfaceTextureHelper: SurfaceTextureHelper

    private var roomId: String = ""
    private var userId: String = ""
    private var displayName: String = ""
    private var sendTransport: SendTransport? = null
    private var recvTransport: RecvTransport? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var cameraCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var localAudioProducer: Producer? = null
    private var localVideoProducer: Producer? = null
    private var screenVideoProducer: Producer? = null
    private var screenCapturer: ScreenCapturerAndroid? = null
    private var screenVideoSource: VideoSource? = null
    private var screenVideoTrack: VideoTrack? = null
    private var stoppingScreenShare = false
    private val remoteConsumers = mutableMapOf<String, Consumer>()
    private var lastState: RoomMediaState = RoomMediaState()

    init {
        audioDeviceModule = JavaAudioDeviceModule.builder(appContext)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
        surfaceTextureHelper = SurfaceTextureHelper.create("sfu-capture", eglBase.eglBaseContext)
    }

    fun setSessionContext(roomId: String, userId: String, displayName: String) {
        this.roomId = roomId
        this.userId = userId
        this.displayName = displayName
    }

    fun loadRouterCapabilities(routerCapabilitiesJson: String) {
        try {
            device.load(routerCapabilitiesJson, null)
            updateState(
                lastState.copy(
                    phase = SfuMediaPhase.RouterReady,
                    routerRtpCapabilitiesJson = routerCapabilitiesJson,
                    error = null,
                ),
            )
        } catch (error: Exception) {
            updateState(lastState.copy(phase = SfuMediaPhase.Failed, error = error.message))
            throw error
        }
    }

    fun attachTransports(send: SfuTransportState, recv: SfuTransportState) {
        sendTransport?.close()
        recvTransport?.close()

        val sendListener = object : SendTransport.Listener {
            override fun onConnect(transport: Transport, dtlsParameters: String) {
                connectTransport(transport.id, dtlsParameters)
            }

            override fun onConnectionStateChange(transport: Transport, connectionState: String) {
                updateTransportConnectionState(transport.id, connectionState)
            }

            override fun onProduce(transport: Transport, kind: String, rtpParameters: String, appData: String): String {
                return produceOnServer(kind, rtpParameters, appData, transport.id)
            }

            override fun onProduceData(transport: Transport, sctpStreamParameters: String, label: String, protocol: String, appData: String): String {
                return ""
            }
        }

        val recvListener = object : RecvTransport.Listener {
            override fun onConnect(transport: Transport, dtlsParameters: String) {
                connectTransport(transport.id, dtlsParameters)
            }

            override fun onConnectionStateChange(transport: Transport, connectionState: String) {
                updateTransportConnectionState(transport.id, connectionState)
            }
        }

        sendTransport = device.createSendTransport(
            sendListener,
            send.id,
            send.iceParametersJson,
            send.iceCandidatesJson,
            send.dtlsParametersJson,
            send.sctpParametersJson.ifBlank { null },
            null,
            transportAppData("send"),
        )
        recvTransport = device.createRecvTransport(
            recvListener,
            recv.id,
            recv.iceParametersJson,
            recv.iceCandidatesJson,
            recv.dtlsParametersJson,
            recv.sctpParametersJson.ifBlank { null },
            null,
            transportAppData("recv"),
        )
        updateState(
            lastState.copy(
                phase = SfuMediaPhase.TransportsReady,
                sendTransport = send.copy(connected = false),
                recvTransport = recv.copy(connected = false),
                error = null,
            ),
        )
    }

    fun startLocalPublish(audioEnabled: Boolean, videoEnabled: Boolean) {
        ensureLocalAudioTrack()
        ensureLocalVideoTrack()

        if (audioEnabled && device.canProduce("audio")) {
            localAudioProducer = produceTrack(localAudioTrack, "audio", "opus", producerAppData("audio"))
        }
        if (videoEnabled && device.canProduce("video")) {
            localVideoProducer = produceTrack(localVideoTrack, "video", "VP8", producerAppData("video", source = "camera"))
            onLocalPreviewChanged(localVideoTrack)
        }
        syncLocalProducerState()
        updateState(lastState.copy(phase = SfuMediaPhase.Connected, mediaEngineReady = true))
    }

    fun consumeExistingRemoteProducers(producers: List<SfuProducerState>) {
        producers.forEach { consumeRemoteProducer(it) }
    }

    fun registerRemoteProducer(producer: SfuProducerState) {
        updateState(lastState.copy(remoteProducers = lastState.remoteProducers.upsertProducer(producer)))
    }

    fun closeRemoteProducer(producerId: String) {
        val consumer = remoteConsumers.remove(producerId)
        val consumerState = lastState.consumers.firstOrNull { it.producerId == producerId }
        if (consumer != null) {
            consumer.close()
            if (consumer.kind == "video" && consumerState != null) {
                onRemoteVideoTrackChanged(consumerState.peerId, null)
            }
        }
        updateState(
            lastState.copy(
                remoteProducers = lastState.remoteProducers.filterNot { it.id == producerId },
                consumers = lastState.consumers.filterNot { it.producerId == producerId },
                remoteVideoTracks = consumerState?.peerId?.let { peerId -> lastState.remoteVideoTracks - peerId }
                    ?: lastState.remoteVideoTracks,
            ),
        )
    }

    fun consumeRemoteProducer(producer: SfuProducerState) {
        if (producer.id.isBlank() || remoteConsumers.containsKey(producer.id)) return
        val recv = recvTransport ?: run {
            updateState(lastState.copy(phase = SfuMediaPhase.AwaitingMediaEngine))
            return
        }
        val rtpCapabilities = runCatching { device.rtpCapabilities }.getOrNull()?.toString().orEmpty()
        if (rtpCapabilities.isBlank()) return

        runCatching {
            updateState(lastState.copy(phase = SfuMediaPhase.Consuming))
            val response = runBlocking {
                socketIoClient.emit(
                    "consume",
                    JSONObject()
                        .put("roomId", roomId)
                        .put("producerId", producer.id)
                        .put("rtpCapabilities", JSONObject(rtpCapabilities)),
                )
            }
            val body = response.asJsonObject() ?: throw IllegalStateException("Invalid consume response")
            val consumerId = body.optString("id")
            val consumerProducerId = body.optString("producerId", producer.id)
            val consumerKind = body.optString("kind", producer.kind)
            val consumerRtpParameters = body.optString("rtpParameters")
            val consumer = recv.consume(
                object : Consumer.Listener {
                    override fun onTransportClose(consumer: Consumer) {
                        remoteConsumers.remove(consumer.producerId)
                        if (consumer.kind == "video") {
                            onRemoteVideoTrackChanged(producer.peerId, null)
                        }
                        updateState(lastState.copy(consumers = lastState.consumers.filterNot { it.id == consumer.id }))
                    }
                },
                consumerId,
                consumerProducerId,
                consumerKind,
                consumerRtpParameters,
                consumerAppData(producer),
            )
            remoteConsumers[producer.id] = consumer
            consumer.resume()
            resumeConsumerOnServer(consumer.id)
            val track = if (consumer.kind == "video") consumer.track as? VideoTrack else null
            if (track != null) {
                onRemoteVideoTrackChanged(producer.peerId, track)
            }
            updateState(
                lastState.copy(
                    consumers = lastState.consumers.upsertConsumer(
                        SfuConsumerState(
                            id = consumer.id,
                            producerId = consumer.producerId,
                            kind = consumer.kind,
                            peerId = producer.peerId,
                            resumed = true,
                            producerPaused = producer.paused,
                        ),
                    ),
                    remoteVideoTracks = lastState.remoteVideoTracks + (producer.peerId to track),
                    phase = SfuMediaPhase.Connected,
                ),
            )
        }.onFailure { error ->
            updateState(lastState.copy(phase = SfuMediaPhase.Failed, error = error.message))
            onMessageChanged(error.message)
        }
    }

    fun toggleAudio(enabled: Boolean) {
        if (enabled) localAudioProducer?.resume() else localAudioProducer?.pause()
        syncLocalProducerState()
    }

    fun toggleVideo(enabled: Boolean) {
        if (enabled) {
            localVideoProducer?.resume()
            localVideoTrack?.setEnabled(true)
        } else {
            localVideoProducer?.pause()
            localVideoTrack?.setEnabled(false)
        }
        syncLocalProducerState()
    }

    fun startScreenShare(resultCode: Int, data: Intent) {
        if (!device.canProduce("video") || screenVideoProducer != null) return

        runCatching {
            localVideoProducer?.let { producer ->
                closeProducerOnServer(producer.id)
                producer.close()
                localVideoProducer = null
            }

            val capturer = ScreenCapturerAndroid(
                data,
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        stopScreenShare()
                    }
                },
            )
            val source = peerConnectionFactory.createVideoSource(true)
            capturer.initialize(surfaceTextureHelper, appContext, source.capturerObserver)
            capturer.startCapture(1280, 720, 30)

            val track = peerConnectionFactory.createVideoTrack("screen-$userId", source)
            val producer = produceTrack(track, "video", "VP8", producerAppData("video", source = "screen"))

            screenCapturer = capturer
            screenVideoSource = source
            screenVideoTrack = track
            screenVideoProducer = producer
            onLocalPreviewChanged(track)
            onScreenShareChanged(true)
            syncLocalProducerState()
        }.onFailure { error ->
            stopScreenShare()
            updateState(lastState.copy(phase = SfuMediaPhase.Failed, error = error.message))
            onMessageChanged(error.message)
        }
    }

    fun stopScreenShare() {
        if (screenVideoProducer == null && screenCapturer == null) return

        runCatching {
            if (stoppingScreenShare) return@runCatching
            stoppingScreenShare = true
            screenVideoProducer?.let { producer ->
                closeProducerOnServer(producer.id)
                producer.close()
            }
            screenCapturer?.stopCaptureSafely()
            screenVideoTrack?.dispose()
            screenVideoSource?.dispose()

            screenVideoProducer = null
            screenCapturer = null
            screenVideoTrack = null
            screenVideoSource = null

            ensureLocalVideoTrack()
            if (localVideoProducer == null && localVideoTrack != null && device.canProduce("video")) {
                localVideoProducer = produceTrack(localVideoTrack, "video", "VP8", producerAppData("video", source = "camera"))
            }
            onLocalPreviewChanged(localVideoTrack)
            onScreenShareChanged(false)
            syncLocalProducerState()
        }.onFailure { error ->
            updateState(lastState.copy(phase = SfuMediaPhase.Failed, error = error.message))
            onMessageChanged(error.message)
        }.also {
            stoppingScreenShare = false
        }
    }

    fun switchCamera() {
        cameraCapturer?.switchCamera(null)
    }

    fun closeSession() {
        remoteConsumers.values.forEach { runCatching { it.close() } }
        remoteConsumers.clear()
        screenVideoProducer?.close()
        screenCapturer?.stopCaptureSafely()
        screenVideoTrack?.dispose()
        screenVideoSource?.dispose()
        screenVideoProducer = null
        screenCapturer = null
        screenVideoTrack = null
        screenVideoSource = null
        localAudioProducer?.close()
        localVideoProducer?.close()
        localAudioProducer = null
        localVideoProducer = null
        sendTransport?.close()
        recvTransport?.close()
        sendTransport = null
        recvTransport = null
        onLocalPreviewChanged(null)
        updateState(RoomMediaState())
    }

    fun release() {
        closeSession()
        cameraCapturer?.stopCaptureSafely()
        cameraCapturer = null
        localAudioTrack?.dispose()
        localVideoTrack?.dispose()
        videoSource?.dispose()
        audioSource?.dispose()
        surfaceTextureHelper.dispose()
        audioDeviceModule.release()
        device.dispose()
    }

    private fun produceTrack(
        track: MediaStreamTrack?,
        kind: String,
        codec: String,
        appData: String,
    ): Producer? {
        val transport = sendTransport ?: return null
        val actualTrack = track ?: return null
        return transport.produce(
            object : Producer.Listener {
                override fun onTransportClose(producer: Producer) {
                    if (kind == "audio") {
                        localAudioProducer = null
                    } else if (producer.id == screenVideoProducer?.id) {
                        screenVideoProducer = null
                    } else {
                        localVideoProducer = null
                    }
                }
            },
            actualTrack,
            emptyList<RtpParameters.Encoding>(),
            "",
            codec,
            appData,
        )
    }

    private fun produceOnServer(kind: String, rtpParameters: String, appData: String, transportId: String): String {
        val response = runBlocking {
            socketIoClient.emit(
                "produce",
                JSONObject()
                    .put("roomId", roomId)
                    .put("transportId", transportId)
                    .put("kind", kind)
                    .put("rtpParameters", JSONObject(rtpParameters))
                    .put("appData", JSONObject(appData.ifBlank { "{}" })),
            )
        }
        return response?.asJsonObject()?.optString("id").orEmpty()
    }

    private fun connectTransport(transportId: String, dtlsParameters: String) {
        runBlocking {
            socketIoClient.emit(
                "connectWebRtcTransport",
                JSONObject()
                    .put("roomId", roomId)
                    .put("transportId", transportId)
                    .put("dtlsParameters", JSONObject(dtlsParameters)),
            )
        }
    }

    private fun updateTransportConnectionState(transportId: String, connectionState: String) {
        val connected = connectionState.equals("connected", ignoreCase = true)
        updateState(
            lastState.copy(
                sendTransport = lastState.sendTransport?.takeIf { it.id == transportId }?.copy(connected = connected)
                    ?: lastState.sendTransport,
                recvTransport = lastState.recvTransport?.takeIf { it.id == transportId }?.copy(connected = connected)
                    ?: lastState.recvTransport,
            ),
        )
    }

    private fun ensureLocalAudioTrack() {
        if (localAudioTrack != null) return
        audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio-$userId", audioSource)
    }

    private fun ensureLocalVideoTrack() {
        if (localVideoTrack != null) return
        val enumerator = Camera2Enumerator(appContext)
        val deviceName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
        val capturer: VideoCapturer = deviceName?.let { enumerator.createCapturer(it, null) } ?: return
        cameraCapturer = capturer as? CameraVideoCapturer
        videoSource = peerConnectionFactory.createVideoSource(false)
        capturer.initialize(surfaceTextureHelper, appContext, videoSource!!.capturerObserver)
        capturer.startCapture(640, 480, 30)
        localVideoTrack = peerConnectionFactory.createVideoTrack("video-$userId", videoSource)
    }

    private fun syncLocalProducerState() {
        val producers = buildList {
            localAudioProducer?.let {
                add(
                    SfuProducerState(
                        id = it.id,
                        peerId = userId,
                        userId = userId,
                        username = displayName,
                        kind = "audio",
                        paused = it.isPaused,
                        local = true,
                    ),
                )
            }
            localVideoProducer?.let {
                add(
                    SfuProducerState(
                        id = it.id,
                        peerId = userId,
                        userId = userId,
                        username = displayName,
                        kind = "video",
                        paused = it.isPaused,
                        local = true,
                        source = "camera",
                    ),
                )
            }
            screenVideoProducer?.let {
                add(
                    SfuProducerState(
                        id = it.id,
                        peerId = userId,
                        userId = userId,
                        username = displayName,
                        kind = "video",
                        paused = it.isPaused,
                        local = true,
                        source = "screen",
                    ),
                )
            }
        }
        updateState(
            lastState.copy(
                localProducers = producers,
                localVideoTrack = screenVideoTrack ?: localVideoTrack,
            ),
        )
    }

    private fun updateState(state: RoomMediaState) {
        lastState = state
        onMediaStateChanged(state)
    }

    private fun transportAppData(direction: String): String =
        JSONObject()
            .put("roomId", roomId)
            .put("userId", userId)
            .put("direction", direction)
            .toString()

    private fun producerAppData(kind: String, source: String = ""): String =
        JSONObject()
            .put("roomId", roomId)
            .put("userId", userId)
            .put("username", displayName)
            .put("kind", kind)
            .put("source", source.ifBlank { kind })
            .toString()

    private fun consumerAppData(producer: SfuProducerState): String =
        JSONObject()
            .put("roomId", roomId)
            .put("peerId", producer.peerId)
            .put("userId", producer.userId)
            .put("username", producer.username)
            .put("kind", producer.kind)
            .toString()

    private fun Any?.asJsonObject(): JSONObject? =
        when (this) {
            is JSONObject -> this
            is Map<*, *> -> JSONObject(this)
            null -> null
            else -> runCatching { JSONObject(toString()) }.getOrNull()
        }

    private fun List<SfuProducerState>.upsertProducer(producer: SfuProducerState): List<SfuProducerState> =
        if (any { it.id == producer.id }) {
            map { current -> if (current.id == producer.id) producer else current }
        } else {
            this + producer
        }

    private fun List<SfuConsumerState>.upsertConsumer(consumer: SfuConsumerState): List<SfuConsumerState> =
        if (any { it.id == consumer.id }) {
            map { current -> if (current.id == consumer.id) consumer else current }
        } else {
            this + consumer
        }

    private fun CameraVideoCapturer.stopCaptureSafely() {
        runCatching { stopCapture() }
    }

    private fun ScreenCapturerAndroid.stopCaptureSafely() {
        runCatching { stopCapture() }
    }

    private fun closeProducerOnServer(producerId: String) {
        if (producerId.isBlank()) return
        runBlocking {
            runCatching {
                socketIoClient.emit(
                    "closeProducer",
                    JSONObject()
                        .put("roomId", roomId)
                        .put("producerId", producerId),
                )
            }
        }
    }

    private fun resumeConsumerOnServer(consumerId: String) {
        if (consumerId.isBlank()) return
        runBlocking {
            runCatching {
                socketIoClient.emit(
                    "resumeConsumer",
                    JSONObject()
                        .put("roomId", roomId)
                        .put("consumerId", consumerId),
                )
            }
        }
    }
}
