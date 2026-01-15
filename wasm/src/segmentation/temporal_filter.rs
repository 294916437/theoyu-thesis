/// EMA 时间平滑滤波器
pub struct TemporalFilter {
    prev_mask: Vec<f32>,
    alpha: f32,
}

impl TemporalFilter {
    pub fn new(size: usize, alpha: f32) -> Self {
        Self {
            prev_mask: vec![0.0; size],
            alpha: alpha.clamp(0.0, 1.0),
        }
    }

    pub fn apply(&mut self, mask: &mut [f32]) {
        for (i, value) in mask.iter_mut().enumerate() {
            let smoothed = self.alpha * *value + (1.0 - self.alpha) * self.prev_mask[i];
            self.prev_mask[i] = smoothed;
            *value = smoothed;
        }
    }

    pub fn reset(&mut self) {
        self.prev_mask.fill(0.0);
    }
}
