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
                <view class="card-main">
                    <view class="cover-wrap" v-if="item.coverUrl">
                        <image :src="item.coverUrl" class="cover" mode="aspectFill"></image>
                    </view>
                    <view class="card-content">
                        <view class="card-header">
                            <view class="price">￥{{item.price}}</view>
                            <view class="status" :class="item.status">{{item.statusText}}</view>
                        </view>
                        <view class="title">{{item.title}}</view>
                        <view class="content-text">{{item.content}}</view>
                        <view class="tags">
                            <text class="tag" v-for="(tag, i) in item.tags" :key="i">{{tag}}</text>
                        </view>
                        <view class="meta">
                            <text class="meta-item">截止 {{item.deadline}}</text>
                            <text class="meta-item">{{item.bids}} 人竞标</text>
                        </view>
                    </view>
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

const rewards = ref([])
const allRewards = ref([])

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

// 格式化日期辅助函数
const formatDateOnly = (str) => {
    if (!str) return '-'
    // 处理 T22:00:00 这种格式，截取 T 之前的内容
    return str.includes('T') ? str.split('T')[0] : str.split(' ')[0]
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
                title: item.title,
                content: item.description || item.title,
                price: Number(item.finalAmount ?? item.budgetAmount ?? 0),
                status: m.status,
                statusText: m.statusText,
                tags: String(item.tags || '').split(',').map(t => t.trim()).filter(Boolean),
                author: item.publisherNickname || `用户#${item.publisherId}`,
                time: formatTime(item.createTime),
                timeValue: new Date(item.createTime || 0).getTime(),
                bids: item.bidCount || 0,
                // 此处修正：过滤掉时间部分
                deadline: formatDateOnly(item.deadlineTime),
                category: item.category || '模型建模',
                coverUrl: item.coverUrl || ''
            }
        })
    } catch (error) {
        uni.showToast({ title: error?.message || '加载悬赏失败', icon: 'none' })
        return []
    }
}

const reloadData = async () => {
    allRewards.value = await loadRewards()
    const tab = tabs.value[activeTab.value]
    if (tab === '我的发布') {
        rewards.value = allRewards.value.filter(r => r.ownerId === currentUserId.value)
    } else {
        rewards.value = allRewards.value
    }
}

watch(activeTab, () => reloadData())

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
    
    const filterMap = {
        '待审核': 'pending_review',
        '待支付': 'pending_pay',
        '招募中': 'recruiting',
        '进行中': 'in_progress',
        '待验收': 'pending_accept',
        '已完成': 'completed'
    }
    if (filterMap[tab]) list = list.filter(r => r.status === filterMap[tab])

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
        actionText: isDesigner.value && item.status === 'recruiting' ? '立即竞标' : (item.status === 'in_progress' ? '查看进度' : '查看详情')
    }))
})

const goPublish = () => {
    if (isDesigner.value) return uni.showToast({ title: '设计者不可发布悬赏', icon: 'none' })
    uni.navigateTo({ url: '/pages/reward/publish' })
}
const goDetail = (item) => uni.navigateTo({ url: '/pages/reward/detail?id=' + item.id })
const quickAction = (item) => goDetail(item)
const goMyRatings = () => uni.navigateTo({ url: '/pages/reward/my-ratings' })
const goMyAppeals = () => uni.navigateTo({ url: '/pages/reward/my-appeals' })

const formatTime = (timeStr) => {
    if (!timeStr) return '-'
    const now = new Date()
    // 统一处理 ISO 格式
    const cleanTime = timeStr.replace('T', ' ')
    const time = new Date(cleanTime)
    const diff = now.getTime() - time.getTime()
    const minutes = Math.floor(diff / (1000 * 60))
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days < 7) return `${days}天前`
    return timeStr.split('T')[0] || timeStr.split(' ')[0]
}
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
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

.reward-container {
    height: 100vh;
    display: flex;
    flex-direction: column;
    background-color: $bg;
}

