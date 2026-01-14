<template>
	<div class="meeting-statistics">
		<v-row dense>
			<v-col v-for="stat in statisticsData" :key="stat.key" cols="6">
				<v-card class="stat-card" elevation="1">
					<v-card-text class="text-center pa-4">
						<v-icon :color="stat.color" size="large" class="mb-2">
							{{ stat.icon }}
						</v-icon>
						<div class="stat-value text-primary">{{ stat.value }}</div>
						<div class="stat-label text-on-surface-variant">{{ stat.label }}</div>
					</v-card-text>
				</v-card>
			</v-col>
		</v-row>

		<!-- 参与度图表 -->
		<v-divider class="my-4"></v-divider>

		<div class="chart-section">
			<h4 class="text-subtitle-2 mb-3 text-on-surface">参与度趋势</h4>
			<div class="participation-chart">
				<div
					v-for="(point, index) in participationData"
					:key="index"
					class="chart-bar"
					:style="{ height: `${point}%` }"
				>
					<v-tooltip activator="parent" location="top"> {{ point }}% </v-tooltip>
				</div>
			</div>
		</div>

		<!-- 设备统计 -->
		<v-divider class="my-4"></v-divider>

		<div class="device-stats">
			<h4 class="text-subtitle-2 mb-3 text-on-surface">设备统计</h4>
			<div v-for="device in deviceData" :key="device.type" class="device-item">
				<div class="d-flex align-center justify-space-between mb-1">
					<span class="text-caption text-on-surface">{{ device.type }}</span>
					<span class="text-caption font-weight-bold text-primary">{{ device.percentage }}%</span>
				</div>
				<v-progress-linear
					:model-value="device.percentage"
					:color="device.color"
					height="6"
					rounded
					bg-color="surface-variant"
				></v-progress-linear>
			</div>
		</div>
	</div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
	statistics: {
		type: Object,
		default: () => ({}),
	},
})

const statisticsData = computed(() => [
	{
		key: 'avgDuration',
		icon: 'mdi-timer-outline',
		color: 'primary',
		value: `${props.statistics.avgDuration || 0} 分钟`,
		label: '平均时长',
	},
	{
		key: 'maxParticipants',
		icon: 'mdi-account-group',
		color: 'success',
		value: props.statistics.maxParticipants || 0,
		label: '最多参与者',
	},
	{
		key: 'messageCount',
		icon: 'mdi-message-text',
		color: 'info',
		value: props.statistics.messageCount || 0,
		label: '消息数量',
	},
	{
		key: 'screenShareTime',
		icon: 'mdi-monitor-share',
		color: 'warning',
		value: `${props.statistics.screenShareTime || 0} 分钟`,
		label: '屏幕共享',
	},
])

const participationData = computed(() => {
	return props.statistics.participation || [65, 78, 82, 75, 88, 92, 85, 79, 86, 90]
})

const deviceData = computed(() => {
	return (
		props.statistics.devices || [
			{ type: 'PC端', percentage: 65, color: 'primary' },
			{ type: '移动端', percentage: 25, color: 'success' },
			{ type: '平板', percentage: 10, color: 'info' },
		]
	)
})
</script>

<style scoped>
.meeting-statistics {
	padding: 8px;
}

.stat-card {
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	border: 1px solid rgb(var(--v-theme-border));
}

.stat-card:hover {
	transform: translateY(-2px);
	box-shadow: 0 4px 8px rgba(var(--v-theme-primary), 0.15) !important;
}

.stat-value {
	font-size: 20px;
	font-weight: bold;
	margin-bottom: 4px;
	color: rgb(var(--v-theme-primary));
}

.stat-label {
	font-size: 12px;
	color: rgb(var(--v-theme-on-surface-variant));
}

.participation-chart {
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	height: 100px;
	gap: 4px;
	padding: 8px 0;
	background-color: rgb(var(--v-theme-surface-variant), 0.3);
	border-radius: 8px;
}

.chart-bar {
	flex: 1;
	background: linear-gradient(to top, rgb(var(--v-theme-primary)), rgb(var(--v-theme-secondary)));
	border-radius: 4px 4px 0 0;
	min-height: 10%;
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	cursor: pointer;
}

.chart-bar:hover {
	opacity: 0.8;
	transform: scaleY(1.05);
}

.device-item {
	margin-bottom: 12px;
	padding: 8px;
	border-radius: 8px;
	background-color: rgb(var(--v-theme-surface-variant), 0.3);
	transition: background-color 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.device-item:hover {
	background-color: rgb(var(--v-theme-surface-variant), 0.5);
}

.device-item:last-child {
	margin-bottom: 0;
}

.chart-section,
.device-stats {
	padding: 8px;
}
</style>
