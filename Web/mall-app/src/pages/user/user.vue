<template>
	<view class="user-container">
		<view class="header-bg"></view>
		
		<view class="user-card container">
			<view class="profile-section">
				<image :src="userInfo.avatar" class="avatar" @click="uni.navigateTo({ url: '/pages/user/profile' })"></image>
				<view class="info" @click="uni.navigateTo({ url: '/pages/user/profile' })">
					<text class="nickname">{{userInfo.nickname}}</text>
					<text class="role">{{roleLabel}}</text>
					<text class="id">ID: {{userInfo.id}}</text>
				</view>
				<view class="settings" @click="uni.navigateTo({ url: '/pages/user/settings' })">
					<uni-icons type="settings" size="24" color="#fff"></uni-icons>
				</view>
			</view>

			<view class="stat-row">
				<view class="stat-item" @click="uni.navigateTo({ url: '/pages/user/wallet' })">
					<text class="val">￥{{ walletAmount }}</text>
					<text class="lab">余额</text>
				</view>
				<view class="stat-item" @click="goFavoriteModels">
					<text class="val">{{ favoriteCount }}</text>
					<text class="lab">收藏模型</text>
				</view>
				<view class="stat-item" @click="goPrintTasks">
					<text class="val">{{ printTaskCount }}</text>
					<text class="lab">打印任务</text>
				</view>
				<view class="stat-item" @click="uni.navigateTo({ url: '/pages/user/points' })">
					<text class="val">{{ pointAmount }}</text>
					<text class="lab">积分</text>
				</view>
			</view>
		</view>

		<!-- 订单状态 -->
		<view class="order-section container">
			<view class="sec-card card">
				<view class="sec-header" @click="goOrderList">
					<text class="t">我的订单</text>
					<text class="all">全部订单 ></text>
				</view>
				<view class="order-grid">
					<view class="order-item" v-for="(item, index) in orderStatus" :key="index" @click="goOrderList(item)">
						<view class="icon-wrap">
							<uni-icons :type="item.icon" size="28" color="#475569"></uni-icons>
							<view v-if="item.badge > 0" class="badge">{{ item.badge > 99 ? '99+' : item.badge }}</view>
						</view>
						<text>{{item.name}}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 常用功能 -->
		<view class="menu-section container">
			<view class="menu-card card">
				<view class="menu-item" v-for="(menu, idx) in menus" :key="idx" @click="goMenu(menu)">
					<uni-icons :type="menu.icon" size="20" color="#4f46e5"></uni-icons>
					<text class="menu-name">{{menu.name}}</text>
					<uni-icons type="right" size="14" color="#cbd5e1"></uni-icons>
				</view>
			</view>
		</view>

		<view class="logout-btn container">
			<button class="btn" @click="handleLogout">退出登录</button>
		</view>

		<!-- #ifdef APP-PLUS -->
		<AppTabbar />
		<!-- #endif -->
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { doLogout, ensureLoginOrRedirect } from '../../utils/auth'
import { getStoredUserRole, isDesignerRole } from '../../utils/role'
import { getWalletAccountApi } from '../../api/wallet'
import { getPointAccountApi } from '../../api/point'
import { getMyFavoriteModelIdsApi } from '../../api/model'
import { getMyAfterSaleListApi, getMyOrdersApi } from '../../api/order'
// #ifdef APP-PLUS
import AppTabbar from '../../components/AppTabbar.vue'
// #endif

const userInfo = ref({
	nickname: 'Admin',
	avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
	id: '3D_88889999',
	role: 'user'
})

const walletAmount = ref('0.00')
const pointAmount = ref(0)
const favoriteCount = ref(0)
const printTaskCount = ref(0)

const userRole = ref('user')
const roleLabel = computed(() => (isDesignerRole(userRole.value) ? '设计者' : '普通用户'))

