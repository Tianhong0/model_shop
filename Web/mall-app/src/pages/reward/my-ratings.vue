<template>
	<view class="ratings-container">
		<view class="section card" v-if="reputation">
			<view class="rep-header">
				<view class="avatar">{{ designerName.slice(0, 1) }}</view>
				<view class="rep-info">
					<text class="name">{{ designerName }}</text>
					<view class="rep-stats">
						<view class="stat">
							<text class="stat-val" :class="repScoreClass">{{ reputation.reputationScore || 0 }}</text>
							<text class="stat-label">信誉分</text>
						</view>
						<view class="stat">
							<text class="stat-val">{{ reputation.totalTasks || 0 }}</text>
							<text class="stat-label">完成任务</text>
						</view>
						<view class="stat">
							<text class="stat-val">{{ reputation.avgScore || '0.0' }}</text>
							<text class="stat-label">平均评分</text>
						</view>
						<view class="stat">
							<text class="stat-val">{{ reputation.qualityAnswerCount || 0 }}</text>
							<text class="stat-label">优质回答</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<view class="section card" v-if="reputation">
			<view class="sec-title">评分分布</view>
			<view class="star-row" v-for="i in 5" :key="i">
				<text class="star-label">{{ 6 - i }}星</text>
				<view class="bar-bg">
					<view class="bar-fill" :style="{ width: starPercent(6 - i) + '%' }"></view>
				</view>
				<text class="star-count">{{ starCount(6 - i) }}</text>
			</view>
		</view>

		<view class="section card">
			<view class="sec-header">
				<text class="sec-title">收到的评价</text>
				<text class="appeal-link" @click="goMyAppeals">查看我的申诉 ></text>
			</view>
			<view v-if="ratingList.length === 0" class="empty-tip">暂无评价</view>
			<view class="rating-item" v-for="item in ratingList" :key="item.id">
				<view class="rating-top">
					<text class="publisher">{{ item.isAnonymous === 1 ? '匿名用户' : (item.publisherName || '用户') }}</text>
					<view class="stars-display">
						<text v-for="s in 5" :key="s" class="star" :class="{ active: s <= item.score }">★</text>
					</view>
					<text class="time">{{ formatTime(item.createTime) }}</text>
				</view>
				<text class="task-title" v-if="item.taskTitle">任务：{{ item.taskTitle }}</text>
				<text class="comment" v-if="item.comment">{{ item.comment }}</text>
				<view class="rating-images" v-if="item.images && item.images.length">
					<image
						v-for="(img, idx) in parseImages(item.images)"
						:key="idx"
						:src="img"
						class="rating-img"
						mode="aspectFill"
						@click="previewImage(parseImages(item.images), idx)"
					/>
				</view>

				<view class="appeal-action" v-if="item.status === 1 && !appealedRatingIds.has(String(item.id))">
					<button class="appeal-btn" @click="toggleAppealForm(item.id)">
						{{ expandedAppealId === item.id ? '收起' : '申诉此评价' }}
					</button>
				</view>
				<view v-if="item.status === 0" class="invalid-badge">
					<text>该评价已被标记为无效</text>
				</view>
				<view v-if="appealedRatingIds.has(String(item.id))" class="appealed-badge">
					<text>已提交申诉</text>
				</view>

				<view class="appeal-form" v-if="expandedAppealId === item.id">
					<textarea
						v-model="appealForm.reason"
						placeholder="请描述申诉原因（为什么认为该评价不合理）"
						:maxlength="1000"
						class="appeal-textarea"
					/>
					<text class="char-count">{{ appealForm.reason.length }}/1000</text>
					<view class="evidence-section">
						<text class="evidence-label">证据材料（可选）</text>
						<view class="image-list">
							<view v-for="(img, idx) in appealForm.evidenceUrls" :key="idx" class="image-item">
								<image :src="img" mode="aspectFill" @click="previewImage(appealForm.evidenceUrls, idx)" />
								<view class="remove-btn" @click="removeEvidence(idx)">×</view>
							</view>
							<view v-if="appealForm.evidenceUrls.length < 5" class="add-image" @click="chooseEvidence">
								<text>+</text>
							</view>
						</view>
					</view>
					<button
						class="submit-appeal-btn"
						:disabled="!appealForm.reason.trim() || submittingAppeal"
						@click="submitAppeal(item.id)"
					>
						{{ submittingAppeal ? '提交中...' : '提交申诉' }}
					</button>
				</view>
			</view>
			<view v-if="hasMore" class="load-more" @click="loadMore">
				<text>加载更多</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
	getDesignerReputationApi,
	getDesignerRatingsApi,
	createBountyRatingAppealApi,
	getMyBountyAppealsApi,
	uploadBountyAttachmentApi
} from '../../api/reward'

