<template>
	<view class="reward-container">
		<view class="reward-header">
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

		<view class="search-bar">
			<uni-icons type="search" size="18" color="#94a3b8"></uni-icons>
			<input v-model="keyword" type="text" placeholder="搜索需求、标签或发布者" />
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
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getBountyTaskPageApi } from '../../api/reward'
import { getStoredUserRole, isDesignerRole } from '../../utils/role'

const userRole = ref(getStoredUserRole())
const currentUserId = ref(String((uni.getStorageSync('user_profile') || {}).id || ''))
const isDesigner = computed(() => isDesignerRole(userRole.value))

const tabs = computed(() => {
	if (isDesigner.value) {
		return ['全部', '招募中', '进行中', '待验收', '已完成', '我的接单']
	}
	return ['全部', '招募中', '进行中', '待验收', '已完成', '我的发布']
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

const rewards = ref([])

const statusMap = {
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
		rewards.value = records.map(item => {
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
	}
}

onShow(() => {
	userRole.value = getStoredUserRole()
	currentUserId.value = String((uni.getStorageSync('user_profile') || {}).id || '')
	loadRewards()
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
	if (tab === '招募中') list = list.filter(r => r.status === 'recruiting')
	if (tab === '进行中') list = list.filter(r => r.status === 'in_progress')
	if (tab === '待验收') list = list.filter(r => r.status === 'pending_accept')
	if (tab === '已完成') list = list.filter(r => r.status === 'completed')
	if (tab === '我的发布') list = list.filter(r => r.ownerId === currentUserId.value)
	if (tab === '我的接单') list = list.filter(r => r.bidderId === currentUserId.value)

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
</script>

<style scoped lang="scss">
.reward-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: #f8fafc;
	padding-bottom: 140rpx;
}

.reward-header {
	background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
	padding: 60rpx 40rpx;
	color: #ffffff;
	.title { font-size: 44rpx; font-weight: 700; }
	.desc { font-size: 26rpx; opacity: 0.9; margin-top: 10rpx; }
	.stats {
		display: flex;
		margin-top: 40rpx;
		.stat-item {
			flex: 1;
			text-align: center;
			.val { font-size: 36rpx; font-weight: 700; display: block; }
			.lab { font-size: 22rpx; opacity: 0.8; margin-top: 6rpx; }
		}
	}
}

.search-bar {
	margin: 20rpx 30rpx 0;
	height: 72rpx;
	background-color: #ffffff;
	border-radius: 36rpx;
	display: flex;
	align-items: center;
	padding: 0 24rpx;
	input { flex: 1; margin-left: 16rpx; font-size: 26rpx; }
}

.tab-scroll {
	white-space: nowrap;
	padding: 20rpx 30rpx 0;
	.tab-item {
		display: inline-block;
		margin-right: 30rpx;
		font-size: 26rpx;
		color: #64748b;
		padding-bottom: 10rpx;
		&.active { color: #f59e0b; font-weight: 700; border-bottom: 4rpx solid #f59e0b; }
	}
}

.filter-row {
	padding: 20rpx 30rpx;
	background-color: #ffffff;
	margin-top: 20rpx;
	.filter-group {
		.label { font-size: 24rpx; color: #94a3b8; margin-bottom: 10rpx; display: block; }
		.chips {
			display: flex;
			flex-wrap: wrap;
			gap: 12rpx;
			.chip {
				font-size: 22rpx;
				color: #64748b;
				background-color: #f1f5f9;
				padding: 4rpx 16rpx;
				border-radius: 8rpx;
				&.active { background-color: #fffbeb; color: #d97706; }
			}
		}
	}
	.sort-group {
		margin-top: 20rpx;
		display: flex;
		gap: 20rpx;
		.sort {
			font-size: 24rpx;
			color: #94a3b8;
			&.active { color: #4f46e5; font-weight: 700; }
		}
	}
}

.list-scroll {
	flex: 1;
	padding: 20rpx 30rpx;
}

.empty {
	text-align: center;
	color: #94a3b8;
	margin-top: 120rpx;
}

.reward-card {
	background-color: #ffffff;
	border-radius: 24rpx;
	padding: 30rpx;
	margin-bottom: 24rpx;
	box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.02);
	.card-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		.price { font-size: 36rpx; color: #f59e0b; font-weight: 700; }
		.status {
			font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx;
			&.recruiting { background-color: #fffbeb; color: #d97706; }
			&.in_progress { background-color: #e0e7ff; color: #4f46e5; }
			&.pending_accept { background-color: #fef3c7; color: #d97706; }
			&.completed { background-color: #dcfce7; color: #16a34a; }
		}
	}
	.title { font-size: 30rpx; font-weight: 700; color: #1e293b; margin-top: 20rpx; display: block; }
	.content { font-size: 26rpx; color: #64748b; margin-top: 12rpx; line-height: 1.6; display: block; }
	.tags {
		margin-top: 20rpx;
		display: flex;
		flex-wrap: wrap;
		gap: 12rpx;
		.tag { font-size: 22rpx; color: #94a3b8; background-color: #f1f5f9; padding: 4rpx 16rpx; border-radius: 8rpx; }
	}
	.meta {
		margin-top: 20rpx;
		font-size: 22rpx;
		color: #94a3b8;
		display: flex;
		justify-content: space-between;
	}
	.card-footer {
		margin-top: 24rpx;
		padding-top: 20rpx;
		border-top: 2rpx solid #f8fafc;
		display: flex;
		justify-content: space-between;
		align-items: center;
		.info { font-size: 22rpx; color: #94a3b8; }
		.btns .btn {
			height: 52rpx;
			padding: 0 20rpx;
			font-size: 22rpx;
			border-radius: 26rpx;
			background-color: #4f46e5;
			color: #ffffff;
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
	background-color: #ffffff;
	box-shadow: 0 -4rpx 12rpx rgba(0,0,0,0.05);
	.pub-btn {
		height: 88rpx;
		background-color: #f59e0b;
		color: #ffffff;
		border-radius: 44rpx;
		font-size: 30rpx;
		font-weight: 700;
	}
}
</style>
