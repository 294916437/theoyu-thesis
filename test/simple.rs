use wasm_bindgen::prelude::*;

#[wasm_bindgen]
pub fn init_panic_hook() {
    console_error_panic_hook::set_once();
}

#[wasm_bindgen]
pub struct MeetProcessor {
    width: usize,
    height: usize,
    input_buffer: Vec<u8>,
    mask_buffer: Vec<f32>,
    output_buffer: Vec<u8>,
    mask_width: usize,
    mask_height: usize,
    upsampled_mask: Vec<f32>,
    gray_cache: Vec<f32>,
    spatial_kernel: Vec<f32>,
    kernel_radius: usize,
}

#[wasm_bindgen]
impl MeetProcessor {
    pub fn new(width: usize, height: usize) -> MeetProcessor {
        let mask_width = 256;
        let mask_height = 144;
        let kernel_radius = 3;

        // 预计算空间高斯核
        let sigma_space = 2.5;
        let side_len = kernel_radius * 2 + 1;
        let kernel_size = side_len * side_len;
        let mut spatial_kernel = vec![0.0; kernel_size];

        let mut idx = 0;
        for dy in -(kernel_radius as isize)..=(kernel_radius as isize) {
            for dx in -(kernel_radius as isize)..=(kernel_radius as isize) {
                let dist_sq = (dx * dx + dy * dy) as f32;
                spatial_kernel[idx] = (-dist_sq / (2.0 * sigma_space * sigma_space)).exp();
                idx += 1;
            }
        }

        MeetProcessor {
            width,
            height,
            input_buffer: vec![0; width * height * 4],
            mask_buffer: vec![0.0; mask_width * mask_height],
            output_buffer: vec![0; width * height * 4],
            upsampled_mask: vec![0.0; width * height],
            gray_cache: vec![0.0; width * height],
            spatial_kernel,
            kernel_radius,
            mask_width,
            mask_height,
        }
    }

    pub fn input_ptr(&self) -> *const u8 {
        self.input_buffer.as_ptr()
    }

    pub fn mask_ptr(&self) -> *const f32 {
        self.mask_buffer.as_ptr()
    }

    pub fn output_ptr(&self) -> *const u8 {
        self.output_buffer.as_ptr()
    }

    pub fn apply_filter_and_blend(&mut self) {
        // 流程优化：分离关注点，提高缓存命中率
        self.compute_gray_cache();
        self.bilinear_upsample();
        self.joint_bilateral_filter();
        self.blend_with_background();
    }

    // 预计算灰度图，避免重复计算
    fn compute_gray_cache(&mut self) {
        let w = self.width;
        let h = self.height;

        for idx in 0..(w * h) {
            let pix_idx = idx * 4;
            let r = self.input_buffer[pix_idx] as f32;
            let g = self.input_buffer[pix_idx + 1] as f32;
            let b = self.input_buffer[pix_idx + 2] as f32;
            self.gray_cache[idx] = 0.299 * r + 0.587 * g + 0.114 * b;
        }
    }

    // 高质量双线性插值
    fn bilinear_upsample(&mut self) {
        let w = self.width;
        let h = self.height;
        let mw = self.mask_width;
        let mh = self.mask_height;

        let scale_x = (mw - 1) as f32 / (w - 1) as f32;
        let scale_y = (mh - 1) as f32 / (h - 1) as f32;

        for y in 0..h {
            let my_f = y as f32 * scale_y;
            let my0 = my_f.floor() as usize;
            let my1 = (my0 + 1).min(mh - 1);
            let fy = my_f - my0 as f32;

            for x in 0..w {
                let mx_f = x as f32 * scale_x;
                let mx0 = mx_f.floor() as usize;
                let mx1 = (mx0 + 1).min(mw - 1);
                let fx = mx_f - mx0 as f32;

                // 双线性插值
                let v00 = self.mask_buffer[my0 * mw + mx0];
                let v10 = self.mask_buffer[my0 * mw + mx1];
                let v01 = self.mask_buffer[my1 * mw + mx0];
                let v11 = self.mask_buffer[my1 * mw + mx1];

                let v0 = v00 * (1.0 - fx) + v10 * fx;
                let v1 = v01 * (1.0 - fx) + v11 * fx;

                self.upsampled_mask[y * w + x] = v0 * (1.0 - fy) + v1 * fy;
            }
        }
    }

