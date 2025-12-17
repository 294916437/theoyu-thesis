<template>
	<v-container class="pa-0">
		<!-- 头部区域 -->
		<div class="profile-header">
			<!-- 背景图 -->
			<div class="background-wrapper">
				<v-img :src="backgroundUrl" cover height="300" class="background-image">
					<div class="background-content">
						<!-- 背景图编辑按钮 -->
						<transition name="fade">
							<div v-if="isEditing" class="background-edit-btn">
								<input
									ref="backgroundInputRef"
									type="file"
									accept="image/*"
									style="display: none"
									@change="handleBackgroundChange"
								/>
								<v-btn
									icon="mdi-camera"
									color="white"
									size="large"
									elevation="4"
									class="edit-btn"
									:loading="uploadingBackground"
									@click="backgroundInputRef?.click()"
								/>
							</div>
						</transition>
					</div>

					<template #placeholder>
						<v-row class="fill-height ma-0" align="center" justify="center">
							<v-progress-circular indeterminate color="primary" />
						</v-row>
					</template>
				</v-img>
			</div>

			<!-- 头像和昵称 -->
			<v-container class="avatar-container">
				<div class="avatar-wrapper">
					<!-- 头像上传 -->
					<div class="avatar-uploader">
						<v-avatar :size="120" class="avatar-main" :class="{ 'avatar-editing': isEditing }">
							<v-img :src="avatarUrl" cover>
								<template #placeholder>
									<v-row class="fill-height ma-0" align="center" justify="center">
										<v-progress-circular indeterminate color="primary" />
									</v-row>
								</template>
							</v-img>

							<!-- 编辑模式覆盖层 -->
							<v-overlay
								v-if="isEditing"
								:model-value="true"
								contained
								class="align-center justify-center avatar-overlay"
								@click="avatarInputRef?.click()"
							>
								<div class="avatar-edit-content">
									<v-progress-circular v-if="uploadingAvatar" indeterminate color="white" size="32" />
									<template v-else>
										<v-icon size="32" color="white">mdi-camera</v-icon>
										<div class="text-caption mt-1">更换头像</div>
									</template>
								</div>
							</v-overlay>
						</v-avatar>

						<!-- 隐藏的文件上传输入 -->
						<input
							ref="avatarInputRef"
							type="file"
							accept="image/*"
							style="display: none"
							@change="handleAvatarChange"
						/>
					</div>

					<div class="nickname-wrapper">
						<h2 class="text-h4 font-weight-bold text-white nickname">
							{{ displayNickname }}
						</h2>
					</div>
				</div>
			</v-container>
		</div>

		<!-- 信息编辑区域 -->
		<v-container class="profile-content">
			<v-row justify="center">
				<v-col cols="12" md="8" lg="6">
					<v-card elevation="2" rounded="lg">
						<v-card-title class="d-flex justify-space-between align-center">
							<span class="text-h6">个人信息</span>
							<v-btn
								v-if="!isEditing"
								variant="text"
								color="primary"
								prepend-icon="mdi-pencil"
								@click="startEdit"
							>
								编辑
							</v-btn>
							<div v-else>
								<v-btn variant="text" color="error" class="mr-2" @click="cancelEdit"> 取消 </v-btn>
								<v-btn variant="text" color="primary" :loading="saving" @click="saveProfile">
									保存
								</v-btn>
							</div>
						</v-card-title>

						<v-divider />

						<v-card-text class="pa-6">
							<v-form ref="formRef" @submit.prevent>
								<!-- 昵称 -->
								<div class="info-field">
									<div class="field-label">
										<v-icon size="20" color="primary" class="mr-2">mdi-account</v-icon>
										<span class="text-subtitle-2 font-weight-medium">昵称</span>
									</div>
									<v-text-field
										v-if="isEditing"
										v-model="editForm.nickname"
										placeholder="请输入昵称"
										:rules="[rules.required, rules.nickname]"
										counter="20"
										maxlength="20"
										density="comfortable"
									/>
									<div v-else class="field-value">{{ userProfile.nickname || '未设置' }}</div>
								</div>

								<v-divider class="my-4" />

								<!-- 性别 -->
								<div class="info-field">
									<div class="field-label">
										<v-icon size="20" color="primary" class="mr-2">mdi-gender-male-female</v-icon>
										<span class="text-subtitle-2 font-weight-medium">性别</span>
									</div>
									<v-radio-group v-if="isEditing" v-model="editForm.sex" inline density="comfortable">
										<v-radio label="男" :value="1" />
										<v-radio label="女" :value="2" />
										<v-radio label="保密" :value="0" />
									</v-radio-group>
									<div v-else class="field-value">{{ formatSex(userProfile.sex) }}</div>
								</div>

								<v-divider class="my-4" />

								<!-- 生日 -->
								<div class="info-field">
									<div class="field-label">
										<v-icon size="20" color="primary" class="mr-2">mdi-cake-variant</v-icon>
										<span class="text-subtitle-2 font-weight-medium">生日</span>
									</div>
									<v-text-field
										v-if="isEditing"
										v-model="editForm.birthday"
										type="date"
										:rules="[rules.required]"
										density="comfortable"
									/>
									<div v-else class="field-value">{{ formatBirthday(userProfile.birthday) }}</div>
								</div>

								<v-divider class="my-4" />

								<!-- 个人简介 -->
								<div class="info-field">
									<div class="field-label">
										<v-icon size="20" color="primary" class="mr-2">mdi-text</v-icon>
										<span class="text-subtitle-2 font-weight-medium">个人简介</span>
									</div>
									<v-textarea
										v-if="isEditing"
										v-model="editForm.introduction"
										placeholder="请输入个人简介"
										:rules="[rules.introduction]"
										counter="200"
										maxlength="200"
										rows="4"
										auto-grow
										density="comfortable"
									/>
									<div v-else class="field-value multiline">
										{{ userProfile.introduction || '暂无简介' }}
									</div>
								</div>
							</v-form>
						</v-card-text>
					</v-card>
				</v-col>
			</v-row>
		</v-container>
	</v-container>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useCloned } from '@vueuse/core'
