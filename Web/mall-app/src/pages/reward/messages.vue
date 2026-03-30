<template>
	<view class="messages-container">
		<scroll-view
			class="msg-list"
			scroll-y
			:scroll-into-view="scrollTargetId"
			@scrolltoupper="loadOlderMessages"
		>
			<view v-if="loadingOlder" class="loading-tip">
				<text>加载中...</text>
			</view>
			<view v-if="!hasMore && messages.length > 0" class="loading-tip">
				<text>没有更多消息了</text>
			</view>
			<view
				v-for="item in messages"
				:key="item.id"
				:id="'msg-' + item.id"
				class="msg-row"
				:class="{ self: isSelf(item), system: isSystem(item) }"
			>
				<view v-if="isSystem(item)" class="system-bubble">
					<text>{{ item.content }}</text>
				</view>
				<template v-else>
					<view class="msg-avatar">{{ (item.senderNickname || item.senderRole || '用').slice(0, 1) }}</view>
					<view class="msg-body">
						<view class="msg-meta">
							<text>{{ item.senderNickname || item.senderRole || '用户' }}</text>
							<text class="meta-time">{{ formatTime(item.createTime) }}</text>
						</view>
						<view class="msg-bubble" :class="{ media: isImageMsg(item) }">
							<image
								v-if="isImageMsg(item)"
								:src="resolveImageUrl(item)"
								class="msg-image"
								mode="widthFix"
								@click="previewMsgImage(item)"
							/>
							<text v-else>{{ item.content }}</text>
						</view>
					</view>
				</template>
			</view>
		</scroll-view>

		<view class="input-bar">
			<button class="media-btn" :disabled="mediaUploading" @click="chooseImage">+</button>
			<textarea
				v-model="messageContent"
				class="msg-textarea"
				maxlength="500"
				auto-height
				placeholder="输入消息..."
				:adjust-position="true"
				confirm-type="send"
				@confirm="sendMessage"
			/>
			<button class="send-btn" :disabled="mediaUploading" @click="sendMessage">
				{{ mediaUploading ? '上传' : '发送' }}
			</button>
		</view>
	</view>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { getApiBaseUrl } from '../../utils/apiBase'
import { sendBountyMessageApi, getBountyMessagePageApi, uploadBountyAttachmentApi } from '../../api/reward'

const taskId = ref(null)
const myId = ref(String((uni.getStorageSync('user_profile') || {}).id || ''))
const messages = ref([])
const messageContent = ref('')
const scrollTargetId = ref('')
const mediaUploading = ref(false)

const pageNum = ref(1)
const pageSize = 20
const hasMore = ref(true)
const loadingOlder = ref(false)

const socketTask = ref(null)
const socketConnected = ref(false)
const socketManualClose = ref(false)
const reconnectTimer = ref(null)

const isSelf = (item) => String(item.senderId) === myId.value
const isSystem = (item) => Number(item.messageType) === 3 || item.senderRole === 'SYSTEM'
const isImageMsg = (item) => Number(item.messageType) === 2

const resolveImageUrl = (item) => {
	if (item.attachments) {
		if (typeof item.attachments === 'string') return item.attachments
		if (Array.isArray(item.attachments) && item.attachments.length) return item.attachments[0]
	}
	return item.content || ''
}

const formatTime = (time) => {
	if (!time) return ''
	return String(time).substring(5, 16).replace('T', ' ')
}

const scrollToBottom = () => {
	if (messages.value.length > 0) {
		const last = messages.value[messages.value.length - 1]
		nextTick(() => { scrollTargetId.value = 'msg-' + last.id })
	}
}

const loadMessages = async (append = false) => {
	try {
		const data = await getBountyMessagePageApi(taskId.value, pageNum.value, pageSize)
		const records = data?.records || []
		if (append) {
			const existIds = new Set(messages.value.map(m => String(m.id)))
			const newMsgs = records.filter(r => !existIds.has(String(r.id)))
			messages.value = [...newMsgs, ...messages.value]
		} else {
			messages.value = records
			nextTick(scrollToBottom)
		}
		hasMore.value = records.length >= pageSize
	} catch (error) {
		uni.showToast({ title: error?.message || '加载消息失败', icon: 'none' })
	}
}

const loadOlderMessages = async () => {
	if (!hasMore.value || loadingOlder.value) return
	loadingOlder.value = true
	pageNum.value++
	await loadMessages(true)
	loadingOlder.value = false
}

const sendMessage = async () => {
	const text = messageContent.value.trim()
	if (!text) return
	messageContent.value = ''
	try {
		await sendBountyMessageApi({
			taskId: taskId.value,
			content: text,
			messageType: 1
		})
		if (!socketConnected.value) {
			pageNum.value = 1
			await loadMessages()
		}
	} catch (error) {
		uni.showToast({ title: error?.message || '发送失败', icon: 'none' })
	}
}

const chooseImage = () => {
	uni.chooseImage({
		count: 1,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: async (res) => {
			const path = res.tempFilePaths[0]
			if (!path) return
			mediaUploading.value = true
			try {
				const url = await uploadBountyAttachmentApi(path, 'postImg')
				await sendBountyMessageApi({
					taskId: taskId.value,
					content: '[图片]',
					messageType: 2,
					attachments: url
				})
				if (!socketConnected.value) {
					pageNum.value = 1
					await loadMessages()
				}
			} catch (error) {
				uni.showToast({ title: error?.message || '发送图片失败', icon: 'none' })
			} finally {
				mediaUploading.value = false
			}
		}
	})
}

