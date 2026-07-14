import axios from '@/utils/axios'

// 接口前缀
const API_PREFIX = '/message'

// ==================== 会话相关接口 ====================

/**
 * 获取会话列表
 * @param {Object} [params] - 查询参数
 * @param {number} [params.userId] - 用户ID
 * @param {string|null} [params.cursor] - 游标（分页）
 * @returns {Promise<Object>}
 * @example
 */
export const fetchConversations = async (params = {}) => {
	const result = await axios.post(`${API_PREFIX}/conversation/list`, params)
	return result
}

/**
 * 获取会话详情
 * @param {number} id - 会话ID (必填)
 * @returns {Promise<Object>}
 * @example
 */
export const fetchConversationDetail = async id => {
	const result = await axios.get(`${API_PREFIX}/conversation/${id}`)
	return result
}

/**
 * 创建私聊会话
 * @param {Object} params - 创建参数
 * @param {number} params.targetUserId - 目标用户ID (必填)
 * @returns {Promise<Object>}
 * @example
 */
export const createConversation = async targetUserId => {
	const result = await axios.post(`${API_PREFIX}/conversation/create`, targetUserId)
	return result
}

/**
 * 退出/删除会话
 * @param {number} id - 会话ID (必填)
 * @returns {Promise<Object>}
 * @example
 */
export const leaveConversation = async id => {
	const result = await axios.put(`${API_PREFIX}/conversation/${id}/leave`)
	return result
}

// ==================== 消息相关接口 ====================

/**
 * 获取消息列表
 * @param {number} id - 会话ID (必填)
 * @param {Object} [params] - 查询参数
 * @param {string|null} [params.cursor] - 游标（分页）
 * @param {number} [params.limit] - 每页数量
 * @returns {Promise<Object>}
 * @example
 */
export const fetchConversationMessages = async (id, params = {}) => {
	const result = await axios.post(`${API_PREFIX}/message/${id}/list`, params)
	return result
}

/**
 * 通过会话发送消息
 * @param {number} id - 会话ID (必填)
 * @param {Object} params - 消息参数
 * @param {number} params.messageType - 消息类型: 1=文本, 2=图片, 3=视频, 4=文件
 * @param {string} [params.content] - 消息文本内容（文本消息必填）
 * @param {string[]} [params.imgUris] - 图片URL数组（图片消息必填）
 * @param {string} [params.videoUri] - 视频URL（视频消息必填）
 * @returns {Promise<Object>}
 */
export const sendConversationMessage = async (id, params = {}) => {
	const result = await axios.post(`${API_PREFIX}/message/${id}/send`, params)
	return result
}

// ==================== 便捷方法（基于上述接口封装） ====================

/**
 * 发送文本消息（便捷方法）
 * @param {number} conversationId - 会话ID
 * @param {string} content - 消息内容
 * @returns {Promise<Object>}
 * @example
 * await sendTextMessage(123, "你好！")
 */
export const sendTextMessage = async (conversationId, content) => {
	return sendConversationMessage(conversationId, {
		messageType: 1,
		content,
	})
}

/**
 * 发送图片消息（便捷方法）
 * @param {number} conversationId - 会话ID
 * @param {string[]} imgUris - 图片URL数组
 * @returns {Promise<Object>}
 * @example
 * await sendImageMessage(123, ["https://example.com/photo.jpg"])
 */
export const sendImageMessage = async (conversationId, imgUris) => {
	return sendConversationMessage(conversationId, {
		messageType: 2,
		imgUris,
	})
}

/**
 * 发送视频消息（便捷方法）
 * @param {number} conversationId - 会话ID
 * @param {string} videoUri - 视频URL
 * @returns {Promise<Object>}
 * @example
 * await sendVideoMessage(123, "https://example.com/video.mp4")
 */
export const sendVideoMessage = async (conversationId, videoUri) => {
	return sendConversationMessage(conversationId, {
		messageType: 3,
		videoUri,
	})
}

/**
 * 发送文件消息（便捷方法）
 * @param {number} conversationId - 会话ID
 * @param {string} fileUri - 文件URL
 * @param {string} [fileName] - 文件名
 * @returns {Promise<Object>}
 * @example
 * await sendFileMessage(123, "https://example.com/document.pdf", "报告.pdf")
 */
export const sendFileMessage = async (conversationId, fileUri, fileName) => {
	return sendConversationMessage(conversationId, {
		messageType: 4,
		fileUri,
		fileName,
	})
}

/**
 * 创建或获取私聊会话（便捷方法）
 * 如果会话已存在则返回现有会话，否则创建新会话
 * @param {number} targetUserId - 目标用户ID
 * @returns {Promise<Object>}
 * @example
 * const conversation = await getOrCreatePrivateConversation(456)
 */
export const getOrCreatePrivateConversation = async targetUserId => {
	const { data } = await axios.post(`${API_PREFIX}/conversation/create`, { targetUserId })
	return data
}
// ==================== 消息类型枚举 ====================

/**
 * 消息类型常量
 */
export const MESSAGE_TYPES = {
	TEXT: 1, // 文本消息
	IMAGE: 2, // 图片消息
	VIDEO: 3, // 视频消息
	FILE: 4, // 文件消息
}

/**
 * 获取消息类型名称
 * @param {number} type - 消息类型代码
 * @returns {string}
 */
export const getMessageTypeName = type => {
	const typeMap = {
		1: '文本',
		2: '图片',
		3: '视频',
		4: '文件',
	}
	return typeMap[type] || '未知'
}
