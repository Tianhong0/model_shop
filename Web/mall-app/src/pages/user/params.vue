<template>
	<view class="list-container" v-if="canAccess">
		<view class="header-card card">
			<view class="title-row">
				<view>
					<view class="title">我的清单</view>
					<view class="desc">收藏模型 / 待购清单 / 已下单 / 清单分享</view>
				</view>
				<button class="share-btn" @click="shareCurrentList">{{currentList && currentList.isPublic ? '更新分享' : '分享清单'}}</button>
			</view>
			<view class="summary">
				<view class="item">
					<text class="val">{{summary.favorites}}</text>
					<text class="lab">收藏模型</text>
				</view>
				<view class="item">
					<text class="val">{{summary.pending}}</text>
					<text class="lab">待购清单</text>
				</view>
				<view class="item">
					<text class="val">{{summary.ordered}}</text>
					<text class="lab">已下单</text>
				</view>
			</view>
			<view class="list-switch">
				<view
					v-for="list in listSets"
					:key="list.id"
					class="switch-item"
					:class="{ active: activeListId === list.id }"
					@click="switchList(list.id)"
				>
					<text class="switch-name">{{list.name}}</text>
					<text class="switch-count">{{list.items.length}}件</text>
				</view>
			</view>
		</view>

		<view class="list-meta card" v-if="currentList">
			<view class="meta-row">
				<text class="meta-title">{{currentList.name}}</text>
				<view class="meta-tags">
					<text class="tag" v-for="tag in currentList.tags" :key="tag">#{{tag}}</text>
				</view>
			</view>
			<text class="meta-desc">{{currentList.desc}}</text>
			<view class="meta-stats">
				<view class="stat">
					<text class="val">{{currentList.views}}</text>
					<text class="lab">浏览</text>
				</view>
				<view class="stat">
					<text class="val">{{currentList.likes}}</text>
					<text class="lab">喜欢</text>
				</view>
				<view class="stat">
					<text class="val">{{currentList.shares}}</text>
					<text class="lab">分享</text>
				</view>
			</view>
			<view class="meta-actions">
				<button class="btn primary" @click="addListToCart">一键加入购物车</button>
				<button class="btn ghost" @click="togglePublic">{{currentList.isPublic ? '设为私密' : '设为公开'}}</button>
			</view>
		</view>

		<view class="tabs">
			<view
				v-for="(tab, idx) in tabs"
				:key="idx"
				class="tab"
				:class="{ active: activeTab === idx }"
				@click="activeTab = idx"
			>
				{{tab}}
			</view>
		</view>

		<view class="list-section">
			<view class="empty" v-if="displayList.length === 0">
				<uni-icons type="cart" size="48" color="#cbd5e1"></uni-icons>
				<text>暂无清单内容</text>
			</view>
			<view class="list-card card" v-for="item in displayList" :key="item.id">
				<image :src="item.image" class="cover" mode="aspectFill"></image>
				<view class="info">
					<view class="row">
						<text class="name">{{item.name}}</text>
						<text class="tag" :class="item.type">{{typeMap[item.type]}}</text>
					</view>
					<text class="params">{{item.params}}</text>
					<view class="price-row">
						<text class="price">￥{{item.price}}</text>
						<text class="time">{{item.time}}</text>
					</view>
					<view class="actions">
						<button class="btn" @click="goDetail(item)">查看详情</button>
						<button v-if="item.type === 'pending'" class="btn primary" @click="addToCart(item)">加入购物车</button>
						<button v-if="item.type === 'favorite'" class="btn primary" @click="moveToPending(item)">加入待购</button>
						<button class="btn ghost" @click="removeItem(item)">移除</button>
					</view>
				</view>
			</view>
		</view>

		<view class="mall-section card">
			<view class="sec-title">商城已有商品</view>
			<view class="mall-list">
				<view class="mall-item" v-for="goods in mallGoods" :key="goods.id">
					<image :src="goods.image" class="mall-cover" mode="aspectFill"></image>
					<view class="mall-info">
						<text class="mall-name">{{goods.name}}</text>
						<text class="mall-params">{{goods.params}}</text>
						<view class="mall-row">
							<text class="mall-price">￥{{goods.price}}</text>
							<button class="mall-btn" :class="{ disabled: isInCurrentList(goods.id) }" @click="addMallItem(goods)">
								{{isInCurrentList(goods.id) ? '已添加' : '添加到清单'}}
							</button>
						</view>
					</view>
				</view>
			</view>
		</view>

		<view class="share-section card" v-if="shareRecords.length">
			<view class="sec-title">我的分享</view>
			<view class="share-card" v-for="record in shareRecords" :key="record.id">
				<image :src="record.cover" class="share-cover" mode="aspectFill"></image>
				<view class="share-info">
					<view class="share-row">
						<text class="share-title">{{record.title}}</text>
						<text class="share-status" :class="record.status">{{record.status}}</text>
					</view>
					<text class="share-desc">{{record.desc}}</text>
					<view class="share-meta">
						<text>{{record.count}}件 · {{record.sharedAt}}</text>
						<text>{{record.platform}} · 浏览{{record.views}} · 喜欢{{record.likes}}</text>
					</view>
					<view class="share-actions">
						<button class="btn" @click="copyShareLink(record)">复制链接</button>
						<button class="btn ghost" @click="removeShare(record.id)">取消分享</button>
					</view>
				</view>
			</view>
		</view>

		<view class="create-mask" v-if="showCreateForm">
			<view class="create-panel card">
				<view class="panel-title">新建清单</view>
				<input class="panel-input" v-model="newListName" placeholder="请输入清单名称" />
				<textarea class="panel-textarea" v-model="newListDesc" placeholder="请输入清单简介"></textarea>
				<view class="panel-actions">
					<button class="btn ghost" @click="cancelCreate">取消</button>
					<button class="btn primary" @click="confirmCreate">创建</button>
				</view>
			</view>
		</view>

		<view class="bottom-bar">
			<button class="action-btn" @click="openCreateForm">新建清单</button>
		</view>
	</view>
	<view class="blocked card" v-else>
		<text class="blocked-title">当前角色无权限使用清单功能</text>
		<text class="blocked-desc">清单分享仅对普通用户开放。</text>
		<button class="back-btn" @click="uni.navigateBack()">返回</button>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyFavoriteModelsApi, toggleModelFavoriteApi } from '../../api/model'
