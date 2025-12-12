<template>
	<v-container class="pa-0 justify-space-between align-center">
		<!-- 头部区域 -->
		<ProfileHeader
			:background-img="userProfile.background_img"
			:avatar="userProfile.avatar"
			:nickname="userProfile.nickname"
			:is-editing="isEditing"
			@update:background-img="handleUpdateBackgroundImg"
			@update:avatar="handleUpdateAvatar"
		/>

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
							<ProfileInfoEditor
								v-model="editForm"
								:is-editing="isEditing"
								:original-data="userProfile"
							/>
						</v-card-text>
					</v-card>
				</v-col>
			</v-row>
		</v-container>
	</v-container>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useCloned } from '@vueuse/core'
import ProfileHeader from '../components/ProfileHeader.vue'
import ProfileInfoEditor from '../components/ProfileInfoEditor.vue'
import { $notify } from '@/plugins/notification'

// 导入默认图片
import defaultAvatar from '@/assets/image/default-avatar.png'
import defaultBackground from '@/assets/image/default-background.jpg'

// 用户信息
const userProfile = reactive({
	nickname: '用户昵称',
	avatar: defaultAvatar, // 使用导入的图片
	birthday: '2000-01-01',
	background_img: defaultBackground, // 使用导入的图片
	introduction: '这是一段个人简介...',
})

// 编辑状态
const isEditing = ref(false)
const saving = ref(false)

// 编辑表单
const { cloned: editForm, sync } = useCloned(userProfile)

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

// 更新背景图
const handleUpdateBackgroundImg = url => {
	if (isEditing.value) {
		editForm.value.background_img = url
	}
}

// 更新头像
const handleUpdateAvatar = url => {
	if (isEditing.value) {
		editForm.value.avatar = url
	}
}

// 保存个人信息
const saveProfile = async () => {
	saving.value = true
	try {
		// TODO: 调用后端API保存用户信息
		// await api.updateUserProfile(editForm.value)

		console.log('保存个人信息:', editForm.value)

		// 模拟API调用
		await new Promise(resolve => setTimeout(resolve, 1000))

		// 更新本地数据
		Object.assign(userProfile, editForm.value)

		isEditing.value = false
		$notify.success('保存成功')
	} catch (error) {
		$notify.error('保存失败，请重试')
	} finally {
		saving.value = false
	}
}

// TODO: 初始化时从后端加载用户信息
// onMounted(async () => {
//   try {
//     const data = await api.getUserProfile()
//     Object.assign(userProfile, data)
//   } catch (error) {
//     $notify.error('加载用户信息失败')
//   }
// })
</script>

<style scoped></style>
