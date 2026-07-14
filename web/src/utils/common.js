export function getInitials(name) {
	if (!name) return '?'
	const trimmed = name.trim()
	if (trimmed.length === 0) return '?'

	// 如果是中文名，取第一个字
	if (/[\u4e00-\u9fa5]/.test(trimmed)) {
		return trimmed.charAt(0)
	}

	// 英文名取首字母
	const words = trimmed.split(/\s+/)
	if (words.length === 1) {
		return words[0].charAt(0).toUpperCase()
	}

	return (words[0].charAt(0) + words[words.length - 1].charAt(0)).toUpperCase()
}