    // 边缘感知的联合双边滤波
    fn joint_bilateral_filter(&mut self) {
        let w = self.width;
        let h = self.height;
        let r = self.kernel_radius as isize;

        let inv_sigma_color = 1.0 / 18.0; // sigma_color = 18
        let mut filtered = vec![0.0; w * h];

        for y in 0..h {
            // 边界优化：跳过边缘像素
            if y < r as usize || y >= h - r as usize {
                for x in 0..w {
                    filtered[y * w + x] = self.upsampled_mask[y * w + x];
                }
                continue;
            }

            for x in 0..w {
                if x < r as usize || x >= w - r as usize {
                    filtered[y * w + x] = self.upsampled_mask[y * w + x];
                    continue;
                }

                let idx = y * w + x;
                let gray_center = self.gray_cache[idx];

                let mut sum_weight = 0.0;
                let mut sum_val = 0.0;
                let mut kernel_idx = 0;

                // 邻域滤波
                for dy in -r..=r {
                    let ny = (y as isize + dy) as usize;
                    for dx in -r..=r {
                        let nx = (x as isize + dx) as usize;
                        let nidx = ny * w + nx;

                        // 空间权重（预计算）
                        let space_weight = self.spatial_kernel[kernel_idx];
                        kernel_idx += 1;

                        // 颜色权重（快速近似）
                        let color_dist = (gray_center - self.gray_cache[nidx]).abs();
                        let color_weight = fast_exp(-color_dist * inv_sigma_color);

                        let weight = space_weight * color_weight;
                        sum_weight += weight;
                        sum_val += self.upsampled_mask[nidx] * weight;
                    }
                }

                filtered[idx] = sum_val / sum_weight.max(1e-6);
            }
        }

        self.upsampled_mask = filtered;
    }

    // 高效 Alpha Blending
    fn blend_with_background(&mut self) {
        let w = self.width;
        let h = self.height;
        let bg_factor = 0.35; // 背景变暗 65%

        for idx in 0..(w * h) {
            let pix_idx = idx * 4;

            // 直接使用 Alpha（移除 Sigmoid 以提升性能）
            let alpha = self.upsampled_mask[idx].clamp(0.0, 1.0);
            let one_minus_alpha = 1.0 - alpha;

            // 前景像素
            let r_fg = self.input_buffer[pix_idx] as f32;
            let g_fg = self.input_buffer[pix_idx + 1] as f32;
            let b_fg = self.input_buffer[pix_idx + 2] as f32;

            // Alpha 混合公式优化
            let blend_factor = alpha + one_minus_alpha * bg_factor;

            self.output_buffer[pix_idx] = (r_fg * blend_factor) as u8;
            self.output_buffer[pix_idx + 1] = (g_fg * blend_factor) as u8;
            self.output_buffer[pix_idx + 2] = (b_fg * blend_factor) as u8;
            self.output_buffer[pix_idx + 3] = 255;
        }
    }
}

// 快速 exp 近似
#[inline]
fn fast_exp(x: f32) -> f32 {
    if x >= 0.0 {
        // 正数直接用标准库（罕见情况）
        x.exp()
    } else if x > -0.7 {
        // 小负数：泰勒级数前 3 项
        1.0 + x + 0.5 * x * x
    } else {
        // 大负数：快速分段近似 (e^x ≈ (1 + x/64)^64)
        let scaled = 1.0 + x / 64.0;
        if scaled > 0.0 {
            // 使用位运算加速幂运算
            scaled.powi(64)
        } else {
            0.0
        }
    }
}
