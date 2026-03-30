<template>
	<view class="video-preview-page">
		<view class="video-card">
			<video
				class="video-player"
				:src="videoUrl"
				controls
				show-fullscreen-btn
				:autoplay="false"
				object-fit="contain"
				preload="metadata"
			/>
		</view>

		<view class="info-card">
			<view class="name">{{ videoName || '视频附件预览' }}</view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const videoUrl = ref('')
const videoName = ref('')

onLoad((options) => {
	const rawUrl = decodeURIComponent(String(options?.url || '').trim())
	const rawName = decodeURIComponent(String(options?.name || '').trim())
	if (!rawUrl) {
		uni.showToast({ title: '视频地址缺失', icon: 'none' })
		setTimeout(() => uni.navigateBack(), 300)
		return
	}
	videoUrl.value = rawUrl
	videoName.value = rawName
})
</script>

<style scoped lang="scss">
$primary: #00bfff;
$light: #5ce1ff;
$bg: #f8f8f8;
$card: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.video-preview-page {
	min-height: 100vh;
	background: $bg;
	padding: 28rpx 32rpx;
}

.video-card {
	background: $card;
	border-radius: 24rpx;
	overflow: hidden;
	box-shadow: $shadow-card;
	margin-bottom: 28rpx;
	animation: fadeInUp 0.4s ease-out;
}

.video-player {
	width: 100%;
	height: 760rpx;
	background: #000;
	display: block;
}

.info-card {
	background: $card;
	border-radius: 24rpx;
	padding: 28rpx 32rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.4s ease-out 0.1s both;

	.name {
		font-size: 30rpx;
		font-weight: 700;
		color: $text-primary;
	}
}
</style>