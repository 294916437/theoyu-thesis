import matplotlib.pyplot as plt
import numpy as np

# 解决图表中文字体显示问题
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'Arial Unicode MS'] # 支持Win和Mac
plt.rcParams['axes.unicode_minus'] = False # 正常显示负号

# ==========================================
# 1. 重构准确的数据
# ==========================================

# X轴时间（以秒为单位），用于计算数据，最大约570秒（9.5分钟）
# 原图数据点大约每 10 秒采集一次
x_seconds_full = np.arange(0, 580, 10)

# 定义关键拐点 (Anchor points) 的 [时间秒, 毫秒值]
x_anchor = [0, 10, 20, 30, 530, 540, 550, 560, 570]

# 根据原图Y轴高度提取的关键锚点数值
y_green_anchor = [40, 160, 590, 780,  50,  30,  45,  90,  65]
y_red_anchor   = [10,  60, 290, 420,  15,  10,  12,  15,  12]
y_blue_anchor  = [ 5,  20, 100, 230,   5,   2,   3,   8,   5]

# 使用 numpy 插值补全中间的线性衰减点，还原原图的连续点状效果
y_green = np.interp(x_seconds_full, x_anchor, y_green_anchor)
y_red   = np.interp(x_seconds_full, x_anchor, y_red_anchor)
y_blue  = np.interp(x_seconds_full, x_anchor, y_blue_anchor)

# 【核心需求】：将 X 轴数据由秒转换为“分钟”
x_minutes = x_seconds_full / 60.0

# ==========================================
# 2. 开始绘制图表
# ==========================================
fig, ax = plt.subplots(figsize=(12, 6)) # 设置类似原图的宽比例画布

# 定义与原图一致的十六进制颜色
c_blue = '#05123d'   # 深蓝色 (POST /user/findById)
c_red = '#e31b34'    # 红色 (GET /room/message/history)
c_green = '#008b41'  # 绿色 (GET /room/detail/{id})

# 绘制三条折线图，设置线宽和圆点标记
ax.plot(x_minutes, y_blue, marker='o', markersize=6, color=c_blue, linewidth=2.5, label='POST /user/findById')
ax.plot(x_minutes, y_red,  marker='o', markersize=6, color=c_red, linewidth=2.5, label='GET /room/message/history')
ax.plot(x_minutes, y_green, marker='o', markersize=6, color=c_green, linewidth=2.5, label='GET /room/detail/{id}')

# ==========================================
# 3. 设置图表格式以还原 Jmeter 风格
# ==========================================
ax.set_title('响应时间图', fontsize=16, pad=15)
ax.set_ylabel('毫秒', fontsize=10, labelpad=10)
ax.set_xlabel('经过时间 (分钟)', fontsize=10, labelpad=10)

# Y 轴设置：范围 0~800，步长 80 (完全匹配原图)
ax.set_yticks(np.arange(0, 801, 80))
ax.set_ylim(-10, 800) 

# X 轴设置：显示 0 到 10 分钟，以 1 分钟为刻度
ax.set_xticks(np.arange(0, 11, 1))
ax.set_xlim(-0.1, 9.7) # 留出一点边距

# 设置水平方向的网格线
ax.grid(axis='y', linestyle='-', color='#d3d3d3', linewidth=1)

# 设置图例 (放置在底部，水平排列，无边框)
ax.legend(loc='upper center', bbox_to_anchor=(0.5, -0.12), ncol=3, frameon=False, 
          handlelength=1, handletextpad=0.5, columnspacing=2)

# 调整布局以防止底部的图例被遮挡
plt.tight_layout()

# 显示图表
plt.show()