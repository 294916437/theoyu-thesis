<template>
	<div>
		<!-- 按钮展示 -->
		<v-card class="mb-6">
			<v-card-title>按钮组件</v-card-title>
			<v-card-text>
				<div class="mb-4">
					<h4 class="mb-3">实心按钮</h4>
					<div class="d-flex flex-wrap gap-2">
						<v-btn color="primary">Primary</v-btn>
						<v-btn color="secondary">Secondary</v-btn>
						<v-btn color="accent">Accent</v-btn>
						<v-btn color="success">Success</v-btn>
						<v-btn color="error">Error</v-btn>
						<v-btn color="warning">Warning</v-btn>
						<v-btn color="info">Info</v-btn>
					</div>
				</div>

				<div class="mb-4">
					<h4 class="mb-3">轮廓按钮</h4>
					<div class="d-flex flex-wrap gap-2">
						<v-btn variant="outlined" color="primary">Primary</v-btn>
						<v-btn variant="outlined" color="secondary">Secondary</v-btn>
						<v-btn variant="outlined" color="success">Success</v-btn>
						<v-btn variant="outlined" color="error">Error</v-btn>
					</div>
				</div>

				<div class="mb-4">
					<h4 class="mb-3">文本按钮</h4>
					<div class="d-flex flex-wrap gap-2">
						<v-btn variant="text" color="primary">Primary</v-btn>
						<v-btn variant="text" color="secondary">Secondary</v-btn>
						<v-btn variant="text" color="success">Success</v-btn>
						<v-btn variant="text" color="error">Error</v-btn>
					</div>
				</div>

				<div class="mb-4">
					<h4 class="mb-3">图标按钮</h4>
					<div class="d-flex flex-wrap gap-2">
						<v-btn icon="mdi-heart" color="primary"></v-btn>
						<v-btn icon="mdi-star" color="warning"></v-btn>
						<v-btn icon="mdi-thumb-up" color="success"></v-btn>
						<v-btn icon="mdi-delete" color="error"></v-btn>
					</div>
				</div>

				<div>
					<h4 class="mb-3">按钮尺寸</h4>
					<div class="d-flex flex-wrap gap-2 align-center">
						<v-btn size="x-small" color="primary">X-Small</v-btn>
						<v-btn size="small" color="primary">Small</v-btn>
						<v-btn size="default" color="primary">Default</v-btn>
						<v-btn size="large" color="primary">Large</v-btn>
						<v-btn size="x-large" color="primary">X-Large</v-btn>
					</div>
				</div>
			</v-card-text>
		</v-card>

		<!-- 卡片展示 -->
		<v-card class="mb-6">
			<v-card-title>卡片组件</v-card-title>
			<v-card-text>
				<v-row>
					<v-col v-for="color in cardColors" :key="color" cols="12" sm="6" md="4">
						<v-card :color="color" elevation="4">
							<v-card-title>{{ color.toUpperCase() }}</v-card-title>
							<v-card-subtitle>卡片副标题</v-card-subtitle>
							<v-card-text> 这是一个使用 {{ color }} 颜色的卡片示例。 </v-card-text>
							<v-card-actions>
								<v-btn variant="text">操作</v-btn>
								<v-spacer></v-spacer>
								<v-btn icon="mdi-heart"></v-btn>
								<v-btn icon="mdi-share"></v-btn>
							</v-card-actions>
						</v-card>
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>

		<!-- 芯片和徽章 -->
		<v-card class="mb-6">
			<v-card-title>芯片和徽章</v-card-title>
			<v-card-text>
				<div class="mb-4">
					<h4 class="mb-3">芯片 (Chips)</h4>
					<div class="d-flex flex-wrap gap-2">
						<v-chip color="primary">Primary</v-chip>
						<v-chip color="secondary">Secondary</v-chip>
						<v-chip color="success" prepend-icon="mdi-check">Success</v-chip>
						<v-chip color="error" prepend-icon="mdi-alert">Error</v-chip>
						<v-chip color="warning" closable>Warning</v-chip>
						<v-chip color="info" variant="outlined">Info</v-chip>
					</div>
				</div>

				<div>
					<h4 class="mb-3">徽章 (Badges)</h4>
					<div class="d-flex flex-wrap gap-4">
						<v-badge content="6" color="primary">
							<v-icon icon="mdi-email" size="large"></v-icon>
						</v-badge>
						<v-badge content="99+" color="error">
							<v-icon icon="mdi-bell" size="large"></v-icon>
						</v-badge>
						<v-badge dot color="success">
							<v-icon icon="mdi-account" size="large"></v-icon>
						</v-badge>
						<v-badge content="NEW" color="warning">
							<v-icon icon="mdi-shopping" size="large"></v-icon>
						</v-badge>
					</div>
				</div>
			</v-card-text>
		</v-card>

		<!-- 警告框 -->
		<v-card class="mb-6">
			<v-card-title>警告框</v-card-title>
			<v-card-text>
				<v-alert type="success" class="mb-3"> 这是一个成功提示信息 </v-alert>
				<v-alert type="info" class="mb-3"> 这是一个信息提示 </v-alert>
				<v-alert type="warning" class="mb-3"> 这是一个警告提示 </v-alert>
				<v-alert type="error" class="mb-3"> 这是一个错误提示 </v-alert>

				<v-alert type="success" variant="tonal" class="mb-3"> 这是一个柔和样式的成功提示 </v-alert>
				<v-alert type="error" variant="outlined" closable> 这是一个可关闭的轮廓样式错误提示 </v-alert>
			</v-card-text>
		</v-card>

		<!-- 进度条和加载 -->
		<v-card>
			<v-card-title>进度指示器</v-card-title>
			<v-card-text>
				<div class="mb-6">
					<h4 class="mb-3">进度条</h4>
					<v-progress-linear color="primary" :model-value="75" class="mb-3"></v-progress-linear>
					<v-progress-linear color="secondary" :model-value="50" class="mb-3"></v-progress-linear>
					<v-progress-linear color="success" :model-value="100" class="mb-3"></v-progress-linear>
					<v-progress-linear color="warning" indeterminate></v-progress-linear>
				</div>

				<div>
					<h4 class="mb-3">加载圈</h4>
					<div class="d-flex flex-wrap gap-4">
						<v-progress-circular color="primary" indeterminate></v-progress-circular>
						<v-progress-circular color="secondary" :model-value="75"></v-progress-circular>
						<v-progress-circular color="success" :model-value="100" size="64">
							<v-icon icon="mdi-check"></v-icon>
						</v-progress-circular>
						<v-progress-circular color="error" indeterminate size="48"></v-progress-circular>
					</div>
				</div>
			</v-card-text>
		</v-card>
	</div>
</template>

<script setup>
const cardColors = ['primary', 'secondary', 'success']
</script>

<style scoped>
.gap-2 {
	gap: 8px;
}

.gap-4 {
	gap: 16px;
}
</style>
