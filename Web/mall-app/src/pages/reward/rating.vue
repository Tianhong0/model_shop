<template>
	<view class="rating-container">
		<view class="section card">
			<view class="sec-title">任务信息</view>
			<view class="task-info" v-if="taskInfo">
				<text class="task-title">{{ taskInfo.title }}</text>
				<text class="task-price">赏金：￥{{ taskInfo.finalAmount || taskInfo.budgetAmount }}</text>
			</view>
		</view>

		<view class="section card">
			<view class="sec-title">设计者评价</view>

			<view class="rating-stars">
				<text class="label">评分</text>
				<view class="stars">
					<view
						v-for="i in 5"
						:key="i"
						class="star"
						:class="{ active: i <= score }"
						@click="score = i"
					>
						★
					</view>
				</view>
				<text class="score-text">{{ scoreText }}</text>
			</view>

			<view class="form-item">
				<text class="label">评价内容</text>
				<textarea
					v-model="comment"
					placeholder="请输入评价内容（选填）"
					:maxlength="1000"
					class="comment-input"
				/>
				<text class="char-count">{{ comment.length }}/1000</text>
			</view>

			<view class="form-item">
				<text class="label">上传图片（选填）</text>
				<view class="image-list">
					<view
						v-for="(img, idx) in images"
						:key="idx"
						class="image-item"
					>
						<image :src="img" mode="aspectFill" @click="previewImage(idx)" />
						<view class="remove-btn" @click="removeImage(idx)">×</view>
					</view>
					<view v-if="images.length < 5" class="add-image" @click="chooseImage">
						<text>+</text>
					</view>
				</view>
			</view>

			<view class="form-item">
				<view class="anonymous-toggle" @click="isAnonymous = isAnonymous === 1 ? 0 : 1">
					<view class="checkbox" :class="{ checked: isAnonymous === 1 }">
						<text v-if="isAnonymous === 1">✓</text>
					</view>
					<text>匿名评价</text>
				</view>
			</view>
		</view>

		<view class="bottom-bar">
			<button class="submit-btn" :disabled="score === 0" @click="submitRating">提交评价</button>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getBountyTaskDetailApi, createBountyRatingApi, uploadBountyAttachmentApi } from '../../api/reward'

const taskId = ref(null)
const taskInfo = ref(null)
const score = ref(0)
const comment = ref('')
const images = ref([])
const isAnonymous = ref(0)
const submitting = ref(false)

const scoreText = computed(() => {
	const texts = ['', '非常差', '较差', '一般', '满意', '非常满意']
	return texts[score.value] || ''
})

const loadTaskInfo = async () => {
	if (!taskId.value) return
	try {
		const data = await getBountyTaskDetailApi(taskId.value)
		taskInfo.value = data
	} catch (error) {
		uni.showToast({ title: error?.message || '加载任务失败', icon: 'none' })
	}
}

const chooseImage = () => {
	uni.chooseImage({
		count: 5 - images.value.length,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: async (res) => {
			uni.showLoading({ title: '上传中...' })
			for (const path of res.tempFilePaths) {
				try {
					const url = await uploadBountyAttachmentApi(path, 'postImg')
					images.value.push(url)
				} catch (error) {
					uni.showToast({ title: '图片上传失败', icon: 'none' })
				}
			}
			uni.hideLoading()
		}
	})
}

const removeImage = (idx) => {
	images.value.splice(idx, 1)
}

const previewImage = (idx) => {
	uni.previewImage({
		current: idx,
		urls: images.value
	})
}

const submitRating = async () => {
	if (score.value === 0) {
		uni.showToast({ title: '请选择评分', icon: 'none' })
		return
	}
	if (submitting.value) return
	submitting.value = true

	try {
		await createBountyRatingApi({
			taskId: taskId.value,
			score: score.value,
			comment: comment.value,
			images: images.value.length > 0 ? images.value.join(',') : null,
			isAnonymous: isAnonymous.value
		})
		uni.showToast({ title: '评价成功', icon: 'success' })
		setTimeout(() => {
			uni.navigateBack()
		}, 1500)
	} catch (error) {
		uni.showToast({ title: error?.message || '评价失败', icon: 'none' })
	} finally {
		submitting.value = false
	}
}