const previewMsgImage = (item) => {
	const url = resolveImageUrl(item)
	if (url) {
		uni.previewImage({ urls: [url] })
	}
}

// WebSocket
const buildWsUrl = () => {
	const token = uni.getStorageSync('token') || ''
	const base = String(getApiBaseUrl() || '').trim().replace(/\/+$/, '')
	const wsBase = base.startsWith('https://') ? base.replace('https://', 'wss://') : base.replace('http://', 'ws://')
	return `${wsBase}/ws/bounty/message?taskId=${encodeURIComponent(String(taskId.value))}&token=${encodeURIComponent(token)}`
}

const connectSocket = () => {
	if (!taskId.value) return
	closeSocket()
	socketManualClose.value = false
	const task = uni.connectSocket({ url: buildWsUrl(), complete: () => {} })
	task.onOpen(() => { socketConnected.value = true })
	task.onMessage((res) => {
		try {
			const payload = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
			const incoming = payload?.message || payload
			if (!incoming?.id) return
			if (messages.value.some(m => String(m.id) === String(incoming.id))) return
			messages.value.push(incoming)
			nextTick(scrollToBottom)
		} catch (_) {}
	})
	task.onClose(() => {
		socketConnected.value = false
		if (!socketManualClose.value) {
			reconnectTimer.value = setTimeout(connectSocket, 2500)
		}
	})
	socketTask.value = task
}

const closeSocket = () => {
	socketManualClose.value = true
	if (reconnectTimer.value) {
		clearTimeout(reconnectTimer.value)
		reconnectTimer.value = null
	}
	if (socketTask.value) {
		try { socketTask.value.close() } catch (_) {}
		socketTask.value = null
	}
	socketConnected.value = false
}

onLoad((options) => {
	myId.value = String((uni.getStorageSync('user_profile') || {}).id || '')
	if (options?.taskId) {
		taskId.value = options.taskId
		loadMessages()
		connectSocket()
	}
})

onUnload(() => {
	closeSocket()
})
</script>

<style scoped lang="scss">
$primary: #00bfff;
$light: #5ce1ff;
$deep: #0099cc;
$bg: #f8f8f8;
$card: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$text-muted: #8a9aaa;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.messages-container {
	display: flex;
	flex-direction: column;
	height: 100vh;
	background-color: $bg;
}

.msg-list {
	flex: 1;
	padding: 24rpx 32rpx;
	padding-bottom: 160rpx;
}

.loading-tip {
	text-align: center;
	padding: 20rpx 0;
	text { font-size: 22rpx; color: $text-muted; }
}

.msg-row {
	display: flex;
	gap: 16rpx;
	margin-bottom: 28rpx;
	animation: fadeInUp 0.3s ease-out both;
	&.self {
		flex-direction: row-reverse;
		.msg-meta { text-align: right; }
		.msg-bubble {
			background: $gradient;
			color: #fff;
			box-shadow: 0 4rpx 16rpx rgba(0,191,255,0.15);
			text { color: #fff; }
		}
	}
	&.system {
		justify-content: center;
	}
}

.system-bubble {
	background-color: rgba(0, 191, 255, 0.06);
	border-radius: 999rpx;
	padding: 10rpx 28rpx;
	text { font-size: 22rpx; color: $text-muted; }
}

.msg-avatar {
	width: 68rpx;
	height: 68rpx;
	border-radius: 50%;
	background: $gradient;
	display: flex;
	align-items: center;
	justify-content: center;
	color: #fff;
	font-size: 28rpx;
	font-weight: 700;
	flex-shrink: 0;
	box-shadow: 0 4rpx 12rpx rgba(0,191,255,0.15);
}

.msg-body {
	max-width: 65%;
}

.msg-meta {
	font-size: 22rpx;
	color: $text-muted;
	margin-bottom: 8rpx;
	.meta-time { margin-left: 12rpx; }
}

.msg-bubble {
	display: inline-block;
	padding: 20rpx 28rpx;
	border-radius: 24rpx;
	background-color: $card;
	box-shadow: $shadow-card;
	text { font-size: 28rpx; color: $text-primary; line-height: 1.5; }
	&.media { padding: 8rpx; }
}

.msg-image {
	width: 320rpx;
	border-radius: 20rpx;
	opacity: 0;
	animation: fadeInUp 0.3s ease-out 0.1s forwards;
}

.input-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	display: flex;
	align-items: flex-end;
	gap: 16rpx;
	padding: 16rpx 28rpx;
	padding-bottom: calc(env(safe-area-inset-bottom) + 16rpx);
	background: rgba(255,255,255,0.72);
	backdrop-filter: blur(24px);
	box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.06);
}

.media-btn {
	width: 76rpx;
	height: 76rpx;
	border-radius: 50%;
	background-color: rgba(0, 191, 255, 0.08);
	color: $primary;
	font-size: 38rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	padding: 0;
	&:active { transform: scale(0.96); }
	&[disabled] { opacity: 0.4; }
}

.msg-textarea {
	flex: 1;
	min-height: 76rpx;
	max-height: 200rpx;
	padding: 18rpx 24rpx;
	font-size: 28rpx;
	color: $text-primary;
	background-color: $bg;
	border-radius: 999rpx;
}

.send-btn {
	width: 128rpx;
	height: 76rpx;
	border-radius: 999rpx;
	background: $gradient;
	color: #fff;
	font-size: 28rpx;
	font-weight: 500;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	padding: 0;
	&:active { transform: scale(0.96); }
	&[disabled] { opacity: 0.4; }
}
</style>
