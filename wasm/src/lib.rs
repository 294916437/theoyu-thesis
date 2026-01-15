mod core;
mod effects;
mod engine;
mod segmentation;

// 导出公共 API
pub use core::{EffectParams, MemoryView};
pub use effects::EffectType;
pub use engine::BackgroundEffectEngine;
