import onnx
import numpy as np

def check_model_details(model_path, model_name):
    print(f"\n{'='*60}")
    print(f"模型: {model_name}")
    print(f"{'='*60}")
    
    try:
        model = onnx.load(model_path)
        
        # 1. ONNX 版本信息
        print(f"\n📌 ONNX 版本信息:")
        print(f"  - IR 版本: {model.ir_version}")
        print(f"  - Opset 版本: {model.opset_import[0].version if model.opset_import else 'Unknown'}")
        print(f"  - Producer: {model.producer_name} {model.producer_version}")
        
        # 2. 输入输出
        print(f"\n📌 输入信息:")
        for inp in model.graph.input:
            shape = [d.dim_value for d in inp.type.tensor_type.shape.dim]
            dtype = inp.type.tensor_type.elem_type
            print(f"  {inp.name}: {shape} (dtype={dtype})")
        
        print(f"\n📌 输出信息:")
        for out in model.graph.output:
            shape = [d.dim_value for d in out.type.tensor_type.shape.dim]
            dtype = out.type.tensor_type.elem_type
            print(f"  {out.name}: {shape} (dtype={dtype})")
        
        # 3. 算子统计
        ops = {}
        for node in model.graph.node:
            ops[node.op_type] = ops.get(node.op_type, 0) + 1
        
        print(f"\n📌 算子使用统计 ({len(ops)} 种, 共 {sum(ops.values())} 个节点):")
        for op, count in sorted(ops.items()):
            print(f"  - {op}: {count} 次")
        
        # 4. 关键算子属性检查
        print(f"\n📌 关键算子详细信息:")
        
        # 检查 Resize 算子
        resize_nodes = [n for n in model.graph.node if n.op_type == "Resize"]
        if resize_nodes:
            print(f"\n  🔍 Resize 算子 ({len(resize_nodes)} 个):")
            for i, node in enumerate(resize_nodes):
                attrs = {attr.name: attr for attr in node.attribute}
                mode = attrs.get('mode')
                coord_mode = attrs.get('coordinate_transformation_mode')
                print(f"    [{i+1}] {node.name}:")
                print(f"        - mode: {mode.s.decode() if mode else 'N/A'}")
                print(f"        - coordinate_transformation_mode: {coord_mode.s.decode() if coord_mode else 'N/A'}")
                print(f"        - inputs: {len(node.input)}")
        
        # 检查 ConvTranspose 算子
        convt_nodes = [n for n in model.graph.node if n.op_type == "ConvTranspose"]
        if convt_nodes:
            print(f"\n  🔍 ConvTranspose 算子 ({len(convt_nodes)} 个):")
            for i, node in enumerate(convt_nodes):
                attrs = {attr.name: attr for attr in node.attribute}
                print(f"    [{i+1}] {node.name}:")
                for attr_name in ['kernel_shape', 'strides', 'pads', 'output_padding', 'group']:
                    if attr_name in attrs:
                        attr = attrs[attr_name]
                        if attr.ints:
                            print(f"        - {attr_name}: {list(attr.ints)}")
                        elif attr.i:
                            print(f"        - {attr_name}: {attr.i}")
        
        # 5. 模型大小
        print(f"\n📌 模型信息:")
        import os
        size_mb = os.path.getsize(model_path) / 1024 / 1024
        print(f"  - 文件大小: {size_mb:.2f} MB")
        print(f"  - 总节点数: {len(model.graph.node)}")
        print(f"  - 初始化器数量: {len(model.graph.initializer)}")
        
    except Exception as e:
        print(f"❌ 错误: {e}")
        import traceback
        traceback.print_exc()

# 检查两个模型
check_model_details("d:/git/monorepo/theoyu-thesis/test/model_float32.onnx", "model_float32.onnx")
check_model_details("d:/git/monorepo/theoyu-thesis/test/model_float32_opt.onnx", "model_float32_opt.onnx")

print(f"\n{'='*60}")
print("🔍 关键差异对比")
print(f"{'='*60}")