onShow(() => {
	if (!ensureLoginOrRedirect()) return

	const saved = uni.getStorageSync('user_profile')
	const role = getStoredUserRole()
	if (saved) {
		userInfo.value.nickname = saved.nickname
		userInfo.value.avatar = saved.avatar
		userInfo.value.id = saved.id || userInfo.value.id
		userInfo.value.role = role
		uni.setStorageSync('user_profile', {
			...saved,
			role
		})
	}
	userRole.value = role
	loadOverview()
})

const loadOverview = async () => {
	await Promise.all([loadAssetOverview(), loadOrderOverview()])
}

const getPageTotal = (pageData) => {
	const total = Number(pageData?.total)
	if (Number.isFinite(total) && total >= 0) return total
	const records = Array.isArray(pageData?.records) ? pageData.records : []
	return records.length
}

const loadAssetOverview = async () => {
	try {
		const [wallet, point, favoriteIds] = await Promise.all([
			getWalletAccountApi(),
			getPointAccountApi(),
			getMyFavoriteModelIdsApi()
		])
		walletAmount.value = Number(wallet?.availableBalance || 0).toFixed(2)
		pointAmount.value = Number(point?.availablePoints || 0)
		favoriteCount.value = Array.isArray(favoriteIds) ? favoriteIds.length : 0
	} catch (_) {
		// 资产数据加载失败时不阻塞页面
	}
}

const orderBadge = ref({
	waitPay: 0,
	waitReceive: 0,
	finished: 0,
	afterSale: 0
})

const orderStatus = computed(() => ([
	{ name: '待付款', icon: 'wallet', badge: orderBadge.value.waitPay, status: 1 },
	{ name: '待收货', icon: 'shop', badge: orderBadge.value.waitReceive, status: 2 },
	{ name: '已完成', icon: 'checkbox', badge: orderBadge.value.finished, status: 3 },
	{ name: '售后', icon: 'help', badge: orderBadge.value.afterSale, path: '/pages/user/after-sale-list' }
]))

const loadOrderOverview = async () => {
	try {
		const [waitPayData, producingData, waitReceiveData, finishedData, afterSaleData] = await Promise.all([
			getMyOrdersApi({ pageNum: 1, pageSize: 1, orderStatus: 0 }),
			getMyOrdersApi({ pageNum: 1, pageSize: 1, orderStatus: 1 }),
			getMyOrdersApi({ pageNum: 1, pageSize: 1, orderStatus: 2 }),
			getMyOrdersApi({ pageNum: 1, pageSize: 1, orderStatus: 3 }),
			getMyAfterSaleListApi({ pageNum: 1, pageSize: 1 })
		])

		const waitPay = getPageTotal(waitPayData)
		const producing = getPageTotal(producingData)
		const waitReceive = getPageTotal(waitReceiveData)
		const finished = getPageTotal(finishedData)
		const afterSale = getPageTotal(afterSaleData)

		printTaskCount.value = producing
		orderBadge.value = {
			waitPay,
			waitReceive: producing + waitReceive,
			finished,
			afterSale
		}
	} catch (_) {
		printTaskCount.value = 0
		orderBadge.value = {
			waitPay: 0,
			waitReceive: 0,
			finished: 0,
			afterSale: 0
		}
	}
}

const menus = computed(() => {
	const isDesigner = isDesignerRole(userRole.value)
	const list = [
		{ name: isDesigner ? '悬赏任务广场' : '我的悬赏任务', icon: 'fire-filled', path: '/pages/reward/index' },
		{ name: '我的帖子', icon: 'chat-filled', path: '/pages/community/my-posts' },
		{ name: '我的互动', icon: 'heart-filled', path: '/pages/community/my-interactions' },
		{ name: '模型上传记录', icon: 'cloud-upload-filled', path: '/pages/user/models', role: 'designer' },
		{ name: '我的清单', icon: 'list', path: '/pages/user/params', role: 'user' },
		{ name: '在线客服', icon: 'headphones' },
		{ name: '关于平台', icon: 'info-filled' }
	]
	return list.filter(item => !item.role || item.role === userRole.value)
})

