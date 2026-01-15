use crate::core::{Dimensions, EffectParams, MemoryView};
use crate::effects::{BlurEffect, EffectType, ReplaceEffect};
use crate::segmentation::{MaskProcessor, Segmentor, TemporalFilter};
use wasm_bindgen::prelude::*;

#[wasm_bindgen]
pub struct BackgroundEffectEngine {
    segmentor: Segmentor,
    blur_effect: BlurEffect,
    replace_effect: ReplaceEffect,
    temporal_filter: TemporalFilter,

    dimensions: Dimensions,
    current_effect: EffectType,
    params: EffectParams,

    mask_buffer: Vec<f32>,
    upsampled_mask: Vec<f32>,
    output_buffer: Vec<u8>,
}

#[wasm_bindgen]
impl BackgroundEffectEngine {
    #[wasm_bindgen(constructor)]
    pub fn new(
        model_bytes: &[u8],
        width: usize,
        height: usize,
    ) -> Result<BackgroundEffectEngine, JsValue> {
        console_error_panic_hook::set_once();

        let dims = Dimensions::new(width, height);

        let segmentor = Segmentor::new(model_bytes).map_err(|e| JsValue::from_str(&e))?;

        Ok(Self {
            segmentor,
            blur_effect: BlurEffect::new(dims),
            replace_effect: ReplaceEffect::new(),
            temporal_filter: TemporalFilter::new(256 * 144 * 2, 0.7),

            dimensions: dims,
            current_effect: EffectType::Blur,
            params: EffectParams::default(),

            mask_buffer: vec![0.0; 256 * 144 * 2],
            upsampled_mask: vec![0.0; dims.pixel_count()],
            output_buffer: vec![0; dims.rgba_size()],
        })
    }

    #[wasm_bindgen(js_name = setEffect)]
    pub fn set_effect(&mut self, effect: EffectType) {
        self.current_effect = effect;
    }

    #[wasm_bindgen(js_name = getParams)]
    pub fn get_params(&self) -> EffectParams {
        self.params
    }

    #[wasm_bindgen(js_name = setParams)]
    pub fn set_params(&mut self, params: EffectParams) {
        self.params = params;
        self.temporal_filter = TemporalFilter::new(256 * 144 * 2, params.temporal_smoothing);
    }

    #[wasm_bindgen(js_name = loadBackground)]
    pub fn load_background(&mut self, image_data: Vec<u8>) -> Result<(), JsValue> {
        if image_data.len() != self.dimensions.rgba_size() {
            return Err(JsValue::from_str("背景图片尺寸不匹配"));
        }
        self.replace_effect.load_background(image_data);
        Ok(())
    }

    #[wasm_bindgen(js_name = processFrame)]
    pub fn process_frame(&mut self, input_rgba: &[u8]) -> Result<(), JsValue> {
        if input_rgba.len() != self.dimensions.rgba_size() {
            return Err(JsValue::from_str("输入数据大小不匹配"));
        }

        // 1. 分割
        self.mask_buffer = self
            .segmentor
            .segment(input_rgba, self.dimensions)
            .map_err(|e| JsValue::from_str(&e))?;

        // 2. 时间平滑
        self.temporal_filter.apply(&mut self.mask_buffer);

        // 3. 上采样 mask
        MaskProcessor::upsample_mask(
            &self.mask_buffer,
            Dimensions::new(256, 144),
            self.dimensions,
            &mut self.upsampled_mask,
        );

        // 4. 应用效果
        match self.current_effect {
            EffectType::None => {
                self.output_buffer.copy_from_slice(input_rgba);
            }
            EffectType::Blur => {
                self.blur_effect.apply(
                    input_rgba,
                    &self.upsampled_mask,
                    &mut self.output_buffer,
                    self.dimensions,
                    self.params.blur_radius as usize,
                );
            }
            EffectType::Replace => {
                self.replace_effect
                    .apply(
                        input_rgba,
                        &self.upsampled_mask,
                        &mut self.output_buffer,
                        self.dimensions,
                    )
                    .map_err(|e| JsValue::from_str(&e))?;
            }
            _ => {
                return Err(JsValue::from_str("效果未实现"));
            }
        }

        Ok(())
    }

    #[wasm_bindgen(js_name = getOutputView)]
    pub fn get_output_view(&self) -> MemoryView {
        MemoryView::new(self.output_buffer.as_ptr(), self.output_buffer.len())
    }
}
