<template>
	<v-container>
		<v-row justify="center">
			<v-col cols="12" md="8">
				<v-card class="mx-auto" elevation="4">
					<v-card-item>
						<v-card-title> Vuetify 集成成功！ </v-card-title>
						<v-card-subtitle> 这是一个使用 Vuetify 组件的示例卡片 </v-card-subtitle>
					</v-card-item>

					<v-card-text>
						<p class="mb-4">你可以使用 Material Design 风格的按钮、输入框和布局系统。</p>

						<v-text-field label="请输入内容" variant="outlined" prepend-icon="mdi-pencil"></v-text-field>

						<div class="d-flex gap-4 mt-4">
							<v-btn color="primary" prepend-icon="mdi-check"> 确认 </v-btn>
							<v-btn color="error" variant="text" prepend-icon="mdi-close"> 取消 </v-btn>
						</div>
					</v-card-text>
				</v-card>
			</v-col>
		</v-row>
	</v-container>
</template>

<script setup>
// 无需手动导入组件，vite-plugin-vuetify 会自动处理
</script>