const myId = ref(String((uni.getStorageSync('user_profile') || {}).id || ''))
const designerName = ref('我')
const reputation = ref(null)
const ratingList = ref([])
const pageNum = ref(1)
const pageSize = 10
const hasMore = ref(false)

const appealedRatingIds = ref(new Set())
const expandedAppealId = ref(null)
const submittingAppeal = ref(false)
const appealForm = ref({
	reason: '',
	evidenceUrls: []
})

const repScoreClass = computed(() => {
	const score = reputation.value?.reputationScore || 0
	if (score >= 80) return 'score-high'
	if (score >= 60) return 'score-mid'
	return 'score-low'
})

const loadReputation = async () => {
	try {
		const data = await getDesignerReputationApi(myId.value)
		reputation.value = data
		if (data?.designerName) {
			designerName.value = data.designerName
		}
	} catch (error) {
		uni.showToast({ title: error?.message || '加载信誉失败', icon: 'none' })
	}
}

const loadRatings = async () => {
	try {
		const data = await getDesignerRatingsApi(myId.value, pageNum.value, pageSize)
		const records = data?.records || []
		if (pageNum.value === 1) {
			ratingList.value = records
		} else {
			ratingList.value.push(...records)
		}
		hasMore.value = records.length >= pageSize
	} catch (error) {
		uni.showToast({ title: error?.message || '加载评价失败', icon: 'none' })
	}
}

const loadAppealedIds = async () => {
	try {
		const data = await getMyBountyAppealsApi(1, 100)
		const records = data?.records || []
		appealedRatingIds.value = new Set(records.map(a => String(a.ratingId)))
	} catch (_) {}
}

const loadMore = () => {
	pageNum.value++
	loadRatings()
}

const starCount = (star) => {
	if (!reputation.value) return 0
	const map = {
		5: reputation.value.fiveStarCount,
		4: reputation.value.fourStarCount,
		3: reputation.value.threeStarCount,
		2: reputation.value.twoStarCount,
		1: reputation.value.oneStarCount
	}
	return map[star] || 0
}

const starPercent = (star) => {
	const total = reputation.value?.totalRatings || 0
	if (total === 0) return 0
	return Math.round((starCount(star) / total) * 100)
}

const parseImages = (images) => {
	if (Array.isArray(images)) return images
	if (typeof images === 'string') return images.split(',').filter(Boolean)
	return []
}

const formatTime = (time) => {
	if (!time) return ''
	return String(time).substring(0, 10)
}

const previewImage = (urls, idx) => {
	uni.previewImage({ current: idx, urls })
}

const toggleAppealForm = (ratingId) => {
	if (expandedAppealId.value === ratingId) {
		expandedAppealId.value = null
	} else {
		expandedAppealId.value = ratingId
		appealForm.value = { reason: '', evidenceUrls: [] }
	}
}

const chooseEvidence = () => {
	uni.chooseImage({
		count: 5 - appealForm.value.evidenceUrls.length,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: async (res) => {
			uni.showLoading({ title: '上传中...' })
			for (const path of res.tempFilePaths) {
				try {
					const url = await uploadBountyAttachmentApi(path, 'postImg')
					appealForm.value.evidenceUrls.push(url)
				} catch (_) {
					uni.showToast({ title: '图片上传失败', icon: 'none' })
				}
			}
			uni.hideLoading()
		}
	})
}