import { useDateFormat } from '@vueuse/core'
import { $notify } from '@/plugins/notification'
import { useUserStore } from '@/stores/user'
import { getUserProfile, updateUserProfile } from '@/api/user'
import { uploadFile } from '@/api/file'

// 导入默认图片
import defaultAvatar from '@/assets/image/default-avatar.png'
import defaultBackground from '@/assets/image/default-background.jpg'

const userStore = useUserStore()
const formRef = ref(null)

// 用户信息
const userProfile = reactive({
	nickname: '',
	avatar: '',
	sex: 0,
	birthday: '',
	backgroundImg: '',
	introduction: '',
})

// 编辑状态
const isEditing = ref(false)
const saving = ref(false)
const uploadingAvatar = ref(false)
const uploadingBackground = ref(false)

// 文件上传 refs
const avatarInputRef = ref(null)
const backgroundInputRef = ref(null)

// 编辑表单
const { cloned: editForm, sync } = useCloned(userProfile)

// 计算属性 - 头像 URL
const avatarUrl = computed(() => {
	if (isEditing.value && editForm.value.avatar) {
		return editForm.value.avatar
	}
	return userProfile.avatar || defaultAvatar
})

// 计算属性 - 背景图 URL
const backgroundUrl = computed(() => {
	if (isEditing.value && editForm.value.backgroundImg) {
		return editForm.value.backgroundImg
	}
	return userProfile.backgroundImg || defaultBackground
})

// 计算属性 - 显示昵称
const displayNickname = computed(() => {
	if (isEditing.value && editForm.value.nickname) {
		return editForm.value.nickname
	}
	return userProfile.nickname || '用户昵称'
})

