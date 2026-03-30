<template>
	<view class="comment-list-page">
		<view v-if="loading && comments.length === 0" class="state-text">评价加载中...</view>
		<view v-else-if="comments.length === 0" class="state-text">暂无评价</view>
		<view v-else>
			<view class="sort-row">
				<view class="sort-btn" :class="{ active: sortType === 'hot' }" @click="changeSort('hot')">最热</view>
				<view class="sort-btn" :class="{ active: sortType === 'latest' }" @click="changeSort('latest')">最新</view>
			</view>
			<view class="comment-item" v-for="comment in comments" :key="comment.id" @click="openCommentDetail(comment)">
				<view class="comment-head">
					<image class="comment-avatar" :src="getCommentAvatar(comment)" mode="aspectFill" />
					<view class="comment-user-meta">
						<text class="comment-user">{{ getCommentDisplayName(comment) }}</text>
						<text class="comment-time">{{ formatCommentTime(comment.createTime) }}</text>
					</view>
				</view>
				<view class="comment-score-row">
					<text class="comment-score-stars">{{ renderStars(comment.avgScore) }}</text>
					<text class="comment-score-text">{{ formatScore(comment.avgScore) }}</text>
				</view>
				<text class="comment-text">{{ comment.commentText || '用户未填写文字评价' }}</text>
				<view class="comment-action-row">
					<text class="reply-count">追评 {{ Number(comment.replyCount || 0) }}</text>
					<view class="comment-like" :class="{ active: !!comment.liked }" @click.stop="toggleCommentLike(comment)">
						<uni-icons :type="comment.liked ? 'heart-filled' : 'heart'" size="18" :color="comment.liked ? '#ef4444' : '#5a6a7a'" />
						<text class="comment-like-text">{{ Number(comment.likeCount || 0) }}</text>
					</view>
				</view>
			</view>
			<view class="load-more-wrap">
				<view class="load-more-btn" v-if="hasMore" @click="loadComments">加载更多</view>
				<text v-else class="state-text">没有更多了</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getModelOrderCommentsApi, toggleModelCommentLikeApi } from '../../api/order'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getApiBaseUrl } from '../../utils/apiBase'

const COMMENT_DETAIL_STORAGE_KEY = 'model_comment_detail'
const DEFAULT_AVATAR = 'https://api.dicebear.com/7.x/avataaars/svg?seed=3dshop-user'
const ANONYMOUS_AVATAR = 'https://api.dicebear.com/7.x/identicon/svg?seed=anonymous'

const modelId = ref('')
const comments = ref([])
const loading = ref(false)
const hasMore = ref(true)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const sortType = ref('hot')

onLoad((options) => {
	const id = decodeURIComponent(String(options?.modelId || '').trim())
	if (!id) {
		uni.showToast({ title: '模型参数缺失', icon: 'none' })
		setTimeout(() => uni.navigateBack({ delta: 1 }), 300)
		return
	}
	modelId.value = id
	resetAndLoad()
})

const resetAndLoad = () => {
	comments.value = []
	pageNum.value = 1
	hasMore.value = true
	total.value = 0
	loadComments()
}

const loadComments = async () => {
	if (loading.value || !hasMore.value) return
	loading.value = true
	try {
		const data = await getModelOrderCommentsApi({
			modelId: modelId.value,
			pageNum: pageNum.value,
			pageSize: pageSize.value,
			sortType: sortType.value
		})
		const records = Array.isArray(data?.records) ? data.records.map(item => ({ ...item, liked: false })) : []
		comments.value = [...comments.value, ...records]
		total.value = Number(data?.total || 0)
		pageNum.value += 1
		hasMore.value = comments.value.length < total.value
	} catch (error) {
		uni.showToast({ title: error?.message || '评价加载失败', icon: 'none' })
	} finally {
		loading.value = false
	}
}

const changeSort = (nextSortType) => {
	if (sortType.value === nextSortType) return
	sortType.value = nextSortType
	resetAndLoad()
}

