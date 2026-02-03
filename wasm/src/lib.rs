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
    background_buffer: Vec<u8>,
    mask_width: usize,
    mask_height: usize,
    upsampled_mask: Vec<f32>,
    gray_cache: Vec<f32>,
    spatial_kernel: Vec<f32>,
    kernel_radius: usize,
    filtered_buffer: Vec<f32>,
    prev_mask: Vec<f32>,
    alpha_temporal: f32,
}

#[wasm_bindgen]
impl MeetProcessor {
    pub fn new(width: usize, height: usize) -> MeetProcessor {
        let mask_width = 256;
        let mask_height = 144;
        let kernel_radius = 1;

        let sigma_space = 1.5;
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
        for val in &mut spatial_kernel {
            *val /= sum;
        }

        MeetProcessor {
            width,
            height,
            input_buffer: vec![0; width * height * 4],
            mask_buffer: vec![0.0; mask_width * mask_height],
            output_buffer: vec![0; width * height * 4],
            background_buffer: vec![0; width * height * 4],
            upsampled_mask: vec![0.0; width * height],
            gray_cache: vec![0.0; width * height],
            filtered_buffer: vec![0.0; width * height],
            prev_mask: vec![0.0; width * height],
            spatial_kernel,
            kernel_radius,
            mask_width,
            mask_height,
            alpha_temporal: 0.7,
        }
    }

    pub fn input_ptr(&self) -> *mut u8 {
        self.input_buffer.as_ptr() as *mut u8
    }

    pub fn mask_ptr(&self) -> *mut f32 {
        self.mask_buffer.as_ptr() as *mut f32
    }

    pub fn output_ptr(&self) -> *const u8 {
        self.output_buffer.as_ptr()
    }

    // 新增：暴露背景缓冲区指针给 JS 填充
    pub fn background_ptr(&self) -> *mut u8 {
        self.background_buffer.as_ptr() as *mut u8
    }

    // 阶段 1: 准备 Mask (计算密集型，每帧只调一次)
    pub fn prepare_mask(&mut self) {
        self.compute_gray_cache();
        self.bilinear_upsample();
        self.temporal_smooth();
        self.joint_bilateral_filter_opt();
    }

    // 阶段 2: 渲染虚化/压暗效果
    pub fn render_blur(&mut self) {
        let bg_factor = 0.25;
        let len = self.width * self.height;

        // 批处理优化：循环展开以提升性能
        let chunks = len / 4;
        let remainder = len % 4;

        // 使用 unsafe 指针遍历以获得最大性能
        // 使用 unsafe 指针遍历以获得最大性能
        unsafe {
            let mut p_in = self.input_buffer.as_ptr();
            let mut p_out = self.output_buffer.as_mut_ptr();
            let mut p_mask = self.upsampled_mask.as_ptr();

            // 4x 循环展开
            for _ in 0..chunks {
                // Pixel 0
                let a0 = (*p_mask).clamp(0.0, 1.0);
                let b0 = a0 + (1.0 - a0) * bg_factor;
                *p_out.add(0) = (*p_in.add(0) as f32 * b0) as u8;
                *p_out.add(1) = (*p_in.add(1) as f32 * b0) as u8;
                *p_out.add(2) = (*p_in.add(2) as f32 * b0) as u8;
                *p_out.add(3) = 255;

                // Pixel 1
                let a1 = (*p_mask.add(1)).clamp(0.0, 1.0);
                let b1 = a1 + (1.0 - a1) * bg_factor;
                *p_out.add(4) = (*p_in.add(4) as f32 * b1) as u8;
                *p_out.add(5) = (*p_in.add(5) as f32 * b1) as u8;
                *p_out.add(6) = (*p_in.add(6) as f32 * b1) as u8;
                *p_out.add(7) = 255;

                // Pixel 2
                let a2 = (*p_mask.add(2)).clamp(0.0, 1.0);
                let b2 = a2 + (1.0 - a2) * bg_factor;
                *p_out.add(8) = (*p_in.add(8) as f32 * b2) as u8;
                *p_out.add(9) = (*p_in.add(9) as f32 * b2) as u8;
                *p_out.add(10) = (*p_in.add(10) as f32 * b2) as u8;
                *p_out.add(11) = 255;

                // Pixel 3
                let a3 = (*p_mask.add(3)).clamp(0.0, 1.0);
                let b3 = a3 + (1.0 - a3) * bg_factor;
                *p_out.add(12) = (*p_in.add(12) as f32 * b3) as u8;
                *p_out.add(13) = (*p_in.add(13) as f32 * b3) as u8;
                *p_out.add(14) = (*p_in.add(14) as f32 * b3) as u8;
                *p_out.add(15) = 255;

                p_in = p_in.add(16);
                p_out = p_out.add(16);
                p_mask = p_mask.add(4);
            }

            // 处理剩余部分
            for _ in 0..remainder {
                let alpha = (*p_mask).clamp(0.0, 1.0);
                let blend = alpha + (1.0 - alpha) * bg_factor;

                *p_out.add(0) = (*p_in.add(0) as f32 * blend) as u8;
                *p_out.add(1) = (*p_in.add(1) as f32 * blend) as u8;
                *p_out.add(2) = (*p_in.add(2) as f32 * blend) as u8;
                *p_out.add(3) = 255;

                p_in = p_in.add(4);
                p_out = p_out.add(4);
                p_mask = p_mask.add(1);
            }
        }
    }

