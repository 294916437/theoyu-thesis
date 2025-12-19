import { ref } from 'vue'

// 模拟API延迟
const delay = ms => new Promise(resolve => setTimeout(resolve, ms))

export function useMeetingApi() {
	const loading = ref(false)
	const error = ref(null)

	/**
	 * 获取即将开始的会议
	 */
	const fetchUpcomingMeetings = async () => {
		loading.value = true
		error.value = null

		try {
			await delay(500) // 模拟网络延迟

			// 预留API调用位置
			// const response = await fetch('/api/meetings/upcoming')
			// return await response.json()

			// 模拟数据
			return [
				{
					id: '1',
					title: '项目进度会议',
					startTime: new Date(Date.now() + 3600000).toISOString(),
					duration: 60,
					participantCount: 5,
				},
				{
					id: '2',
					title: '技术讨论',
					startTime: new Date(Date.now() + 7200000).toISOString(),
					duration: 90,
					participantCount: 8,
				},
			]
		} catch (err) {
			error.value = err
			throw err
		} finally {
			loading.value = false
		}
	}

	/**
	 * 获取最近的会议
	 */
	const fetchRecentMeetings = async () => {
		loading.value = true
		error.value = null

		try {
			await delay(500)

			// 预留API调用位置
			// const response = await fetch('/api/meetings/recent')
			// return await response.json()

			return [
				{
					id: '3',
					title: '团队周会',
					startTime: new Date(Date.now() - 86400000).toISOString(),
					duration: 45,
					status: 'completed',
					participantCount: 12,
					color: 'primary',
				},
				{
					id: '4',
					title: '客户演示',
					startTime: new Date(Date.now() - 172800000).toISOString(),
					duration: 120,
					status: 'completed',
					participantCount: 6,
					color: 'success',
				},
			]
		} catch (err) {
			error.value = err
			throw err
		} finally {
			loading.value = false
		}
	}

	/**
	 * 创建会议
	 */
	const createMeeting = async meetingData => {
		loading.value = true
		error.value = null

		try {
			await delay(800)

			// 预留API调用位置
			// const response = await fetch('/api/meetings', {
			//   method: 'POST',
			//   headers: { 'Content-Type': 'application/json' },
			//   body: JSON.stringify(meetingData)
			// })
			// return await response.json()

			return {
				id: `meeting-${Date.now()}`,
				...meetingData,
			}
		} catch (err) {
			error.value = err
			throw err
		} finally {
			loading.value = false
		}
	}

	/**
	 * 获取会议详情
	 */
	const fetchMeetingDetail = async meetingId => {
		loading.value = true
		error.value = null

		try {
			await delay(600)

			// 预留API调用位置
			// const response = await fetch(`/api/meetings/${meetingId}`)
			// return await response.json()

			return {
				meeting: {
					id: meetingId,
					title: '项目讨论会议',
					description: '讨论项目下一阶段的计划和任务分配',
					startTime: new Date().toISOString(),
					duration: 60,
					status: 'scheduled',
					host: { id: 'user-1', name: '张三', email: 'zhangsan@example.com' },
					participantCount: 8,
				},
				participants: [
					{
						id: 'user-1',
						name: '张三',
						email: 'zhangsan@example.com',
						isHost: true,
						joinTime: new Date(Date.now() - 600000).toISOString(),
						avatarColor: 'primary',
					},
					{
						id: 'user-2',
						name: '李四',
						email: 'lisi@example.com',
						isHost: false,
						joinTime: new Date(Date.now() - 300000).toISOString(),
						avatarColor: 'success',
					},
				],
				relatedMeetings: [
					{
						id: 'related-1',
						title: '上周项目会议',
						startTime: new Date(Date.now() - 604800000).toISOString(),
					},
				],
				statistics: {
					avgDuration: 45,
					maxParticipants: 12,
					messageCount: 156,
					screenShareTime: 15,
					participation: [65, 78, 82, 75, 88, 92, 85, 79, 86, 90],
					devices: [
						{ type: 'PC端', percentage: 65, color: 'primary' },
						{ type: '移动端', percentage: 25, color: 'success' },
						{ type: '平板', percentage: 10, color: 'info' },
					],
				},
				hasRecording: true,
				hasTranscript: true,
				recordingSize: 245000000,
			}
		} catch (err) {
			error.value = err
			throw err
		} finally {
			loading.value = false
		}
	}

	/**
	 * 更新会议
	 */
	const updateMeeting = async (meetingId, meetingData) => {
		loading.value = true
		error.value = null

		try {
			await delay(800)

			// 预留API调用位置
			// const response = await fetch(`/api/meetings/${meetingId}`, {
			//   method: 'PUT',
			//   headers: { 'Content-Type': 'application/json' },
			//   body: JSON.stringify(meetingData)
			// })
			// return await response.json()

			return {
				id: meetingId,
				...meetingData,
			}
		} catch (err) {
			error.value = err
			throw err
		} finally {
			loading.value = false
		}
	}

	/**
	 * 删除会议
	 */
	const deleteMeeting = async meetingId => {
		loading.value = true
		error.value = null

		try {
			await delay(500)

			// 预留API调用位置
			// await fetch(`/api/meetings/${meetingId}`, { method: 'DELETE' })

			return true
		} catch (err) {
			error.value = err
			throw err
		} finally {
			loading.value = false
		}
	}

	return {
		loading,
		error,
		fetchUpcomingMeetings,
		fetchRecentMeetings,
		createMeeting,
		fetchMeetingDetail,
		updateMeeting,
		deleteMeeting,
	}
}