// 验证规则
const rules = {
	required: value => !!value || '此字段不能为空',
	nickname: value => {
		if (!value) return true
		if (value.length < 2) return '昵称至少2个字符'
		if (value.length > 20) return '昵称最多20个字符'
		return true
	},
	introduction: value => {
		if (!value) return true
		if (value.length > 200) return '简介最多200个字符'
		return true
	},
}

// 格式化性别
const formatSex = sex => {
	const sexMap = { 0: '保密', 1: '男', 2: '女' }
	return sexMap[sex] || '未设置'
}

// 格式化生日
const formatBirthday = birthday => {
	if (!birthday) return '未设置'
	const formatted = useDateFormat(birthday, 'YYYY年MM月DD日')
	return formatted.value
}

// 处理头像文件选择
const handleAvatarChange = async event => {
	const file = event.target.files?.[0]
	if (!file) return

	// 验证文件大小 (1MB)
	if (file.size > 1024 * 1024) {
		$notify.warning('头像文件大小不能超过 1MB')
		return
	}

	// 验证文件类型
	if (!file.type.startsWith('image/')) {
		$notify.warning('请选择图片文件')
		return
	}

	uploadingAvatar.value = true

	try {
		// 上传文件
		const formData = new FormData()
		formData.append('file', file)

		const response = await uploadFile(formData)

		if (!response.success) {
			throw new Error(response.message || '上传失败')
		}

		// 获取文件 URL
		const fileUrl = response.data.url
		editForm.value.avatar = fileUrl

		$notify.success('头像上传成功')
	} catch (error) {
		console.error('上传头像失败:', error)
		$notify.error(error.message || '上传头像失败，请重试')
	} finally {
		uploadingAvatar.value = false
		// 清空 input，允许重复选择同一文件
		if (avatarInputRef.value) {
			avatarInputRef.value.value = ''
		}
	}
}

// 处理背景图文件选择
const handleBackgroundChange = async event => {
	const file = event.target.files?.[0]
	if (!file) return

	// 验证文件大小 (5MB)
	if (file.size > 5 * 1024 * 1024) {
		$notify.warning('背景图文件大小不能超过 5MB')
		return
	}

	// 验证文件类型
	if (!file.type.startsWith('image/')) {
		$notify.warning('请选择图片文件')
		return
	}

	uploadingBackground.value = true

	try {
		// 上传文件
		const formData = new FormData()
		formData.append('file', file)

		const response = await uploadFile(formData)

		if (!response.success) {
			throw new Error(response.message || '上传失败')
		}

		// 获取文件 URL
		const fileUrl = response.data.url
		editForm.value.backgroundImg = fileUrl

		$notify.success('背景图上传成功')
	} catch (error) {
		console.error('上传背景图失败:', error)
		$notify.error(error.message || '上传背景图失败，请重试')
	} finally {
		uploadingBackground.value = false
		// 清空 input
		if (backgroundInputRef.value) {
			backgroundInputRef.value.value = ''
		}
	}
}

// 开始编辑
const startEdit = () => {
	isEditing.value = true
	sync() // 同步数据到编辑表单
}

// 取消编辑
const cancelEdit = () => {
	isEditing.value = false
	sync() // 重置编辑表单
}

// 保存个人信息
const saveProfile = async () => {
	// 验证表单
	// eslint-disable-next-line no-unsafe-optional-chaining
	const { valid } = await formRef.value?.validate()
	if (!valid) {
		$notify.warning('请检查表单填写是否正确')
		return
	}

	saving.value = true

	try {
		// 准备提交数据
		const submitData = {
			userId: userStore.userId,
			nickname: editForm.value.nickname,
			sex: editForm.value.sex,
			birthday: editForm.value.birthday,
			introduction: editForm.value.introduction,
		}

		// 如果头像有变化，添加头像 URL
		if (editForm.value.avatar !== userProfile.avatar) {
			submitData.avatar = editForm.value.avatar
		}

		// 如果背景图有变化，添加背景图 URL
		if (editForm.value.backgroundImg !== userProfile.backgroundImg) {
			submitData.backgroundImg = editForm.value.backgroundImg
		}

		// 调用更新接口
		const response = await updateUserProfile(submitData)

		if (!response.success) {
			throw new Error(response.message || '保存失败')
		}

		// 重新获取用户信息
		const profileRes = await getUserProfile(userStore.userId)

		if (profileRes.success) {
			// 更新本地数据
			Object.assign(userProfile, profileRes.data)
			// 更新 store
			userStore.setProfile(profileRes.data)
		}

		isEditing.value = false
		$notify.success('保存成功')
	} catch (error) {
		console.error('保存个人信息失败:', error)
		$notify.error(error.message || '保存失败，请重试')
	} finally {
		saving.value = false
	}
}

