# React Native Meeting Room Test Cases

This checklist covers the pure React Native meeting room flow based on `react-native-webrtc`, `mediasoup-client`, the backend `/media/room/*` APIs, and the SFU Socket.io signaling protocol.

## Meeting Creation And Join

| Case | Steps | Expected Result |
| --- | --- | --- |
| Create an instant meeting | Select "立即会议" and create a meeting. | Calls `/media/room/create` with `type=1`; enters pre-join preview after success. |
| Create a scheduled meeting | Select "预约会议", enter a future date and time, then create. | Calls `/media/room/create` with `type=2`; returns to the meetings list after success. |
| Reject invalid scheduled time | Create a scheduled meeting with a start time earlier than now. | Submission is blocked and the app shows "预约会议开始时间必须晚于当前时间". |
| Validate meeting number | Enter a valid meeting number and validate it. | Calls `/media/room/validate-no`; shows the meeting card. |
| Join pre-validation succeeds | Tap join for an allowed meeting. | Calls `/media/room/join`; enters pre-join preview when `allowed=true`. |
| Join pre-validation fails | Backend returns `allowed=false`. | Does not enter the room and shows the backend message. |

## Local Media Publishing

| Case | Steps | Expected Result |
| --- | --- | --- |
| Join with mic and camera enabled | Grant camera and microphone permissions, then enter the room. | Creates send/recv transports, publishes local audio/video producers, and shows the local camera preview. |
| Join with camera disabled | Disable camera in pre-join, then enter the room. | Does not publish video; local tile shows avatar or "摄像头已关闭". |
| Join with microphone disabled | Disable microphone in pre-join, then enter the room. | Audio is not published or is disabled; local member state shows muted. |
| Deny camera permission | Deny Android `CAMERA`. | Does not enter the meeting and shows the camera permission hint. |
| Deny microphone permission | Deny Android `RECORD_AUDIO`. | Does not enter the meeting and shows the microphone permission hint. |

## Remote Subscription And Multi-Participant Media

| Case | Steps | Expected Result |
| --- | --- | --- |
| Second participant joins | A is in the room; B joins. | A receives `newPeer/newProducer`; B consumes A's producers; both sides see video and hear audio. |
| Third participant joins | A and B are in the room; C joins. | C consumes existing A/B producers; A/B receive and consume C's producers. |
| Producer arrives before recv transport is ready | Publish a producer while a joining client is still creating recv transport. | Producer is queued and consumed after recv transport and device are ready. |
| Remote audio-only stream | Remote participant disables video but keeps audio enabled. | Local client still hears audio and shows no remote video frame. |
| Remote participant leaves | B leaves the meeting. | A/C remove B from the participant list and clean B's consumers, tracks, producer mappings, and remote stream. |
| Remote producer closes | B closes video producer. | A/C switch B's tile to "摄像头已关闭" and do not keep stale video tracks. |

## Participant Self-Control

| Case | Steps | Expected Result |
| --- | --- | --- |
| Self mute | Tap "静音". | Local audio track is disabled, SFU `toggleAudio` succeeds, and remote clients show the participant as muted. |
| Self unmute | Tap "开麦". | Local audio track is enabled or re-produced, and remote clients show audio restored. |
| Self disable video | Tap "关视频". | Local video track is disabled, SFU broadcasts `producerStateChanged`, and remote clients show camera disabled. |
| Self enable video | Tap "开视频". | Local video preview and remote video display recover. |
| Switch camera | Tap "切换摄像头". | Front/back camera switches without permanently breaking the producer. |

## Host Controls

| Case | Steps | Expected Result |
| --- | --- | --- |
| Host mutes one participant | Host mutes participant B. | SFU `hostToggleAudio` succeeds; B receives the control, disables the local audio track, and all clients show B muted. |
| Host disables one participant's video | Host disables participant B's video. | B receives the control, disables the local video track, and all clients show B's camera disabled. |
| Host mutes all | Host taps "全体静音". | Non-host participants are muted and UI states sync across clients. |
| Host disables all video | Host taps "全体关视频". | Non-host participants' video is disabled and UI states sync across clients. |
| Host removes a participant | Host removes B. | B receives `removedFromRoom` and exits; other clients remove B from the member list. |
| Non-host attempts host control | A regular member bypasses UI and sends a host control event. | Server should reject the request; SFU-side host authorization must be verified. |

## Spotlight

| Case | Steps | Expected Result |
| --- | --- | --- |
| Local spotlight selection | Tap a member's small tile. | Main stage switches to that member. |
| Host sets spotlight | Host selects B as spotlight. | All clients receive `spotlightChanged` and show B on the main stage. |
| Host clears spotlight | Host taps "取消焦点". | All clients leave global spotlight mode and return to local or active-speaker layout. |
| Regular member requests spotlight | B taps "申请焦点". | Host receives `spotlightRequest`. |
| Spotlight layer optimization | Set B as spotlight. | B's video consumer requests high layers; non-spotlight video consumers request lower layers. |

## Mobile Network And Lifecycle

| Case | Steps | Expected Result |
| --- | --- | --- |
| Socket disconnects | Disable network during a meeting. | UI enters reconnect/recovering state and does not crash. |
| Network recovers | Re-enable network. | Socket reconnects, `recover()` runs, the client rejoins, recreates transports, republishes local media, and resubscribes remote media. |
| App returns from background | Press Home, then reopen the app. | If media is not ready, the app calls `recover()`. |
| Switch Wi-Fi and cellular | Change network type during a meeting. | Media path recovers and remote clients see local media again. |
| Lock screen recovery | Lock the device for a while, then unlock. | The meeting recovers without stale consumers or streams. |

## Shutdown And Cleanup

| Case | Steps | Expected Result |
| --- | --- | --- |
| Participant leaves normally | Tap hang up. | Local tracks stop, producers/consumers/transports close, and socket leave/disconnect runs. |
| Host closes meeting | Host closes the meeting. | All clients receive `roomClosed` and exit the room. |
| Repeated join and leave | Join and leave the same room five times. | No duplicate remote tiles, stale streams, or obvious memory growth. |
| Long multi-participant meeting | Keep at least three participants in a meeting for 30 minutes. | Remote states remain stable, join/leave cleanup works, and audio routing remains correct. |

## Priority Regression Set

Run these first when time is limited:

1. Three-participant join and remote subscription.
2. Producer arrives before recv transport is ready.
3. Host-forced audio/video disable.
4. Network disconnect and recovery.
5. `peerLeft` and `producerClosed` cleanup.
6. Spotlight layout and simulcast layer optimization.