import { isDesignerRole } from '../../utils/role'

const userRole = ref(uni.getStorageSync('user_role') || 'user')
const canAccess = computed(() => !isDesignerRole(userRole.value))

const tabs = ['全部', '收藏模型', '待购清单', '已下单']
const activeTab = ref(0)

const typeMap = {
	favorite: '收藏',
	pending: '待购',
	ordered: '已下单'
}

const listSets = ref([
	{
		id: 'list_1',
		name: '桌面整理清单',
		desc: '办公桌与学习桌整理必备模型组合，适合一键分享。',
		tags: ['桌面', '收纳', '效率'],
		views: 128,
		likes: 36,
		shares: 2,
		isPublic: true,
		items: [
			{
				id: 101,
				name: '蒸汽朋克猫',
				params: '光敏树脂 / 复古铜 / 0.1mm',
				price: '199.00',
				image: 'https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=300',
				time: '2026-01-28',
				type: 'favorite'
			},
			{
				id: 102,
				name: '几何抽象花瓶',
				params: 'PLA / 象牙白 / 0.2mm',
				price: '89.00',
				image: 'https://images.unsplash.com/photo-1601121141461-9d6647bca1ed?w=300',
				time: '2026-01-27',
				type: 'pending'
			}
		]
	},
	{
		id: 'list_2',
		name: '家居美学清单',
		desc: '温柔治愈风的家居装饰组合，适合摆件与花器。',
		tags: ['家居', '美学', '装饰'],
		views: 98,
		likes: 22,
		shares: 1,
		isPublic: false,
		items: [
			{
				id: 201,
				name: '桌面理线器套装',
				params: 'PLA / 深空灰 / 0.2mm',
				price: '120.00',
				image: 'https://images.unsplash.com/photo-1555664424-778a1e5e1b48?w=300',
				time: '2026-01-25',
				type: 'ordered'
			}
		]
	},
	{
		id: 'list_3',
		name: '创意礼物清单',
		desc: '适合节日或纪念日的创意模型清单。',
		tags: ['礼物', '创意', '手作'],
		views: 76,
		likes: 18,
		shares: 0,
		isPublic: false,
		items: []
	}
])

