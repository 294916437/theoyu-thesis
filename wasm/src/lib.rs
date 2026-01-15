use std::io::Cursor;
use tract_onnx::prelude::*;
use wasm_bindgen::prelude::*;

// 模型固定的输入尺寸
const MODEL_WIDTH: usize = 256;
const MODEL_HEIGHT: usize = 144;

type RunnableModel = SimplePlan<TypedFact, Box<dyn TypedOp>, Graph<TypedFact, Box<dyn TypedOp>>>;

#[wasm_bindgen]
pub struct BackgroundTransformer {
    model: RunnableModel,
    input_tensor_buffer: Vec<f32>, // 存储预处理后的数据
    output_buffer: Vec<u8>,        // 存储最终渲染的图像 (RGBA)
    width: usize,                  // 实际视频宽度 (e.g., 640)
    height: usize,                 // 实际视频高度 (e.g., 480)
}

#[wasm_bindgen]
impl BackgroundTransformer {
    pub fn new(
        model_bytes: &[u8],
        width: usize,
        height: usize,
    ) -> Result<BackgroundTransformer, JsValue> {
        console_error_panic_hook::set_once();

        let mut cursor = Cursor::new(model_bytes);
        let model = tract_onnx::onnx()
            .model_for_read(&mut cursor)
            .map_err(|e| e.to_string())?
            .into_optimized()
            .map_err(|e| e.to_string())?
            .into_runnable()
            .map_err(|e| e.to_string())?;

        Ok(BackgroundTransformer {
            model,
            // NHWC 格式：144 * 256 * 3
            input_tensor_buffer: vec![0.0; MODEL_WIDTH * MODEL_HEIGHT * 3],
            output_buffer: vec![0; width * height * 4],
            width,
            height,
        })
    }

    pub fn get_output_ptr(&self) -> *const u8 {
        self.output_buffer.as_ptr()
    }

    pub fn process_frame(&mut self, input_ptr: *const u8) -> Result<(), JsValue> {
        let input_slice =
            unsafe { std::slice::from_raw_parts(input_ptr, self.width * self.height * 4) };

        // 1. 预处理：RGBA -> RGB (NHWC Layout)
        self.preprocess_input(input_slice);

        // 2. 推理
        // 注意：这里 shape 是 [1, 144, 256, 3] (NHWC)
        let tensor = Tensor::from_shape(
            &[1, MODEL_HEIGHT, MODEL_WIDTH, 3],
            &self.input_tensor_buffer,
        )
        .map_err(|e| e.to_string())?;

        let result = self
            .model
            .run(tvec!(tensor.into()))
            .map_err(|e| e.to_string())?;

        // 3. 获取输出
        // 输出 Shape 是 [1, 144, 256, 2]
        // 数据排列是：[bg0, fg0, bg1, fg1, bg2, fg2, ...]
        let output_view = result[0]
            .to_array_view::<f32>()
            .map_err(|e| e.to_string())?;

        // 4. 后处理与合成
        self.apply_effect(input_slice, &output_view);

        Ok(())
    }

    // 关键修改：适配 NHWC (1x144x256x3)
    fn preprocess_input(&mut self, input: &[u8]) {
        let scale_x = self.width as f32 / MODEL_WIDTH as f32;
        let scale_y = self.height as f32 / MODEL_HEIGHT as f32;

        for y in 0..MODEL_HEIGHT {
            for x in 0..MODEL_WIDTH {
                // 最近邻采样 (Nearest Neighbor)
                let src_x = (x as f32 * scale_x) as usize;
                let src_y = (y as f32 * scale_y) as usize;

                // 边界检查，防止 panic
                let safe_src_x = src_x.min(self.width - 1);
                let safe_src_y = src_y.min(self.height - 1);

                let idx = (safe_src_y * self.width + safe_src_x) * 4;

                // 归一化 [0, 255] -> [0.0, 1.0] (MediaPipe 标准)
                let r = input[idx] as f32 / 255.0;
                let g = input[idx + 1] as f32 / 255.0;
                let b = input[idx + 2] as f32 / 255.0;

                // 写入 input_tensor_buffer
                // NHWC 布局：RGB, RGB, RGB...
                let offset = (y * MODEL_WIDTH + x) * 3;
                self.input_tensor_buffer[offset] = r;
                self.input_tensor_buffer[offset + 1] = g;
                self.input_tensor_buffer[offset + 2] = b;
            }
        }
    }

    // 关键修改：适配双通道输出 (Background, Foreground)
    fn apply_effect(&mut self, input: &[u8], output_tensor: &tract_ndarray::ArrayViewD<f32>) {
        let scale_x = MODEL_WIDTH as f32 / self.width as f32;
        let scale_y = MODEL_HEIGHT as f32 / self.height as f32;

        // 获取底层数据切片，扁平化处理
        // 布局：[bg, fg, bg, fg, ...]
        let mask_data = output_tensor.as_slice().unwrap();

        for y in 0..self.height {
            for x in 0..self.width {
                let idx = (y * self.width + x) * 4;

                // 映射回模型坐标
                let mx = (x as f32 * scale_x) as usize;
                let my = (y as f32 * scale_y) as usize;

                let safe_mx = mx.min(MODEL_WIDTH - 1);
                let safe_my = my.min(MODEL_HEIGHT - 1);

                // 计算 Mask 索引
                // 每个像素有 2 个值，所以乘以 2
                let mask_offset = (safe_my * MODEL_WIDTH + safe_mx) * 2;

                // 读取 Logits (未经过 Softmax 的原始值)
                let bg_logit = mask_data[mask_offset];
                let fg_logit = mask_data[mask_offset + 1];

                // 简单的 Softmax 模拟或直接比较
                // 如果 fg > bg，则是人像
                // 为了更好的效果，我们可以做一个简单的 Sigmoid 映射来实现软边缘
                // score = fg - bg; probability = 1 / (1 + exp(-score))

                let is_person = fg_logit > bg_logit;

                if is_person {
                    // 前景：直接拷贝原图
                    self.output_buffer[idx] = input[idx];
                    self.output_buffer[idx + 1] = input[idx + 1];
                    self.output_buffer[idx + 2] = input[idx + 2];
                    self.output_buffer[idx + 3] = 255;
                } else {
                    // 背景：应用简单的变暗/绿色滤镜 (模拟虚化)
                    // 实际项目中这里应该读取预先模糊好的背景图
                    self.output_buffer[idx] = input[idx] / 2;
                    self.output_buffer[idx + 1] = input[idx + 1] / 2;
                    self.output_buffer[idx + 2] = input[idx + 2] / 2;
                    self.output_buffer[idx + 3] = 255;
                }
            }
        }
    }
}
