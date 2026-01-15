/// 快速 Sigmoid 近似（使用有理逼近）
#[inline(always)]
pub fn sigmoid_fast(x: f32) -> f32 {
    let clamped = x.clamp(-6.0, 6.0);
    1.0 / (1.0 + (-clamped).exp())
}

/// 双线性插值
#[inline(always)]
pub fn bilinear_interpolate(v00: f32, v10: f32, v01: f32, v11: f32, dx: f32, dy: f32) -> f32 {
    let top = v00 * (1.0 - dx) + v10 * dx;
    let bottom = v01 * (1.0 - dx) + v11 * dx;
    top * (1.0 - dy) + bottom * dy
}

/// 计算缩放因子
pub fn compute_scale_factors(
    src_width: usize,
    src_height: usize,
    dst_width: usize,
    dst_height: usize,
) -> (f32, f32) {
    let scale_x = (src_width - 1) as f32 / (dst_width - 1) as f32;
    let scale_y = (src_height - 1) as f32 / (dst_height - 1) as f32;
    (scale_x, scale_y)
}
