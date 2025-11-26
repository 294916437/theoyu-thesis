/**
 * 格式化时间为用户友好的格式
 * @param {string} dateTimeStr - 时间字符串，格式: "2025-11-04 17:29:47"
 * @returns {string} 格式化后的时间
 */
export function formatTime(dateTimeStr) {
	if (!dateTimeStr) return ''

	const messageTime = new Date(dateTimeStr)
	const now = new Date()

	// 计算时间差(毫秒)
	const diff = now - messageTime
	const diffMinutes = Math.floor(diff / (1000 * 60))
	const diffHours = Math.floor(diff / (1000 * 60 * 60))
	const diffDays = Math.floor(diff / (1000 * 60 * 60 * 24))

	// 刚刚 (1分钟内)
	if (diffMinutes < 1) {
		return '刚刚'
	}

	// N分钟前 (1小时内)
	if (diffMinutes < 60) {
		return `${diffMinutes}分钟前`
	}

	// N小时前 (24小时内)
	if (diffHours < 24) {
		return `${diffHours}小时前`
	}

	// 昨天 HH:mm
	if (diffDays === 1) {
		const hours = String(messageTime.getHours()).padStart(2, '0')
		const minutes = String(messageTime.getMinutes()).padStart(2, '0')
		return `昨天 ${hours}:${minutes}`
	}

	// 前天 HH:mm
	if (diffDays === 2) {
		const hours = String(messageTime.getHours()).padStart(2, '0')
		const minutes = String(messageTime.getMinutes()).padStart(2, '0')
		return `前天 ${hours}:${minutes}`
	}

	// 7天内显示星期
	if (diffDays < 7) {
		const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
		const hours = String(messageTime.getHours()).padStart(2, '0')
		const minutes = String(messageTime.getMinutes()).padStart(2, '0')
		return `${weekdays[messageTime.getDay()]} ${hours}:${minutes}`
	}

	// 今年内显示 MM-DD HH:mm
	if (messageTime.getFullYear() === now.getFullYear()) {
		const month = String(messageTime.getMonth() + 1).padStart(2, '0')
		const day = String(messageTime.getDate()).padStart(2, '0')
		const hours = String(messageTime.getHours()).padStart(2, '0')
		const minutes = String(messageTime.getMinutes()).padStart(2, '0')
		return `${month}-${day} ${hours}:${minutes}`
	}

	// 超过一年显示完整日期 YYYY-MM-DD HH:mm
	const year = messageTime.getFullYear()
	const month = String(messageTime.getMonth() + 1).padStart(2, '0')
	const day = String(messageTime.getDate()).padStart(2, '0')
	const hours = String(messageTime.getHours()).padStart(2, '0')
	const minutes = String(messageTime.getMinutes()).padStart(2, '0')
	return `${year}-${month}-${day} ${hours}:${minutes}`
}
