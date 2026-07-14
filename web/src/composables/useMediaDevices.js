import { ref, computed } from 'vue'
import { usePermission } from '@vueuse/core'

export function useMediaDevices() {
	const cameras = ref([])
	const microphones = ref([])
	const speakers = ref([])

	const selectedCamera = ref('')
	const selectedMicrophone = ref('')
	const selectedSpeaker = ref('')

	const cameraPermission = usePermission('camera')
	const microphonePermission = usePermission('microphone')

	const hasCamera = computed(() => cameras.value.length > 0)
	const hasMicrophone = computed(() => microphones.value.length > 0)
	const hasSpeaker = computed(() => speakers.value.length > 0)

	const permissionsGranted = computed(() => {
		return cameraPermission.value === 'granted' && microphonePermission.value === 'granted'
	})

	/**
	 * 枚举所有媒体设备
	 */
	const enumerateDevices = async () => {
		if (!navigator.mediaDevices) {
			throw new Error('当前页面不在安全上下文中（需要 HTTPS 或 localhost），无法访问媒体设备')
		}
		try {
			const devices = await navigator.mediaDevices.enumerateDevices()

			cameras.value = devices
				.filter(device => device.kind === 'videoinput')
				.map(device => ({
					deviceId: device.deviceId,
					label: device.label || `摄像头 ${cameras.value.length + 1}`,
				}))

			microphones.value = devices
				.filter(device => device.kind === 'audioinput')
				.map(device => ({
					deviceId: device.deviceId,
					label: device.label || `麦克风 ${microphones.value.length + 1}`,
				}))

			speakers.value = devices
				.filter(device => device.kind === 'audiooutput')
				.map(device => ({
					deviceId: device.deviceId,
					label: device.label || `扬声器 ${speakers.value.length + 1}`,
				}))

			// 设置默认设备
			if (cameras.value.length > 0 && !selectedCamera.value) {
				selectedCamera.value = cameras.value[0].deviceId
			}

			if (microphones.value.length > 0 && !selectedMicrophone.value) {
				selectedMicrophone.value = microphones.value[0].deviceId
			}

			if (speakers.value.length > 0 && !selectedSpeaker.value) {
				selectedSpeaker.value = speakers.value[0].deviceId
			}

			return { cameras: cameras.value, microphones: microphones.value, speakers: speakers.value }
		} catch (error) {
			console.error('Failed to enumerate devices:', error)
			throw error
		}
	}

	/**
	 * 请求媒体权限
	 */
	const requestPermissions = async () => {
		if (!navigator.mediaDevices) {
			console.error('当前页面不在安全上下文中（需要 HTTPS 或 localhost），无法请求媒体权限')
			return false
		}
		try {
			const stream = await navigator.mediaDevices.getUserMedia({
				video: true,
				audio: true,
			})

			// 停止所有轨道
			stream.getTracks().forEach(track => track.stop())

			// 重新枚举设备以获取标签
			await enumerateDevices()

			return true
		} catch (error) {
			console.error('Failed to request permissions:', error)
			return false
		}
	}

	/**
	 * 获取用户媒体流
	 */
	const getUserMedia = async (constraints = {}) => {
		const defaultConstraints = {
			video: selectedCamera.value ? { deviceId: { exact: selectedCamera.value } } : true,
			audio: selectedMicrophone.value ? { deviceId: { exact: selectedMicrophone.value } } : true,
		}

		try {
			const stream = await navigator.mediaDevices.getUserMedia({
				...defaultConstraints,
				...constraints,
			})

			return stream
		} catch (error) {
			console.error('Failed to get user media:', error)
			throw error
		}
	}

	/**
	 * 切换设备
	 */
	const switchDevice = async (deviceId, kind) => {
		try {
			if (kind === 'videoinput') {
				selectedCamera.value = deviceId
			} else if (kind === 'audioinput') {
				selectedMicrophone.value = deviceId
			} else if (kind === 'audiooutput') {
				selectedSpeaker.value = deviceId
			}

			return true
		} catch (error) {
			console.error('Failed to switch device:', error)
			return false
		}
	}

	/**
	 * 测试音频输入
	 */
	const testMicrophone = async () => {
		try {
			const stream = await navigator.mediaDevices.getUserMedia({
				audio: { deviceId: { exact: selectedMicrophone.value } },
			})

			const audioContext = new AudioContext()
			const analyser = audioContext.createAnalyser()
			const source = audioContext.createMediaStreamSource(stream)

			source.connect(analyser)
			analyser.fftSize = 256

			const bufferLength = analyser.frequencyBinCount
			const dataArray = new Uint8Array(bufferLength)

			const getVolume = () => {
				analyser.getByteFrequencyData(dataArray)
				const average = dataArray.reduce((a, b) => a + b) / bufferLength
				return average
			}

			return {
				stream,
				getVolume,
				stop: () => {
					stream.getTracks().forEach(track => track.stop())
					audioContext.close()
				},
			}
		} catch (error) {
			console.error('Failed to test microphone:', error)
			throw error
		}
	}

	return {
		cameras,
		microphones,
		speakers,
		selectedCamera,
		selectedMicrophone,
		selectedSpeaker,
		hasCamera,
		hasMicrophone,
		hasSpeaker,
		permissionsGranted,
		enumerateDevices,
		requestPermissions,
		getUserMedia,
		switchDevice,
		testMicrophone,
	}
}