// 初始化用户信息
const initProfile = async () => {
	try {
		// 先从 store 获取
		if (userStore.profile && Object.keys(userStore.profile).length > 0) {
			Object.assign(userProfile, userStore.profile)
			return
		}

		// 如果 store 为空，从后端获取
		if (!userStore.userId) {
			$notify.warning('用户未登录')
			return
		}

		const response = await getUserProfile(userStore.userId)

		if (response.success) {
			Object.assign(userProfile, response.data)
			userStore.setProfile(response.data)
		} else {
			throw new Error(response.message || '获取用户信息失败')
		}
	} catch (error) {
		console.error('加载用户信息失败:', error)
		$notify.error(error.message || '加载用户信息失败')
	}
}

// 组件挂载时初始化
onMounted(() => {
	initProfile()
})
</script>

<style scoped>
/* 头部区域 */
.profile-header {
	position: relative;
	width: 100%;
	max-width: 960px;
	margin: 0 auto;
}

.background-wrapper {
	position: relative;
	width: 100%;
}

.background-image {
	width: 100%;
	position: relative;
}

.background-content {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	pointer-events: none;
	z-index: 1;
}

.background-edit-btn {
	position: absolute;
	top: 16px;
	right: 16px;
	z-index: 100;
	pointer-events: auto;
}

.edit-btn {
	backdrop-filter: blur(8px);
	background-color: rgba(255, 255, 255, 0.95) !important;
	transition: all 0.3s ease;
}

.edit-btn:hover {
	transform: scale(1.1);
	background-color: rgba(255, 255, 255, 1) !important;
}

/* 渐入渐出动画 */
.fade-enter-active,
.fade-leave-active {
	transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
	opacity: 0;
}

/* 头像容器 */
.avatar-container {
	position: relative;
	margin-top: -80px;
	z-index: 2;
}

.avatar-wrapper {
	display: flex;
	align-items: flex-end;
	gap: 24px;
}

.nickname-wrapper {
	padding-bottom: 12px;
}

.nickname {
	text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

/* 头像上传 */
.avatar-uploader {
	position: relative;
}

.avatar-main {
	border: 4px solid white;
	box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.avatar-editing {
	cursor: pointer;
}

.avatar-editing:hover {
	transform: scale(1.05);
	box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
}

.avatar-overlay {
	background-color: rgba(0, 0, 0, 0.5);
	cursor: pointer;
	transition: background-color 0.3s;
}

.avatar-overlay:hover {
	background-color: rgba(0, 0, 0, 0.7);
}

.avatar-edit-content {
	display: flex;
	flex-direction: column;
	align-items: center;
	color: white;
	text-align: center;
}

/* 信息字段 */
.info-field {
	margin-bottom: 8px;
}

.field-label {
	display: flex;
	align-items: center;
	margin-bottom: 8px;
	color: rgb(var(--v-theme-on-surface));
}

.field-value {
	padding: 12px 16px;
	background-color: rgb(var(--v-theme-surface-variant));
	border-radius: 8px;
	color: rgb(var(--v-theme-on-surface));
	min-height: 48px;
	display: flex;
	align-items: center;
}

.field-value.multiline {
	align-items: flex-start;
	white-space: pre-wrap;
	word-break: break-word;
}

.profile-content {
	margin-top: 24px;
}
</style>