const toggleCommentLike = async (comment) => {
	if (!ensureLoginOrRedirect()) return
	const commentId = String(comment?.id || '').trim()
	if (!commentId) return
	try {
		const result = await toggleModelCommentLikeApi({ commentId })
		comment.liked = !!result?.active
		const current = Number(comment.likeCount || 0)
		const safeCurrent = Number.isFinite(current) ? current : 0
		comment.likeCount = result?.active ? safeCurrent + 1 : Math.max(0, safeCurrent - 1)
	} catch (error) {
		uni.showToast({ title: error?.message || '操作失败', icon: 'none' })
	}
}

const openCommentDetail = (comment) => {
	uni.setStorageSync(COMMENT_DETAIL_STORAGE_KEY, comment)
	uni.navigateTo({ url: '/pages/custom/comment-detail' })
}

const getCommentDisplayName = (comment) => {
	if (Number(comment?.isAnonymous) === 1) return '匿名用户'
	return comment?.userNickname || '用户'
}

const toAbsoluteAssetUrl = (url) => {
	const value = String(url || '').trim()
	if (!value) return ''
	if (/^https?:\/\//i.test(value)) return value
	const base = String(getApiBaseUrl() || '').replace(/\/+$/, '')
	if (!base) return value
	return `${base}${value.startsWith('/') ? '' : '/'}${value}`
}

const getCommentAvatar = (comment) => {
	if (Number(comment?.isAnonymous) === 1) return ANONYMOUS_AVATAR
	const avatar = toAbsoluteAssetUrl(comment?.userAvatar)
	return avatar || DEFAULT_AVATAR
}

const formatCommentTime = (value) => {
	if (!value) return '-'
	return String(value).replace('T', ' ').slice(0, 19)
}

const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

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

.comment-list-page {
	min-height: 100vh;
	background: $surface;
	padding: 24rpx;
	box-sizing: border-box;
}

.sort-row {
	display: flex;
	gap: 16rpx;
	margin-bottom: 24rpx;
}

.sort-btn {
	padding: 12rpx 32rpx;
	border-radius: 999rpx;
	font-size: 26rpx;
	color: $text-secondary;
	background: $surface-raised;
	box-shadow: $shadow-card;

	&:active {
		transform: scale(0.96);
	}

	&.active {
		background: $gradient-primary;
		color: #fff;
		box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.35);
	}
}

.comment-item {
	padding: 28rpx 32rpx;
	border-radius: 24rpx;
	background: $surface-raised;
	margin-bottom: 20rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.35s ease-out both;
}

.comment-head {
	display: flex;
	align-items: center;
}

.comment-avatar {
	width: 64rpx;
	height: 64rpx;
	border-radius: 50%;
	background-color: $surface;
}

.comment-user-meta {
	margin-left: 14rpx;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
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

.comment-score-row {
	margin-top: 16rpx;
	display: flex;
	align-items: center;
	gap: 10rpx;
}

.comment-score-stars {
	font-size: 26rpx;
	letter-spacing: 2rpx;
	color: #f59e0b;
}

.comment-score-text {
	font-size: 24rpx;
	color: $text-secondary;
}

.comment-text {
	display: block;
	margin-top: 16rpx;
	font-size: 28rpx;
	line-height: 1.7;
	color: $text-primary;
}

.comment-action-row {
	margin-top: 16rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.reply-count {
	font-size: 24rpx;
	color: $text-secondary;
}

.comment-like {
	display: flex;
	align-items: center;
	gap: 8rpx;
	padding: 8rpx 16rpx;
	border-radius: 999rpx;
	background: $surface;

	&.active {
		background: #fff1f2;
	}
}

.comment-like-text {
	font-size: 24rpx;
	color: $text-secondary;
}

.load-more-wrap {
	padding: 28rpx 0 36rpx;
	display: flex;
	justify-content: center;
}

.load-more-btn {
	padding: 14rpx 36rpx;
	border-radius: 999rpx;
	background: $gradient-primary;
	color: #fff;
	font-size: 26rpx;
	font-weight: 600;
	box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.35);

	&:active {
		transform: scale(0.96);
	}
}

.state-text {
	font-size: 26rpx;
	color: $text-muted;
	text-align: center;
	padding: 36rpx 0;
}
</style>
