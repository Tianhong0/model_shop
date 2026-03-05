<template>
	<view class="custom-tabbar">
		<view
			v-for="(item, index) in tabs"
			:key="item.pagePath"
			class="tab-item"
			:class="{ active: selected === index, pulse: pulseIndex === index }"
			@tap="switchTab(item, index)"
		>
			<u-icon :name="item.icon" size="20" :color="selected === index ? '#111318' : '#94a3b8'"></u-icon>
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
	}, 220)

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
	height: calc(108rpx + env(safe-area-inset-bottom));
	padding-top: 12rpx;
	background: #ffffff;
	border-top: 1rpx solid #f0f2f4;
	display: flex;
	justify-content: space-around;
	align-items: flex-start;
	z-index: 999;
}

.tab-item {
	width: 20%;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 6rpx;
	color: #94a3b8;
}

.tab-item.active {
	color: #111318;
}

.tab-text {
	font-size: 20rpx;
	line-height: 1;
	font-weight: 400;
}

.tab-item.active .tab-text {
	font-weight: 500;
}

.pulse {
	animation: pulse 0.22s ease-out;
}

.safe-bottom {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	height: env(safe-area-inset-bottom);
	background: #ffffff;
	pointer-events: none;
}

@keyframes pulse {
	0% { transform: scale(1); }
	50% { transform: scale(1.08); }
	100% { transform: scale(1); }
}
</style>