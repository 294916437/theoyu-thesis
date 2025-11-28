use anyhow::Result;
use std::sync::Arc;
use tokio::sync::Mutex;
use warp::Filter;
use webrtc::api::interceptor_registry::register_default_interceptors;
use webrtc::api::media_engine::MediaEngine;
use webrtc::api::APIBuilder;
use webrtc::ice_transport::ice_server::RTCIceServer;
use webrtc::interceptor::registry::Registry;
use webrtc::peer_connection::configuration::RTCConfiguration;
use webrtc::peer_connection::sdp::session_description::RTCSessionDescription;
use webrtc::peer_connection::RTCPeerConnection;
use webrtc::track::track_local::track_local_static_rtp::TrackLocalStaticRTP;
use webrtc::track::track_local::{TrackLocal, TrackLocalWriter};

// 全局共享状态
struct AppState {
    video_track: Option<Arc<TrackLocalStaticRTP>>,
    audio_track: Option<Arc<TrackLocalStaticRTP>>,
    // *** 关键修复：保存发布者和订阅者的 PeerConnection ***
    publisher_pc: Option<Arc<RTCPeerConnection>>,
    subscriber_pcs: Vec<Arc<RTCPeerConnection>>,
}

lazy_static::lazy_static! {
    static ref APP_STATE: Arc<Mutex<AppState>> = Arc::new(Mutex::new(AppState {
        video_track: None,
        audio_track: None,
        publisher_pc: None,
        subscriber_pcs: Vec::new(),
    }));
}

#[tokio::main]
async fn main() -> Result<()> {
    // 1. 定义发布路由
    let publish = warp::post()
        .and(warp::path("publish"))
        .and(warp::body::json())
        .and_then(handle_publish);

    // 2. 定义订阅路由
    let subscribe = warp::post()
        .and(warp::path("subscribe"))
        .and(warp::body::json())
        .and_then(handle_subscribe);

    // 3. 增强的 CORS 配置
    let cors = warp::cors()
        .allow_any_origin()
        .allow_methods(vec!["POST", "GET", "OPTIONS"])
        .allow_headers(vec![
            "content-type",
            "user-agent",
            "sec-fetch-mode",
            "referrer",
        ]);

    // 4. 组合路由并应用 CORS
    let routes = publish.or(subscribe).with(cors);

    println!("SFU Server running at http://127.0.0.1:8080");

    // 启动服务器
    warp::serve(routes).run(([127, 0, 0, 1], 8080)).await;

    Ok(())
}

// 创建 WebRTC API 实例的辅助函数
async fn create_api() -> Result<webrtc::api::API> {
    let mut media_engine = MediaEngine::default();
    media_engine.register_default_codecs()?;

    let mut registry = Registry::new();
    registry = register_default_interceptors(registry, &mut media_engine)?;

    let api = APIBuilder::new()
        .with_media_engine(media_engine)
        .with_interceptor_registry(registry)
        .build();
    Ok(api)
}

