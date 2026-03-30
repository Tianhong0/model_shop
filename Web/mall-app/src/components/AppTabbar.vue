<template>
	<view class="custom-tabbar">
		<view
			v-for="(item, index) in tabs"
			:key="item.pagePath"
			class="tab-item"
			:class="{ active: selected === index, pulse: pulseIndex === index }"
			@tap="switchTab(item, index)"
		>
			<view class="tab-icon-wrap">
				<u-icon :name="item.icon" size="20" :color="selected === index ? '#00bfff' : '#94a3b8'"></u-icon>
			</view>
			<text class="tab-text">{{ item.text }}</text>
		</view>
		<view class="safe-bottom"></view>
	</view>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

const selected = ref(0)
const pulseIndex = ref(-1)

const tabs = [
	{ pagePath: 'pages/index/index', text: '首页', icon: 'home' },
	{ pagePath: 'pages/mall/list', text: '分类', icon: 'grid' },
	{ pagePath: 'pages/community/index', text: '社区', icon: 'chat' },
	{ pagePath: 'pages/cart/cart', text: '购物车', icon: 'shopping-cart' },
	{ pagePath: 'pages/user/user', text: '我的', icon: 'account' }
]

const syncSelected = () => {
	const pages = getCurrentPages()
	const current = pages[pages.length - 1]
	const route = current?.route || ''
	const idx = tabs.findIndex(item => item.pagePath === route)
	selected.value = idx > -1 ? idx : 0
}

const switchTab = (item, index) => {
	if (!item?.pagePath) return
	pulseIndex.value = index
	setTimeout(() => {
		pulseIndex.value = -1
	}, 280)

	if (selected.value === index) {
		syncSelected()
		return
	}

	const targetUrl = '/' + item.pagePath
	uni.switchTab({
		url: targetUrl,
		success: () => {
			selected.value = index
		},
		fail: (err) => {
			uni.reLaunch({
				url: targetUrl,
				fail: () => {
					uni.showToast({
						title: (err && err.errMsg) || '页面跳转失败',
						icon: 'none'
					})
				}
			})
		}
	})
}

onShow(() => {
	syncSelected()
})

onMounted(() => {
	syncSelected()
})
</script>

<style scoped lang="scss">
.custom-tabbar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	height: calc(116rpx + env(safe-area-inset-bottom));
	padding-top: 14rpx;
	background: rgba(255, 255, 255, 0.82);
	backdrop-filter: blur(24px);
	-webkit-backdrop-filter: blur(24px);
	display: flex;
	justify-content: space-around;
	align-items: flex-start;
	z-index: 999;
	box-shadow: 0 -4rpx 32rpx rgba(0, 0, 0, 0.04);
}

.tab-item {
	width: 20%;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 6rpx;
	color: #94a3b8;
	transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.tab-icon-wrap {
	width: 56rpx;
	height: 56rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 18rpx;
	transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.tab-item.active .tab-icon-wrap {
	background: rgba(0, 191, 255, 0.10);
}

.tab-item.active {
	color: #00bfff;
}

.tab-text {
	font-size: 20rpx;
	line-height: 1;
	font-weight: 400;
}

.tab-item.active .tab-text {
	font-weight: 600;
	color: #00bfff;
}

.pulse {
	animation: tabPulse 0.28s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.safe-bottom {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	height: env(safe-area-inset-bottom);
	background: rgba(255, 255, 255, 0.82);
	pointer-events: none;
}

@keyframes tabPulse {
	0% { transform: scale(1); }
	40% { transform: scale(1.12); }
	100% { transform: scale(1); }
}
</style>
