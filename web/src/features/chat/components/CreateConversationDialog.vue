<template>
	<v-dialog v-model="isOpen" max-width="440" :scrim-opacity="0.6" transition="dialog-bottom-transition" persistent>
		<v-card rounded="lg" elevation="8">
			<!-- 头部 -->
			<v-card-title class="dialog-header px-6 pt-5 pb-0">
				<div class="d-flex align-center ga-3">
					<v-avatar color="primary" size="40" rounded="md">
						<v-icon icon="mdi-message-plus-outline" size="22" color="on-primary" />
					</v-avatar>
					<div>
						<div class="text-subtitle-1 font-weight-bold text-on-surface">新建私聊</div>
						<div class="text-caption text-medium-emphasis">输入对方的用户 ID 发起会话</div>
					</div>
				</div>
				<v-btn icon="mdi-close" variant="text" size="small" density="comfortable" class="close-btn" :disabled="loading" @click="handleClose" />
			</v-card-title>

			<v-divider class="mt-4" />

			<!-- 表单区域 -->
			<v-card-text class="px-6 py-5">
				<v-form ref="formRef" @submit.prevent="handleSubmit">
					<div class="field-label text-caption font-weight-medium text-medium-emphasis mb-2">目标用户 ID</div>
					<v-text-field
						v-model.trim="targetUserIdInput"
						:rules="userIdRules"
						:error-messages="serverError"
						:disabled="loading"
						placeholder="请输入用户ID（纯数字）"
						variant="outlined"
						density="comfortable"
						color="primary"
						rounded="md"
						clearable
						autofocus
						hide-details="auto"
						prepend-inner-icon="mdi-account-search-outline"
						@input="serverError = ''"
					/>

					<v-alert v-if="hint" :type="hint.type" :text="hint.text" density="compact" variant="tonal" rounded="md" class="mt-3" />
				</v-form>
			</v-card-text>

			<v-divider />

			<!-- 操作按钮 -->
			<v-card-actions class="px-6 py-4">
				<v-spacer />
				<v-btn variant="text" color="default" rounded="md" :disabled="loading" @click="handleClose"> 取消 </v-btn>
				<v-btn
					color="primary"
					variant="elevated"
					rounded="md"
					:loading="loading"
					:disabled="!canSubmit"
					prepend-icon="mdi-send-outline"
					min-width="100"
					@click="handleSubmit"
				>
					发起会话
				</v-btn>
			</v-card-actions>
		</v-card>
	</v-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useVModel } from '@vueuse/core'

// ==================== Props & Emits ====================

const props = defineProps({
	modelValue: {
		type: Boolean,
		default: false,
	},
})

const emit = defineEmits(['update:modelValue', 'confirm'])

// ==================== 状态 ====================

const isOpen = useVModel(props, 'modelValue', emit)
const formRef = ref(null)
const targetUserIdInput = ref('')
const loading = ref(false)
const serverError = ref('')
const hint = ref(null)

// ==================== 计算属性 ====================

const canSubmit = computed(() => !loading.value && !!targetUserIdInput.value)

// ==================== 表单校验规则 ====================

const userIdRules = [
	v => !!v || '用户 ID 不能为空',
	v => /^\d+$/.test(v) || '用户 ID 只能包含数字',
	v => Number(v) > 0 || '用户 ID 必须为正整数',
	v => String(v).length <= 18 || '用户 ID 格式不正确',
]

// ==================== 方法 ====================

const resetForm = () => {
	targetUserIdInput.value = ''
	serverError.value = ''
	hint.value = null
	formRef.value?.reset()
}

const handleClose = () => {
	if (loading.value) return
	resetForm()
	isOpen.value = false
}

const handleSubmit = async () => {
	const { valid } = await formRef.value.validate()
	if (!valid || loading.value) return

	loading.value = true
	serverError.value = ''
	hint.value = null

	try {
		emit('confirm', {
			targetUserId: Number(targetUserIdInput.value),
			onSuccess: isNew => {
				hint.value = {
					type: 'success',
					text: isNew ? '会话已创建，即将跳转...' : '会话已存在，即将跳转...',
				}
				setTimeout(() => {
					loading.value = false
					handleClose()
				}, 800)
			},
			onError: message => {
				loading.value = false
				serverError.value = message || '创建失败，请稍后重试'
			},
		})
	} catch {
		loading.value = false
		serverError.value = '操作异常，请稍后重试'
	}
}

// Dialog 关闭时重置表单
watch(isOpen, val => {
	if (!val) resetForm()
})
</script>

<style scoped>
.dialog-header {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
}

.close-btn {
	margin-top: -4px;
	margin-right: -8px;
}

.field-label {
	letter-spacing: 0.03em;
}
</style>