onLoad((options) => {
	if (options?.taskId) {
		taskId.value = String(options.taskId)
		loadTaskInfo()
	}
})
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
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
	30% { transform: scale(1.25); }
	50% { transform: scale(0.9); }
	70% { transform: scale(1.08); }
	100% { transform: scale(1); }
}

.rating-container {
	min-height: 100vh;
	background-color: $bg;
	padding-bottom: 140rpx;
}

.section {
	margin: 28rpx 32rpx;
	padding: 32rpx;
	background-color: $card;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.4s ease-out both;

	.sec-title {
		font-size: 30rpx;
		font-weight: 700;
		color: $text-primary;
		margin-bottom: 24rpx;
	}
}

.task-info {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
	.task-title {
		font-size: 28rpx;
		color: $text-primary;
		font-weight: 600;
	}
	.task-price {
		font-size: 26rpx;
		color: $primary;
		font-weight: 500;
	}
}

.rating-stars {
	display: flex;
	align-items: center;
	gap: 20rpx;
	margin-bottom: 36rpx;
	padding: 24rpx;
	background: rgba(0,191,255,0.03);
	border-radius: 20rpx;

	.label {
		font-size: 28rpx;
		color: $text-secondary;
		font-weight: 500;
	}

	.stars {
		display: flex;
		gap: 12rpx;

		.star {
			font-size: 52rpx;
			color: #e0e0e0;
			transition: all 0.25s;

			&.active {
				color: #ffc107;
				animation: jellyPop 0.3s ease-out;
			}
		}
	}

	.score-text {
		font-size: 24rpx;
		color: $primary;
		font-weight: 500;
	}
}

.form-item {
	margin-bottom: 32rpx;

	.label {
		font-size: 28rpx;
		color: $text-secondary;
		margin-bottom: 14rpx;
		display: block;
		font-weight: 500;
	}

	.comment-input {
		width: 100%;
		min-height: 220rpx;
		padding: 24rpx;
		font-size: 28rpx;
		color: $text-primary;
		background-color: $bg;
		border-radius: 20rpx;
		box-sizing: border-box;
	}

	.char-count {
		font-size: 22rpx;
		color: $text-muted;
		text-align: right;
		display: block;
		margin-top: 10rpx;
	}
}

.image-list {
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
	margin-top: 12rpx;

	.image-item {
		position: relative;
		width: 140rpx;
		height: 140rpx;

		image {
			width: 100%;
			height: 100%;
			border-radius: 16rpx;
			opacity: 0;
			animation: fadeInUp 0.3s ease-out forwards;
		}

		.remove-btn {
			position: absolute;
			top: -10rpx;
			right: -10rpx;
			width: 40rpx;
			height: 40rpx;
			background-color: $danger;
			color: #fff;
			border-radius: 50%;
			display: flex;
			align-items: center;
			justify-content: center;
			font-size: 24rpx;
		}
	}

	.add-image {
		width: 140rpx;
		height: 140rpx;
		background-color: $bg;
		border: 2rpx dashed rgba(0,191,255,0.25);
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: center;

		text {
			font-size: 52rpx;
			color: $text-muted;
		}

		&:active { background: rgba(0,191,255,0.04); }
	}
}

.anonymous-toggle {
	display: flex;
	align-items: center;
	gap: 16rpx;

	.checkbox {
		width: 40rpx;
		height: 40rpx;
		border: 2rpx solid rgba(0,0,0,0.12);
		border-radius: 10rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: all 0.2s;

		&.checked {
			background-color: $primary;
			border-color: $primary;

			text {
				color: #fff;
				font-size: 24rpx;
			}
		}
	}

	text {
		font-size: 28rpx;
		color: $text-secondary;
	}
}

.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	background: rgba(255,255,255,0.72);
	backdrop-filter: blur(24px);
	padding: 20rpx 32rpx;
	padding-bottom: calc(env(safe-area-inset-bottom) + 20rpx);
	box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.06);

	.submit-btn {
		width: 100%;
		height: 84rpx;
		border-radius: 999rpx;
		font-size: 30rpx;
		font-weight: 600;
		color: #fff;
		background: $gradient;
		border: none;
		&:active { transform: scale(0.96); }
		&[disabled] {
			opacity: 0.4;
		}
	}
}
</style>
