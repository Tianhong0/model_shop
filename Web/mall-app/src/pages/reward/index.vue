<template>
	<view class="reward-container">
		<view class="reward-header">
			<view class="header-bg"></view>
			<view class="header-content">
				<view class="title">悬赏定制中心</view>
				<view class="desc">发布需求 · 竞标接单 · 交付验收</view>
				<view class="stats">
					<view class="stat-item">
						<text class="val">{{stats.active}}</text>
						<text class="lab">正在进行</text>
					</view>
					<view class="stat-item">
						<text class="val">￥{{stats.totalBounty}}</text>
						<text class="lab">累计赏金</text>
					</view>
					<view class="stat-item">
						<text class="val">{{stats.successRate}}%</text>
						<text class="lab">解决率</text>
					</view>
				</view>
			</view>
		</view>

		<view class="designer-actions" v-if="isDesigner">
			<view class="action-item" @click="goMyRatings">
				<text class="action-icon">★</text>
				<text class="action-label">我的评价</text>
			</view>
			<view class="action-item" @click="goMyAppeals">
				<text class="action-icon">✉</text>
				<text class="action-label">我的申诉</text>
			</view>
		</view>

		<view class="search-bar">
			<uni-icons type="search" size="18" color="#8a9aaa"></uni-icons>
			<input v-model="keyword" type="text" placeholder="搜索需求、标签或发布者" placeholder-style="color: #8a9aaa;" />
		</view>

		<scroll-view scroll-x class="tab-scroll">
			<view
				v-for="(tab, idx) in tabs"
				:key="idx"
				class="tab-item"
				:class="{ active: activeTab === idx }"
				@click="activeTab = idx"
			>
				{{tab}}
			</view>
		</scroll-view>

		<view class="filter-row">
			<view class="filter-group">
				<text class="label">类型</text>
				<view class="chips">
					<text
						v-for="(item, index) in categories"
						:key="index"
						class="chip"
						:class="{ active: activeCategory === item }"
						@click="activeCategory = item"
					>
						{{item}}
					</text>
				</view>
			</view>
			<view class="sort-group">
				<text
					v-for="(item, index) in sorts"
					:key="index"
					class="sort"
					:class="{ active: activeSort === item.key }"
					@click="activeSort = item.key"
				>
					{{item.name}}
				</text>
			</view>
		</view>

		<scroll-view scroll-y class="list-scroll">
			<view v-if="displayRewards.length === 0" class="empty">
				<text>暂无符合条件的悬赏</text>
			</view>

			<view class="reward-card" v-for="(item, index) in displayRewards" :key="item.id" @click="goDetail(item)">
				<view class="card-header">
					<view class="price">￥{{item.price}}</view>
					<view class="status" :class="item.status">{{item.statusText}}</view>
				</view>
				<view class="title">{{item.title}}</view>
				<view class="content">{{item.content}}</view>
				<view class="tags">
					<text class="tag" v-for="(tag, i) in item.tags" :key="i">{{tag}}</text>
				</view>
				<view class="meta">
					<text>截止 {{item.deadline}}</text>
					<text>{{item.bids}} 人竞标</text>
				</view>
				<view class="card-footer">
					<text class="info">{{item.author}} · {{item.time}}</text>
					<view class="btns">
						<button class="btn" @click.stop="quickAction(item)">{{item.actionText}}</button>
					</view>
				</view>
			</view>
		</scroll-view>

		<view class="publish-bar" v-if="!isDesigner">
			<button class="pub-btn" @click="goPublish">发布我的悬赏需求</button>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getBountyTaskPageApi } from '../../api/reward'
import { getStoredUserRole, isDesignerRole } from '../../utils/role'

const userRole = ref(getStoredUserRole())
const currentUserId = ref(String((uni.getStorageSync('user_profile') || {}).id || ''))
const isDesigner = computed(() => isDesignerRole(userRole.value))

const tabs = computed(() => {
	if (isDesigner.value) {
		return ['全部', '待审核', '待支付', '招募中', '进行中', '待验收', '已完成', '我的接单']
	}
	return ['全部', '待审核', '待支付', '招募中', '进行中', '待验收', '已完成', '我的发布']
})

