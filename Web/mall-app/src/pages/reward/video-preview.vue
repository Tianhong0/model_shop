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
.video-preview-page {
	min-height: 100vh;
	background: #f8fafc;
	padding: 24rpx;
}

.video-card {
	background: #fff;
	border-radius: 20rpx;
	overflow: hidden;
	box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.06);
	margin-bottom: 20rpx;
}

.video-player {
	width: 100%;
	height: 760rpx;
	background: #000;
	display: block;
}

.info-card {
	background: #fff;
	border-radius: 20rpx;
	padding: 20rpx 24rpx;

	.name {
		font-size: 30rpx;
		font-weight: 600;
		color: #1e293b;
	}
	}
</style>