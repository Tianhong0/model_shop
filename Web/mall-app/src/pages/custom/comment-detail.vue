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
							<uni-icons :type="item.liked ? 'heart-filled' : 'heart'" size="16" :color="item.liked ? '#ef4444' : '#64748b'" />
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
.comment-detail-page {
	padding: 24rpx;
	background-color: #f8fafc;
	min-height: 100vh;
	box-sizing: border-box;
}

.comment-user-row {
	display: flex;
	align-items: center;
	padding: 18rpx;
	border-radius: 16rpx;
	background-color: #ffffff;
}

.comment-avatar {
	width: 72rpx;
	height: 72rpx;
	border-radius: 50%;
	background-color: #e2e8f0;
}

.comment-user-meta {
	margin-left: 14rpx;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}

.comment-user {
	font-size: 26rpx;
	font-weight: 600;
	color: #0f172a;
}

.comment-time {
	font-size: 22rpx;
	color: #94a3b8;
}

.score-card {
	margin-top: 18rpx;
	padding: 18rpx;
	border-radius: 16rpx;
	background-color: #ffffff;
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.score-item {
	display: flex;
	align-items: center;
	gap: 12rpx;
}

.score-item.total {
	padding-top: 8rpx;
	border-top: 1px solid #e2e8f0;
}

.score-label {
	width: 130rpx;
	font-size: 24rpx;
	color: #475569;
}

.score-stars {
	font-size: 24rpx;
	letter-spacing: 2rpx;
	color: #f59e0b;
}

.score-value {
	margin-left: auto;
	font-size: 24rpx;
	font-weight: 600;
	color: #334155;
}

.comment-text {
	margin-top: 18rpx;
	padding: 18rpx;
	border-radius: 16rpx;
	background-color: #ffffff;
	font-size: 24rpx;
	line-height: 1.6;
	color: #334155;
}

.media-list {
	margin-top: 18rpx;
	display: flex;
	flex-wrap: wrap;
	gap: 12rpx;
}

.media-item {
	width: 220rpx;
}

.media-thumb,
.media-video {
	width: 220rpx;
	height: 220rpx;
	border-radius: 12rpx;
	background-color: #0f172a;
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
	padding: 6rpx 12rpx;
	border-radius: 999rpx;
	font-size: 20rpx;
	color: #ffffff;
	background-color: rgba(15, 23, 42, 0.65);
}

.reply-card {
	margin-top: 18rpx;
	padding: 18rpx;
	border-radius: 16rpx;
	background: #fff;
}

.reply-title-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 10rpx;
}

.reply-title {
	font-size: 26rpx;
	font-weight: 600;
	color: #0f172a;
}

.reply-total {
	font-size: 22rpx;
	color: #64748b;
}

.reply-list {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.reply-item {
	padding: 14rpx;
	border-radius: 12rpx;
	background: #f8fafc;
}

.reply-user-row {
	display: flex;
	align-items: center;
}

.reply-avatar {
	width: 52rpx;
	height: 52rpx;
	border-radius: 50%;
	background: #e2e8f0;
}

.reply-user-meta {
	margin-left: 10rpx;
	display: flex;
	flex-direction: column;
	gap: 2rpx;
}

.reply-user {
	font-size: 23rpx;
	font-weight: 600;
	color: #1e293b;
}

.reply-time {
	font-size: 20rpx;
	color: #94a3b8;
}

.reply-like {
	margin-left: auto;
	display: flex;
	align-items: center;
	gap: 6rpx;
	padding: 6rpx 12rpx;
	border-radius: 999rpx;
	background: #fff;

	&.active {
		background: #fff1f2;
	}
}

.reply-like-text {
	font-size: 20rpx;
	color: #64748b;
}

.reply-content {
	display: block;
	margin-top: 8rpx;
	font-size: 23rpx;
	line-height: 1.6;
	color: #334155;
}

.reply-empty {
	font-size: 22rpx;
	color: #94a3b8;
	padding: 10rpx 0;
}

.reply-input-row {
	margin-top: 14rpx;
	display: flex;
	gap: 10rpx;
}

.reply-input {
	flex: 1;
	height: 64rpx;
	padding: 0 16rpx;
	border-radius: 999rpx;
	background: #f1f5f9;
	font-size: 24rpx;
	box-sizing: border-box;
}

.reply-send {
	width: 110rpx;
	height: 64rpx;
	border-radius: 999rpx;
	background: #2563eb;
	color: #fff;
	font-size: 24rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}
</style>