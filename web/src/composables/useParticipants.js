import { ref, computed, watch } from 'vue'
import { fetchParticipantsList } from '@/api/room'
import { useIntervalFn } from '@vueuse/core'

export function useParticipants(roomId, sfuParticipants) {
	// 后端参与者数据缓存
	const backendParticipants = ref([])
	const loading = ref(false)
	const error = ref(null)

	/**
	 * 合并 SFU 实时数据和后端业务数据
	 */
	const mergedParticipants = computed(() => {
		const merged = []
		const backendMap = new Map(backendParticipants.value.map(p => [p.userId, p]))

		// 1. 遍历 SFU 在线用户（实时数据优先）
		sfuParticipants.value.forEach(sfuUser => {
			const backendData = backendMap.get(sfuUser.userId)

			merged.push({
				// SFU 实时数据
				...sfuUser,

				// 后端补充数据
				avatar: backendData?.avatar || '',
				role: backendData?.role || 1,
				status: 1, // SFU 中存在即为在线
				joinedAt: backendData?.joinedAt || new Date().toISOString(),
				leftAt: null,

				// 计算字段
				audioEnabled: !sfuUser.producers?.audio?.paused,
				videoEnabled: !sfuUser.producers?.video?.paused,
				connectionQuality: 'good', // 可从 stats 计算
				isSpeaking: false, // 需要音量检测
				handRaised: false, // 需要信令支持
			})

			// 标记已处理
			backendMap.delete(sfuUser.userId)
		})

		// 2. 添加已离线但后端有记录的用户（可选，用于历史展示）
		// backendMap.forEach(backendUser => {
		// 	if (backendUser.status === 1) { // 仅显示离线用户
		// 		merged.push({
		// 			userId: backendUser.userId,
		// 			username: backendUser.userName,
		// 			avatar: backendUser.avatar,
		// 			role: backendUser.role,
		// 			status: 2,
		// 			joinedAt: backendUser.joinedAt,
		// 			leftAt: backendUser.leftAt,
		// 			// 离线用户无实时数据
		// 			streams: {},
		// 			producers: {},
		// 			audioEnabled: false,
		// 			videoEnabled: false,
		// 		})
		// 	}
		// })

		return merged
	})

	/**
	 * 获取在线参与者
	 */
	const onlineParticipants = computed(() => mergedParticipants.value.filter(p => p.status === 1))
	/**
	 * 从后端加载参与者列表
	 */
	const loadParticipants = async (status = 1, page = 1, size = 100) => {
		if (!roomId.value) return

		try {
			loading.value = true
			error.value = null

			const { data } = await fetchParticipantsList(roomId.value, status, page, size)

			backendParticipants.value = data.map(p => ({
				userId: p.userId,
				userName: p.userName,
				avatar: p.avatar,
				role: p.role,
				status: p.status,
				audioMuted: p.audioMuted,
				videoMuted: p.videoMuted,
				joinedAt: p.joinedAt,
				leftAt: p.leftAt,
			}))
		} catch (err) {
			console.error('Failed to load participants:', err)
			error.value = err
		} finally {
			loading.value = false
		}
	}

	/**
	 * 定时刷新后端数据（30秒）
	 */
	const { pause: stopPolling, resume: startPolling } = useIntervalFn(
		() => loadParticipants(1), // 仅拉取在线用户
		30000,
		{ immediate: false },
	)

	/**
	 * 监听房间 ID 变化，自动加载
	 */
	watch(
		roomId,
		newRoomId => {
			if (newRoomId) {
				loadParticipants(1)
				startPolling()
			} else {
				stopPolling()
			}
		},
		{ immediate: true },
	)

	return {
		// 数据
		mergedParticipants,
		onlineParticipants,
		loading,
		error,

		// 方法
		loadParticipants,
		startPolling,
		stopPolling,
	}
}