const categories = ['全部', '模型建模', '结构设计', '打印服务', '渲染展示', '装配改造']
const sorts = [
	{ name: '最新', key: 'latest' },
	{ name: '赏金高', key: 'price' },
	{ name: '热度', key: 'hot' }
]

const activeTab = ref(0)
const activeCategory = ref('全部')
const activeSort = ref('latest')
const keyword = ref('')

watch(activeTab, () => {
	reloadData()
})

const rewards = ref([])

const statusMap = {
	'-1': { status: 'pending_review', statusText: '待审核' },
	0: { status: 'pending_pay', statusText: '待支付托管' },
	1: { status: 'recruiting', statusText: '招募中' },
	2: { status: 'in_progress', statusText: '已选标' },
	3: { status: 'in_progress', statusText: '交付中' },
	4: { status: 'pending_accept', statusText: '待验收' },
	5: { status: 'completed', statusText: '已完成' },
	6: { status: 'closed', statusText: '已关闭' },
	7: { status: 'disputed', statusText: '争议中' }
}

const loadRewards = async () => {
	try {
		const data = await getBountyTaskPageApi({ pageNum: 1, pageSize: 100 })
		const records = data?.records || []
		return records.map(item => {
			const m = statusMap[item.status] || { status: 'recruiting', statusText: '招募中' }
			return {
				id: item.id,
				ownerId: String(item.publisherId || ''),
				bidderId: '',
				title: item.title,
				content: item.description || item.title,
				price: Number(item.finalAmount ?? item.budgetAmount ?? 0),
				status: m.status,
				statusText: m.statusText,
				tags: String(item.tags || '').split(',').map(t => t.trim()).filter(Boolean),
				author: `用户#${item.publisherId}`,
				time: item.createTime || '-',
				timeValue: new Date(item.createTime || 0).getTime(),
				bids: item.bidCount || 0,
				deadline: item.deadlineTime || '-',
				category: item.category || '模型建模'
			}
		})
	} catch (error) {
		uni.showToast({ title: error?.message || '加载悬赏失败', icon: 'none' })
		return []
	}
}

const allRewards = ref([])

const reloadData = async () => {
	// 每次都重新加载所有任务
	allRewards.value = await loadRewards()

	const tab = tabs.value[activeTab.value]
	if (tab === '我的发布') {
		// 我的发布：根据 ownerId 过滤
		rewards.value = allRewards.value.filter(r => r.ownerId === currentUserId.value)
	} else if (tab === '我的接单') {
		// 我的接单：根据 bidderId 过滤（暂时显示全部）
		rewards.value = allRewards.value
	} else {
		// 其他 tab：显示所有任务
		rewards.value = allRewards.value
	}
}

onShow(() => {
	userRole.value = getStoredUserRole()
	currentUserId.value = String((uni.getStorageSync('user_profile') || {}).id || '')
	reloadData()
})

const stats = computed(() => {
	const active = rewards.value.filter(r => r.status === 'recruiting' || r.status === 'in_progress').length
	const totalBounty = rewards.value.reduce((sum, r) => sum + r.price, 0)
	const completed = rewards.value.filter(r => r.status === 'completed').length
	const successRate = rewards.value.length ? Math.round((completed / rewards.value.length) * 100) : 0
	return { active, totalBounty, successRate }
})

const displayRewards = computed(() => {
	let list = rewards.value

	const tab = tabs.value[activeTab.value]
	// "我的发布"已经在reloadData中通过ownerId过滤，"我的接单"暂时显示全部
	if (tab === '待审核') list = list.filter(r => r.status === 'pending_review')
	if (tab === '待支付') list = list.filter(r => r.status === 'pending_pay')
	if (tab === '招募中') list = list.filter(r => r.status === 'recruiting')
	if (tab === '进行中') list = list.filter(r => r.status === 'in_progress')
	if (tab === '待验收') list = list.filter(r => r.status === 'pending_accept')
	if (tab === '已完成') list = list.filter(r => r.status === 'completed')

	if (activeCategory.value !== '全部') {
		list = list.filter(r => r.category === activeCategory.value)
	}

	if (keyword.value.trim()) {
		const key = keyword.value.trim()
		list = list.filter(r => r.title.includes(key) || r.content.includes(key) || r.author.includes(key))
	}

	list = [...list].sort((a, b) => {
		if (activeSort.value === 'price') return b.price - a.price
		if (activeSort.value === 'hot') return b.bids - a.bids
		return b.timeValue - a.timeValue
	})

	return list.map(item => ({
		...item,
		actionText: isDesigner.value && item.status === 'recruiting'
			? '立即竞标'
			: item.status === 'in_progress'
				? '查看进度'
				: '查看详情'
	}))
})

