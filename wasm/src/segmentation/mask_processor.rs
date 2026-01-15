use crate::core::{Dimensions, math};

pub struct MaskProcessor;

impl MaskProcessor {
    /// 上采样 mask 到目标分辨率
    pub fn upsample_mask(
        mask: &[f32],
        src_dims: Dimensions,
        dst_dims: Dimensions,
        output: &mut [f32],
    ) {
        let (scale_x, scale_y) = math::compute_scale_factors(
            dst_dims.width,
            dst_dims.height,
            src_dims.width,
            src_dims.height,
        );

        for y in 0..dst_dims.height {
            let map_y = y as f32 * scale_y;
            let y_low = map_y.floor() as usize;
            let y_high = (y_low + 1).min(src_dims.height - 1);
            let dy = map_y - y_low as f32;

            for x in 0..dst_dims.width {
                let map_x = x as f32 * scale_x;
                let x_low = map_x.floor() as usize;
                let x_high = (x_low + 1).min(src_dims.width - 1);
                let dx = map_x - x_low as f32;

                let idx_00 = (y_low * src_dims.width + x_low) * 2;
                let idx_10 = (y_low * src_dims.width + x_high) * 2;
                let idx_01 = (y_high * src_dims.width + x_low) * 2;
                let idx_11 = (y_high * src_dims.width + x_high) * 2;

                let v00 = mask[idx_00 + 1] - mask[idx_00];
                let v10 = mask[idx_10 + 1] - mask[idx_10];
                let v01 = mask[idx_01 + 1] - mask[idx_01];
                let v11 = mask[idx_11 + 1] - mask[idx_11];

                let interpolated = math::bilinear_interpolate(v00, v10, v01, v11, dx, dy);
                output[y * dst_dims.width + x] = math::sigmoid_fast(interpolated);
            }
        }
    }
}