    // 阶段 3: 渲染背景替换效果
    pub fn render_replace(&mut self) {
        let len = self.width * self.height;

        unsafe {
            let mut p_in = self.input_buffer.as_ptr();
            let mut p_bg = self.background_buffer.as_ptr(); // 背景图片
            let mut p_out = self.output_buffer.as_mut_ptr();
            let mut p_mask = self.upsampled_mask.as_ptr();

            for _ in 0..len {
                let alpha = (*p_mask).clamp(0.0, 1.0);
                let inv_alpha = 1.0 - alpha;

                // Output = Input * Alpha + Background * (1 - Alpha)
                // 前景(Input) + 背景(Background)

                *p_out.add(0) =
                    (*p_in.add(0) as f32 * alpha + *p_bg.add(0) as f32 * inv_alpha) as u8;
                *p_out.add(1) =
                    (*p_in.add(1) as f32 * alpha + *p_bg.add(1) as f32 * inv_alpha) as u8;
                *p_out.add(2) =
                    (*p_in.add(2) as f32 * alpha + *p_bg.add(2) as f32 * inv_alpha) as u8;
                *p_out.add(3) = 255;

                p_in = p_in.add(4);
                p_bg = p_bg.add(4);
                p_out = p_out.add(4);
                p_mask = p_mask.add(1);
            }
        }
    }

    // 保留辅助函数
    fn temporal_smooth(&mut self) {
        let len = self.width * self.height;
        for i in 0..len {
            let curr = unsafe { *self.upsampled_mask.get_unchecked(i) };
            let prev = unsafe { *self.prev_mask.get_unchecked(i) };
            let smoothed = self.alpha_temporal * curr + (1.0 - self.alpha_temporal) * prev;
            unsafe {
                *self.upsampled_mask.get_unchecked_mut(i) = smoothed;
                *self.prev_mask.get_unchecked_mut(i) = smoothed;
            }
        }
    }

    fn compute_gray_cache(&mut self) {
        let len = self.width * self.height;
        let chunks = len / 8;

        for chunk_idx in 0..chunks {
            let base_idx = chunk_idx * 8;
            for offset in 0..8 {
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

        for idx in (chunks * 8)..len {
            let pix_idx = idx * 4;
            let r = self.input_buffer[pix_idx] as f32;
            let g = self.input_buffer[pix_idx + 1] as f32;
            let b = self.input_buffer[pix_idx + 2] as f32;
            self.gray_cache[idx] = 0.299 * r + 0.587 * g + 0.114 * b;
        }
    }

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

                let row0 = my0 * mw;
                let row1 = my1 * mw;

                // Unsafe get for speed
                unsafe {
                    let v00 = *self.mask_buffer.get_unchecked(row0 + mx0);
                    let v10 = *self.mask_buffer.get_unchecked(row0 + mx1);
                    let v01 = *self.mask_buffer.get_unchecked(row1 + mx0);
                    let v11 = *self.mask_buffer.get_unchecked(row1 + mx1);

                    let v0 = v00 * fx_inv + v10 * fx;
                    let v1 = v01 * fx_inv + v11 * fx;
                    *self.upsampled_mask.get_unchecked_mut(y * w + x) = v0 * fy_inv + v1 * fy;
                }
            }
        }
    }

    fn joint_bilateral_filter_opt(&mut self) {
        let w = self.width;
        let h = self.height;
        let r = self.kernel_radius as isize;
        let inv_sigma_color = 1.0 / 30.0;

        for y in 0..h {
            if y < r as usize || y >= h - r as usize {
                // 边界略过
                let start = y * w;
                unsafe {
                    std::ptr::copy_nonoverlapping(
                        self.upsampled_mask.as_ptr().add(start),
                        self.filtered_buffer.as_mut_ptr().add(start),
                        w,
                    );
                }
                continue;
            }

            for x in 0..w {
                if x < r as usize || x >= w - r as usize {
                    let idx = y * w + x;
                    unsafe {
                        *self.filtered_buffer.get_unchecked_mut(idx) =
                            *self.upsampled_mask.get_unchecked(idx);
                    }
                    continue;
                }

                let idx = y * w + x;
                let gray_center = unsafe { *self.gray_cache.get_unchecked(idx) };

                let mut sum_weight = 0.0;
                let mut sum_val = 0.0;
                let mut kernel_idx = 0;

                for dy in -r..=r {
                    let ny = (y as isize + dy) as usize;
                    let ny_offset = ny * w;
                    for dx in -r..=r {
                        let nx = (x as isize + dx) as usize;
                        let nidx = ny_offset + nx;

                        unsafe {
                            let space_weight = *self.spatial_kernel.get_unchecked(kernel_idx);
                            kernel_idx += 1;

                            let gray_neighbor = *self.gray_cache.get_unchecked(nidx);
                            let color_dist = (gray_center - gray_neighbor).abs();
                            let color_weight = fast_exp_opt(-color_dist * inv_sigma_color);

                            let weight = space_weight * color_weight;
                            sum_weight += weight;
                            sum_val += *self.upsampled_mask.get_unchecked(nidx) * weight;
                        }
                    }
                }

                unsafe {
                    *self.filtered_buffer.get_unchecked_mut(idx) = sum_val / sum_weight.max(1e-6);
                }
            }
        }

        std::mem::swap(&mut self.upsampled_mask, &mut self.filtered_buffer);
    }
}

#[inline(always)]
fn fast_exp_opt(x: f32) -> f32 {
    if x >= 0.0 {
        x.exp()
    } else if x > -1.0 {
        1.0 + x * (1.0 + x * (0.5 + x * 0.16667))
    } else {
        let scaled = 1.0 + x * 0.015625;
        if scaled > 0.0 {
            let t = scaled * scaled;
            let t2 = t * t;
            let t4 = t2 * t2;
            t4 * t4
        } else {
            0.0
        }
    }
}
