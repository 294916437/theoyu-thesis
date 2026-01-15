use wasm_bindgen::prelude::*;

/// 零拷贝内存视图包装器
#[wasm_bindgen]
pub struct MemoryView {
    ptr: *const u8,
    len: usize,
}

#[wasm_bindgen]
impl MemoryView {
    pub fn new(ptr: *const u8, len: usize) -> Self {
        Self { ptr, len }
    }

    #[wasm_bindgen(getter)]
    pub fn ptr(&self) -> *const u8 {
        self.ptr
    }

    #[wasm_bindgen(getter)]
    pub fn len(&self) -> usize {
        self.len
    }
}

/// 重用缓冲区池
pub struct BufferPool {
    buffers: Vec<Vec<u8>>,
    capacity: usize,
}

impl BufferPool {
    pub fn new(capacity: usize) -> Self {
        Self {
            buffers: Vec::new(),
            capacity,
        }
    }

    pub fn acquire(&mut self, size: usize) -> Vec<u8> {
        if let Some(mut buffer) = self.buffers.pop() {
            buffer.resize(size, 0);
            buffer
        } else {
            vec![0; size]
        }
    }

    pub fn release(&mut self, buffer: Vec<u8>) {
        if self.buffers.len() < self.capacity {
            self.buffers.push(buffer);
        }
    }
}