const goPublish = () => {
	if (isDesigner.value) {
		uni.showToast({ title: '设计者不可发布悬赏', icon: 'none' })
		return
	}
	uni.navigateTo({ url: '/pages/reward/publish' })
}

const goDetail = (item) => {
	uni.navigateTo({ url: '/pages/reward/detail?id=' + item.id })
}

const quickAction = (item) => {
	goDetail(item)
}

const goMyRatings = () => {
	uni.navigateTo({ url: '/pages/reward/my-ratings' })
}

const goMyAppeals = () => {
	uni.navigateTo({ url: '/pages/reward/my-appeals' })
}
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
@keyframes breathGlow {
	0%, 100% { box-shadow: 0 0 12rpx rgba(0,191,255,0.15); }
	50% { box-shadow: 0 0 24rpx rgba(0,191,255,0.35); }
}
@keyframes jellyPop {
	0% { transform: scale(1); }
	30% { transform: scale(1.15); }
	50% { transform: scale(0.95); }
	70% { transform: scale(1.05); }
	100% { transform: scale(1); }
}

.reward-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: $bg;
	padding-bottom: 140rpx;
}

.reward-header {
	position: relative;
	overflow: hidden;
	.header-bg {
		position: absolute;
		inset: 0;
		background: $gradient;
	}
	.header-content {
		position: relative;
		padding: 60rpx 40rpx 48rpx;
		color: #ffffff;
	}
	.title { font-size: 44rpx; font-weight: 700; }
	.desc { font-size: 26rpx; opacity: 0.85; margin-top: 10rpx; }
	.stats {
		display: flex;
		margin-top: 36rpx;
		background: rgba(255,255,255,0.15);
		border-radius: 24rpx;
		padding: 24rpx 0;
		backdrop-filter: blur(12px);
		.stat-item {
			flex: 1;
			text-align: center;
			.val { font-size: 36rpx; font-weight: 700; display: block; }
			.lab { font-size: 22rpx; opacity: 0.8; margin-top: 8rpx; display: block; }
		}
	}
}

.designer-actions {
	display: flex;
	gap: 24rpx;
	margin: 28rpx 32rpx 0;
	animation: fadeInUp 0.4s ease-out;
	.action-item {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 12rpx;
		height: 88rpx;
		background-color: $card;
		border-radius: 24rpx;
		box-shadow: $shadow-card;
		&:active { transform: scale(0.96); }
	}
	.action-icon {
		font-size: 32rpx;
		color: $primary;
		animation: jellyPop 0.6s ease-out;
	}
	.action-label { font-size: 28rpx; color: $text-primary; font-weight: 600; }
}

.search-bar {
	margin: 28rpx 32rpx 0;
	height: 76rpx;
	background-color: $card;
	border-radius: 999rpx;
	display: flex;
	align-items: center;
	padding: 0 28rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.35s ease-out;
	input { flex: 1; margin-left: 16rpx; font-size: 28rpx; color: $text-primary; }
}

.tab-scroll {
	white-space: nowrap;
	padding: 24rpx 32rpx 0;
	.tab-item {
		display: inline-block;
		margin-right: 36rpx;
		font-size: 28rpx;
		color: $text-secondary;
		padding-bottom: 14rpx;
		transition: all 0.25s;
		&.active {
			color: $primary;
			font-weight: 700;
			border-bottom: 6rpx solid $primary;
			border-radius: 3rpx;
		}
	}
}

