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
    // 【新增】缓存上采样后的 Mask
    upsampled_mask: Vec<f32>,
}

#[wasm_bindgen]
impl MeetProcessor {
    pub fn new(width: usize, height: usize) -> MeetProcessor {
        let mask_width = 256;
        let mask_height = 144;

        MeetProcessor {
            width,
            height,
            input_buffer: vec![0; width * height * 4],
            mask_buffer: vec![0.0; mask_width * mask_height],
            output_buffer: vec![0; width * height * 4],
            upsampled_mask: vec![0.0; width * height], // 【新增】
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

    // 【核心优化】Google Meet 风格的联合双边上采样
    pub fn apply_filter_and_blend(&mut self) {
        // Step 1: 双线性插值上采样 Mask
        self.bilinear_upsample();

        // Step 2: 边缘感知的联合双边滤波
        self.joint_bilateral_filter();

        // Step 3: Alpha Blending 合成
        self.blend_with_background();
    }

    // 【新增】高质量双线性插值
    fn bilinear_upsample(&mut self) {
        let w = self.width;
        let h = self.height;
        let mw = self.mask_width;
        let mh = self.mask_height;

        let scale_x = (mw - 1) as f32 / (w - 1) as f32;
        let scale_y = (mh - 1) as f32 / (h - 1) as f32;

        for y in 0..h {
            for x in 0..w {
                let mx_f = x as f32 * scale_x;
                let my_f = y as f32 * scale_y;

                let mx0 = mx_f.floor() as usize;
                let my0 = my_f.floor() as usize;
                let mx1 = (mx0 + 1).min(mw - 1);
                let my1 = (my0 + 1).min(mh - 1);

                let fx = mx_f - mx0 as f32;
                let fy = my_f - my0 as f32;

                // 双线性插值
                let v00 = self.mask_buffer[my0 * mw + mx0];
                let v10 = self.mask_buffer[my0 * mw + mx1];
                let v01 = self.mask_buffer[my1 * mw + mx0];
                let v11 = self.mask_buffer[my1 * mw + mx1];

                let v0 = v00 * (1.0 - fx) + v10 * fx;
                let v1 = v01 * (1.0 - fx) + v11 * fx;
                let val = v0 * (1.0 - fy) + v1 * fy;

                self.upsampled_mask[y * w + x] = val;
            }
        }
    }

    // 【优化】真正的联合双边滤波 (Edge-Aware)
    fn joint_bilateral_filter(&mut self) {
        let w = self.width;
        let h = self.height;

        // JBF 参数
        let sigma_space = 5.0; // 空间域标准差
        let sigma_color = 25.0; // 颜色域标准差 (0-255)
        let kernel_radius = 5; // 滤波窗口半径

        let mut filtered = vec![0.0; w * h];

        for y in 0..h {
            for x in 0..w {
                let idx = y * w + x;
                let center_idx = idx * 4;

                // 中心像素的颜色 (转为灰度)
                let r_c = self.input_buffer[center_idx] as f32;
                let g_c = self.input_buffer[center_idx + 1] as f32;
                let b_c = self.input_buffer[center_idx + 2] as f32;
                let gray_center = 0.299 * r_c + 0.587 * g_c + 0.114 * b_c;

                let mut sum_weight = 0.0;
                let mut sum_val = 0.0;

                // 遍历邻域
                for dy in -(kernel_radius as isize)..=(kernel_radius as isize) {
                    for dx in -(kernel_radius as isize)..=(kernel_radius as isize) {
                        let nx = (x as isize + dx).max(0).min(w as isize - 1) as usize;
                        let ny = (y as isize + dy).max(0).min(h as isize - 1) as usize;
                        let nidx = ny * w + nx;
                        let npix_idx = nidx * 4;

                        // 空间权重 (高斯核)
                        let spatial_dist = (dx * dx + dy * dy) as f32;
                        let space_weight =
                            (-spatial_dist / (2.0 * sigma_space * sigma_space)).exp();

                        // 颜色权重 (基于原图边缘)
                        let r_n = self.input_buffer[npix_idx] as f32;
                        let g_n = self.input_buffer[npix_idx + 1] as f32;
                        let b_n = self.input_buffer[npix_idx + 2] as f32;
                        let gray_neighbor = 0.299 * r_n + 0.587 * g_n + 0.114 * b_n;

                        let color_dist = (gray_center - gray_neighbor).abs();
                        let color_weight = (-color_dist / sigma_color).exp();

                        // 联合权重
                        let weight = space_weight * color_weight;
                        sum_weight += weight;
                        sum_val += self.upsampled_mask[nidx] * weight;
                    }
                }

                filtered[idx] = if sum_weight > 0.0 {
                    sum_val / sum_weight
                } else {
                    self.upsampled_mask[idx]
                };
            }
        }

        self.upsampled_mask = filtered;
    }

    // 【优化】更平滑的 Alpha Blending
    fn blend_with_background(&mut self) {
        let w = self.width;
        let h = self.height;

        for y in 0..h {
            for x in 0..w {
                let idx = (y * w + x) * 4;
                let mask_idx = y * w + x;

                // 获取滤波后的 Alpha
                let alpha_raw = self.upsampled_mask[mask_idx];

                // Sigmoid 平滑 (减少硬边缘)
                let alpha = 1.0 / (1.0 + (-(alpha_raw - 0.5) * 12.0).exp());

                // 背景变暗 30%
                let r_fg = self.input_buffer[idx];
                let g_fg = self.input_buffer[idx + 1];
                let b_fg = self.input_buffer[idx + 2];

                let r_bg = (r_fg as f32 * 0.3) as u8;
                let g_bg = (g_fg as f32 * 0.3) as u8;
                let b_bg = (b_fg as f32 * 0.3) as u8;

                // Alpha 混合
                self.output_buffer[idx] = (r_fg as f32 * alpha + r_bg as f32 * (1.0 - alpha)) as u8;
                self.output_buffer[idx + 1] =
                    (g_fg as f32 * alpha + g_bg as f32 * (1.0 - alpha)) as u8;
                self.output_buffer[idx + 2] =
                    (b_fg as f32 * alpha + b_bg as f32 * (1.0 - alpha)) as u8;
                self.output_buffer[idx + 3] = 255;
            }
        }
    }
}
