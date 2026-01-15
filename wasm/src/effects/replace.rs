use crate::core::Dimensions;

pub struct ReplaceEffect {
    background: Option<Vec<u8>>,
}

impl ReplaceEffect {
    pub fn new() -> Self {
        Self { background: None }
    }

    pub fn load_background(&mut self, image_data: Vec<u8>) {
        self.background = Some(image_data);
    }

    pub fn apply(
        &self,
        input: &[u8],
        mask: &[f32],
        output: &mut [u8],
        dims: Dimensions,
    ) -> Result<(), String> {
        let bg = self
            .background
            .as_ref()
            .ok_or_else(|| "未加载背景图片".to_string())?;

        for y in 0..dims.height {
            for x in 0..dims.width {
                let alpha = mask[y * dims.width + x];

                let idx = (y * dims.width + x) * 4;

                let fg_r = input[idx] as f32;
                let fg_g = input[idx + 1] as f32;
                let fg_b = input[idx + 2] as f32;

                let bg_r = bg[idx] as f32;
                let bg_g = bg[idx + 1] as f32;
                let bg_b = bg[idx + 2] as f32;

                output[idx] = (fg_r * alpha + bg_r * (1.0 - alpha)) as u8;
                output[idx + 1] = (fg_g * alpha + bg_g * (1.0 - alpha)) as u8;
                output[idx + 2] = (fg_b * alpha + bg_b * (1.0 - alpha)) as u8;
                output[idx + 3] = 255;
            }
        }

        Ok(())
    }
}