const mallGoods = ref([
	{
		id: 501,
		name: '极简手机支架',
		params: 'PLA / 磨砂黑 / 0.2mm',
		price: '39.00',
		image: 'https://images.unsplash.com/photo-1519710164239-da123dc03ef4?w=300'
	},
	{
		id: 502,
		name: '几何摆件套装',
		params: 'PLA / 雾灰 / 0.2mm',
		price: '59.00',
		image: 'https://images.unsplash.com/photo-1555664424-778a1e5e1b48?w=300'
	},
	{
		id: 503,
		name: '壁挂钥匙盒',
		params: 'PLA / 奶白 / 0.2mm',
		price: '69.00',
		image: 'https://images.unsplash.com/photo-1601121141461-9d6647bca1ed?w=300'
	}
])

const activeListId = ref(listSets.value[0]?.id || '')
const shareRecords = ref([])
const showCreateForm = ref(false)
const newListName = ref('')
const newListDesc = ref('')
const favoriteRecords = ref([])
const favoriteTotal = ref(0)

const normalizeSize = (value) => {
	const raw = String(value || '').trim()
	if (!raw) return '--*--*--'
	const normalized = raw
		.replace(/[xX×\*]/g, '*')
		.split('*')
		.map(part => part.trim())
		.filter(Boolean)
	if (normalized.length !== 3) {
		return raw.replace(/[xX×]/g, '*')
	}
	return normalized.join('*')
}

const normalizeVolume = (value) => {
	if (value == null || value === '') return '--'
	const volume = Number(value)
	if (!Number.isFinite(volume) || volume <= 0) return '--'
	return volume >= 1000 ? volume.toFixed(0) : volume.toFixed(2)
}

const favoriteDisplayList = computed(() => {
	return favoriteRecords.value.map(record => ({
		id: `fav_${record.id}`,
		modelId: record.id,
		name: record.modelName,
		params: `尺寸 ${normalizeSize(record.baseSize)} / 体积 ${normalizeVolume(record.baseVolume)}mm³`,
		price: Number(record.basePrice || 0).toFixed(2),
		image: record.mainImageUrl || 'https://images.unsplash.com/photo-1519710164239-da123dc03ef4?w=300',
		time: '',
		type: 'favorite',
		remote: true
	}))
})

const currentList = computed(() => {
	return listSets.value.find(item => item.id === activeListId.value) || listSets.value[0]
})

const allItems = computed(() => {
	return listSets.value.flatMap(item => item.items)
})

const summary = computed(() => {
	return {
		favorites: favoriteTotal.value,
		pending: allItems.value.filter(i => i.type === 'pending').length,
		ordered: allItems.value.filter(i => i.type === 'ordered').length
	}
})

const displayList = computed(() => {
	if (activeTab.value === 1) {
		return favoriteDisplayList.value
	}
	const items = currentList.value?.items || []
	if (activeTab.value === 0) return items
	if (activeTab.value === 2) return items.filter(i => i.type === 'pending')
	return items.filter(i => i.type === 'ordered')
})

const loadFavoriteModels = async () => {
	try {
		const data = await getMyFavoriteModelsApi({ pageNum: 1, pageSize: 200 })
		favoriteRecords.value = Array.isArray(data?.records) ? data.records : []
		favoriteTotal.value = Number(data?.total || favoriteRecords.value.length)
	} catch (_) {
		favoriteRecords.value = []
		favoriteTotal.value = 0
	}
}

const switchList = (id) => {
	activeListId.value = id
	activeTab.value = 0
}

const isInCurrentList = (id) => {
	return currentList.value?.items.some(item => item.id === id)
}

const addMallItem = (goods) => {
	const list = currentList.value
	if (!list) return
	if (list.items.find(item => item.id === goods.id)) {
		uni.showToast({ title: '已在清单中', icon: 'none' })
		return
	}
	list.items.push({
		id: goods.id,
		name: goods.name,
		params: goods.params,
		price: goods.price,
		image: goods.image,
		time: formatDate(new Date()),
		type: 'pending'
	})
	uni.showToast({ title: '已添加到清单', icon: 'success' })
}

