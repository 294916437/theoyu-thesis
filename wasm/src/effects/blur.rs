use crate::core::Dimensions;

pub struct BlurEffect {
    temp_buffer: Vec<u8>,
}

impl BlurEffect {
    pub fn new(dims: Dimensions) -> Self {
        Self {
            temp_buffer: vec![0; dims.rgba_size()],
        }
    }

    pub fn apply(
        &mut self,
        input: &[u8],
        mask: &[f32],
        output: &mut [u8],
        dims: Dimensions,
        radius: usize,
    ) {
        // 1. 水平模糊
        self.blur_horizontal(input, dims, radius);

        // 2. 垂直模糊 + Alpha 混合
        self.blur_vertical_and_composite(
            &self.temp_buffer.clone(),
            input,
            mask,
            output,
            dims,
            radius,
        );
    }

    fn blur_horizontal(&mut self, input: &[u8], dims: Dimensions, radius: usize) {
        for y in 0..dims.height {
            for x in 0..dims.width {
                let mut sum_r = 0u32;
                let mut sum_g = 0u32;
                let mut sum_b = 0u32;
                let mut count = 0u32;

                let x_start = x.saturating_sub(radius);
                let x_end = (x + radius + 1).min(dims.width);

                for nx in x_start..x_end {
                    let idx = (y * dims.width + nx) * 4;
                    sum_r += input[idx] as u32;
                    sum_g += input[idx + 1] as u32;
                    sum_b += input[idx + 2] as u32;
                    count += 1;
                }

                let idx = (y * dims.width + x) * 4;
                self.temp_buffer[idx] = (sum_r / count) as u8;
                self.temp_buffer[idx + 1] = (sum_g / count) as u8;
                self.temp_buffer[idx + 2] = (sum_b / count) as u8;
                self.temp_buffer[idx + 3] = 255;
            }
        }
    }

    fn blur_vertical_and_composite(
        &self,
        blurred_h: &[u8],
        original: &[u8],
        mask: &[f32],
        output: &mut [u8],
        dims: Dimensions,
        radius: usize,
    ) {
        for y in 0..dims.height {
            for x in 0..dims.width {
                let mut sum_r = 0u32;
                let mut sum_g = 0u32;
                let mut sum_b = 0u32;
                let mut count = 0u32;

                let y_start = y.saturating_sub(radius);
                let y_end = (y + radius + 1).min(dims.height);

                for ny in y_start..y_end {
                    let idx = (ny * dims.width + x) * 4;
                    sum_r += blurred_h[idx] as u32;
                    sum_g += blurred_h[idx + 1] as u32;
                    sum_b += blurred_h[idx + 2] as u32;
                    count += 1;
                }

                let blur_r = (sum_r / count) as f32;
                let blur_g = (sum_g / count) as f32;
                let blur_b = (sum_b / count) as f32;

                let alpha = mask[y * dims.width + x];

                let idx = (y * dims.width + x) * 4;
                let orig_r = original[idx] as f32;
                let orig_g = original[idx + 1] as f32;
                let orig_b = original[idx + 2] as f32;

                output[idx] = (orig_r * alpha + blur_r * (1.0 - alpha)) as u8;
                output[idx + 1] = (orig_g * alpha + blur_g * (1.0 - alpha)) as u8;
                output[idx + 2] = (orig_b * alpha + blur_b * (1.0 - alpha)) as u8;
                output[idx + 3] = 255;
            }
        }
    }
}