const removeEvidence = (idx) => {
	appealForm.value.evidenceUrls.splice(idx, 1)
}

const submitAppeal = async (ratingId) => {
	if (!appealForm.value.reason.trim()) {
		uni.showToast({ title: '请填写申诉原因', icon: 'none' })
		return
	}
	if (submittingAppeal.value) return
	submittingAppeal.value = true
	try {
		await createBountyRatingAppealApi({
			ratingId,
			reason: appealForm.value.reason,
			evidence: appealForm.value.evidenceUrls.join(',')
		})
		uni.showToast({ title: '申诉已提交', icon: 'success' })
		expandedAppealId.value = null
		appealedRatingIds.value.add(String(ratingId))
	} catch (error) {
		uni.showToast({ title: error?.message || '提交失败', icon: 'none' })
	} finally {
		submittingAppeal.value = false
	}
}

const goMyAppeals = () => {
	uni.navigateTo({ url: '/pages/reward/my-appeals' })
}

onLoad(() => {
	myId.value = String((uni.getStorageSync('user_profile') || {}).id || '')
	const profile = uni.getStorageSync('user_profile') || {}
	designerName.value = profile.nickname || profile.userName || '我'
	loadReputation()
	loadRatings()
	loadAppealedIds()
})
</script>

<style scoped lang="scss">
$primary: #00bfff;
$light: #5ce1ff;
$deep: #0099cc;
$success: #10b981;
$danger: #ff4d6d;
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
@keyframes jellyPop {
	0% { transform: scale(1); }
	30% { transform: scale(1.12); }
	50% { transform: scale(0.95); }
	70% { transform: scale(1.04); }
	100% { transform: scale(1); }
}

.ratings-container {
	min-height: 100vh;
	background-color: $bg;
	padding-bottom: 48rpx;
}

.section {
	margin: 28rpx 32rpx;
	padding: 32rpx;
	background-color: $card;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.4s ease-out both;
	.sec-title { font-size: 30rpx; font-weight: 700; color: $text-primary; margin-bottom: 24rpx; }
}

.sec-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 24rpx;
	.sec-title { margin-bottom: 0; }
}

.appeal-link {
	font-size: 24rpx;
	color: $primary;
	font-weight: 500;
}

.rep-header {
	display: flex;
	gap: 28rpx;
	align-items: center;
	.avatar {
		width: 108rpx;
		height: 108rpx;
		border-radius: 50%;
		background: $gradient;
		display: flex;
		align-items: center;
		justify-content: center;
		color: #fff;
		font-size: 42rpx;
		font-weight: 700;
		box-shadow: 0 8rpx 24rpx rgba(0,191,255,0.2);
		animation: jellyPop 0.6s ease-out;
	}
	.rep-info { flex: 1; }
	.name { font-size: 32rpx; font-weight: 700; color: $text-primary; display: block; margin-bottom: 16rpx; }
}

