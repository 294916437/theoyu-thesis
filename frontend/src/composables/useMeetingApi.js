import { ref } from 'vue'
import axios from '@/utils/axios'

export function useMeetingApi() {
	const loading = ref(false)
	const error = ref(null)

	/**
	 * 验证会议号
	 */
	async function validateMeetingNo(meetingNo) {
		loading.value = true
		error.value = null

		try {
			console.log('Validating meeting number', meetingNo)

			const response = await axios.post('/api/media/room/validate-no', {
				meetingNo: meetingNo.trim(),
			})

			console.log('Meeting validation response', response.data)

			return response.data.data
		} catch (err) {
			console.error('Failed to validate meeting number', err)
			error.value = err
			throw err
		} finally {
			loading.value = false
		}
	}

	/**
	 * 获取会议详情
	 */
	async function fetchMeetingDetail(roomId) {
		loading.value = true
		error.value = null

		try {
			// console.log('Fetching meeting detail', roomId)

			// const response = await axios.get(`/api/media/room/${roomId}`)

			// console.log('Meeting detail response', response.data)

			// return response.data.data
			return {
				meeting: {
					id: roomId,
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
			console.error('Failed to fetch meeting detail', err)
			error.value = err
			throw err
		} finally {
			loading.value = false
		}
	}

	/**
	 * 创建会议
	 */
	async function createMeeting(meetingData) {
		loading.value = true
		error.value = null

		try {
			console.log('Creating meeting', meetingData)

			const response = await axios.post('/api/media/room/create', meetingData)

			console.log('Meeting created', response.data)

			return response.data.data
		} catch (err) {
			console.error('Failed to create meeting', err)
			error.value = err
			throw err
		} finally {
			loading.value = false
		}
	}

	/**
	 * 加入会议 (预验证)
	 */
	async function joinMeeting(joinData) {
		loading.value = true
		error.value = null

		try {
			console.log('Joining meeting', joinData)

			const response = await axios.post('/api/media/room/join', joinData)

			console.log('Join meeting response', response.data)

			return response.data.data
		} catch (err) {
			console.error('Failed to join meeting', err)
			error.value = err
			throw err
		} finally {
			loading.value = false
		}
	}

	/**
	 * 获取即将开始的会议
	 */
	async function fetchUpcomingMeetings() {
		loading.value = true
		error.value = null

		try {
			// console.log('Fetching upcoming meetings')

			// const response = await axios.get('/api/media/room/upcoming', {
			// 	params: {
			// 		page: 1,
			// 		size: 5,
			// 	},
			// })

			// console.log('Upcoming meetings response', response.data)

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

			// return response.data.data || []
		} catch (err) {
			console.error('Failed to fetch upcoming meetings', err)
			error.value = err
			return []
		} finally {
			loading.value = false
		}
	}

	/**
	 * 获取最近的会议
	 */
	async function fetchRecentMeetings() {
		loading.value = true
		error.value = null

		try {
			// console.log('Fetching recent meetings')

			// const response = await axios.get('/api/media/room/recent', {
			// 	params: {
			// 		page: 1,
			// 		size: 10,
			// 	},
			// })

			// console.log('Recent meetings response', response.data)

			// return response.data.data || []
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
			console.error('Failed to fetch recent meetings', err)
			error.value = err
			return []
		} finally {
			loading.value = false
		}
	}

	/**
	 * 更新会议
	 */
	async function updateMeeting(roomId, meetingData) {
		loading.value = true
		error.value = null

		try {
			console.log('Updating meeting', roomId, meetingData)

			const response = await axios.put(`/api/media/room/${roomId}`, meetingData)

			console.log('Meeting updated', response.data)

			return response.data.data
		} catch (err) {
			console.error('Failed to update meeting', err)
			error.value = err
			throw err
		} finally {
			loading.value = false
		}
	}

	/**
	 * 删除会议
	 */
	async function deleteMeeting(roomId) {
		loading.value = true
		error.value = null

		try {
			console.log('Deleting meeting', roomId)

			await axios.delete(`/api/media/room/${roomId}`)

			console.log('Meeting deleted')
		} catch (err) {
			console.error('Failed to delete meeting', err)
			error.value = err
			throw err
		} finally {
			loading.value = false
		}
	}

	return {
		loading,
		error,
		validateMeetingNo,
		fetchMeetingDetail,
		createMeeting,
		joinMeeting,
		fetchUpcomingMeetings,
		fetchRecentMeetings,
		updateMeeting,
		deleteMeeting,
	}
}