.reward-header {
    position: relative;
    .header-bg { position: absolute; inset: 0; background: $gradient; }
    .header-content { position: relative; padding: 60rpx 40rpx 48rpx; color: #fff; }
    .title { font-size: 44rpx; font-weight: 700; }
    .desc { font-size: 26rpx; opacity: 0.85; margin-top: 10rpx; }
    .stats {
        display: flex; margin-top: 36rpx; background: rgba(255,255,255,0.15);
        border-radius: 24rpx; padding: 24rpx 0; backdrop-filter: blur(12px);
        .stat-item {
            flex: 1; text-align: center;
            .val { font-size: 36rpx; font-weight: 700; display: block; }
            .lab { font-size: 22rpx; opacity: 0.8; margin-top: 8rpx; display: block; }
        }
    }
}

.designer-actions {
    display: flex; gap: 24rpx; margin: 28rpx 32rpx 0;
    .action-item {
        flex: 1; display: flex; align-items: center; justify-content: center;
        gap: 12rpx; height: 88rpx; background-color: $card; border-radius: 24rpx;
        box-shadow: $shadow-card; &:active { transform: scale(0.96); }
    }
    .action-icon { font-size: 32rpx; color: $primary; }
    .action-label { font-size: 28rpx; color: $text-primary; font-weight: 600; }
}

.search-bar {
    margin: 28rpx 32rpx 0; height: 76rpx; background-color: $card;
    border-radius: 999rpx; display: flex; align-items: center; padding: 0 28rpx;
    box-shadow: $shadow-card;
    input { flex: 1; margin-left: 16rpx; font-size: 28rpx; color: $text-primary; }
}

.tab-scroll {
    white-space: nowrap; padding: 24rpx 32rpx 0;
    .tab-item {
        display: inline-block; margin-right: 36rpx; font-size: 28rpx;
        color: $text-secondary; padding-bottom: 14rpx; transition: all 0.25s;
        &.active { color: $primary; font-weight: 700; border-bottom: 6rpx solid $primary; }
    }
}

.filter-row {
    padding: 24rpx 32rpx; background-color: $card; margin: 20rpx 32rpx 0;
    border-radius: 24rpx; box-shadow: $shadow-card;
    .filter-group .chips {
        display: flex; flex-wrap: wrap; gap: 14rpx; margin-top: 14rpx;
        .chip {
            font-size: 24rpx; color: $text-secondary; background-color: $bg;
            padding: 8rpx 24rpx; border-radius: 999rpx;
            &.active { background-color: rgba(0, 191, 255, 0.12); color: $deep; font-weight: 600; }
        }
    }
    .sort-group { margin-top: 20rpx; display: flex; gap: 28rpx;
        .sort { font-size: 24rpx; color: $text-muted; &.active { color: $primary; font-weight: 700; } }
    }
}

.list-scroll { flex: 1; padding: 24rpx 32rpx 160rpx; box-sizing: border-box; }

.reward-card {
    background-color: $card; border-radius: 24rpx; padding: 28rpx;
    margin-bottom: 24rpx; box-shadow: $shadow-card;
    animation: fadeInUp 0.4s ease-out both;
    &:active { transform: scale(0.98); }

    .card-main { display: flex; gap: 20rpx; width: 100%; }
    .cover-wrap { flex-shrink: 0; }
    .cover { width: 140rpx; height: 140rpx; border-radius: 16rpx; background: #e2e8f0; }

    .card-content {
        flex: 1; width: 0; display: flex; flex-direction: column;
    }

    .card-header {
        display: flex; justify-content: space-between; align-items: flex-start;
        .price { font-size: 34rpx; color: $deep; font-weight: 700; }
        .status {
            font-size: 20rpx; padding: 4rpx 16rpx; border-radius: 999rpx;
            white-space: nowrap;
            &.pending_review { background: rgba(255, 153, 0, 0.1); color: #ff9900; }
            &.pending_pay { background: rgba(255, 77, 109, 0.1); color: $danger; }
            &.completed { background: rgba(16, 185, 129, 0.1); color: $success; }
            &.recruiting, &.in_progress { background: rgba(0, 191, 255, 0.1); color: $deep; }
        }
    }

    .title {
        font-size: 30rpx; font-weight: 700; color: $text-primary;
        margin-top: 10rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }

    .content-text {
        font-size: 26rpx; color: $text-secondary; margin-top: 8rpx;
        overflow: hidden; text-overflow: ellipsis;
        display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical;
    }

    .tags {
        margin-top: 12rpx; display: flex; flex-wrap: wrap; gap: 8rpx;
        .tag { font-size: 20rpx; color: $text-muted; background: $bg; padding: 4rpx 12rpx; border-radius: 4rpx; }
    }

    .meta {
        margin-top: 12rpx; display: flex; justify-content: space-between;
        font-size: 22rpx; color: $text-muted;
        .meta-item { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 50%; }
    }

    .card-footer {
        margin-top: 24rpx; padding-top: 20rpx; border-top: 2rpx solid #f0f4f7;
        display: flex; justify-content: space-between; align-items: center;
        .info { font-size: 22rpx; color: $text-muted; }
        .btns .btn {
            height: 56rpx; line-height: 56rpx; padding: 0 32rpx; font-size: 24rpx;
            border-radius: 999rpx; background: $gradient; color: #fff;
        }
    }
}

.publish-bar {
    position: fixed; bottom: 0; left: 0; right: 0; padding: 20rpx 40rpx;
    padding-bottom: calc(env(safe-area-inset-bottom) + 20rpx);
    background: rgba(255,255,255,0.85); backdrop-filter: blur(20px);
    .pub-btn { height: 88rpx; background: $gradient; color: #fff; border-radius: 999rpx; font-weight: 700; }
}

.empty { text-align: center; padding-top: 100rpx; color: $text-muted; }
</style>