const goMenu = (menu) => {
	if (menu.path) {
		if (menu.path.includes('index') && (menu.path.includes('community') || menu.path.includes('mall'))) {
			uni.switchTab({ url: menu.path })
		} else {
			uni.navigateTo({ url: menu.path })
		}
	} else {
		uni.showToast({ title: menu.name + '功能演示中', icon: 'none' })
	}
}

const goOrderList = (item = {}) => {
	if (item.path) {
		uni.navigateTo({ url: item.path })
		return
	}
	uni.navigateTo({
		url: '/pages/user/orders?status=' + (item.status || 0)
	})
}

const goFavoriteModels = () => {
	uni.navigateTo({ url: '/pages/user/favorite-models' })
}

const goPrintTasks = () => {
	uni.navigateTo({ url: '/pages/user/orders?status=2' })
}

const handleLogout = () => {
	uni.showModal({
		title: '提示',
		content: '确定要退出登录吗？',
		success: async (res) => {
			if (!res.confirm) return
			uni.showLoading({ title: '正在退出...' })
			await doLogout()
			uni.hideLoading()
			uni.reLaunch({
				url: '/pages/auth/login'
			})
			
		}
	})
}
</script>

<style scoped lang="scss">
.user-container {
	min-height: 100vh;
	background-color: #f8fafc;
}

.header-bg {
	height: 400rpx;
	background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
}

.user-card {
	position: relative;
	padding-top: 100rpx;
}

.profile-section {
	display: flex;
	align-items: center;
	.avatar {
		width: 120rpx;
		height: 120rpx;
		border-radius: 60rpx;
		border: 4rpx solid #ffffff;
		background-color: #ffffff;
	}
	.info {
		margin-left: 24rpx;
		color: #ffffff;
		.nickname { font-size: 36rpx; font-weight: 700; display: block; }
		.role { font-size: 22rpx; opacity: 0.9; margin-top: 6rpx; display: block; }
		.id { font-size: 24rpx; opacity: 0.8; margin-top: 6rpx; display: block; }
	}
	.settings {
		margin-left: auto;
	}
}

.stat-row {
	display: flex;
	margin-top: 40rpx;
	.stat-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		.val { font-size: 32rpx; font-weight: 700; color: #ffffff; }
		.lab { font-size: 22rpx; color: #ffffff; opacity: 0.8; margin-top: 8rpx; }
	}
}

.order-section {
	margin-top: -10rpx;
	.sec-card {
		padding: 30rpx;
	}
	.sec-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 30rpx;
		.t { font-size: 28rpx; font-weight: 700; color: #1e293b; }
		.all { font-size: 22rpx; color: #94a3b8; }
	}
}

.order-grid {
	display: flex;
	justify-content: space-between;
	.order-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		.icon-wrap {
			position: relative;
			margin-bottom: 12rpx;
			.badge {
				position: absolute;
				top: -10rpx;
				right: -10rpx;
				background-color: #ef4444;
				color: #ffffff;
				font-size: 18rpx;
				padding: 2rpx 10rpx;
				border-radius: 20rpx;
			}
		}
		text { font-size: 24rpx; color: #475569; }
	}
}

.menu-section {
	margin-top: -10rpx;
	.menu-card {
		padding: 10rpx 30rpx;
	}
	.menu-item {
		height: 100rpx;
		display: flex;
		align-items: center;
		border-bottom: 2rpx solid #f1f5f9;
		&:last-child { border-bottom: none; }
		.menu-name {
			flex: 1;
			margin-left: 20rpx;
			font-size: 28rpx;
			color: #1e293b;
		}
	}
}

.logout-btn {
	margin-top: 40rpx;
	padding-bottom: 60rpx;
	.btn {
		height: 90rpx;
		background-color: #ffffff;
		color: #ef4444;
		border-radius: 24rpx;
		font-size: 30rpx;
		font-weight: 600;
		border: 2rpx solid #fee2e2;
	}
}
</style>
