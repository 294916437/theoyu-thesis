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
    blur_buffer: Vec<u8>,
    blur_temp: Vec<u8>,
    blur_radius: usize,
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
            prev_mask: vec![0.0; mask_width * mask_height],
            spatial_kernel,
            kernel_radius,
            mask_width,
            mask_height,
            alpha_temporal: 0.7,
            blur_buffer: vec![0u8; width * height * 4],
            blur_temp: vec![0u8; width * height * 4],
            blur_radius: 20,
        }
    }

    /// 设置模糊半径（每次 box blur 的半径，默认 20，范围 1..=50）
    pub fn set_blur_radius(&mut self, radius: usize) {
        self.blur_radius = radius.clamp(1, 50);
    }

    /// 动态调整处理分辨率（仅重建宽高相关缓冲区，mask 尺寸保持 256×144）。
    pub fn resize(&mut self, width: usize, height: usize) {
        if self.width == width && self.height == height {
            return;
        }
        self.width = width;
        self.height = height;
        let len = width * height;
        self.input_buffer      = vec![0u8;   len * 4];
        self.output_buffer     = vec![0u8;   len * 4];
        self.background_buffer = vec![0u8;   len * 4];
        self.upsampled_mask    = vec![0.0f32; len];
        self.gray_cache        = vec![0.0f32; len];
        self.filtered_buffer   = vec![0.0f32; len];
        self.blur_buffer       = vec![0u8;   len * 4];
        self.blur_temp         = vec![0u8;   len * 4];
        // prev_mask / mask_buffer 保持 mask_width×mask_height，无需重建
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

    // 暴露背景缓冲区指针给 JS 填充
    pub fn background_ptr(&self) -> *mut u8 {
        self.background_buffer.as_ptr() as *mut u8
    }

    // 阶段 1: 准备 Mask (计算密集型，每帧只调一次)
    // 优化后执行顺序：低分辨率时域平滑 → 上采样 → 灰度缓存 → 联合双边滤波
    pub fn prepare_mask(&mut self) {
        self.temporal_smooth_low_res();   // 在 256×144 上做 EMA，再上采样
        self.bilinear_upsample();
        self.compute_gray_cache();
        self.joint_bilateral_filter_opt();
    }

    // 阶段 2: 渲染背景高斯模糊效果
    pub fn render_blur(&mut self) {
        // 对 input_buffer 应用可分离盒型模糊，结果写入 blur_buffer
        self.apply_separable_box_blur();

        let len = self.width * self.height;
        unsafe {
            let p_in = self.input_buffer.as_ptr();
            let p_blurred = self.blur_buffer.as_ptr();
            let p_out = self.output_buffer.as_mut_ptr();
            let p_mask = self.upsampled_mask.as_ptr();

            for i in 0..len {
                // alpha=1 表示前景（保留原图），alpha=0 表示背景（显示模糊）
                let alpha = (*p_mask.add(i)).clamp(0.0, 1.0);
                let inv_alpha = 1.0 - alpha;
                let pi = i * 4;

                *p_out.add(pi)     = (*p_in.add(pi)     as f32 * alpha + *p_blurred.add(pi)     as f32 * inv_alpha) as u8;
                *p_out.add(pi + 1) = (*p_in.add(pi + 1) as f32 * alpha + *p_blurred.add(pi + 1) as f32 * inv_alpha) as u8;
                *p_out.add(pi + 2) = (*p_in.add(pi + 2) as f32 * alpha + *p_blurred.add(pi + 2) as f32 * inv_alpha) as u8;
                *p_out.add(pi + 3) = 255;
            }
        }
    }

    /// 1 次可分离盒型模糊（水平 + 垂直各一次为一轮），1 轮叠加逼近高斯模糊
    fn apply_separable_box_blur(&mut self) {
        let w = self.width;
        let h = self.height;
        let r = self.blur_radius;

        // 将原始帧复制到 blur_buffer 作为第一轮输入
        self.blur_buffer.copy_from_slice(&self.input_buffer);

        for _ in 0..1 {
            // 水平方向模糊: blur_buffer -> blur_temp
            box_blur_h_pass(&self.blur_buffer, &mut self.blur_temp, w, h, r);
            // 垂直方向模糊: blur_temp -> blur_buffer
            box_blur_v_pass(&self.blur_temp, &mut self.blur_buffer, w, h, r);
        }
    }

    // 阶段 3: 渲染背景替换效果
    pub fn render_replace(&mut self) {
        let len = self.width * self.height;

        unsafe {
            let mut p_in = self.input_buffer.as_ptr();
            let mut p_bg = self.background_buffer.as_ptr();
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

    // 低分辨率时域平滑：在 mask_width×mask_height（256×144）上对 mask_buffer 做 EMA
    fn temporal_smooth_low_res(&mut self) {
        let len = self.mask_width * self.mask_height;
        for i in 0..len {
            let curr = unsafe { *self.mask_buffer.get_unchecked(i) };
            let prev = unsafe { *self.prev_mask.get_unchecked(i) };
            let smoothed = self.alpha_temporal * curr + (1.0 - self.alpha_temporal) * prev;
            unsafe {
                *self.mask_buffer.get_unchecked_mut(i) = smoothed;
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

/// 水平方向盒型模糊（逐行前缀和法，O(n)，边界自动 clamp）
fn box_blur_h_pass(src: &[u8], dst: &mut [u8], width: usize, height: usize, radius: usize) {
    let w1 = width + 1;
    let mut pr = vec![0u32; w1];
    let mut pg = vec![0u32; w1];
    let mut pb = vec![0u32; w1];

    for y in 0..height {
        let row_base = y * width * 4;

        // 构建当前行的前缀和
        pr[0] = 0;
        pg[0] = 0;
        pb[0] = 0;
        for x in 0..width {
            let s = row_base + x * 4;
            pr[x + 1] = pr[x] + src[s] as u32;
            pg[x + 1] = pg[x] + src[s + 1] as u32;
            pb[x + 1] = pb[x] + src[s + 2] as u32;
        }

        // 利用前缀和采样 [x-r, x+r] 邻域均值
        for x in 0..width {
            let x0 = x.saturating_sub(radius);
            let x1 = (x + radius + 1).min(width);
            let inv = 1.0 / (x1 - x0) as f32;
            let o = row_base + x * 4;
            dst[o]     = ((pr[x1] - pr[x0]) as f32 * inv) as u8;
            dst[o + 1] = ((pg[x1] - pg[x0]) as f32 * inv) as u8;
            dst[o + 2] = ((pb[x1] - pb[x0]) as f32 * inv) as u8;
            dst[o + 3] = 255;
        }
    }
}

/// 垂直方向盒型模糊（滑动窗口法，行主序访问，O(n)，边界自动 clamp）
fn box_blur_v_pass(src: &[u8], dst: &mut [u8], width: usize, height: usize, radius: usize) {
    // 每列的当前垂直窗口累加和及有效像素计数
    let mut sr = vec![0u32; width];
    let mut sg = vec![0u32; width];
    let mut sb = vec![0u32; width];
    let mut cnt = vec![0u32; width];

    // 初始化：累加前 min(radius+1, height) 行
    let init_end = (radius + 1).min(height);
    for y in 0..init_end {
        let row = y * width * 4;
        for x in 0..width {
            sr[x]  += src[row + x * 4] as u32;
            sg[x]  += src[row + x * 4 + 1] as u32;
            sb[x]  += src[row + x * 4 + 2] as u32;
            cnt[x] += 1;
        }
    }

    for y in 0..height {
        let out_row = y * width * 4;
        // 输出当前行
        for x in 0..width {
            let inv = 1.0 / cnt[x] as f32;
            dst[out_row + x * 4]     = (sr[x] as f32 * inv) as u8;
            dst[out_row + x * 4 + 1] = (sg[x] as f32 * inv) as u8;
            dst[out_row + x * 4 + 2] = (sb[x] as f32 * inv) as u8;
            dst[out_row + x * 4 + 3] = 255;
        }
        // 移除窗口顶部行（y - radius）
        if y >= radius {
            let rem_row = (y - radius) * width * 4;
            for x in 0..width {
                sr[x]  -= src[rem_row + x * 4] as u32;
                sg[x]  -= src[rem_row + x * 4 + 1] as u32;
                sb[x]  -= src[rem_row + x * 4 + 2] as u32;
                cnt[x] -= 1;
            }
        }
        // 添加窗口底部行（y + radius + 1）
        let add_y = y + radius + 1;
        if add_y < height {
            let add_row = add_y * width * 4;
            for x in 0..width {
                sr[x]  += src[add_row + x * 4] as u32;
                sg[x]  += src[add_row + x * 4 + 1] as u32;
                sb[x]  += src[add_row + x * 4 + 2] as u32;
                cnt[x] += 1;
            }
        }
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
