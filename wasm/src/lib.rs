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
    // 优化：重用滤波缓冲区
    filtered_buffer: Vec<f32>,
}

#[wasm_bindgen]
impl MeetProcessor {
    pub fn new(width: usize, height: usize) -> MeetProcessor {
        let mask_width = 256;
        let mask_height = 144;
        let kernel_radius = 2; // 降低半径 3->2 提升性能

        // 预计算空间高斯核
        let sigma_space = 2.0; // 调整 sigma
        let side_len = kernel_radius * 2 + 1;
        let kernel_size = side_len * side_len;
        let mut spatial_kernel = vec![0.0; kernel_size];

        let mut sum = 0.0;
        let mut idx = 0;
        for dy in -(kernel_radius as isize)..=(kernel_radius as isize) {
            for dx in -(kernel_radius as isize)..=(kernel_radius as isize) {
                let dist_sq = (dx * dx + dy * dy) as f32;
                let val = (-dist_sq / (2.0 * sigma_space * sigma_space)).exp();
                spatial_kernel[idx] = val;
                sum += val;
                idx += 1;
            }
        }
        // 归一化空间核
        for val in &mut spatial_kernel {
            *val /= sum;
        }

        MeetProcessor {
            width,
            height,
            input_buffer: vec![0; width * height * 4],
            mask_buffer: vec![0.0; mask_width * mask_height],
            output_buffer: vec![0; width * height * 4],
            upsampled_mask: vec![0.0; width * height],
            gray_cache: vec![0.0; width * height],
            filtered_buffer: vec![0.0; width * height],
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
        self.compute_gray_cache();
        self.bilinear_upsample();
        self.joint_bilateral_filter_opt(); // 优化版本
        self.blend_with_background();
    }

    // 预计算灰度图（SIMD 友好）
    fn compute_gray_cache(&mut self) {
        let w = self.width;
        let h = self.height;
        let len = w * h;

        // 批量处理 4 像素（避免边界检查）
        let chunks = len / 4;
        for chunk_idx in 0..chunks {
            let base_idx = chunk_idx * 4;
            for offset in 0..4 {
                let idx = base_idx + offset;
                let pix_idx = idx * 4;
                unsafe {
                    let r = *self.input_buffer.get_unchecked(pix_idx) as f32;
                    let g = *self.input_buffer.get_unchecked(pix_idx + 1) as f32;
                    let b = *self.input_buffer.get_unchecked(pix_idx + 2) as f32;
                    *self.gray_cache.get_unchecked_mut(idx) = 0.299 * r + 0.587 * g + 0.114 * b;
                }
            }
        }

        // 处理剩余像素
        for idx in (chunks * 4)..len {
            let pix_idx = idx * 4;
            let r = self.input_buffer[pix_idx] as f32;
            let g = self.input_buffer[pix_idx + 1] as f32;
            let b = self.input_buffer[pix_idx + 2] as f32;
            self.gray_cache[idx] = 0.299 * r + 0.587 * g + 0.114 * b;
        }
    }

    // 高质量双线性插值（无变化）
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
            let fy_inv = 1.0 - fy;

            for x in 0..w {
                let mx_f = x as f32 * scale_x;
                let mx0 = mx_f.floor() as usize;
                let mx1 = (mx0 + 1).min(mw - 1);
                let fx = mx_f - mx0 as f32;
                let fx_inv = 1.0 - fx;

                let v00 = self.mask_buffer[my0 * mw + mx0];
                let v10 = self.mask_buffer[my0 * mw + mx1];
                let v01 = self.mask_buffer[my1 * mw + mx0];
                let v11 = self.mask_buffer[my1 * mw + mx1];

                let v0 = v00 * fx_inv + v10 * fx;
                let v1 = v01 * fx_inv + v11 * fx;

                self.upsampled_mask[y * w + x] = v0 * fy_inv + v1 * fy;
            }
        }
    }

    // 优化版联合双边滤波
    fn joint_bilateral_filter_opt(&mut self) {
        let w = self.width;
        let h = self.height;
        let r = self.kernel_radius as isize;

        let inv_sigma_color = 1.0 / 25.0; // 增大容忍度

        for y in 0..h {
            if y < r as usize || y >= h - r as usize {
                for x in 0..w {
                    self.filtered_buffer[y * w + x] = self.upsampled_mask[y * w + x];
                }
                continue;
            }

            for x in 0..w {
                if x < r as usize || x >= w - r as usize {
                    self.filtered_buffer[y * w + x] = self.upsampled_mask[y * w + x];
                    continue;
                }

                let idx = y * w + x;
                let gray_center = unsafe { *self.gray_cache.get_unchecked(idx) };

                let mut sum_weight = 0.0;
                let mut sum_val = 0.0;
                let mut kernel_idx = 0;

                for dy in -r..=r {
                    let ny = (y as isize + dy) as usize;
                    for dx in -r..=r {
                        let nx = (x as isize + dx) as usize;
                        let nidx = ny * w + nx;

                        let space_weight =
                            unsafe { *self.spatial_kernel.get_unchecked(kernel_idx) };
                        kernel_idx += 1;

                        let gray_neighbor = unsafe { *self.gray_cache.get_unchecked(nidx) };
                        let color_dist = (gray_center - gray_neighbor).abs();
                        let color_weight = fast_exp_opt(-color_dist * inv_sigma_color);

                        let weight = space_weight * color_weight;
                        sum_weight += weight;
                        sum_val += unsafe { *self.upsampled_mask.get_unchecked(nidx) } * weight;
                    }
                }

                self.filtered_buffer[idx] = sum_val / sum_weight.max(1e-6);
            }
        }

        std::mem::swap(&mut self.upsampled_mask, &mut self.filtered_buffer);
    }

    // 优化 Alpha Blending
    fn blend_with_background(&mut self) {
        let w = self.width;
        let h = self.height;
        let bg_factor = 0.35;

        let len = w * h;
        for idx in 0..len {
            let pix_idx = idx * 4;

            let alpha = unsafe { (*self.upsampled_mask.get_unchecked(idx)).clamp(0.0, 1.0) };
            let blend_factor = alpha + (1.0 - alpha) * bg_factor;

            unsafe {
                let r = *self.input_buffer.get_unchecked(pix_idx) as f32;
                let g = *self.input_buffer.get_unchecked(pix_idx + 1) as f32;
                let b = *self.input_buffer.get_unchecked(pix_idx + 2) as f32;

                *self.output_buffer.get_unchecked_mut(pix_idx) = (r * blend_factor) as u8;
                *self.output_buffer.get_unchecked_mut(pix_idx + 1) = (g * blend_factor) as u8;
                *self.output_buffer.get_unchecked_mut(pix_idx + 2) = (b * blend_factor) as u8;
                *self.output_buffer.get_unchecked_mut(pix_idx + 3) = 255;
            }
        }
    }
}

// 优化版快速 exp
#[inline(always)]
fn fast_exp_opt(x: f32) -> f32 {
    if x >= 0.0 {
        x.exp()
    } else if x > -1.0 {
        // 泰勒展开 3 阶
        1.0 + x * (1.0 + x * (0.5 + x * 0.16667))
    } else {
        // 快速近似
        let scaled = 1.0 + x * 0.015625; // x/64
        if scaled > 0.0 {
            let t = scaled * scaled;
            let t2 = t * t;
            let t4 = t2 * t2;
            t4 * t4 // scaled^64
        } else {
            0.0
        }
    }
}
