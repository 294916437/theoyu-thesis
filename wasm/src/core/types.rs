use wasm_bindgen::prelude::*;

/// 视频帧尺寸
#[derive(Clone, Copy, Debug)]
pub struct Dimensions {
    pub width: usize,
    pub height: usize,
}

impl Dimensions {
    pub fn new(width: usize, height: usize) -> Self {
        Self { width, height }
    }

    pub fn pixel_count(&self) -> usize {
        self.width * self.height
    }

    pub fn rgba_size(&self) -> usize {
        self.pixel_count() * 4
    }
}

/// 效果强度参数
#[wasm_bindgen]
#[derive(Clone, Copy, Debug)]
pub struct EffectParams {
    pub blur_radius: f32,
    pub edge_softness: f32,
    pub temporal_smoothing: f32,
}

#[wasm_bindgen]
impl EffectParams {
    #[wasm_bindgen(constructor)]
    pub fn new() -> Self {
        Self::default()
    }

    pub fn set_blur_radius(&mut self, radius: f32) {
        self.blur_radius = radius.clamp(0.0, 20.0);
    }

    pub fn set_edge_softness(&mut self, softness: f32) {
        self.edge_softness = softness.clamp(0.0, 1.0);
    }

    pub fn set_temporal_smoothing(&mut self, smoothing: f32) {
        self.temporal_smoothing = smoothing.clamp(0.0, 1.0);
    }
}

impl Default for EffectParams {
    fn default() -> Self {
        Self {
            blur_radius: 5.0,
            edge_softness: 0.5,
            temporal_smoothing: 0.7,
        }
    }
}