const putCartItem = (item, silent = false) => {
	const cart = uni.getStorageSync('cart_list') || []
	const existing = cart.find(c => c.id === item.id)
	if (existing) {
		existing.num += 1
	} else {
		cart.push({
			id: item.id,
			name: item.name,
			params: item.params,
			price: item.price,
			image: item.image,
			num: 1,
			selected: true
		})
	}
	uni.setStorageSync('cart_list', cart)
	if (!silent) {
		uni.showToast({ title: '已加入购物车', icon: 'success' })
	}
}

const addToCart = (item) => {
	putCartItem(item)
}

const addListToCart = () => {
	const items = currentList.value?.items || []
	if (items.length === 0) {
		uni.showToast({ title: '清单为空', icon: 'none' })
		return
	}
	items.forEach(item => putCartItem(item, true))
	uni.showToast({ title: '清单已加入购物车', icon: 'success' })
}

const moveToPending = (item) => {
	if (item?.remote) {
		const list = currentList.value
		if (!list) return
		const exists = list.items.find(i => i.id === item.modelId)
		if (!exists) {
			list.items.push({
				id: item.modelId,
				name: item.name,
				params: item.params,
				price: item.price,
				image: item.image,
				time: formatDate(new Date()),
				type: 'pending'
			})
		}
		uni.showToast({ title: '已加入待购清单', icon: 'success' })
		return
	}
	const target = currentList.value?.items.find(i => i.id === item.id)
	if (target) {
		target.type = 'pending'
		uni.showToast({ title: '已加入待购清单', icon: 'success' })
	}
}

const removeItem = (item) => {
	if (item?.remote && item?.modelId) {
		uni.showModal({
			title: '提示',
			content: '确定取消收藏该模型吗？',
			success: async (res) => {
				if (!res.confirm) return
				try {
					await toggleModelFavoriteApi(item.modelId)
					await loadFavoriteModels()
					uni.showToast({ title: '已取消收藏', icon: 'success' })
					uni.$emit('favorite-model-changed')
				} catch (error) {
					uni.showToast({ title: error.message || '操作失败', icon: 'none' })
				}
			}
		})
		return
	}
	const id = item?.id
	uni.showModal({
		title: '提示',
		content: '确定移除该清单项吗？',
		success: (res) => {
			if (res.confirm) {
				const items = currentList.value?.items || []
				const idx = items.findIndex(item => item.id === id)
				if (idx > -1) items.splice(idx, 1)
			}
		}
	})
}

