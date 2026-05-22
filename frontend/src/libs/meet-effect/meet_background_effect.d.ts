/* tslint:disable */
/* eslint-disable */

export class MeetProcessor {
    private constructor();
    free(): void;
    [Symbol.dispose](): void;
    background_ptr(): number;
    input_ptr(): number;
    mask_ptr(): number;
    static new(width: number, height: number): MeetProcessor;
    output_ptr(): number;
    prepare_mask(): void;
    render_blur(): void;
    render_replace(): void;
    /**
     * 动态调整处理分辨率（仅重建宽高相关缓冲区，mask 尺寸保持 256×144）。
     */
    resize(width: number, height: number): void;
    /**
     * 设置模糊半径（每次 box blur 的半径，默认 20，范围 1..=50）
     */
    set_blur_radius(radius: number): void;
}

export function init_panic_hook(): void;

export type InitInput = RequestInfo | URL | Response | BufferSource | WebAssembly.Module;

export interface InitOutput {
    readonly memory: WebAssembly.Memory;
    readonly __wbg_meetprocessor_free: (a: number, b: number) => void;
    readonly init_panic_hook: () => void;
    readonly meetprocessor_background_ptr: (a: number) => number;
    readonly meetprocessor_input_ptr: (a: number) => number;
    readonly meetprocessor_mask_ptr: (a: number) => number;
    readonly meetprocessor_new: (a: number, b: number) => number;
    readonly meetprocessor_output_ptr: (a: number) => number;
    readonly meetprocessor_prepare_mask: (a: number) => void;
    readonly meetprocessor_render_blur: (a: number) => void;
    readonly meetprocessor_render_replace: (a: number) => void;
    readonly meetprocessor_resize: (a: number, b: number, c: number) => void;
    readonly meetprocessor_set_blur_radius: (a: number, b: number) => void;
    readonly __wbindgen_free: (a: number, b: number, c: number) => void;
    readonly __wbindgen_malloc: (a: number, b: number) => number;
    readonly __wbindgen_realloc: (a: number, b: number, c: number, d: number) => number;
    readonly __wbindgen_externrefs: WebAssembly.Table;
    readonly __wbindgen_start: () => void;
}

export type SyncInitInput = BufferSource | WebAssembly.Module;

/**
 * Instantiates the given `module`, which can either be bytes or
 * a precompiled `WebAssembly.Module`.
 *
 * @param {{ module: SyncInitInput }} module - Passing `SyncInitInput` directly is deprecated.
 *
 * @returns {InitOutput}
 */
export function initSync(module: { module: SyncInitInput } | SyncInitInput): InitOutput;

/**
 * If `module_or_path` is {RequestInfo} or {URL}, makes a request and
 * for everything else, calls `WebAssembly.instantiate` directly.
 *
 * @param {{ module_or_path: InitInput | Promise<InitInput> }} module_or_path - Passing `InitInput` directly is deprecated.
 *
 * @returns {Promise<InitOutput>}
 */
export default function __wbg_init (module_or_path?: { module_or_path: InitInput | Promise<InitInput> } | InitInput | Promise<InitInput>): Promise<InitOutput>;