.rep-stats {
	display: flex;
	gap: 24rpx;
	.stat { text-align: center; }
	.stat-val {
		font-size: 32rpx;
		font-weight: 700;
		color: $primary;
		display: block;
		&.score-high { color: $success; }
		&.score-mid { color: #ff9900; }
		&.score-low { color: $danger; }
	}
	.stat-label { font-size: 22rpx; color: $text-muted; margin-top: 4rpx; }
}

.star-row {
	display: flex;
	align-items: center;
	gap: 16rpx;
	margin-bottom: 16rpx;
	.star-label { font-size: 24rpx; color: $text-secondary; width: 64rpx; }
	.bar-bg {
		flex: 1;
		height: 18rpx;
		background-color: $bg;
		border-radius: 999rpx;
		overflow: hidden;
	}
	.bar-fill {
		height: 100%;
		background: $gradient;
		border-radius: 999rpx;
		transition: width 0.4s ease-out;
	}
	.star-count { font-size: 22rpx; color: $text-muted; width: 56rpx; text-align: right; }
}

.empty-tip { font-size: 26rpx; color: $text-muted; text-align: center; padding: 48rpx 0; }

.rating-item {
	padding: 24rpx 0;
	& + .rating-item {
		border-top: 1rpx solid rgba(0,0,0,0.04);
	}
	.rating-top {
		display: flex;
		align-items: center;
		gap: 12rpx;
		margin-bottom: 12rpx;
	}
	.publisher { font-size: 28rpx; color: $text-primary; font-weight: 600; }
	.stars-display { display: flex; gap: 4rpx; }
	.star { font-size: 28rpx; color: #e0e0e0; &.active { color: #ffc107; } }
	.time { font-size: 22rpx; color: $text-muted; margin-left: auto; }
	.task-title { font-size: 24rpx; color: $primary; display: block; margin-bottom: 8rpx; }
	.comment { font-size: 26rpx; color: $text-secondary; line-height: 1.6; display: block; }
}

.rating-images {
	display: flex; gap: 12rpx; flex-wrap: wrap; margin-top: 16rpx;
	.rating-img {
		width: 120rpx; height: 120rpx; border-radius: 16rpx;
		opacity: 0; animation: fadeInUp 0.3s ease-out forwards;
	}
}

.appeal-action {
	margin-top: 16rpx;
	.appeal-btn {
		height: 56rpx;
		padding: 0 28rpx;
		font-size: 24rpx;
		border-radius: 999rpx;
		background-color: rgba(255, 77, 109, 0.06);
		color: $danger;
		display: inline-flex;
		align-items: center;
		font-weight: 500;
		&:active { transform: scale(0.96); }
	}
}

.invalid-badge {
	margin-top: 12rpx;
	text { font-size: 22rpx; color: $text-muted; font-style: italic; }
}

.appealed-badge {
	margin-top: 12rpx;
	text {
		font-size: 22rpx; color: #ff9900;
		background: rgba(255,153,0,0.08);
		padding: 4rpx 16rpx;
		border-radius: 999rpx;
	}
}

.appeal-form {
	margin-top: 20rpx;
	padding: 24rpx;
	background-color: $bg;
	border-radius: 20rpx;
}

.appeal-textarea {
	width: 100%;
	min-height: 180rpx;
	padding: 20rpx;
	font-size: 28rpx;
	color: $text-primary;
	background-color: $card;
	border-radius: 16rpx;
	box-sizing: border-box;
}

.char-count {
	font-size: 22rpx;
	color: $text-muted;
	text-align: right;
	display: block;
	margin-top: 8rpx;
}

.evidence-section {
	margin-top: 20rpx;
	.evidence-label { font-size: 26rpx; color: $text-secondary; display: block; margin-bottom: 14rpx; font-weight: 500; }
}

.image-list {
	display: flex; flex-wrap: wrap; gap: 14rpx;
	.image-item {
		position: relative; width: 120rpx; height: 120rpx;
		image {
			width: 100%; height: 100%; border-radius: 16rpx;
			opacity: 0; animation: fadeInUp 0.3s ease-out forwards;
		}
		.remove-btn {
			position: absolute; top: -8rpx; right: -8rpx;
			width: 36rpx; height: 36rpx; background-color: $danger; color: #fff;
			border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 22rpx;
		}
	}
	.add-image {
		width: 120rpx; height: 120rpx; background-color: $card;
		border: 2rpx dashed rgba(0,191,255,0.25); border-radius: 16rpx;
		display: flex; align-items: center; justify-content: center;
		text { font-size: 44rpx; color: $text-muted; }
		&:active { background: rgba(0,191,255,0.04); }
	}
}

.submit-appeal-btn {
	margin-top: 20rpx;
	width: 100%;
	height: 76rpx;
	border-radius: 999rpx;
	font-size: 28rpx;
	font-weight: 500;
	color: #fff;
	background: linear-gradient(135deg, $danger 0%, #ff6b8a 100%);
	&:active { transform: scale(0.96); }
	&[disabled] { opacity: 0.4; }
}

.load-more {
	text-align: center;
	padding: 24rpx 0;
	text { font-size: 26rpx; color: $primary; font-weight: 500; }
	&:active { opacity: 0.7; }
}
</style>
