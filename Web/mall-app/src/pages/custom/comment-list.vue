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
						<uni-icons :type="comment.liked ? 'heart-filled' : 'heart'" size="18" :color="comment.liked ? '#ef4444' : '#64748b'" />
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
.comment-list-page {
	min-height: 100vh;
	background: #f8fafc;
	padding: 20rpx;
	box-sizing: border-box;
}

.sort-row {
	display: flex;
	gap: 16rpx;
	margin-bottom: 16rpx;
}

.sort-btn {
	padding: 10rpx 24rpx;
	border-radius: 999rpx;
	font-size: 24rpx;
	color: #475569;
	background: #e2e8f0;

	&.active {
		background: #2563eb;
		color: #fff;
	}
}

.comment-item {
	padding: 20rpx;
	border-radius: 16rpx;
	background: #fff;
	margin-bottom: 14rpx;
}

.comment-head {
	display: flex;
	align-items: center;
}

.comment-avatar {
	width: 64rpx;
	height: 64rpx;
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

.comment-score-row {
	margin-top: 12rpx;
	display: flex;
	align-items: center;
	gap: 10rpx;
}

.comment-score-stars {
	font-size: 24rpx;
	letter-spacing: 2rpx;
	color: #f59e0b;
}

.comment-score-text {
	font-size: 22rpx;
	color: #64748b;
}

.comment-text {
	display: block;
	margin-top: 12rpx;
	font-size: 24rpx;
	line-height: 1.7;
	color: #334155;
}

.comment-action-row {
	margin-top: 12rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.reply-count {
	font-size: 22rpx;
	color: #64748b;
}

.comment-like {
	display: flex;
	align-items: center;
	gap: 8rpx;
	padding: 6rpx 14rpx;
	border-radius: 999rpx;
	background: #f8fafc;

	&.active {
		background: #fff1f2;
	}
}

.comment-like-text {
	font-size: 22rpx;
	color: #64748b;
}

.load-more-wrap {
	padding: 20rpx 0 30rpx;
	display: flex;
	justify-content: center;
}

.load-more-btn {
	padding: 12rpx 28rpx;
	border-radius: 999rpx;
	background: #2563eb;
	color: #fff;
	font-size: 24rpx;
}

.state-text {
	font-size: 24rpx;
	color: #94a3b8;
	text-align: center;
	padding: 30rpx 0;
}
</style>