.filter-row {
	padding: 24rpx 32rpx;
	background-color: $card;
	margin: 20rpx 32rpx 0;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	.filter-group {
		.label { font-size: 24rpx; color: $text-muted; margin-bottom: 14rpx; display: block; }
		.chips {
			display: flex;
			flex-wrap: wrap;
			gap: 14rpx;
			.chip {
				font-size: 24rpx;
				color: $text-secondary;
				background-color: $bg;
				padding: 8rpx 24rpx;
				border-radius: 999rpx;
				transition: all 0.2s;
				&.active {
					background-color: rgba(0, 191, 255, 0.12);
					color: $deep;
					font-weight: 600;
				}
			}
		}
	}
	.sort-group {
		margin-top: 20rpx;
		display: flex;
		gap: 28rpx;
		.sort {
			font-size: 24rpx;
			color: $text-muted;
			transition: all 0.2s;
			&.active { color: $primary; font-weight: 700; }
		}
	}
}

.list-scroll {
	flex: 1;
	padding: 24rpx 32rpx;
}

.empty {
	text-align: center;
	color: $text-muted;
	margin-top: 120rpx;
	font-size: 28rpx;
}

.reward-card {
	background-color: $card;
	border-radius: 24rpx;
	padding: 32rpx;
	margin-bottom: 28rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.4s ease-out both;
	&:active { transform: scale(0.98); }
	.card-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		.price { font-size: 36rpx; color: $deep; font-weight: 700; }
		.status {
			font-size: 22rpx;
			padding: 6rpx 20rpx;
			border-radius: 999rpx;
			font-weight: 500;
			animation: breathGlow 2s ease-in-out infinite;
			&.pending_review { background-color: rgba(255, 153, 0, 0.1); color: #ff9900; }
			&.pending_pay { background-color: rgba(255, 77, 109, 0.1); color: $danger; }
			&.recruiting { background-color: rgba(0, 191, 255, 0.1); color: $deep; }
			&.in_progress { background-color: rgba(0, 191, 255, 0.15); color: $primary; }
			&.pending_accept { background-color: rgba(0, 191, 255, 0.1); color: $deep; }
			&.completed {
				background-color: rgba(16, 185, 129, 0.1);
				color: $success;
				animation: none;
			}
			&.closed { background-color: rgba(0,0,0,0.05); color: $text-muted; animation: none; }
		}
	}
	.title { font-size: 30rpx; font-weight: 700; color: $text-primary; margin-top: 20rpx; display: block; }
	.content {
		font-size: 28rpx; color: $text-secondary; margin-top: 12rpx; line-height: 1.6; display: block;
		overflow: hidden; text-overflow: ellipsis;
		display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
	}
	.tags {
		margin-top: 20rpx;
		display: flex;
		flex-wrap: wrap;
		gap: 12rpx;
		.tag {
			font-size: 22rpx; color: $text-muted; background-color: $bg;
			padding: 6rpx 20rpx; border-radius: 999rpx;
		}
	}
	.meta {
		margin-top: 20rpx;
		font-size: 24rpx;
		color: $text-muted;
		display: flex;
		justify-content: space-between;
	}
	.card-footer {
		margin-top: 24rpx;
		padding-top: 20rpx;
		display: flex;
		justify-content: space-between;
		align-items: center;
		.info { font-size: 24rpx; color: $text-muted; }
		.btns .btn {
			height: 56rpx;
			padding: 0 28rpx;
			font-size: 24rpx;
			border-radius: 999rpx;
			background: $gradient;
			color: #ffffff;
			font-weight: 500;
			&:active { transform: scale(0.96); }
		}
	}
}

.publish-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 20rpx 40rpx;
	padding-bottom: calc(env(safe-area-inset-bottom) + 20rpx);
	background: rgba(255,255,255,0.72);
	backdrop-filter: blur(24px);
	box-shadow: 0 -4rpx 24rpx rgba(0,0,0,0.06);
	.pub-btn {
		height: 88rpx;
		background: $gradient;
		color: #ffffff;
		border-radius: 999rpx;
		font-size: 30rpx;
		font-weight: 700;
		&:active { transform: scale(0.96); }
	}
}
</style>
