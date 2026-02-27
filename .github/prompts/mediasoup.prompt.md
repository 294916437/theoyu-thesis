---
name: mediasoup
description: Describe when to use this prompt
---

### Install mediasoup v3 via NPM

Source: https://mediasoup.org/documentation/v3/mediasoup/installation

The primary method to install the mediasoup v3 package is using NPM. This command fetches the package and initiates the worker binary download or build process.

```bash
$ npm install mediasoup@3

```

---

### Install libmediasoupclient

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/installation

This command installs the compiled libmediasoupclient library and header files to the system's default locations. This step is optional and depends on whether you need the library to be system-wide available.

```bash
make install -C build/

```

---

### Force local build of mediasoup-worker binary

Source: https://mediasoup.org/documentation/v3/mediasoup/installation

This command forces the mediasoup installation to skip downloading a prebuilt worker binary and instead build it locally. This is useful for development or when prebuilt binaries are unavailable.

```bash
MEDIASOUP_SKIP_WORKER_PREBUILT_DOWNLOAD="true" npm install mediasoup@3

```

---

### Specify Python executable for build

Source: https://mediasoup.org/documentation/v3/mediasoup/installation

If the system's default Python 3 executable is not recognized, you can specify it using the `PYTHON` environment variable during the mediasoup installation process.

```bash
$ PYTHON=python3.9 npm install mediasoup@3

```

---

### Install mediasoup-client v3 using npm

Source: https://mediasoup.org/documentation/v3/mediasoup-client/installation

Installs the mediasoup-client library version 3.x.x using the npm package manager. This is the primary step for integrating the client library into your project.

```bash
$ npm install mediasoup-client@3

```

---

### Include libmediasoupclient in C++ Application

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/installation

Example of including the libmediasoupclient header file in a C++ application. This demonstrates the necessary include directive and highlights the use of the mediasoupclient namespace.

```cpp
#include "libmediasoupclient/mediasoupclient.hpp"

```

---

### Specify mediasoup-worker binary path

Source: https://mediasoup.org/documentation/v3/mediasoup/installation

This method allows you to specify a custom path for the mediasoup-worker binary during installation or when running your Node.js application. This bypasses the default download and build mechanisms.

```bash
MEDIASOUP_WORKER_BIN="/home/xxx/src/foo/mediasoup-worker" npm install mediasoup@3
MEDIASOUP_WORKER_BIN="/home/xxx/src/foo/mediasoup-worker" node myapp.js

```

---

### Clone and Checkout libmediasoupclient Repository

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/installation

This snippet demonstrates how to clone the libmediasoupclient repository from GitHub and checkout the latest stable version using Git. Ensure you have Git installed and replace '3.X.Y' with the desired tag.

```bash
git clone https://github.com/versatica/libmediasoupclient.git
cd libmediasoupclient/
git checkout 3.X.Y.

```

---

### Instantiate mediasoup-client Device Class

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Provides an example of how to create a new instance of the Device class from the mediasoup-client library.

```javascript
const device = new mediasoupClient.Device()
```

---

### Build libmediasoupclient

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/installation

Instructions to build the libmediasoupclient library after libwebrtc has been successfully built. This process uses cmake and make, specifying paths to the libwebrtc headers and binaries.

```shell
cd /home/foo/src/libmediasoupclient

cmake . -Bbuild \
  -DLIBWEBRTC_INCLUDE_PATH:PATH=/home/foo/src/webrtc-checkout/src \
  -DLIBWEBRTC_BINARY_PATH:PATH=/home/foo/src/webrtc-checkout/src/out/m140/obj

make -C build/
```

---

### Checkout and Build libwebrtc

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/installation

Steps to checkout and build the libwebrtc library. This involves fetching the repository, syncing dependencies, checking out a specific branch, and then configuring the build using gn.

```shell
cd /home/foo/src
mkdir webrtc-checkout
cd webrtc-checkout
fetch --nohooks webrtc
gclient sync
cd src
git checkout -b m140 refs/remotes/branch-heads/7339
gclient sync
```

```shell
gn gen out/m140 --args='is_debug=false is_component_build=false is_clang=true rtc_include_tests=false rtc_use_h264=true use_rtti=true use_custom_libcxx=false'
```

```shell
gn gen out/m140 --args='is_debug=false is_component_build=false is_clang=false rtc_include_tests=false rtc_use_h264=true use_rtti=true use_custom_libcxx=false treat_warnings_as_errors=false use_ozone=true'
```

```shell
ninja -C out/m140
```

---

### PlainTransport connect() Method Examples

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Demonstrates various ways to call the `connect()` method on a PlainTransport based on its configuration (comedia, rtcpMux, enableSrtp). These examples show how to provide endpoint parameters and SRTP details.

```javascript
// Calling connect() on a PlainTransport created with comedia and rtcpMux set.
await plainTransport.connect({
	ip: "1.2.3.4",
	port: 9998,
})
```

```javascript
// Calling connect() on a PlainTransport created with comedia unset and rtcpMux
// also unset.
await plainTransport.connect({
	ip: "1.2.3.4",
	port: 9998,
	rtcpPort: 9999,
})
```

```javascript
// Calling connect() on a PlainTransport created with comedia set and
// enableSrtp enabled.
await plainTransport.connect({
	srtpParameters: {
		cryptoSuite: "AES_CM_128_HMAC_SHA1_80",
		keyBase64: "ZnQ3eWJraDg0d3ZoYzM5cXN1Y2pnaHU5NWxrZTVv",
	},
})
```

```javascript
// Calling connect() on a PlainTransport created with comedia unset, rtcpMux
// set and enableSrtp enabled.
await plainTransport.connect({
	ip: "1.2.3.4",
	port: 9998,
	srtpParameters: {
		cryptoSuite: "AEAD_AES_256_GCM",
		keyBase64: "YTdjcDBvY2JoMGY5YXNlNDc0eDJsdGgwaWRvNnJsamRrdG16aWVpZHphdHo=",
	},
})
```

---

### Configure and Build libmediasoupclient with CMake

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/installation

This snippet shows how to configure the libmediasoupclient build using CMake, specifying the paths to the libwebrtc source and binary directories. It then proceeds to build the library using the Make utility.

```bash
cmake . -Bbuild \
  -DLIBWEBRTC_INCLUDE_PATH:PATH=${PATH_TO_LIBWEBRTC_SOURCES} \
  -DLIBWEBRTC_BINARY_PATH:PATH=${PATH_TO_LIBWEBRTC_BINARY}

make -C build/

```

---

### mediasoup v3 Worker Logging Example

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This is an example log output from a mediasoup v3 worker process. It shows detailed information about the worker's startup, configuration, detected environment, and interactions with other components like libuv, OpenSSL, and libSRTP. The logs include namespaces like 'mediasoup:worker[pid:NNNNNN]' and indicate the log severity and component.

```log
mediasoup:worker[pid:80653] mediasoup-worker::main() | starting mediasoup-worker process [version:3.0.0-dev] +0ms
mediasoup:worker[pid:80653] mediasoup-worker::main() | little-endian CPU detected +0ms
mediasoup:worker[pid:80653] mediasoup-worker::main() | 64 bits architecture detected +1ms
mediasoup:worker[pid:80653] Settings::PrintConfiguration() | <configuration> +0ms
mediasoup:worker[pid:80653] Settings::PrintConfiguration() |   logLevel            : debug +0ms
mediasoup:worker[pid:80653] Settings::PrintConfiguration() |   logTags             : info,simulcast +0ms
mediasoup:worker[pid:80653] Settings::PrintConfiguration() |   rtcMinPort          : 40000 +0ms
mediasoup:worker[pid:80653] Settings::PrintConfiguration() |   rtcMaxPort          : 49999 +0ms
mediasoup:worker[pid:80653] Settings::PrintConfiguration() | </configuration> +0ms
mediasoup:worker[pid:80653] DepLibUV::PrintVersion() | libuv version: "1.27.0" +0ms
mediasoup:worker[pid:80653] DepOpenSSL::ClassInit() | openssl version: "OpenSSL 1.1.1b  26 Feb 2019" +0ms
mediasoup:worker[pid:80653] DepLibSRTP::ClassInit() | libsrtp version: "libsrtp 2.0.0" +0ms
mediasoup:Worker worker process running [pid:80653] +28ms
mediasoup:Worker createRouter() +1m
mediasoup:Channel[pid:80653] request() [method:worker.createRouter, id:1] +1m
mediasoup:Channel[pid:80653] request succeeded [method:worker.createRouter, id:1] +4ms
mediasoup:Router constructor() +0ms
mediasoup:Channel[pid:80653] request() [method:router.createWebRtcTransport, id:3] +360ms
mediasoup:Channel[pid:80653] request succeeded [method:router.createWebRtcTransport, id:3] +4ms
mediasoup:Transport constructor() +0ms
mediasoup:WebRtcTransport constructor() +0ms
mediasoup:Transport setMaxIncomingBitrate() [bitrate:1500000] +4ms
mediasoup:Channel[pid:80653] request() [method:transport.setMaxIncomingBitrate, id:4] +8ms
mediasoup:Channel[pid:80653] request succeeded [method:transport.setMaxIncomingBitrate, id:4] +2ms
```

---

### C++ SendTransport Produce Method for Audio and Video

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Demonstrates using the SendTransport::Produce method to send audio and video tracks. The audio example shows specific opus codec options, while the video example illustrates setting up 3 simulcast streams. This method is asynchronous and blocks the current thread until completion.

```cpp
// Send opus audio track with specific codec options.
if (device.CanProduce("audio"))
{
	auto* audioTrack = myUtils::createAudioTrack();

	json codecOptions =
	{
		{ "opusStereo", true },
		{ "opusDtx",    true }
	};

	auto* audioProducerListener = new MyProducerListener();
	auto* audioProducer = sendTransport->Produce(
		audioProducerListener,
		audioTrack,
		nullptr,
		&codecOptions);
}

// Send video track with 3 simulcast streams.
if (device.CanProduce("video"))
{
	auto* videoTrack = myUtils::createVideoTrack();

	std::vector<webrtc::RtpEncodingParameters> encodings;

	encodings.emplace_back(webrtc::RtpEncodingParameters());
	encodings.emplace_back(webrtc::RtpEncodingParameters());
	encodings.emplace_back(webrtc::RtpEncodingParameters());

	auto* videoProducerListener = new MyProducerListener();

	// This will block the current thread until completion.
	auto* videoProducer = sendTransport->Produce(
		videoProducerListener,
		videoTrack,
		&encodings,
		nullptr);
}
```

---

### RecvTransport::Consume C++ Example

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Instructs a RecvTransport instance to consume an audio or video track from the mediasoup router. This method requires a consumer listener, the server-side consumer and producer IDs, the media kind, and RTP parameters. It is a blocking call that returns a Consumer instance upon successful setup.

```cpp
auto* consumerListener = new MyConsumerListener();

// This will block the current thread until completion.
auto* consumer = recvTransport->Consume(
  consumerListener,
  id,
  producerId,
  kind,
  rtpParameters);


```

---

### Import mediasoup-client using ES Modules

Source: https://mediasoup.org/documentation/v3/mediasoup-client/installation

Demonstrates how to import the mediasoup-client library using the ES module 'import' syntax. This method is suitable for modern JavaScript environments that support ES modules.

```javascript
import * as mediasoupClient from "mediasoup-client"
```

---

### Require mediasoup-client using CommonJS

Source: https://mediasoup.org/documentation/v3/mediasoup-client/installation

Shows how to include the mediasoup-client library using the CommonJS 'require' syntax. This is compatible with Node.js and module bundlers like browserify or webpack.

```javascript
const mediasoupClient = require("mediasoup-client")
```

---

### Worker Methods

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Methods for closing the worker, getting resource usage, updating settings, creating routers, and creating WebRTC servers.

````APIDOC
## Worker Methods

### `worker.close()`

Closes the worker. Triggers a “workerclose” event in all its routers.

### `worker.getResourceUsage()`

Provides resource usage of the worker subprocess.

> `@async`
> `@returns` WorkerResourceUsage

```javascript
const usage = await worker.getResourceUsage();

// =>
{
  ru_idrss: 0,
  ru_inblock: 0,
  ru_isrss: 0,
  ru_ixrss: 0,
  ru_majflt: 0,
  ru_maxrss: 46047232,
  ru_minflt: 11446,
  ru_msgrcv: 23641,
  ru_msgsnd: 40005,
  ru_nivcsw: 27926,
  ru_nsignals: 0,
  ru_nswap: 0,
  ru_nvcsw: 0,
  ru_oublock: 0,
  ru_stime: 1026,
  ru_utime: 3066
}
````

### `worker.updateSettings(settings)`

Updates the worker settings in runtime. Just a subset of the worker settings can be updated.

#### Parameters

- **settings** (WorkerUpdateableSettings) - Required: No - Worker updateable settings.

> `@async`

```javascript
await worker.updateSettings({ logLevel: "warn" })
```

### `worker.createRouter<RouterAppData>(options)`

Creates a new router.

#### Parameters

- **options** (RouterOptions) - Required: Yes - Router options.
- **RouterAppData** (AppData) - Required: No - Custom `appData` definition. - Default: `{ }

> `@async`
> `@returns` Router

```javascript
const mediaCodecs = [
	{
		kind: "audio",
		mimeType: "audio/opus",
		clockRate: 48000,
		channels: 2,
	},
	{
		kind: "video",
		mimeType: "video/H264",
		clockRate: 90000,
		parameters: {
			"packetization-mode": 1,
			"profile-level-id": "42e01f",
			"level-asymmetry-allowed": 1,
		},
	},
]

const router = await worker.createRouter({ mediaCodecs })
```

### `worker.createWebRtcServer<WebRtcServerAppData>(options)`

Creates a new WebRTC server.

#### Parameters

- **options** (WebRtcServerOptions) - Required: Yes - WebRTC server options.
- **WorkerAppData** (AppData) - Required: No - Custom `appData` definition. - Default: `{ }

> `@async`
> `@returns` WebRtcServer

```javascript
const webRtcServer = await worker.createWebRtcServer({
	listenInfos: [
		{
			protocol: "udp",
			ip: "9.9.9.9",
			port: 20000,
		},
		{
			protocol: "tcp",
			ip: "9.9.9.9",
			port: 20000,
		},
	],
})
```

````

--------------------------------

### Producer Methods - C++

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Provides C++ code examples for various producer methods such as replacing tracks and setting spatial layers. These methods are essential for controlling media streams and their quality in Mediasoup.

```cpp
producer.ReplaceTrack(newVideoTrack);

````

```cpp
// Assuming `encodings` array has 3 entries, let's enable just the first and
// second streams (indexes 0 and 1).
producer.setMaxSpatialLayer(1);

```

---

### Consumer API

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

This section covers the methods available for managing a consumer, including getting statistics, accessing application data, checking status, and controlling its lifecycle.

```APIDOC
## Consumer Methods

### `consumer.GetStats()`

Gets the local RTP receiver statistics by calling `getStats()` in the underlying `RTCRtpReceiver` instance.

*   **Description:** Retrieves detailed statistics for the consumer's RTP receiver.
*   **Method:** GET
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
*   **Returns:** `nlohmann::json` RTCStatsReport

### `consumer.GetAppData()`

Custom data Object provided by the application in the consumer factory method. The app can modify its content at any time.

*   **Description:** Accesses custom application data associated with the consumer.
*   **Method:** GET
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
*   **Returns:** `const nlohmann::json&`

### `consumer.IsClosed()`

Whether the consumer is closed.

*   **Description:** Checks if the consumer has been closed.
*   **Method:** GET
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
*   **Returns:** `bool`

### `consumer.IsPaused()`

Whether the consumer is paused.

*   **Description:** Checks if the consumer is currently paused.
*   **Method:** GET
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
*   **Returns:** `bool`

### `consumer.Close()`

Closes the consumer. This method should be called when the server side consumer has been closed (and vice-versa).

*   **Description:** Initiates the closure of the consumer.
*   **Method:** POST
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None

### `consumer.Pause()`

Pauses the consumer. Internally the library executes `track->set_enabled(false)` in the remote track. This method should be called when the server side consumer has been paused (and vice-versa).

*   **Description:** Pauses the consumer's media stream.
*   **Method:** POST
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None

### `consumer.Resume()`

Resumes the consumer. Internally the library executes `track->set_enabled(true)` in the remote track. This method should be called when the server side consumer has been resumed (and vice-versa).

*   **Description:** Resumes the consumer's media stream after it has been paused.
*   **Method:** POST
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
```

---

### PipeTransport Connection Example

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Demonstrates how to connect a PipeTransport to a remote peer. This involves providing the remote IP address and port, and optionally SRTP parameters if SRTP is enabled on both transports.

```javascript
await pipeTransport.connect({
	ip: "1.2.3.4",
	port: 9999,
	srtpParameters: {
		cryptoSuite: "AEAD_AES_256_GCM",
		keyBase64: "YTdjcDBvY2JoMGY5YXNlNDc0eDJsdGgwaWRvNnJsamRrdG16aWVpZHphdHo=",
	},
})
```

---

### Configure DataProducer Options (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This example shows the configuration options for creating a mediasoup DataProducer. It includes parameters like `sctpStreamParameters` for SCTP/DataChannel usage, `label` and `protocol` for identification, `paused` state, and custom `appData`. Note that `sctpStreamParameters` should not be provided for DataProducers on `DirectTransport`.

```javascript
const dataProducerOptions = {
	label: "my-data-channel",
	protocol: "my-protocol",
	paused: false,
	appData: { userId: 123 },
}
```

---

### DirectTransport Methods

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Methods available on the DirectTransport object, including getting statistics, connecting (no-op), and attempting to set bitrate limits.

````APIDOC
## DirectTransport API

### `directTransport.getStats()`

Returns current RTC statistics of the direct transport.

- **Returns**: Array<DirectTransportStat>
- **Details**: Check the RTC Statistics section for more details.

### `directTransport.connect()`

This method is a no-op for direct transports as they are always connected.

- **Note**: There is no need to call this method on direct transports.

### `directTransport.setMaxIncomingBitrate(options)`

Not implemented in direct transports. Calling this method will reject with `UnsupportedError`.

- **Note**: This method is not supported for direct transports.

### `directTransport.setMaxOutgoingBitrate(options)`

Not implemented in direct transports. Calling this method will reject with `UnsupportedError`.

- **Note**: This method is not supported for direct transports.

### `directTransport.setMinOutgoingBitrate(options)`

Not implemented in direct transports. Calling this method will reject with `UnsupportedError`.

- **Note**: This method is not supported for direct transports.

### `directTransport.sendRtcp(rtcpPacket)`

Sends a RTCP packet from the Node.js process.

- **Availability**: Only available in direct transports created via `router.createDirectTransport()`.

#### Parameters

- **rtcpPacket** (Buffer) - Required - A Node.js Buffer containing a valid RTCP packet (can be a compound packet).

#### Request Example
```javascript
// Send a RTCP packet.
directTransport.sendRtcp(rtcpPacket);
````

````

--------------------------------

### Get DataProducer Statistics (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This example shows how to asynchronously retrieve statistics for a mediasoup DataProducer. The `getStats()` method returns an array of `DataProducerStat` objects, providing insights into the data producer's performance and current status. Refer to the RTC Statistics section for detailed information on the returned statistics structure.

```javascript
async function getDataProducerStats(dataProducer) {
  const stats = await dataProducer.getStats();
  console.log("DataProducer stats:", stats);
  return stats;
}

````

---

### Get mediasoupclient Version - C++

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Retrieves the current version string of the libmediasoupclient library. This is useful for compatibility checks and logging.

```cpp
mediasoupclient::Version();
// "1.0.0"
```

---

### DataConsumer Methods

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This section outlines the available methods for interacting with a DataConsumer object, including closing, getting statistics, managing buffered data, and sending/pausing/resuming streams.

````APIDOC
## DataConsumer Methods

### `close()`

Closes the data consumer.

### `getStats()`

Retrieves current statistics for the data consumer.

*   **Asynchronous**: Yes
*   **Returns**: `Array<DataConsumerStat>`
*   **Details**: Refer to the RTC Statistics section for more information.

### `getBufferedAmount()`

Returns the number of bytes of data currently buffered for sending over the underlying SCTP association.

*   **Note**: The SCTP association uses a shared send buffer for all data consumers. This value represents the buffered data for all data consumers on the transport.
*   **Asynchronous**: Yes
*   **Returns**: `Number` (The buffered amount in bytes).

### `setBufferedAmountLowThreshold(bufferedAmountLowThreshold)`

Sets a threshold for the buffered amount. When the number of buffered bytes drops to this value, the `bufferedamountlow` event is fired.

*   **Asynchronous**: Yes
*   **Parameters**:
    *   `bufferedAmountLowThreshold` (Number): The byte count considered low for the buffered outgoing data. (Optional, Default: 0)

### `send(message, ppid)`

Sends direct messages from the Node.js process. This method is only available for data consumers of type 'SCTP'. It will fail with an error if the underlying SCTP send buffer is full (`sctpsendbufferfull`).

*   **Asynchronous**: Yes
*   **Arguments**:
    *   `message` (String | Buffer): The message to send. Can be binary if a Node.js Buffer is provided.
    *   `ppid` (Number): Mimics the SCTP Payload Protocol Identifier. Usually not required.
        *   Default: 51 (`WebRTC String`) if `message` is a String.
        *   Default: 53 (`WebRTC Binary`) if `message` is a Buffer.

*   **Example**:
    ```javascript
    const stringMessage = "hello";
    const binaryMessage = Buffer.from([ 1, 2, 3, 4 ]);

    dataConsumer.send(stringMessage);
    dataConsumer.send(binaryMessage);
    ```

### `pause()`

Pauses the data consumer, preventing messages from being sent to the consuming endpoint.

*   **Asynchronous**: Yes

### `resume()`

Resumes the data consumer, allowing messages to be sent again to the consuming endpoint.

*   **Asynchronous**: Yes

### `setSubchannels(subchannels)`

Updates the subchannels that this data consumer is subscribed to.

*   **Asynchronous**: Yes
*   **Arguments**:
    *   `subchannels` (Array<Number>): An array of unsigned 16-bit integers representing the subchannels to subscribe to. (Required)
*   **Note**: `subchannels` are relevant when receiving messages from a data producer created on a direct transport that specified subchannels during `dataProducer.send()`.

*   **Example**:
    ```javascript
    dataConsumer.setSubchannels([ 1, 4 ]);
    ```

### `addSubchannel(subchannel)`

Adds a subchannel to the list of subchannels this data consumer is subscribed to.

*   **Asynchronous**: Yes
*   **Arguments**:
    *   `subchannel` (Number): The unsigned 16-bit integer representing the subchannel to add. (Required)
*   **Note**: `subchannels` are relevant when receiving messages from a data producer created on a direct transport that specified subchannels during `dataProducer.send()`.

### `removeSubchannel(subchannel)`

Removes a subchannel from the list of subchannels this data consumer is subscribed to.

*   **Asynchronous**: Yes
*   **Arguments**:
    *   `subchannel` (Number): The unsigned 16-bit integer representing the subchannel to remove. (Required)
*   **Note**: `subchannels` are relevant when receiving messages from a data producer created on a direct transport that specified subchannels during `dataProducer.send()`.
````

---

### Build libmediasoupclient with Hidden Symbol Visibility

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/installation

This snippet demonstrates how to pass the CMAKE_CXX_FLAGS to CMake to set symbol visibility to 'hidden'. This is crucial for avoiding linker warnings related to symbol visibility mismatches, especially when libwebrtc was also built with hidden visibility.

```bash
cmake . -Bbuild \
  -DLIBWEBRTC_INCLUDE_PATH:PATH=${PATH_TO_LIBWEBRTC_SOURCES} \
  -DLIBWEBRTC_BINARY_PATH:PATH=${PATH_TO_LIBWEBRTC_BINARY} \
  -DCMAKE_CXX_FLAGS="-fvisibility=hidden"

```

---

### Transport API

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Methods for managing a WebRTC Transport instance, including getting information, statistics, and controlling its state.

````APIDOC
## Transport
A `Transport` instance in libmediasoupclient represents the local side of a WebRtcTransport in mediasoup server. A WebRTC transport connects a mediasoupclient Device with a mediasoup Router at media level and enables the sending of media (by means of Producer instances) **or** the receiving of media (by means of Consumer instances).
Internally, the transport holds a WebRTC RTCPeerConnection instance.

### Methods
#### transport.GetId()

### Description
Transport identifier. It matches the `id` of the server side transport.

### Method
`transport.GetId()`

### Parameters
None

### Response
#### Success Response (200)
- **id** (const std::string&) - The transport identifier.

#### Response Example
```json
{
  "id": "<transport_id>"
}
````

#### transport.GetConnectionState()

### Description

The current connection state of the local peerconnection.

### Method

`transport.GetConnectionState()`

### Parameters

None

### Response

#### Success Response (200)

- **connectionState** (const std::string&) - The RTCPeerConnectionState.

#### Response Example

```json
{
	"connectionState": "connected"
}
```

#### transport.GetStats()

### Description

Gets the local transport statistics by calling `getStats()` in the underlying `RTCPeerConnection` instance.
This method blocks the current thread until completion.

### Method

`transport.GetStats()`

### Parameters

None

### Response

#### Success Response (200)

- **statsReport** (nlohmann::json&) - An RTCStatsReport object containing transport statistics.

#### Response Example

```json
{
  "statsReport": { ... }
}
```

#### transport.GetAppData()

### Description

Custom data Object provided by the application in the transport constructor. The app can modify its content at any time.

### Method

`transport.GetAppData()`

### Parameters

None

### Response

#### Success Response (200)

- **appData** (const nlohmann::json&) - The custom application data.

#### Response Example

```json
{
  "appData": { ... }
}
```

#### transport.IsClosed()

### Description

Checks if the transport is closed.

### Method

`transport.IsClosed()`

### Parameters

None

### Response

#### Success Response (200)

- **isClosed** (bool) - True if the transport is closed, false otherwise.

#### Response Example

```json
{
	"isClosed": false
}
```

#### transport.Close()

### Description

Closes the transport, including all its producers and consumers. This method should be called when the server side transport has been closed (and vice-versa).

### Method

`transport.Close()`

### Parameters

None

### Request Example

```cpp
transport.Close();
```

### Response

#### Success Response (200)

Indicates that the transport has been successfully closed.

#### Response Example

```json
{
	"status": "closed"
}
```

#### transport.RestartIce(iceParameters)

### Description

Instructs the underlying peerconnection to restart ICE by providing it with new remote ICE parameters. This method must be called after restarting ICE in server side via webRtcTransport.restartIce().
This method blocks the current thread until completion.

### Method

`transport.RestartIce`

### Parameters

#### Path Parameters

None

#### Query Parameters

None

#### Request Body

- **iceParameters** (const nlohmann::json& IceParameters) - Required - New ICE parameters of the server side transport.

### Request Example

```cpp
transport.RestartIce(iceParameters);
```

### Response

#### Success Response (200)

Indicates that ICE has been successfully restarted.

#### Response Example

```json
{
	"status": "ice_restarted"
}
```

#### transport.UpdateIceServers(iceServers)

### Description

Provides the underlying peerconnection with a new list of TURN servers. This method is especially useful if the TURN server credentials have changed.

### Method

`transport.UpdateIceServers`

### Parameters

#### Path Parameters

None

#### Query Parameters

None

#### Request Body

- **iceServers** (const nlohmann::json& Array<RTCIceServer>) - Optional - List of TURN servers to provide the local peerconnection with. Default: `[ ]`

### Request Example

```cpp
transport.updateIceServers(iceServers);
```

### Response

#### Success Response (200)

Indicates that the ICE servers have been updated.

#### Response Example

```json
{
	"status": "ice_servers_updated"
}
```

````

--------------------------------

### Get Supported RTP Capabilities

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Demonstrates how to retrieve the supported RTP capabilities of mediasoup. This function returns a cloned copy of the capabilities defined in the library. Note that these are not the capabilities required for client-side device loading.

```javascript
const rtpCapabilities = mediasoup.getSupportedRtpCapabilities();

console.log(rtpCapabilities);
// => { codecs: [...], headerExtensions: [...] }

````

---

### Mediasoup Transport Method: updateIceServers()

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Provides an example of using the asynchronous `updateIceServers()` method to supply a new list of TURN servers to the underlying peer connection. This is particularly useful for updating credentials or changing TURN server configurations.

```javascript
async function updateTransportIceServers(transport, newIceServers) {
	await transport.updateIceServers({ iceServers: newIceServers })
}
```

---

### Listen to Producer Trace Event Data

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This example shows how to attach an event listener to the 'trace' event of a producer to log the received trace data. This is typically used after enabling trace events via `enableTraceEvent()`.

```javascript
producer.on("trace", (trace) => {
	console.log(trace)
})
```

---

### Enable Producer Trace Event for FIR

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This example demonstrates enabling the 'trace' event on a mediasoup producer for 'fir' (Full Intra Request) events. The listener captures information related to FIR requests, including the affected SSRC.

```javascript
producer.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "out",
    "info": {
      "ssrc": 95438003
    }
    "timestamp": 1544498155,
    "type": "fir"
  }
});

```

---

### Get Router ID

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Retrieves the unique identifier for a mediasoup router. This ID is a read-only string property.

```javascript
console.log(router.id)
// => "15177e19-5665-4eba-9a6a-c6cf3db16259"
```

---

### Enable mediasoup-client Debug Logging in localStorage

Source: https://mediasoup.org/documentation/v3/mediasoup-client/debugging

This example demonstrates how to enable debugging logs for mediasoup-client in the browser's localStorage. It sets the 'debug' key to include WARN and ERROR level messages from 'mediasoup-client'. This is useful for diagnosing issues in web applications using mediasoup-client.

```html
<script>
	window.localStorage.setItem("debug", "mediasoup-client:WARN* mediasoup-client:ERROR*")
</script>

<script src="/js/your-bundled-app.js"></script>
```

---

### Get Mediasoup Device Handler Name

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Retrieves the name of the currently selected handler for the mediasoup device. This property is read-only and returns a string representing the handler's name.

```javascript
console.log(device.handlerName)
// => "Chrome74"
```

---

### Get DataConsumer Statistics

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a DataConsumer. This includes metrics like messages sent/received and byte counts, specific to data transport.

```javascript
const stats = await dataConsumer.getStats()

// Example output:
// [
//   {
//     "type": "data-consumer",
//     "label": "nnawjiwbav",
//     "protocol": "app-protocol",
//     "messagesSent": 3496,
//     "bytesSent": 65934
//   }
// ]
```

---

### Get DataProducer Statistics

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a DataProducer. This includes metrics like messages sent/received and byte counts, specific to data transport.

```javascript
const stats = await dataProducer.getStats()

// Example output:
// [
//   {
//     "type": "data-producer",
//     "label": "nnawjiwbav",
//     "protocol": "app-protocol",
//     "messagesReceived": 3496,
//     "bytesReceived": 65934
//   }
// ]
```

---

### Create Device Instance - C++

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Instantiates a new Device object for a mediasoup client endpoint. This is the primary entry point for C++ client-side applications interacting with mediasoup.

```cpp
auto* device = new mediasoupclient::Device();
```

---

### Get Worker PID - JavaScript

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Retrieves the process ID (pid) of a mediasoup worker instance. This is a read-only property and provides information about the running worker subprocess.

```javascript
console.log(worker.pid)
// => 86665
```

---

### Initialization and Cleanup

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Provides functions for initializing and cleaning up the libmediasoupclient library, including libwebrtc.

````APIDOC
## Initialization and Cleanup

### mediasoupclient::Initialize()

Initializes libmediasoupclient, including libwebrtc.

```cpp
mediasoupclient::Initialize();
````

### mediasoupclient::Cleanup()

Cleans up libmediasoupclient, including libwebrtc.

```cpp
mediasoupclient::Cleanup();
```

### mediasoupclient::Version()

Returns the libmediasoupclient version.

> `@returns` std::string

```cpp
mediasoupclient::Version();
// "1.0.0"
```

````

--------------------------------

### Enable Transport Trace Event for Bandwidth Estimation (BWE)

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This example shows how to enable the 'trace' event on a mediasoup transport for 'bwe' (Bandwidth Estimation) type events. The listener captures and logs details about the network's available and desired bitrates, along with other BWE-related information.

```javascript
transport.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "out",
    "info": {
      "availableBitrate": 1981250,
      "desiredBitrate": 1483574,
      "effectiveDesiredBitrate": 1483574,
      "maxBitrate": 2002824,
      "maxPaddingBitrate": 1702400,
      "minBitrate": 30000,
      "startBitrate": 1981250,
      "type": 'transport-cc'
    },
    "timestamp": 1513191082,
    "type": "bwe"
  }
});

````

---

### Enable Producer Trace Event for PLI

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This example shows how to enable the 'trace' event on a mediasoup producer for 'pli' (Picture Loss Indication) events. The listener captures information related to PLI requests, including the affected SSRC.

```javascript
producer.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "out",
    "info": {
      "ssrc": 87654321
    }
    "timestamp": 1544498146,
    "type": "pli"
  }
});

```

---

### RTPObserver Add Producer Method

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Adds a producer to be monitored by an RTP observer. This method takes an options object containing the `producerId` of the producer to add. The operation is asynchronous and allows the RTP observer to start inspecting the media from the specified producer.

```javascript
await rtpObserver.addProducer({ producerId: "some-producer-id" })
```

---

### Define OnRtpSenderCallback Type (TypeScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Defines the TypeScript type for the OnRtpSenderCallback function. This callback is invoked synchronously after an RTCRtpSender is created, allowing for immediate manipulation or setup of encoded streams in compatible browsers.

```typescript
type OnRtpSenderCallback = (rtpSender: RTCRtpSender) => void
```

---

### Consume with Specific Stream IDs (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

This example demonstrates consuming media streams with specific `streamId` values. The `streamId` is used to generate the 'a=msid' attribute for SDP and helps synchronize inbound audio/video streams, especially when multiple streams originate from the same peer.

```javascript
micConsumer = await transport.consume({ streamId: `${remotePeerId}-mic-webcam` })
webcamConsumer = await transport.consume({ streamId: `${remotePeerId}-mic-webcam` })
screensharingConsumer = await transport.consume({ streamId: `${remotePeerId}-screensharing` })
```

---

### mediasoupclient Namespace Initialization and Versioning (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Provides core functions for initializing and cleaning up the mediasoupclient library, as well as retrieving its version. It also includes a utility function for parsing scalability modes. These functions are essential for setting up and managing the mediasoupclient environment.

```cpp
#include "libmediasoupclient/mediasoupclient.hpp"

// Example usage:
// Get version
const char* version = mediasoupclient::Version();

// Initialize the library
mediasoupclient::Initialize();

// Parse scalability mode
// auto scalabilityMode = mediasoupclient::parseScalabilityMode("simulcast");

// Cleanup the library
// mediasoupclient::Cleanup();

```

---

### Get Worker Resource Usage (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Retrieves the resource usage statistics for the worker subprocess. This is an asynchronous operation and returns a WorkerResourceUsage object containing various resource metrics.

```javascript
const usage = await worker.getResourceUsage();

// =>
{
  ru_idrss: 0,
  ru_inblock: 0,
  ru_isrss: 0,
  ru_ixrss: 0,
  ru_majflt: 0,
  ru_maxrss: 46047232,
  ru_minflt: 11446,
  ru_msgrcv: 23641,
  ru_msgsnd: 40005,
  ru_nivcsw: 27926,
  ru_nsignals: 0,
  ru_nswap: 0,
  ru_nvcsw: 0,
  ru_oublock: 0,
  ru_stime: 1026,
  ru_utime: 3066
}

```

---

### Configure RTCP Warnings and Errors Logging

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This example shows how to configure the mediasoup worker to log RTCP warnings and all errors using environment variables and worker settings. It sets the log level to 'warn' and specifies 'rtcp' as the tag.

```bash
DEBUG="mediasoup:WARN:* mediasoup:ERROR:*" node myapp.js
```

```javascript
const mediasoup = require("mediasoup")

const worker = await mediasoup.createWorker({
	logLevel: "warn",
	logTags: ["rtcp"],
})
```

---

### Enable Producer Trace Event for NACK

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This example demonstrates enabling the 'trace' event on a mediasoup producer for 'nack' (Negative Acknowledgment) events. The listener captures basic information when a NACK is sent, typically indicating lost packets.

```javascript
producer.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "out",
    "info": {}
    "timestamp": 1544498146,
    "type": "nack"
  }
});

```

---

### Initialize Mediasoup and Set Event Listeners (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This snippet demonstrates how to initialize the mediasoup library and set up global event listeners. It shows the usage of `mediasoup.setLogEventListeners` to capture various log events emitted by mediasoup. No external dependencies are required beyond the mediasoup library itself.

```javascript
import * as mediasoup from "mediasoup"

// Example event listeners
const listeners = {
	log: (level, log) => {
		console.log(`[${level}]: ${log}`)
	},
	error: (error) => {
		console.error("Mediasoup Error:", error)
	},
}

// Set the event listeners
mediasoup.setLogEventListeners(listeners)

console.log("Mediasoup event listeners set.")
// Further mediasoup initialization would go here.
```

---

### Set RTP Encoding Parameters for Producer (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

This method allows adding parameters to all encodings within a producer's RTCRtpSender. Use with caution as it modifies existing parameters. An example shows setting the network priority to 'high'.

```javascript
await producer.setRtpEncodingParameters({ networkPriority: "high" })
```

---

### Get Consumer Statistics (PipeTransport)

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves RTP stream statistics for a consumer attached to a PipeTransport. This will only include 'outbound-rtp' entries for each forwarded stream, as inbound RTP statistics from the producer are not included.

```javascript
const stats = await pipeConsumer.getStats()

// Example output:
// [
//   {
//     "bitrate": 868184,
//     "byteCount": 19478693,
//     "firCount": 0,
//     "fractionLost": 0,
//     "kind": "video",
//     "mimeType": "video/VP8",
//     "nackCount": 0,
//     "nackPacketCount": 0,
//     "packetCount": 18696,
//     "packetsDiscarded": 0,
//     "packetsLost": 0,
//     "packetsRepaired": 0,
//     "packetsRetransmitted": 0,
//     "pliCount": 0,
//     "roundTripTime": 5.15,
//     "score": 10,
//     "ssrc": 116684231,
//     "timestamp": 514442975,
//     "type": "outbound-rtp"
//   },
//   {
//     "bitrate": 350000,
//     "byteCount": 8393425,
//     "firCount": 0,
//     "fractionLost": 0,
//     "kind": "video",
//     "mimeType": "video/VP8",
//     "nackCount": 0,
//     "nackPacketCount": 0,
//     "packetCount": 9417,
//     "packetsDiscarded": 0,
//     "packetsLost": 0,
//     "packetsRepaired": 0,
//     "packetsRetransmitted": 0,
//     "pliCount": 0,
//     "roundTripTime": 4.43,
//     "score": 10,
//     "ssrc": 116684230,
//     "timestamp": 514442975,
//     "type": "outbound-rtp"
//   },
//   {
//     "bitrate": 153456,
//     "byteCount": 3442897,
//     "firCount": 0,
//     "fractionLost": 0,
//     "kind": "video",
//     "mimeType": "video/VP8",
//     "nackCount": 0,
//     "nackPacketCount": 0,
//     "packetCount": 5393,
//     "packetsDiscarded": 0,
//     "packetsLost": 0,
//     "packetsRepaired": 0,
//     "packetsRetransmitted": 0,
//     "pliCount": 0,
//     "roundTripTime": 5.6,
//     "score": 10,
//     "ssrc": 116684229,
//     "timestamp": 514442975,
//     "type": "outbound-rtp"
//   }
// ]
```

---

### Configure ICE/DTLS Debugs, Warnings, and Errors Logging

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This example demonstrates how to configure the mediasoup worker to log ICE and DTLS debug messages, warnings, and all errors. It uses environment variables for broad logging and sets the worker's log level to 'debug' with 'ice' and 'dtls' tags.

```bash
DEBUG="mediasoup* *ERROR*" node myapp.js
```

```javascript
const mediasoup = require("mediasoup")

const worker = await mediasoup.createWorker({
	logLevel: "debug",
	logTags: ["ice", "dtls"],
})
```

---

### Get PipeTransport Statistics (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a PipeTransport. This provides insights into packet transmission and bitrate for transports used for media relay. It includes timestamp and transport ID for identification.

```javascript
const stats = await pipeTransport.getStats()

// =>
;[
	{
		probationBytesSent: 0,
		probationSendBitrate: 0,
		recvBitrate: 1802072,
		rtpBytesReceived: 5104571,
		rtpBytesSent: 0,
		rtpRecvBitrate: 1835651,
		rtpSendBitrate: 0,
		rtxBytesReceived: 0,
		rtxBytesSent: 0,
		rtxRecvBitrate: 0,
		rtxSendBitrate: 0,
		sendBitrate: 24,
		timestamp: 924308980,
		transportId: "352f60cd-10ac-443b-8529-6474ecba2e46",
		tuple: {
			localAddress: "11.22.33.44",
			localPort: 12455,
			protocol: "udp",
			remoteIp: "11.22.33.44",
			remotePort: 42301,
		},
		type: "pipe-transport",
	},
]
```

---

### Enable Producer Trace Event for Keyframes

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This example shows how to enable the 'trace' event on a mediasoup producer specifically for 'keyframe' packets. When this type is enabled, it replaces 'rtp' events for keyframes, providing details about the keyframe itself, such as isKeyFrame status and SSRC.

```javascript
producer.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "in",
    "info": {
      "isKeyFrame": true,
      "marker": "false",
      "mid": "2",
      "payloadSize": 1088,
      "payloadType": 96,
      "rid": "r2",
      "rrid": "r2",
      "sequenceNumber": 14176,
      "size": 1116,
      "spatialLayer": 0,
      "ssrc": 3838709357,
      "temporalLayer": 0,
      "timestamp": 3003475216,
      "wideSequenceNumber": 62
    },
    "timestamp": 1513798049,
    "type": "keyframe"
  }
});

```

---

### Update Runtime Settings for ICE/DTLS Warning Logs

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This example shows how to dynamically update the mediasoup worker's settings at runtime to enable only ICE and DTLS warning logs. It modifies the log level to 'warn' and specifies 'ice' and 'dtls' as the relevant tags.

```javascript
worker.updateSettings({
	logLevel: "warn",
	logTags: ["ice", "dtls"],
})
```

---

### DataConsumer Class: Receiving Data (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

The `DataConsumer` class represents a received data stream. It provides methods to get its ID, data producer ID, SCTP stream parameters, ready state, label, protocol, and application data. It also allows closing the consumer.

```cpp
#include "libmediasoupclient/DataConsumer.hpp"

// Assuming 'dataConsumer' is an instance of mediasoupclient::DataConsumer

// Get data consumer ID
// const std::string& id = dataConsumer.GetId();

// Get associated data producer ID
// const std::string& producerId = dataConsumer.GetDataProducerId();

// Get ready state
// const std::string& state = dataConsumer.GetReadyState();

// Check if closed
// bool closed = dataConsumer.IsClosed();

// Close the data consumer
// dataConsumer.Close();

```

---

### Initialize and Cleanup mediasoupclient - C++

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Provides functions to initialize and clean up the libmediasoupclient library, including its dependency on libwebrtc. Initialization should be called before using other library functions, and cleanup should be called when the application exits.

```cpp
mediasoupclient::Initialize();
// ... use mediasoupclient ...
mediasoupclient::Cleanup();
```

---

### Create Mediasoup Device using Factory

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Asynchronously creates a new mediasoup Device instance using the `Device.factory()` method. This is the recommended approach for creating a device. It includes error handling for unsupported browsers or devices, logging a warning if the environment is not supported.

```javascript
let device

try {
	device = await Device.factory()
} catch (error) {
	if (error.name === "UnsupportedError") console.warn("browser not supported")
}
```

---

### Enable Transport Trace Event for Probation

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This example demonstrates how to enable the 'trace' event on a mediasoup transport for 'probation' type events. It includes a listener that receives and logs detailed information about probation events, such as direction, timestamp, and specific info.

```javascript
transport.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "out",
    "info": {
      "isKeyFrame": false,
      "marker": "false",
      "payloadSize": 360,
      "payloadType": 127,
      "sequenceNumber": 19244,
      "size": 384,
      "spatialLayer": 0,
      "ssrc": 1234,
      "temporalLayer": 0,
      "timestamp": 239090504,
      "wideSequenceNumber": 166
    },
    "timestamp": 1513191082,
    "type": "probation"
  }
});

```

---

### Create AudioLevelObserver with Mediasoup v3

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a new audio level observer to monitor audio levels. It accepts optional AudioLevelObserverOptions including maxEntries, threshold, and interval. Returns an AudioLevelObserver object.

```typescript
const audioLevelObserver = await router.createAudioLevelObserver({
	maxEntries: 1,
	threshold: -70,
	interval: 2000,
})
```

---

### Worker: Get Resource Usage (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This method retrieves the current resource usage (CPU and memory) of a mediasoup Worker. It returns a promise that resolves with a `WorkerResourceUsage` object. This is useful for monitoring worker performance.

```javascript
import * as mediasoup from "mediasoup"

async function getWorkerResources(worker) {
	try {
		const usage = await worker.getResourceUsage()
		console.log(`Worker ${worker.pid} resource usage:`, usage)
		return usage
	} catch (error) {
		console.error(`Failed to get resource usage for worker ${worker.pid}:`, error)
		throw error
	}
}

// Example usage (assuming 'myWorker' is an existing Worker instance):
// getWorkerResources(myWorker);
```

---

### Set Mediasoup Log Event Listeners

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Demonstrates how to set custom event listeners for mediasoup logs (debug, warn, error). If called without arguments, no log events will be emitted. Example shows logging warnings and errors to a custom logger.

```javascript
mediasoup.setLogEventListeners({
	ondebug: undefined,
	onwarn: (namespace, log) => {
		MyEnterpriseLogger.warn(`${namespace} ${log}`)
	},
	onerror: (namespace, log, error) => {
		if (error) {
			MyEnterpriseLogger.error(`${namespace} ${log}: ${error}`)
		} else {
			MyEnterpriseLogger.error(`${namespace} ${log}`)
		}
	},
})
```

---

### Get Consumer Statistics (RTP)

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves RTP stream statistics for a consumer. Includes 'outbound-rtp' for the consumer's stream and 'inbound-rtp' for the associated producer's stream. This is the standard behavior for most consumers.

```javascript
const stats = await consumer.getStats()

// Example output:
// [
//   {
//     "bitrate": 625312,
//     "byteCount": 879947,
//     "firCount": 0,
//     "fractionLost": 0,
//     "kind": "video",
//     "mimeType": "video/VP8",
//     "nackCount": 0,
//     "nackPacketCount": 0,
//     "packetCount": 979,
//     "packetsDiscarded": 0,
//     "packetsLost": 0,
//     "packetsRepaired": 0,
//     "packetsRetransmitted": 0,
//     "pliCount": 0,
//     "roundTripTime": 33.02,
//     "rtxSsrc": 836324070,
//     "score": 10,
//     "ssrc": 328066115,
//     "timestamp": 925531753,
//     "type": "outbound-rtp"
//   },
//   {
//     "bitrate": 627872,
//     "bitrateByLayer": {
//       "0.0": 238856,
//       "0.1": 145872,
//       "0.2": 243144
//     },
//     "byteCount": 883855,
//     "firCount": 0,
//     "fractionLost": 0,
//     "jitter": 2,
//     "kind": "video",
//     "mimeType": "video/VP8",
//     "nackCount": 0,
//     "nackPacketCount": 0,
//     "packetCount": 979,
//     "packetsDiscarded": 0,
//     "packetsLost": 0,
//     "packetsRepaired": 0,
//     "packetsRetransmitted": 167,
//     "pliCount": 2,
//     "rtxSsrc": 1976184061,
//     "score": 10,
//     "ssrc": 2440984788,
//     "timestamp": 925531753,
//     "type": "inbound-rtp"
//   }
// ]
```

---

### Enable Consumer Trace Event for RTP

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This example demonstrates enabling the 'trace' event on a mediasoup consumer for 'rtp' type packets. The event listener captures detailed RTP packet information received by the consumer, such as sequence number, timestamp, SSRC, and payload details.

```javascript
consumer.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "out",
    "info": {
      "isKeyFrame": false,
      "marker": "false",
      "payloadSize": 1,
      "payloadType": 100,
      "sequenceNumber": 6,
      "size": 21,
      "spatialLayer": 0,
      "ssrc": 198373608,
      "temporalLayer": 0,
      "timestamp": 54740510
    },
    "timestamp": 1514273430,
    "type": "rtp"
  }
});

```

---

### Create Mediasoup Device using Constructor (Deprecated)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Creates a new mediasoup Device instance using the `Device` constructor. This method is deprecated and error handling for unsupported environments is included. It logs a warning if the browser or device is not supported.

```javascript
let device

try {
	device = new mediasoupClient.Device()
} catch (error) {
	if (error.name === "UnsupportedError") console.warn("browser not supported")
}
```

---

### DataProducer Class: Sending Data (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

The `DataProducer` class manages sending non-media data over SCTP. It provides methods to get its ID, SCTP stream parameters, ready state, label, protocol, and buffered amount. It also allows closing the producer and sending data via a buffer.

```cpp
#include "libmediasoupclient/DataProducer.hpp"

// Assuming 'dataProducer' is an instance of mediasoupclient::DataProducer
// Assuming 'buffer' is a data buffer (e.g., std::string or char*)

// Get data producer ID
// const std::string& id = dataProducer.GetId();

// Get ready state
// const std::string& state = dataProducer.GetReadyState();

// Check if closed
// bool closed = dataProducer.IsClosed();

// Close the data producer
// dataProducer.Close();

// Send data
// dataProducer.Send(buffer);

```

---

### Get Supported RTP Capabilities (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This function retrieves the RTP capabilities supported by the mediasoup library. It's useful for understanding the media codecs and other RTP-related features that can be negotiated. This method does not require any arguments and returns an object detailing the supported capabilities.

```javascript
import * as mediasoup from "mediasoup"

function getSupportedRtpCapabilities() {
	const capabilities = mediasoup.getSupportedRtpCapabilities()
	console.log("Supported RTP Capabilities:", JSON.stringify(capabilities, null, 2))
	return capabilities
}

// Example usage:
const rtpCapabilities = getSupportedRtpCapabilities()
```

---

### Get DirectTransport Statistics (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a DirectTransport. This function returns data on packet transmission and bitrate, useful for understanding direct media path performance. Includes timestamp and transport ID.

```javascript
const stats = await directTransport.getStats()

// =>
;[
	{
		probationBytesSent: 0,
		probationSendBitrate: 0,
		recvBitrate: 5672,
		rtpBytesReceived: 0,
		rtpBytesSent: 0,
		rtpRecvBitrate: 0,
		rtpSendBitrate: 0,
		rtxBytesReceived: 0,
		rtxBytesSent: 0,
		rtxRecvBitrate: 0,
		rtxSendBitrate: 0,
		sendBitrate: 3204,
		timestamp: 894308981,
		transportId: "huif60cd-10ac-443b-8529-6474ecba2123",
		type: "direct-transport",
	},
]
```

---

### DataConsumer SCTP Send Buffer Full Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Handles the 'sctpsendbufferfull' event for a data consumer. This event is emitted when a message cannot be sent because the SCTP send buffer is full. No specific action is shown in the example, but this indicates a potential bottleneck.

```javascript
dataConsumer.on("sctpsendbufferfull", () => {
	// Handle SCTP send buffer full scenario
})
```

---

### Producer Class: Media Track Management (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

The `Producer` class represents a media track being sent. It provides methods to get its ID, kind, track object, RTP parameters, statistics, and application data. It also allows pausing, resuming, closing, replacing the track, and setting the maximum spatial layer.

```cpp
#include "libmediasoupclient/Producer.hpp"

// Assuming 'producer' is an instance of mediasoupclient::Producer

// Get producer ID
// const std::string& id = producer.GetId();

// Get track kind
// const std::string& kind = producer.GetKind();

// Check if paused
// bool paused = producer.IsPaused();

// Pause the producer
// producer.Pause();

// Resume the producer
// producer.Resume();

// Close the producer
// producer.Close();

```

---

### RecvTransport::ConsumeData C++ Example

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Enables a RecvTransport to receive data via a DataChannel from the mediasoup router. It takes a DataConsumer listener, server-side consumer and producer IDs, and optionally a label and protocol for the DataChannel. This is a blocking asynchronous operation that returns a DataConsumer instance.

```cpp
auto* consumerListener = new MyConsumerListener();

// This will block the current thread until completion.
auto* consumer = recvTransport->Consume(
  consumerListener,
  id,
  "dataChannelLabel",
  "dataChannelProtocol");


```

---

### Get PlainTransport Statistics (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a PlainTransport. This includes data on received and sent bytes, bitrate, and the transport's tuple information. It's useful for debugging and monitoring raw RTP traffic.

```javascript
const stats = await plainTransport.getStats()

// =>
;[
	{
		bytesReceived: 467406,
		bytesSent: 2550,
		comedia: true,
		rtcpMux: true,
		probationBytesSent: 0,
		probationSendBitrate: 0,
		recvBitrate: 1802072,
		rtpBytesReceived: 5104571,
		rtpBytesSent: 0,
		rtpRecvBitrate: 1835651,
		rtpSendBitrate: 0,
		rtxBytesReceived: 0,
		rtxBytesSent: 0,
		rtxRecvBitrate: 0,
		rtxSendBitrate: 0,
		sendBitrate: 24,
		timestamp: 924308648,
		transportId: "8e7dc219-5cb0-4cca-b1ca-0bbbc584a364",
		tuple: {
			localAddress: "11.22.33.44",
			localPort: 45346,
			protocol: "udp",
			remoteIp: "55.66.77.88",
			remotePort: 56971,
		},
		type: "plain-rtp-transport",
	},
]
```

---

### Create WebRTC Server (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a new WebRTC server instance. This asynchronous method requires WebRtcServerOptions, specifying listen information like protocol, IP, and port. It can also accept custom AppData.

```javascript
const webRtcServer = await worker.createWebRtcServer({
	listenInfos: [
		{
			protocol: "udp",
			ip: "9.9.9.9",
			port: 20000,
		},
		{
			protocol: "tcp",
			ip: "9.9.9.9",
			port: 20000,
		},
	],
})
```

---

### Get WebRtcTransport Statistics (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a WebRtcTransport. This includes details on DTLS and ICE states, bitrate, packet counts, and transport IDs. It's essential for monitoring the health and performance of WebRTC connections.

```javascript
const stats = await webRtcTransport.getStats()

// =>
;[
	{
		availableOutgoingBitrate: 6750000,
		bytesReceived: 5360091,
		bytesSent: 20988,
		dtlsState: "connected",
		iceRole: "controlled",
		iceSelectedTuple: {
			localAddress: "11.22.33.44",
			localPort: 56726,
			protocol: "udp",
			remoteIp: "55.66.77.88",
			remotePort: 52320,
		},
		iceState: "completed",
		maxIncomingBitrate: 5500000,
		probationBytesSent: 0,
		probationSendBitrate: 0,
		recvBitrate: 1802072,
		rtpBytesReceived: 5104571,
		rtpBytesSent: 0,
		rtpPacketLossSent: 0,
		rtpRecvBitrate: 1835651,
		rtpSendBitrate: 0,
		rtxBytesReceived: 179934,
		rtxBytesSent: 0,
		rtxRecvBitrate: 0,
		rtxSendBitrate: 0,
		sctpState: "connected",
		sendBitrate: 4992,
		timestamp: 18079607138,
		transportId: "a00746bd-0758-4dfc-9f5f-c0ad4eb326d5",
		type: "webrtc-transport",
	},
]
```

---

### Device Management

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Manages the mediasoup client device, including loading, checking capabilities, and producing media.

````APIDOC
## Device

A device represents an endpoint that connects to a mediasoup Router to send and/or receive media. This is the entry point for C++ client side applications.

### Constructor

#### new Device()

Creates a new device.

```cpp
auto* device = new mediasoupclient::Device();
````

### Methods

#### device.GetRtpCapabilities()

Returns the device RTP capabilities, generated by combining both the underlying WebRTC capabilities and the router RTP capabilities (see the Load() method).

> `@async` blocks current thread
> `@returns` const nlohmann::json& RtpCapabilities
> `@throws` mediasoupclient::Exception, if device not loaded

These RTP capabilities must be given to the mediasoup router in order to consume a remote stream. Check the Communication Between Client and Server section for more details.

#### device.IsLoaded()

Checks if the device has been loaded (see the Load() method).

> `@returns` bool

#### device.Load(routerRtpCapabilities, peerConnectionOptions = nullptr)

Loads the device with the RTP capabilities of the mediasoup router. This is how the device knows about the allowed media codecs and other settings.

### Parameters

#### Path Parameters

- **routerRtpCapabilities** (const nlohmann::json& RtpCapabilities) - Required - The mediasoup router RTP capabilities.
- **peerConnectionOptions** (PeerConnection::Options\*) - Optional - PeerConnection options.

> `@throws` mediasoupclient::Exception, if device already loaded

The router RTP capabilities are exposed via the router.rtpCapabilities getter. Check the Communication Between Client and Server section for more details. See also how to filter those server-side capabilities before applying them to the libmediasoupclient device.

### Request Example

```cpp
device.Load(routerRtpCapabilities);
// Now the device is ready.
```

#### device.CanProduce(kind)

Checks whether the device can produce media of the given kind. This depends on the media codecs enabled in the mediasoup router and the media capabilities of libwebrtc.

### Parameters

#### Path Parameters

- **kind** (const std::string&) - Required - "audio" or "video".

> `@returns` bool
> `@throws` mediasoupclient::Exception, if device not loaded
> `@throws` mediasoupclient::Exception, if invalid kind

### Request Example

```cpp
if (device.CanProduce("video")) {
  // Produce video.
}
```

### Dictionaries

#### PeerConnection::Options

- **config** (webrtc::PeerConnectionInterface::RTCConfiguration) - Optional - PeerConnection configuration.
- **factory** (webrtc::PeerConnectionFactoryInterface\*) - Optional - PeerConnection factory.

````

--------------------------------

### Enabling mediasoup Logging with DEBUG Environment Variable

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This section provides examples on how to set the DEBUG environment variable to enable logging for mediasoup. The DEBUG variable controls which messages are outputted by libraries using the 'debug' module. Setting it to 'mediasoup*' will log all mediasoup messages.

```shell
$ export DEBUG="mediasoup*"
$ node myapp.js
````

```shell
$ DEBUG="mediasoup*" node myapp.js
```

```shell
$ DEBUG="*" node myapp.js
```

```shell
$ DEBUG="mediasoup*" node myapp.js
```

```shell
$ DEBUG="mediasoup:WARN:* mediasoup:ERROR:*" node myapp.js
```

---

### Monitor Mediasoup Entity Events with Observer API (Node.js)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This example demonstrates how to use the Mediasoup observer API in Node.js to monitor events like 'newworker', 'close', 'newrouter', 'newtransport', 'newproducer', 'newconsumer', 'newdataproducer', 'newdataconsumer', and 'newwebrtcserver'. It sets up event listeners for various Mediasoup entities to log their lifecycle events. This is intended for use by external libraries or modules integrating with Mediasoup.

```javascript
const mediasoup = require("mediasoup")

mediasoup.observer.on("newworker", (worker) => {
	console.log("new worker created [worke.pid:%d]", worker.pid)

	worker.observer.on("close", () => {
		console.log("worker closed [worker.pid:%d]", worker.pid)
	})

	worker.observer.on("newrouter", (router) => {
		console.log("new router created [worker.pid:%d, router.id:%s]", worker.pid, router.id)

		router.observer.on("close", () => {
			console.log("router closed [router.id:%s]", router.id)
		})

		router.observer.on("newtransport", (transport) => {
			console.log("new transport created [worker.pid:%d, router.id:%s, transport.id:%s]", worker.pid, router.id, transport.id)

			transport.observer.on("close", () => {
				console.log("transport closed [transport.id:%s]", transport.id)
			})

			transport.observer.on("newproducer", (producer) => {
				console.log("new producer created [worker.pid:%d, router.id:%s, transport.id:%s, producer.id:%s]", worker.pid, router.id, transport.id, producer.id)

				producer.observer.on("close", () => {
					console.log("producer closed [producer.id:%s]", producer.id)
				})
			})

			transport.observer.on("newconsumer", (consumer) => {
				console.log("new consumer created [worker.pid:%d, router.id:%s, transport.id:%s, consumer.id:%s]", worker.pid, router.id, transport.id, consumer.id)

				consumer.observer.on("close", () => {
					console.log("consumer closed [consumer.id:%s]", consumer.id)
				})
			})

			transport.observer.on("newdataproducer", (dataProducer) => {
				console.log("new data producer created [worker.pid:%d, router.id:%s, transport.id:%s, dataProducer.id:%s]", worker.pid, router.id, transport.id, dataProducer.id)

				dataProducer.observer.on("close", () => {
					console.log("data producer closed [dataProducer.id:%s]", dataProducer.id)
				})
			})

			transport.observer.on("newdataconsumer", (dataConsumer) => {
				console.log("new data consumer created [worker.pid:%d, router.id:%s, transport.id:%s, dataConsumer.id:%s]", worker.pid, router.id, transport.id, dataConsumer.id)

				dataConsumer.observer.on("close", () => {
					console.log("data consumer closed [dataConsumer.id:%s]", dataConsumer.id)
				})
			})
		})
	})

	worker.observer.on("newwebrtcserver", (webRtcServer) => {
		console.log("new WebRTC server created [worker.pid:%d, webRtcServer.id:%s]", worker.pid, webRtcServer.id)

		webRtcServer.observer.on("close", () => {
			console.log("WebRTC server closed [webRtcServer.id:%s]", webRtcServer.id)
		})
	})
})
```

---

### Enable Producer Trace Event for RTP

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This example demonstrates enabling the 'trace' event on a mediasoup producer for 'rtp' type packets. The event listener captures detailed RTP packet information, including sequence numbers, timestamps, SSRC, payload types, and spatial/temporal layers.

```javascript
producer.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "in",
    "info": {
      "isKeyFrame": false,
      "marker": "true",
      "mid": "6",
      "payloadSize": 914,
      "payloadType": 96,
      "rid": "r1",
      "rrid": "r1",
      "sequenceNumber": 19694,
      "size": 942,
      "spatialLayer": 0,
      "ssrc": 27777256,
      "temporalLayer": 1,
      "timestamp": 1227771600,
      "wideSequenceNumber": 2413
    },
    "timestamp": 1513714260,
    "type": "rtp"
  }
});

```

---

### RtpObserver Properties, Methods, and Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Details the RtpObserver API, including its properties, methods for managing the observer, and events it can emit.

```APIDOC
## RtpObserver API

### Description
Provides methods and properties for managing RtpObservers.

### Properties
- `id` (string): The unique identifier of the RtpObserver.
- `closed` (boolean): Indicates if the RtpObserver is closed.
- `type` (RtpObserverType): The type of the RtpObserver.
- `paused` (boolean): Indicates if the RtpObserver is paused.
- `appData` (object): Application-specific data.
- `observer`: An object for observing RtpObserver events.

### Methods
- `close()`: Closes the RtpObserver.
- `pause()`: Pauses the RtpObserver.
- `resume()`: Resumes the RtpObserver.
- `addProducer(options)`: Adds a producer to the RtpObserver.
- `removeProducer(options)`: Removes a producer from the RtpObserver.

### Events
- `routerclose`: Emitted when the associated router is closed.
- `listenererror`: Emitted with an event name and error.

### Observer Events
- `close`: Emitted when the observer is closed.
- `pause`: Emitted when the observer is paused.
- `resume`: Emitted when the observer is resumed.
- `addproducer`: Emitted with the added producer.
- `removeproducer`: Emitted with the removed producer.
```

---

### device.load()

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Loads the device with the RTP capabilities of the mediasoup router. This action configures the device with the allowed media codecs and other necessary settings for communication.

````APIDOC
## device.load({ routerRtpCapabilities, preferLocalCodecsOrder })

### Description
Loads the device with the RTP capabilities of the mediasoup router. This is how the device knows about the allowed media codecs and other settings.

### Method
`device.load()`

### Parameters
#### Request Body
- **routerRtpCapabilities** (RtpCapabilities) - Required - The mediasoup router RTP capabilities.
- **preferLocalCodecsOrder** (Boolean) - Optional - Whether to prefer device's local order of codecs rather than the order of codecs provided to mediasoup server.

### Request Example
```javascript
await device.load({ routerRtpCapabilities });
// Now the device is ready.
````

### Throws

- `InvalidStateError`: if device already loaded
- `TypeError`: if invalid arguments

````

--------------------------------

### Device Methods: Transport Creation (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Methods within the Device class for creating `SendTransport` and `RecvTransport` instances. These methods require listener interfaces, unique IDs, ICE and DTLS parameters, and optional peer connection options and application data.

```cpp
#include "libmediasoupclient/Device.hpp"
#include "libmediasoupclient/Transport.hpp"

// Assuming 'device' is an instance of mediasoupclient::Device
// Assuming 'listener' is an object implementing Transport::Listener
// Assuming 'iceParameters', 'iceCandidates', 'dtlsParameters' are properly initialized

// Create a sending transport
// mediasoupclient::Transport* sendTransport = device.CreateSendTransport(
//     listener, "transport-id-1", iceParameters, iceCandidates, dtlsParameters
// );

// Create a receiving transport
// mediasoupclient::Transport* recvTransport = device.CreateRecvTransport(
//     listener, "transport-id-2", iceParameters, iceCandidates, dtlsParameters
// );

````

---

### Consumer Methods

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This section details the methods available for interacting with and managing a consumer instance in Mediasoup v3.

````APIDOC
## Consumer Methods

### `consumer.close()`

**Description**: Closes the consumer.

**Method**: `async`

### `consumer.getStats()`

**Description**: Returns current RTC statistics of the consumer. Refer to the RTC Statistics section for more details.

**Method**: `async`

**Returns**: `Array<ConsumerStat>`

### `consumer.pause()`

**Description**: Pauses the consumer, ceasing RTP transmission to the consuming endpoint.

**Method**: `async`

### `consumer.resume()`

**Description**: Resumes the consumer, re-initiating RTP transmission to the consuming endpoint.

**Method**: `async`

### `consumer.setPreferredLayers(preferredLayers)`

**Description**: Sets the preferred spatial and temporal layers for simulcast and SVC consumers. The temporal layer is optional and defaults to the highest if not specified.

**Method**: `async`

**Parameters**:

*   **`preferredLayers`** (ConsumerLayers) - Required - Preferred spatial and temporal layers.

**Example**:
```javascript
await consumer.setPreferredLayers({ spatialLayer: 3 });
````

### `consumer.setPriority(priority)`

**Description**: Sets the priority for the consumer, influencing bitrate distribution among video consumers when bandwidth is limited. Priority ranges from 1 (minimum) to 255 (maximum).

**Method**: `async`

**Parameters**:

- **`priority`** (Number) - Required - Priority value from 1 to 255.

**Note**: Consumers' priority is only significant when the estimated outgoing bitrate is insufficient for all video consumers.

**Example**:

```javascript
await consumer.setPriority(2)
```

### `consumer.unsetPriority()`

**Description**: Unsets the priority for the consumer, resetting it to the default value of 1.

**Method**: `async`

**Example**:

```javascript
await consumer.unsetPriority()
```

### `consumer.requestKeyFrame()`

**Description**: Requests a key frame from the associated producer. Only applicable to video consumers.

**Method**: `async`

### `consumer.enableTraceEvent(types)`

**Description**: Enables the emission of "trace" events for monitoring purposes. Use with caution.

**Method**: `async`

**Parameters**:

- **`types`** (Array<ConsumerTraceEventType>) - Optional - Types of trace events to enable.

**Example**:

```javascript
await consumer.enableTraceEvent(["rtp", "pli", "fir"])

consumer.on("trace", (trace) => {
	// trace.type can be "rtp" or "pli" or "fir".
})
```

````

--------------------------------

### Consumer

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the Consumer class, including its properties, methods, and events.

```APIDOC
## Consumer

### Description
Represents a media consumer, receiving audio or video streams.

### Dictionaries
- **ConsumerOptions**
- **ConsumerLayers**
- **ConsumerScore**
- **ConsumerTraceEventData**

### Enums
- **ConsumerType**
- **ConsumerTraceEventType**

### Properties
- **id** (string) - The unique identifier for the consumer.
- **producerId** (string) - The ID of the associated producer.
- **closed** (boolean) - Indicates if the consumer is closed.
- **kind** (MediaKind) - The media kind (audio or video).
- **rtpParameters** (RtpParameters) - The RTP parameters for the consumer.
- **type** (ConsumerType) - The type of the consumer.
- **paused** (boolean) - Indicates if the consumer is paused.
- **producerPaused** (boolean) - Indicates if the associated producer is paused.
- **score** (ConsumerScore) - The current score of the consumer.
- **preferredLayers** (ConsumerLayers) - The preferred layers for the consumer.
- **currentLayers** (ConsumerLayers) - The current layers of the consumer.
- **priority** (number) - The priority of the consumer.
- **appData** (object) - Application-specific data.
- **observer** (ConsumerObserver) - An observer for consumer events.

### Methods
- **close()** - Closes the consumer.
- **getStats()** - Retrieves statistics for the consumer.
- **pause()** - Pauses the consumer.
- **resume()** - Resumes the consumer.
- **setPreferredLayers(preferredLayers)** - Sets the preferred layers for the consumer.
- **setPriority(priority)** - Sets the priority for the consumer.
- **unsetPriority()** - Unsets the priority for the consumer.
- **requestKeyFrame()** - Requests a key frame from the producer.
- **enableTraceEvent(types)** - Enables trace events for specified types.

### Events
- **on(“transportclose”, fn())** - Emitted when the associated transport is closed.
- **on(“producerclose”, fn())** - Emitted when the associated producer is closed.
- **on(“producerpause”, fn())** - Emitted when the associated producer is paused.
- **on(“producerresume”, fn())** - Emitted when the associated producer is resumed.

### Observer Events
- **observer.on(“close”, fn())** - Observer event for consumer closure.
- **observer.on(“pause”, fn())** - Observer event for consumer pausing.
- **observer.on(“resume”, fn())** - Observer event for consumer resumption.
- **observer.on(“score”, fn(score))** - Observer event for score changes.
- **observer.on(“videoorientationchange”, fn(videoOrientation))** - Observer event for video orientation changes.
- **observer.on(“trace”, fn(trace))** - Observer event for trace events.
````

---

### Enable Producer Trace Event for SR (Sender Report)

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This example shows how to enable the 'trace' event on a mediasoup producer for 'sr' (Sender Report) events. The listener captures detailed sender report information, including SSRC, NTP timestamps, RTP timestamp, packet count, and octet count.

```javascript
producer.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "out",
    "info": {
      "ssrc": 15438003,
      "ntp_sec": 768723434,
      "ntp_frac": 87876,
      "rtp_ts": 23768,
      "packet_count": 100,
      "octet_count": 200
    }
    "timestamp": 164498155,
    "type": "sr"
  }
});

```

---

### Get Simulcast Producer Statistics (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a producer using simulcast. This will return multiple RTP streams, each potentially with multiple temporal layers. The output includes bitrate, packet loss, jitter, and other relevant metrics for each stream and layer.

```javascript
const stats = await producer.getStats()

// =>
;[
	{
		bitrate: 678400,
		bitrateByLayer: {
			"0.0": 237992,
			0.1: 145496,
			0.2: 294912,
		},
		byteCount: 4265668,
		firCount: 0,
		fractionLost: 0,
		jitter: 0,
		kind: "video",
		mimeType: "video/VP8",
		nackCount: 0,
		nackPacketCount: 0,
		packetCount: 4150,
		packetsDiscarded: 0,
		packetsLost: 0,
		packetsRepaired: 0,
		packetsRetransmitted: 95,
		pliCount: 5,
		rid: "r2",
		roundTripTime: 43.55,
		rtxPacketsDiscarded: 0,
		rtxSsrc: 2830213299,
		score: 10,
		ssrc: 689337360,
		timestamp: 925298114,
		type: "inbound-rtp",
	},
	{
		bitrate: 242784,
		bitrateByLayer: {
			"0.0": 85608,
			0.1: 52752,
			0.2: 104424,
		},
		byteCount: 1677745,
		firCount: 0,
		fractionLost: 0,
		jitter: 0,
		kind: "video",
		mimeType: "video/VP8",
		nackCount: 5,
		nackPacketCount: 31,
		packetCount: 2045,
		packetsDiscarded: 0,
		packetsLost: 4294967281,
		packetsRepaired: 15,
		packetsRetransmitted: 563,
		pliCount: 3,
		rid: "r1",
		roundTripTime: 48.1,
		rtxPacketsDiscarded: 0,
		rtxSsrc: 2486781276,
		score: 10,
		ssrc: 2995277190,
		timestamp: 925298114,
		type: "inbound-rtp",
	},
	{
		bitrate: 86768,
		bitrateByLayer: {
			"0.0": 29648,
			0.1: 19344,
			0.2: 37776,
		},
		byteCount: 581258,
		firCount: 0,
		fractionLost: 0,
		jitter: 2,
		kind: "video",
		mimeType: "video/VP8",
		nackCount: 0,
		nackPacketCount: 0,
		packetCount: 1362,
		packetsDiscarded: 0,
		packetsLost: 0,
		packetsRepaired: 0,
		packetsRetransmitted: 10,
		pliCount: 1,
		rid: "r0",
		roundTripTime: 49.77,
		rtxPacketsDiscarded: 0,
		rtxSsrc: 2118917939,
		score: 10,
		ssrc: 3060700812,
		timestamp: 925298114,
		type: "inbound-rtp",
	},
]
```

---

### Router Options

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Options for creating a Mediasoup router, including supported media codecs and custom application data.

```APIDOC
## RouterOptions

A dictionary with options for creating a Router.

### Fields
- **mediaCodecs** (Array<RouterRtpCodecCapability>) - Required: No, Default: `[]` - Router media codecs. Feature codecs such as RTX MUST NOT be placed into the `mediaCodecs` list. If `preferredPayloadType` is given in a `RouterRtpCodecCapability` (although it's unnecessary) it's extremely recommended to use a value in the 96-127 range.
- **appData** (AppData) - Required: No, Default: `{}` - Custom application data.
```

---

### Get SVC Producer Statistics (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a producer using SVC (Scalable Video Coding). This will return a single RTP stream with multiple spatial and temporal layers. The output details bitrate, packet loss, and other metrics for each layer.

```javascript
;[
	{
		bitrate: 680020,
		bitrateByLayer: {
			"0.0": 38957,
			0.1: 48842,
			0.2: 72589,
			"1.0": 135837,
			1.1: 175149,
			1.2: 260762,
			"2.0": 323139,
			2.1: 461565,
			2.2: 680020,
		},
		byteCount: 337978,
		firCount: 0,
		fractionLost: 0,
		jitter: 4,
		kind: "video",
		mimeType: "video/VP9",
		nackCount: 0,
		nackPacketCount: 0,
		packetCount: 347,
		packetsDiscarded: 0,
		packetsLost: 0,
		packetsRepaired: 0,
		packetsRetransmitted: 149,
		pliCount: 0,
		roundTripTime: 34.57,
		rtxPacketsDiscarded: 0,
		rtxSsrc: 4171189299,
		score: 10,
		ssrc: 518176773,
		timestamp: 1205013977,
		type: "inbound-rtp",
	},
]
```

---

### RTP Negotiation Overview

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Describes the process of RTP negotiation between mediasoup routers and endpoints, including how RTP capabilities and parameters are exchanged and used for media transmission and reception.

```APIDOC
## RTP Negotiation Overview

When a mediasoup Router is created, it's configured with `RtpCodecCapability` for enabled audio and video codecs. The application then obtains `router.rtpCapabilities`, which includes enhanced router codecs with retransmission and RTCP support, along with supported RTP header extensions, and provides these to the endpoints.

An endpoint intending to send media to mediasoup uses the router's RTP capabilities and its own to compute its sending RTP parameters. These parameters are then transmitted to the router, assuming a transport has been established. Subsequently, a Producer instance is created in the router using `transport.produce()`.

To receive media from the router associated with a specific producer, the application uses the endpoint's RTP capabilities with the `transport.consume()` API, specifying the `producerId`. This generates a Consumer instance whose RTP receive parameters are calculated by merging the producer's RTP parameters and the endpoint's RTP capabilities. The application then signals the resulting `consumer.rtpParameters` to the endpoint.

mediasoup is flexible regarding received RTP parameters from endpoints; `payloadType` and header extension `id` values can differ from the router's preferred ones, but sent codecs **MUST** be present in the router's capabilities.

Conversely, mediasoup is strict when sending to endpoints; `preferredPayloadType` and `preferredId` values in the endpoint's RTP capabilities **MUST** match those in the router's RTP capabilities. mediasoup constructs RTP receive parameters based on the consumed producer's RTP parameters and the endpoint's RTP capabilities.

**Rule:**
  * The entity sending RTP (mediasoup or endpoint) determines the sending IDs.
  * The entity receiving RTP (mediasoup or endpoint) must adhere to those IDs.
```

---

### DataProducer Properties, Methods, and Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the DataProducer API, including its properties, methods for managing the producer, and events it can emit.

```APIDOC
## DataProducer API

### Description
Provides methods and properties for managing DataProducers.

### Properties
- `id` (string): The unique identifier of the DataProducer.
- `closed` (boolean): Indicates if the DataProducer is closed.
- `type` (DataProducerType): The type of the DataProducer.
- `sctpStreamParameters` (SctpStreamParameters): SCTP stream parameters.
- `label` (string): The label of the DataProducer.
- `protocol` (string): The protocol of the DataProducer.
- `paused` (boolean): Indicates if the DataProducer is paused.
- `appData` (object): Application-specific data.
- `observer`: An object for observing DataProducer events.

### Methods
- `close()`: Closes the DataProducer.
- `getStats()`: Retrieves statistics for the DataProducer.
- `send(message, ppid, subchannels, requiredSubchannel)`: Sends a message.
- `pause()`: Pauses the DataProducer.
- `resume()`: Resumes the DataProducer.

### Events
- `transportclose`: Emitted when the associated transport is closed.
- `listenererror`: Emitted with an event name and error.
```

---

### Handle New Data Producer Creation (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Listens for the 'producedata' event on a Mediasoup transport. This event is emitted when a new data producer needs to be created on the server-side transport, occurring before produceData() finishes. The example illustrates sending SCTP stream parameters, label, and protocol to the server and then using the server-assigned producer ID to complete the operation.

```javascript
transport.on("producedata", async (parameters, callback, errback) => {
	// Signal parameters to the server side transport and retrieve the id of
	// the server side new producer.
	try {
		const data = await mySignaling.send("transport-producedata", {
			transportId: transport.id,
			sctpStreamParameters: parameters.sctpStreamParameters,
			label: parameters.label,
			protocol: parameters.protocol,
		})

		// Let's assume the server included the created producer id in the response
		// data object.
		const { id } = data

		// Tell the transport that parameters were transmitted and provide it with the
		// server side producer's id.
		callback({ id })
	} catch (error) {
		// Tell the transport that something was wrong.
		errback(error)
	}
})
```

---

### SendTransport::OnProduceData C++ Example

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Handles the emission of data producer information from the transport to the server side. It involves constructing a JSON body with transport details and signaling information, sending it via a signaling client, and then processing the server's response to extract the data producer ID. This function is called before produceData() completes and returns a future containing the server-side data producer ID.

```cpp
std::future<std::string> MySendTransportListener::OnProduceData(
		SendTransport* transport,
		const nlohmann::json& sctpStreamParameters,
		const std::string& label,
		const std::string& protocol,
		const nlohmann::json& appData)
{
	std::promise<std::string> promise;

	json body =
	{
		{ "transportId",          transport->GetId()   },
		{ "sctpStreamParameters", sctpStreamParameters },
		{ "label",                label                },
		{ "protocol",             protocol             },
		{ "appData",              appData              }
	};

	json response = mySignaling.send("transport-produce-data", body);

  // [...] Let's assume code execution continues once we get a success response
  // from the server.

  // Read the id in the response.
	auto idIt = response.find("id");
	if (idIt == response.end() || !idIt->is_string())
  {
		promise.set_exception(
      std::make_exception_ptr("'id' missing/invalid in response"));
  }

  // Fulfil the promise with the id in the response and return its future.
	promise.set_value(idIt->get<std::string>());

	return promise.get_future();
}

```

---

### mediasoupClient.Device Class

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Documentation for the main `Device` class in mediasoup-client, which is essential for managing WebRTC devices.

````APIDOC
## mediasoupClient.Device Class

### Description
The main `Device` class used to interact with WebRTC functionalities within mediasoup-client.

> `@type` Device, read only

### Usage
```javascript
const device = new mediasoupClient.Device();
````

````

--------------------------------

### DirectTransportOptions

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Options for creating a DirectTransport.

```APIDOC
## DirectTransportOptions

### Description
Options for creating a DirectTransport.

### Parameters
#### Request Body
- **maxMessageSize** (Number) - Optional - Maximum allowed size for direct messages sent by `DataProducers`. Defaults to 262144.
- **appData** (AppData) - Optional - Custom application data. Defaults to `{ }`.
````

---

### TransportOptions

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Configuration options for creating a new WebRTC transport.

```APIDOC
## Transport Options

### Description
Configuration options for creating a new WebRTC transport.

### Parameters
#### Request Body
- **id** (String) - Required - The identifier of the server side transport.
- **iceParameters** (IceParameters) - Required - ICE parameters of the server side transport.
- **iceCandidates** (Array<IceCandidate>) - Required - ICE candidates of the server side transport.
- **dtlsParameters** (DtlsParameters) - Required - DTLS parameters of the server side transport.
- **sctpParameters** (SctpParameters) - Optional - SCTP parameters of the server side transport.
- **iceServers** (Array<RTCIceServer>) - Optional - List of TURN servers. This setting is given to the local peerconnection. Default: `[ ]`
- **iceTransportPolicy** (RTCIceTransportPolicy) - Optional - ICE candidate policy for the local peerconnection. Default: `"all"`
- **additionalSettings** (Object) - Optional - Additional RTCConfiguration settings other than `iceServers`, `iceTransportPolicy`, `bundlePolicy`, `rtcpMuxPolic` and `sdpSemantics`. Use it to enable experimental settings.
- **proprietaryConstraints** (Object) - Optional - Browser vendor's proprietary constraints used as second argument in the peerconnection constructor (deprecated in the WebRTC 1.0 API).
- **appData** (Object) - Optional - Custom application data. Default: `{ }`
```

---

### Handle Listener Errors in Consumer Events (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This example shows how to subscribe to the 'listenererror' event on a mediasoup consumer. This event fires when an application-provided event listener throws an error. By listening to this event, developers can be notified of exceptions occurring within their custom event handlers, even though these exceptions are internally ignored to maintain the consumer's state.

```javascript
consumer.on("listenererror", (eventName, error) => {
	console.error(`Error in event listener [${eventName}]:`, error)
})
```

---

### Import mediasoup-client Module (ES6 and CommonJS)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Demonstrates how to import the entire mediasoup-client module or specific components using ES6 import statements and CommonJS require statements.

```javascript
// Using ES6 import.
import * as mediasoupClient from "mediasoup-client"

// Or using destructuring assignment:
import { types, version, Device, detectDevice, detectDeviceAsync, parseScalabilityMode, debug } from "mediasoup-client"

// Using CommonJS.
const mediasoupClient = require("mediasoup-client")

// Or using destructuring assignment:
const { types, version, Device, detectDevice, detectDeviceAsync, parseScalabilityMode, debug } = require("mediasoup-client")
```

---

### Create Router with Media Codecs (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a new router instance within the worker. This asynchronous method accepts RouterOptions, which includes an array of media codecs to be supported by the router. It can also accept custom AppData.

```javascript
const mediaCodecs = [
	{
		kind: "audio",
		mimeType: "audio/opus",
		clockRate: 48000,
		channels: 2,
	},
	{
		kind: "video",
		mimeType: "video/H264",
		clockRate: 90000,
		parameters: {
			"packetization-mode": 1,
			"profile-level-id": "42e01f",
			"level-asymmetry-allowed": 1,
		},
	},
]

const router = await worker.createRouter({ mediaCodecs })
```

---

### Consumer Methods: Set Preferred Layers and Priority

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Demonstrates how to set the preferred video layers and priority for a consumer. This is crucial for optimizing bandwidth usage, especially in simulcast or SVC scenarios, and for prioritizing specific consumers when bandwidth is limited.

```javascript
await consumer.setPreferredLayers({ spatialLayer: 3 })

await consumer.setPriority(2)

await consumer.unsetPriority()
```

---

### PipeTransportOptions

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Options for creating a PipeTransport.

```APIDOC
## PipeTransportOptions

### Description
Options for creating a PipeTransport.

### Parameters
#### Request Body
- **listenInfo** (TransportListenInfo) - Required - Listening information.
- **listenIp** (TransportListenIp|String) - Required - Listening IP address. DEPRECATED, use `listenInfo` instead.
- **port** (Number) - Optional - Fixed port to listen on instead of selecting automatically from Worker's port range.
- **enableSctp** (Boolean) - Optional - Create a SCTP association. Defaults to `false`.
- **numSctpStreams** (NumSctpStreams) - Optional - SCTP streams number.
- **maxSctpMessageSize** (Number) - Optional - Maximum allowed size for SCTP messages sent by `DataProducers`. Defaults to 268435456.
- **sctpSendBufferSize** (Number) - Optional - SCTP send buffer size used by usrsctp. Defaults to 268435456.
- **enableRtx** (Boolean) - Optional - Enable RTX and NACK for RTP retransmission. Useful if both `pipeTransports` run in different hosts. If enabled, the paired `pipeTransport` must also enable this setting. Defaults to `false`.
- **enableSrtp** (Boolean) - Optional - Enable SRTP to encrypt RTP and SRTP. If enabled, the paired `pipeTransport` must also enable this setting. Defaults to `false`.
- **appData** (AppData) - Optional - Custom application data. Defaults to `{ }`.
```

---

### Device Class: Creation and Management (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

The Device class is central to mediasoupclient, representing the client's device. It allows for loading device capabilities, checking production capabilities, and creating send or receive transports. It requires router RTP capabilities and optional peer connection options for loading.

```cpp
#include "libmediasoupclient/Device.hpp"

// Example usage:
// Create a Device instance
// mediasoupclient::Device device;

// Load the device with router RTP capabilities and optional peer connection options
// bool success = device.Load(routerRtpCapabilities, peerConnectionOptions);

// Check if the device is loaded
// bool loaded = device.IsLoaded();

// Check if the device can produce a given media kind
// bool canProduce = device.CanProduce("video");

```

---

### mediasoupClient

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Top-level object for mediasoup-client.

```APIDOC
## mediasoupClient

### Description
The main object for interacting with mediasoup-client.

### Properties
- **types**: (Object) - Contains type definitions.
- **version**: (String) - The current version of mediasoup-client.
- **debug**: (Boolean) - Enables or disables debug logging.

### Classes
- **Device**: Represents a client device capable of connecting to a mediasoup router.

### Functions
- **detectDevice(userAgent, userAgentData)**: Detects device capabilities based on user agent information.
- **detectDeviceAsync(userAgent)**: Asynchronously detects device capabilities.
- **parseScalabilityMode(scalabilityMode)**: Parses a scalability mode string into an object.
```

---

### WebRtcServer API

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the WebRtcServer object, which manages listening on a UDP/TCP port for WebRtcTransports.

````APIDOC
## WebRtcServer

### Description
A WebRTC server facilitates listening on a single UDP/TCP port for WebRtcTransports. It handles network traffic for transports instead of them listening on their own ports. Note that each worker requires its own WebRTC server instance, and mediasoup uses ICE Lite.

### WebRtcServerOptions
- **listenInfos** (Array<TransportListenInfo>) - Optional - Listening information, ordered by preference. If '0.0.0.0' or '::' is used, `announcedAddress` must be provided.
- **appData** (AppData) - Optional - Custom application data.

### Properties
- **id** (String, read only) - The unique identifier for the WebRTC server.
- **closed** (Boolean, read only) - Indicates if the WebRTC server is closed.
- **appData** (AppData) - Mutable custom data provided by the application.
- **observer** (EventEmitter, read only) - An event emitter for observer events.

### Methods
#### webRtcServer.close()
Closes the WebRTC server. This action triggers a 'listenserverclose' event on all associated WebRTC transports.

### Events
#### webRtcServer.on('workerclose')
Emitted when the worker associated with this WebRTC server is closed. The server itself is also closed, and a 'listenserverclose' event is triggered on all associated WebRTC transports.
```javascript
webRtcServer.on('workerclose', () => {
  console.log('Worker closed, WebRtcServer is now closed.');
});
````

````

--------------------------------

### Router Interconnection and Observers

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Methods for piping producers between routers and creating observers for media events.

```APIDOC
## POST /router/pipeToRouter

### Description
Pipes a media or data producer into another router within the same host. This method can create an underlying PipeTransport if one doesn't exist and is useful for expanding broadcasting capabilities across different workers.

### Method
POST

### Endpoint
/router/pipeToRouter

### Parameters
#### Request Body
- **options** (PipeToRouterOptions) - Required - Options object containing `producerId`, `router` (the target router), and optionally `skip`, `appData`, `paused`, `data`, `media`.
  - **producerId** (string) - The ID of the producer to pipe.
  - **router** (Router) - The target router object.
  - **keepId** (boolean) - Optional. If true (default), the producer ID is preserved. If false, a new ID is generated.

### Request Example
```json
{
  "options": {
    "producerId": "producer-id-to-pipe",
    "router": "target-router-object",
    "keepId": true
  }
}
````

### Response

#### Success Response (200)

- **pipeResult** (PipeToRouterResult) - Contains information about the piped producer or data producer.

#### Response Example

```json
{
	"pipeResult": {
		"producerId": "piped-producer-id",
		"dataProducerId": null
	}
}
```

## POST /router/createActiveSpeakerObserver

### Description

Creates a new active speaker observer to detect which participants are speaking.

### Method

POST

### Endpoint

/router/createActiveSpeakerObserver

### Parameters

#### Request Body

- **options** (ActiveSpeakerObserverOptions) - Optional - Options for the observer, such as `interval`.
- **AppData** (AppData) - Optional - Custom application data.

### Request Example

```json
{
	"options": {
		"interval": 500
	}
}
```

### Response

#### Success Response (200)

- **observer** (ActiveSpeakerObserver) - The created active speaker observer object.

#### Response Example

```json
{
	"observer": {
		"id": "active-speaker-observer-id",
		"type": "activeSpeaker"
	}
}
```

## POST /router/createAudioLevelObserver

### Description

Creates a new audio level observer to monitor the audio levels of participants.

### Method

POST

### Endpoint

/router/createAudioLevelObserver

### Parameters

#### Request Body

- **options** (AudioLevelObserverOptions) - Optional - Options for the observer, such as `maxEntries`, `threshold`, and `interval`.
- **AppData** (AppData) - Optional - Custom application data.

### Request Example

```json
{
	"options": {
		"maxEntries": 1,
		"threshold": -70,
		"interval": 2000
	}
}
```

### Response

#### Success Response (200)

- **observer** (AudioLevelObserver) - The created audio level observer object.

#### Response Example

```json
{
	"observer": {
		"id": "audio-level-observer-id",
		"type": "audioLevel"
	}
}
```

````

--------------------------------

### Load Device with Router Capabilities - C++

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Loads the Device with the RTP capabilities of a mediasoup router. This step is crucial for the device to understand the allowed media codecs and other settings for sending and receiving media. It can optionally accept PeerConnection options.

```cpp
device.Load(routerRtpCapabilities);
// Now the device is ready.
````

---

### Handle Transport Connection Establishment (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Listens for the 'connect' event on a Mediasoup transport. This event is emitted when the transport is about to establish an ICE+DTLS connection. It requires exchanging DTLS parameters with the server-side transport. The provided code snippet demonstrates signaling local DTLS parameters to the server and handling success or failure callbacks.

```javascript
transport.on("connect", async ({ dtlsParameters }, callback, errback) => {
	// Signal local DTLS parameters to the server side transport.
	try {
		await mySignaling.send("transport-connect", {
			transportId: transport.id,
			dtlsParameters: dtlsParameters,
		})

		// Tell the transport that parameters were transmitted.
		callback()
	} catch (error) {
		// Tell the transport that something was wrong.
		errback(error)
	}
})
```

---

### DataProducer Listener Events

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Defines the abstract listener class for DataProducers and the events it can handle, such as DataChannel opening, closing, and buffer amount changes.

```APIDOC
## DataProducer::Listener

This is an abstract class which must be implemented and used according to the API.

### Events

#### `DataProducerListener::OnOpen(producer)`

Executed when the underlying DataChannel is open.

*   **Description:** Callback function invoked when the DataChannel associated with the producer becomes open.
*   **Arguments:**
    *   `producer` (DataProducer*) - The producer instance executing this method. (Required)

#### `DataProducerListener::OnClose(producer)`

Executed when the underlying DataChannel is closed for unknown reasons.

*   **Description:** Callback function invoked when the DataChannel associated with the producer is closed unexpectedly.
*   **Arguments:**
    *   `producer` (DataProducer*) - The producer instance executing this method. (Required)

#### `DataProducerListener::OnBufferAmountChange(producer, sentDataSize)`

Executed when the DataChannel buffered amount of bytes changes.

*   **Description:** Callback function invoked when the amount of data buffered for sending changes.
*   **Arguments:**
    *   `producer` (DataProducer*) - The producer instance executing this method. (Required)
    *   `sentDataSize` (uint64_t) - The amount of data sent. (Required)

```

---

### Router API

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the Router object, including methods for checking consumer capabilities, updating media codecs, and handling related events.

````APIDOC
## POST /router/canConsume

### Description
Checks if the given RTP capabilities are valid for consuming a specific producer.

### Method
POST

### Endpoint
/router/canConsume

### Parameters
#### Request Body
- **producerId** (String) - Required - The ID of the producer.
- **rtpCapabilities** (RtpCapabilities) - Required - The RTP capabilities of the potential consumer.

### Request Example
```json
{
  "producerId": "some_producer_id",
  "rtpCapabilities": { ... }
}
````

### Response

#### Success Response (200)

- **canConsume** (Boolean) - Indicates whether the consumer can consume the producer.

#### Response Example

```json
{
	"canConsume": true
}
```

## PUT /router/updateMediaCodecs

### Description

Updates the media codecs of the router. This action modifies the router's RTP capabilities.

### Method

PUT

### Endpoint

/router/updateMediaCodecs

### Parameters

#### Request Body

- **mediaCodecs** (Array<RouterRtpCodecCapability>) - Required - The new array of media codecs for the router.

### Request Example

```json
{
  "mediaCodecs": [
    { ... },
    { ... }
  ]
}
```

### Response

#### Success Response (200)

- **success** (Boolean) - Indicates if the media codecs were updated successfully.

#### Response Example

```json
{
	"success": true
}
```

## Events

### router.on('workerclose')

Emitted when the worker associated with this router is closed. This also closes the router, its transports, and RTP observers.

```javascript
router.on("workerclose", () => {
	console.log("Worker closed, router is now closed.")
})
```

### router.on('listenererror')

Emitted when an event listener provided by the application throws an error. The exception is silently ignored internally.

#### Parameters

- **eventName** (String) - The name of the event where the error occurred.
- **error** (Error) - The error object thrown by the event listener.

### router.observer.on('close')

Emitted when the router is closed.

### router.observer.on('newtransport')

Emitted when a new transport is created.

#### Parameters

- **transport** (Transport) - The newly created transport object.

```javascript
router.observer.on("newtransport", (transport) => {
	console.log(`New transport created with ID: ${transport.id}`)
})
```

### router.observer.on('newrtpobserver')

Emitted when a new RTP observer is created.

#### Parameters

- **rtpObserver** (RtpObserver) - The newly created RTP observer object.

```javascript
router.observer.on("newrtpobserver", (rtpObserver) => {
	console.log(`New RTP observer created with ID: ${rtpObserver.id}`)
})
```

````

--------------------------------

### Observe New WebRTC Server Creation (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Subscribes to the 'newwebrtcserver' event on the worker's observer. This event is triggered when a new WebRTC server is created, and it provides the WebRtcServer instance as an argument.

```javascript
worker.observer.on("newwebrtcserver", (webRtcServer) =>
{
  console.log("new WebRTC server created [id:%s]", webRtcServer.id);
});

````

---

### Create a Mediasoup Worker (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This code illustrates how to create a new mediasoup Worker instance. It requires the `mediasoup` library and takes an optional `WorkerSettings` object for configuration. The function returns a `Worker` object or rejects with an error if creation fails. The `WorkerAppData` allows for custom data to be attached to the worker.

```javascript
import * as mediasoup from 'mediasoup';

// Define custom app data for the worker
interface MyWorkerAppData {
  roomId: string;
}

async function createMediasoupWorker() {
  try {
    const workerSettings = {
      logLevel: 'debug',
      logTags: ['info', 'ice', 'dtls', 'sctp'],
      rtcMinPort: 10000,
      rtcMaxPort: 50000,
    };
    const worker = await mediasoup.createWorker<MyWorkerAppData>(workerSettings);
    worker.appData = { roomId: 'my-room-id' }; // Assign custom app data

    console.log('Mediasoup worker created successfully:', worker.pid);
    return worker;
  } catch (error) {
    console.error('Failed to create mediasoup worker:', error);
    throw error;
  }
}

// Example usage:
createMediasoupWorker().then(worker => {
  // Worker is ready, can now create routers, etc.
}).catch(err => {
  // Handle worker creation error
});
```

---

### DataConsumer Properties, Methods, and Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Details the DataConsumer API, including its properties, methods for managing the consumer, and events it can emit.

```APIDOC
## DataConsumer API

### Description
Provides methods and properties for managing DataConsumers.

### Properties
- `id` (string): The unique identifier of the DataConsumer.
- `dataProducerId` (string): The ID of the associated DataProducer.
- `closed` (boolean): Indicates if the DataConsumer is closed.
- `type` (DataConsumerType): The type of the DataConsumer.
- `sctpStreamParameters` (SctpStreamParameters): SCTP stream parameters.
- `label` (string): The label of the DataConsumer.
- `protocol` (string): The protocol of the DataConsumer.
- `paused` (boolean): Indicates if the DataConsumer is paused.
- `dataProducerPaused` (boolean): Indicates if the associated DataProducer is paused.
- `subchannels` (object): Subchannel information.
- `appData` (object): Application-specific data.
- `observer`: An object for observing DataConsumer events.

### Methods
- `close()`: Closes the DataConsumer.
- `getStats()`: Retrieves statistics for the DataConsumer.
- `getBufferedAmount()`: Gets the buffered amount.
- `setBufferedAmountLowThreshold()`: Sets the buffered amount low threshold.
- `send(message, ppid)`: Sends a message.
- `pause()`: Pauses the DataConsumer.
- `resume()`: Resumes the DataConsumer.
- `setSubchannels(subchannels)`: Sets the subchannels.
- `addSubchannel(subchannel)`: Adds a subchannel.
- `removeSubchannel(subchannel)`: Removes a subchannel.

### Events
- `transportclose`: Emitted when the associated transport is closed.
- `dataproducerclose`: Emitted when the associated DataProducer is closed.
- `dataproducerpause`: Emitted when the associated DataProducer is paused.
- `dataproducerresume`: Emitted when the associated DataProducer is resumed.
- `message`: Emitted with received message and PPID.
- `sctpsendbufferfull`: Emitted when the SCTP send buffer is full.
- `bufferedamountlow`: Emitted when the buffered amount is low.
- `listenererror`: Emitted with an event name and error.
```

---

### Device Transport Creation

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Methods to create WebRTC transports for sending or receiving media using the mediasoup v3 client.

````APIDOC
## device.CreateSendTransport

### Description
Creates a new WebRTC transport to **send** media. The transport must be previously created in the mediasoup router via router.createWebRtcTransport().

### Method
`device.CreateSendTransport`

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
- **listener** (SendTransport::Listener) - Required - The identifier of the server side transport.
- **id** (const std::string&) - Required - The identifier of the server side transport.
- **iceParameters** (const nlohmann::json& IceParameters) - Required - ICE parameters of the server side transport.
- **iceCandidates** (const nlohmann::json& Array<IceCandidate>) - Required - ICE candidates of the server side transport.
- **dtlsParameters** (const nlohmann::json& DtlsParameters) - Required - DTLS parameters of the server side transport.
- **peerConnectionOptions** (PeerConnection::Options*) - Optional - PeerConnection options.
- **appData** (nlohmann::json) - Optional - Custom application data. Default: `{ }`

### Request Example
```cpp
auto* sendTransportListener = new MySendTransportListener();

// This will block the current thread until completion.
auto* sendTransport = device.CreateSendTransport(
  sendTransportListener,
  id,
  iceParameters,
  iceCandidates,
  dtlsParameters);
````

### Response

#### Success Response (200)

- **sendTransport** (SendTransport\*) - A pointer to the newly created send transport.

#### Response Example

```json
{
	"sendTransport": "<pointer_to_send_transport>"
}
```

## device.CreateRecvTransport

### Description

Creates a new WebRTC transport to **receive** media. The transport must be previously created in the mediasoup router via router.createWebRtcTransport().

### Method

`device.CreateRecvTransport`

### Parameters

#### Path Parameters

None

#### Query Parameters

None

#### Request Body

- **listener** (RecvTransport::Listener) - Required - The identifier of the server side transport.
- **id** (const std::string&) - Required - The identifier of the server side transport.
- **iceParameters** (const nlohmann::json& IceParameters) - Required - ICE parameters of the server side transport.
- **iceCandidates** (const nlohmann::json& Array<IceCandidate>) - Required - ICE candidates of the server side transport.
- **dtlsParameters** (const nlohmann::json& DtlsParameters) - Required - DTLS parameters of the server side transport.
- **peerConnectionOptions** (PeerConnection::Options) - Optional - PeerConnection options.
- **appData** (nlohmann::json) - Optional - Custom application data. Default: `{ }`

### Request Example

```cpp
auto* recvTransportListener = new MyRecvTransportListener();

// This will block the current thread until completion.
auto* recvTransport = device.CreateRecvTransport(
  recvTransportListener,
  id,
  iceParameters,
  iceCandidates,
  dtlsParameters);
```

### Response

#### Success Response (200)

- **recvTransport** (RecvTransport\*) - A pointer to the newly created receive transport.

#### Response Example

```json
{
	"recvTransport": "<pointer_to_recv_transport>"
}
```

````

--------------------------------

### Producer

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the Producer class, including its properties, methods, and events.

```APIDOC
## Producer

### Description
Represents a media producer, handling audio or video streams.

### Dictionaries
- **ProducerOptions**
- **ProducerScore**
- **ProducerVideoOrientation**
- **ProducerTraceEventData**

### Enums
- **ProducerType**
- **ProducerTraceEventType**

### Properties
- **id** (string) - The unique identifier for the producer.
- **closed** (boolean) - Indicates if the producer is closed.
- **kind** (MediaKind) - The media kind (audio or video).
- **rtpParameters** (RtpParameters) - The RTP parameters for the producer.
- **type** (ProducerType) - The type of the producer.
- **paused** (boolean) - Indicates if the producer is paused.
- **score** (ProducerScore) - The current score of the producer.
- **appData** (object) - Application-specific data.
- **observer** (ProducerObserver) - An observer for producer events.

### Methods
- **close()** - Closes the producer.
- **getStats()** - Retrieves statistics for the producer.
- **pause()** - Pauses the producer.
- **resume()** - Resumes the producer.
- **enableTraceEvent(types)** - Enables trace events for specified types.
- **send(rtpPacket)** - Sends an RTP packet.

### Events
- **on(“transportclose”, fn())** - Emitted when the associated transport is closed.
- **on(“score”, fn(score))** - Emitted when the producer's score changes.
- **on(“videoorientationchange”, fn(videoOrientation))** - Emitted when the video orientation changes.
- **on(“trace”, fn(trace))** - Emitted when a trace event occurs.
- **on(“listenererror”, fn(eventName, error))** - Emitted when a listener error occurs.

### Observer Events
- **observer.on(“close”, fn())** - Observer event for producer closure.
- **observer.on(“pause”, fn())** - Observer event for producer pausing.
- **observer.on(“resume”, fn())** - Observer event for producer resumption.
- **observer.on(“score”, fn(score))** - Observer event for score changes.
- **observer.on(“videoorientationchange”, fn(videoOrientation))** - Observer event for video orientation changes.
- **observer.on(“trace”, fn(trace))** - Observer event for trace events.
````

---

### DataProducer API

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Details the methods for interacting with a DataProducer, including retrieving its properties, status, and managing its lifecycle.

```APIDOC
## DataProducer Methods

### `dataProducer.GetId()`

Producer identifier.

*   **Description:** Retrieves the unique identifier for the data producer.
*   **Method:** GET
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
*   **Returns:** `const std::string&`

### `dataProducer.GetSctpStreamParameters()`

The SCTP stream parameters.

*   **Description:** Gets the parameters governing the SCTP stream for this data producer.
*   **Method:** GET
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
*   **Returns:** `const SctpStreamParameters`

### `dataProducer.GetReadyState()`

The DataChannel ready state.

*   **Description:** Returns the current ready state of the associated DataChannel.
*   **Method:** GET
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
*   **Returns:** `webrtc::DataChannelInterface::DataState`

### `dataProducer.GetLabel()`

The DataChannel label.

*   **Description:** Retrieves the label assigned to the DataChannel.
*   **Method:** GET
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
*   **Returns:** `std::string`, read only

### `dataProducer.GetProtocol()`

The DataChannel sub-protocol.

*   **Description:** Retrieves the sub-protocol used by the DataChannel.
*   **Method:** GET
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
*   **Returns:** `std::string`, read only

### `dataProducer.GetBufferedAmount()`

The number of bytes of application data (UTF-8 text and binary data) that have been queued using `send()`.

*   **Description:** Returns the amount of data currently buffered for sending.
*   **Method:** GET
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
*   **Returns:** `uint64_t`, read only

### `dataProducer.GetAppData()`

Custom data Object provided by the application in the data producer factory method. The app can modify its content at any time.

*   **Description:** Accesses custom application data associated with the data producer.
*   **Method:** GET
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
*   **Returns:** `const nlohmann::json&`

### `dataProducer.IsClosed()`

Whether the data producer is closed.

*   **Description:** Checks if the data producer has been closed.
*   **Method:** GET
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None
*   **Returns:** `bool`

### `dataProducer.Close()`

Closes the data producer. No more data is transmitted. This method should be called when the server side producer has been closed (and vice-versa).

*   **Description:** Initiates the closure of the data producer.
*   **Method:** POST
*   **Endpoint:** N/A (Method call on an object)
*   **Parameters:** None

### `dataProducer.Send(buffer)`

Sends the given data over the corresponding DataChannel. If the data can't be sent at the SCTP level (due to congestion control), it's buffered at the data channel level, up to a maximum of 16MB. If `Send` is called while this buffer is full, the data channel will be closed abruptly. So, it's important to use `GetBufferedAmount` and `OnBufferedAmountChange` to ensure the data channel is used efficiently but without filling this buffer.

*   **Description:** Transmits data through the DataChannel.
*   **Method:** POST
*   **Endpoint:** N/A (Method call on an object)
*   **Arguments:**
    *   `buffer` (webrtc::DataBuffer&) - Data message to be sent. (Optional)

```

---

### Import Mediasoup Module (ES6 and CommonJS)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Demonstrates how to import the top-level mediasoup module using both ES6 import syntax and CommonJS require. It shows both default imports and destructuring assignments for accessing specific functionalities.

```javascript
// Using ES6 import:
import * as mediasoup from "mediasoup"

// Or using destructuring assignment:
import { types, version, observer, createWorker, getSupportedRtpCapabilities, parseScalabilityMode } from "mediasoup"

// Using CommonJS:
const mediasoup = require("mediasoup")

// Or using destructuring assignment:
const { types, version, observer, createWorker, getSupportedRtpCapabilities, parseScalabilityMode } = require("mediasoup")
```

---

### DataProducer Methods: Getters

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Provides methods to retrieve information about a data producer, including its ID, SCTP stream parameters, data channel ready state, label, protocol, buffered amount, and custom application data.

```cpp
const std::string& id = dataProducer.GetId();
const SctpStreamParameters& sctpParams = dataProducer.GetSctpStreamParameters();
webrtc::DataChannelInterface::DataState readyState = dataProducer.GetReadyState();
std::string label = dataProducer.GetLabel();
std::string protocol = dataProducer.GetProtocol();
uint64_t bufferedAmount = dataProducer.GetBufferedAmount();
const nlohmann::json& appData = dataProducer.GetAppData();
```

---

### Device Class

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Represents a client device for mediasoup.

```APIDOC
## Device Class

### Description
Represents a client device that can connect to a mediasoup router.

### Dictionaries
- **DeviceOptions**: Options for creating a new Device.
- **DeviceSctpCapabilities**: SCTP capabilities of the device.

### Enums
- **BuiltinHandlerName**: Names of built-in handlers.

### Class Functions
- **Device.factory(options)**: Creates a new Device instance.

### Constructor
- **new Device(options)**: Initializes a new Device with the given options.

### Properties
- **handlerName**: (String) - The name of the handler being used.
- **loaded**: (Boolean) - Indicates if the device has been loaded.
- **rtpCapabilities**: (Object) - The RTP capabilities of the device.
- **sctpCapabilities**: (Object) - The SCTP capabilities of the device.
- **observer**: (Object) - An observer for device events.

### Methods
- **device.load({ routerRtpCapabilities, preferLocalCodecsOrder })**: Loads the device with router RTP capabilities.
- **device.canProduce(kind)**: Checks if the device can produce a given media kind.
- **device.createSendTransport(options)**: Creates a new sending transport.
- **device.createRecvTransport(options)**: Creates a new receiving transport.

### Observer Events
- **device.observer.on(“newtransport”, fn(transport))**: Emitted when a new transport is created.
```

---

### mediasoupClient Module

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

The top-level exported module for mediasoup-client. Shows different ways to import the library and its destructured components.

````APIDOC
## mediasoupClient Module

### Description
The top-level exported module for mediasoup-client. Provides access to various functionalities including types, version, Device class, and utility functions.

### Usage

#### ES6 Import
```javascript
import * as mediasoupClient from "mediasoup-client";

// Or using destructuring assignment:
import {
  types,
  version,
  Device,
  detectDevice,
  detectDeviceAsync,
  parseScalabilityMode,
  debug
} from "mediasoup-client";
````

#### CommonJS Import

```javascript
const mediasoupClient = require("mediasoup-client")

// Or using destructuring assignment:
const { types, version, Device, detectDevice, detectDeviceAsync, parseScalabilityMode, debug } = require("mediasoup-client")
```

````

--------------------------------

### PlainTransport Methods and Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the PlainTransport class, including methods like getStats() and connect(), as well as events such as 'tuple', 'rtcptuple', and 'sctpstatechange'.

```APIDOC
## PlainTransport

### Description
Represents a network path for RTP, RTCP, and SCTP transmission.

### Methods

#### plainTransport.getStats()
Returns current RTC statistics of the WebRTC transport.

- **Returns**: Array<PlainTransportStat>

#### plainTransport.connect({ ip, port, rtcpPort, srtpParameters })
Provides the plain transport with the endpoint parameters.

- **Parameters**:
  - `ip` (String): Remote IPv4 or IPv6. Required if `comedia` is not set.
  - `port` (Number): Remote port. Required if `comedia` is not set.
  - `rtcpPort` (Number): Remote RTCP port. Required if `comedia` is not set and RTCP-mux is not enabled.
  - `srtpParameters` (SrtpParameters): SRTP parameters used by the remote endpoint. Required if `enableSrtp` was set.

### Events

#### plainTransport.on("tuple", fn(tuple))
Emitted after the remote RTP origin has been discovered. Only emitted if `comedia` mode was set.

- **Argument `tuple`**: TransportTuple - The updated transport tuple.

#### plainTransport.on("rtcptuple", fn(rtcpTuple))
Emitted after the remote RTCP origin has been discovered. Only emitted if `comedia` mode was set and `rtcpMux` was not.

- **Argument `rtcpTuple`**: TransportTuple - The updated RTCP transport tuple.

#### plainTransport.on("sctpstatechange", fn(sctpState))
Emitted when the transport SCTP state changes.

- **Argument `sctpState`**: SctpState - The new SCTP state.

### Observer Events

#### plainTransport.observer.on("tuple", fn(tuple))
Same as the tuple event.

#### plainTransport.observer.on("rtcptuple", fn(rtcpTuple))
Same as the rtcpTuple event.

#### plainTransport.observer.on("sctpstatechange", fn(sctpState))
Same as the sctpstatechange event.

### Request Example for connect()
```javascript
// Calling connect() on a PlainTransport created with comedia and rtcpMux set.
await plainTransport.connect(
  {
    ip   : '1.2.3.4',
    port : 9998
  });

// Calling connect() on a PlainTransport created with comedia unset and rtcpMux
// also unset.
await plainTransport.connect(
  {
    ip       : '1.2.3.4',
    port     : 9998,
    rtcpPort : 9999
  });

// Calling connect() on a PlainTransport created with comedia set and
// enableSrtp enabled.
await plainTransport.connect(
  {
    srtpParameters :
    {
      cryptoSuite : 'AES_CM_128_HMAC_SHA1_80',
      keyBase64   : 'ZnQ3eWJraDg0d3ZoYzM5cXN1Y2pnaHU5NWxrZTVv'
    }
  });

// Calling connect() on a PlainTransport created with comedia unset, rtcpMux
// set and enableSrtp enabled.
await plainTransport.connect(
  {
    ip             : '1.2.3.4',
    port           : 9998,
    srtpParameters :
    {
      cryptoSuite : 'AEAD_AES_256_GCM',
      keyBase64   : 'YTdjcDBvY2JoMGY5YXNlNDc0eDJsdGgwaWRvNnJsamRrdG16aWVpZHphdHo='
    }
  });
````

````

--------------------------------

### DataConsumer API

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the DataConsumer class, including its properties and options for creating a data consumer.

```APIDOC
## DataConsumer Class

A data consumer represents an endpoint capable of receiving data messages from a mediasoup `Router`. It can use SCTP (DataChannel) or receive messages directly if created on top of a `DirectTransport`.

### Dictionaries

#### `DataConsumerOptions`

Represents the options for creating a data consumer.

| Field | Type | Description | Required | Default |
|---|---|---|---|---|
| `dataProducerId` | String | The id of the data producer to consume. | Yes | |
| `ordered` | Boolean | Just if consuming over SCTP. Whether data messages must be received in order. If `true`, messages are sent reliably. | No | The value in the data producer (if 'sctp') or `true` (if 'direct'). |
| `maxPacketLifeTime` | Number | Just if consuming over SCTP. When `ordered` is `false`, indicates the time (in ms) after which a SCTP packet will stop being retransmitted. | No | The value in the data producer (if 'sctp') or unset (if 'direct'). |
| `maxRetransmits` | Number | Just if consuming over SCTP. When `ordered` is `false`, indicates the maximum number of times a packet will be retransmitted. | No | The value in the data producer (if 'sctp') or unset (if 'direct'). |
| `paused` | Boolean | Whether the data consumer must start in paused mode. | No | `false` |
| `subchannels` | Array<Number> | Subchannels (unsigned 16 bit integers) this data consumer initially subscribes to. | No | |
| `appData` | AppData | Custom application data. | No | `{}` |

*Note: `subchannels` are used when receiving messages from a data producer created on a direct transport that specifies subchannels during `dataProducer.send()`.*

### Enums

#### `DataConsumerType`

Specifies the type of data reception.

| Value | Description |
|---|---|
| `"sctp"` | The endpoint receives messages using the SCTP protocol. |
| `"direct"` | Messages are received directly by the Node.js process over a direct transport. |

### Properties

#### `id`

Data consumer identifier.

- `@type` String, read only

#### `dataProducerId`

The associated data producer identifier.

- `@type` String, read only

#### `closed`

Indicates whether the data consumer is closed.

- `@type` Boolean, read only
````

---

### device.createRecvTransport(options)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Establishes a new WebRTC transport for receiving media. This transport must correspond to a WebRtcTransport previously created on the mediasoup router.

````APIDOC
## device.createRecvTransport(options)

### Description
Creates a new WebRTC transport to **receive** media. The transport must be previously created in the mediasoup router via router.createWebRtcTransport().

### Method
`device.createRecvTransport(options)`

### Parameters
#### Request Body
- **options** (TransportOptions) - Required - WebRTC transport options.

### Request Example
```javascript
const transport = device.createRecvTransport(
  {
    id             : "152f60cd-10ac-443b-8529-6474ecba2e44",
    iceParameters  : { ... },
    iceCandidates  : [ ... ],
    dtlsParameters : { ... },
    sctpParameters : { ... }
  });
````

### Returns

- `Transport`

### Throws

- `InvalidStateError`: if device not loaded
- `TypeError`: if invalid arguments

````

--------------------------------

### Pipe to Router Options

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Options for piping media or data producers to another router, specifying source producers, destination router, and connection details.

```APIDOC
## PipeToRouterOptions

### Fields
- **producerId** (String) - Required: No - Producer id.
- **dataProducerId** (String) - Required: No - Data producer id.
- **router** (Router) - Required: Yes - Destination router to pipe the given producer.
- **keepId** (Boolean) - Required: No, Default: `true` - Whether the `id` of the returned producer or dataProducer should be the same than the `id` of the original producer or dataProducer.
- **listenInfo** (TransportListenInfo) - Required: No - Listening information to connect both routers in the same host. Default: `{ protocol: "udp", ip: "127.0.0.1" }`
- **listenIp** (String) - Required: No, DEPRECATED: Use `listenInfo` instead. Default: “127.0.0.1” - IP to connect both routers in the same host.
- **enableSctp** (Boolean) - Required: No, Default: `true` - Create a SCTP association.
- **numSctpStreams** (NumSctpStreams) - Required: No - SCTP streams number.
- **enableRtx** (Boolean) - Required: No, Default: `false` - Enable RTX and NACK for RTP retransmission. Typically not needed since the link is typically localhost.
- **enableSrtp** (Boolean) - Required: No, Default: `false` - Enable SRTP.

* Only one of `producerId` and `dataProducerId` must be provided.
* SCTP arguments will only apply the first time the underlying transports are created.
````

---

### WebRtcTransport

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the WebRtcTransport class, including its properties, methods, and events.

```APIDOC
## WebRtcTransport

### Description
Provides methods and properties for managing WebRTC transports.

### Properties
- **iceState** (RTCIceTransportState) - The current ICE state.
- **iceSelectedTuple** (RTCIceCandidate) - The currently selected ICE tuple.
- **dtlsParameters** (DtlsParameters) - The DTLS parameters for the transport.
- **dtlsState** (DtlsTransportState) - The current DTLS state.
- **dtlsRemoteCert** (Buffer) - The remote certificate in DTLS.
- **sctpParameters** (SctpParameters) - The SCTP parameters for the transport.
- **sctpState** (SctpState) - The current SCTP state.

### Methods
- **getStats()** - Retrieves statistics for the transport.
- **connect({ dtlsParameters })** - Connects the transport with the given DTLS parameters.
- **restartIce()** - Restarts the ICE process.

### Events
- **on(“icestatechange”, fn(iceState))** - Emitted when the ICE state changes.
- **on(“iceselectedtuplechange”, fn(iceSelectedTuple))** - Emitted when the selected ICE tuple changes.
- **on(“dtlsstatechange”, fn(dtlsState))** - Emitted when the DTLS state changes.
- **on(“sctpstatechange”, fn(sctpState))** - Emitted when the SCTP state changes.

### Observer Events
- **observer.on(“icestatechange”, fn(iceState))** - Observer event for ICE state changes.
- **observer.on(“iceselectedtuplechange”, fn(iceSelectedTuple))** - Observer event for selected ICE tuple changes.
- **observer.on(“dtlsstatechange”, fn(dtlsState))** - Observer event for DTLS state changes.
- **observer.on(“sctpstatechange”, fn(sctpState))** - Observer event for SCTP state changes.
```

---

### RtpHeaderExtension

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Provides information about supported RTP header extensions. Lists URIs and preferred IDs for extensions.

```APIDOC
## RtpHeaderExtension

Provides information relating to supported header extensions. The list of RTP header extensions supported by mediasoup is defined in the supportedRtpCapabilities.ts file.

### Fields

- **`kind`** (MediaKind) - Required - Media kind (“audio” or “video”).
- **`uri`** (String) - Required - The URI of the RTP header extension, as defined in RFC 5285.
- **`preferredId`** (Number) - Required - The preferred numeric identifier that goes in the RTP packet. Must be unique.
- **`preferredEncrypt`** (Boolean) - Optional - If `true`, it is preferred that the value in the header be encrypted as per RFC 6904. Default: `false`.
- **`direction`** (String) - Optional - If “sendrecv”, mediasoup supports sending and receiving this RTP extension. “sendonly” means that mediasoup can send (but not receive) it. “recvonly” means that mediasoup can receive (but not send) it.

* mediasoup does not currently support encrypted RTP header extensions.
* The `direction` field is just present in mediasoup RTP capabilities (retrieved via router.rtpCapabilities or mediasoup.getSupportedRtpCapabilities()). It's ignored if present in endpoints' RTP capabilities.
```

---

### Create Plain Transport for Audio in Mediasoup

Source: https://mediasoup.org/documentation/v3/communication-between-client-and-server

Creates a plain transport in mediasoup for sending audio. It configures the transport to listen on localhost and specifies RTCP muxing and comedia settings. It also retrieves the local RTP and RTCP ports for subsequent use.

```javascript
const audioTransport = await router.createPlainTransport({
	listenIp: "127.0.0.1",
	rtcpMux: false,
	comedia: true,
})

// Read the transport local RTP port.
const audioRtpPort = audioTransport.tuple.localPort
// => 3301

// Read the transport local RTCP port.
const audioRtcpPort = audioTransport.rtcpTuple.localPort
// => 4502
```

---

### RouterRtpCapabilities

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Similar to RtpCapabilities, but used for routers, where codecs are of type RouterRtpCodecCapability.

````APIDOC
## RouterRtpCapabilities

### Description
Same as RtpCapabilities. However `codecs` is not an array of `RtpCodecCapability` but of `RouterRtpCodecCapability` (in which `preferredPayloadType` is not mandatory).

### Method
N/A (Object Definition)

### Endpoint
N/A

### Parameters
#### Body Parameters
- **codecs** (Array<RouterRtpCodecCapability>) - Optional - Supported media and RTX codecs.
- **headerExtensions** (Array<RtpHeaderExtension>) - Optional - Supported RTP header extensions.

### Request Example
```json
{
  "codecs": [
    {
      "mimeType": "video/VP8",
      "clockRate": 90000,
      "parameters": {"profile-level-id": "42e01f"},
      "rtcpFeedback": [
        { "type": "nack" },
        { "type": "nack", "parameter": "pli" }
      ]
    }
  ],
  "headerExtensions": [
    { "uri": "urn:ietf:params:rtp-hdrext:toffset", "id": 4 }
  ]
}
````

### Response

#### Success Response (200)

- **codecs** (Array<RouterRtpCodecCapability>) - Supported media and RTX codecs.
- **headerExtensions** (Array<RtpHeaderExtension>) - Supported RTP header extensions.

### Response Example

```json
{
	"codecs": [
		{
			"mimeType": "video/VP8",
			"payloadType": 100,
			"clockRate": 90000,
			"parameters": { "profile-level-id": "42e01f" },
			"rtcpFeedback": [{ "type": "nack" }, { "type": "nack", "parameter": "pli" }]
		}
	],
	"headerExtensions": [{ "uri": "urn:ietf:params:rtp-hdrext:toffset", "id": 4 }]
}
```

````

--------------------------------

### Enable and Listen to Producer Trace Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This snippet demonstrates how to enable trace events for a producer, specifically for RTP and PLI types, and how to listen for these trace events. It requires the producer object and the EventEmitter interface.

```javascript
await producer.enableTraceEvent([ "rtp", "pli" ]);

producer.on("trace", (trace) =>
{
  // trace.type can be "rtp" or "pli".
});
````

---

### DataProducer Listener Events: OnOpen, OnClose, OnBufferAmountChange

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Abstract listener methods for data producer events. OnOpen is triggered when the DataChannel opens. OnClose is triggered on unknown DataChannel closure. OnBufferAmountChange notifies about changes in the buffered data amount.

```cpp
void MyDataProducerListener::OnOpen(DataProducer* producer) {
	std::cout << "DataChannel opened" << std::endl;
}

void MyDataProducerListener::OnClose(DataProducer* producer) {
	std::cout << "DataChannel closed" << std::endl;
}

void MyDataProducerListener::OnBufferAmountChange(DataProducer* producer, uint64_t sentDataSize) {
	std::cout << "Buffered amount changed: " << sentDataSize << std::endl;
}
```

---

### RtpCapabilities

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Defines the media-level RTP capabilities, including supported codecs and header extensions, for an endpoint or mediasoup.

````APIDOC
## RtpCapabilities

### Description
The RTP capabilities define what mediasoup or an endpoint can receive at media level.

### Method
N/A (Object Definition)

### Endpoint
N/A

### Parameters
#### Body Parameters
- **codecs** (Array<RtpCodecCapability>) - Optional - Supported media and RTX codecs.
- **headerExtensions** (Array<RtpHeaderExtension>) - Optional - Supported RTP header extensions.

### Request Example
```json
{
  "codecs": [
    {
      "mimeType": "audio/opus",
      "clockRate": 48000,
      "channels": 2,
      "parameters": {},
      "rtcpFeedback": []
    }
  ],
  "headerExtensions": [
    { "uri": "urn:ietf:params:rtp-hdrext:ssrc-audio-level", "id": 1 }
  ]
}
````

### Response

#### Success Response (200)

- **codecs** (Array<RtpCodecCapability>) - Supported media and RTX codecs.
- **headerExtensions** (Array<RtpHeaderExtension>) - Supported RTP header extensions.

### Response Example

```json
{
	"codecs": [
		{
			"mimeType": "audio/opus",
			"payloadType": 111,
			"clockRate": 48000,
			"channels": 2,
			"parameters": {},
			"rtcpFeedback": []
		}
	],
	"headerExtensions": [{ "uri": "urn:ietf:params:rtp-hdrext:ssrc-audio-level", "id": 1 }]
}
```

````

--------------------------------

### DataProducer API

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the DataProducer class, including methods for sending data, pausing/resuming, and event handling.

```APIDOC
## DataProducer Methods

### `send(message, ppid, subchannels, requiredSubchannel)`

Sends direct messages from the Node.js process.

#### Parameters

- **`message`** (String|Buffer) - Required - Message to be sent (can be binary by using a Node.js Buffer).
- **`ppid`** (Number) - Optional - Mimics the SCTP Payload Protocol Identifier. In most cases it must not be set. Defaults to 51 (WebRTC String) if `message` is a String and 53 (WebRTC Binary) if it's a Buffer.
- **`subchannels`** (Array<Number>) - Optional - Only data consumers subscribed to at least one of these subchannels (unsigned 16 bit integers) will receive the message.
- **`requiredSubchannel`** (Number) - Optional - Only data consumers subscribed to this subchannel (unsigned 16 bit integer) will receive the message.

*Note: Only available in direct transports created via `router.createDirectTransport()`.*

#### Request Example

```javascript
const stringMessage = "hello";
const binaryMessage = Buffer.from([ 1, 2, 3, 4 ]);

dataProducer.send(stringMessage);
dataProducer.send(binaryMessage);
dataProducer.send("bye", undefined, [ 24 ]);
````

### `pause()`

Pauses the data producer. No messages are sent to its associated data consumers. Triggers a “dataproducerpause” event in all its associated data consumers.

_Note: This is an asynchronous operation._

### `resume()`

Resumes the data producer. Messages are sent again to its associated data consumers. Triggers a “dataproducerresume” event in all its associated data consumers.

_Note: This is an asynchronous operation._

## DataProducer Events

### `transportclose`

Emitted when the transport this data producer belongs to is closed. The producer itself is also closed, and a “dataproducerclose” event is triggered in all its associated consumers.

#### Event Listener Example

```javascript
dataProducer.on("transportclose", () => {
	console.log("transport closed so dataProducer closed")
})
```

### `listenererror`

Emitted when an event listener given by the application throws an error. The exception is silently ignored internally to prevent breaking the internal state.

#### Parameters

- **`eventName`** (String) - The name of the event.
- **`error`** (Error) - The error happening in the application-provided event listener.

## DataProducer Observer Events

### `close`

Emitted when the producer is closed for any reason.

### `pause`

Emitted when the data producer is paused.

### `resume`

Emitted when the data producer is resumed.

````

--------------------------------

### Create PlainTransport with Mediasoup v3

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a new plain transport for media communication. It requires PlainTransportOptions, which include listenInfo (protocol and IP), rtcpMux, and comedia settings. Returns a PlainTransport object.

```typescript
const transport = await router.createPlainTransport(
  {
    listenInfo : { protocol: "udp", ip: "a1:22:aA::08" },
    rtcpMux    : true,
    comedia    : true
  }
);

````

---

### Consumer API

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Defines the structure for Consumer options and describes the properties of a consumer object.

````APIDOC
## Consumer Dictionary: ConsumerOptions

### Description
Defines the options required for creating a consumer, specifying the producer to consume from and media details.

### Method
Not Applicable (Dictionary Definition)

### Endpoint
Not Applicable

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body (Implicit for consume method)
- **id** (String) - Required - The identifier of the server side consumer.
- **producerId** (String) - Required - The identifier of the server side producer being consumed.
- **kind** (MediaKind) - Required - Media kind ("audio" or "video").
- **rtpParameters** (RtpReceiveParameters) - Required - Receive RTP parameters.
- **streamId** (String) - Optional - Stream id. Used to limit the inbound RTP streams that the underlying RTC stack should try to synchronize.
- **onRtpReceiver** (OnRtpReceiverCallback) - Optional - Callback called immediately once a RTCRtpReceiver is created.
- **appData** (Object) - Optional - Custom application data.

### Request Example (for consume method)
```javascript
await transport.consume({
  streamId: `${remotePeerId}-mic-webcam`,
  producerId: 'some-producer-id',
  kind: 'audio',
  rtpParameters: { /* ... RtpReceiveParameters ... */ }
});
````

### Response

None (This defines options for a consume operation)

## Consumer Properties

### Description

Properties available on a consumer object after it has been created.

### Method

Not Applicable (Property Access)

### Endpoint

Not Applicable

### Parameters

None

### Request Example

None

### Response

None

#### consumer.id

Consumer identifier.

> `@type` String, read only

#### consumer.producerId

The associated producer identifier.

> `@type` String, read only

#### consumer.closed

Whether the consumer is closed.

> `@type` Boolean, read only

#### consumer.kind

The media kind ("audio" or "video").

> `@type` MediaKind, read only

````

--------------------------------

### Instantiate Mediasoup Device with Handler

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Creates a new mediasoup Device instance, optionally specifying a handler. When not in a React Native environment, mediasoup-client automatically detects the browser and selects an appropriate handler. This allows forcing a specific handler if needed.

```javascript
const device = new mediasoupClient.Device({ handlerName: "Chrome67" });
````

---

### VP9 Codec Matching Parameters

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Specific parameters for VP9 codec matching, used to determine compatibility between RTP capabilities.

```APIDOC
#### VP9

- **`profile-id`** (Number) - Optional - VP9 coding profile (more info). Supported values are 0 and 2. Default: 0.
```

---

### Consumer Methods: Close, Pause, Resume

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Methods for controlling the lifecycle and state of a consumer. Close terminates the consumer, Pause disables transmission by disabling the remote track, and Resume re-enables transmission by enabling the remote track.

```cpp
consumer.Close();
consumer.Pause();
consumer.Resume();
```

---

### mediasoupClient.detectDeviceAsync() Function

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Asynchronously detects the appropriate WebRTC handler for the current browser/device.

````APIDOC
## mediasoupClient.detectDeviceAsync(userAgent) Function

### Description
Performs current browser/device detection and returns the corresponding mediasoup-client WebRTC handler name (or nothing if the browser/device is not supported). This is the recommended asynchronous alternative to `detectDevice()`.

> `@async`
> `@returns` BuiltinHandlerName | undefined

### Parameters

| Argument | Type | Description | Required | Default |
|---|---|---|---|---|
| `userAgent` | String | Optional browser User-Agent string. If not given, `navigator.userAgent` will be used (in case of browser). | No | |
| `userAgentData` | NavigatorUAData | Optional data obtained via `navigator.userAgentData()` in browsers supporting it. | No |   |

### Usage
```javascript
const handlerName = await mediasoupClient.detectDeviceAsync();

if (handlerName) {
  console.log("detected handler: %s", handlerName);
} else {
  console.warn("no suitable handler found for current browser/device");
}
````

Compared to `detectDevice()`, `detectDeviceAsync()` may allow for more accurate checks in the future due to its asynchronous nature.

````

--------------------------------

### Handle SendTransport::OnProduce Event (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

This C++ code snippet demonstrates how to implement the OnProduce event handler for the SendTransportListener. It shows how to gather transport details, package them into a JSON request, send it to a signaling server, and then fulfill a promise with the producer ID received from the server. This is essential for the client to inform the server about a new media producer.

```cpp
std::future<std::string> MySendTransportListener::OnProduce(
  mediasoupclient::Transport* transport,
  const std::string& kind,
  json rtpParameters,
  const json& appData
)
{
	std::promise<std::string> promise;

	json body =
	{
		{ "transportId",   transport->GetId() },
		{ "kind",          kind               },
		{ "rtpParameters", rtpParameters      },
		{ "appData",       appData            }
	};

	json response = mySignaling.send("transport-produce", body);

  // [...] Let's assume code execution continues once we get a success response
  // from the server.

  // Read the id in the response.
	auto idIt = response.find("id");
	if (idIt == response.end() || !idIt->is_string())
  {
		promise.set_exception(
      std::make_exception_ptr("'id' missing/invalid in response"));
  }

  // Fulfil the promise with the id in the response and return its future.
	promise.set_value(idIt->get<std::string>());

	return promise.get_future();
}

````

---

### Consumer Methods: KeyFrame Request and Trace Event Enablement

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Details methods for requesting a key frame and enabling trace events on a consumer. Requesting a key frame is important for video synchronization, while trace events are useful for debugging and monitoring RTP, PLI, and FIR events.

```javascript
await consumer.requestKeyFrame()

await consumer.enableTraceEvent(["rtp", "pli", "fir"])

consumer.on("trace", (trace) => {
	// trace.type can be "rtp" or "pli" or "fir".
	console.log(trace)
})
```

---

### PipeTransport Options Configuration

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Defines the configuration options for creating a PipeTransport. Key options include listening information, SCTP and SRTP enablement, and custom application data. Note that 'listenIp' and 'port' are deprecated in favor of 'listenInfo'.

```typescript
interface PipeTransportOptions {
	listenInfo?: TransportListenInfo
	listenIp?: TransportListenIp | string
	port?: number
	enableSctp?: boolean
	numSctpStreams?: NumSctpStreams
	maxSctpMessageSize?: number
	sctpSendBufferSize?: number
	enableRtx?: boolean
	enableSrtp?: boolean
	appData?: AppData
}
```

---

### Worker: Create WebRtcServer (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a WebRtcServer instance managed by the worker. This server handles incoming WebRTC connections. It takes an optional `WebRtcServerAppData` for custom data and returns a `WebRtcServer` object.

```javascript
import * as mediasoup from 'mediasoup';

interface MyWebRtcServerAppData {
  serverName: string;
}

async function createWebRtcServerForWorker(worker) {
  try {
    const webRtcServerOptions = {
      // WebRtcServer specific options can go here
    };
    const webRtcServer = await worker.createWebRtcServer<MyWebRtcServerAppData>(webRtcServerOptions);
    webRtcServer.appData = { serverName: 'default-server' };
    console.log(`WebRtcServer created for worker ${worker.pid} with ID: ${webRtcServer.id}`);
    return webRtcServer;
  } catch (error) {
    console.error(`Failed to create WebRtcServer for worker ${worker.pid}:`, error);
    throw error;
  }
}

// Example usage (assuming 'myWorker' is an existing Worker instance):
// createWebRtcServerForWorker(myWorker).then(webRtcServer => {
//   // WebRtcServer is ready
// });
```

---

### DataProducer Listener Interface: Data Events (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Defines the `DataProducerListener` interface for handling events related to a `DataProducer`. Events include `OnOpen`, `OnClose`, `OnBufferAmountChange`, and `OnTransportClose`, providing comprehensive control over data transmission.

```cpp
#include "libmediasoupclient/DataProducer.hpp"

class MyDataProducerListener : public mediasoupclient::DataProducer::Listener {
public:
    void OnOpen(mediasoupclient::DataProducer* producer) override {
        // Handle producer opening
    }

    void OnClose(mediasoupclient::DataProducer* producer) override {
        // Handle producer closing
    }

    void OnBufferAmountChange(mediasoupclient::DataProducer* producer, uint64_t sentDataSize) override {
        // Handle changes in the buffer amount
    }

    void OnTransportClose(mediasoupclient::DataProducer* producer) override {
        // Handle transport closing event
    }
};

```

---

### K-SVC Consumer with 4 Spatial and 5 Temporal Layers

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Presents the `encodings` configuration for a mediasoup consumer receiving a K-SVC stream. The `scalabilityMode` reflects the producer's settings, including the '\_KEY' suffix for K-SVC.

```javascript
encodings: [{ ssrc: 222220, scalabilityMode: "L4T5_KEY" }]
```

---

### AudioLevelObserver Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Details the events emitted by the AudioLevelObserver, including volume levels and silence detection.

```APIDOC
## AudioLevelObserver API

### Description
Provides event handling for audio level monitoring.

### Events
- `volumes`: Emitted with volume information for participants.
- `silence`: Emitted when silence is detected.

### Observer Events
- `volumes`: Emitted with volume information for participants.
- `silence`: Emitted when silence is detected.
```

---

### Handle New Media Producer Creation (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Listens for the 'produce' event on a Mediasoup transport. This event signals the need to create a new server-side producer before the produce() method completes. The code shows how to send producer parameters (kind, RTP parameters, appData) to the server via a signaling channel and then use the returned producer ID to complete the client-side producer creation.

```javascript
transport.on("produce", async (parameters, callback, errback) => {
	// Signal parameters to the server side transport and retrieve the id of
	// the server side new producer.
	try {
		const data = await mySignaling.send("transport-produce", {
			transportId: transport.id,
			kind: parameters.kind,
			rtpParameters: parameters.rtpParameters,
			appData: parameters.appData,
		})

		// Let's assume the server included the created producer id in the response
		// data object.
		const { id } = data

		// Tell the transport that parameters were transmitted and provide it with the
		// server side producer's id.
		callback({ id })
	} catch (error) {
		// Tell the transport that something was wrong.
		errback(error)
	}
})
```

---

### Consumer Listener Events

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Defines the abstract listener class for consumers and the events it can handle, particularly focusing on transport closure.

````APIDOC
## Consumer::Listener

This is an abstract class which must be implemented and used according to the API.

### Events

#### `ConsumerListener::OnTransportClose(consumer)`

Executed when the transport this consumer belongs to is closed for whatever reason. The consumer itself is also closed.

*   **Description:** Callback function invoked when the associated transport is closed.
*   **Arguments:**
    *   `consumer` (Consumer*) - The consumer instance executing this method. (Required)

*   **Example Implementation:**
    ```cpp
    void MyConsumerListener::OnTransportClose(mediasoupclient::Consumer* consumer)
    {
        std::cout << "transport closed" << std::endl;
    }
    ```
````

---

### Handle DataConsumer Message Reception (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

This C++ code demonstrates the OnMessage callback for a DataConsumerListener, which is triggered when a message is received over the DataChannel. It checks the DataConsumer's label to identify 'chat' messages and prints the received message to standard output. The function takes the DataConsumer pointer and the received webrtc::DataBuffer as arguments.

```cpp
void MyConsumerListener::OnMessage(DataConsumer* dataConsumer, const webrtc::DataBuffer& buffer)
{
	if (dataConsumer->GetLabel() == "chat")
	{
		std::string message = std::string(buffer.data.data<char>(), buffer.data.size());
		std::cout << "received chat message: " << message << std::endl;
	}
}
```

---

### Create Mediasoup Worker (Async)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Shows the asynchronous function to create a new mediasoup worker. It accepts optional settings like logLevel, dtlsCertificateFile, dtlsPrivateKeyFile, and custom appData. The function returns a Promise that resolves to a Worker object.

```javascript
const worker = await mediasoup.createWorker({
	logLevel: "warn",
	dtlsCertificateFile: "/home/foo/dtls-cert.pem",
	dtlsPrivateKeyFile: "/home/foo/dtls-key.pem",
	appData: { foo: 123 },
})
```

---

### router.createWebRtcTransport()

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a new WebRTC transport on the router with specified options.

````APIDOC
### router.createWebRtcTransport<WebRtcTransportAppData>(options)

Creates a new WebRTC transport.

#### Arguments
- **options** (WebRtcTransportOptions) - Required: Yes - WebRTC transport options.
- **WebRtcTransportAppData** (AppData) - Required: No - Custom `appData` definition.

> `@async`
> `@returns` WebRtcTransport

#### Example 1
```javascript
const transport = await router.createWebRtcTransport(
  {
    webRtcServer : webRtcServer,
    enableUdp    : true,
    enableTcp    : false
  });
````

#### Example 2

```javascript
const transport = await router.createWebRtcTransport({
	listenInfos: [
		{
			protocol: "udp",
			ip: "192.168.0.111",
			announcedAddress: "88.12.10.41",
		},
	],
})
```

````

--------------------------------

### Monitor Mediasoup Events with Observer API (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

This JavaScript code demonstrates how to use the mediasoup-client observer API to listen for various events, including the creation and closure of transports, producers, consumers, and data producers/consumers. It utilizes the `on` method of the EventEmitter to register callbacks for these events.

```javascript
const mediasoupClient = require("mediasoup-client");

const mediasoupClientDevice = new mediasoupClientTypes.Device();

mediasoupClientDevice.observer.on("newtransport", (transport) =>
{
  console.log(
    "new transport created [transport.id:%s]", transport.id);

  transport.observer.on("close", () =>
  {
    console.log("transport closed [transport.id:%s]", transport.id);
  });

  transport.observer.on("newproducer", (producer) =>
  {
    console.log(
      "new producer created [transport.id:%s, producer.id:%s]",
      transport.id, producer.id);

    producer.observer.on("close", () =>
    {
      console.log("producer closed [producer.id:%s]", producer.id);
    });
  });

  transport.observer.on("newconsumer", (consumer) =>
  {
    console.log(
      "new consumer created [transport.id:%s, consumer.id:%s]",
      transport.id, consumer.id);

    consumer.observer.on("close", () =>
    {
      console.log("consumer closed [consumer.id:%s]", consumer.id);
    });
  });

  transport.observer.on("newdataproducer", (dataProducer) =>
  {
    console.log(
      "new data producer created [transport.id:%s, dataProducer.id:%s]",
      transport.id, dataProducer.id);

    dataProducer.observer.on("close", () =>
    {
      console.log("data producer closed [dataProducer.id:%s]", dataProducer.id);
    });
  });

  transport.observer.on("newdataconsumer", (dataConsumer) =>
  {
    console.log(
      "new data consumer created [transport.id:%s, dataConsumer.id:%s]",
      transport.id, dataConsumer.id);

    dataConsumer.observer.on("close", () =>
    {
      console.log("data consumer closed [dataConsumer.id:%s]", dataConsumer.id);
    });
  });
});

````

---

### Create ActiveSpeakerObserver with Mediasoup v3

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a new active speaker observer to detect the active speaker in a conference. It takes optional ActiveSpeakerObserverOptions, such as the observation interval. Returns an ActiveSpeakerObserver object.

```typescript
const activeSpeakerObserver = await router.createActiveSpeakerObserver({
	interval: 500,
})
```

---

### Router: Create WebRtcTransport (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a WebRtcTransport instance within a Router. WebRTC transports are used for establishing peer-to-peer connections using WebRTC protocols. This method takes `WebRtcTransportOptions` and returns a `WebRtcTransport` object.

```javascript
import * as mediasoup from "mediasoup"

async function createWebRtcTransportForRouter(router) {
	try {
		const webRtcTransportOptions = {
			listenIps: [{ ip: "127.0.0.1", announcedIp: "127.0.0.1" }],
			enableIce: true,
			enableSctp: true,
		}
		const transport = (await router.createWebRtcTransport) < any > webRtcTransportOptions
		console.log(`WebRtcTransport created for router ${router.id} with ID: ${transport.id}`)
		return transport
	} catch (error) {
		console.error(`Failed to create WebRtcTransport for router ${router.id}:`, error)
		throw error
	}
}

// Example usage (assuming 'myRouter' is an existing Router instance):
// createWebRtcTransportForRouter(myRouter).then(transport => {
//   // Transport is ready, get its ICE parameters etc.
//   console.log('ICE Parameters:', transport.iceParameters);
// });
```

---

### Producer Observer Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This section details the events emitted by the Producer's observer, mirroring the main producer events but accessed through the `producer.observer` interface.

```APIDOC
## Producer Observer Events

See the Observer API section below.

### `producer.observer.on('close', fn())`

Emitted when the producer is closed.

### `producer.observer.on('pause', fn())`

Emitted when the producer is paused.

### `producer.observer.on('resume', fn())`

Emitted when the producer is resumed.

### `producer.observer.on('score', fn(score))`

Same as the `score` event emitted directly by the producer.

### `producer.observer.on('videoorientationchange', fn(videoOrientation))`

Same as the `videoorientationchange` event emitted directly by the producer.

### `producer.observer.on('trace', fn(trace))`

Same as the `trace` event emitted directly by the producer.
```

---

### Observe New RTP Observer Creation

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'newrtpobserver' event emitted by a router's observer. This event indicates that a new RTP observer has been successfully created.

```javascript
router.observer.on("newrtpobserver", (rtpObserver) => {
	console.log("new RTP observer created [id:%s]", rtpObserver.id)
})
```

---

### Handle Consumer Observer Events (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This snippet illustrates how to use the observer pattern for a mediasoup consumer to monitor its lifecycle events. It covers 'close', 'pause', 'resume', 'score', 'layerschange', and 'trace' events, allowing applications to react to changes in the consumer's state or associated producer's status.

```javascript
consumer.observer.on("close", () => {
	console.log("Consumer closed")
})

consumer.observer.on("pause", () => {
	console.log("Consumer paused")
})

consumer.observer.on("resume", () => {
	console.log("Consumer resumed")
})

consumer.observer.on("score", (score) => {
	console.log("Consumer score updated:", score)
})

consumer.observer.on("layerschange", (layers) => {
	console.log("Consumer layers changed:", layers)
})

consumer.observer.on("trace", (trace) => {
	console.log("Consumer trace event:", trace)
})
```

---

### device.createSendTransport(options)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Establishes a new WebRTC transport for sending media. This transport must correspond to a WebRtcTransport previously created on the mediasoup router.

````APIDOC
## device.createSendTransport(options)

### Description
Creates a new WebRTC transport to **send** media. The transport must be previously created in the mediasoup router via router.createWebRtcTransport().

### Method
`device.createSendTransport(options)`

### Parameters
#### Request Body
- **options** (TransportOptions) - Required - WebRTC transport options.

### Request Example
```javascript
const transport = device.createSendTransport(
  {
    id             : "0b38d662-ea00-4c70-9ae3-b675d6a89e09",
    iceParameters  : { ... },
    iceCandidates  : [ ... ],
    dtlsParameters : { ... },
    sctpParameters : { ... }
  });
````

### Returns

- `Transport`

### Throws

- `InvalidStateError`: if device not loaded
- `TypeError`: if invalid arguments

````

--------------------------------

### Create WebRTC Transport with Listen Info

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a new WebRTC transport specifying listening information, including protocol, IP address, and announced address. This is an asynchronous operation returning a WebRtcTransport instance.

```javascript
const transport = await router.createWebRtcTransport(
  {
    listenInfos :
    [
      {
        protocol         : "udp",
        ip               : "192.168.0.111",
        announcedAddress : "88.12.10.41"
      }
    ]
  });
````

---

### Transport Class: Connection Management (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

The Transport class provides methods for managing the state and properties of a media transport. It allows retrieving connection state, statistics, application data, and control over closing and restarting ICE. It requires ICE parameters for restarting.

```cpp
#include "libmediasoupclient/Transport.hpp"

// Assuming 'transport' is an instance of mediasoupclient::Transport

// Get transport ID
// const std::string& id = transport.GetId();

// Get connection state
// const std::string& state = transport.GetConnectionState();

// Get transport stats
// auto stats = transport.GetStats();

// Check if closed
// bool closed = transport.IsClosed();

// Close the transport
// transport.Close();

// Restart ICE
// transport.RestartIce(newIceParameters);

```

---

### DataConsumer Listener Interface: Data Events (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Defines the `DataConsumerListener` interface for handling events related to a `DataConsumer`. Key events include connection status changes (`OnConnecting`, `OnOpen`, `OnClosing`, `OnClose`), transport closure (`OnTransportClose`), and receiving data messages (`OnMessage`).

```cpp
#include "libmediasoupclient/DataConsumer.hpp"

class MyDataConsumerListener : public mediasoupclient::DataConsumer::Listener {
public:
    void OnConnecting(mediasoupclient::DataConsumer* consumer) override {
        // Handle consumer connecting
    }

    void OnOpen(mediasoupclient::DataProducer* producer) override {
        // Handle consumer opening (Note: This signature seems inconsistent in the provided text, assuming it refers to the data producer opening)
    }

    void OnClosing(mediasoupclient::DataConsumer* consumer) override {
        // Handle consumer closing
    }

    void OnClose(mediasoupclient::DataProducer* producer) override {
        // Handle consumer closing (Note: This signature seems inconsistent in the provided text, assuming it refers to the data producer closing)
    }

    void OnTransportClose(mediasoupclient::DataConsumer* consumer) override {
        // Handle transport closing event
    }

    void OnMessage(mediasoupclient::DataConsumer* consumer, const data_t* buffer) override {
        // Handle incoming data message
    }
};

```

---

### Check Media Production Capability - C++

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Determines if the loaded device is capable of producing media of a specified kind (audio or video). This capability depends on the codecs configured in the mediasoup router and the underlying WebRTC implementation.

```cpp
if (device.CanProduce("video"))
{
  // Produce video.
}
```

---

### PipeTransport Methods

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Methods available on a PipeTransport instance.

````APIDOC
## PipeTransport Methods

### Description
Methods available on a PipeTransport instance.

### Methods
#### pipeTransport.getStats()
- Description: Returns current RTC statistics of the pipe transport.
- Type: Async
- Returns: Array<PipeTransportStat>
- See also: RTC Statistics section.

#### pipeTransport.connect({ ip, port })
- Description: Provides the pipe RTP transport with the remote parameters.
- Type: Async
- Arguments:
  - **ip** (String) - Required - Remote IPv4 or IPv6.
  - **port** (Number) - Required - Remote port.
  - **srtpParameters** (SrtpParameters) - Optional - SRTP parameters used by the paired `pipeTransport` to encrypt its RTP and RTCP.

### Request Example
```javascript
await pipeTransport.connect(
  {
    ip             : '1.2.3.4',
    port           : 9999,
    srtpParameters :
    {
      cryptoSuite : 'AEAD_AES_256_GCM',
      keyBase64   : 'YTdjcDBvY2JoMGY5YXNlNDc0eDJsdGgwaWRvNnJsamRrdG16aWVpZHphdHo='
    }
  });
````

````

--------------------------------

###  mediasoup:WARN:* mediasoup:ERROR:*

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This configuration enables logging for all RTCP warnings and any type of error messages from mediasoup.

```APIDOC
## Set Debug Logging for RTCP Warnings and Errors

### Description
This command enables detailed logging for RTCP warnings and all error messages emitted by mediasoup.

### Method
Environment Variable and Node.js Execution

### Command Example
```bash
$ DEBUG="mediasoup:WARN:* mediasoup:ERROR:*" node myapp.js
````

### Code Example

```javascript
const mediasoup = require("mediasoup")

const worker = await mediasoup.createWorker({
	logLevel: "warn",
	logTags: ["rtcp"],
})
```

````

--------------------------------

### Include nlohmann::json for Mediasoup JSON Handling

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Shows how to include the nlohmann::json library header for use with libmediasoupclient. This is necessary for constructing and passing JSON objects to the mediasoup API. The library is a dependency for JSON manipulation within the application.

```cpp
#include "nlohmann/json.hpp"

````

---

### Producer Dictionaries

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Data structures used for configuring and describing Producers, including options, scores, video orientation, and trace events.

```APIDOC
## Producer

A producer represents an audio or video source being injected into a mediasoup router. It's created on top of a transport that defines how the media packets are carried.

### Dictionaries

#### `ProducerOptions`

Configuration options for creating a Producer.

| Field             | Type          | Description                                                                                                             | Required | Default |
|-------------------|---------------|-------------------------------------------------------------------------------------------------------------------------|----------|---------|
| `id`              | String        | Useful for `PipeTransport` usages when connecting mediasoup instances running in different hosts. Not needed otherwise. | No       | (auto-generated UUID v4) |
| `kind`            | MediaKind     | Media kind ("audio" or "video").                                                                                      | Yes      |         |
| `rtpParameters`   | RtpSendParameters | RTP parameters defining what the endpoint is sending.                                                                   | Yes      |         |
| `paused`          | Boolean       | Whether the producer must start in paused mode.                                                                         | No       | `false` |
| `keyFrameRequestDelay` | Number        | Just for video. Time (in ms) before asking the sender for a new key frame after having asked a previous one. If 0 there is no delay. | No       | 0       |
| `appData`         | AppData       | Custom application data.                                                                                                | No       | `{ }`   |

Check the [RTP Parameters and Capabilities section](link-to-rtp-params) for more details.

#### `ProducerScore`

Represents the quality score of a producer's RTP stream.

| Field     | Type   | Description                                                                          | Required | Default |
|-----------|--------|--------------------------------------------------------------------------------------|----------|---------|
| `encodingIdx` | Number | Index of the RTP stream in the `rtpParameters.encodings` array of the producer.      | Yes      |         |
| `ssrc`    | Number | RTP stream SSRC.                                                                     | Yes      |         |
| `rid`     | String | RTP stream RID value.                                                                | No       |         |
| `score`   | Number | RTP stream score (from 0 to 10) representing the transmission quality.                 | Yes      |         |

#### `ProducerVideoOrientation`

Describes the orientation of a video source.

As documented in [WebRTC Video Processing and Codec Requirements](link-to-webrtc-video-processing).

| Field  | Type    | Description                         | Required | Default |
|--------|---------|-------------------------------------|----------|---------|
| `camera` | Boolean | Whether the source is a video camera. | Yes      |         |
| `flip` | Boolean | Whether the video source is flipped.  | Yes      |         |
| `rotation` | Number | Rotation degrees (0, 90, 180 or 270). | Yes      |         |

#### `ProducerTraceEventData`

Data associated with producer trace events.

| Field     | Type                     | Description                                                     | Required | Default |
|-----------|--------------------------|-----------------------------------------------------------------|----------|---------|
| `type`    | ProducerTraceEventType   | Trace event type.                                               | Yes      |         |
| `timestamp` | Number                   | Event timestamp.                                                | Yes      |         |
| `direction` | String                   | "in" (incoming direction) or "out" (outgoing direction).    | Yes      |         |
| `info`    | Object                   | Per type specific information.                                  | Yes      |         |

See also "trace" Event in the [Debugging section](link-to-debugging).

```

---

### Consumer Events and Observer Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This section details the events emitted by a consumer and its observer, including score, layerschange, trace, rtp, listenererror, close, pause, and resume.

```APIDOC
## Consumer Events and Observer Events

### Description
Details the events emitted by a consumer and its observer.

### Consumer Events
- `score`: Emitted with the score of the consumer.
- `layerschange`: Emitted when the consumer's layers change.
- `trace`: Emitted with trace information.
- `rtp`: Emitted with an RTP packet.
- `listenererror`: Emitted with an event name and error.

### Observer Events
- `close`: Emitted when the observer is closed.
- `pause`: Emitted when the observer is paused.
- `resume`: Emitted when the observer is resumed.
- `score`: Emitted with the score of the consumer.
- `layerschange`: Emitted when the consumer's layers change.
- `trace`: Emitted with trace information.
```

---

### Consumer Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This section details the events emitted by a consumer instance in Mediasoup v3, providing insights into its state and the state of its associated producer and transport.

````APIDOC
## Consumer Events

### `consumer.on("transportclose", fn())`

**Description**: Emitted when the transport associated with this consumer is closed. The consumer instance is also closed automatically.

**Example**:
```javascript
consumer.on("transportclose", () => {
  console.log("transport closed so consumer closed");
});
````

### `consumer.on("producerclose", fn())`

**Description**: Emitted when the producer associated with this consumer is closed. The consumer instance is also closed automatically.

**Example**:

```javascript
consumer.on("producerclose", () => {
	console.log("associated producer closed so consumer closed")
})
```

### `consumer.on("producerpause", fn())`

**Description**: Emitted when the associated producer is paused.

### `consumer.on("producerresume", fn())`

**Description**: Emitted when the associated producer is resumed.

### `consumer.on("score", fn(score))`

**Description**: Emitted when the consumer's score changes.

**Parameters**:

- **`score`** (ConsumerScore) - The RTP stream score.

### `consumer.on("layerschange", fn(layers))`

**Description**: Emitted when the spatial/temporal layers being sent to the endpoint change. This event is relevant for simulcast or SVC consumers.

**Parameters**:

- **`layers`** (ConsumerLayers | undefined) - Current spatial and temporal layers, or `undefined` if no current layers are set.

**Context**: This event is triggered under several conditions in SVC or simulcast consumers (assuming the endpoint supports BWE via REMB or Transport-CC):

- When the consumer or its associated producer is paused.
- When all RTP streams from the associated producer become inactive (no RTP received for a period).
- When the available bitrate (BWE) causes the consumer to upgrade or downgrade spatial and/or temporal layers.
- When there is insufficient available bitrate for the consumer, even for the lowest layers, resulting in the event being emitted with `null` as the argument.

**Detection**: The Node.js application can detect if a consumer has been deactivated due to insufficient bandwidth by checking if both `consumer.paused` and `consumer.producerPaused` are falsy after this event has been emitted with `null`.

### `consumer.on("trace", fn(trace))`

**Description**: Emitted for trace event data, as configured by the `enableTraceEvent()` method.

**Parameters**:

- **`trace`** (ConsumerTraceEventData) - The trace data.

**Example**:

```javascript
consumer.on("trace", (trace) => {
	console.log(trace)
})
```

````

--------------------------------

### RecvTransport Methods: Media and Data Consumption (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

The `RecvTransport` class provides methods for consuming media and data. `Consume` is used for receiving media tracks, requiring a listener, ID, producer ID, media kind, RTP parameters, and app data. `ConsumeData` is for receiving data, requiring similar parameters.

```cpp
#include "libmediasoupclient/RecvTransport.hpp"

// Assuming 'recvTransport' is an instance of mediasoupclient::RecvTransport
// Assuming 'listener' is an object implementing RecvTransportListener
// Assuming 'rtpParameters' are received for a specific producer

// Consume a media track
// mediasoupclient::Consumer* consumer = recvTransport.Consume(
//     listener, "consumer-id", "producer-id", "video", rtpParameters, appData
// );

// Consume data
// mediasoupclient::DataConsumer* dataConsumer = recvTransport.ConsumeData(
//     listener, "data-consumer-id", "data-producer-id", "chat", "text/plain", appData
// );

````

---

### Check if Device Can Produce Media

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Determines if the device is capable of producing media of a specified kind (e.g., 'video' or 'audio'). This capability depends on the media codecs enabled in the mediasoup router and the device's media capabilities. It requires a media kind string and returns a boolean. The device must be loaded before calling this method; otherwise, it throws InvalidStateError.

```javascript
if (device.canProduce("video")) {
	// Do getUserMedia() and produce video.
}
```

---

### Mediasoup Transport Method: restartIce()

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Shows how to use the asynchronous `restartIce()` method to instruct the underlying peer connection to restart ICE with new remote ICE parameters. This is typically called after the server-side transport has also restarted ICE.

```javascript
async function restartTransportIce(transport, newIceParameters) {
	await transport.restartIce({ iceParameters: newIceParameters })
}
```

---

### device.canProduce(kind)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Checks if the device is capable of producing media of a specified kind. This capability depends on the media codecs configured in the mediasoup router and the media capabilities of the client's browser or device.

````APIDOC
## device.canProduce(kind)

### Description
Whether the device can produce media of the given kind. This depends on the media codecs enabled in the mediasoup router and the media capabilities of the browser/device.

### Method
`device.canProduce(kind)`

### Parameters
#### Query Parameters
- **kind** (String) - Required - MediaKind

### Request Example
```javascript
if (device.canProduce("video"))
{
  // Do getUserMedia() and produce video.
}
````

### Returns

- `Boolean`

### Throws

- `InvalidStateError`: if device not loaded
- `TypeError`: if invalid kind

````

--------------------------------

### FFmpeg Command for RTP Stream Injection

Source: https://mediasoup.org/documentation/v3/communication-between-client-and-server

This FFmpeg command streams an MP4 file (`/home/foo/party.mp4`) as two separate RTP streams (audio and video) to specified localhost IPs and ports. It configures codecs, payload types, and SSRCs to match mediasoup transport producers. The output is split using the `tee` muxer.

```bash
ffmpeg \
  -re \
  -v info \
  -stream_loop -1 \
  -i /home/foo/party.mp4 \
  -map 0:a:0 \
  -acodec libopus -ab 128k -ac 2 -ar 48000 \
  -map 0:v:0 \
  -pix_fmt yuv420p -c:v libvpx -b:v 1000k -deadline realtime -cpu-used 4 \
  -f tee \
  "[select=a:f=rtp:ssrc=11111111:payload_type=101]rtp://127.0.0.1:3301?rtcpport=4502|[select=v:f=rtp:ssrc=22222222:payload_type=102]rtp://127.0.0.1:3501?rtcpport=2989"

````

---

### PlainTransport

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the PlainTransport class, including its properties, methods, and events.

```APIDOC
## PlainTransport

### Description
Provides methods and properties for managing plain transports.

### Dictionaries
- **PlainTransportOptions**

### Properties
- **tuple** (TransportTuple) - The transport tuple.
- **rtcpTuple** (TransportTuple) - The RTCP transport tuple.
- **sctpParameters** (SctpParameters) - The SCTP parameters for the transport.
- **sctpState** (SctpState) - The current SCTP state.
- **srtpParameters** (SrtpParameters) - The SRTP parameters for the transport.

### Methods
- **getStats()** - Retrieves statistics for the transport.
- **connect({ ip, port, rtcpPort, srtpParameters })** - Connects the transport with the given options.

### Events
- **on(“tuple”, fn(tuple))** - Emitted when the transport tuple is set.
- **on(“rtcptuple”, fn(rtcpTuple))** - Emitted when the RTCP transport tuple is set.
- **on(“sctpstatechange”, fn(sctpState))** - Emitted when the SCTP state changes.

### Observer Events
- **observer.on(“tuple”, fn(tuple))** - Observer event for the transport tuple.
- **observer.on(“rtcptuple”, fn(rtcpTuple))** - Observer event for the RTCP transport tuple.
- **observer.on(“sctpstatechange”, fn(sctpState))** - Observer event for SCTP state changes.
```

---

### Observe New Router Creation (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Subscribes to the 'newrouter' event on the worker's observer. This event fires whenever a new router is successfully created, providing the router instance as an argument.

```javascript
worker.observer.on("newrouter", (router) => {
	console.log("new router created [id:%s]", router.id)
})
```

---

### SendTransport Methods: Media and Data Production (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

The `SendTransport` class extends `Transport` with methods for producing media tracks and data. `Produce` is used for media, requiring a listener, track, encodings, codec options, and app data. `ProduceData` is for non-media data, requiring a listener, label, protocol, and other data transfer options.

```cpp
#include "libmediasoupclient/SendTransport.hpp"

// Assuming 'sendTransport' is an instance of mediasoupclient::SendTransport
// Assuming 'listener' is an object implementing SendTransportListener
// Assuming 'track' is a media track and 'encodings'/'codecOptions' are configured

// Produce a media track
// mediasoupclient::Producer* producer = sendTransport.Produce(
//     listener, track, encodings, codecOptions, appData
// );

// Produce data
// mediasoupclient::DataProducer* dataProducer = sendTransport.ProduceData(
//     listener, "chat", "text/plain", true, 0, -1, appData
// );

```

---

### Import mediasoup Module in Node.js

Source: https://mediasoup.org/documentation/v3/mediasoup/design

Demonstrates how to import the mediasoup module in a Node.js application. This is the initial step to integrate mediasoup into your project.

```javascript
const mediasoup = require("mediasoup")
```

---

### Consumer Methods: Pause, Resume, Close, and Statistics

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Provides an overview of core consumer management methods including pausing, resuming, closing the consumer, and retrieving its real-time statistics. These are fundamental operations for controlling media flow and monitoring consumer health.

```javascript
await consumer.pause()

await consumer.resume()

consumer.close()

const stats = await consumer.getStats()
```

---

### RtpObserver

Source: https://mediasoup.org/documentation/v3/mediasoup/api

An abstract class for RTP observers that inspect media received by a set of selected producers.

```APIDOC
## RtpObserver

> `@abstract`

An RTP observer inspects the media received by a set of selected producers. Mediasoup implements the following RTP observer classes: ActiveSpeakerObserver, AudioLevelObserver.

### Dictionaries
#### RtpObserverAddRemoveProducerOptions
- **producerId** (String) - Required - Id of the producer to add or remove.

### Enums
#### RtpObserverType
- **"activespeaker"** - The type of ActiveSpeakerObserver.
- **"audiolevel"** - The type of AudioLevelObserver.

### Properties
- **rtpObserver.id** (String, read only) - RTP observer identifier.
- **rtpObserver.closed** (Boolean, read only) - Whether the RTP observer is closed.
- **rtpObserver.type** (RtpObserverType, read only) - RTP observer type.
- **rtpObserver.paused** (Boolean, read only) - Whether the RTP observer is paused.
- **rtpObserver.appData** (AppData) - Custom data provided by the application.
- **rtpObserver.observer** (EventEmitter, read only) - Observer events.

### Methods
#### rtpObserver.close()

Closes the RTP observer.

#### rtpObserver.pause()

Pauses the RTP observer. No RTP is inspected until `resume()` is called.
> `@async`

#### rtpObserver.resume()

Resumes the RTP observer. RTP is inspected again.
> `@async`

#### rtpObserver.addProducer(options)

Provides the RTP observer with a new producer to monitor.
Argument | Type | Description | Required | Default
---|---|---|---|---
`options` | RtpObserverAddRemoveProducerOptions | Options. | Yes |
> `@async`

#### rtpObserver.removeProducer(options)

Removes the given producer from the RTP observer.
Argument | Type | Description | Required | Default
---|---|---|---|---
`options` | RtpObserverAddRemoveProducerOptions | Options. | Yes |
> `@async`
```

---

### Consumer Trace Events API

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This section details how to enable and handle various trace events emitted by a Mediasoup v3 consumer.

````APIDOC
## Consumer Trace Events API

This API allows you to enable and handle various trace events emitted by a Mediasoup v3 consumer. These events provide insights into the media stream's behavior and network conditions.

### Enable Trace Event ('keyframe')

Enables the consumer to emit 'trace' events specifically for keyframe packets.

- **Method**: N/A (This is an event listener setup)
- **Endpoint**: N/A

#### Event Listener Example:

```javascript
consumer.on('trace', (trace) => {
  // trace =>
  // {
  //   "direction": "out",
  //   "info": {
  //     "isKeyFrame": true,
  //     "marker": "true",
  //     "payloadSize": 437,
  //     "payloadType": 101,
  //     "sequenceNumber": 1,
  //     "size": 465,
  //     "spatialLayer": 0,
  //     "ssrc": 185272966,
  //     "temporalLayer": 0,
  //     "timestamp": 936997226,
  //     "wideSequenceNumber": 17
  //   },
  //   "timestamp": 1514298020,
  //   "type": "keyframe"
  // }
});
````

**Note**: When 'keyframe' type is enabled, you will receive 'trace' events with type 'keyframe' instead of 'rtp' for keyframe packets.

### Enable Trace Event ('nack')

Enables the consumer to emit 'trace' events for Negative Acknowledgement (NACK) packets.

- **Method**: N/A (This is an event listener setup)
- **Endpoint**: N/A

#### Event Listener Example:

```javascript
consumer.on("trace", (trace) => {
	// trace =>
	// {
	//   "direction": "in",
	//   "info": {}
	//   "timestamp": 1546498145,
	//   "type": "nack"
	// }
})
```

### Enable Trace Event ('pli')

Enables the consumer to emit 'trace' events for Picture Loss Indication (PLI) packets.

- **Method**: N/A (This is an event listener setup)
- **Endpoint**: N/A

#### Event Listener Example:

```javascript
consumer.on("trace", (trace) => {
	// trace =>
	// {
	//   "direction": "in",
	//   "info": {
	//     "ssrc": 5698432
	//   }
	//   "timestamp": 1544798444,
	//   "type": "pli"
	// }
})
```

### Enable Trace Event ('fir')

Enables the consumer to emit 'trace' events for Full Intra Request (FIR) packets.

- **Method**: N/A (This is an event listener setup)
- **Endpoint**: N/A

#### Event Listener Example:

```javascript
consumer.on("trace", (trace) => {
	// trace =>
	// {
	//   "direction": "in",
	//   "info": {
	//     "ssrc": 776452943
	//   }
	//   "timestamp": 1543498101,
	//   "type": "fir"
	// }
})
```

````

--------------------------------

### Consumer Class: Media Track Reception (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

The `Consumer` class represents a received media track. It provides methods to retrieve its ID, producer ID, media kind, RTP parameters, track object, statistics, and application data. It supports pausing, resuming, and closing the consumer.

```cpp
#include "libmediasoupclient/Consumer.hpp"

// Assuming 'consumer' is an instance of mediasoupclient::Consumer

// Get consumer ID
// const std::string& id = consumer.GetId();

// Get associated producer ID
// const std::string& producerId = consumer.GetProducerId();

// Get RTP parameters
// const mediasoupclient::RtpParameters& rtpParams = consumer.GetRtpParameters();

// Check if paused
// bool paused = consumer.IsPaused();

// Pause the consumer
// consumer.Pause();

// Resume the consumer
// consumer.Resume();

// Close the consumer
// consumer.Close();

````

---

### RtpCodecParameters

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Provides details on codec settings within RTP parameters, including MIME type, payload type, clock rate, channels, and specific parameters.

````APIDOC
## RtpCodecParameters

### Description
Provides information on codec settings within the RTP parameters. The list of media codecs supported by mediasoup and their settings is defined in the supportedRtpCapabilities.ts file.

### Method
N/A (Object Definition)

### Endpoint
N/A

### Parameters
#### Body Parameters
- **mimeType** (String) - Yes - The codec MIME media type/subtype (e.g. "audio/opus", "video/VP8").
- **payloadType** (Number) - Yes - The value that goes in the RTP Payload Type Field. Must be unique.
- **clockRate** (Number) - Yes - Codec clock rate expressed in Hertz.
- **channels** (Number) - Optional - The number of channels supported (e.g. two for stereo). Just for audio. Default: 1.
- **parameters** (Object) - Optional - Codec-specific parameters available for signaling. Some parameters (such as “packetization-mode” and “profile-level-id” in H264 or “profile-id” in VP9) are critical for codec matching. Default: {}.
- **rtcpFeedback** (Array<RtcpFeedback>) - Optional - Transport layer and codec-specific feedback messages for this codec. Default: `[]`.

See the Codec Parameters section below for more info about the codec `parameters`.

### Request Example
```json
{
  "mimeType": "audio/opus",
  "payloadType": 111,
  "clockRate": 48000,
  "channels": 2,
  "parameters": {},
  "rtcpFeedback": [
    { "type": "transport-cc" }
  ]
}
````

### Response

#### Success Response (200)

- **mimeType** (String) - The codec MIME media type/subtype.
- **payloadType** (Number) - The RTP Payload Type Field value.
- **clockRate** (Number) - Codec clock rate in Hertz.
- **channels** (Number) - Number of channels (audio only).
- **parameters** (Object) - Codec-specific parameters.
- **rtcpFeedback** (Array<RtcpFeedback>) - RTCP feedback messages.

### Response Example

```json
{
	"mimeType": "audio/opus",
	"payloadType": 111,
	"clockRate": 48000,
	"channels": 2,
	"parameters": {},
	"rtcpFeedback": [{ "type": "transport-cc" }]
}
```

````

--------------------------------

### PlainTransport Options

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Configuration options for creating a PlainTransport, which handles RTP, RTCP, and SCTP transmission without WebRTC signaling.

```APIDOC
## PlainTransportOptions Dictionary

### Description
Options for creating a PlainTransport.

### Fields
- **listenInfo** (TransportListenInfo) - Required - Listening information.
- **rtcpListenInfo** (TransportListenInfo) - Optional - RTCP listening information. If not given and `rtcpPort` is not `false`, RTCP will use the same listening info as RTP.
- **listenIp** (TransportListenIp | String) - Required - Listening IP address. **DEPRECATED**: Use `listenInfo` instead.
- **port** (Number) - Optional - Fixed port to listen on instead of selecting automatically from the Worker's port range.
- **rtcpMux** (Boolean) - Optional - Use RTCP-mux (RTP and RTCP in the same port). Defaults to `true`.
- **comedia** (Boolean) - Optional - Whether remote IP:port should be auto-detected based on the first RTP/RTCP packet received. Defaults to `false`.
- **enableSctp** (Boolean) - Optional - Create a SCTP association. Defaults to `false`.
- **numSctpStreams** (NumSctpStreams) - Optional - SCTP streams number.
- **maxSctpMessageSize** (Number) - Optional - Maximum allowed size for SCTP messages sent by `DataProducers`. Defaults to `262144`.
- **sctpSendBufferSize** (Number) - Optional - SCTP send buffer size used by usrsctp. Defaults to `262144`.
- **enableSrtp** (Boolean) - Optional - Enable SRTP to encrypt RTP and SRTP. Defaults to `false`.
- **srtpCryptoSuite** (SrtpCryptoSuite) - Optional - SRTP crypto suite. Only valid if `enableSrtp` is `true`. Defaults to `"AES_CM_128_HMAC_SHA1_80"`.
- **appData** (AppData) - Optional - Custom application data. Defaults to `{ }`.

### Notes
- `listenIp` and `port` are **DEPRECATED**. Use `listenInfo` instead.
- `rtcpPort` is **DEPRECATED**. Use `rtcpListenInfo` instead to set up different listening information for RTCP.
- `comedia` mode is useful when the remote endpoint will produce RTP on this plain transport. If the remote endpoint does not send any RTP (or SCTP) packet, mediasoup cannot detect its remote RTP IP and port, and thus won't send any packets to it. Do not use `comedia` if the remote endpoint only consumes RTP; instead, call `connect()` with the remote IP and port(s).

### Example
```javascript
const plainTransportOptions = {
  listenInfo: {
    ip: '0.0.0.0',
    port: 10000
  },
  enableSctp: true,
  numSctpStreams: {
    OSC: 1,
    MISC: 1
  }
};
````

````

--------------------------------

### RtpParameters Dictionary

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Defines the common structure for RTP parameters (RtpSendParameters and RtpReceiveParameters), including fields for MID, codecs, header extensions, encodings, RTCP, and MSID.

```APIDOC
## RtpParameters Dictionary

There are two types of RTP parameters (`RtpSendParameters` and `RtpReceiveParameters`), both sharing the following definition:

| Field           | Type                      | Description                                                                      |
|-----------------|---------------------------|----------------------------------------------------------------------------------|
| `mid`           | String                    | The MID RTP extension value as defined in the BUNDLE specification.              |
| `codecs`        | Array<RtpCodecParameters> | Media and RTX codecs in use.                                                     |
| `headerExtensions` | Array<RtpHeaderExtensionParameters> | RTP header extensions in use.                                                    |
| `encodings`     | Array<RtpEncodingParameters> | Transmitted RTP streams and their settings.                                      |
| `rtcp`          | RtcpParameters            | Parameters used for RTCP.                                                        |
| `msid`          | String                    | The value of the `a=msid` attribute in the SDP media section (see RFC 8830). |
````

---

### K-SVC Producer with 4 Spatial and 5 Temporal Layers

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Details the `encodings` for a mediasoup K-SVC (Key-frame SVC) producer. The `scalabilityMode` includes '\_KEY' to denote the K-SVC variant, with 4 spatial and 5 temporal layers.

```javascript
encodings: [{ ssrc: 111110, scalabilityMode: "L4T5_KEY" }]
```

---

### Consumer Class

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Represents a media consumer.

```APIDOC
## Consumer Class

### Description
Represents a media consumer receiving media from a mediasoup router.

### Dictionaries
- **ConsumerOptions**: Options for creating a consumer.
- **OnRtpReceiverCallback**: Callback for RTP receiver information.

### Properties
- **id**: (String) - The unique identifier of the consumer.
- **producerId**: (String) - The ID of the producer this consumer is receiving from.
- **closed**: (Boolean) - Indicates if the consumer is closed.
- **kind**: (String) - The media kind ('audio' or 'video').
- **rtpReceiver**: (Object) - The underlying RTCRtpReceiver.
- **track**: (MediaStreamTrack) - The media track being received.
- **rtpParameters**: (Object) - The RTP parameters for the consumer.
- **paused**: (Boolean) - Indicates if the consumer is paused.
- **appData**: (Object) - Arbitrary application data.
- **observer**: (Object) - An observer for consumer events.

### Methods
- **consumer.close()**: Closes the consumer.
- **consumer.getStats()**: Retrieves statistics for the consumer.
- **consumer.pause()**: Pauses the consumer.
- **consumer.resume()**: Resumes the consumer.

### Events
- **consumer.on(“transportclose”, fn())**: Emitted when the associated transport is closed.
- **consumer.on(“trackended”, fn())**: Emitted when the media track ends.

### Observer Events
- **consumer.observer.on(“close”, fn())**: Emitted when the consumer is closed.
- **consumer.observer.on(“pause”, fn())**: Emitted when the consumer is paused.
- **consumer.observer.on(“resume”, fn())**: Emitted when the consumer is resumed.
- **consumer.observer.on(“trackended”, fn())**: Emitted when the media track ends.
```

---

### Create Plain Transport for Video in Mediasoup

Source: https://mediasoup.org/documentation/v3/communication-between-client-and-server

Creates a plain transport in mediasoup for sending video. Similar to the audio transport, it configures listening on localhost with specific RTCP muxing and comedia options, and extracts the local RTP and RTCP ports.

```javascript
const videoTransport = await router.createPlainTransport({
	listenIp: "127.0.0.1",
	rtcpMux: false,
	comedia: true,
})

// Read the transport local RTP port.
const videoRtpPort = videoTransport.tuple.localPort
// => 3501

// Read the transport local RTCP port.
const videoRtcpPort = videoTransport.rtcpTuple.localPort
// => 2989
```

---

### Simulcast Producer with 4 RIDs and Temporal Layers

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Configures a mediasoup producer for Simulcast using RIDs (Replaceable Identifier) for 4 streams, each with 3 temporal layers. This allows for more granular control and identification of individual streams.

```javascript
encodings: [
	{ rid: "r0", scalabilityMode: "L1T3" },
	{ rid: "r1", scalabilityMode: "L1T3" },
	{ rid: "r2", scalabilityMode: "L1T3" },
	{ rid: "r3", scalabilityMode: "L1T3" },
]
```

---

### Producer Methods

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This section details the methods available for controlling and querying the Producer object, including closing, pausing, resuming, and enabling trace events.

````APIDOC
## Producer Methods

### `producer.close()`

Closes the producer and triggers a `producerclose` event on all associated consumers.

### `producer.getStats()`

Asynchronously retrieves the current RTC statistics for the producer.

*   **Returns**: `Promise<Array<ProducerStat>>`
*   **Description**: Check the RTC Statistics section for more details.

### `producer.pause()`

Pauses the producer, ceasing RTP transmission to consumers. Triggers a `producerpause` event on all associated consumers.

*   **Returns**: `Promise<void>`

### `producer.resume()`

Resumes the producer, enabling RTP transmission to consumers. Triggers a `producerresume` event on all associated consumers.

*   **Returns**: `Promise<void>`

### `producer.enableTraceEvent(types)`

Instructs the producer to emit `trace` events for monitoring purposes. Use with caution.

*   **Arguments**:
    *   `types` (Array<ProducerTraceEventDataEventType>): The types of trace events to enable. Defaults to unset (disabled).
*   **Returns**: `Promise<void>`

**Example**:
```javascript
await producer.enableTraceEvent([ "rtp", "pli" ]);

producer.on("trace", (trace) => {
  // trace.type can be "rtp" or "pli".
});
````

### `producer.send(rtpPacket)`

Sends a raw RTP packet from the Node.js process. This method is only available on direct transports.

- **Arguments**:
  - `rtpPacket` (Buffer): A Node.js Buffer containing a valid RTP packet.
- **Description**: Only available on transports created via `router.createDirectTransport()`.

**Example**:

```javascript
const producer = await directTransport.produce({
  kind: "audio",
  rtpParameters: { ... },
});

// Send a RTP packet.
producer.send(rtpPacket);
```

````

--------------------------------

### RtpReceiveParameters

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Describes a media stream sent by mediasoup to an endpoint via a Consumer. It typically contains a single encoding, with random SSRC values, and supports layer selection via `setPreferredLayers()`.

```APIDOC
## RtpReceiveParameters

### Description
The RTP receive parameters describe a media stream as sent by mediasoup to an endpoint through its corresponding mediasoup Consumer.

- There is a single entry in the `encodings` array (even if the corresponding producer uses simulcast). The consumer sends a single and continuous RTP stream to the endpoint and spatial/temporal layer selection is possible via `consumer.setPreferredLayers()`.
- As an exception, previous bullet is not true when consuming a stream over a PipeTransport, in which all RTP streams from the associated producer are forwarded verbatim through the consumer.
- The RTP receive parameters will always have their `ssrc` values randomly generated for all of its `encodings` (and optional `rtx: { ssrc: XXXX }` if the endpoint supports RTX), regardless of the original RTP send parameters in the associated producer. This applies even if the producer's `encodings` have `rid` set.

### Method
N/A (Object Definition)

### Endpoint
N/A

### Parameters
(Inherits from RtpParameters)

### Request Example
```json
{
  "encodings": [
    { "ssrc": 98765 }
  ]
}
````

### Response

(Object Definition)

### Response Example

```json
{
	"encodings": [{ "ssrc": 98765 }]
}
```

````

--------------------------------

### mediasoupClient Properties

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Details on the properties available on the top-level mediasoupClient module, including types, version, and debug access.

```APIDOC
## mediasoupClient Properties

### mediasoupClient.types

An Object holding all classes and **TypeScript** types exported by mediasoup-client.

> `@type` Object, read only

```typescript
import { types as mediasoupTypes } from "mediasoup-client";

let producer: mediasoupTypes.Producer;
let rtpParameters: mediasoupTypes.RtpParameters;

// or alternatively:

import { Producer, RtpParameters } from "mediasoup-client/types";

let producer: Producer;
let rtpParameters: RtpParameters;
````

### mediasoupClient.version

The mediasoup-client version.

> `@type` String, read only

```javascript
console.log(mediasoupClient.version)
// => "3.0.0"
```

### mediasoupClient.debug

Exposes the debug dependency used by mediasoup-client. Useful if you need to enable/disable `debug` namespaces programatically.

````

--------------------------------

### Create PipeTransport with Mediasoup v3

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a new pipe transport, used for interconnecting routers. It requires PipeTransportOptions, including listenInfo. Returns a PipeTransport object.

```typescript
const transport = await router.createPipeTransport(
  {
    listenInfo : { protocol: "udp", ip: "192.168.1.33" },
  }
);

````

---

### DirectTransport

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the DirectTransport class, including its properties, methods, and events.

```APIDOC
## DirectTransport

### Description
Provides methods and properties for managing direct transports.

### Dictionaries
- **DirectTransportOptions**

### Properties
- None

### Methods
- **getStats()** - Retrieves statistics for the transport.
- **connect()** - Connects the transport.
- **setMaxIncomingBitrate(options)** - Sets the maximum incoming bitrate.
- **setMaxOutgoingBitrate(options)** - Sets the maximum outgoing bitrate.
- **setMinOutgoingBitrate(options)** - Sets the minimum outgoing bitrate.
- **sendRtcp(rtcpPacket)** - Sends an RTCP packet.

### Events
- **on(“rtcp”, fn(rtcpPacket))** - Emitted when an RTCP packet is received.

### Observer Events
- None
```

---

### H264 Codec Matching Parameters

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Specific parameters for H264 codec matching, used to determine compatibility between RTP capabilities.

```APIDOC
### Parameters for Codec Matching

#### H264

H264 codec matching rules are complex and involve inspection of the following parameters (see the RFC 6184 for more details):

- **`packetization-mode`** (Number) - Optional - 0 means that the single NAL mode must be used. 1 means that the non-interleaved mode must be used. Default: 0.
- **`profile-level-id`** (String) - Required - Indicates the default sub-profile and the default level of the stream.
- **`level-asymmetry-allowed`** (Number) - Optional - Indicates whether level asymmetry is allowed. Default: 0.

mediasoup uses the h264-profile-level-id JavaScript library to evaluate those parameters and perform proper H264 codec matching.

Depending the negotiated H264 “packetization-mode” and “profile-level-id”, Chrome may use OpenH264 software encoder or H264 external hardware encoder. In the latter case, Chrome will **NOT** generate simulcast but a single stream.

See the reported issue for more information.
```

---

### Access Mediasoup Version and Worker Binary Path

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Illustrates how to access the current mediasoup version and the absolute path to the mediasoup-worker binary. It also mentions that the worker binary path can be overridden by the MEDIASOUP_WORKER_BIN environment variable.

```javascript
console.log(mediasoup.version)
// => "3.0.0"

console.log(mediasoup.workerBin)
// => "/home/deploy/media-server-app/node_modules/mediasoup/worker/out/Release/mediasoup-worker"
```

---

### CreateRecvTransport Method for Mediasoup Device

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Creates a new WebRTC transport for receiving media. Similar to CreateSendTransport, it requires a listener, transport ID, ICE parameters, ICE candidates, and DTLS parameters. Optional parameters include PeerConnection options and application data. The transport must be created on the mediasoup router beforehand.

```cpp
auto* recvTransportListener = new MyRecvTransportListener();

// This will block the current thread until completion.
auto* recvTransport = device.CreateRecvTransport(
  recvTransportListener,
  id,
  iceParameters,
  iceCandidates,
  dtlsParameters);

```

---

### Detect Browser/Device Handler Asynchronously

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Shows how to use the `detectDeviceAsync` function to asynchronously detect the suitable mediasoup-client WebRTC handler for the current environment. It logs the handler name or a warning if none is found. This function may allow for more accurate checks in the future compared to the deprecated `detectDevice`.

```javascript
const handlerName = await mediasoupClient.detectDeviceAsync()

if (handlerName) {
	console.log("detected handler: %s", handlerName)
} else {
	console.warn("no suitable handler found for current browser/device")
}
```

---

### Transport Methods

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Methods available on the transport object.

````APIDOC
## Transport Methods

### Description
Methods available on the transport object.

### Methods
#### `close()`
Closes the transport, including all its producers and consumers.
This method should be called when the server side transport has been closed (and vice-versa).

#### `getStats()`
Gets the local transport statistics by calling `getStats()` in the underlying `RTCPeerConnection` instance.
> `@async`
> `@returns` RTCStatsReport

#### `restartIce({ iceParameters })`
Instructs the underlying peerconnection to restart ICE by providing it with new remote ICE parameters.
Argument | Type | Description | Required | Default
---|---|---|---|---
`iceParameters` | IceParameters | New ICE parameters of the server side transport. | Yes |
> `@async`
This method must be called after restarting ICE in server side via webRtcTransport.restartIce().
```javascript
await transport.restartIce({ iceParameters: { ... } });
````

#### `updateIceServers({ iceServers })`

Provides the underlying peerconnection with a new list of TURN servers.
Argument | Type | Description | Required | Default  
---|---|---|---|---
`iceServers` | Array<RTCIceServer> | List of TURN servers to provide the local peerconnection with. | No | `[ ]`

> `@async`
> This method is specially useful if the TURN server credentials have changed.

```javascript
await transport.updateIceServers({ iceServers: [ ... ] });
```

````

--------------------------------

### Produce Media Track with Transport

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Instructs the transport to send an audio or video track to the mediasoup router. Requires ProducerOptions, including the track and optional encodings and codec options. Emits a 'produce' event that must be handled by the application to signal parameters to the server.

```javascript
const stream = await navigator.mediaDevices.getUserMedia({ video: true });
const videoTrack = stream.getVideoTracks()[0];
const producer = await transport.produce(
  {
    track       : videoTrack,
    encodings   :
    [
      { maxBitrate: 100000 },
      { maxBitrate: 300000 },
      { maxBitrate: 900000 }
    ],
    codecOptions :
    {
      videoGoogleStartBitrate : 1000
    }
  });

````

---

### TransportProduceParameters

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Parameters for producing media on a transport.

```APIDOC
## Transport Produce Parameters

### Description
Parameters for producing media on a transport.

### Parameters
#### Request Body
- **kind** (MediaKind) - Producer's media kind (“audio” or “video”).
- **rtpParameters** (RtpSendParameters) - Producer's RTP parameters.
- **appData** (Object) - Custom application data as given in the `transport.produce()` method.
```

---

### Enable Trace Events

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

Enables the 'trace' event on Transports, Producers, and Consumers for RTP/RTCP monitoring.

````APIDOC
## Enable Trace Events for RTP/RTCP Monitoring

### Description
Transports, producers, and consumers in mediasoup offer a `enableTraceEvent()` method. Calling this method allows you to capture and analyze RTP/RTCP packet information through the 'trace' event, which is crucial for debugging network performance and media stream quality.

### Methods
- `transport.enableTraceEvent(types)`
- `producer.enableTraceEvent(types)`
- `consumer.enableTraceEvent(types)`

### Parameters
#### Path Parameters
- **types** (array of strings) - Required - An array specifying the types of trace events to enable. Possible values include 'probation', 'bwe', 'rtp', 'keyframe', 'nack', 'pli', 'fir', 'sr'.

### Event Listener Example
```javascript
// For Transport
transport.on('trace', (trace) => {
  console.log('Transport trace event:', trace);
});

// For Producer
producer.on('trace', (trace) => {
  console.log('Producer trace event:', trace);
});

// For Consumer
consumer.on('trace', (trace) => {
  console.log('Consumer trace event:', trace);
});
````

````

--------------------------------

### SendTransport Listener Interface: Production Events (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Defines the `SendTransportListener` interface for handling events related to media and data production on a `SendTransport`. `OnProduce` is called when the transport needs to create a producer, providing details about the media kind, RTP parameters, and app data. `OnProduceData` is for data producers.

```cpp
#include "libmediasoupclient/SendTransport.hpp"

class MySendTransportListener : public mediasoupclient::SendTransport::Listener {
public:
    void OnProduce(
        mediasoupclient::Transport* transport,
        const std::string& kind,
        const mediasoupclient::RtpParameters& rtpParameters,
        const nlohmann::json& appData
    ) override {
        // Handle media production request
    }

    void OnProduceData(
        mediasoupclient::Transport* transport,
        const mediasoupclient::SctpStreamParameters& sctpStreamParameters,
        const std::string& label,
        const std::string& protocol,
        const nlohmann::json& appData
    ) override {
        // Handle data production request
    }
};

````

---

### Enable and Handle FIR Trace Events in Mediasoup v3 Consumer

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

Enables 'fir' (Full Intra Request) trace events on a consumer, which is similar to PLI but is a more efficient way to request a keyframe. An event listener is provided to capture and process the FIR trace data.

```javascript
consumer.enableTraceEvent([ 'fir' ]);

consumer.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "in",
    "info": {
      "ssrc": 776452943
    }
    "timestamp": 1543498101,
    "type": "fir"
  }
});
```

---

### Access mediasoup-client Types and Version

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Shows how to import and use TypeScript types from mediasoup-client for variables like Producer and RtpParameters, and how to access the library's version string.

```typescript
import { types as mediasoupTypes } from "mediasoup-client"

let producer: mediasoupTypes.Producer
let rtpParameters: mediasoupTypes.RtpParameters

// or alternatively:

import { Producer, RtpParameters } from "mediasoup-client/types"

let producer: Producer
let rtpParameters: RtpParameters

// Accessing version:
console.log(mediasoupClient.version)
// => "3.0.0"
```

---

### Producer Class

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Represents a media producer.

```APIDOC
## Producer Class

### Description
Represents a media producer sending media to a mediasoup router.

### Dictionaries
- **ProducerOptions**: Options for creating a producer.
- **ProducerCodecOptions**: Codec options for a producer.
- **ProducerHeaderExtensionOptions**: Header extension options for a producer.
- **OnRtpSenderCallback**: Callback for RTP sender information.

### Properties
- **id**: (String) - The unique identifier of the producer.
- **closed**: (Boolean) - Indicates if the producer is closed.
- **kind**: (String) - The media kind ('audio' or 'video').
- **rtpSender**: (Object) - The underlying RTCRtpSender.
- **track**: (MediaStreamTrack) - The media track being sent.
- **rtpParameters**: (Object) - The RTP parameters for the producer.
- **paused**: (Boolean) - Indicates if the producer is paused.
- **maxSpatialLayer**: (Number) - The maximum spatial layer for simulcast.
- **appData**: (Object) - Arbitrary application data.
- **observer**: (Object) - An observer for producer events.

### Methods
- **producer.close()**: Closes the producer.
- **producer.getStats()**: Retrieves statistics for the producer.
- **producer.pause()**: Pauses the producer.
- **producer.resume()**: Resumes the producer.
- **producer.replaceTrack({ track })**: Replaces the current media track.
- **producer.setMaxSpatialLayer(spatialLayer)**: Sets the maximum spatial layer for simulcast.
- **producer.setRtpEncodingParameters(params)**: Sets RTP encoding parameters.

### Events
- **producer.on(“transportclose”, fn())**: Emitted when the associated transport is closed.
- **producer.on(“trackended”, fn())**: Emitted when the media track ends.

### Observer Events
- **producer.observer.on(“close”, fn())**: Emitted when the producer is closed.
- **producer.observer.on(“pause”, fn())**: Emitted when the producer is paused.
- **producer.observer.on(“resume”, fn())**: Emitted when the producer is resumed.
- **producer.observer.on(“trackended”, fn())**: Emitted when the media track ends.
```

---

### Transport RestartIce Method

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Initiates an ICE restart for the underlying peer connection by providing new remote ICE parameters. This method must be called after restarting ICE on the server-side WebRTC transport. It is an asynchronous operation that blocks the current thread.

```cpp
transport.RestartIce(iceParameters);

```

---

### Scalability Mode Parsing

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Parses scalability mode strings to extract spatial and temporal layer information.

````APIDOC
## mediasoupclient::parseScalabilityMode(scalabilityMode)

Parses the given `scalabilityMode` string according to the rules in webrtc-svc.

### Parameters

#### Path Parameters

* **scalabilityMode** (const std::string&) - Required - Scalability mode.

### Returns

nlohmann::json:
* `spatialLayers` (uint16_t) - Number of spatial layers (by default 1).
* `temporalLayers` (uint16_t) - Number of temporal layers (by default 1).

### Examples

```cpp
mediasoupclient::parseScalabilityMode("L2T3");
// => { "spatialLayers": 2, "temporalLayers": 3 }

mediasoupclient::parseScalabilityMode("L4T7_KEY_SHIFT");
// => { "spatialLayers": 4, "temporalLayers": 7 }
````

````

--------------------------------

### CreateSendTransport Method for Mediasoup Device

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Creates a new WebRTC transport for sending media. This method requires a listener, transport ID, ICE parameters, ICE candidates, and DTLS parameters. It optionally accepts PeerConnection options and custom application data. The transport must be pre-created on the mediasoup router.

```cpp
auto* sendTransportListener = new MySendTransportListener();

// This will block the current thread until completion.
auto* sendTransport = device.CreateSendTransport(
  sendTransportListener,
  id,
  iceParameters,
  iceCandidates,
  dtlsParameters);

````

---

### DirectTransport Options Configuration

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Specifies the configuration options for a DirectTransport. The primary option is `maxMessageSize` for direct messages, along with custom application data.

```typescript
interface DirectTransportOptions {
	maxMessageSize?: number
	appData?: AppData
}
```

---

### Logger LogHandlerInterface: Custom Logging (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

The `Logger::LogHandlerInterface` is an abstract base class for creating custom log handlers. Implementing the `OnLog` method allows you to define how log messages, including their level and payload, are processed and outputted.

```cpp
#include "libmediasoupclient/Logger.hpp"

class MyLogHandler : public mediasoupclient::Logger::LogHandlerInterface {
public:
    void OnLog(mediasoupclient::LogLevel level, const char* payload, size_t len) override {
        // Process the log message here
        // For example, print to console or a file
        printf("Log [%d]: %.*s\n", (int)level, (int)len, payload);
    }
};

```

---

### DataProducer Method: Send

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Sends data over the corresponding DataChannel. Data is buffered if congestion occurs, up to 16MB. Exceeding this buffer limit will cause the data channel to close abruptly, necessitating monitoring of buffered amount.

```cpp
webrtc::DataBuffer buffer(data, size);
dataProducer.Send(buffer);
```

---

### PipeTransport

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the PipeTransport class, including its properties, methods, and events.

```APIDOC
## PipeTransport

### Description
Provides methods and properties for managing pipe transports.

### Dictionaries
- **PipeTransportOptions**

### Properties
- **tuple** (TransportTuple) - The transport tuple.
- **sctpParameters** (SctpParameters) - The SCTP parameters for the transport.
- **sctpState** (SctpState) - The current SCTP state.
- **srtpParameters** (SrtpParameters) - The SRTP parameters for the transport.

### Methods
- **getStats()** - Retrieves statistics for the transport.
- **connect({ ip, port })** - Connects the transport with the given options.

### Events
- **on(“sctpstatechange”, fn(sctpState))** - Emitted when the SCTP state changes.

### Observer Events
- **observer.on(“sctpstatechange”, fn(sctpState))** - Observer event for SCTP state changes.
```

---

### mediasoup\* _ERROR_

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This configuration enables logging for all ICE and DTLS debug messages, warnings, and any type of error from mediasoup.

````APIDOC
## Set Debug Logging for ICE, DTLS, and Errors

### Description
This command enables detailed logging for all ICE and DTLS related debug messages, warnings, and any critical errors from mediasoup.

### Method
Environment Variable and Node.js Execution

### Command Example
```bash
$ DEBUG="mediasoup* *ERROR*" node myapp.js
````

### Code Example

```javascript
const mediasoup = require("mediasoup")

const worker = await mediasoup.createWorker({
	logLevel: "debug",
	logTags: ["ice", "dtls"],
})
```

````

--------------------------------

### Transport Concepts and Data Structures

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Explains the abstract Transport class in mediasoup, its implementations, and various data structures used for configuration, such as TransportListenInfo, TransportListenIp, TransportPortRange, TransportSocketFlags, and TransportTuple.

```APIDOC
## Transport

> `@abstract`
A transport connects an endpoint with a mediasoup router and enables transmission of media in both directions by means of Producer, Consumer, DataProducer and DataConsumer instances created on it.

mediasoup implements the following transport classes:
  * WebRtcTransport
  * PlainTransport
  * PipeTransport
  * DirectTransport

### Dictionaries

#### TransportListenInfo

| Field            | Type    | Description                                                                                                                                                                                                                                            | Required | Default |
|------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| `protocol`       | String  | Protocol (“udp” / “tcp”).                                                                                                                                                                                                                              | Yes      |         |
| `ip`             | String  | Listening IPv4 or IPv6.                                                                                                                                                                                                                                | Yes      |         |
| `announcedAddress` | String  | Announced IPv4, IPv6 or hostname (useful when running mediasoup behind NAT with private IP).                                                                                                                                                                | No       |         |
| `exposeInternalIp` | Boolean | When using ICE candidates and `announcedAddress` is set (e.g., a public IP or domain), this option controls whether to also include the internal/local IP address in the ICE candidate list. If `true`, both the local IP and announced address are advertised. If `false`, only the announced address is used. | No       | `false` |
| `port`           | Number  | Listening port.                                                                                                                                                                                                                                        | No       | If not given, a random available port from the Worker's port range will be used. |
| `portRange`      | TransportPortRange | Listening port range.                                                                                                                                                                                                                                  | No       | If given, a random available port in this range (in given IP and protocol) will be used. |
| `flags`          | TransportSocketFlags | UDP/TCP socket flags.                                                                                                                                                                                                                                | No       | All flags are disabled. |
| `sendBufferSize` | Number  | Send buffer size (in bytes).                                                                                                                                                                                                                             | No       |         |
| `recvBufferSize` | Number  | Receive buffer size (in bytes).                                                                                                                                                                                                                          | No       |         |

If you use “0.0.0.0” or “::” as `ip` value, then you need to also provide `announcedAddress`.

#### TransportListenIp

| Field         | Type   | Description                                                                                         | Required | Default |
|---------------|--------|-----------------------------------------------------------------------------------------------------|----------|---------|
| `ip`          | String | Listening IPv4 or IPv6.                                                                             | Yes      |         |
| `announcedIp` | String | Announced IPv4 or IPv6 (useful when running mediasoup behind NAT with private IP).                   | No       |         |

  * **DEPRECATED:** Use TransportListenInfo instead.
  * If you use “0.0.0.0” or “::” as `ip` value, then you need to also provide `announcedIp`.

#### TransportPortRange

| Field | Type   | Description                | Required | Default |
|-------|--------|----------------------------|----------|---------|
| `min` | Number | Lowest port of the range.  | Yes      | 0       |
| `max` | Number | Highest port of the range. | Yes      | 0       |

#### TransportSocketFlags

| Field        | Type    | Description                                                                            | Required | Default |
|--------------|---------|----------------------------------------------------------------------------------------|----------|---------|
| `ipv6Only`   | Boolean | Disable dual-stack support so only IPv6 is used (only if ip is IPv6).                  | No       | `false` |
| `udpReusePort` | Boolean | Make different transports bind to the same ip and port (only for UDP). Useful for multicast scenarios with plain transport. Use with caution. | No       | `false` |

#### TransportTuple

| Field        | Type   | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Required | Default |
|--------------|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- `protocol` | String | Protocol (“udp” / “tcp”). | Yes | | `localAddress` | String | Local IP address or announced IP or hostname. | Yes | | `localPort` | Number | Local port. | Yes | | `remoteIp` | String | Remote IP address. | No | | `remotePort` | Number | Remote port. | No | | `protocol` | String | Protocol (“udp” / “tcp”). | Yes | | Both `remoteIp` and `remotePort` are unset until the media address of the remote endpoint is known, which happens after calling `transport.connect()` in `PlainTransport` and `PipeTransport`, or via dynamic detection as it happens in `WebRtcTransport` (in which the remote media address is detected by ICE means), or in `PlainTransport` (when using `comedia` mode).
````

---

### DataConsumer: Send Messages (SCTP)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Demonstrates how to send string and binary messages using the `send` method of a DataConsumer. This method is only available for SCTP data consumers. It handles potential errors if the underlying SCTP send buffer is full.

```javascript
const stringMessage = "hello"
const binaryMessage = Buffer.from([1, 2, 3, 4])

dataConsumer.send(stringMessage)
dataConsumer.send(binaryMessage)
```

---

### SRTP Parameters

Source: https://mediasoup.org/documentation/v3/mediasoup/srtp-parameters

Details the SRTP parameters used in mediasoup v3 for transports with optional SRTP support.

```APIDOC
## SRTP Parameters

These SRTP parameters apply to `PlainTransport` and `PipeTransport` which have optional SRTP support.

### Dictionaries

#### SrtpParameters

| Field       | Type            | Description                                       | Required | Default |
|-------------|-----------------|---------------------------------------------------|----------|---------|
| `cryptoSuite` | SrtpCryptoSuite | Encryption and authentication transforms to be used. | Yes      |         |
| `keyBase64`   | String          | SRTP keying material (master key and salt) in Base64. | Yes      |         |

### Enums

#### SrtpCryptoSuite

| Value                  | Description                                             |
|------------------------|---------------------------------------------------------|
| "AEAD_AES_256_GCM"     | It requires SRTP keying material of 44 bytes (60 bytes in Base64). |
| "AEAD_AES_128_GCM"     | It requires SRTP keying material of 28 bytes (40 bytes in Base64). |
| "AES_CM_128_HMAC_SHA1_80" | It requires SRTP keying material of 30 bytes (40 bytes in Base64). |
| "AES_CM_128_HMAC_SHA1_32"  | It requires SRTP keying material of 30 bytes (40 bytes in Base64). |
```

---

### Mediasoup Transport Options and Parameters

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Defines the structure for TransportOptions, TransportProduceParameters, and TransportProduceDataParameters used in Mediasoup v3. These structures specify configuration and data for creating and managing transports, producing media, and producing data.

```typescript
interface TransportOptions {
	id: string
	iceParameters: IceParameters
	iceCandidates: Array<IceCandidate>
	dtlsParameters: DtlsParameters
	sctpParameters?: SctpParameters
	iceServers?: Array<RTCIceServer>
	iceTransportPolicy?: RTCIceTransportPolicy
	additionalSettings?: object
	proprietaryConstraints?: object
	appData?: object
}

interface TransportProduceParameters {
	kind: MediaKind
	rtpParameters: RtpSendParameters
	appData?: object
}

interface TransportProduceDataParameters {
	sctpStreamParameters: SctpStreamParameters
	label?: string
	protocol?: string
	appData?: object
}
```

---

### Transport Listener Interface: Events (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Defines the `TransportListener` interface for handling events related to a media transport. Key events include `OnConnect`, which is triggered when a connection needs to be established with DTLS parameters, and `OnConnectionStateChange` to monitor changes in the transport's connection state.

```cpp
#include "libmediasoupclient/Transport.hpp"

class MyTransportListener : public mediasoupclient::Transport::Listener {
public:
    void OnConnect(mediasoupclient::Transport* transport, const mediasoupclient::DtlsParameters& dtlsParameters) override {
        // Handle connection establishment
    }

    void OnConnectionStateChange(mediasoupclient::Transport* transport, const std::string& connectionState) override {
        // Handle connection state changes
    }
};

```

---

### Observer Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Provides details on observer events for the WebRTC server, including when the server closes or when WebRTC transports are handled or unhandled.

```APIDOC
## Observer Events

### webRtcServer.observer.on("close", fn())

#### Description
Emitted when the WebRTC server is closed for any reason.

#### Method
Event Listener

### webRtcServer.observer.on("webrtctransporthandled", fn(webRtcTransport))

#### Description
Emitted when a new WebRTC transport that uses this WebRTC server is created.

#### Method
Event Listener

#### Arguments
- **webRtcTransport** (WebRtcTransport) - Handled WebRTC transport.

### webRtcServer.observer.on("webrtctransportunhandled", fn(webRtcTransport))

#### Description
Emitted when a new WebRTC transport that uses this WebRTC server is closed. It's also emitted for all WebRTC transports handled by this WebRTC server when the latter is closed.

#### Method
Event Listener

#### Arguments
- **webRtcTransport** (WebRtcTransport) - Unhandled WebRTC transport.
```

---

### Set Different Video Codecs per Producer (JavaScript)

Source: https://mediasoup.org/documentation/v3/tricks

Allows using distinct video codecs (e.g., H264 for webcam, VP8 for screen sharing) by specifying the 'codec' option in ProducerOptions. This requires the mediasoup Router to be configured with the desired codecs. The code filters available codecs based on MIME type.

```javascript
const webcamProducer = await sendTransport.produce({
	track: videoTrack,
	codec: device.rtpCapabilities.codecs.find((codec) => codec.mimeType.toLowerCase() === "video/h264"),
})

const sharingProducer = await sendTransport.produce({
	track: sharingTrack,
	codec: device.rtpCapabilities.codecs.find((codec) => codec.mimeType.toLowerCase() === "video/vp8"),
})
```

---

### Simulcast Consumer with 4 Spatial Layers

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Defines the `encodings` for a mediasoup consumer receiving a Simulcast stream configured with 4 spatial layers and 3 temporal layers using RIDs. The `scalabilityMode` reflects the total number of spatial and temporal layers.

```javascript
encodings: [{ ssrc: 222220, scalabilityMode: "L4T3" }]
```

---

### RouterRtpCodecCapability

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Defines the capabilities of a router for RTP codecs. It's similar to RtpCodecCapability but allows the preferredPayloadType to be optional.

```APIDOC
## RouterRtpCodecCapability

Same as RtpCodecCapability, however `preferredPayloadType` is optional.

### Fields

- **`kind`** (MediaKind) - Required - Media kind (“audio” or “video”).
- **`mimeType`** (String) - Required - The codec MIME media type/subtype (e.g. “audio/opus”, “video/VP8”).
- **`preferredPayloadType`** (Number) - Optional - The preferred RTP payload type.
- **`clockRate`** (Number) - Required - Codec clock rate expressed in Hertz.
- **`channels`** (Number) - Optional - The number of channels supported (e.g. two for stereo). Just for audio. Default: 1.
- **`parameters`** (Object) - Optional - Codec specific parameters. Some parameters (such as “packetization-mode” and “profile-level-id” in H264 or “profile-id” in VP9) are critical for codec matching.
- **`rtcpFeedback`** (Array<RtcpFeedback>) - Optional - Transport layer and codec-specific feedback messages for this codec. Default: `[ ]`.
```

---

### Producer API

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Methods for managing an active media producer, including setting spatial layers and RTP encoding parameters.

````APIDOC
## POST /producer/setMaxSpatialLayer

### Description
Limits the highest RTP stream being transmitted to the server in case of simulcast.

### Method
POST

### Endpoint
/producer/setMaxSpatialLayer

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
- **spatialLayer** (Number) - Required - The index of the entry in `encodings` representing the highest RTP stream that will be transmitted.

### Request Example
```json
{
  "spatialLayer": 1
}
````

### Response

#### Success Response (200)

- **message** (String) - Confirmation message.

#### Response Example

```json
{
	"message": "Spatial layer set successfully."
}
```

## POST /producer/setRtpEncodingParameters

### Description

Adds parameters to all `encodings` in the `RTCRtpSender` of the producer. Use with caution.

### Method

POST

### Endpoint

/producer/setRtpEncodingParameters

### Parameters

#### Path Parameters

None

#### Query Parameters

None

#### Request Body

- **params** (Object) - Required - Object with key-value pairs for RTP encoding parameters.

### Request Example

```json
{
	"params": {
		"networkPriority": "high"
	}
}
```

### Response

#### Success Response (200)

- **message** (String) - Confirmation message.

#### Response Example

```json
{
	"message": "RTP encoding parameters set successfully."
}
```

## Producer Events

### Description

Callbacks for various producer events, such as transport closure and track end.

### Method

Not Applicable (Event Listeners)

### Endpoint

Not Applicable

### Parameters

None

### Request Example

None

### Response

None

#### producer.on("transportclose", fn())

Emitted when the transport this producer belongs to is closed.

```javascript
producer.on("transportclose", () => {
	console.log("transport closed so producer closed")
})
```

#### producer.on("trackended", fn())

Emitted when the audio/video track being transmitted is externally stopped.

```javascript
producer.on("trackended", () => {
	console.log("track ended")
})
```

## Producer Observer Events

### Description

Events triggered through the producer's observer, including close, pause, resume, and track ended.

### Method

Not Applicable (Event Listeners)

### Endpoint

Not Applicable

### Parameters

None

### Request Example

None

### Response

None

#### producer.observer.on("close", fn())

Emitted when the producer is closed.

#### producer.observer.on("pause", fn())

Emitted when the producer is paused.

#### producer.observer.on("resume", fn())

Emitted when the producer is resumed.

#### producer.observer.on("trackended", fn())

Emitted when the audio/video track being transmitted is externally stopped.

````

--------------------------------

### Implement Custom Log Handler for Mediasoup

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Demonstrates how to implement a custom log handler by inheriting from Logger::LogHandlerInterface and overriding the OnLog method. This allows redirecting log messages to custom destinations like standard output. The handler receives log level, message payload, and message length as arguments.

```cpp
void MyLogHandler::OnLog(LogLevel level, char* payload, size_t len)
{
	std::cout << payload << std::endl;
}

````

---

### Enable Transport Trace Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Instructs the transport to emit 'trace' events for monitoring purposes. Use with caution. Accepts an array of TransportTraceEventType. An event listener can be attached to the 'trace' event.

```typescript
await transport.enableTraceEvent(["probation"])

transport.on("trace", (trace) => {
	// trace.type can just be "probation".
})
```

---

### Configure SRTP Endpoint as SDP Answerer in Mediasoup

Source: https://mediasoup.org/documentation/v3/communication-between-client-and-server

This process describes how to configure a mediasoup PlainTransport to act as an SDP answerer for an SRTP endpoint that only receives RTP. It involves creating the transport with specific options, generating the remote SDP offer, parsing the answer from the SRTP endpoint, and connecting the transport with SRTP parameters.

```javascript
const plainTransport = await router.createPlainTransport({
  comedia: true,
  rtcpMux: true,
  enable: true,
  // Optionally specify srtpCryptoSuite if needed
  // srtpCryptoSuite: "AES_CM_128_HMAC_SHA1_32"
});

// ... generate remote SDP offer from plainTransport.srtpParameters ...

// ... parse crypto suite and key material from SRTP endpoint's SDP answer ...

await plainTransport.connect({
  ip: "<SRTP_ENDPOINT_IP>",
  port: <SRTP_ENDPOINT_PORT>,
  sRtpParameters: { /* parsed srtpParameters */ }
});
```

---

### Send Direct Messages with DataProducer

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Demonstrates sending string and binary messages using the `dataProducer.send()` method. This method is available on direct transports. The `ppid`, `subchannels`, and `requiredSubchannel` arguments can be optionally provided to control message delivery.

```javascript
const stringMessage = "hello"
const binaryMessage = Buffer.from([1, 2, 3, 4])

dataProducer.send(stringMessage)
dataProducer.send(binaryMessage)
```

```javascript
dataProducer.send("bye", /*ppid*/ undefined, /*subchannels*/ [24])
```

---

### Consumer Methods: GetStats, GetAppData, IsClosed, IsPaused

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Provides methods to retrieve statistics, custom application data, and the current state (closed or paused) of a consumer. GetStats leverages RTCRtpReceiver's getStats(), while GetAppData returns a JSON object that can be modified.

```cpp
nlohmann::json stats = consumer.GetStats();
const nlohmann::json& appData = consumer.GetAppData();
bool isClosed = consumer.IsClosed();
bool isPaused = consumer.IsPaused();
```

---

### Load Device with Router RTP Capabilities

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Loads the mediasoup device with the RTP capabilities of the mediasoup router. This function prepares the device to know about allowed media codecs and other settings. It requires router RTP capabilities and optionally accepts a boolean to prefer local codec order. It is an asynchronous operation and may throw InvalidStateError or TypeError.

```javascript
await device.load({ routerRtpCapabilities })
// Now the device is ready.
```

---

### mediasoupClient.detectDevice() Function

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Detects the appropriate WebRTC handler for the current browser/device. This function is deprecated.

````APIDOC
## mediasoupClient.detectDevice(userAgent, userAgentData) Function

### Description
Performs current browser/device detection and returns the corresponding mediasoup-client WebRTC handler name (or nothing if the browser/device is not supported).

> `@deprecated`
> `@returns` BuiltinHandlerName | undefined

### Parameters

| Argument | Type | Description | Required | Default |
|---|---|---|---|---|
| `userAgent` | String | Optional browser User-Agent string. If not given, `navigator.userAgent` will be used (in case of browser). | No | |
| `userAgentData` | NavigatorUAData | Optional data obtained via `navigator.userAgentData()` in browsers supporting it. | No |   |

### Usage
```javascript
const handlerName = mediasoupClient.detectDevice();

if (handlerName) {
  console.log("detected handler: %s", handlerName);
} else {
  console.warn("no suitable handler found for current browser/device");
}
````

**Note:** This function is deprecated. Use `detectDeviceAsync()` instead.

````

--------------------------------

### Parse Scalability Mode String - C++

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Parses a string representing the scalability mode for video streams according to WebRTC SVC rules. It returns a JSON object specifying the number of spatial and temporal layers.

```cpp
mediasoupclient::parseScalabilityMode("L2T3");
// => { spatialLayers: 2, temporalLayers: 3 }

mediasoupclient::parseScalabilityMode("L4T7_KEY_SHIFT");
// => { spatialLayers: 4, temporalLayers: 7 }
````

---

### Create WebRTC Transport with WebRTC Server

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a new WebRTC transport associated with a WebRTC server. This method is asynchronous and returns a WebRtcTransport instance. It allows enabling UDP and disabling TCP for the transport.

```javascript
const transport = await router.createWebRtcTransport({
	webRtcServer: webRtcServer,
	enableUdp: true,
	enableTcp: false,
})
```

---

### SVC Consumer with 3 Spatial and 2 Temporal Layers

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Shows the `encodings` array for a mediasoup consumer receiving an SVC stream. The `scalabilityMode` matches the producer's configuration, indicating the available spatial and temporal layers.

```javascript
encodings: [{ ssrc: 222220, scalabilityMode: "L3T2" }]
```

---

### Router Transport Creation

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Methods for creating different types of transports on a router. These transports facilitate media and data flow.

````APIDOC
## POST /router/createPlainTransport

### Description
Creates a new plain transport for media or data transmission.

### Method
POST

### Endpoint
/router/createPlainTransport

### Parameters
#### Request Body
- **options** (PlainTransportOptions) - Required - Plain transport options, including `listenInfo`, `rtcpMux`, and `comedia`.
- **AppData** (AppData) - Optional - Custom application data.

### Request Example
```json
{
  "options": {
    "listenInfo": {
      "protocol": "udp",
      "ip": "a1:22:aA::08"
    },
    "rtcpMux": true,
    "comedia": true
  }
}
````

### Response

#### Success Response (200)

- **transport** (PlainTransport) - The created plain transport object.

#### Response Example

```json
{
	"transport": {
		"id": "some-transport-id",
		"type": "plain",
		"sctpParameters": null,
		"iceParameters": null,
		"iceCandidates": null,
		"dtlsParameters": null,
		"sctpState": null,
		"iceState": null,
		"connectionState": "new",
		"transportOptions": {
			"listenInfo": {
				"protocol": "udp",
				"ip": "a1:22:aA::08"
			},
			"rtcpMux": true,
			"comedia": true
		}
	}
}
```

## POST /router/createPipeTransport

### Description

Creates a new pipe transport for interconnecting routers.

### Method

POST

### Endpoint

/router/createPipeTransport

### Parameters

#### Request Body

- **options** (PipeTransportOptions) - Required - Pipe transport options, including `listenInfo`.
- **AppData** (AppData) - Optional - Custom application data.

### Request Example

```json
{
	"options": {
		"listenInfo": {
			"protocol": "udp",
			"ip": "192.168.1.33"
		}
	}
}
```

### Response

#### Success Response (200)

- **transport** (PipeTransport) - The created pipe transport object.

#### Response Example

```json
{
	"transport": {
		"id": "some-pipe-transport-id",
		"type": "pipe",
		"tuple": null,
		"remoteIp": null,
		"data": null,
		"pipeTransportOptions": {
			"listenInfo": {
				"protocol": "udp",
				"ip": "192.168.1.33"
			}
		}
	}
}
```

## POST /router/createDirectTransport

### Description

Creates a new direct transport.

### Method

POST

### Endpoint

/router/createDirectTransport

### Parameters

#### Request Body

- **options** (DirectTransportOptions) - Required - Direct transport options.
- **AppData** (AppData) - Optional - Custom application data.

### Request Example

```json
{
	"options": {}
}
```

### Response

#### Success Response (200)

- **transport** (DirectTransport) - The created direct transport object.

#### Response Example

```json
{
	"transport": {
		"id": "some-direct-transport-id",
		"type": "direct"
	}
}
```

````

--------------------------------

### WebRTC Transport Creation

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This section details the options available when creating a new WebRTC transport. A WebRTC transport facilitates network communication using ICE and DTLS procedures.

```APIDOC
## POST /websites/mediasoup_v3/transports

### Description
Creates a new WebRTC transport. A WebRTC transport represents a network path negotiated by both a WebRTC endpoint and mediasoup, via ICE and DTLS procedures. It can be used to send or receive media, or both.

### Method
POST

### Endpoint
/websites/mediasoup_v3/transports

### Parameters
#### Request Body
- **webRtcServer** (WebRtcServer) - Optional - Instead of opening its own listening port(s), let a WebRTC server handle the network traffic of this transport.
- **listenInfos** (Array<TransportListenInfo>) - Optional - Listening information in order of preference (first one is the preferred one).
- **listenIps** (Array<TransportListenIp|String>) - Optional - Listening IP address or addresses in order of preference (first one is the preferred one). DEPRECATED. Use `listenInfos` instead.
- **port** (Number) - Optional - Fixed port to listen on instead of selecting automatically from Worker's port range.
- **enableUdp** (Boolean) - Optional - Listen in UDP. Default: `true`.
- **enableTcp** (Boolean) - Optional - Listen in TCP. Default: `false`.
- **preferUdp** (Boolean) - Optional - Prioritize UDP. Default: `false`.
- **preferTcp** (Boolean) - Optional - Prioritize TCP. Default: `false`.
- **iceConsentTimeout** (Number) - Optional - ICE consent timeout in seconds. If 0, it is disabled. Default: 30.
- **initialAvailableOutgoingBitrate** (Number) - Optional - Initial available outgoing bitrate in bps. Applied if the consumer endpoint supports REMB or Transport-CC. Default: 600000.
- **enableSctp** (Boolean) - Optional - Create a SCTP association. Default: `false`.
- **numSctpStreams** (NumSctpStreams) - Optional - SCTP streams number.
- **maxSctpMessageSize** (Number) - Optional - Maximum allowed size for SCTP messages sent by `DataProducers`. Default: 262144.
- **sctpSendBufferSize** (Number) - Optional - SCTP send buffer size used by usrsctp. Default: 262144.
- **appData** (AppData) - Optional - Custom application data. Default: `{ }`.

*Note*: One of `webRtcServer`, `listenInfos`, or `listenIps` must be provided.
*Note*: If using `0.0.0.0` or `::` in `listenInfos` or `listenIps`, `announcedAddress` or `announcedIp` must also be provided in the corresponding entry.

### Request Example
```json
{
  "listenInfos": [
    {
      "ip": "192.168.1.100",
      "announcedAddress": "1.2.3.4"
    }
  ],
  "enableSctp": true,
  "maxSctpMessageSize": 524288
}
````

### Response

#### Success Response (200)

- **id** (String) - The unique identifier for the WebRTC transport.
- **iceParameters** (IceParameters) - ICE parameters needed for connection establishment.
- **dtlsParameters** (DtlsParameters) - DTLS parameters needed for secure connection establishment.
- **sctpParameters** (SctpParameters) - SCTP parameters if SCTP is enabled.
- **iceCandidates** (Array<IceCandidate>) - An array of ICE candidates.
- **dtlsRole** (DtlsRole) - The DTLS role assigned to this transport.

#### Response Example

```json
{
	"id": "transport-id-12345",
	"iceParameters": {
		"usernameFragment": "abcdef123456",
		"password": "securepassword",
		"iceLite": false
	},
	"dtlsParameters": {
		"role": "auto",
		"fingerprints": [
			{
				"algorithm": "sha-256",
				"value": "..."
			}
		]
	},
	"sctpParameters": {
		"port": 5000,
		"ossc": 1000,
		"mis": 0,
		"maxMessageSize": 262144,
		"sendBufferSize": 262144
	},
	"iceCandidates": [],
	"dtlsRole": "auto"
}
```

````

--------------------------------

### Create DirectTransport with Mediasoup v3

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a new direct transport. This method does not require any specific options and returns a DirectTransport object.

```typescript
const transport = await router.createDirectTransport();

````

---

### DataConsumer Properties

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This section details the properties of a DataConsumer object, providing information about its type, associated parameters, and state.

```APIDOC
## DataConsumer Properties

### `type`

The type of the data consumer.

*   **Type**: `DataProducerType`
*   **Read Only**: Yes

### `sctpStreamParameters`

The SCTP stream parameters. This property is only available if the data consumer `type` is 'sctp'.

*   **Type**: `SctpStreamParameters` or `Undefined`
*   **Read Only**: Yes

### `label`

The data consumer label.

*   **Type**: `String`
*   **Read Only**: Yes

### `protocol`

The data consumer sub-protocol.

*   **Type**: `String`
*   **Read Only**: Yes

### `paused`

Indicates whether the data consumer is currently paused.

*   **Type**: `Boolean`
*   **Read Only**: Yes

### `dataProducerPaused`

Indicates whether the associated data producer is paused.

*   **Type**: `Boolean`
*   **Read Only**: Yes

### `subchannels`

An array of unsigned 16-bit integers representing the subchannels the data consumer is currently subscribed to.

*   **Type**: `Array<Number>`
*   **Read Only**: Yes
*   **Note**: `subchannels` are used when receiving messages from a data producer created on a direct transport that specifies subchannels during `dataProducer.send()`.

### `appData`

Custom data provided by the application. This data can be modified at any time.

*   **Type**: `AppData`

### `observer`

An `EventEmitter` instance for observing events. Refer to the Observer Events section for more details.

*   **Type**: `EventEmitter`
*   **Read Only**: Yes
```

---

### AudioLevelObserver 'volumes' Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'volumes' event on an AudioLevelObserver. This event is emitted periodically (at most every 'interval' ms) with an array of audio producer volumes, ordered from loudest to quietest. Each entry includes the producer instance and its average volume in dBvo.

```javascript
audioLevelObserver.on("volumes", (volumes) => {
	volumes.forEach((volumeInfo) => {
		console.log(`Producer ${volumeInfo.producer.id} has volume ${volumeInfo.volume} dBvo`)
	})
})
```

---

### WebRTC Transport Details

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Provides detailed information about the structure of ICE and DTLS parameters used in WebRTC transports.

```APIDOC
## WebRTC Transport Parameters

### IceParameters
Represents the ICE parameters required for establishing a connection.
- **usernameFragment** (String) - Required: No - ICE username fragment.
- **password** (String) - Required: No - ICE password.
- **iceLite** (Boolean) - Required: No - Indicates if ICE Lite is enabled.

### IceCandidate
Represents a single ICE candidate.
- **foundation** (String) - Required: Yes - Unique identifier for correlating candidates across transports.
- **priority** (Number) - Required: Yes - The assigned priority of the candidate.
- **address** (String) - Required: Yes - The IP address or hostname of the candidate.
- **protocol** (String) - Required: Yes - The protocol of the candidate ("udp" or "tcp").
- **port** (Number) - Required: Yes - The port for the candidate.
- **type** (String) - Required: Yes - The type of candidate (always "host").
- **tcpType** (String) - Required: No - The type of TCP candidate (always "passive").

### DtlsParameters
Represents the DTLS parameters for securing the connection.
- **role** (DtlsRole) - Required: No - DTLS role. Default: "auto".
- **fingerprints** (Array<DtlsFingerprint>) - Required: Yes - An array of DTLS fingerprints.

### DtlsFingerprint
Represents a DTLS fingerprint, including the algorithm and its value.
- **algorithm** (String) - Required: Yes - Hash function algorithm (e.g., "sha-256").
- **value** (String) - Required: Yes - Certificate fingerprint value in lowercase hex string.
```

---

### transport.on('connect')

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Emitted when the transport is about to establish the ICE+DTLS connection. It requires exchanging local DTLS parameters with the associated server-side transport.

````APIDOC
## transport.on('connect')

### Description
Emitted when the transport is about to establish the ICE+DTLS connection and needs to exchange information with the associated server side transport.

### Method
Event Listener

### Endpoint
N/A

### Parameters
#### Arguments
- **dtlsParameters** (DtlsParameters) - Local DTLS parameters.
- **callback** (Function) - A function that must be called by the application once the parameters have been transmitted to the associated server side transport.
- **errback** (Function) - A function that must be called by the application (with the corresponding error) if the transmission of parameters to the associated server side transport failed for any reason.

### Request Example
```javascript
transport.on("connect", async ({ dtlsParameters }, callback, errback) =>
{
  try
  {
    await mySignaling.send(
      "transport-connect",
      {
        transportId    : transport.id,
        dtlsParameters : dtlsParameters
      });
    callback();
  }
  catch (error)
  {
    errback(error);
  }
});
````

### Response

#### Callback Response

- **No specific response fields**, callback() signals success, errback(error) signals failure.

````

--------------------------------

### Router: Create PlainTransport (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a PlainTransport instance within a Router. Plain transports are used for RTP/RTCP streams over UDP without WebRTC specifics. This method takes `PlainTransportOptions` and returns a `PlainTransport` object.

```javascript
import * as mediasoup from 'mediasoup';

async function createPlainTransportForRouter(router) {
  try {
    const plainTransportOptions = {
      listenIps: [
        { ip: '127.0.0.1', announcedIp: '127.0.0.1' },
      ],
      udpRtpPort: 1234,
      udpRtcpPort: 1235,
    };
    const transport = await router.createPlainTransport<any>(plainTransportOptions);
    console.log(`PlainTransport created for router ${router.id} with ID: ${transport.id}`);
    return transport;
  } catch (error) {
    console.error(`Failed to create PlainTransport for router ${router.id}:`, error);
    throw error;
  }
}

// Example usage (assuming 'myRouter' is an existing Router instance):
// createPlainTransportForRouter(myRouter).then(transport => {
//   // Transport is ready
// });
````

---

### DataConsumer Observer Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Events emitted by the DataConsumer's observer.

```APIDOC
## dataConsumer.observer.on("close", fn())

### Description
Emitted when the data consumer is closed for whatever reason.

### Method
ON

### Endpoint
dataConsumer.observer.on("close")

### Parameters
None

### Request Example
None
```

```APIDOC
## dataConsumer.observer.on("pause", fn())

### Description
Emitted when the data consumer is paused.

### Method
ON

### Endpoint
dataConsumer.observer.on("pause")

### Parameters
None

### Request Example
None
```

```APIDOC
## dataConsumer.observer.on("resume", fn())

### Description
Emitted when the data consumer is resumed.

### Method
ON

### Endpoint
dataConsumer.observer.on("resume")

### Parameters
None

### Request Example
None
```

---

### Consume Media Track with Transport

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Instructs the transport to receive an audio or video track from the mediasoup router. Requires ConsumerOptions, including id, producerId, kind, and rtpParameters. The consumer is created server-side and its parameters are signaled to the client.

```javascript
mySignaling.on("newConsumer", async (data) => {
	const consumer = await transport.consume({
		id: data.id,
		producerId: data.producerId,
		kind: data.kind,
		rtpParameters: data.rtpParameters,
	})

	// Render the remote video track into a HTML video element.
	const { track } = consumer

	videoElem.srcObject = new MediaStream([track])
})
```

---

### Update Runtime Settings for ICE and DTLS Logs

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

Allows changing mediasoup worker settings at runtime to specifically enable ICE and DTLS warning logs.

````APIDOC
## Update Runtime Settings for ICE and DTLS Logs

### Description
This method allows dynamically updating the logging configuration of a mediasoup worker at runtime. It's useful for enabling or disabling specific log levels and tags without restarting the worker.

### Method
Worker Method

### Endpoint
`worker.updateSettings(settings)`

### Parameters
#### Request Body
- **logLevel** (string) - Required - The desired logging level (e.g., 'debug', 'warn', 'error').
- **logTags** (array) - Required - An array of strings specifying the log tags to enable (e.g., ['ice', 'dtls']).

### Request Example
```javascript
worker.updateSettings({
  logLevel : "warn",
  logTags  : [ "ice", "dtls" ]
});
````

````

--------------------------------

### Handle Transport Trace Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'trace' event, which provides trace data as described by the enableTraceEvent() method.

```typescript
transport.on("trace", (trace) =>
{
  console.log(trace);
});

````

---

### DataProducer Class

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Represents a data producer.

```APIDOC
## DataProducer Class

### Description
Represents a data producer sending data over SCTP.

### Dictionaries
- **DataProducerOptions**: Options for creating a data producer.

### Properties
- **id**: (String) - The unique identifier of the data producer.
- **closed**: (Boolean) - Indicates if the data producer is closed.
- **sctpStreamParameters**: (Object) - SCTP stream parameters.
- **readyState**: (String) - The ready state of the data producer ('connecting', 'open', 'closing', 'closed').
- **label**: (String) - The label of the data producer.
- **protocol**: (String) - The protocol of the data producer.
- **bufferedAmount**: (Number) - The number of bytes of data that have been queued but not yet transmitted.
- **bufferedAmountLowThreshold**: (Number) - Threshold for `bufferedamountlow` event.
- **appData**: (Object) - Arbitrary application data.
- **observer**: (Object) - An observer for data producer events.

### Methods
- **dataProducer.close()**: Closes the data producer.
- **dataProducer.send(data)**: Sends data over the data producer.

### Events
- **dataProducer.on(“transportclose”, fn())**: Emitted when the associated transport is closed.
- **dataProducer.on(“open”)**: Emitted when the data producer is open.
- **dataProducer.on(“error”, fn(error))**: Emitted when an error occurs.
- **dataProducer.on(“close”)**: Emitted when the data producer is closed.
- **dataProducer.on(“bufferedamountlow”)**: Emitted when `bufferedAmount` is below the threshold.
```

---

### Set Mediasoupclient Log Handler (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

This C++ code demonstrates how to set a custom log handler for the mediasoupclient library using Logger::SetHandler. This allows applications to define their own logic for processing log messages, such as writing to a file or a custom console. The 'handler' argument should be a pointer to an object implementing the Logger::LogHandlerInterface.

```cpp
MyLogHandler handler;
mediasoupclient::Logger::SetHandler(&handler);
```

---

### Listen for New Consumer on Mediasoup Transport

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Attaches an event listener to the transport's observer to be notified when a new consumer is created. This is useful for tracking active media sinks. Requires a mediasoup transport instance.

```javascript
transport.observer.on("newconsumer", (consumer) => {
	console.log("new consumer created [id:%s]", consumer.id)
})
```

---

### RtcpFeedback

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Provides information on RTCP feedback messages for a specific codec, including transport layer and codec-specific messages.

````APIDOC
## RtcpFeedback

### Description
Provides information on RTCP feedback messages for a specific codec. Those messages can be transport layer feedback messages or codec-specific feedback messages. The list of RTCP feedbacks supported by mediasoup is defined in the supportedRtpCapabilities.ts file.

### Method
N/A (Object Definition)

### Endpoint
N/A

### Parameters
#### Body Parameters
- **type** (String) - Yes - RTCP feedback type.
- **parameter** (String) - Optional - RTCP feedback parameter.

### Request Example
```json
{
  "type": "nack",
  "parameter": "pli"
}
````

### Response

#### Success Response (200)

- **type** (String) - RTCP feedback type.
- **parameter** (String) - RTCP feedback parameter.

### Response Example

```json
{
	"type": "nack",
	"parameter": "pli"
}
```

````

--------------------------------

### PipeTransport Properties

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Properties of a PipeTransport instance.

```APIDOC
## PipeTransport Properties

### Description
Properties of a PipeTransport instance.

### Properties
#### pipeTransport.tuple
- Type: TransportTuple (read only)
- Description: The transport tuple. It refers to both RTP and RTCP since pipe transports use RTCP-mux by design.
  - Once the pipe transport is created, `transport.tuple` will contain information about its `localAddress`, `localPort` and `protocol`.
  - Information about `remoteIp` and `remotePort` will be set after calling `connect()` method.

#### pipeTransport.sctpParameters
- Type: SctpParameters (read only)
- Description: Local SCTP parameters. Or `undefined` if SCTP is not enabled.

#### pipeTransport.sctpState
- Type: SctpState (read only)
- Description: Current SCTP state. Or `undefined` if SCTP is not enabled.

#### pipeTransport.srtpParameters
- Type: SrtpParameters (read only)
- Description: Local SRTP parameters representing the crypto suite and key material used to encrypt sending RTP and SRTP. Those parameters must be given to the paired `pipeTransport` in the `connect()` method.
````

---

### Restart ICE Layer (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Restarts the ICE layer of the WebRTC transport, generating new local ICE parameters. These new parameters must then be signaled to the remote endpoint. This is an asynchronous operation.

```javascript
const iceParameters = await webRtcTransport.restartIce()

// Send the new ICE parameters to the endpoint.
```

---

### Cloning MediaStreamTrack for Producer

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Demonstrates how to clone a MediaStreamTrack before passing it to transport.produce() to ensure the original track remains usable after the producer is closed or replaced. This is necessary when `stopTracks` is true (the default) in the producer options.

```javascript
const clonedTrack = track.clone();
const producer = await transport.produce(clonedTrack, ...);
```

---

### Mediasoup Types Declaration (TypeScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Shows how to import and use mediasoup's public TypeScript types, such as Worker and RtpParameters. It also illustrates the alternative import method for specific types and the definition of a generic AppData type.

```typescript
import { types as mediasoupTypes } from "mediasoup"

let worker: mediasoupTypes.Worker
let rtpParameters: mediasoupTypes.RtpParameters

// or alternatively:

import { Worker, RtpParameters } from "mediasoup/node/lib/types"

let worker: Worker
let rtpParameters: RtpParameters

export type AppData = {
	[key: string]: unknown
}
```

---

### RtpSendParameters

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Describes a media stream received by mediasoup from an endpoint via a Producer. It can include a 'mid' for packet matching and supports single or multiple encodings (simulcast).

````APIDOC
## RtpSendParameters

### Description
The RTP send parameters describe a media stream received by mediasoup from an endpoint through its corresponding mediasoup Producer.

- These parameters may include a `mid` value that the mediasoup transport will use to match received RTP packets based on their MID RTP extension value.
- mediasoup allows RTP send parameters with a single encoding and with multiple encodings (simulcast). In the latter case, each entry in the `encodings` array must include a `ssrc` field or a `rid` field (the RID RTP extension value).
- If a single encoding is given, RTP send parameters must include `mid` value or the encoding must indicate the `ssrc` of the stream.
- If no encoding is given (so this is a simple stream without layers), then RTP send parameters must include `mid` value.

See the Simulcast and SVC sections for more information.

### Method
N/A (Object Definition)

### Endpoint
N/A

### Parameters
(Inherits from RtpParameters)

### Request Example
```json
{
  "mid": "0",
  "encodings": [
    { "ssrc": 12345 },
    { "ssrc": 67890, "rid": "r0" },
    { "ssrc": 11223, "rid": "r1" }
  ]
}
````

### Response

(Object Definition)

### Response Example

```json
{
	"mid": "0",
	"encodings": [{ "ssrc": 12345 }, { "ssrc": 67890, "rid": "r0" }, { "ssrc": 11223, "rid": "r1" }]
}
```

````

--------------------------------

### PipeTransport

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Documentation for the PipeTransport class, used for inter-router communication.

```APIDOC
## PipeTransport

### Description
A pipe transport represents a network path through which RTP, RTCP (optionally secured with SRTP) and SCTP (DataChannel) is transmitted. Pipe transports are intended to intercommunicate two Router instances collocated on the same host or on separate hosts.

When calling consume() on a pipe transport, all RTP streams of the Producer are transmitted verbatim (in contrast to what happens in WebRtcTransport and PlainTransport in which a single and continuous RTP stream is sent to the consuming endpoint).

### Dictionaries

* **`@inherits`**: Transport
````

---

### Producer Properties

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This section outlines the read-only properties of a Producer object, providing insights into its RTP parameters, type, status, score, application data, and observer.

```APIDOC
## Producer Properties

### `producer.rtpParameters`

Producer RTP parameters.

*   **Type**: RtpSendParameters (read only)
*   **Description**: Contains the RTP parameters associated with the producer. Refer to the RTP Parameters and Capabilities section for more details.

### `producer.type`

Producer type.

*   **Type**: ProducerType (read only)

### `producer.paused`

Indicates whether the producer is currently paused.

*   **Type**: Boolean (read only)

### `producer.score`

The transmission quality score for each RTP stream received by the producer.

*   **Type**: Array<ProducerScore> (read only)

### `producer.appData`

Custom data provided by the application. This can be modified at any time.

*   **Type**: AppData

### `producer.observer`

Provides access to the producer's event observer.

*   **Type**: EventEmitter (read only)
*   **Description**: See the Observer Events section below.
```

---

### RTPObserver Resume Method

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Resumes the RTP observer after it has been paused. Once resumed, the observer will again inspect RTP media. This method is asynchronous.

```javascript
await rtpObserver.resume()
```

---

### MediaKind Enum

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Defines the possible media kinds for RTP codecs and header extensions.

```APIDOC
## Enums

### MediaKind

- **“audio”**: Audio media kind.
- **“video”**: Video media kind.
```

---

### Parse Scalability Mode (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This utility function parses a scalability mode string into a structured object. It's helpful for understanding and validating different simulcast or SVC configurations. The input is a string, and the output is an object representing the parsed mode.

```javascript
import * as mediasoup from "mediasoup"

function parseScalabilityMode(modeString) {
	try {
		const parsedMode = mediasoup.parseScalabilityMode(modeString)
		console.log(`Parsed scalability mode for '${modeString}':`, parsedMode)
		return parsedMode
	} catch (error) {
		console.error(`Failed to parse scalability mode '${modeString}':`, error)
		throw error
	}
}

// Example usage:
parseScalabilityMode("simulcast")
parseScalabilityMode("svc/L33T/t24/r1280p/s30")
```

---

### Enable and Handle Nack Trace Events in Mediasoup v3 Consumer

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

Enables 'nack' (Negative Acknowledgement) trace events on a consumer and sets up a listener to receive and log the trace data. The 'nack' event signifies packet loss detection.

```javascript
consumer.enableTraceEvent([ 'nack' ]);

consumer.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "in",
    "info": {}
    "timestamp": 1546498145,
    "type": "nack"
  }
});
```

---

### SctpParameters Dictionary

Source: https://mediasoup.org/documentation/v3/mediasoup/sctp-parameters

Defines the parameters for an SCTP association, including port, outgoing streams, incoming streams, and maximum message size.

```APIDOC
## SctpParameters Dictionary

### Description
Parameters of the SCTP association.

### Fields
- **port** (Number) - Required - Must always equal 5000.
- **OS** (Number) - Required - Initially requested number of outgoing SCTP streams.
- **MIS** (Number) - Required - Maximum number of incoming SCTP streams.
- **maxMessageSize** (Number) - Required - Maximum allowed size for SCTP messages.
```

---

### RTP Observer Observer Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

These are common observer events for RTP observers, including 'close', 'pause', 'resume', 'addproducer', and 'removeproducer'. These events allow external monitoring and reaction to the state changes of the RTP observer and its associated producers.

```javascript
rtpObserver.observer.on("close", () => {
	console.log("RTP observer closed")
})

rtpObserver.observer.on("pause", () => {
	console.log("RTP observer paused")
})

rtpObserver.observer.on("resume", () => {
	console.log("RTP observer resumed")
})

rtpObserver.observer.on("addproducer", (producer) => {
	console.log(`Producer added: ${producer.id}`)
})

rtpObserver.observer.on("removeproducer", (producer) => {
	console.log(`Producer removed: ${producer.id}`)
})
```

---

### Produce Audio Stream in Mediasoup

Source: https://mediasoup.org/documentation/v3/communication-between-client-and-server

Creates an audio producer on a previously established plain transport. This involves specifying the media kind as 'audio' and defining the RTP parameters, including the Opus codec, clock rate, payload type, and SSRC.

```javascript
const audioProducer = await audioTransport.produce(
  {
    kind          : 'audio',
    rtpParameters :
    {
      codecs :
      [
        {
          mimeType     : 'audio/opus',
          clockRate    : 48000,
          payloadType  : 101,
          channels     : 2,
          rtcpFeedback : [ ],
          parameters   : { sprop-stereo: 1 }
        }
      ],
      encodings : [ { ssrc: 11111111 } ]
    }
  });

```

---

### ActiveSpeakerObserver Observer 'dominantspeaker' Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'dominantspeaker' event on the ActiveSpeakerObserver's observer interface. This event mirrors the 'dominantspeaker' event emitted directly by the ActiveSpeakerObserver, providing the dominant audio producer.

```javascript
activeSpeakerObserver.observer.on("dominantspeaker", (dominantSpeaker) => {
	console.log(`Observer notified: Dominant speaker is ${dominantSpeaker.producer.id}`)
})
```

---

### Observer Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Events emitted by the worker's observer, such as 'close', 'newrouter', and 'newwebrtcserver'.

````APIDOC
## Observer Events

See the Observer API section below.

### `worker.observer.on(“close”, fn())`

Emitted when the worker is closed for whatever reason.

### `worker.observer.on(“newrouter”, fn(router))`

Emitted when a new router is created.

#### Parameters

- **router** (Router) - New router.

```javascript
worker.observer.on("newrouter", (router) =>
{
  console.log("new router created [id:%s]", router.id);
});
````

### `worker.observer.on(“newwebrtcserver”, fn(router))`

Emitted when a new router is created.

#### Parameters

- **webRtcServer** (WebRtcServer) - New WebRTC server.

```javascript
worker.observer.on("newwebrtcserver", (webRtcServer) => {
	console.log("new WebRTC server created [id:%s]", webRtcServer.id)
})
```

````

--------------------------------

### Detect Browser/Device Handler (Deprecated)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Demonstrates the usage of the deprecated `detectDevice` function to identify the appropriate mediasoup-client WebRTC handler for the current browser or device. It logs the handler name or a warning if none is found. This function uses `navigator.userAgent` if no user agent is provided.

```javascript
const handlerName = mediasoupClient.detectDevice();

if (handlerName) {
  console.log("detected handler: %s", handlerName);
} else {
  console.warn("no suitable handler found for current browser/device");
}

````

---

### SVC Producer with 3 Spatial and 2 Temporal Layers

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Illustrates the `encodings` configuration for a mediasoup SVC producer. It specifies a single encoding with a `scalabilityMode` indicating 3 spatial and 2 temporal layers.

```javascript
encodings: [{ ssrc: 111110, scalabilityMode: "L3T2" }]
```

---

### Create Receiving WebRTC Transport

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Creates a new WebRTC transport instance on the client side for receiving media. This transport must correspond to a WebRtcTransport previously created on the mediasoup router. It requires transport options including ID, ICE parameters, DTLS parameters, and potentially SCTP parameters. This operation is asynchronous and requires the device to be loaded.

```javascript
const transport = device.createRecvTransport(
  {
    id             : "152f60cd-10ac-443b-8529-6474ecba2e44",
    iceParameters  : { ... },
    iceCandidates  : [ ... ],
    dtlsParameters : { ... },
    sctpParameters : { ... }
  });

```

---

### Transport

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Details about the Transport instance in mediasoup-client, representing the local side of a WebRtcTransport and handling media communication.

````APIDOC
## Transport

A `Transport` instance in mediasoup-client represents the local side of a WebRtcTransport in mediasoup server. It connects a mediasoup-client Device with a mediasoup Router at media level and enables the sending of media (by means of Producer instances) **or** the receiving of media (by means of Consumer instances).
Internally, the transport holds a WebRTC RTCPeerConnection instance.

### Observer Events
See the Observer API section below.
#### device.observer.on("newtransport", fn(transport))

### Description
Emitted when a new transport is created.

### Method
`device.observer.on("newtransport", fn(transport))`

### Parameters
#### Path Parameters
- **transport** (Transport) - New transport.

### Request Example
```javascript
device.observer.on("newtransport", (transport) =>
{
  console.log("new transport created [id:%s]", transport.id);
});
````

### Dictionaries

(Further details on dictionaries would follow here if provided in the source text)

````

--------------------------------

### Produce Video Stream in Mediasoup

Source: https://mediasoup.org/documentation/v3/communication-between-client-and-server

Creates a video producer on a designated plain transport. It sets the media kind to 'video' and defines the RTP parameters, including the VP8 codec, clock rate, payload type, and SSRC. Note that FFmpeg may not support all RTCP feedback mechanisms like NACK or PLI/FIR.

```javascript
const videoProducer = await videoTransport.produce(
  {
    kind          : 'video',
    rtpParameters :
    {
      codecs :
      [
        {
          mimeType     : 'video/vp8',
          clockRate    : 90000,
          payloadType  : 102,
          rtcpFeedback : [ ], // FFmpeg does not support NACK nor PLI/FIR.
        }
      ],
      encodings : [ { ssrc: 22222222 } ]
    }
  });

````

---

### RecvTransport::Consume

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Instructs the transport to receive an audio or video track from the mediasoup router.

````APIDOC
## RecvTransport::Consume

### Description
Instructs the transport to receive an audio or video track to the mediasoup router. This method is asynchronous and blocks the current thread until completion.

### Method
`recvTransport->Consume()`

### Endpoint
N/A (This is a method call, not a REST endpoint)

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
None

#### Arguments
- **listener** (Consumer::Listener) - Consumer listener.
- **id** (const std::string&) - The identifier of the server-side consumer.
- **producerId** (const std::string&) - The identifier of the server-side producer being consumed.
- **kind** (const std::string&) - Media kind ("audio" or "video").
- **rtpParameters** (const nlohmann::json* RtpReceiveParameters) - Receive RTP parameters.
- **appData** (nlohmann::json) - Custom application data. (Optional, defaults to `{}`)

### Request Example
```cpp
auto* consumerListener = new MyConsumerListener();

// This will block the current thread until completion.
auto* consumer = recvTransport->Consume(
  consumerListener,
  id,
  producerId,
  kind,
  rtpParameters);
````

### Response

#### Success Response (200)

- **consumer** (Consumer\*) - Pointer to the created Consumer instance.

#### Response Example

```cpp
// consumer is a pointer to a Consumer object
```

````

--------------------------------

### Create Sending WebRTC Transport

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Creates a new WebRTC transport instance on the client side for sending media. This transport must correspond to a WebRtcTransport previously created on the mediasoup router. It requires transport options including ID, ICE parameters, DTLS parameters, and potentially SCTP parameters. This operation is asynchronous and requires the device to be loaded.

```javascript
const transport = device.createSendTransport(
  {
    id             : "0b38d662-ea00-4c70-9ae3-b675d6a89e09",
    iceParameters  : { ... },
    iceCandidates  : [ ... ],
    dtlsParameters : { ... },
    sctpParameters : { ... }
  });

````

---

### Worker: Create Router (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a new Router instance associated with a specific Worker. Routers are responsible for managing media. This method takes an optional `RouterAppData` for custom data. It returns a `Router` object.

```javascript
import * as mediasoup from 'mediasoup';

interface MyRouterAppData {
  routerName: string;
}

async function createRouterForWorker(worker) {
  try {
    const routerOptions = {
      mediaCodecs: [
        { kind: 'audio', mimeType: 'audio/opus', clockRate: 48000, channels: 2 },
        { kind: 'video', mimeType: 'video/vp8', clockRate: 90000, parameters: { important: true } },
      ]
    };
    const router = await worker.createRouter<MyRouterAppData>(routerOptions);
    router.appData = { routerName: 'main-router' };
    console.log(`Router created for worker ${worker.pid} with ID: ${router.id}`);
    return router;
  } catch (error) {
    console.error(`Failed to create router for worker ${worker.pid}:`, error);
    throw error;
  }
}

// Example usage (assuming 'myWorker' is an existing Worker instance):
// createRouterForWorker(myWorker).then(router => {
//   // Router is ready
// });
```

---

### Worker Observer: New WebRtcServer Event (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'newwebrtcserver' event on the worker's observer. This event is emitted when a new WebRtcServer is created using `worker.createWebRtcServer()`. The listener receives the newly created `WebRtcServer` object.

```javascript
import * as mediasoup from "mediasoup"

async function setupNewWebRtcServerListener(worker) {
	worker.observer.on("newwebrtcserver", (webRtcServer) => {
		console.log(`New WebRtcServer created for worker ${worker.pid}: ${webRtcServer.id}`)
		// You can now interact with the new WebRtcServer
	})
	console.log(`Listening for 'newwebrtcserver' events on worker ${worker.pid}.`)
}

// Example usage (assuming 'myWorker' is an existing Worker instance):
// setupNewWebRtcServerListener(myWorker);
// // Later, to trigger the event:
// // await worker.createWebRtcServer();
```

---

### Router Properties

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Properties available on a Router instance, including its ID, closed status, RTP capabilities, application data, and observer.

````APIDOC
### Router Properties

#### router.id
Router identifier.
> `@type` String, read only
```javascript
console.log(router.id);
// => "15177e19-5665-4eba-9a6a-c6cf3db16259"
````

#### router.closed

Whether the router is closed.

> `@type` Boolean, read only

#### router.rtpCapabilities

An Object with the RTP capabilities of the router. These capabilities are typically needed by mediasoup clients to compute their sending RTP parameters.

> `@type` RtpCapabilities, read only

- Check the RTP Parameters and Capabilities section for more details.
- See also how to filter these RTP capabilities before using them into a client.

#### router.appData

Custom data provided by the application in the worker factory method. The app can modify it at any time.

> `@type` AppData

#### router.observer

See the Observer Events section below.

> `@type` EventEmitter, read only

````

--------------------------------

### Enable and Handle Keyframe Trace Events in Mediasoup v3 Consumer

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

Enables 'keyframe' trace events on a consumer and provides an event listener to process the trace data. When 'keyframe' is enabled, only 'keyframe' type traces are emitted, not 'rtp' traces for keyframes. The trace object contains detailed information about the keyframe.

```javascript
consumer.enableTraceEvent([ 'keyframe' ]);

consumer.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "out",
    "info": {
      "isKeyFrame": true,
      "marker": "true",
      "payloadSize": 437,
      "payloadType": 101,
      "sequenceNumber": 1,
      "size": 465,
      "spatialLayer": 0,
      "ssrc": 185272966,
      "temporalLayer": 0,
      "timestamp": 936997226,
      "wideSequenceNumber": 17
    },
    "timestamp": 1514298020,
    "type": "keyframe"
  }
});
````

---

### Set Mediasoupclient Log Level (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

This C++ code snippet shows how to set the global log level for the mediasoupclient library using the Logger::SetLogLevel function. The 'level' argument specifies the desired logging verbosity, with options like LOG_DEBUG, LOG_WARN, and LOG_ERROR. This configuration is essential for controlling the amount of log output generated by the library.

```cpp
mediasoupclient::Logger::SetLogLevel(mediasoupclient::LogLevel::LOG_DEBUG);
```

---

### Enable and Handle PLI Trace Events in Mediasoup v3 Consumer

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

Enables 'pli' (Picture Loss Indication) trace events on a consumer and provides an event handler for these events. PLI requests a new keyframe from the encoder when the decoder detects picture loss.

```javascript
consumer.enableTraceEvent([ 'pli' ]);

consumer.on('trace', (trace) =>
{
  // trace =>
  {
    "direction": "in",
    "info": {
      "ssrc": 5698432
    }
    "timestamp": 1544798444,
    "type": "pli"
  }
});
```

---

### Router: Create PipeTransport (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a PipeTransport instance within a Router. Pipe transports are used for piping media between different routers, potentially across different workers or processes. This method takes `PipeTransportOptions` and returns a `PipeTransport` object.

```javascript
import * as mediasoup from "mediasoup"

async function createPipeTransportForRouter(router) {
	try {
		const pipeTransportOptions = {
			// PipeTransport specific options
		}
		const transport = (await router.createPipeTransport) < any > pipeTransportOptions
		console.log(`PipeTransport created for router ${router.id} with ID: ${transport.id}`)
		return transport
	} catch (error) {
		console.error(`Failed to create PipeTransport for router ${router.id}:`, error)
		throw error
	}
}

// Example usage (assuming 'myRouter' is an existing Router instance):
// createPipeTransportForRouter(myRouter).then(transport => {
//   // Transport is ready
// });
```

---

### Router: Create DirectTransport (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Creates a DirectTransport instance within a Router. Direct transports are used for sending media directly to or receiving media directly from external endpoints, bypassing typical RTP/RTCP stream handling.

```javascript
import * as mediasoup from "mediasoup"

async function createDirectTransportForRouter(router) {
	try {
		const directTransportOptions = {
			// DirectTransport specific options
		}
		const transport = (await router.createDirectTransport) < any > directTransportOptions
		console.log(`DirectTransport created for router ${router.id} with ID: ${transport.id}`)
		return transport
	} catch (error) {
		console.error(`Failed to create DirectTransport for router ${router.id}:`, error)
		throw error
	}
}

// Example usage (assuming 'myRouter' is an existing Router instance):
// createDirectTransportForRouter(myRouter).then(transport => {
//   // Transport is ready
// });
```

---

### Replace Producer Track (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Demonstrates how to replace the current media track of a producer with a new one using the `replaceTrack` method. This operation is asynchronous and does not require server-side negotiation. It's useful for switching camera sources or media types dynamically.

```javascript
const stream = await navigator.mediaDevices.getUserMedia({ video: true })
const newVideoTrack = stream.getVideoTracks()[0]

await producer.replaceTrack({ track: newVideoTrack })
```

---

### Mediasoup Transport Methods: close() and getStats()

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Demonstrates the usage of the `close()` method to terminate a transport and all its associated producers/consumers, and the asynchronous `getStats()` method to retrieve transport statistics from the underlying RTCPeerConnection.

```javascript
transport.close()

async function getTransportStats(transport) {
	const stats = await transport.getStats()
	console.log(stats)
}
```

---

### C++ TransportListener OnConnect Event Handler

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Handles the TransportListener::OnConnect event, called when the transport is about to establish an ICE+DTLS connection. It exchanges DTLS parameters with the server-side transport. This function is asynchronous and returns a std::future<void>.

```cpp
std::future<void> MyTransportListener::OnConnect(
  mediasoupclient::Transport transport,
  const json& dtlsParameters
)
{
	std::promise<void> promise;

	json body =
	{
		{ "transportId",    transport->GetId() },
		{ "dtlsParameters", dtlsParameters     }
	};

	// Signal local DTLS parameters to the server side transport.
	mySignaling.send("transport-connect", body);

	// [...] Let's assume code execution continues once we get a success response
	// from the server.

	// Fulfil the promise and return its future.
	promise.set_value();

	return promise.get_future();
}
```

---

### ActiveSpeakerObserver Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Details the events emitted by the ActiveSpeakerObserver, specifically the dominant speaker event.

```APIDOC
## ActiveSpeakerObserver API

### Description
Provides event handling for dominant speaker detection.

### Events
- `dominantspeaker`: Emitted with the dominant speaker information.

### Observer Events
- `dominantspeaker`: Emitted with the dominant speaker information.
```

---

### Simulcast Producer with 3 SSRCs

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Defines the `encodings` array for a mediasoup producer utilizing Simulcast with 3 separate SSRCs. Each SSRC represents a different quality layer of the same video source.

```javascript
encodings: [{ ssrc: 111110 }, { ssrc: 111111 }, { ssrc: 111112 }]
```

---

### Logger Class: Log Level and Handler Configuration (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

The `Logger` class provides functionality for controlling logging within mediasoupclient. It allows setting the log level, assigning a custom log handler, or resetting to the default handler. The `LogLevel` enum defines different severity levels for logs.

```cpp
#include "libmediasoupclient/Logger.hpp"

// Example usage:
// Set log level to debug
// mediasoupclient::Logger::SetLogLevel(mediasoupclient::LogLevel::LOG_DEBUG);

// Set a custom log handler (requires implementing Logger::LogHandlerInterface)
// MyLogHandler handler;
// mediasoupclient::Logger::SetHandler(&handler);

// Reset to default handler
// mediasoupclient::Logger::SetDefaultHandler();

```

---

### Enabling mediasoup Logging in Node.js Application

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

This code snippet demonstrates how to enable mediasoup logging programmatically within a Node.js application by setting the process.env.DEBUG variable before requiring the mediasoup module. This approach is useful for configuring logging within the application itself.

```javascript
process.env.DEBUG = "mediasoup*"

const mediasoup = require("mediasoup")
```

---

### Callback Type for RTCRtpReceiver Creation (TypeScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Defines the type for a callback function that is invoked synchronously right after a new RTCRtpReceiver is created. This allows for the creation of encoded streams in browsers that support this feature.

```typescript
type OnRtpReceiverCallback = (rtpReceiver: RTCRtpReceiver) => void
```

---

### Listen for New Producer on Mediasoup Transport

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Attaches an event listener to the transport's observer to be notified when a new producer is created. This is useful for tracking active media sources. Requires a mediasoup transport instance.

```javascript
transport.observer.on("newproducer", (producer) => {
	console.log("new producer created [id:%s]", producer.id)
})
```

---

### Observe New Worker Event - JavaScript

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'newworker' event emitted by the mediasoup observer. Logs the process ID (pid) of the newly created worker. Requires the mediasoup observer to be initialized.

```javascript
mediasoup.observer.on("newworker", (worker) => {
	console.log("new worker created [pid:%d]", worker.pid)
})
```

---

### Parse Scalability Mode - JavaScript

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Parses a given scalabilityMode string into spatial and temporal layers. Handles undefined input by defaulting to 1 spatial and 1 temporal layer. Useful for configuring video stream parameters.

```javascript
mediasoup.parseScalabilityMode("L2T3")
// => { spatialLayers: 2, temporalLayers: 3 }

mediasoup.parseScalabilityMode("S3T3")
// => { spatialLayers: 3, temporalLayers: 3 }

mediasoup.parseScalabilityMode("L4T7_KEY_SHIFT")
// => { spatialLayers: 4, temporalLayers: 7 }

mediasoup.parseScalabilityMode(undefined)
// => { spatialLayers: 1, temporalLayers: 1 }
```

---

### Pipe to Router Result

Source: https://mediasoup.org/documentation/v3/mediasoup/api

The result of piping a producer to another router, containing the created consumer and producer/dataProducer references.

```APIDOC
## PipeToRouterResult

### Fields
- **pipeConsumer** (Consumer) - Required: No - The consumer created in the current router.
- **pipeProducer** (Producer) - Required: No - The producer created in the target router.
- **pipeDataConsumer** (DataConsumer) - Required: No - The data consumer created in the current router.
- **pipeDataProducer** (DataProducer) - Required: No - The data producer created in the target router.
```

---

### Consumer Events: Transport and Producer Status

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Illustrates how to listen for events related to the consumer's associated transport and producer. These events, such as 'transportclose', 'producerclose', 'producerpause', and 'producerresume', are vital for reacting to external changes that affect the consumer's state.

```javascript
consumer.on("transportclose", () => {
	console.log("transport closed so consumer closed")
})

consumer.on("producerclose", () => {
	console.log("associated producer closed so consumer closed")
})

consumer.on("producerpause", () => {})

consumer.on("producerresume", () => {})
```

---

### TransportProduceDataParameters

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Parameters for producing data on a transport.

```APIDOC
## Transport Produce Data Parameters

### Description
Parameters for producing data on a transport.

### Parameters
#### Request Body
- **sctpStreamParameters** (SctpStreamParameters) - Data producer's SCTP stream parameters.
- **label** (String) - DataChannel label.
- **protocol** (String) - DataChannel protocol.
- **appData** (Object) - Custom application data as given in the `transport.produceData()` method.
```

---

### Simulcast Consumer with 3 Spatial Layers

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Represents the `encodings` array for a mediasoup consumer receiving a Simulcast stream. The `scalabilityMode` indicates the number of spatial layers (simulcast streams) and temporal layers.

```javascript
encodings: [{ ssrc: 222220, scalabilityMode: "L3T1" }]
```

---

### Worker Properties

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Access custom application data and the event observer for the worker.

```APIDOC
## Worker Properties

### `worker.appData`

Custom data provided by the application in the worker factory method. The app can modify it at any time.

> `@type` AppData

### `worker.observer`

See the Observer Events section below.

> `@type` EventEmitter, read only
```

---

### Consumer Trace Event Types

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

Details the information provided by 'trace' events when enabled on a Consumer, specifically for 'rtp' type.

````APIDOC
## Consumer Trace Event Details

### Description
This section details the structure of the 'trace' event payload when enabled on a mediasoup Consumer, focusing on the 'rtp' event type.

### Consumer Trace Event Types

#### 'rtp'
Enables tracing for standard RTP packets received by the consumer.

##### Event Example
```javascript
consumer.on('trace', (trace) => {
  // trace =>
  {
    "direction": "out",
    "info": {
      "isKeyFrame": false,
      "marker": "false",
      "payloadSize": 1,
      "payloadType": 100,
      "sequenceNumber": 6,
      "size": 21,
      "spatialLayer": 0,
      "ssrc": 198373608,
      "temporalLayer": 0,
      "timestamp": 54740510
    },
    "timestamp": 1514273430,
    "type": "rtp"
  }
});
````

````

--------------------------------

### NumSctpStreams Dictionary

Source: https://mediasoup.org/documentation/v3/mediasoup/sctp-parameters

Specifies the number of outgoing and incoming SCTP streams, with default values and explanations for their usage in the SCTP handshake.

```APIDOC
## NumSctpStreams Dictionary

### Description
Specifies the number of outgoing and incoming SCTP streams. Both `OS` and `MIS` are part of the SCTP INIT+ACK handshake. `OS` refers to the initial number of outgoing SCTP streams that the server side transport creates (to be used by DataConsumers), while `MIS` refers to the maximum number of incoming SCTP streams that the server side transport can receive (to be used by DataProducers).

### Fields
- **OS** (Number) - Optional - Initially requested number of outgoing SCTP streams (from 1 to 65535). Default: 1024.
- **MIS** (Number) - Optional - Maximum number of incoming SCTP streams (from 1 to 65535). Default: 1024.

### Notes
- If the server side transport will just be used to create data producers (but no data consumers), `OS` can be low (~1).
- If data consumers are desired on the server side transport, `OS` must have a proper value and such a proper value depends on whether the remote endpoint supports `SCTP_ADD_STREAMS` extension or not.
  - libwebrtc (Chrome, Safari, etc) does not enable `SCTP_ADD_STREAMS` so, if data consumers are required, `OS` should be 1024 (the maximum number of DataChannels that libwebrtc enables).
  - Firefox does enable `SCTP_ADD_STREAMS` so, if data consumers are required, `OS` can be lower (16 for instance). The mediasoup transport will allocate and announce more outgoing SCTP streams when needed.
  - mediasoup-client provides specific per browser/version `OS` and `MIS` values via the device.sctpCapabilities getter.
````

---

### DataProducer Methods: IsClosed, Close

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Methods for managing the state of a data producer. IsClosed checks if the data producer has been closed, and Close terminates the data producer, preventing further data transmission.

```cpp
bool isClosed = dataProducer.IsClosed();
dataProducer.Close();
```

---

### Producer Enums

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Enumerations used for Producer types and trace event types.

```APIDOC
### Enums

#### `ProducerType`

Specifies the type of RTP stream received by the producer.

| Value     | Description                                                     |
|-----------|-----------------------------------------------------------------|
| "simple"  | A single RTP stream is received with no spatial/temporal layers. |
| "simulcast" | Two or more RTP streams are received, each with one or more temporal layers. |
| "svc"     | A single RTP stream is received with spatial/temporal layers.    |

#### `ProducerTraceEventType`

Defines the types of trace events that can occur for a producer.

| Value     | Description          |
|-----------|----------------------|
| "rtp"     | RTP packet.          |
| "keyframe"| RTP video keyframe packet. |
| "nack"    | RTCP NACK packet.    |
| "pli"     | RTCP PLI packet.     |
| "fir"     | RTCP FIR packet.     |
| "sr"      | RTCP Sender Report.  |

```

---

### DataConsumer Buffered Amount Low Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'bufferedamountlow' event on a data consumer, applicable only for 'sctp' type consumers. This event signifies that the number of bytes buffered in the underlying SCTP association has dropped below the `bufferedAmountLowThreshold`. The event callback receives the current buffered amount.

```javascript
dataConsumer.on("bufferedamountlow", (bufferedAmount) => {
	console.log("SCTP buffered amount is low:", bufferedAmount)
})
```

---

### SendTransportListener::OnProduce

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Emitted when the transport needs to transmit information about a new producer to the associated server-side transport. This event occurs before the `produce()` method completes.

````APIDOC
## SendTransportListener::OnProduce(transport, kind, rtpParameters, appData)

### Description
Emitted when the transport needs to transmit information about a new producer to the associated server-side transport. This event occurs before the `produce()` method completes. The application should call `transport.produce()` on the server side.

### Method
POST

### Endpoint
/websites/mediasoup_v3

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
- **transport** (SendTransport*) - Required - SendTransport instance.
- **kind** (string) - Required - Producer's media kind ("audio" or "video").
- **rtpParameters** (object) - Required - Producer's RTP parameters.
- **appData** (object) - Required - Custom application data as given in the `transport.produce()` method.

### Request Example
```json
{
  "transport": "<SendTransport*>",
  "kind": "video",
  "rtpParameters": {
    "codecs": [
      {
        "mimeType": "video/vp8",
        "clockRate": 90000,
        "payloadType": 96
      }
    ],
    "encodings": [
      {
        "ssrc": 123456
      }
    ]
  },
  "appData": {
    "roomId": "abcde"
  }
}
````

### Response

#### Success Response (200)

- **ID** (string) - The ID of the producer created on the server side.

#### Response Example

```json
{
	"id": "producer-id-12345"
}
```

````

--------------------------------

### Producer Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This section describes the events emitted by the Producer object, including transport closure, score changes, video orientation changes, trace events, and listener errors.

```APIDOC
## Producer Events

### `producer.on('transportclose', fn())`

Emitted when the producer's associated transport is closed. The producer is also closed, and a `producerclose` event is triggered on all associated consumers.

**Example**:
```javascript
producer.on("transportclose", () => {
  console.log("transport closed so producer closed");
});
````

### `producer.on('score', fn(score))`

Emitted when the producer's score changes.

- **Arguments**:
  - `score` (Array<ProducerScore>): The RTP streams' scores.

### `producer.on('videoorientationchange', fn(videoOrientation))`

Emitted when the video orientation changes. This event is only possible if the `"urn:3gpp:video-orientation"` RTP extension has been negotiated.

- **Arguments**:
  - `videoOrientation` (ProducerVideoOrientation): The new video orientation.

### `producer.on('trace', fn(trace))`

Emitted for trace events. See the `enableTraceEvent()` method documentation.

- **Arguments**:
  - `trace` (ProducerTraceEventData): The trace data.

**Example**:

```javascript
producer.on("trace", (trace) => {
	console.log(trace)
})
```

### `producer.on('listenererror', fn(eventName, error))`

Emitted when an event listener provided by the application throws an error. The exception is silently ignored internally to maintain internal state. Listening to this event allows the application to be aware of exceptions occurring in its event listeners.

- **Arguments**:
  - `eventName` (String): The name of the event.
  - `error` (Error): The error thrown by the application's event listener.

````

--------------------------------

### Listen for New Data Producer on Mediasoup Transport

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Attaches an event listener to the transport's observer to be notified when a new data producer is created. This is useful for tracking active data sources. Requires a mediasoup transport instance.

```javascript
transport.observer.on("newdataproducer", (dataProducer) =>
{
  console.log("new data producer created [id:%s]", dataProducer.id);
});

````

---

### DataConsumer: Update Subchannels

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Shows how to update the list of subchannels a DataConsumer is subscribed to using `setSubchannels`. This is relevant when receiving messages from a data producer configured with specific subchannels.

```javascript
dataConsumer.setSubchannels([1, 4])
```

---

### DataConsumer Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Events emitted by the DataConsumer instance related to its transport and associated data producer.

````APIDOC
## dataConsumer.on("transportclose", fn())

### Description
Emitted when the transport this data consumer belongs to is closed for whatever reason. The data consumer itself is also closed.

### Method
ON

### Endpoint
dataConsumer.on("transportclose")

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
None

### Request Example
```javascript
dataConsumer.on("transportclose", () =>
{
  console.log("transport closed so dataConsumer closed");
});
````

### Response

#### Success Response (200)

N/A (Event Listener)

#### Response Example

N/A

````

```APIDOC
## dataConsumer.on("dataproducerclose", fn())

### Description
Emitted when the associated data producer is closed for whatever reason. The data consumer itself is also closed.

### Method
ON

### Endpoint
dataConsumer.on("dataproducerclose")

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
None

### Request Example
```javascript
dataConsumer.on("dataproducerclose", () =>
{
  console.log("associated data producer closed so dataConsumer closed");
});
````

### Response

#### Success Response (200)

N/A (Event Listener)

#### Response Example

N/A

````

```APIDOC
## dataConsumer.on("dataproducerpause", fn())

### Description
Emitted when the associated data producer is paused.

### Method
ON

### Endpoint
dataConsumer.on("dataproducerpause")

### Parameters
None

### Request Example
None
````

```APIDOC
## dataConsumer.on("dataproducerresume", fn())

### Description
Emitted when the associated data producer is resumed.

### Method
ON

### Endpoint
dataConsumer.on("dataproducerresume")

### Parameters
None

### Request Example
None
```

````APIDOC
## dataConsumer.on("message", fn(message, ppid))

### Description
Emitted when a message has been received from the corresponding data producer. This is only available in direct transports created via `router.createDirectTransport()`.

### Method
ON

### Endpoint
dataConsumer.on("message")

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
None

### Request Example
```javascript
dataConsumer.on("message", (message, ppid) =>
{
  if (ppid === 51)
    console.log("text message received:", message.toString("utf-8"));
  else if (ppid === 53)
    console.log("binary message received");
});
````

### Response

#### Success Response (200)

- **message** (Buffer) - Received message.
- **ppid** (Number) - Mimics the SCTP Payload Protocol Identifier. Typically 51 (`WebRTC String`) if message is a String and 53 (`WebRTC Binary`) if it's a Buffer.

#### Response Example

N/A

````

```APIDOC
## dataConsumer.on("sctpsendbufferfull")

### Description
Emitted when a message could not be sent because the SCTP send buffer was full.

### Method
ON

### Endpoint
dataConsumer.on("sctpsendbufferfull")

### Parameters
None

### Request Example
None
````

```APIDOC
## dataConsumer.on("bufferedamountlow", fn(bufferedAmount))

### Description
Emitted when the underlaying SCTP association buffered bytes drop down to `bufferedAmountLowThreshold`. Only applicable for consumers of type 'sctp'.

### Method
ON

### Endpoint
dataConsumer.on("bufferedamountlow")

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
None

### Request Example
None

### Response
#### Success Response (200)
- **bufferedAmount** (Number) - Number of bytes buffered in the underlaying SCTP association.

#### Response Example
N/A
```

```APIDOC
## dataConsumer.on("listenererror", fn(eventName, error))

### Description
Emitted when an event listener given by the application throws. The exception is silently ignored internally to not break the internal state. By listening to this event, the application can be aware of exceptions happening in its given event listeners.

### Method
ON

### Endpoint
dataConsumer.on("listenererror")

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
None

### Request Example
None

### Response
#### Success Response (200)
- **eventName** (String) - The name of the event.
- **error** (Error) - The error happening in the application given event listener.

#### Response Example
N/A
```

---

### Producer Trace Event Types

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

Details the information provided by 'trace' events when enabled on a Producer, covering 'rtp', 'keyframe', 'nack', 'pli', 'fir', and 'sr' types.

````APIDOC
## Producer Trace Event Details

### Description
This section details the structure of the 'trace' event payload when enabled on a mediasoup Producer, covering various RTP/RTCP related event types.

### Producer Trace Event Types

#### 'rtp'
Enables tracing for standard RTP packets.

##### Event Example
```javascript
producer.on('trace', (trace) => {
  // trace =>
  {
    "direction": "in",
    "info": {
      "isKeyFrame": false,
      "marker": "true",
      "mid": "6",
      "payloadSize": 914,
      "payloadType": 96,
      "rid": "r1",
      "rrid": "r1",
      "sequenceNumber": 19694,
      "size": 942,
      "spatialLayer": 0,
      "ssrc": 27777256,
      "temporalLayer": 1,
      "timestamp": 1227771600,
      "wideSequenceNumber": 2413
    },
    "timestamp": 1513714260,
    "type": "rtp"
  }
});
````

#### 'keyframe'

Enables tracing specifically for keyframe packets. When this type is enabled, 'rtp' events for keyframes will not be generated.

##### Event Example

```javascript
producer.on('trace', (trace) => {
  // trace =>
  {
    "direction": "in",
    "info": {
      "isKeyFrame": true,
      "marker": "false",
      "mid": "2",
      "payloadSize": 1088,
      "payloadType": 96,
      "rid": "r2",
      "rrid": "r2",
      "sequenceNumber": 14176,
      "size": 1116,
      "spatialLayer": 0,
      "ssrc": 3838709357,
      "temporalLayer": 0,
      "timestamp": 3003475216,
      "wideSequenceNumber": 62
    },
    "timestamp": 1513798049,
    "type": "keyframe"
  }
});
```

#### 'nack'

Enables tracing for Negative Acknowledgement (NACK) packets sent by the receiver.

##### Event Example

```javascript
producer.on('trace', (trace) => {
  // trace =>
  {
    "direction": "out",
    "info": {}
    "timestamp": 1544498146,
    "type": "nack"
  }
});
```

#### 'pli' (Picture Loss Indication)

Enables tracing for Picture Loss Indication (PLI) requests sent by the receiver.

##### Event Example

```javascript
producer.on('trace', (trace) => {
  // trace =>
  {
    "direction": "out",
    "info": {
      "ssrc": 87654321
    }
    "timestamp": 1544498146,
    "type": "pli"
  }
});
```

#### 'fir' (Full Intra Request)

Enables tracing for Full Intra (FIR) requests sent by the receiver.

##### Event Example

```javascript
producer.on('trace', (trace) => {
  // trace =>
  {
    "direction": "out",
    "info": {
      "ssrc": 95438003
    }
    "timestamp": 1544498155,
    "type": "fir"
  }
});
```

#### 'sr' (Sender Report)

Enables tracing for Sender Report (SR) packets sent by the producer.

##### Event Example

```javascript
producer.on('trace', (trace) => {
  // trace =>
  {
    "direction": "out",
    "info": {
      "ssrc": 15438003,
      "ntp_sec": 768723434,
      "ntp_frac": 87876,
      "rtp_ts": 23768,
      "packet_count": 100,
      "octet_count": 200
    }
    "timestamp": 164498155,
    "type": "sr"
  }
});
```

````

--------------------------------

### Node-SCTP for SCTP/DataChannel over UDP in Node.js

Source: https://mediasoup.org/documentation/v3/communication-between-client-and-server

This section details using the node-sctp library to enable SCTP/DataChannel communication over plain UDP with mediasoup. It involves creating a PlainTransport with SCTP enabled, setting up a Node.js UDP socket, and configuring an SCTP socket from node-sctp to use the UDP socket for transport.

```javascript
// 1. Create a PlainTransport with SCTP enabled
const transport = await router.createPlainTransport({
  // ... other options ...
  enableSctp: true
});

// 2. Create a Node.js UDP socket
const dgram = require('dgram');
const udpSocket = dgram.createSocket('udp4');
await transport.connect({
  ip: udpSocket.address().address,
  port: udpSocket.address().port
});

// 3. Create an SCTP socket using node-sctp
const sctp = require('sctp');
const sctpSocket = sctp.connect({
  udpSocket: udpSocket,
  remoteIp: '<WEBRTC_IP>',
  remotePort: 5000,
  localPort: 5000,
  // ... other sctp options ...
});

// 4. Create a SCTP stream
const streamId = 0;
const ppid = 51; // 51 for WebRTC String, 53 for WebRTC Binary

// 5. Create a DataProducer on the mediasoup transport
const dataProducer = await transport.produceData({
  streamId: streamId,
  ppid: ppid
});

// 6. Write data into the SCTP stream
sctpSocket.write(Buffer.from('Hello SCTP!'), {
  streamId: streamId,
  ppid: ppid
});

// Remember to close sockets when done
// udpSocket.close();
// sctpSocket.end();
````

---

### Update Worker Settings (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Updates worker settings at runtime. Only a subset of settings can be modified. This is an asynchronous operation that takes a WorkerUpdateableSettings object as an argument.

```javascript
await worker.updateSettings({ logLevel: "warn" })
```

---

### Listen for New Data Consumer on Mediasoup Transport

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Attaches an event listener to the transport's observer to be notified when a new data consumer is created. This is useful for tracking active data sinks. Requires a mediasoup transport instance.

```javascript
transport.observer.on("newdataconsumer", (dataConsumer) => {
	console.log("new data consumer created [id:%s]", dataConsumer.id)
})
```

---

### Transport Class

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Represents a transport for sending or receiving media.

```APIDOC
## Transport Class

### Description
Represents a transport for sending or receiving media streams.

### Dictionaries
- **TransportOptions**: Options for creating a transport.
- **TransportProduceParameters**: Parameters for producing media.
- **TransportProduceDataParameters**: Parameters for producing data.

### Properties
- **id**: (String) - The unique identifier of the transport.
- **closed**: (Boolean) - Indicates if the transport is closed.
- **direction**: (String) - The direction of the transport ('send' or 'recv').
- **iceGatheringState**: (String) - The ICE gathering state.
- **connectionState**: (String) - The connection state of the transport.
- **appData**: (Object) - Arbitrary application data.
- **observer**: (Object) - An observer for transport events.

### Methods
- **transport.close()**: Closes the transport.
- **transport.getStats()**: Retrieves statistics for the transport.
- **transport.restartIce({ iceParameters })**: Restarts the ICE process.
- **transport.updateIceServers({ iceServers })**: Updates the ICE servers for the transport.
- **transport.produce(options)**: Creates a producer on this transport.
- **transport.consume(options)**: Creates a consumer on this transport.
- **transport.produceData(options)**: Creates a data producer on this transport.
- **transport.consumeData(options)**: Creates a data consumer on this transport.

### Events
- **transport.on(“connect”, fn({ dtlsParameters }, callback(), errback(error)))**: Emitted when the transport needs to connect.
- **transport.on(“produce”, fn(parameters, callback({ id }), errback(error)))**: Emitted when a producer is requested.
- **transport.on(“producedata”, fn(parameters, callback({ id }), errback(error)))**: Emitted when a data producer is requested.
- **transport.on(“icegatheringstatechange”, fn(iceGatheringState))**: Emitted when the ICE gathering state changes.
- **transport.on(“icecandidateerror”, fn(event))**: Emitted when an ICE candidate error occurs.
- **transport.on(“connectionstatechange”, fn(connectionState))**: Emitted when the connection state changes.

### Observer Events
- **transport.observer.on(“close”, fn())**: Emitted when the transport is closed.
- **transport.observer.on(“newproducer”, fn(producer))**: Emitted when a new producer is created.
- **transport.observer.on(“newconsumer”, fn(consumer))**: Emitted when a new consumer is created.
- **transport.observer.on(“newdataproducer”, fn(dataProducer))**: Emitted when a new data producer is created.
- **transport.observer.on(“newdataconsumer”, fn(dataConsumer))**: Emitted when a new data consumer is created.
```

---

### WebRTC Transport Observer Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Provides observer events for WebRTC transports, mirroring the direct transport events but allowing for detached observation.

````APIDOC
## webRtcTransport.observer.on(event)

### Description
Observer events for WebRTC transports. These events are the same as the direct transport events but are accessed via the `observer` property.

### Available Events
- `icestatechange` (iceState)
- `iceselectedtuplechange` (iceSelectedTuple)
- `dtlsstatechange` (dtlsState)
- `sctpstatechange` (sctpState)

### Example
```javascript
webRtcTransport.observer.on('dtlsstatechange', (dtlsState) => {
  console.log('Observer detected DTLS state change:', dtlsState);
});
````

````

--------------------------------

### Router: Pipe to Router (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Facilitates piping media streams from a producer in one router to a consumer in another router. This is essential for inter-router communication. It requires the `PipeToRouterOptions` and returns a `PipeToRouterResult`.

```javascript
import * as mediasoup from 'mediasoup';

async function pipeStreamToAnotherRouter(router, producer, destinationRouter) {
  try {
    const pipeToRouterOptions = {
      producerId: producer.id,
      // Other options like 'ỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡỡ
````

---

### transport.on('produce')

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Emitted when the transport needs to transmit information about a new producer to the associated server-side transport. This event occurs before the produce() method completes.

````APIDOC
## transport.on('produce')

### Description
Emitted when the transport needs to transmit information about a new producer to the associated server side transport. This event occurs before the produce() method completes.

### Method
Event Listener

### Endpoint
N/A

### Parameters
#### Arguments
- **parameters** (TransportProduceParameters) - Parameters to create a server-side producer.
- **callback** (Function) - A function that must be called with the server-side producer's ID upon successful creation.
- **errback** (Function) - A function that must be called with an error if the producer creation fails.

### Request Example
```javascript
transport.on("produce", async (parameters, callback, errback) =>
{
  try
  {
    const data = await mySignaling.send(
      "transport-produce",
      {
        transportId   : transport.id,
        kind          : parameters.kind,
        rtpParameters : parameters.rtpParameters,
        appData       : parameters.appData
      });
    const { id } = data;
    callback({ id });
  }
  catch (error)
  {
    errback(error);
  }
});
````

### Response

#### Callback Response

- **id** (String) - The ID of the newly created server-side producer.

````

--------------------------------

### Produce media using transport.produce in TypeScript

Source: https://mediasoup.org/documentation/v3/mediasoup/api

The transport.produce method is used to inject audio or video RTP streams into the mediasoup router. It requires ProducerOptions including RTP parameters and accepts optional custom appData. The method returns a Producer instance.

```typescript
const producer = await transport.produce(
  {
    kind          : "video",
    rtpParameters :
    {
      mid    : "1",
      codecs :
      [
        {
          mimeType    : "video/VP8",
          payloadType : 101,
          clockRate   : 90000,
          rtcpFeedback :
          [
            { type: "nack" },
            { type: "nack", parameter: "pli" },
            { type: "ccm", parameter: "fir" },
            { type: "goog-remb" }
          ]
        },
        {
          mimeType    : "video/rtx",
          payloadType : 102,
          clockRate   : 90000,
          parameters  : { apt: 101 }
        }
      ],
      headerExtensions :
      [
        {
          id  : 2,
          uri : "urn:ietf:params:rtp-hdrext:sdes:mid"
        },
        {
          id  : 3,
          uri : "urn:ietf:params:rtp-hdrext:sdes:rtp-stream-id"
        },
        {
          id  : 5,
          uri : "urn:3gpp:video-orientation"
        },
        {
          id  : 6,
          uri : "http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time"
        }
      ],
      encodings :
      [
        { rid: "r0", active: true, maxBitrate: 100000 },
        { rid: "r1", active: true, maxBitrate: 300000 },
        { rid: "r2", active: true, maxBitrate: 900000 }
      ],
      rtcp :
      {
        cname : "Zjhd656aqfoo"
      }
    }
  });

````

---

### Consume Data with Transport

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Instructs the transport to receive data via DataChannel from the mediasoup router. Requires DataConsumerOptions, including id, producerId, sctpStreamParameters, label, and protocol. The data consumer is created server-side and its parameters are signaled to the client.

```javascript
mySignaling.on("newDataConsumer", async (data) => {
	const consumer = await transport.consumeData({
		id: data.id,
		producerId: data.producerId,
		sctpStreamParameters: data.sctpStreamParameters,
		label: data.label,
		protocol: data.protocol,
	})
})
```

---

### Parse Scalability Mode String

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Parses a scalability mode string into spatial and temporal layer counts. Handles undefined input by defaulting to 1 spatial and 1 temporal layer. The output is an object with 'spatialLayers' and 'temporalLayers' properties.

```javascript
mediasoupClient.parseScalabilityMode("L2T3")
// => { spatialLayers: 2, temporalLayers: 3 }

mediasoupClient.parseScalabilityMode("S3T3")
// => { spatialLayers: 3, temporalLayers: 3 }

mediasoupClient.parseScalabilityMode("L4T7_KEY_SHIFT")
// => { spatialLayers: 4, temporalLayers: 7 }

mediasoupClient.parseScalabilityMode(undefined)
// => { spatialLayers: 1, temporalLayers: 1 }
```

---

### PlainTransport Properties

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Properties available on a PlainTransport instance, providing information about its network connections and SCTP status.

```APIDOC
## PlainTransport Properties

### Description
Properties providing information about the PlainTransport instance.

### Properties
#### `plainTransport.tuple`
- **Type**: `TransportTuple` (read only)
- **Description**: The transport tuple containing `localAddress`, `localPort`, and `protocol`. If RTCP-mux is enabled, this tuple applies to both RTP and RTCP.

#### `plainTransport.rtcpTuple`
- **Type**: `TransportTuple` (read only)
- **Description**: The transport tuple specifically for RTCP. If RTCP-mux is enabled, this property is `undefined`.

#### `plainTransport.sctpParameters`
- **Type**: `SctpParameters` or `undefined` (read only)
- **Description**: Local SCTP parameters. `undefined` if SCTP is not enabled for this transport.

#### `plainTransport.sctpState`
- **Type**: `SctpState` or `undefined` (read only)
- **Description**: The current SCTP state. `undefined` if SCTP is not enabled for this transport.

### Notes
- Information about `remoteIp` and `remotePort` in `tuple` and `rtcpTuple` is set after calling the `connect()` method or dynamically detected when using `comedia` mode.
```

---

### Observe New Transport Creation

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'newtransport' event emitted by a router's observer. This event is triggered whenever a new transport is created within the router's context.

```javascript
router.observer.on("newtransport", (transport) => {
	console.log("new transport created [id:%s]", transport.id)
})
```

---

### Mediasoup Observer: New Worker Event (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This snippet shows how to attach an event listener to the mediasoup observer for the 'newworker' event. This event is fired whenever a new worker is created via `mediasoup.createWorker`. The listener receives the newly created `Worker` object as an argument.

```javascript
import * as mediasoup from "mediasoup"

mediasoup.observer.on("newworker", (worker) => {
	console.log("New mediasoup worker created:", worker.pid)
	// You can attach listeners to the worker here if needed
	worker.observer.on("close", () => {
		console.log("Worker closed:", worker.pid)
	})
})

console.log("Listening for new mediasoup workers...")
// Ensure you call mediasoup.createWorker() elsewhere to trigger this event.
```

---

### transport.on('connectionstatechange')

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Emitted when the local transport's connection state changes.

```APIDOC
## transport.on('connectionstatechange')

### Description
Emitted when the local transport connection state changes.

### Method
Event Listener

### Endpoint
N/A

### Parameters
#### Arguments
- **connectionState** (RTCPeerConnectionState | Transport connection state) - The new connection state.
```

---

### SctpStreamParameters Dictionary

Source: https://mediasoup.org/documentation/v3/mediasoup/sctp-parameters

Defines reliability parameters for a specific SCTP stream, including ordering, maximum packet lifetime, and retransmission limits.

```APIDOC
## SctpStreamParameters Dictionary

### Description
SCTP stream parameters describe the reliability of a certain SCTP stream.

### Fields
- **streamId** (Number) - Required - SCTP stream id.
- **ordered** (Boolean) - Optional - Whether data messages must be received in order. If true, the messages will be sent reliably. Default: true.
- **maxPacketLifeTime** (Number) - Optional - When `ordered` is false, indicates the time (in milliseconds) after which a SCTP packet will stop being retransmitted.
- **maxRetransmits** (Number) - Optional - When `ordered` is false, indicates the maximum number of times a packet will be retransmitted.

### Notes
- If `ordered` is true, then `maxPacketLifeTime` and `maxRetransmits` must be false.
- If `ordered` is false, only one of `maxPacketLifeTime` or `maxRetransmits` can be true.
```

---

### webRtcServer.on("listenererror", fn(eventName, error))

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Emitted when an event listener provided by the application throws an error. This allows applications to be aware of exceptions within their event listeners, which are otherwise silently ignored internally.

```APIDOC
## webRtcServer.on("listenererror", fn(eventName, error))

### Description
Emitted when an event listener given by the application throws an exception. The exception is silently ignored internally to not break the internal state. By listening to this event, the application can be aware of exceptions happening in its given event listeners.

### Method
Event Listener

### Parameters
#### Arguments
- **eventName** (String) - The name of the event.
- **error** (Error) - The error happening in the application given event listener.
```

---

### Consumer Events: Score and Layers Change

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Shows how to handle 'score' and 'layerschange' events emitted by the consumer. The 'score' event provides updates on the RTP stream quality, while 'layerschange' informs about changes in spatial or temporal layers, particularly relevant for simulcast and SVC streams.

```javascript
consumer.on("score", (score) => {
	console.log(score)
})

consumer.on("layerschange", (layers) => {
	console.log(layers)
})

// Example of detecting consumer deactivation due to bandwidth
consumer.on("layerschange", (layers) => {
	if (layers === null && consumer.paused === false && consumer.producerPaused === false) {
		console.log("consumer deactivated due to not enough bandwidth")
	}
})
```

---

### Handle RTP Packet Reception in Consumer (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This code snippet demonstrates how to listen for the 'rtp' event on a mediasoup consumer. This event is triggered when the consumer receives an RTP packet from its associated producer via a direct transport. The received packet is provided as a Node.js Buffer, which can then be processed as needed.

```javascript
consumer.on("rtp", (rtpPacket) => {
	// Do stuff with the binary RTP packet.
})
```

---

### Transport UpdateIceServers Method

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Updates the list of TURN servers used by the underlying peer connection. This is particularly useful when TURN server credentials change. The method accepts an array of RTCIceServer objects.

```cpp
transport.updateIceServers(iceServers);

```

---

### transport.consume

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Instructs the router to send audio or video RTP (or SRTP depending on the transport class). This is the way to extract media from mediasoup.

````APIDOC
## POST /transport/consume

### Description
Consumes media (audio or video) from a producer via the transport. It's recommended to create the consumer in a paused state and then resume it after the remote endpoint is ready.

### Method
POST

### Endpoint
/transport/consume

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
- **producerId** (string) - Required - The ID of the producer to consume from.
- **rtpCapabilities** (object) - Required - The RTP capabilities of the consumer.
- **paused** (boolean) - Optional - Whether to create the consumer in a paused state. Defaults to `false`.
- **appData** (object) - Optional - Custom application data.

### Request Example
```json
{
  "producerId": "a7a955cf-fe67-4327-bd98-bbd85d7e2ba3",
  "rtpCapabilities": {
    "codecs": [
      {
        "mimeType": "audio/opus",
        "kind": "audio",
        "clockRate": 48000,
        "preferredPayloadType": 100,
        "channels": 2
      },
      {
        "mimeType": "video/H264",
        "kind": "video",
        "clockRate": 90000,
        "preferredPayloadType": 101,
        "rtcpFeedback": [
          { "type": "nack" },
          { "type": "nack", "parameter": "pli" },
          { "type": "ccm", "parameter": "fir" },
          { "type": "goog-remb" }
        ],
        "parameters": {
          "level-asymmetry-allowed": 1,
          "packetization-mode": 1,
          "profile-level-id": "4d0032"
        }
      },
      {
        "mimeType": "video/rtx",
        "kind": "video",
        "clockRate": 90000,
        "preferredPayloadType": 102,
        "rtcpFeedback": [],
        "parameters": {
          "apt": 101
        }
      }
    ],
    "headerExtensions": [
      {
        "kind": "video",
        "uri": "http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time",
        "preferredId": 4,
        "preferredEncrypt": false
      },
      {
        "kind": "audio",
        "uri": "urn:ietf:params:rtp-hdrext:ssrc-audio-level",
        "preferredId": 8,
        "preferredEncrypt": false
      },
      {
        "kind": "video",
        "uri": "urn:3gpp:video-orientation",
        "preferredId": 9,
        "preferredEncrypt": false
      },
      {
        "kind": "video",
        "uri": "urn:ietf:params:rtp-hdrext:toffset",
        "preferredId": 10,
        "preferredEncrypt": false
      }
    ]
  },
  "paused": true
}
````

### Response

#### Success Response (200)

- **id** (string) - The ID of the created consumer.
- **producerId** (string) - The ID of the producer.
- **kind** (string) - The media kind (audio or video).
- **rtpParameters** (object) - The RTP parameters for the consumer.
- **paused** (boolean) - Whether the consumer is paused.
- **appData** (object) - Custom application data.

#### Response Example

```json
{
  "id": "consumer-id-123",
  "producerId": "a7a955cf-fe67-4327-bd98-bbd85d7e2ba3",
  "kind": "audio",
  "rtpParameters": { ... },
  "paused": true,
  "appData": {}
}
```

````

--------------------------------

### DataConsumer Message Reception

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'message' event on a data consumer, which is triggered when a message is received from the corresponding data producer. This functionality is only available for direct transports. The event provides the message content (as a Buffer) and a payload protocol identifier (ppid). The code differentiates between text and binary messages.

```javascript
dataConsumer.on("message", (message, ppid) =>
{
  if (ppid === 51)
    console.log("text message received:", message.toString("utf-8"));
  else if (ppid === 53)
    console.log("binary message received");
});
````

---

### Transport GetAppData and IsClosed Methods

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Allows access to custom application data provided during transport creation, which can be modified at any time. It also provides a method to check if the transport has been closed.

```cpp
// Get custom application data
nlohmann::json& appData = transport.GetAppData();

// Check if transport is closed
bool isClosed = transport.IsClosed();

```

---

### DataTransport ProduceData

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Instructs the transport to send data via DataChannel to the mediasoup router. This method is asynchronous and requires implementing the OnProduceData callback in the listener.

````APIDOC
## sendTransport.ProduceData(listener, label, protocol, order, maxRetransmits, maxPacketLifeTime, appData)

### Description
Instructs the transport to send data via DataChannel to the mediasoup router. The local transport will call the `OnProduceData` method in the listener before this method completes. The application must define this method to signal parameters to the server and invoke `transport.produce()` on the corresponding WebRTC transport.

### Method
POST

### Endpoint
/websites/mediasoup_v3

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
- **listener** (DataProducer::Listener*) - Required - DataProducer listener.
- **label** (string) - Optional - A label which can be used to distinguish this DataChannel from others.
- **protocol** (string) - Optional - Name of the sub-protocol used by this DataChannel.
- **ordered** (boolean) - Optional - Whether data messages must be received in order. If true, messages will be sent reliably. Defaults to `true`.
- **maxPacketLifeTime** (integer) - Optional - When `ordered` is false, indicates the time (in milliseconds) after which a SCTP packet will stop being retransmitted.
- **maxRetransmits** (integer) - Optional - When `ordered` is false, indicates the maximum number of times a packet will be retransmitted.
- **appData** (object) - Optional - Custom application data. Defaults to `{}`.

### Request Example
```json
{
  "listener": "<DataProducer::Listener*>",
  "label": "chat",
  "protocol": "string",
  "ordered": true,
  "maxPacketLifeTime": 3000,
  "maxRetransmits": 5,
  "appData": {
    "custom": "data"
  }
}
````

### Response

#### Success Response (200)

- **DataProducer** (DataProducer\*) - The created DataProducer instance.

#### Response Example

```json
{
	"DataProducer": "<DataProducer*>"
}
```

````

--------------------------------

### transport.on('icegatheringstatechange')

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Emitted when the local transport's ICE gathering state changes.

```APIDOC
## transport.on('icegatheringstatechange')

### Description
Emitted when the local transport ICE gathering state changes.

### Method
Event Listener

### Endpoint
N/A

### Parameters
#### Arguments
- **iceGatheringState** (RTCIceGatheringState | Transport ICE gathering state) - The new ICE gathering state.
````

---

### Produce Data with Transport

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Instructs the transport to send data via DataChannel to the mediasoup router. This method is asynchronous and returns a DataProducer. The application must handle the 'produceData' event and signal parameters to the server.

```javascript
const producer = await transport.produceData()
```

---

### PipeTransport Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Events emitted by a PipeTransport instance.

```APIDOC
## PipeTransport Events

### Description
Events emitted by a PipeTransport instance.

### Events
#### pipeTransport.on(“sctpstatechange”, fn(sctpState))
- Description: Emitted when the transport SCTP state changes.
- Arguments:
  - **sctpState** (SctpState) - The new SCTP state.

#### pipeTransport.observer.on(“sctpstatechange”, fn(sctpState))
- Description: Same as the `sctpstatechange` event.
```

---

### Transport GetStats Method

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Fetches statistics for the local transport by calling the getStats() method on the underlying RTCPeerConnection instance. This operation is asynchronous and will block the current thread until completion, returning an RTCStatsReport.

```cpp
// This will block the current thread until completion.
nlohmann::json& stats = transport.GetStats();

```

---

### Worker: Update Settings (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Allows updating certain settings of a running mediasoup Worker, such as log level or tags. It takes a `WorkerUpdateableSettings` object. Note that not all settings can be updated after worker creation.

```javascript
import * as mediasoup from "mediasoup"

async function updateWorkerSettings(worker) {
	try {
		const newSettings = {
			logLevel: "warn",
			rtcMinPort: 20000, // Example of a setting that might not be updateable
		}
		await worker.updateSettings(newSettings)
		console.log(`Worker ${worker.pid} settings updated.`)
	} catch (error) {
		console.error(`Failed to update settings for worker ${worker.pid}:`, error)
		throw error
	}
}

// Example usage (assuming 'myWorker' is an existing Worker instance):
// updateWorkerSettings(myWorker);
```

---

### Handle New Transport Creation Event

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Listens for the 'newtransport' event emitted by the device's observer. This event is triggered whenever a new transport is successfully created on the client side. The event handler receives the newly created transport object as an argument.

```javascript
device.observer.on("newtransport", (transport) => {
	console.log("new transport created [id:%s]", transport.id)
})
```

---

### Worker Observer: New Router Event (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'newrouter' event on the worker's observer. This event fires whenever a new Router is created using `worker.createRouter()`. The listener receives the newly created `Router` object.

```javascript
import * as mediasoup from "mediasoup"

async function setupNewRouterListener(worker) {
	worker.observer.on("newrouter", (router) => {
		console.log(`New router created for worker ${worker.pid}: ${router.id}`)
		// You can now interact with the new router
	})
	console.log(`Listening for 'newrouter' events on worker ${worker.pid}.`)
}

// Example usage (assuming 'myWorker' is an existing Worker instance):
// setupNewRouterListener(myWorker);
// // Later, to trigger the event:
// // await worker.createRouter();
```

---

### Connect WebRTC Transport (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Connects the WebRTC transport by providing remote DTLS parameters. This method is asynchronous and requires DtlsParameters as input. It's crucial for establishing the secure connection.

```javascript
await webRtcTransport.connect({
	dtlsParameters: {
		role: "server",
		fingerprints: [
			{
				algorithm: "sha-256",
				value: "E5:F5:CA:A7:2D:93:E6:16:AC:21:09:9F:23:51:62:8C:D0:66:E9:0C:22:54:2B:82:0C:DF:E0:C5:2C:7E:CD:53",
			},
		],
	},
})
```

---

### Producer Properties

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Read-only properties of a Producer object, including its ID, closed status, and media kind.

```APIDOC
### Properties

#### `producer.id`

- **Type**: String
- **Description**: Producer identifier.
- **Read only**: Yes

#### `producer.closed`

- **Type**: Boolean
- **Description**: Indicates whether the producer is closed.
- **Read only**: Yes

#### `producer.kind`

- **Type**: MediaKind
- **Description**: The media kind ("audio" or "video") of the producer.
- **Read only**: Yes

```

---

### Pipe Producer to Another Router with Mediasoup v3

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Pipes a media or data producer into another router on the same host. This method creates an underlying PipeTransport if one doesn't exist. It's useful for expanding broadcasting capabilities across different workers. Options include producerId and the target router. Returns PipeToRouterResult.

```typescript
// Have two workers.
const worker1 = await mediasoup.createWorker();
const worker2 = await mediasoup.createWorker();

// Create a router in each worker.
const router1 = await worker1.createRouter({ mediaCodecs });
const router2 = await worker2.createRouter({ mediaCodecs });

// Produce in router1.
const transport1 = await router1.createWebRtcTransport({ ... });
const producer1 = await transport1.produce({ ... });

// Pipe producer1 into router2.
await router1.pipeToRouter({ producerId: producer1.id, router: router2 });

// Consume producer1 from router2.
const transport2 = await router2.createWebRtcTransport({ ... });
const consumer2 = await transport2.consume({ producerId: producer1.id, ... });

```

---

### Producer Listener Interface: Transport Close Event (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Defines the `ProducerListener` interface, which handles events related to a `Producer`. The primary event is `OnTransportClose`, called when the associated transport is closed, allowing the producer to react accordingly.

```cpp
#include "libmediasoupclient/Producer.hpp"

class MyProducerListener : public mediasoupclient::Producer::Listener {
public:
    void OnTransportClose(mediasoupclient::Producer* producer) override {
        // Handle the event when the transport associated with the producer is closed
    }
};

```

---

### Consumer Closure Management in Mediasoup

Source: https://mediasoup.org/documentation/v3/mediasoup/garbage-collection

Illustrates the closure events for a Consumer object in mediasoup. A consumer can be closed by `consumer.close()`, or by the `transportclose` or `producerclose` events, indicating closure of its associated transport or producer, respectively. References to the consumer should be removed.

```javascript
consumer.close()

consumer.on("transportclose", () => {
	// Clean up consumer reference
})

consumer.on("producerclose", () => {
	// Clean up consumer reference
})
```

---

### RecvTransport::ConsumeData

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Instructs the transport to receive data via DataChannel from the mediasoup router.

````APIDOC
## RecvTransport::ConsumeData

### Description
Instructs the transport to receive data via DataChannel from the mediasoup router. This method is asynchronous and blocks the current thread until completion.

### Method
`recvTransport->ConsumeData()`

### Endpoint
N/A (This is a method call, not a REST endpoint)

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
None

#### Arguments
- **listener** (DataConsumer::Listener) - Consumer listener.
- **id** (const std::string&) - The identifier of the server-side consumer.
- **producerId** (const std::string&) - The identifier of the server-side producer being consumed.
- **label** (const std::string&) - A label which can be used to distinguish this DataChannel from others.
- **protocol** (const std::string&) - Name of the sub-protocol used by this DataChannel. (Optional)
- **appData** (nlohmann::json) - Custom application data. (Optional, defaults to `{}`)

### Request Example
```cpp
auto* consumerListener = new MyConsumerListener();

// This will block the current thread until completion.
auto* consumer = recvTransport->ConsumeData(
  consumerListener,
  id,
  "dataChannelLabel",
  "dataChannelProtocol");
````

### Response

#### Success Response (200)

- **dataConsumer** (DataConsumer\*) - Pointer to the created DataConsumer instance.

#### Response Example

```cpp
// consumer is a pointer to a DataConsumer object
```

````

--------------------------------

### ActiveSpeakerObserver 'dominantspeaker' Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'dominantspeaker' event on an ActiveSpeakerObserver. This event is triggered when a new dominant speaker is detected based on audio activity within the observed producers. The handler receives an object containing the dominant producer instance.

```javascript
activeSpeakerObserver.on("dominantspeaker", (dominantSpeaker) =>
{
  console.log(`Dominant speaker detected: ${dominantSpeaker.producer.id}`);
});

````

---

### Connect SRTP Endpoint to Mediasoup Plain Transport

Source: https://mediasoup.org/documentation/v3/communication-between-client-and-server

Connects an SRTP-capable endpoint to a mediasoup plain transport. This involves providing SRTP parameters, including the crypto suite and key material obtained from the endpoint's SDP offer. The mediasoup transport then uses these parameters for encrypted RTP transmission.

```javascript
await plainTransport.connect({
	cryptoSuite: "AES_CM_128_HMAC_SHA1_80",
	keyBase64: "PS1uQCVeeCFCanVmcjkpPywjNWhcYD0mXXtxaVBR",
})
```

---

### Transport GetId and GetConnectionState Methods

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Retrieves the unique identifier for a transport, which matches the server-side transport's ID. It also provides the current connection state of the local peer connection associated with the transport.

```cpp
// Get transport identifier
const std::string& transportId = transport.GetId();

// Get current connection state
const std::string& connectionState = transport.GetConnectionState();

```

---

### ProducerListener::OnTransportClose Event - C++

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Demonstrates the implementation of the OnTransportClose event handler for the ProducerListener interface in C++. This callback is invoked when the associated transport is closed, automatically closing the producer.

```cpp
void MyProducerListener::OnTransportClose(mediasoupclient::Producer* producer)
{
	std::cout << "transport closed" << std::endl;
}

```

---

### Check if Router can Consume Producer

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Determines if the provided RTP capabilities are valid for consuming a specific producer. This is a crucial step before attempting to consume a producer.

```javascript
if (router.canConsume({ producerId, rtpCapabilities })) {
	// Consume the producer by calling transport.consume({ producerId, rtpCapabilities }).
}
```

---

### Define ProducerCodecOptions Type (TypeScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Defines the TypeScript type for ProducerCodecOptions, specifying parameters for audio and video codecs like Opus and VP8. These options control codec-specific behaviors such as stereo, FEC, DTX, and bitrate limits.

```typescript
type ProducerCodecOptions = {
	opusStereo?: boolean
	opusFec?: boolean
	opusDtx?: boolean
	opusMaxPlaybackRate?: number
	opusMaxAverageBitrate?: number
	opusPtime?: number
	opusNack?: boolean
	videoGoogleStartBitrate?: number
	videoGoogleMaxBitrate?: number
	videoGoogleMinBitrate?: number
}
```

---

### Consumer Listener Interface: Transport Close Event (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Defines the `ConsumerListener` interface for handling events related to a `Consumer`. Similar to `ProducerListener`, it includes `OnTransportClose` to notify when the associated transport has been closed.

```cpp
#include "libmediasoupclient/Consumer.hpp"

class MyConsumerListener : public mediasoupclient::Consumer::Listener {
public:
    void OnTransportClose(mediasoupclient::Consumer* consumer) override {
        // Handle the event when the transport associated with the consumer is closed
    }
};

```

---

### Produce Data with transport.produceData

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Instructs the router to receive data messages. This can be done via SCTP protocol (DataChannel) or directly from the Node.js application if the transport is a `DirectTransport`. Options include `sctpStreamParameters` and `label` for SCTP-based data production.

```typescript
// Using SCTP:
const dataProducer = await transport.produceData({
	sctpStreamParameters: {
		streamId: 4,
		ordered: true,
	},
	label: "foo",
})

// Using a direct transport:
const dataProducer = await transport.produceData()
```

---

### Handle DataProducer Transport Close Event (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

This C++ code snippet demonstrates the implementation of the OnTransportClose callback for a DataProducerListener. It is executed when the associated transport is closed, leading to the closure of the producer itself. The function takes a pointer to the DataProducer as an argument.

```cpp
void MyProducerListener::OnTransportClose(mediasoupclient::Producer* producer)
{
	std::cout << "transport closed" << std::endl;
}
```

---

### Handle Listener Server Close Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'listenserverclose' event, emitted in WebRTC transports when the underlying WebRTC server closes. This event also closes the transport and its associated producers and consumers.

```typescript
transport.on("listenserverclose", () => {
	console.log("WebRTC server closed so transport closed")
})
```

---

### RTPObserver Pause Method

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Pauses the RTP observer. When paused, the observer stops inspecting any RTP media. To resume inspection, the `resume()` method must be called. This operation is asynchronous.

```javascript
await rtpObserver.pause()
```

---

### Mediasoup Transport Properties

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Illustrates how to access and utilize properties of a Mediasoup transport object, such as its ID, closed status, direction, ICE gathering state, connection state, application data, and observer.

```javascript
console.log(transport.id)
console.log(transport.closed)
console.log(transport.direction)
console.log(transport.iceGatheringState)
console.log(transport.connectionState)

transport.appData.foo = "bar"
console.log(transport.appData.foo)

// Accessing the observer for event handling would be done here, e.g.:
// transport.observer.on('close', () => console.log('Transport closed'));
```

---

### Consume Data via Transport

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Consumes data messages sent to the endpoint via SCTP or directly to the Node.js process. Requires DataConsumerOptions. Returns a DataConsumer object.

```typescript
const dataConsumer = await transport.consumeData({
	dataProducerId: "a7a955cf-fe67-4327-bd98-bbd85d7e2ba4",
})
```

---

### RtpObserver Closure Management in Mediasoup

Source: https://mediasoup.org/documentation/v3/mediasoup/garbage-collection

Explains how to manage the closure of an RtpObserver in mediasoup. Closure can be initiated by `rtpObserver.close()` or by the `routerclose` event, which is emitted when the parent router is closed. The application must remove references to the RtpObserver.

```javascript
rtpObserver.close()

rtpObserver.on("routerclose", () => {
	// Clean up rtpObserver reference
})
```

---

### Transport Trace Event Types

Source: https://mediasoup.org/documentation/v3/mediasoup/debugging

Details the information provided by 'trace' events when enabled on a Transport, specifically for 'probation' and 'bwe' types.

````APIDOC
## Transport Trace Event Details

### Description
This section details the structure of the 'trace' event payload when enabled on a mediasoup Transport, focusing on specific event types.

### Transport Trace Event Types

#### 'probation'
Enables tracing for the 'probation' event, typically related to initial connection state or quality assessment.

##### Event Example
```javascript
transport.on('trace', (trace) => {
  // trace =>
  {
    "direction": "out",
    "info": {
      "isKeyFrame": false,
      "marker": "false",
      "payloadSize": 360,
      "payloadType": 127,
      "sequenceNumber": 19244,
      "size": 384,
      "spatialLayer": 0,
      "ssrc": 1234,
      "temporalLayer": 0,
      "timestamp": 239090504,
      "wideSequenceNumber": 166
    },
    "timestamp": 1513191082,
    "type": "probation"
  }
});
````

#### 'bwe' (Bandwidth Estimation)

Enables tracing for bandwidth estimation events, providing insights into the network's available and desired bitrates.

##### Event Example

```javascript
transport.on('trace', (trace) => {
  // trace =>
  {
    "direction": "out",
    "info": {
      "availableBitrate": 1981250,
      "desiredBitrate": 1483574,
      "effectiveDesiredBitrate": 1483574,
      "maxBitrate": 2002824,
      "maxPaddingBitrate": 1702400,
      "minBitrate": 30000,
      "startBitrate": 1981250,
      "type": 'transport-cc'
    },
    "timestamp": 1513191082,
    "type": "bwe"
  }
});
```

````

--------------------------------

### OPUS Codec Operation Parameters

Source: https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities

Parameters affecting mediasoup operation for the OPUS codec, enabling or disabling specific features.

```APIDOC
### Parameters Affecting mediasoup Operation

#### OPUS

- **`useinbandfec`** (Number) - Optional - If 1, mediasoup will use the worst packet fraction lost in the RTCP Receiver Report received from the consuming endpoints and use it into the Receiver Report that mediasoup sends to the OPUS producer endpoint. This will force it to generate more in-band FEC into the OPUS packets to accommodate to the worst receiver. Default: 0.
- **`usedtx`** (Number) - Optional - If 1, mediasoup will not consider the stream as inactive when there is no RTP traffic. Same behavior is achieved by indicating `dtx`: `true` in the corresponding encoding in the RTP send parameters. Default: 0.
````

---

### DataConsumer Observer Close Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'close' event on the data consumer's observer. This event is emitted when the data consumer is closed, regardless of the reason. It's part of the observer API for monitoring the data consumer's lifecycle.

```javascript
dataConsumer.observer.on("close", () => {
	console.log("dataConsumer observer detected close event")
})
```

---

### Worker Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Events emitted by the worker, including 'died', 'subprocessclose', and 'listenererror'.

````APIDOC
## Worker Events

### `worker.on(“died”, fn(error))`

Emitted when the worker subprocess unexpectedly dies.

#### Parameters

- **error** (Error) - Originating error.

This should never happens. If it happens, it's a bug. Please report it following these instructions.

```javascript
worker.on("died", (error) =>
{
  console.error("mediasoup worker died!: %o", error);
});
````

### `worker.on(“subprocessclose”, fn())`

Emitted when the worker subprocess has closed completely. This event is emitted asynchronously once worker.close() has been called (or after 'died' event in case the worker subprocess abnormally died).
Await for this event if you can to be sure that no Node handler is still open/running after you close a worker.

### `worker.on(“listenererror”, fn(eventName, error))`

Emitted when an event listener given by the application throws. The exception is silently ignored internally to not break the internal state. By listening to this event, the application can be aware of exceptions happening in its given event listeners.

#### Parameters

- **eventName** (String) - The name of the event.
- **error** (Error) - The error happening in the application given event listener.

````

--------------------------------

### Transport Properties

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Read-only properties of a transport object.

```APIDOC
## Transport Properties

### Description
Read-only properties of a transport object.

### Properties
- **id** (String) - Transport identifier. It matches the `id` of the server side transport.
- **closed** (Boolean) - Whether the transport is closed.
- **direction** (String) - The direction of this transport. "send" means that this is a WebRTC transport for sending media. "recv" means that this is a WebRTC transport for receiving media.
- **iceGatheringState** (RTCIceGatheringState) - The current ICE gathering state of the local peerconnection.
- **connectionState** (RTCPeerConnectionState) - The current connection state of the local peerconnection.
- **appData** (Object) - Custom data Object provided by the application in the transport constructor. The app can modify its content at any time.
```javascript
transport.appData.foo = "bar";
````

- **observer** (EventEmitter) - See the Observer Events section below.

````

--------------------------------

### router.close()

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Closes the router, triggering close events on its associated transports and RTP observers.

```APIDOC
### router.close()

Closes the router. Triggers a “routerclose” event in all its transports and also “routerclose” event in all its RTP observers.
````

---

### Handle ICE State Change Event (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for changes in the WebRTC transport's ICE state. The 'icestatechange' event is emitted whenever the ICE state transitions, providing the new state as an argument. This is useful for monitoring connection status and taking appropriate actions, such as closing the transport if it becomes 'disconnected'.

```javascript
webRtcTransport.on("icestatechange", (iceState) => {
	console.log("ICE state changed to %s", iceState)
})
```

---

### RTPObserver Close Method

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Closes the RTP observer. This action terminates the observer and it will no longer inspect any media. This method is synchronous.

```javascript
rtpObserver.close()
```

---

### DataConsumer Closure Management in Mediasoup

Source: https://mediasoup.org/documentation/v3/mediasoup/garbage-collection

Outlines the closure events for a DataConsumer object in mediasoup. A data consumer is closed by calling `dataConsumer.close()`, or by the `transportclose` or `dataproducerclose` events, indicating the closure of its associated transport or data producer. References should be cleaned up.

```javascript
dataConsumer.close()

dataConsumer.on("transportclose", () => {
	// Clean up dataConsumer reference
})

dataConsumer.on("dataproducerclose", () => {
	// Clean up dataConsumer reference
})
```

---

### Filter RTP Capabilities to Prevent Video Orientation Issues (JavaScript)

Source: https://mediasoup.org/documentation/v3/tricks

Filters router RTP capabilities to remove the 'urn:3gpp:video-orientation' header extension. This prevents issues where receivers (like Firefox or FFmpeg) that don't support this extension incorrectly orient video. The filtered capabilities are then loaded into the device. This is particularly useful for libwebrtc-based clients like Chrome.

```javascript
// Let's get router RTP capabilities via our own app signaling.
let routerRtpCapabilities = await mySignaling.request("getRouterRtpCapabilities")

// Just for Chrome, Safari or any libwebrtc based browser.
if (supportsVideoOrientationHeaderExtension) {
	// Remove the "urn:3gpp:video-orientation" extension so when rotating the
	// device, Chrome will encode rotated video instead of indicating the video
	// orientation in an RTP header extension.
	routerRtpCapabilities.headerExtensions = routerRtpCapabilities.headerExtensions.filter((ext) => ext.uri !== "urn:3gpp:video-orientation")
}

// Finally apply the router RTP capabilities to the device.
await device.load({ routerRtpCapabilities })
```

---

### Modify Transport App Data

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Demonstrates how to modify the custom application data associated with a transport. The `appData` property can be updated to hold arbitrary data, allowing for flexible application-specific storage. This can be done by assigning a new object to `transport.appData` or by directly setting properties on the existing `appData` object.

```javascript
transport.appData.foo = "bar"

transport.appData = { foo: "bar", bar: 123 }
```

---

### Monitor ICE Gathering State Changes (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Listens for the 'icegatheringstatechange' event on a Mediasoup transport. This event is fired whenever the local transport's ICE gathering state changes. It provides the new state as an argument, allowing applications to track the progress of ICE candidate gathering.

```javascript
transport.on("icegatheringstatechange", (iceGatheringState) => {
	console.log(`ICE gathering state changed: ${iceGatheringState}`)
})
```

---

### Consume Media Stream with transport.consume

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Instructs the router to send audio or video RTP streams. This method is used to extract media from mediasoup. It's recommended to create consumers in a paused state to prevent potential issues with RTP packet association and optimize video rendering.

```typescript
const consumer = await transport.consume({
	producerId: "a7a955cf-fe67-4327-bd98-bbd85d7e2ba3",
	rtpCapabilities: {
		codecs: [
			{
				mimeType: "audio/opus",
				kind: "audio",
				clockRate: 48000,
				preferredPayloadType: 100,
				channels: 2,
			},
			{
				mimeType: "video/H264",
				kind: "video",
				clockRate: 90000,
				preferredPayloadType: 101,
				rtcpFeedback: [{ type: "nack" }, { type: "nack", parameter: "pli" }, { type: "ccm", parameter: "fir" }, { type: "goog-remb" }],
				parameters: {
					"level-asymmetry-allowed": 1,
					"packetization-mode": 1,
					"profile-level-id": "4d0032",
				},
			},
			{
				mimeType: "video/rtx",
				kind: "video",
				clockRate: 90000,
				preferredPayloadType: 102,
				rtcpFeedback: [],
				parameters: {
					apt: 101,
				},
			},
		],
		headerExtensions: [
			{
				kind: "video",
				uri: "http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time", // eslint-disable-line max-len
				preferredId: 4,
				preferredEncrypt: false,
			},
			{
				kind: "audio",
				uri: "urn:ietf:params:rtp-hdrext:ssrc-audio-level",
				preferredId: 8,
				preferredEncrypt: false,
			},
			{
				kind: "video",
				uri: "urn:3gpp:video-orientation",
				preferredId: 9,
				preferredEncrypt: false,
			},
			{
				kind: "video",
				uri: "urn:ietf:params:rtp-hdrext:toffset",
				preferredId: 10,
				preferredEncrypt: false,
			},
		],
	},
})
```

---

### Define ProducerHeaderExtensionOptions Type (TypeScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Defines the TypeScript type for ProducerHeaderExtensionOptions, specifically for the 'abs-capture-time' extension. This option enables the inclusion of the absolute capture time header extension in RTP packets, useful for precise timestamping.

```typescript
type ProducerHeaderExtensionOptions = {
	absCaptureTime?: boolean
}
```

---

### Router: Close Method (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Closes a Router instance. This action invalidates the router and any associated transports, producers, or consumers. It's important to close routers when they are no longer needed to free up resources.

```javascript
import * as mediasoup from "mediasoup"

async function manageRouterLifecycle(worker) {
	const router = await worker.createRouter()
	console.log("Router created:", router.id)

	// ... use the router ...

	router.close()
	console.log("Router closed.")
}

// Example usage (assuming 'myWorker' is an existing Worker instance):
// manageRouterLifecycle(myWorker).catch(err => {
//   console.error('Error during router management:', err);
// });
```

---

### transport.observer.on('close')

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Emitted when the transport is closed for any reason.

```APIDOC
## transport.observer.on('close')

### Description
Emitted when the transport is closed for whatever reason.

### Method
Event Listener

### Endpoint
N/A

### Parameters
#### Arguments
- None.
```

---

### Worker: Close Method (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Demonstrates closing a mediasoup Worker instance. The `close()` method terminates the worker process. Any routers or transports associated with this worker will also be closed. This is a crucial method for resource management.

```javascript
import * as mediasoup from "mediasoup"

async function manageWorkerLifecycle() {
	const worker = await mediasoup.createWorker()
	console.log("Worker created with PID:", worker.pid)

	// ... perform operations with the worker ...

	// Close the worker
	worker.close()
	console.log("Worker closed.")
}

manageWorkerLifecycle().catch((err) => {
	console.error("Error during worker management:", err)
})
```

---

### Send RTP Packet via Direct Transport Producer

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This code shows how to send an RTP packet from a Node.js process using a producer attached to a direct transport. It assumes the producer has been created with specific RTP parameters and that `rtpPacket` is a Buffer containing a valid RTP packet.

```javascript
const producer = await directTransport.produce(
  {
    kind          : "audio",
    rtpParameters : { ... },
  });

// Send a RTP packet.
producer.send(rtpPacket);
```

---

### Handle DataConsumer Transport Close Event (C++)

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

This C++ code snippet shows the implementation of the OnTransportClose callback for a DataConsumerListener. This event fires when the transport associated with the DataConsumer is closed, which also results in the DataConsumer being closed. The method receives a pointer to the DataConsumer instance.

```cpp
void MyConsumerListener::OnTransportClose(mediasoupclient::DataConsumer* consumer)
{
	std::cout << "transport closed" << std::endl;
}
```

---

### Handle DataProducer transportclose Event

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Listens for the 'transportclose' event on a DataProducer. This event is emitted when the associated transport is closed, which also leads to the DataProducer being closed. This is a crucial event for managing resource cleanup.

```javascript
dataProducer.on("transportclose", () => {
	console.log("transport closed so dataProducer closed")
})
```

---

### DirectTransport Events

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Events emitted by the DirectTransport object, specifically the 'rtcp' event for receiving RTCP packets.

````APIDOC
### Events

See also [Transport Events](link-to-transport-events).

#### `directTransport.on("rtcp", fn(rtcpPacket))`

Emitted when the direct transport receives an RTCP packet from its router.

- **Availability**: Only available in direct transports created via `router.createDirectTransport()`.

#### Parameters

- **rtcpPacket** (Buffer) - Received RTP packet. It's always a Node.js Buffer. It may be a compound RTCP packet or a standalone RTCP packet.

#### Event Example
```javascript
directTransport.on("rtcp", (rtcpPacket) => {
  // Do stuff with the binary RTCP packet.
});
````

````

--------------------------------

### Handle Router Close Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'routerclose' event, which is emitted when the associated router is closed. This event also closes the transport, its producers, and consumers.

```typescript
transport.on("routerclose", () =>
{
  console.log("router closed so transport closed");
});

````

---

### Consumer Statistics

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Provides statistics for a Consumer, detailing sent RTP streams to the consumer.

````APIDOC
## Consumer Statistics

### Description
Provides statistics for a Consumer, detailing the RTP streams sent to the consumer. This includes data transfer metrics and stream quality indicators.

### Method
`getStats()`

### Parameters
This method does not take any parameters.

### Request Example
```javascript
const stats = await consumer.getStats();
````

### Response

#### Success Response (200)

- **type** (string) - The type of the stats (e.g., 'outbound-rtp', 'inbound-rtp').
- **mediaKind** (string) - The kind of media ('audio' or 'video').
- **mid** (string) - The media ID associated with the RTP stream.
- **trackId** (string) - The ID of the media track.
- **transportId** (string) - The ID of the associated transport.
- **codecId** (string) - The ID of the codec used.
- **ssrc** (number) - The SSRC of the RTP stream.
- **packetsSent** (number) - Total packets sent for this stream.
- **packetsReceived** (number) - Total packets received for this stream.
- **bytesSent** (number) - Total bytes sent for this stream.
- **bytesReceived** (number) - Total bytes received for this stream.
- **bitrate** (number) - The current bitrate in bps.
- **jitter** (number) - The current jitter in milliseconds.
- **roundTripTime** (number) - The current round-trip time in milliseconds.
- **packetLossPercentage** (number) - The percentage of packet loss.
- **timestamp** (number) - The timestamp of the stats in milliseconds.

_Note: The specific fields may vary slightly depending on whether it's an inbound or outbound RTP stream._

````

--------------------------------

### Node.js DirectTransport for DataChannel Communication

Source: https://mediasoup.org/documentation/v3/communication-between-client-and-server

This guideline explains how to create a DirectTransport in mediasoup for direct DataChannel communication within a Node.js application. It covers consuming data from WebRTC endpoints using `consumeData` and producing data to WebRTC peers using `produceData`.

```javascript
// Create a DirectTransport
const directTransport = await router.createDirectTransport();

// Consume data from WebRTC endpoints
const dataConsumer = await directTransport.consumeData({
  // ... options ...
});
dataConsumer.on('message', (message) => {
  console.log('Received message:', message);
});

// Produce data to WebRTC peers
const dataProducer = await directTransport.produceData({
  // ... options ...
});

// Send data to WebRTC peers
await dataProducer.send('Hello WebRTC!');
````

---

### Send RTCP Packet with DirectTransport

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Allows sending an RTCP packet from the Node.js process using a direct transport. This is only available for direct transports created via `router.createDirectTransport()`. It requires a Buffer containing a valid RTCP packet.

```javascript
directTransport.sendRtcp(rtcpPacket)
```

---

### Handle Listener Error Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'listenererror' event, which is emitted when an application-provided event listener throws an exception. This allows awareness of exceptions within event listeners.

```typescript
transport.on("listenererror", (eventName, error) => {
	console.error("Error in event listener:", eventName, error)
})
```

---

### rtpObserver routerclose Event Listener

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'routerclose' event on an RTP observer. This event is emitted when the associated router is closed, which also leads to the closure of the RTP observer itself. The handler logs a message indicating the router and observer closure.

```javascript
rtpObserver.on("routerclose", () => {
	console.log("router closed so RTP observer closed")
})
```

---

### DirectTransport

Source: https://mediasoup.org/documentation/v3/mediasoup/api

A direct transport represents a direct connection between the mediasoup Node.js process and a `Router` instance in a mediasoup-worker subprocess.

```APIDOC
## DirectTransport

### Description
A direct transport represents a direct connection between the mediasoup Node.js process and a `Router` instance in a mediasoup-worker subprocess. It can be used to send and receive data messages directly, and to inject/consume RTP and RTCP packets.
```

---

### DataProducer Closure Management in Mediasoup

Source: https://mediasoup.org/documentation/v3/mediasoup/garbage-collection

Describes the closure process for a DataProducer in mediasoup. Closure is achieved via `dataProducer.close()` or the `transportclose` event, which fires when the associated transport is closed. Application code should remove references to the data producer.

```javascript
dataProducer.close()

dataProducer.on("transportclose", () => {
	// Clean up dataProducer reference
})
```

---

### transport.on('icecandidateerror')

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Emitted when an error occurs during ICE negotiations with a STUN or TURN server.

```APIDOC
## transport.on('icecandidateerror')

### Description
Emitted when an error occurs while performing ICE negotiations through a STUN or TURN server.

### Method
Event Listener

### Endpoint
N/A

### Parameters
#### Arguments
- **event** (RTCPeerConnectionIceErrorEvent) - Details about the ICE negotiation error.
```

---

### Producer Closure Management in Mediasoup

Source: https://mediasoup.org/documentation/v3/mediasoup/garbage-collection

Covers the closure procedures for a Producer object in mediasoup. A producer is closed by calling `producer.close()` or by listening for the `transportclose` event, triggered when its associated transport is closed. Application logic should clean up producer references.

```javascript
producer.close()

producer.on("transportclose", () => {
	// Clean up producer reference
})
```

---

### Handle DataConsumer transportclose Event

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Listens for the 'transportclose' event on a DataConsumer. This event signifies that the transport associated with the DataConsumer has been closed, resulting in the DataConsumer also being closed. This is important for handling connection interruptions.

```javascript
dataConsumer.on("transportclose", () => {
	console.log("transport closed so dataConsumer closed")
})
```

---

### webRtcTransport SCTP State Change

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Emitted when the WebRTC transport's SCTP state changes. This is relevant for DataChannel communication over WebRTC.

````APIDOC
## webRtcTransport.on('sctpstatechange')

### Description
Emitted when the transport SCTP state changes.

### Method
`on(event, callback)`

### Parameters
#### Event Parameters
- **sctpState** (SctpState) - Required - The new SCTP state.

### Example
```javascript
webRtcTransport.on('sctpstatechange', (sctpState) => {
  console.log('WebRTC transport SCTP state changed to:', sctpState);
});
````

````

--------------------------------

### Monitor Connection State Changes (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Listens for the 'connectionstatechange' event on a Mediasoup transport. This event is triggered whenever the transport's connection state changes (e.g., connecting, connected, disconnected, failed). It allows applications to react to the current state of the media connection.

```javascript
transport.on("connectionstatechange", (connectionState) =>
{
  console.log(`Connection state changed: ${connectionState}`);
});

````

---

### Set Max Incoming Bitrate for Transport

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Configures the maximum incoming bitrate for media streams on a transport. This method is effective when REMB (Receiver Estimated Maximum Bitrate) is supported by the remote sender, typically in WebRTC scenarios. Setting the bitrate to 0 effectively removes the limit. This is an asynchronous operation.

```javascript
await transport.setMaxIncomingBitrate(3500000)
```

---

### Worker Closure Management in Mediasoup

Source: https://mediasoup.org/documentation/v3/mediasoup/garbage-collection

Demonstrates how a Worker object in mediasoup can be closed either by explicitly calling the `close()` method or by listening for the `died` event, which indicates an unexpected worker process termination. The application should clean up references when a worker is closed.

```javascript
worker.close()

worker.on("died", () => {
	// Clean up worker reference
})
```

---

### Consumer Listener: OnTransportClose Event

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

An abstract listener class method that is executed when the associated transport is closed. This event automatically closes the consumer. It requires implementation to handle transport closure notifications.

```cpp
void MyConsumerListener::OnTransportClose(mediasoupclient::Consumer* consumer) {
	std::cout << "transport closed" << std::endl;
}
```

---

### DataConsumer Statistics

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a DataConsumer, focusing on data received volume and rate.

````APIDOC
## DataConsumer Statistics

### Description
Retrieves statistics for a DataConsumer, providing information about the data received, including volume and bitrate.

### Method
`getStats()`

### Parameters
This method does not take any parameters.

### Request Example
```javascript
const stats = await dataConsumer.getStats();
````

### Response

#### Success Response (200)

- **type** (string) - The type of the stats ('data-consumer').
- **dataConsumerId** (string) - The ID of the data consumer.
- **timestamp** (number) - The timestamp of the stats in milliseconds.
- **label** (string) - The label of the data channel.
- **protocol** (string) - The protocol of the data channel.
- **bytesReceived** (number) - Total bytes received.
- **messagesReceived** (number) - Total messages received.
- **bandwidth** (number) - The current bandwidth in bps.

````

--------------------------------

### Router Closure Management in Mediasoup

Source: https://mediasoup.org/documentation/v3/mediasoup/garbage-collection

Explains the closure mechanisms for a Router object in mediasoup. A router can be closed by calling `router.close()` or by responding to the `workerclose` event, which fires when the associated worker is closed. The application must manage references accordingly.

```javascript
router.close();

router.on('workerclose', () => {
  // Clean up router reference
});
````

---

### Update Router Media Codecs

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Updates the media codecs utilized by the router. Invoking this method will alter the router's `rtpCapabilities`. Ensure that the `mediaCodecs` array is correctly formatted.

```javascript
if (router.updateMediaCodecs({ producerId, rtpCapabilities })) {
	// Consume the producer by calling transport.consume({ producerId, rtpCapabilities }).
}
```

---

### transport.on('producedata')

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Emitted when the transport needs to transmit information about a new data producer to the associated server-side transport. This event occurs before the produceData() method completes.

````APIDOC
## transport.on('producedata')

### Description
Emitted when the transport needs to transmit information about a new data producer to the associated server side transport. This event occurs before the produceData() method completes.

### Method
Event Listener

### Endpoint
N/A

### Parameters
#### Arguments
- **parameters** (TransportProduceDataParameters) - Parameters to create a server-side data producer.
- **callback** (Function) - A function that must be called with the server-side data producer's ID upon successful creation.
- **errback** (Function) - A function that must be called with an error if the data producer creation fails.

### Request Example
```javascript
transport.on("producedata", async (parameters, callback, errback) =>
{
  try
  {
    const data = await mySignaling.send(
      "transport-producedata",
      {
        transportId          : transport.id,
        sctpStreamParameters : parameters.sctpStreamParameters,
        label                : parameters.label,
        protocol             : parameters.protocol
      });
    const { id } = data;
    callback({ id });
  }
  catch (error)
  {
    errback(error);
  }
});
````

### Response

#### Callback Response

- **id** (String) - The ID of the newly created server-side data producer.

````

--------------------------------

### DataConsumer Data Producer Close Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Handles the 'dataproducerclose' event for a data consumer. This event fires when the associated data producer is closed. Similar to transport closure, the data consumer is also closed upon this event. The listener logs a confirmation message.

```javascript
dataConsumer.on("dataproducerclose", () =>
{
  console.log("associated data producer closed so dataConsumer closed");
});
````

---

### transport.produceData

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Instructs the router to receive data messages. These messages can be delivered by an endpoint via SCTP protocol or sent directly from the Node.js application if the transport is a `DirectTransport`.

````APIDOC
## POST /transport/produceData

### Description
Produces data over the transport, either via SCTP (DataChannel) or a direct transport.

### Method
POST

### Endpoint
/transport/produceData

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
- **sctpStreamParameters** (object) - Optional - Parameters for the SCTP stream, required if using SCTP.
  - **streamId** (number) - Required - The ID of the SCTP stream.
  - **ordered** (boolean) - Required - Whether the stream is ordered.
- **label** (string) - Optional - The label for the DataChannel.
- **protocol** (string) - Optional - The protocol for the DataChannel.
- **appData** (object) - Optional - Custom application data.

### Request Example
```json
// Using SCTP:
{
  "sctpStreamParameters": {
    "streamId": 4,
    "ordered": true
  },
  "label": "chat"
}

// Using a direct transport:
{
  "appData": { "custom": "data" }
}
````

### Response

#### Success Response (200)

- **id** (string) - The ID of the data producer.
- **sctpStreamParameters** (object) - The SCTP stream parameters, if applicable.
- **label** (string) - The label of the DataChannel.
- **protocol** (string) - The protocol of the DataChannel.
- **appData** (object) - Custom application data.

#### Response Example

```json
{
	"id": "dataproducer-id-456",
	"sctpStreamParameters": {
		"streamId": 4,
		"ordered": true
	},
	"label": "chat",
	"protocol": "",
	"appData": {}
}
```

````

--------------------------------

### Close DataProducer (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

This code snippet demonstrates how to close a mediasoup DataProducer. Calling the `close()` method on a DataProducer instance will terminate it and trigger a 'dataproducerclose' event on all associated consumers.

```javascript
dataProducer.close();

````

---

### DataProducer Statistics

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a DataProducer, focusing on data transfer volume and rate.

````APIDOC
## DataProducer Statistics

### Description
Retrieves statistics for a DataProducer, providing information about the data sent, including volume and bitrate.

### Method
`getStats()`

### Parameters
This method does not take any parameters.

### Request Example
```javascript
const stats = await dataProducer.getStats();
````

### Response

#### Success Response (200)

- **type** (string) - The type of the stats ('data-producer').
- **dataProducerId** (string) - The ID of the data producer.
- **timestamp** (number) - The timestamp of the stats in milliseconds.
- **label** (string) - The label of the data channel.
- **protocol** (string) - The protocol of the data channel.
- **bytesSent** (number) - Total bytes sent.
- **messagesSent** (number) - Total messages sent.
- **bandwidth** (number) - The current bandwidth in bps.

````

--------------------------------

### Producer Statistics

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Provides statistics for a Producer, detailing received RTP streams from the producer endpoint.

```APIDOC
## Producer Statistics

### Description
Provides statistics for a Producer, including details about each RTP stream received from the producer. These stats reflect the streams as sent by the producer without modification.

### Method
`getStats()`

### Parameters
This method does not take any parameters.

### Request Example
```javascript
const stats = await producer.getStats();
````

### Response

#### Success Response (200)

- **type** (string) - The type of the stats (e.g., 'outbound-rtp', 'inbound-rtp').
- **mediaKind** (string) - The kind of media ('audio' or 'video').
- **mid** (string) - The media ID associated with the RTP stream.
- **trackId** (string) - The ID of the media track.
- **transportId** (string) - The ID of the associated transport.
- **codecId** (string) - The ID of the codec used.
- **ssrc** (number) - The SSRC of the RTP stream.
- **packetsSent** (number) - Total packets sent for this stream.
- **packetsReceived** (number) - Total packets received for this stream.
- **bytesSent** (number) - Total bytes sent for this stream.
- **bytesReceived** (number) - Total bytes received for this stream.
- **bitrate** (number) - The current bitrate in bps.
- **jitter** (number) - The current jitter in milliseconds.
- **roundTripTime** (number) - The current round-trip time in milliseconds.
- **packetLossPercentage** (number) - The percentage of packet loss.
- **timestamp** (number) - The timestamp of the stats in milliseconds.

_Note: The specific fields may vary slightly depending on whether it's an inbound or outbound RTP stream._

````

--------------------------------

### SendTransportListener::OnProduceData

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Emitted when the transport needs to transmit information about a new data producer to the associated server side transport. This event occurs before the produceData() method completes.

```APIDOC
## SendTransportListener::OnProduceData

### Description
Emitted when the transport needs to transmit information about a new data producer to the associated server side transport. This event occurs before the produceData() method completes.

### Method
`SendTransportListener::OnProduceData`

### Parameters
#### Path Parameters
None

#### Query Parameters
None

#### Request Body
This method does not directly take a request body in the typical REST sense. The arguments passed to the function are used to construct the request payload.
- **transport** (SendTransport*) - SendTransport instance.
- **sctpStreamParameters** (const nlohmann::json&)
- **label** (const std::string&) - A label which can be used to distinguish this DataChannel from others.
- **protocol** (const std::string&) - Name of the sub-protocol used by this DataChannel.
- **appData** (const nlohmann::json&) - Custom application data as given in the `transport.produceData()` method.

### Request Example
```cpp
std::future<std::string> MySendTransportListener::OnProduceData(
		SendTransport* transport,
		const nlohmann::json& sctpStreamParameters,
		const std::string& label,
		const std::string& protocol,
		const nlohmann::json& appData)
{
	std::promise<std::string> promise;

	json body =
	{
		{ "transportId",          transport->GetId()   },
		{ "sctpStreamParameters", sctpStreamParameters },
		{ "label",                label                },
		{ "protocol",             protocol             },
		{ "appData",              appData              }
	};

	json response = mySignaling.send("transport-produce-data", body);

  // Read the id in the response.
	auto idIt = response.find("id");
	if (idIt == response.end() || !idIt->is_string())
  {
		promise.set_exception(
      std::make_exception_ptr("'id' missing/invalid in response"));
  }

  // Fulfil the promise with the id in the response and return its future.
	promise.set_value(idIt->get<std::string>());

	return promise.get_future();
}
````

### Response

#### Success Response (200)

- **id** (std::string) - The ID of the data producer created in the server-side mediasoup.

#### Response Example

```json
{
	"id": "some-data-producer-id"
}
```

````

--------------------------------

### Handle WebRtcServer Worker Close Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'workerclose' event on a WebRtcServer. This event indicates that the associated worker has been closed, resulting in the WebRtcServer also being closed.

```javascript
webRtcServer.on("workerclose", () =>
{
  console.log("worker closed so webRtcServer closed");
});
````

---

### Handle Transport Closure (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Listens for the 'close' event on the transport's observer. This event is emitted when the transport is closed for any reason. It's essential for performing cleanup tasks or notifying the user interface when the transport is no longer active.

```javascript
transport.observer.on("close", () => {
	console.log("Transport closed")
})
```

---

### RTPObserver Remove Producer Method

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Removes a previously added producer from an RTP observer's monitoring list. This method requires an options object with the `producerId` to specify which producer to remove. The operation is asynchronous.

```javascript
await rtpObserver.removeProducer({ producerId: "some-producer-id" })
```

---

### WebRtcTransport Statistics

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a WebRtcTransport. This includes information about DTLS state, ICE state, bitrate, packet counts, and transport details.

````APIDOC
## WebRtcTransport Statistics

### Description
Retrieves statistics for a WebRtcTransport, providing insights into network conditions, data transfer, and connection state.

### Method
`getStats()`

### Parameters
This method does not take any parameters.

### Request Example
```javascript
const stats = await webRtcTransport.getStats();
````

### Response

#### Success Response (200)

- **availableOutgoingBitrate** (number) - The available outgoing bitrate in bps.
- **bytesReceived** (number) - Total bytes received.
- **bytesSent** (number) - Total bytes sent.
- **dtlsState** (string) - The DTLS state of the transport (e.g., 'connected', 'connecting', 'failed').
- **iceRole** (string) - The ICE role of the transport ('controlling' or 'controlled').
- **iceSelectedTuple** (object) - Information about the selected ICE tuple.
  - **localAddress** (string) - The local IP address.
  - **localPort** (number) - The local port.
  - **protocol** (string) - The transport protocol ('udp' or 'tcp').
  - **remoteIp** (string) - The remote IP address.
  - **remotePort** (number) - The remote port.
- **iceState** (string) - The ICE state of the transport (e.g., 'new', 'checking', 'connected', 'completed', 'failed').
- **maxIncomingBitrate** (number) - The maximum allowed incoming bitrate in bps.
- **probationBytesSent** (number) - Bytes sent during probation phase.
- **probationSendBitrate** (number) - Send bitrate during probation phase.
- **recvBitrate** (number) - Current received bitrate in bps.
- **rtpBytesReceived** (number) - Total RTP bytes received.
- **rtpBytesSent** (number) - Total RTP bytes sent.
- **rtpPacketLossSent** (number) - RTP packet loss count for sent packets.
- **rtpRecvBitrate** (number) - Current RTP received bitrate in bps.
- **rtpSendBitrate** (number) - Current RTP send bitrate in bps.
- **rtxBytesReceived** (number) - Total RTX bytes received.
- **rtxBytesSent** (number) - Total RTX bytes sent.
- **rtxRecvBitrate** (number) - Current RTX received bitrate in bps.
- **rtxSendBitrate** (number) - Current RTX send bitrate in bps.
- **sctpState** (string) - The SCTP state of the transport (e.g., 'connecting', 'connected').
- **sendBitrate** (number) - Current send bitrate in bps.
- **timestamp** (number) - The timestamp of the stats in milliseconds.
- **transportId** (string) - The unique identifier of the transport.
- **type** (string) - The type of the stats ('webrtc-transport').

#### Response Example

```json
[
	{
		"availableOutgoingBitrate": 6750000,
		"bytesReceived": 5360091,
		"bytesSent": 20988,
		"dtlsState": "connected",
		"iceRole": "controlled",
		"iceSelectedTuple": {
			"localAddress": "11.22.33.44",
			"localPort": 56726,
			"protocol": "udp",
			"remoteIp": "55.66.77.88",
			"remotePort": 52320
		},
		"iceState": "completed",
		"maxIncomingBitrate": 5500000,
		"probationBytesSent": 0,
		"probationSendBitrate": 0,
		"recvBitrate": 1802072,
		"rtpBytesReceived": 5104571,
		"rtpBytesSent": 0,
		"rtpPacketLossSent": 0,
		"rtpRecvBitrate": 1835651,
		"rtpSendBitrate": 0,
		"rtxBytesReceived": 179934,
		"rtxBytesSent": 0,
		"rtxRecvBitrate": 0,
		"rtxSendBitrate": 0,
		"sctpState": "connected",
		"sendBitrate": 4992,
		"timestamp": 18079607138,
		"transportId": "a00746bd-0758-4dfc-9f5f-c0ad4eb326d5",
		"type": "webrtc-transport"
	}
]
```

````

--------------------------------

### Handle Track End Event for Producer (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

This event is emitted when the audio or video track being sent by the producer is stopped externally, such as a microphone or webcam being disconnected. It provides an opportunity to close, pause the producer, or replace its track.

```javascript
producer.on("trackended", () =>
{
  console.log("track ended");
});
````

---

### Set Min Outgoing Bitrate for Transport

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Establishes the minimum outgoing bitrate for media streams sent by mediasoup. This method overrides the estimated outgoing bitrate if the provided value is higher. It relies on transport congestion control availability in the remote receiver, typically found in WebRTC. The method is asynchronous, and a value of 0 signifies no limit.

```javascript
await transport.setMinOutgoingBitrate(1000000)
```

---

### Handle Router Worker Close Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'workerclose' event on a router. This event signifies that the associated worker has been closed, leading to the router also being closed.

```javascript
router.on("workerclose", () => {
	console.log("worker closed so router closed")
})
```

---

### Transport Close Method

Source: https://mediasoup.org/documentation/v3/libmediasoupclient/api

Closes the transport instance and all associated producers and consumers. This method should be invoked when the server-side transport is closed to maintain synchronization.

```cpp
transport.Close();

```

---

### Handle Worker Died Event (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'died' event, which is emitted when the worker subprocess unexpectedly terminates. This is a critical event for monitoring worker health and should include error handling.

```javascript
worker.on("died", (error) => {
	console.error("mediasoup worker died!: %o", error)
})
```

---

### PlainTransport Statistics

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a PlainTransport. This includes data transfer volume, bitrate, and tuple information.

````APIDOC
## PlainTransport Statistics

### Description
Retrieves statistics for a PlainTransport, focusing on data transfer rates and the network endpoint configuration.

### Method
`getStats()`

### Parameters
This method does not take any parameters.

### Request Example
```javascript
const stats = await plainTransport.getStats();
````

### Response

#### Success Response (200)

- **bytesReceived** (number) - Total bytes received.
- **bytesSent** (number) - Total bytes sent.
- **comedia** (boolean) - Indicates if Comedia is enabled.
- **rtcpMux** (boolean) - Indicates if RTCP muxing is enabled.
- **probationBytesSent** (number) - Bytes sent during probation phase.
- **probationSendBitrate** (number) - Send bitrate during probation phase.
- **recvBitrate** (number) - Current received bitrate in bps.
- **rtpBytesReceived** (number) - Total RTP bytes received.
- **rtpBytesSent** (number) - Total RTP bytes sent.
- **rtpRecvBitrate** (number) - Current RTP received bitrate in bps.
- **rtpSendBitrate** (number) - Current RTP send bitrate in bps.
- **rtxBytesReceived** (number) - Total RTX bytes received.
- **rtxBytesSent** (number) - Total RTX bytes sent.
- **rtxRecvBitrate** (number) - Current RTX received bitrate in bps.
- **rtxSendBitrate** (number) - Current RTX send bitrate in bps.
- **sendBitrate** (number) - Current send bitrate in bps.
- **timestamp** (number) - The timestamp of the stats in milliseconds.
- **transportId** (string) - The unique identifier of the transport.
- **tuple** (object) - Information about the network tuple.
  - **localAddress** (string) - The local IP address.
  - **localPort** (number) - The local port.
  - **protocol** (string) - The transport protocol ('udp' or 'tcp').
  - **remoteIp** (string) - The remote IP address.
  - **remotePort** (number) - The remote port.
- **type** (string) - The type of the stats ('plain-rtp-transport').

#### Response Example

```json
[
	{
		"bytesReceived": 467406,
		"bytesSent": 2550,
		"comedia": true,
		"rtcpMux": true,
		"probationBytesSent": 0,
		"probationSendBitrate": 0,
		"recvBitrate": 1802072,
		"rtpBytesReceived": 5104571,
		"rtpBytesSent": 0,
		"rtpRecvBitrate": 1835651,
		"rtpSendBitrate": 0,
		"rtxBytesReceived": 0,
		"rtxBytesSent": 0,
		"rtxRecvBitrate": 0,
		"rtxSendBitrate": 0,
		"sendBitrate": 24,
		"timestamp": 924308648,
		"transportId": "8e7dc219-5cb0-4cca-b1ca-0bbbc584a364",
		"tuple": {
			"localAddress": "11.22.33.44",
			"localPort": 45346,
			"protocol": "udp",
			"remoteIp": "55.66.77.88",
			"remotePort": 56971
		},
		"type": "plain-rtp-transport"
	}
]
```

````

--------------------------------

### Handle ICE Candidate Errors (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

Listens for the 'icecandidateerror' event on a Mediasoup transport. This event is emitted when an error occurs during ICE negotiation, typically when interacting with STUN or TURN servers. The event object contains detailed information about the error, which can be used for debugging network issues.

```javascript
transport.on("icecandidateerror", (event) =>
{
  console.error("ICE candidate error:", event);
});

````

---

### Handle Transport Close Event for Producer (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

This event is emitted when the transport associated with the producer is closed. When this event fires, the producer is also automatically closed. The callback logs a message indicating the transport closure.

```javascript
producer.on("transportclose", () => {
	console.log("transport closed so producer closed")
})
```

---

### rtpObserver listenererror Event Handler

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Handles the 'listenererror' event emitted when an application-provided event listener throws an error. This allows the application to be aware of exceptions occurring within its event listeners without disrupting the internal state of the RTP observer. The handler receives the event name and the error object.

```javascript
rtpObserver.on("listenererror", (eventName, error) => {
	console.error(`Error in event listener '${eventName}':`, error)
})
```

---

### DataConsumer Transport Close Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Listens for the 'transportclose' event on a data consumer. This event is emitted when the associated transport is closed. The data consumer is also automatically closed when this event occurs. The callback function logs a message indicating the transport closure.

```javascript
dataConsumer.on("transportclose", () => {
	console.log("transport closed so dataConsumer closed")
})
```

---

### Transport Closure Management in Mediasoup

Source: https://mediasoup.org/documentation/v3/mediasoup/garbage-collection

Details how to handle the closure of a Transport object in mediasoup. Closure occurs via `transport.close()` or the `routerclose` event, emitted when the parent router is closed. Applications must ensure references are removed upon transport closure.

```javascript
transport.close()

transport.on("routerclose", () => {
	// Clean up transport reference
})
```

---

### DirectTransport Statistics

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a DirectTransport. This includes bitrate and packet counts.

````APIDOC
## DirectTransport Statistics

### Description
Retrieves statistics for a DirectTransport, which is a simplified transport for specific use cases. It provides bitrate information.

### Method
`getStats()`

### Parameters
This method does not take any parameters.

### Request Example
```javascript
const stats = await directTransport.getStats();
````

### Response

#### Success Response (200)

- **probationBytesSent** (number) - Bytes sent during probation phase.
- **probationSendBitrate** (number) - Send bitrate during probation phase.
- **recvBitrate** (number) - Current received bitrate in bps.
- **rtpBytesReceived** (number) - Total RTP bytes received.
- **rtpBytesSent** (number) - Total RTP bytes sent.
- **rtpRecvBitrate** (number) - Current RTP received bitrate in bps.
- **rtpSendBitrate** (number) - Current RTP send bitrate in bps.
- **rtxBytesReceived** (number) - Total RTX bytes received.
- **rtxBytesSent** (number) - Total RTX bytes sent.
- **rtxRecvBitrate** (number) - Current RTX received bitrate in bps.
- **rtxSendBitrate** (number) - Current RTX send bitrate in bps.
- **sendBitrate** (number) - Current send bitrate in bps.
- **timestamp** (number) - The timestamp of the stats in milliseconds.
- **transportId** (string) - The unique identifier of the transport.
- **type** (string) - The type of the stats ('direct-transport').

#### Response Example

```json
[
	{
		"probationBytesSent": 0,
		"probationSendBitrate": 0,
		"recvBitrate": 5672,
		"rtpBytesReceived": 0,
		"rtpBytesSent": 0,
		"rtpRecvBitrate": 0,
		"rtpSendBitrate": 0,
		"rtxBytesReceived": 0,
		"rtxBytesSent": 0,
		"rtxRecvBitrate": 0,
		"rtxSendBitrate": 0,
		"sendBitrate": 3204,
		"timestamp": 894308981,
		"transportId": "huif60cd-10ac-443b-8529-6474ecba2123",
		"type": "direct-transport"
	}
]
```

````

--------------------------------

### DataConsumer Listener Error Event

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Handles the 'listenererror' event for a data consumer. This event is emitted when an application-provided event listener throws an error. The internal state is not broken, but this event allows the application to be notified of such exceptions. It provides the event name and the error object.

```javascript
dataConsumer.on("listenererror", (eventName, error) =>
{
  console.error("Error in data consumer event listener:", eventName, error);
});
````

---

### PipeTransport Statistics

Source: https://mediasoup.org/documentation/v3/mediasoup/rtc-statistics

Retrieves statistics for a PipeTransport. This includes data transfer rates and connection details.

````APIDOC
## PipeTransport Statistics

### Description
Retrieves statistics for a PipeTransport, which is used for media routing between mediasoup instances. It provides data transfer metrics.

### Method
`getStats()`

### Parameters
This method does not take any parameters.

### Request Example
```javascript
const stats = await pipeTransport.getStats();
````

### Response

#### Success Response (200)

- **probationBytesSent** (number) - Bytes sent during probation phase.
- **probationSendBitrate** (number) - Send bitrate during probation phase.
- **recvBitrate** (number) - Current received bitrate in bps.
- **rtpBytesReceived** (number) - Total RTP bytes received.
- **rtpBytesSent** (number) - Total RTP bytes sent.
- **rtpRecvBitrate** (number) - Current RTP received bitrate in bps.
- **rtpSendBitrate** (number) - Current RTP send bitrate in bps.
- **rtxBytesReceived** (number) - Total RTX bytes received.
- **rtxBytesSent** (number) - Total RTX bytes sent.
- **rtxRecvBitrate** (number) - Current RTX received bitrate in bps.
- **rtxSendBitrate** (number) - Current RTX send bitrate in bps.
- **sendBitrate** (number) - Current send bitrate in bps.
- **timestamp** (number) - The timestamp of the stats in milliseconds.
- **transportId** (string) - The unique identifier of the transport.
- **tuple** (object) - Information about the network tuple.
  - **localAddress** (string) - The local IP address.
  - **localPort** (number) - The local port.
  - **protocol** (string) - The transport protocol ('udp' or 'tcp').
  - **remoteIp** (string) - The remote IP address.
  - **remotePort** (number) - The remote port.
- **type** (string) - The type of the stats ('pipe-transport').

#### Response Example

```json
[
	{
		"probationBytesSent": 0,
		"probationSendBitrate": 0,
		"recvBitrate": 1802072,
		"rtpBytesReceived": 5104571,
		"rtpBytesSent": 0,
		"rtpRecvBitrate": 1835651,
		"rtpSendBitrate": 0,
		"rtxBytesReceived": 0,
		"rtxBytesSent": 0,
		"rtxRecvBitrate": 0,
		"rtxSendBitrate": 0,
		"sendBitrate": 24,
		"timestamp": 924308980,
		"transportId": "352f60cd-10ac-443b-8529-6474ecba2e46",
		"tuple": {
			"localAddress": "11.22.33.44",
			"localPort": 12455,
			"protocol": "udp",
			"remoteIp": "11.22.33.44",
			"remotePort": 42301
		},
		"type": "pipe-transport"
	}
]
```

````

--------------------------------

### Handle RTCP Event on DirectTransport

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Emitted when the direct transport receives an RTCP packet from its router. This event is specific to direct transports. The handler receives a Node.js Buffer containing the RTCP packet, which could be compound or standalone.

```javascript
directTransport.on("rtcp", (rtcpPacket) =>
{
  // Do stuff with the binary RTCP packet.
});
````

---

### Worker Observer: Close Event (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Attaches a listener to the worker's observer for the 'close' event. This event is emitted when the worker is closed (e.g., via `worker.close()`). It signals that the worker process has terminated.

```javascript
import * as mediasoup from "mediasoup"

async function setupWorkerCloseListener(worker) {
	worker.observer.on("close", () => {
		console.log(`Worker ${worker.pid} has been closed.`)
		// Perform cleanup actions here
	})
	console.log(`Listening for 'close' event on worker ${worker.pid}.`)
}

// Example usage (assuming 'myWorker' is an existing Worker instance):
// setupWorkerCloseListener(myWorker);
// // Later, to trigger the event:
// // myWorker.close();
```

---

### webRtcTransport DTLS State Change

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Emitted when the WebRTC transport's DTLS state changes. This event is crucial for monitoring the security state of the connection.

````APIDOC
## webRtcTransport.on('dtlsstatechange')

### Description
Emitted when the transport DTLS state changes.

### Method
`on(event, callback)`

### Parameters
#### Event Parameters
- **dtlsState** (DtlsState) - Required - The new DTLS state.

### Event Details
- This event will be emitted with `dtlsState: 'closed'` when the remote endpoint sends a DTLS Close Alert message. If so, this event will be emitted before the `icestatechange` event with `iceState: 'disconnected'`. The application should close the transport when this happens since it's not recoverable.

### Example
```javascript
webRtcTransport.on('dtlsstatechange', (dtlsState) => {
  console.log('WebRTC transport DTLS state changed to:', dtlsState);
});
````

````

--------------------------------

### Set Max Outgoing Bitrate for Transport

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Sets the maximum outgoing bitrate for media streams sent by mediasoup to the remote endpoint. This method overrides the estimated outgoing bitrate if the provided value is lower. It requires transport congestion control support from the remote receiver, common in WebRTC. The operation is asynchronous, and setting the bitrate to 0 removes the limit.

```javascript
await transport.setMaxOutgoingBitrate(2000000);
````

---

### Mediasoup Consumer: Handle Transport Close Event (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

This snippet demonstrates how to listen for the 'transportclose' event on a Mediasoup consumer. This event is emitted when the associated transport is closed, automatically closing the consumer as well. It's crucial for managing resources and ensuring proper cleanup.

```javascript
consumer.on("transportclose", () => {
	console.log("transport closed so consumer closed")
})
```

---

### Limit Highest Spatial Layer for Producer (JavaScript)

Source: https://mediasoup.org/documentation/v3/mediasoup-client/api

This method limits the highest RTP stream being transmitted by a producer. It's used in simulcast scenarios to control which quality layers are sent to the server. It requires the index of the desired highest layer from the encodings array.

```javascript
await producer.setMaxSpatialLayer(1)
```

---

### Check if Worker is Closed - JavaScript

Source: https://mediasoup.org/documentation/v3/mediasoup/api

Determines if a mediasoup worker instance has been closed. This read-only boolean property indicates the current state of the worker.

```javascript
console.log(worker.closed)
// => false
```