// 处理发布者请求
async fn handle_publish(sdp_str: String) -> Result<impl warp::Reply, warp::Rejection> {
    let api = create_api().await.unwrap();
    let config = RTCConfiguration {
        ice_servers: vec![RTCIceServer {
            urls: vec!["stun:stun.l.google.com:19302".to_owned()],
            ..Default::default()
        }],
        ..Default::default()
    };

    let peer_connection = Arc::new(api.new_peer_connection(config).await.unwrap());
    let pc_clone = Arc::clone(&peer_connection);

    // *** 核心逻辑：处理远端传来的 Track ***
    peer_connection.on_track(Box::new(|track, _receiver, _| {
        Box::pin(async move {
            println!("Publisher: 收到 Track: {:?}", track.kind());

            // 创建一个本地的 RTP Track 用于转发
            let local_track = Arc::new(TrackLocalStaticRTP::new(
                track.codec().capability.clone(),
                "sfu_track".to_owned(),
                "sfu_stream".to_owned(),
            ));

            // 将这个 Track 保存到全局状态中，供订阅者使用
            let mut state = APP_STATE.lock().await;
            if track.kind() == webrtc::rtp_transceiver::rtp_codec::RTPCodecType::Video {
                state.video_track = Some(local_track.clone());
                println!("Publisher: 视频 Track 已保存到全局状态");
            } else {
                state.audio_track = Some(local_track.clone());
                println!("Publisher: 音频 Track 已保存到全局状态");
            }
            drop(state);

            // 循环读取发布者的 RTP 包，并写入到 local_track (即转发)
            let mut rtp_count = 0;
            loop {
                match track.read_rtp().await {
                    Ok((rtp, _)) => {
                        rtp_count += 1;
                        if rtp_count % 100 == 0 {
                            println!("Publisher: 已转发 {} 个 RTP 包", rtp_count);
                        }
                        // 直接转发 RTP 包
                        if let Err(e) = local_track.write_rtp(&rtp).await {
                            println!("转发 RTP 失败: {}", e);
                            break;
                        }
                    }
                    Err(e) => {
                        println!("Publisher: 读取 RTP 失败: {}", e);
                        break;
                    }
                }
            }
            println!("Publisher: Track 读取循环结束");
        })
    }));

    // 设置 SDP
    let offer = RTCSessionDescription::offer(sdp_str).unwrap();
    peer_connection.set_remote_description(offer).await.unwrap();

    let answer = peer_connection.create_answer(None).await.unwrap();
    peer_connection
        .set_local_description(answer.clone())
        .await
        .unwrap();

    // 等待 ICE 收集完成
    let mut gather_complete = peer_connection.gathering_complete_promise().await;
    let _ = gather_complete.recv().await;

    let local_desc = peer_connection.local_description().await.unwrap();

    // *** 关键修复：保存 PeerConnection 到全局状态 ***
    let mut state = APP_STATE.lock().await;
    state.publisher_pc = Some(pc_clone);
    drop(state);

    println!("Publisher: PeerConnection 已保存");

    Ok(warp::reply::json(&local_desc.sdp))
}

// 处理订阅者请求
async fn handle_subscribe(sdp_str: String) -> Result<impl warp::Reply, warp::Rejection> {
    let api = create_api().await.unwrap();
    let config = RTCConfiguration {
        ice_servers: vec![RTCIceServer {
            urls: vec!["stun:stun.l.google.com:19302".to_owned()],
            ..Default::default()
        }],
        ..Default::default()
    };

    let peer_connection = Arc::new(api.new_peer_connection(config).await.unwrap());
    let pc_clone = Arc::clone(&peer_connection);

    // *** 先添加 Track，再设置远端描述 ***
    let state = APP_STATE.lock().await;
    if let Some(video_track) = &state.video_track {
        let sender = peer_connection
            .add_track(Arc::clone(video_track) as Arc<dyn TrackLocal + Send + Sync>)
            .await
            .unwrap();

        tokio::spawn(async move {
            let mut rtcp_buf = vec![0u8; 1500];
            while let Ok((_, _)) = sender.read(&mut rtcp_buf).await {}
        });

        println!("Subscriber: 已添加视频 Track");
    } else {
        println!("Subscriber: 警告 - 没有可用的视频 Track");
    }

    if let Some(audio_track) = &state.audio_track {
        let sender = peer_connection
            .add_track(Arc::clone(audio_track) as Arc<dyn TrackLocal + Send + Sync>)
            .await
            .unwrap();

        tokio::spawn(async move {
            let mut rtcp_buf = vec![0u8; 1500];
            while let Ok((_, _)) = sender.read(&mut rtcp_buf).await {}
        });
        println!("Subscriber: 已添加音频 Track");
    }
    drop(state);

    // 设置远端 SDP
    let offer = RTCSessionDescription::offer(sdp_str).unwrap();
    peer_connection.set_remote_description(offer).await.unwrap();

    // 创建 Answer
    let answer = peer_connection.create_answer(None).await.unwrap();
    peer_connection
        .set_local_description(answer.clone())
        .await
        .unwrap();

    let mut gather_complete = peer_connection.gathering_complete_promise().await;
    let _ = gather_complete.recv().await;

    let local_desc = peer_connection.local_description().await.unwrap();

    // *** 保存订阅者的 PeerConnection ***
    let mut state = APP_STATE.lock().await;
    state.subscriber_pcs.push(pc_clone);
    drop(state);

    println!("Subscriber: 已创建 Answer SDP 并保存 PeerConnection");

    Ok(warp::reply::json(&local_desc.sdp))
}
