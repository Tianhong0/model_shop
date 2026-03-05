<template>
	<view class="event-detail-container">
		<image :src="event.banner" class="banner" mode="aspectFill"></image>
		
		<view class="content-section container">
			<view class="event-header card">
				<view class="status-tag" :class="event.status">{{statusText}}</view>
				<text class="title">{{event.title}}</text>
				<view class="meta">
					<view class="item">
						<uni-icons type="calendar" size="14" color="#94a3b8"></uni-icons>
						<text>{{event.startTime}} ~ {{event.endTime}}</text>
					</view>
					<view class="item">
						<uni-icons type="location" size="14" color="#94a3b8"></uni-icons>
						<text>{{event.location}}</text>
					</view>
				</view>
			</view>

			<view class="detail-card card">
				<view class="sec-title">活动介绍</view>
				<text class="desc">{{event.description}}</text>
				
				<view class="sec-title">奖励设置</view>
				<view class="reward-list">
					<view class="reward-item" v-for="(reward, idx) in event.rewards" :key="idx">
						<text class="rank">{{reward.rank}}</text>
						<text class="prize">{{reward.prize}}</text>
					</view>
				</view>

				<view class="sec-title">参赛要求</view>
				<text class="desc">{{event.rules}}</text>
			</view>
		</view>

		<view class="bottom-bar">
			<view class="share-btn">
				<uni-icons type="redo" size="20" color="#64748b"></uni-icons>
				<text>分享</text>
			</view>
			<button class="join-btn" :disabled="event.status !== 'ongoing'" @click="handleJoin">
				{{event.status === 'ongoing' ? '立即报名' : '报名已结束'}}
			</button>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const event = ref({
	id: 501,
	title: '第一届“创意无限”3D打印设计大赛',
	banner: 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=800',
	status: 'ongoing',
	startTime: '2026-01-01',
	endTime: '2026-03-31',
	location: '线上投稿 / 平台社区',
	description: '本次大赛旨在发掘最具创意的 3D 打印设计作品。无论是功能性构件、艺术装饰品还是极客工具，只要是原创设计，均可投稿参赛。我们将邀请行业专家进行评审，获奖作品将获得丰厚奖品及平台首页展示机会。',
	rewards: [
		{ rank: '一等奖 (1名)', prize: 'Creality K1 Max 打印机 + ￥5000 奖金' },
		{ rank: '二等奖 (3名)', prize: '高精度光固化打印机 + ￥2000 奖金' },
		{ rank: '三等奖 (10名)', prize: '5kg 优质 PLA 耗材套装' }
	],
	rules: '1. 参赛作品必须为原创，禁止抄袭；\n2. 需提交完整的 STL 文件及设计说明（不少于200字）；\n3. 需上传至少 3 张不同角度的成品实拍图或高质量渲染图。'
})

const statusText = computed(() => {
	const map = { ongoing: '进行中', ended: '已结束', upcoming: '即将开始' }
	return map[event.status] || '未知'
})

onLoad((options) => {
	if (options.id == '502') {
		event.value = {
			id: 502,
			title: '线下沙龙：成都站模型展示会',
			banner: 'https://images.unsplash.com/photo-1540575861501-7ad05823c95b?w=800',
			status: 'ended',
			startTime: '2026-01-20',
			endTime: '2026-01-21',
			location: '四川省成都市武侯区科华北路 100 号',
			description: '汇聚成都本地的 3D 打印爱好者，分享最新的打印作品，交流切片技巧与机器调优经验。现场提供免费耗材试用及礼品抽奖。',
			rewards: [
				{ rank: '参与奖', prize: '精美 3D 打印纪念品一份' },
				{ rank: '分享达人', prize: '500 平台积分' }
			],
			rules: '1. 携带至少一件自己的 3D 打印作品到场展示；\n2. 遵守会场秩序，爱护展示器材。'
		}
	}
})

const handleJoin = () => {
	uni.showModal({
		title: '报名确认',
		content: '确定报名参加《' + event.value.title + '》吗？',
		success: (res) => {
			if (res.confirm) {
				uni.showToast({ title: '报名成功', icon: 'success' })
			}
		}
	})
}
</script>

<style scoped lang="scss">
.event-detail-container {
	min-height: 100vh;
	background-color: #f8fafc;
	padding-bottom: 120rpx;
}

.banner {
	width: 100%;
	height: 450rpx;
}

.content-section {
	margin-top: -60rpx;
	position: relative;
}

.event-header {
	padding: 40rpx;
	.status-tag {
		display: inline-block;
		padding: 4rpx 20rpx;
		border-radius: 8rpx;
		font-size: 22rpx;
		margin-bottom: 20rpx;
		&.ongoing { background-color: #dcfce7; color: #16a34a; }
		&.ended { background-color: #f1f5f9; color: #94a3b8; }
	}
	.title { font-size: 36rpx; font-weight: 700; color: #1e293b; display: block; line-height: 1.4; }
	.meta {
		margin-top: 30rpx;
		.item {
			display: flex;
			align-items: center;
			margin-bottom: 12rpx;
			text { font-size: 26rpx; color: #64748b; margin-left: 12rpx; }
		}
	}
}

.detail-card {
	margin-top: 24rpx;
	padding: 40rpx;
	.sec-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1e293b;
		margin-bottom: 20rpx;
		position: relative;
		padding-left: 20rpx;
		&::before {
			content: '';
			position: absolute;
			left: 0;
			top: 50%;
			transform: translateY(-50%);
			width: 6rpx;
			height: 28rpx;
			background-color: #4f46e5;
			border-radius: 3rpx;
		}
		&:not(:first-child) { margin-top: 40rpx; }
	}
	.desc { font-size: 28rpx; color: #475569; line-height: 1.6; white-space: pre-wrap; }
	.reward-list {
		.reward-item {
			background-color: #f8fafc;
			padding: 20rpx;
			border-radius: 12rpx;
			margin-bottom: 16rpx;
			display: flex;
			justify-content: space-between;
			align-items: center;
			.rank { font-size: 26rpx; font-weight: 700; color: #4f46e5; }
			.prize { font-size: 26rpx; color: #1e293b; }
		}
	}
}

.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	height: 100rpx;
	background-color: #ffffff;
	display: flex;
	align-items: center;
	padding: 0 30rpx;
	box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.05);
	.share-btn {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin-right: 40rpx;
		text { font-size: 20rpx; color: #64748b; margin-top: 4rpx; }
	}
	.join-btn {
		flex: 1;
		height: 80rpx;
		background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
		color: #ffffff;
		border-radius: 40rpx;
		font-size: 28rpx;
		font-weight: 700;
		display: flex;
		align-items: center;
		justify-content: center;
		&[disabled] { opacity: 0.6; background: #cbd5e1; }
	}
}
</style>
