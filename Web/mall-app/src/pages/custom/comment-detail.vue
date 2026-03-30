<template>
	<view class="comment-detail-page" v-if="comment">
		<view class="comment-user-row">
			<image class="comment-avatar" :src="getCommentAvatar(comment)" mode="aspectFill" />
			<view class="comment-user-meta">
				<text class="comment-user">{{ getCommentDisplayName(comment) }}</text>
				<text class="comment-time">{{ formatCommentTime(comment.createTime) }}</text>
			</view>
		</view>

		<view class="score-card">
			<view class="score-item">
				<text class="score-label">模型评分</text>
				<text class="score-stars">{{ renderStars(comment.modelScore) }}</text>
				<text class="score-value">{{ formatScore(comment.modelScore) }}</text>
			</view>
			<view class="score-item">
				<text class="score-label">打印评分</text>
				<text class="score-stars">{{ renderStars(comment.printScore) }}</text>
				<text class="score-value">{{ formatScore(comment.printScore) }}</text>
			</view>
			<view class="score-item">
				<text class="score-label">服务评分</text>
				<text class="score-stars">{{ renderStars(comment.serviceScore) }}</text>
				<text class="score-value">{{ formatScore(comment.serviceScore) }}</text>
			</view>
			<view class="score-item total">
				<text class="score-label">综合评分</text>
				<text class="score-stars">{{ renderStars(comment.avgScore) }}</text>
				<text class="score-value">{{ formatScore(comment.avgScore) }}</text>
			</view>
		</view>

		<view class="comment-text">{{ comment.commentText || '用户未填写文字评价' }}</view>

		<view class="media-list" v-if="commentMediaList.length">
			<view class="media-item" v-for="(media, idx) in commentMediaList" :key="`${comment.id || 'detail'}-${idx}`">
				<image
					v-if="media.type === 'image'"
					:src="media.url"
					mode="aspectFill"
					class="media-thumb"
					@click="previewCommentImage(media.url)"
				/>
				<view v-else class="media-video-wrap">
					<video
						class="media-video"
						:src="media.url"
						controls
						show-fullscreen-btn
						preload="metadata"
					/>
					<view class="video-full-btn" @click="previewCommentVideo(media.url)">全屏观看</view>
				</view>
			</view>
		</view>

		<view class="reply-card">
			<view class="reply-title-row">
				<text class="reply-title">追评互动</text>
				<text class="reply-total">{{ replyTotal }}条</text>
			</view>
			<view class="reply-list" v-if="replies.length">
				<view class="reply-item" v-for="item in replies" :key="item.id">
					<view class="reply-user-row">
						<image class="reply-avatar" :src="getReplyAvatar(item)" mode="aspectFill" />
						<view class="reply-user-meta">
							<text class="reply-user">{{ item.userNickname || '用户' }}</text>
							<text class="reply-time">{{ formatCommentTime(item.createTime) }}</text>
						</view>
						<view class="reply-like" :class="{ active: !!item.liked }" @click="toggleReplyLike(item)">
							<uni-icons :type="item.liked ? 'heart-filled' : 'heart'" size="16" :color="item.liked ? '#ef4444' : '#5a6a7a'" />
							<text class="reply-like-text">{{ Number(item.likeCount || 0) }}</text>
						</view>
					</view>
					<text class="reply-content">{{ item.content }}</text>
				</view>
			</view>
			<view v-else class="reply-empty">暂无追评，快来抢沙发吧</view>

			<view class="reply-input-row">
				<input class="reply-input" v-model="replyContent" maxlength="300" placeholder="说说你的看法（最多300字）" />
				<view class="reply-send" @click="submitReply">发送</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getApiBaseUrl } from '../../utils/apiBase'
import {
	createModelCommentReplyApi,
	getModelCommentRepliesApi,
	toggleModelCommentReplyLikeApi
} from '../../api/order'

const COMMENT_DETAIL_STORAGE_KEY = 'model_comment_detail'
const DEFAULT_AVATAR = 'https://api.dicebear.com/7.x/avataaars/svg?seed=3dshop-user'
const ANONYMOUS_AVATAR = 'https://api.dicebear.com/7.x/identicon/svg?seed=anonymous'

const comment = ref(null)
const replies = ref([])
const replyTotal = ref(0)
const replyPageNum = ref(1)
const replyPageSize = ref(20)
const replyContent = ref('')

onLoad(() => {
	if (!ensureLoginOrRedirect()) return
	const cached = uni.getStorageSync(COMMENT_DETAIL_STORAGE_KEY)
	if (cached && typeof cached === 'object') {
		comment.value = cached
		loadReplies()
		return
	}
	uni.showToast({ title: '评价数据不存在', icon: 'none' })
	setTimeout(() => {
		uni.navigateBack({ delta: 1 })
	}, 400)
})

const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

