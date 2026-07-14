<template>
	<div>
		<!-- 文本输入 -->
		<v-card class="mb-6">
			<v-card-title>文本输入框</v-card-title>
			<v-card-text>
				<v-row>
					<v-col cols="12" md="6">
						<v-text-field
							label="Primary Color"
							color="primary"
							variant="outlined"
							placeholder="输入文本..."
						></v-text-field>
					</v-col>
					<v-col cols="12" md="6">
						<v-text-field
							label="Secondary Color"
							color="secondary"
							variant="outlined"
							prepend-inner-icon="mdi-account"
						></v-text-field>
					</v-col>
					<v-col cols="12" md="6">
						<v-text-field
							label="Success State"
							color="success"
							variant="filled"
							hint="这是一个提示文本"
							persistent-hint
						></v-text-field>
					</v-col>
					<v-col cols="12" md="6">
						<v-text-field
							label="Error State"
							color="error"
							variant="solo"
							error-messages="这是一个错误信息"
						></v-text-field>
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>

		<!-- 选择器 -->
		<v-card class="mb-6">
			<v-card-title>选择器</v-card-title>
			<v-card-text>
				<v-row>
					<v-col cols="12" md="6">
						<v-select
							label="单选下拉"
							:items="['选项 1', '选项 2', '选项 3']"
							color="primary"
							variant="outlined"
						></v-select>
					</v-col>
					<v-col cols="12" md="6">
						<v-select
							label="多选下拉"
							:items="['选项 1', '选项 2', '选项 3', '选项 4']"
							color="secondary"
							variant="outlined"
							multiple
							chips
						></v-select>
					</v-col>
					<v-col cols="12">
						<v-autocomplete
							label="自动完成"
							:items="['Apple', 'Banana', 'Cherry', 'Date', 'Elderberry']"
							color="success"
							variant="outlined"
							prepend-inner-icon="mdi-magnify"
						></v-autocomplete>
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>

		<!-- 复选框和单选框 -->
		<v-card class="mb-6">
			<v-card-title>复选框和单选框</v-card-title>
			<v-card-text>
				<v-row>
					<v-col cols="12" md="6">
						<h4 class="mb-3">复选框</h4>
						<v-checkbox label="Primary" color="primary"></v-checkbox>
						<v-checkbox label="Secondary" color="secondary"></v-checkbox>
						<v-checkbox label="Success" color="success"></v-checkbox>
						<v-checkbox label="Error" color="error"></v-checkbox>
					</v-col>
					<v-col cols="12" md="6">
						<h4 class="mb-3">单选框</h4>
						<v-radio-group>
							<v-radio label="Primary" value="1" color="primary"></v-radio>
							<v-radio label="Secondary" value="2" color="secondary"></v-radio>
							<v-radio label="Success" value="3" color="success"></v-radio>
							<v-radio label="Warning" value="4" color="warning"></v-radio>
						</v-radio-group>
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>

		<!-- 开关和滑块 -->
		<v-card class="mb-6">
			<v-card-title>开关和滑块</v-card-title>
			<v-card-text>
				<v-row>
					<v-col cols="12" md="6">
						<h4 class="mb-3">开关</h4>
						<v-switch label="Primary" color="primary" inset></v-switch>
						<v-switch label="Secondary" color="secondary" inset></v-switch>
						<v-switch label="Success" color="success" inset></v-switch>
						<v-switch label="Error" color="error" inset></v-switch>
					</v-col>
					<v-col cols="12" md="6">
						<h4 class="mb-3">滑块</h4>
						<v-slider color="primary" :model-value="30" class="mb-4"></v-slider>
						<v-slider color="secondary" :model-value="50" class="mb-4"></v-slider>
						<v-slider color="success" :model-value="75" class="mb-4"></v-slider>
						<v-slider color="warning" :model-value="90"></v-slider>
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>

		<!-- 文本域 -->
		<v-card>
			<v-card-title>文本域</v-card-title>
			<v-card-text>
				<v-row>
					<v-col cols="12" md="6">
						<v-textarea
							label="Outlined"
							color="primary"
							variant="outlined"
							rows="3"
							placeholder="输入多行文本..."
						></v-textarea>
					</v-col>
					<v-col cols="12" md="6">
						<v-textarea label="Filled" color="secondary" variant="filled" rows="3" auto-grow></v-textarea>
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>
	</div>
</template>

<script setup>
// 表单组件展示
</script>
