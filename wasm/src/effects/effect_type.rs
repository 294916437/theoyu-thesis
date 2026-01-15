use wasm_bindgen::prelude::*;

#[wasm_bindgen]
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum EffectType {
    None,
    Blur,
    Replace,
    GreenScreen,
}