const formatDate = (date) => {
	const pad = (n) => (n < 10 ? `0${n}` : n)
	return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const syncShareRecords = (records) => {
	shareRecords.value = records
	uni.setStorageSync('shared_lists', records)
}

const upsertShareRecord = (list, platform) => {
	const records = uni.getStorageSync('shared_lists') || []
	const index = records.findIndex(r => r.listId === list.id)
	const record = {
		id: index > -1 ? records[index].id : `SL${Date.now()}`,
		listId: list.id,
		title: list.name,
		desc: list.desc,
		cover: list.items[0]?.image || 'https://images.unsplash.com/photo-1519710164239-da123dc03ef4?w=300',
		count: list.items.length,
		views: index > -1 ? records[index].views : 0,
		likes: index > -1 ? records[index].likes : 0,
		sharedAt: formatDate(new Date()),
		status: platform,
		platform
	}
	if (index > -1) {
		records.splice(index, 1, record)
	} else {
		records.unshift(record)
	}
	syncShareRecords(records)
}

const shareToCommunity = (list) => {
	list.isPublic = true
	list.shares += 1
	upsertShareRecord(list, '社区')
	uni.showToast({ title: '已分享到社区', icon: 'success' })
}

const shareToThirdParty = (list) => {
	list.isPublic = true
	list.shares += 1
	upsertShareRecord(list, '第三方')
	const link = `https://3dprint.example.com/list/${list.id}`
	uni.setClipboardData({
		data: link,
		success: () => {
			uni.showToast({ title: '分享链接已复制', icon: 'success' })
		}
	})
}

const shareCurrentList = () => {
	const list = currentList.value
	if (!list || list.items.length === 0) {
		uni.showToast({ title: '清单为空，无法分享', icon: 'none' })
		return
	}
	uni.showActionSheet({
		itemList: ['分享到社区', '分享到第三方'],
		success: (res) => {
			if (res.tapIndex === 0) {
				shareToCommunity(list)
			} else {
				shareToThirdParty(list)
			}
		}
	})
}

const togglePublic = () => {
	const list = currentList.value
	if (!list) return
	list.isPublic = !list.isPublic
	if (list.isPublic) {
		upsertShareRecord(list, '社区')
		uni.showToast({ title: '清单已设为公开', icon: 'success' })
	} else {
		removeShareByList(list.id)
		uni.showToast({ title: '清单已设为私密', icon: 'none' })
	}
}

const removeShareByList = (listId) => {
	const records = uni.getStorageSync('shared_lists') || []
	const next = records.filter(item => item.listId !== listId)
	syncShareRecords(next)
}

const removeShare = (recordId) => {
	uni.showModal({
		title: '提示',
		content: '确定取消该清单的分享吗？',
		success: (res) => {
			if (res.confirm) {
				const records = uni.getStorageSync('shared_lists') || []
				const target = records.find(item => item.id === recordId)
				const next = records.filter(item => item.id !== recordId)
				if (target) {
					const list = listSets.value.find(item => item.id === target.listId)
					if (list) list.isPublic = false
				}
				syncShareRecords(next)
			}
		}
	})
}

const copyShareLink = (record) => {
	const link = `https://3dprint.example.com/list/${record.id}`
	uni.setClipboardData({
		data: link,
		success: () => {
			uni.showToast({ title: '分享链接已复制', icon: 'success' })
		}
	})
}

const openCreateForm = () => {
	showCreateForm.value = true
}

const cancelCreate = () => {
	showCreateForm.value = false
	newListName.value = ''
	newListDesc.value = ''
}

const confirmCreate = () => {
	if (!newListName.value.trim()) {
		uni.showToast({ title: '请输入清单名称', icon: 'none' })
		return
	}
	if (!newListDesc.value.trim()) {
		uni.showToast({ title: '请输入清单简介', icon: 'none' })
		return
	}
	const name = newListName.value.trim()
	const desc = newListDesc.value.trim()
	const tags = name.split(/[\s、，,]+/).filter(Boolean).slice(0, 3)
	listSets.value.unshift({
		id: `list_${Date.now()}`,
		name,
		desc,
		tags: tags.length ? tags : ['自定义'],
		views: 0,
		likes: 0,
		shares: 0,
		isPublic: false,
		items: []
	})
	activeListId.value = listSets.value[0].id
	cancelCreate()
	uni.showToast({ title: '清单已创建', icon: 'success' })
}

const goDetail = (item) => {
	if (item?.modelId) {
		uni.navigateTo({ url: '/pages/custom/detail?id=' + encodeURIComponent(item.modelId) })
		return
	}
	uni.navigateTo({ url: '/pages/custom/detail?name=' + item.name })
}

onShow(() => {
	shareRecords.value = uni.getStorageSync('shared_lists') || []
	loadFavoriteModels()
})
</script>

<style scoped lang="scss">
.list-container {
	min-height: 100vh;
	background-color: #f8fafc;
	padding-bottom: 120rpx;
}

.header-card {
	margin: 20rpx 30rpx 10rpx;
	padding: 30rpx;
	.title { font-size: 34rpx; font-weight: 700; color: #1e293b; }
	.desc { font-size: 24rpx; color: #94a3b8; margin-top: 8rpx; }
	.title-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		.share-btn {
			height: 60rpx;
			padding: 0 24rpx;
			font-size: 24rpx;
			border-radius: 30rpx;
			background-color: #4f46e5;
			color: #ffffff;
		}
	}
	.summary {
		margin-top: 24rpx;
		display: flex;
		.item { flex: 1; text-align: center; }
		.val { font-size: 28rpx; font-weight: 700; color: #4f46e5; display: block; }
		.lab { font-size: 22rpx; color: #94a3b8; margin-top: 6rpx; display: block; }
	}
	.list-switch {
		margin-top: 20rpx;
		display: flex;
		gap: 16rpx;
		overflow-x: auto;
		.switch-item {
			min-width: 220rpx;
			padding: 16rpx 20rpx;
			border-radius: 20rpx;
			background-color: #f1f5f9;
			color: #475569;
			display: flex;
			flex-direction: column;
			gap: 8rpx;
			&.active {
				background-color: #e0e7ff;
				color: #4338ca;
				font-weight: 700;
			}
			.switch-name { font-size: 26rpx; }
			.switch-count { font-size: 22rpx; }
		}
	}
}

.list-meta {
	margin: 0 30rpx 10rpx;
	padding: 26rpx 30rpx;
	.meta-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}
	.meta-title { font-size: 30rpx; font-weight: 700; color: #1e293b; }
	.meta-tags {
		display: flex;
		gap: 12rpx;
		.tag {
			font-size: 20rpx;
			background-color: #f1f5f9;
			padding: 4rpx 12rpx;
			border-radius: 16rpx;
			color: #64748b;
		}
	}
	.meta-desc { font-size: 24rpx; color: #64748b; margin-top: 10rpx; }
	.meta-stats {
		margin-top: 20rpx;
		display: flex;
		gap: 30rpx;
		.stat { display: flex; flex-direction: column; align-items: center; }
		.val { font-size: 26rpx; font-weight: 700; color: #4f46e5; }
		.lab { font-size: 20rpx; color: #94a3b8; margin-top: 4rpx; }
	}
	.meta-actions {
		margin-top: 20rpx;
		display: flex;
		gap: 16rpx;
		.btn { flex: 1; height: 64rpx; border-radius: 32rpx; font-size: 24rpx; }
		.btn.primary { background-color: #4f46e5; color: #ffffff; }
		.btn.ghost { background-color: #ffffff; border: 2rpx solid #e2e8f0; color: #475569; }
	}
}

.tabs {
	margin: 10rpx 30rpx;
	display: flex;
	gap: 16rpx;
	.tab {
		flex: 1;
		text-align: center;
		padding: 14rpx 0;
		font-size: 24rpx;
		color: #64748b;
		background-color: #ffffff;
		border-radius: 20rpx;
		&.active { color: #4f46e5; font-weight: 700; border: 2rpx solid #4f46e5; }
	}
}

.list-section {
	padding: 10rpx 30rpx;
	.empty {
		margin-top: 120rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		color: #94a3b8;
		text { margin-top: 20rpx; }
	}
}

.list-card {
	display: flex;
	padding: 20rpx;
	margin-bottom: 20rpx;
	.cover { width: 160rpx; height: 160rpx; border-radius: 12rpx; }
	.info { flex: 1; margin-left: 20rpx; }
	.row { display: flex; justify-content: space-between; align-items: center; }
	.name { font-size: 28rpx; font-weight: 700; color: #1e293b; }
	.tag { font-size: 20rpx; padding: 2rpx 12rpx; border-radius: 6rpx; }
	.tag.favorite { background-color: #e0e7ff; color: #4f46e5; }
	.tag.pending { background-color: #fef3c7; color: #d97706; }
	.tag.ordered { background-color: #dcfce7; color: #16a34a; }
	.params { font-size: 24rpx; color: #94a3b8; margin-top: 8rpx; }
	.price-row { display: flex; justify-content: space-between; margin-top: 10rpx; font-size: 24rpx; color: #475569; }
	.actions { margin-top: 14rpx; display: flex; gap: 12rpx; flex-wrap: wrap; }
	.btn { height: 52rpx; padding: 0 16rpx; font-size: 22rpx; border-radius: 26rpx; background-color: #f1f5f9; color: #475569; }
	.btn.primary { background-color: #4f46e5; color: #ffffff; }
	.btn.ghost { background-color: #ffffff; border: 2rpx solid #e2e8f0; }
}

.mall-section {
	margin: 0 30rpx 10rpx;
	padding: 26rpx 30rpx;
	.sec-title { font-size: 28rpx; font-weight: 700; color: #1e293b; margin-bottom: 20rpx; }
	.mall-list {
		display: flex;
		flex-direction: column;
		gap: 20rpx;
	}
	.mall-item {
		display: flex;
		gap: 16rpx;
		background-color: #f8fafc;
		border-radius: 16rpx;
		padding: 16rpx;
	}
	.mall-cover { width: 140rpx; height: 140rpx; border-radius: 12rpx; }
	.mall-info { flex: 1; display: flex; flex-direction: column; gap: 6rpx; }
	.mall-name { font-size: 26rpx; font-weight: 700; color: #1e293b; }
	.mall-params { font-size: 22rpx; color: #94a3b8; }
	.mall-row { display: flex; justify-content: space-between; align-items: center; margin-top: auto; }
	.mall-price { font-size: 26rpx; font-weight: 700; color: #ef4444; }
	.mall-btn { height: 52rpx; padding: 0 16rpx; font-size: 22rpx; border-radius: 26rpx; background-color: #4f46e5; color: #ffffff; }
	.mall-btn.disabled { background-color: #e2e8f0; color: #94a3b8; }
}

.share-section {
	margin: 0 30rpx 140rpx;
	padding: 26rpx 30rpx;
	.sec-title { font-size: 28rpx; font-weight: 700; color: #1e293b; margin-bottom: 20rpx; }
	.share-card {
		display: flex;
		gap: 20rpx;
		padding: 20rpx 0;
		border-bottom: 2rpx solid #f1f5f9;
		&:last-child { border-bottom: none; }
	}
	.share-cover { width: 140rpx; height: 140rpx; border-radius: 16rpx; }
	.share-info { flex: 1; }
	.share-row { display: flex; justify-content: space-between; align-items: center; }
	.share-title { font-size: 26rpx; font-weight: 700; color: #1e293b; }
	.share-status { font-size: 20rpx; padding: 2rpx 10rpx; border-radius: 6rpx; background-color: #e0e7ff; color: #4f46e5; }
	.share-status.私密 { background-color: #fee2e2; color: #ef4444; }
	.share-status.第三方 { background-color: #fef3c7; color: #d97706; }
	.share-desc { font-size: 22rpx; color: #64748b; margin-top: 6rpx; }
	.share-meta { margin-top: 10rpx; display: flex; justify-content: space-between; font-size: 20rpx; color: #94a3b8; }
	.share-actions { margin-top: 12rpx; display: flex; gap: 12rpx; }
	.btn { height: 48rpx; padding: 0 16rpx; font-size: 20rpx; border-radius: 24rpx; background-color: #f1f5f9; color: #475569; }
	.btn.ghost { background-color: #ffffff; border: 2rpx solid #e2e8f0; }
}

.create-mask {
	position: fixed;
	inset: 0;
	background-color: rgba(15, 23, 42, 0.4);
	display: flex;
	justify-content: center;
	align-items: center;
	z-index: 20;
}

.create-panel {
	width: 620rpx;
	padding: 30rpx;
	.panel-title { font-size: 30rpx; font-weight: 700; color: #1e293b; }
	.panel-input {
		margin-top: 20rpx;
		height: 80rpx;
		background-color: #f8fafc;
		border-radius: 16rpx;
		padding: 0 20rpx;
		font-size: 26rpx;
	}
	.panel-textarea {
		margin-top: 16rpx;
		min-height: 160rpx;
		background-color: #f8fafc;
		border-radius: 16rpx;
		padding: 16rpx 20rpx;
		font-size: 24rpx;
	}
	.panel-actions {
		margin-top: 20rpx;
		display: flex;
		gap: 16rpx;
		.btn { flex: 1; height: 72rpx; border-radius: 36rpx; font-size: 24rpx; }
		.btn.primary { background-color: #4f46e5; color: #ffffff; }
		.btn.ghost { background-color: #ffffff; border: 2rpx solid #e2e8f0; color: #475569; }
	}
}

.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 20rpx 40rpx;
	padding-bottom: calc(env(safe-area-inset-bottom) + 20rpx);
	background-color: #ffffff;
	box-shadow: 0 -4rpx 12rpx rgba(0,0,0,0.05);
	.action-btn { height: 88rpx; background-color: #4f46e5; color: #ffffff; border-radius: 44rpx; font-size: 30rpx; font-weight: 700; }
}

.blocked {
	margin: 40rpx 30rpx;
	padding: 40rpx 30rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	text-align: center;
	.blocked-title { font-size: 30rpx; font-weight: 700; color: #1e293b; }
	.blocked-desc { font-size: 24rpx; color: #64748b; margin-top: 16rpx; line-height: 1.6; }
	.back-btn {
		margin-top: 30rpx;
		height: 80rpx;
		padding: 0 40rpx;
		background-color: #4f46e5;
		color: #ffffff;
		border-radius: 40rpx;
		font-size: 26rpx;
	}
}
</style>