const toAbsoluteAssetUrl = (url) => {
	const value = String(url || '').trim()
	if (!value) return ''
	if (/^https?:\/\//i.test(value)) return value
	const base = String(getApiBaseUrl() || '').replace(/\/+$/, '')
	if (!base) return value
	return `${base}${value.startsWith('/') ? '' : '/'}${value}`
}

const isVideoMedia = (url) => {
	const lower = String(url || '').toLowerCase().split('?')[0]
	return /\.(mp4|mov|m4v|webm|ogg)$/i.test(lower) || lower.includes('/videos/')
}

const parseCommentMediaList = (raw) => {
	return String(raw || '')
		.split(',')
		.map(item => item.trim())
		.filter(Boolean)
		.map(url => {
			const mediaUrl = toAbsoluteAssetUrl(url)
			return { url: mediaUrl, type: isVideoMedia(mediaUrl) ? 'video' : 'image' }
		})
}

const commentMediaList = computed(() => parseCommentMediaList(comment.value?.commentImages))

const getCommentDisplayName = (record) => {
	if (record?.isMine) return '我的评价'
	if (Number(record?.isAnonymous) === 1) return '匿名用户'
	return record?.userNickname || '用户'
}

const getCommentAvatar = (record) => {
	if (Number(record?.isAnonymous) === 1 && !record?.isMine) {
		return ANONYMOUS_AVATAR
	}
	const avatar = toAbsoluteAssetUrl(record?.userAvatar)
	return avatar || DEFAULT_AVATAR
}

const toStarScore = (value) => {
	const numeric = Number(value)
	if (!Number.isFinite(numeric)) return 0
	return clamp(Math.round(numeric), 0, 5)
}

const renderStars = (value) => {
	const score = toStarScore(value)
	return `${'★'.repeat(score)}${'☆'.repeat(5 - score)}`
}

const formatScore = (value) => {
	const numeric = Number(value)
	if (!Number.isFinite(numeric)) return '-'
	return numeric.toFixed(1)
}

const formatCommentTime = (value) => {
	if (!value) return '-'
	return String(value).replace('T', ' ').slice(0, 19)
}

const previewCommentImage = (current) => {
	const urls = commentMediaList.value.filter(item => item.type === 'image').map(item => item.url)
	if (!urls.length) return
	uni.previewImage({ urls, current })
}

const previewCommentVideo = (url) => {
	const videoUrl = String(url || '').trim()
	if (!videoUrl) return
	if (typeof uni.previewMedia === 'function') {
		uni.previewMedia({
			sources: [{ url: videoUrl, type: 'video' }],
			current: 0
		})
		return
	}
	uni.showToast({ title: '当前平台不支持全屏预览', icon: 'none' })
}

const getReplyAvatar = (reply) => {
	const avatar = toAbsoluteAssetUrl(reply?.userAvatar)
	return avatar || DEFAULT_AVATAR
}

const loadReplies = async () => {
	const commentId = String(comment.value?.id || '').trim()
	if (!commentId) return
	try {
		const data = await getModelCommentRepliesApi({
			commentId,
			pageNum: replyPageNum.value,
			pageSize: replyPageSize.value
		})
		replies.value = Array.isArray(data?.records) ? data.records.map(item => ({ ...item, liked: false })) : []
		replyTotal.value = Number(data?.total || 0)
	} catch (error) {
		uni.showToast({ title: error?.message || '追评加载失败', icon: 'none' })
	}
}

const submitReply = async () => {
	if (!ensureLoginOrRedirect()) return
	const content = String(replyContent.value || '').trim()
	if (!content) {
		uni.showToast({ title: '请输入追评内容', icon: 'none' })
		return
	}
	const commentId = String(comment.value?.id || '').trim()
	if (!commentId) return
	try {
		await createModelCommentReplyApi({ commentId, content })
		replyContent.value = ''
		await loadReplies()
		comment.value.replyCount = Number(comment.value.replyCount || 0) + 1
		uni.showToast({ title: '追评成功', icon: 'none' })
	} catch (error) {
		uni.showToast({ title: error?.message || '追评失败', icon: 'none' })
	}
}

const toggleReplyLike = async (reply) => {
	if (!ensureLoginOrRedirect()) return
	const replyId = String(reply?.id || '').trim()
	if (!replyId) return
	try {
		const result = await toggleModelCommentReplyLikeApi({ replyId })
		reply.liked = !!result?.active
		const current = Number(reply.likeCount || 0)
		const safeCurrent = Number.isFinite(current) ? current : 0
		reply.likeCount = result?.active ? safeCurrent + 1 : Math.max(0, safeCurrent - 1)
	} catch (error) {
		uni.showToast({ title: error?.message || '操作失败', icon: 'none' })
	}
}
</script>

<style scoped lang="scss">
$sky-blue: #00bfff;
$sky-light: #5ce1ff;
$sky-deep: #0099cc;
$surface: #f8f8f8;
$surface-raised: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$text-muted: #94a3b8;
$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
$danger: #ff4d6d;
$gradient-primary: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.comment-detail-page {
	padding: 24rpx;
	background-color: $surface;
	min-height: 100vh;
	box-sizing: border-box;
}

.comment-user-row {
	display: flex;
	align-items: center;
	padding: 28rpx 32rpx;
	border-radius: 24rpx;
	background-color: $surface-raised;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.35s ease-out both;
}

.comment-avatar {
	width: 72rpx;
	height: 72rpx;
	border-radius: 50%;
	background-color: $surface;
}

.comment-user-meta {
	margin-left: 18rpx;
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.comment-user {
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
}

.comment-time {
	font-size: 24rpx;
	color: $text-muted;
}

.score-card {
	margin-top: 24rpx;
	padding: 28rpx 32rpx;
	border-radius: 24rpx;
	background-color: $surface-raised;
	box-shadow: $shadow-card;
	display: flex;
	flex-direction: column;
	gap: 16rpx;
	animation: fadeInUp 0.35s ease-out 0.05s both;
}

.score-item {
	display: flex;
	align-items: center;
	gap: 12rpx;
}

.score-item.total {
	padding-top: 12rpx;
	position: relative;

	&::before {
		content: '';
		position: absolute;
		left: 0;
		right: 0;
		top: 0;
		height: 1rpx;
		background: rgba(0, 0, 0, 0.03);
	}
}

.score-label {
	width: 130rpx;
	font-size: 26rpx;
	color: $text-secondary;
}

.score-stars {
	font-size: 26rpx;
	letter-spacing: 2rpx;
	color: #f59e0b;
}

.score-value {
	margin-left: auto;
	font-size: 26rpx;
	font-weight: 600;
	color: $text-primary;
}

.comment-text {
	margin-top: 24rpx;
	padding: 28rpx 32rpx;
	border-radius: 24rpx;
	background-color: $surface-raised;
	box-shadow: $shadow-card;
	font-size: 28rpx;
	line-height: 1.7;
	color: $text-primary;
	animation: fadeInUp 0.35s ease-out 0.1s both;
}

.media-list {
	margin-top: 24rpx;
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
}

.media-item {
	width: 220rpx;
}

.media-thumb,
.media-video {
	width: 220rpx;
	height: 220rpx;
	border-radius: 16rpx;
	background-color: $surface;
}

.media-video-wrap {
	position: relative;
	width: 220rpx;
	height: 220rpx;
}

.video-full-btn {
	position: absolute;
	left: 50%;
	bottom: 10rpx;
	transform: translateX(-50%);
	padding: 6rpx 16rpx;
	border-radius: 999rpx;
	font-size: 20rpx;
	color: #ffffff;
	background-color: rgba(0, 0, 0, 0.45);
	backdrop-filter: blur(8px);
	-webkit-backdrop-filter: blur(8px);
}

.reply-card {
	margin-top: 24rpx;
	padding: 28rpx 32rpx;
	border-radius: 24rpx;
	background: $surface-raised;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.35s ease-out 0.15s both;
}

.reply-title-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 16rpx;
}

.reply-title {
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}

.reply-total {
	font-size: 24rpx;
	color: $text-secondary;
}

.reply-list {
	display: flex;
	flex-direction: column;
	gap: 16rpx;
}

.reply-item {
	padding: 20rpx;
	border-radius: 16rpx;
	background: $surface;
}

.reply-user-row {
	display: flex;
	align-items: center;
}

.reply-avatar {
	width: 52rpx;
	height: 52rpx;
	border-radius: 50%;
	background: $surface-raised;
}

.reply-user-meta {
	margin-left: 12rpx;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}

.reply-user {
	font-size: 26rpx;
	font-weight: 600;
	color: $text-primary;
}

.reply-time {
	font-size: 22rpx;
	color: $text-muted;
}

.reply-like {
	margin-left: auto;
	display: flex;
	align-items: center;
	gap: 6rpx;
	padding: 8rpx 16rpx;
	border-radius: 999rpx;
	background: $surface-raised;

	&.active {
		background: #fff1f2;
	}
}

.reply-like-text {
	font-size: 22rpx;
	color: $text-secondary;
}

.reply-content {
	display: block;
	margin-top: 10rpx;
	font-size: 26rpx;
	line-height: 1.7;
	color: $text-primary;
}

.reply-empty {
	font-size: 24rpx;
	color: $text-muted;
	padding: 16rpx 0;
}

.reply-input-row {
	margin-top: 20rpx;
	display: flex;
	gap: 12rpx;
}

.reply-input {
	flex: 1;
	height: 68rpx;
	padding: 0 20rpx;
	border-radius: 999rpx;
	background: $surface;
	font-size: 26rpx;
	box-sizing: border-box;
}

.reply-send {
	width: 120rpx;
	height: 68rpx;
	border-radius: 999rpx;
	background: $gradient-primary;
	color: #fff;
	font-size: 26rpx;
	font-weight: 600;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.35);

	&:active {
		transform: scale(0.96);
	}
}
</style>