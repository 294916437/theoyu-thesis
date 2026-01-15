import onnx
import numpy as np

try:
    # 加载模型
    model = onnx.load("d:/git/monorepo/theoyu-thesis/test/model_float32_opt.onnx")
    # 检查输入
    print("输入信息:")
    for inp in model.graph.input:
        shape = [d.dim_value for d in inp.type.tensor_type.shape.dim]
        print(f"  {inp.name}: {shape}")
    
    # 检查输出
    print("输出信息:")
    for out in model.graph.output:
        shape = [d.dim_value for d in out.type.tensor_type.shape.dim]
        print(f"  {out.name}: {shape}")
    
    # 列出算子
    ops = set(node.op_type for node in model.graph.node)
    print(f"使用的算子 ({len(ops)} 种):")
    for op in sorted(ops):
        print(f"  - {op}")
        
except Exception as e:
    print(f"错误: {e}")