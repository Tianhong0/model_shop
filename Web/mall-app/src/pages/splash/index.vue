<template>
	<view class="splash-page">
		<view class="splash-overlay" v-if="showOverlay">
			<view class="logo-section">
<!-- 				<image class="logo" src="../../static/logo.png" mode="aspectFit"></image>
				<text class="app-slogan">您的个性化3D打印定制专家</text> -->
			</view>
			<view class="skip-btn" @click="skipSplash">
				<text class="skip-text">{{ countdown > 0 ? `${countdown}s 跳过` : '跳过' }}</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { hasValidLogin } from '../../utils/auth'

const countdown = ref(3)
const showOverlay = ref(true)
let timer = null

const navigateToNext = () => {
	// 检查是否已登录
	if (hasValidLogin()) {
		// 已登录，跳转到首页
		uni.switchTab({
			url: '/pages/index/index',
			fail: () => {
				uni.reLaunch({ url: '/pages/index/index' })
			}
		})
	} else {
		// 未登录，跳转到登录页
		uni.redirectTo({
			url: '/pages/auth/login'
		})
	}
}

const skipSplash = () => {
	if (timer) {
		clearInterval(timer)
		timer = null
	}
	navigateToNext()
}

const startCountdown = () => {
	timer = setInterval(() => {
		countdown.value--
		if (countdown.value <= 0) {
			clearInterval(timer)
			timer = null
			navigateToNext()
		}
	}, 1000)
}

onMounted(() => {
	// 开始倒计时
	startCountdown()
})

onUnmounted(() => {
	if (timer) {
		clearInterval(timer)
		timer = null
	}
})
</script>

<style scoped lang="scss">
.splash-page {
	width: 100vw;
	height: 100vh;
	position: relative;
	overflow: hidden;
	/* 使用 CSS 背景图 */
	background-image: url('../../static/kaiping.png');
	background-size: contain;
	background-position: center;
	background-repeat: no-repeat;
	background-color: #f8f8f8;
}

.splash-overlay {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 2;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: space-between;
	padding: 120rpx 48rpx 80rpx;
	background: linear-gradient(
		180deg,
		rgba(0, 0, 0, 0.1) 0%,
		rgba(0, 0, 0, 0) 30%,
		rgba(0, 0, 0, 0) 70%,
		rgba(0, 0, 0, 0.3) 100%
	);
}

.logo-section {
	display: flex;
	flex-direction: column;
	align-items: center;
	animation: fadeInDown 0.8s ease-out;
}

@keyframes fadeInDown {
	from {
		opacity: 0;
		transform: translateY(-40rpx);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

.logo {
	width: 140rpx;
	height: 140rpx;
	border-radius: 28rpx;
	box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.15);
}

.app-name {
	margin-top: 32rpx;
	font-size: 44rpx;
	font-weight: 700;
	color: #ffffff;
	text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.3);
	letter-spacing: 4rpx;
}

.app-slogan {
	margin-top: 16rpx;
	font-size: 26rpx;
	color: rgba(255, 255, 255, 0.9);
	text-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.3);
}

.skip-btn {
	padding: 16rpx 36rpx;
	background: rgba(255, 255, 255, 0.25);
	border-radius: 999rpx;
	backdrop-filter: blur(10px);
	animation: fadeIn 0.6s ease-out 0.3s both;
}

@keyframes fadeIn {
	from {
		opacity: 0;
	}
	to {
		opacity: 1;
	}
}

.skip-text {
	font-size: 26rpx;
	color: #ffffff;
	font-weight: 500;
	text-shadow: 0 1rpx 4rpx rgba(0, 0, 0, 0.2);
}

.skip-btn:active {
	opacity: 0.8;
	transform: scale(0.96);
}
</style>
