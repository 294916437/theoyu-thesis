package com.theoyu.thesis.android.core.signaling

import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.Polling
import io.socket.engineio.client.transports.WebSocket
import java.net.URISyntaxException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class SocketIoClient {
    private val eventHandlers = ConcurrentHashMap<String, MutableMap<SocketEventHandler, Emitter.Listener>>()
    private var socket: Socket? = null

    private val _connectionState = MutableStateFlow(SocketConnectionState())
    val connectionState: StateFlow<SocketConnectionState> = _connectionState

    val isConnected: Boolean
        get() = socket?.connected() == true

    @Throws(URISyntaxException::class)
    suspend fun connect(
        url: String,
        config: SocketIoConfig = SocketIoConfig(),
    ) {
        if (isConnected) {
            return
        }

        val options = IO.Options().apply {
            transports = arrayOf(WebSocket.NAME, Polling.NAME)
            reconnection = true
            reconnectionDelay = config.reconnectionDelayMillis
            reconnectionDelayMax = config.reconnectionDelayMaxMillis
            reconnectionAttempts = config.reconnectionAttempts
            timeout = config.connectionTimeoutMillis
            path = config.path
            forceNew = true
            config.query?.let { query = it }
            if (config.auth.isNotEmpty()) {
                auth = config.auth
            }
        }

        val newSocket = IO.socket(url, options)
        socket = newSocket

        withTimeout(config.connectionTimeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                newSocket.on(Socket.EVENT_CONNECT) {
                    _connectionState.value = SocketConnectionState(
                        connected = true,
                        reconnecting = false,
                        socketId = newSocket.id(),
                    )
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                newSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val message = args.firstOrNull()?.toString() ?: "Socket connect error"
                    _connectionState.value = _connectionState.value.copy(
                        connected = false,
                        lastError = message,
                    )
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException(message))
                    }
                }

                newSocket.on(Socket.EVENT_DISCONNECT) { args ->
                    _connectionState.value = _connectionState.value.copy(
                        connected = false,
                        socketId = null,
                        lastError = args.firstOrNull()?.toString(),
                    )
                }

                newSocket.on("reconnecting") {
                    _connectionState.value = _connectionState.value.copy(reconnecting = true)
                }

                newSocket.on("reconnect_failed") {
                    val error = IllegalStateException("Socket reconnection failed")
                    _connectionState.value = _connectionState.value.copy(
                        connected = false,
                        reconnecting = false,
                        lastError = error.message,
                    )
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }

                newSocket.on("error") { args ->
                    val message = args.firstOrNull()?.toString() ?: "Socket error"
                    _connectionState.value = _connectionState.value.copy(lastError = message)
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException(message))
                    }
                }

                continuation.invokeOnCancellation {
                    newSocket.disconnect()
                    socket = null
                }

                newSocket.connect()
            }
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
        eventHandlers.clear()
        _connectionState.value = SocketConnectionState()
    }

    suspend fun emit(
        event: String,
        data: Any? = JSONObject(),
        timeoutMillis: Long = DEFAULT_EMIT_TIMEOUT_MILLIS,
    ): Any? {
        val activeSocket = socket
        check(activeSocket?.connected() == true) { "Socket not connected" }

        return withTimeout(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                activeSocket.emit(
                    event,
                    data,
                    Ack { args ->
                        val response = args.firstOrNull()
                        val errorMessage = extractError(response)

                        if (continuation.isActive) {
                            if (errorMessage != null) {
                                continuation.resumeWithException(IllegalStateException(errorMessage))
                            } else {
                                continuation.resume(response)
                            }
                        }
                    },
                )
            }
        }
    }

    fun on(event: String, handler: SocketEventHandler): SocketSubscription {
        val listener = Emitter.Listener { args -> handler.onEvent(args) }
        eventHandlers.getOrPut(event) { ConcurrentHashMap() }[handler] = listener
        socket?.on(event, listener)

        return SocketSubscription { off(event, handler) }
    }

    fun off(event: String, handler: SocketEventHandler) {
        val listener = eventHandlers[event]?.remove(handler)
        if (eventHandlers[event].isNullOrEmpty()) {
            eventHandlers.remove(event)
        }
        if (listener != null) {
            socket?.off(event, listener)
        }
    }

    fun once(event: String, handler: SocketEventHandler) {
        socket?.once(event) { args -> handler.onEvent(args) }
    }

    private fun extractError(response: Any?): String? =
        when (response) {
            is JSONObject -> if (response.has("error")) response.optString("error") else null
            is Map<*, *> -> response["error"]?.toString()
            else -> null
        }

    private companion object {
        const val DEFAULT_EMIT_TIMEOUT_MILLIS = 10_000L
    }
}

data class SocketIoConfig(
    val path: String = "/socket.io",
    val query: String? = null,
    val auth: Map<String, String> = emptyMap(),
    val connectionTimeoutMillis: Long = 10_000L,
    val reconnectionDelayMillis: Long = 1_000L,
    val reconnectionDelayMaxMillis: Long = 5_000L,
    val reconnectionAttempts: Int = 5,
)

fun interface SocketEventHandler {
    fun onEvent(args: Array<Any>)
}

fun interface SocketSubscription {
    fun dispose()
}
