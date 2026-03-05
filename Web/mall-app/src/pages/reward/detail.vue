<template>
	<view class="detail-container">
		<view class="status-header">
			<view class="left">
				<text class="status">{{detail.statusText}}</text>
				<text class="title">{{detail.title}}</text>
			</view>
			<view class="right">
				<text class="price">￥{{detail.price}}</text>
				<text class="deadline">截止 {{detail.deadline}}</text>
			</view>
		</view>

		<view class="section card">
			<view class="sec-title">需求描述</view>
			<text class="desc">{{detail.content}}</text>
			<view class="tags">
				<text class="tag" v-for="(tag, i) in detail.tags" :key="i">{{tag}}</text>
			</view>
		</view>

		<view class="section card">
			<view class="sec-title">预算与周期</view>
			<view class="row"><text class="label">预算金额</text><text class="val">￥{{detail.price}}</text></view>
			<view class="row"><text class="label">交付周期</text><text class="val">{{detail.cycle}}</text></view>
			<view class="row"><text class="label">发布者</text><text class="val">{{detail.author}}</text></view>
			<view class="row"><text class="label">竞标人数</text><text class="val">{{detail.bids}} 人</text></view>
		</view>

		<view class="section card">
			<view class="sec-title">附件与参考</view>
			<view class="file-list" v-if="detail.files.length">
				<image
					class="thumb-img"
					v-for="(file, idx) in detail.files"
					:key="file + idx"
					:src="file"
					mode="aspectFill"
					@click="previewAttachment(idx)"
				/>
			</view>
			<view v-if="!detail.files.length" class="file">
				<text>暂无附件</text>
			</view>
		</view>

		<view class="section card">
			<view class="sec-title">竞标方案</view>
			<view class="bid-item" v-for="(bid, idx) in displayBidList" :key="idx">
				<view class="bid-header">
					<view class="bid-left">
						<text class="name">{{bid.name}}</text>
						<text class="status-pill" :class="bid.statusClass">{{ bid.statusText }}</text>
					</view>
					<text class="price">￥{{bid.quote}}</text>
				</view>
				<text class="content">{{bid.plan}}</text>
				<view class="bid-assets" v-if="bid.assets.length">
					<view
						class="asset-item"
						v-for="(asset, assetIdx) in bid.assets"
						:key="asset.url + assetIdx"
						@click="previewBidAsset(asset, bid.assets, assetIdx)"
					>
						<image v-if="asset.type === 'image'" class="asset-image" :src="asset.url" mode="aspectFill" />
						<view v-else class="asset-file">
							<text class="asset-type">{{ asset.type === 'video' ? '视频' : '模型' }}</text>
							<text class="asset-name">{{ asset.name || '点击查看' }}</text>
						</view>
					</view>
				</view>
				<view class="bid-footer">
					<text class="time">{{bid.time}}</text>
					<view class="bid-actions">
						<button v-if="canPickBid" class="btn" @click="pickBid(bid)">选为中标</button>
						<button v-if="isDesigner && canEditDesignerBid && currentDesignerBid && currentDesignerBid.id === bid.id" class="btn" @click="goBidPage(bid.id)">修改方案</button>
						<button v-if="isDesigner && canEditDesignerBid && currentDesignerBid && currentDesignerBid.id === bid.id" class="btn danger" @click="withdrawBid(bid)">撤回竞标</button>
					</view>
				</view>
			</view>
			<view v-if="displayBidList.length === 0" class="file">
				<text>暂无竞标方案</text>
			</view>
		</view>

		<view class="section card">
			<view class="sec-title">进度跟踪</view>
			<view class="step" v-for="(step, idx) in detail.steps" :key="idx">
				<view class="dot"></view>
				<view class="step-content">
					<text class="s-title">{{step.title}}</text>
					<text class="s-desc">{{step.desc}}</text>
					<text class="s-time">{{step.time}}</text>
				</view>
			</view>
		</view>

		<view class="section card" v-if="payStatus.needPay">
			<view class="sec-title">改价补差支付</view>
			<view class="row"><text class="label">目标金额</text><text class="val">￥{{payStatus.targetAmount || 0}}</text></view>
			<view class="row"><text class="label">当前任务金额</text><text class="val">￥{{payStatus.currentTaskAmount || 0}}</text></view>
			<view class="row"><text class="label">支付状态</text><text class="val">{{payStatus.paid ? '已完成' : '待支付'}}</text></view>
			<view class="pay-actions" v-if="!payStatus.paid && canPayPriceIncrease">
				<button class="btn" @click="syncPriceIncreaseStatus">刷新状态</button>
				<button class="btn primary" @click="payPriceIncrease">立即补差支付</button>
			</view>
		</view>

		<view class="bottom-bar" v-if="canConfirm || canGoBidPage">
			<button v-if="canGoBidPage" class="btn" @click="goBidPage()">提交竞标方案</button>
			<button v-if="canConfirm" class="btn primary" @click="confirmDelivery">确认验收</button>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { getUserDetailApi } from '../../api/user'
import { getStoredUserRole, isDesignerRole } from '../../utils/role'
import {
	acceptBountyDeliveryApi,
	createBountyPriceIncreasePayApi,
	getBountyPriceIncreasePayStatusByTaskApi,
	getBountyTaskDetailApi,
	pickBountyBidApi,
	syncBountyPriceIncreasePayStatusByTaskApi,
	withdrawBountyBidApi
} from '../../api/reward'

const userRole = ref(getStoredUserRole())
const currentUserId = ref(String((uni.getStorageSync('user_profile') || {}).id || ''))
const isDesigner = computed(() => isDesignerRole(userRole.value))

const detail = ref({
	id: 0,
	title: '',
	content: '',
	price: 0,
	statusText: '',
	deadline: '',
	cycle: '',
	tags: [],
	files: [],
	author: '',
	bids: 0,
	bidList: [],
	steps: [],
	status: 0,
	ownerId: '',
	publisherId: null,
	pendingDeliveryId: null
})

const payStatus = ref({
	priceChangeId: null,
	needPay: false,
	paid: true,
	currentTaskAmount: 0,
	targetAmount: 0,
	outTradeNo: ''
})

const BID_ASSET_MARK = '[BID_ASSETS]'
const BID_STATUS_WITHDRAWN = 3

const getBidStatusMeta = (status) => {
	if (status === 1) return { text: '已中标', className: 'winner' }
	if (status === 2) return { text: '未中标', className: 'lost' }
	if (status === 3) return { text: '已撤回', className: 'withdrawn' }
	return { text: '已提交', className: 'submitted' }
}

const statusMap = {
	0: '待支付托管',
	1: '招募中',
	2: '已选标',
	3: '交付中',
	4: '待验收',
	5: '已完成',
	6: '已关闭',
	7: '争议中'
}

const loadDetail = async (id) => {
	try {
		const data = await getBountyTaskDetailApi(id)
		const publisherId = Number(data.publisherId || 0)
		const computedCycleDays = data.expectedDays || calcCycleDays(data.createTime, data.deadlineTime)
		const mappedBids = (data.bids || []).map(bid => {
			const parsed = parseBidProposal(bid.proposal)
			const bidStatus = Number(bid.status ?? 0)
			const statusMeta = getBidStatusMeta(bidStatus)
			return {
				id: bid.id,
				designerId: bid.designerId,
				status: bidStatus,
				statusText: statusMeta.text,
				statusClass: statusMeta.className,
				name: `设计者#${bid.designerId}`,
				quote: bid.quoteAmount,
				deliveryDays: bid.deliveryDays,
				plan: parsed.plan,
				assets: parsed.assets,
				time: bid.createTime || ''
			}
		})
		detail.value = {
			id: data.id,
			title: data.title,
			content: data.description,
			price: Number(data.finalAmount || data.budgetAmount || 0),
			statusText: statusMap[data.status] || `状态${data.status}`,
			deadline: data.deadlineTime || '-',
			cycle: computedCycleDays ? `${computedCycleDays} 天` : '-',
			tags: String(data.tags || '').split(',').filter(Boolean),
			files: Array.isArray(data.attachments) ? data.attachments : [],
			author: publisherId ? `用户#${publisherId}` : '-',
			bids: mappedBids.length,
			bidList: mappedBids,
			steps: [
				{ title: '需求发布', desc: '任务已创建', time: data.createTime || '' },
				{ title: '当前状态', desc: statusMap[data.status] || '处理中', time: data.updateTime || '' }
			],
			status: data.status,
			publisherId: publisherId || null,
			ownerId: String(data.publisherId || ''),
			pendingDeliveryId: data.pendingDeliveryId || null
		}
		await fillPublisherNickname()
		await loadPriceIncreasePayStatus(id)
	} catch (error) {
		uni.showToast({ title: error?.message || '加载详情失败', icon: 'none' })
	}
}

const fillPublisherNickname = async () => {
	const publisherId = detail.value.publisherId
	if (!publisherId) {
		return
	}
	try {
		const user = await getUserDetailApi(publisherId)
		const nickname = String(user?.nickname || '').trim()
		const userName = String(user?.userName || '').trim()
		detail.value.author = nickname || userName || `用户#${publisherId}`
	} catch (_) {
		detail.value.author = `用户#${publisherId}`
	}
}

const calcCycleDays = (createTime, deadlineTime) => {
	if (!deadlineTime) {
		return null
	}
	const start = createTime ? new Date(createTime).getTime() : Date.now()
	const end = new Date(deadlineTime).getTime()
	if (!Number.isFinite(start) || !Number.isFinite(end)) {
		return null
	}
	const diff = end - start
	if (diff <= 0) {
		return 1
	}
	return Math.ceil(diff / (24 * 60 * 60 * 1000))
}

const loadPriceIncreasePayStatus = async (taskId) => {
	try {
		const data = await getBountyPriceIncreasePayStatusByTaskApi(taskId)
		payStatus.value = {
			priceChangeId: data?.priceChangeId || null,
			needPay: Boolean(data?.needPay),
			paid: Boolean(data?.paid),
			currentTaskAmount: Number(data?.currentTaskAmount || 0),
			targetAmount: Number(data?.targetAmount || 0),
			outTradeNo: data?.outTradeNo || ''
		}
	} catch (error) {
		payStatus.value = {
			priceChangeId: null,
			needPay: false,
			paid: true,
			currentTaskAmount: 0,
			targetAmount: 0,
			outTradeNo: ''
		}
	}
}

onLoad((options) => {
	userRole.value = getStoredUserRole()
	currentUserId.value = String((uni.getStorageSync('user_profile') || {}).id || '')
	if (options?.id) {
		loadDetail(options.id)
	}
})

const isOwner = computed(() => detail.value.ownerId && detail.value.ownerId === currentUserId.value)
const canPickBid = computed(() => !isDesigner.value && isOwner.value)
const canApplyBid = computed(() => isDesigner.value && detail.value.status === 1)
const canConfirm = computed(() => !isDesigner.value && isOwner.value && detail.value.status === 4 && !!detail.value.pendingDeliveryId)
const canPayPriceIncrease = computed(() => !isDesigner.value && isOwner.value)
const displayBidList = computed(() => {
	const visibleBids = detail.value.bidList.filter(bid => bid.status !== BID_STATUS_WITHDRAWN)
	if (!isDesigner.value) {
		return visibleBids
	}
	return visibleBids.filter(bid => String(bid.designerId || '') === currentUserId.value)
})
const currentDesignerBid = computed(() => displayBidList.value[0] || null)
const hasDesignerBid = computed(() => Boolean(currentDesignerBid.value))
const canEditDesignerBid = computed(() => {
	return Boolean(currentDesignerBid.value && currentDesignerBid.value.status === 0 && detail.value.status === 1)
})
const canGoBidPage = computed(() => canApplyBid.value && !hasDesignerBid.value)

const goBidPage = (bidId) => {
	const query = bidId
		? `?taskId=${detail.value.id}&bidId=${bidId}`
		: `?taskId=${detail.value.id}`
	uni.navigateTo({ url: `/pages/reward/bid${query}` })
}

const sleep = (ms = 1000) => new Promise(resolve => setTimeout(resolve, ms))

const ensureAlipayAvailable = async () => {
	const providers = await new Promise(resolve => {
		uni.getProvider({
			service: 'payment',
			success: (res) => resolve(res.provider || []),
			fail: () => resolve([])
		})
	})
	if (!providers.includes('alipay')) {
		uni.showToast({ title: '当前环境不支持支付宝支付', icon: 'none' })
		return false
	}
	return true
}

const withSandboxOrderInfo = (orderInfo) => {
	if (!orderInfo || typeof orderInfo !== 'string') {
		return orderInfo
	}
	if (orderInfo.includes('app_env=system')) {
		return orderInfo.replace(/app_env=system/g, 'app_env=default')
	}
	if (orderInfo.includes('app_env="system"')) {
		return orderInfo.replace(/app_env="system"/g, 'app_env="default"')
	}
	return orderInfo
}

const ensureAlipaySandboxEnv = () => {
	const platform = uni.getSystemInfoSync().platform
	if (platform !== 'android') {
		uni.showToast({ title: '请在安卓设备上使用支付宝沙箱版测试', icon: 'none' })
		return false
	}
	return true
}

const pollPriceIncreasePayStatus = async (taskId, maxRetry = 8) => {
	for (let i = 0; i < maxRetry; i++) {
		try {
			const status = await syncBountyPriceIncreasePayStatusByTaskApi(taskId)
			if (status?.paid) {
				return true
			}
		} catch (_) {}
		await sleep(2000)
	}
	return false
}

const withdrawBid = (bid) => {
	uni.showModal({
		title: '撤回竞标',
		content: '撤回后发布者将不再看到你的竞标方案，是否继续？',
		success: async (res) => {
			if (!res.confirm) return
			try {
				await withdrawBountyBidApi(bid.id)
				uni.showToast({ title: '竞标已撤回', icon: 'success' })
				loadDetail(detail.value.id)
			} catch (error) {
				uni.showToast({ title: error?.message || '撤回失败', icon: 'none' })
			}
		}
	})
}

const parseBidProposal = (rawProposal) => {
	const text = String(rawProposal || '')
	const index = text.indexOf(BID_ASSET_MARK)
	if (index < 0) {
		return { plan: text, assets: [] }
	}
	const plan = text.slice(0, index).trim()
	const payload = text.slice(index + BID_ASSET_MARK.length).trim()
	try {
		const assets = JSON.parse(payload)
		if (!Array.isArray(assets)) {
			return { plan: plan || text, assets: [] }
		}
		return {
			plan: plan || '（含附件）',
			assets: assets
				.filter(item => item && typeof item.url === 'string' && item.url)
				.map(item => ({
					url: item.url,
					type: item.type === 'video' || item.type === 'model' ? item.type : 'image',
					name: item.name || ''
				}))
		}
	} catch (_) {
		return { plan: text, assets: [] }
	}
}

const parseModelType = (asset = {}) => {
	const rawUrl = String(asset.url || '')
	const cleanUrl = rawUrl.split('?')[0].split('#')[0]
	const ext = cleanUrl.includes('.') ? cleanUrl.split('.').pop().toLowerCase() : ''
	if (ext) return ext
	const name = String(asset.name || '').toLowerCase()
	if (name.includes('.')) {
		return name.split('.').pop() || 'stl'
	}
	return 'stl'
}

const openModelPreviewPage = (asset) => {
	const modelUrl = encodeURIComponent(String(asset.url || ''))
	const modelName = encodeURIComponent(String(asset.name || '模型文件'))
	const modelType = encodeURIComponent(parseModelType(asset))
	uni.navigateTo({
		url: `/pages/reward/model-preview?url=${modelUrl}&name=${modelName}&type=${modelType}`
	})
}

const openVideoPreviewPage = (asset) => {
	const videoUrl = encodeURIComponent(String(asset.url || ''))
	const videoName = encodeURIComponent(String(asset.name || '视频附件'))
	uni.navigateTo({
		url: `/pages/reward/video-preview?url=${videoUrl}&name=${videoName}`
	})
}

const previewBidAsset = (asset, list, index) => {
	if (asset.type === 'image') {
		uni.previewImage({
			current: index,
			urls: list.filter(item => item.type === 'image').map(item => item.url)
		})
		return
	}
	if (asset.type === 'model') {
		openModelPreviewPage(asset)
		return
	}
	if (asset.type === 'video') {
		openVideoPreviewPage(asset)
		return
	}
	uni.showModal({
		title: asset.type === 'video' ? '视频链接' : '模型文件链接',
		content: asset.url,
		confirmText: '复制链接',
		success: (res) => {
			if (res.confirm) {
				uni.setClipboardData({ data: asset.url })
			}
		}
	})
}

const pickBid = (bid) => {
	uni.showModal({
		title: '确认中标',
		content: `确定选择 ${bid.name} 的方案吗？`,
		success: async (res) => {
			if (res.confirm) {
				try {
					await pickBountyBidApi({
						taskId: detail.value.id,
						bidId: bid.id,
						pickReason: '综合方案最优'
					})
					uni.showToast({ title: '已选为中标', icon: 'success' })
					loadDetail(detail.value.id)
				} catch (error) {
					uni.showToast({ title: error?.message || '选标失败', icon: 'none' })
				}
			}
		}
	})
}

const syncPriceIncreaseStatus = async () => {
	try {
		await syncBountyPriceIncreasePayStatusByTaskApi(detail.value.id)
		await loadDetail(detail.value.id)
		uni.showToast({ title: '状态已更新', icon: 'success' })
	} catch (error) {
		uni.showToast({ title: error?.message || '刷新失败', icon: 'none' })
	}
}

const payPriceIncrease = async () => {
	try {
		if (!payStatus.value.priceChangeId) {
			uni.showToast({ title: '暂无可支付补差记录', icon: 'none' })
			return
		}
		const alipayReady = await ensureAlipayAvailable()
		if (!alipayReady) {
			return
		}
		if (!ensureAlipaySandboxEnv()) {
			return
		}

		uni.showLoading({ title: '创建补差支付中...' })
		const payResult = await createBountyPriceIncreasePayApi(payStatus.value.priceChangeId)
		uni.hideLoading()

		await new Promise((resolve, reject) => {
			uni.requestPayment({
				provider: 'alipay',
				orderInfo: withSandboxOrderInfo(payResult.orderString),
				success: () => resolve(true),
				fail: (err) => reject(err)
			})
		})

		uni.showLoading({ title: '确认支付结果...' })
		const paid = await pollPriceIncreasePayStatus(detail.value.id)
		uni.hideLoading()
		if (!paid) {
			uni.showToast({ title: '支付结果确认超时，请稍后刷新', icon: 'none' })
			return
		}
		uni.showToast({ title: '补差支付成功', icon: 'success' })
		await loadDetail(detail.value.id)
	} catch (error) {
		uni.hideLoading()
		const message = error?.errMsg || error?.message || '补差支付失败'
		if (String(message).includes('cancel')) {
			uni.showToast({ title: '您已取消支付', icon: 'none' })
		} else {
			uni.showToast({ title: message, icon: 'none' })
		}
	}
}

const confirmDelivery = () => {
	uni.showModal({
		title: '确认验收',
		content: '确认已收到成果并完成验收？',
		success: async (res) => {
			if (res.confirm) {
				if (!detail.value.pendingDeliveryId) {
					uni.showToast({ title: '当前没有可验收交付', icon: 'none' })
					return
				}
				try {
					await acceptBountyDeliveryApi({
						taskId: detail.value.id,
						deliveryId: detail.value.pendingDeliveryId,
						acceptNote: '用户确认验收通过'
					})
					uni.showToast({ title: '验收完成', icon: 'success' })
					loadDetail(detail.value.id)
				} catch (error) {
					uni.showToast({ title: error?.message || '验收失败', icon: 'none' })
				}
			}
		}
	})
}

const previewAttachment = (index) => {
	if (!detail.value.files.length) {
		return
	}
	uni.previewImage({
		current: index,
		urls: detail.value.files
	})
}

onUnload(() => {
	uni.hideLoading()
})
</script>

<style scoped lang="scss">
.detail-container {
	min-height: 100vh;
	background-color: #f8fafc;
	padding-bottom: 120rpx;
}

.status-header {
	background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
	padding: 50rpx 40rpx;
	color: #ffffff;
	display: flex;
	justify-content: space-between;
	.status { font-size: 26rpx; opacity: 0.9; }
	.title { font-size: 34rpx; font-weight: 700; margin-top: 10rpx; display: block; }
	.price { font-size: 36rpx; font-weight: 700; display: block; text-align: right; }
	.deadline { font-size: 22rpx; opacity: 0.9; margin-top: 8rpx; display: block; }
}

.section {
	margin: 20rpx 30rpx;
	padding: 30rpx;
	.sec-title { font-size: 28rpx; font-weight: 700; color: #1e293b; margin-bottom: 16rpx; }
	.desc { font-size: 26rpx; color: #475569; line-height: 1.6; }
	.tags { margin-top: 20rpx; display: flex; gap: 12rpx; flex-wrap: wrap; }
	.tag { font-size: 22rpx; color: #94a3b8; background-color: #f1f5f9; padding: 4rpx 16rpx; border-radius: 8rpx; }
	.row { display: flex; justify-content: space-between; padding: 10rpx 0; font-size: 24rpx; color: #64748b; }
	.pay-actions { display: flex; gap: 16rpx; margin-top: 20rpx; }
	.label { color: #94a3b8; }
	.val { color: #1e293b; }
	.file { display: flex; align-items: center; gap: 10rpx; font-size: 24rpx; color: #4f46e5; margin-bottom: 10rpx; }
	.file-list { display: flex; gap: 12rpx; flex-wrap: wrap; }
	.thumb-img {
		width: 140rpx;
		height: 140rpx;
		border-radius: 12rpx;
		background-color: #eef2ff;
	}
	.bid-item { border-top: 2rpx solid #f1f5f9; padding-top: 20rpx; margin-top: 20rpx; }
	.bid-header { display: flex; justify-content: space-between; font-size: 26rpx; font-weight: 700; color: #1e293b; }
	.bid-left { display: flex; align-items: center; gap: 10rpx; }
	.status-pill {
		font-size: 20rpx;
		padding: 2rpx 12rpx;
		border-radius: 20rpx;
		font-weight: 500;
		&.submitted { background-color: #eef2ff; color: #4f46e5; }
		&.winner { background-color: #dcfce7; color: #16a34a; }
		&.lost { background-color: #fee2e2; color: #dc2626; }
		&.withdrawn { background-color: #f1f5f9; color: #64748b; }
	}
	.content { font-size: 24rpx; color: #475569; margin-top: 10rpx; }
	.bid-assets {
		margin-top: 12rpx;
		display: flex;
		flex-wrap: wrap;
		gap: 12rpx;
	}
	.asset-item {
		position: relative;
		width: 140rpx;
		height: 140rpx;
		border-radius: 10rpx;
		overflow: hidden;
		background-color: #eef2ff;
	}
	.asset-image { width: 100%; height: 100%; }
	.asset-file {
		display: flex;
		flex-direction: column;
		justify-content: center;
		height: 100%;
		padding: 10rpx;
	}
	.asset-type { font-size: 20rpx; color: #4f46e5; }
	.asset-name { font-size: 20rpx; color: #475569; margin-top: 6rpx; }
	.bid-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 14rpx; }
	.bid-actions { display: flex; gap: 10rpx; }
	.time { font-size: 22rpx; color: #94a3b8; }
	.btn { height: 52rpx; padding: 0 20rpx; font-size: 22rpx; border-radius: 26rpx; background-color: #4f46e5; color: #ffffff; }
	.danger { background-color: #ef4444; }
	.step { display: flex; gap: 20rpx; padding: 12rpx 0; }
	.dot { width: 12rpx; height: 12rpx; background-color: #4f46e5; border-radius: 50%; margin-top: 8rpx; }
	.step-content { flex: 1; }
	.s-title { font-size: 24rpx; color: #1e293b; font-weight: 700; }
	.s-desc { font-size: 22rpx; color: #64748b; margin-top: 6rpx; display: block; }
	.s-time { font-size: 22rpx; color: #94a3b8; margin-top: 6rpx; display: block; }
}

.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	background-color: #ffffff;
	padding: 20rpx 30rpx;
	display: flex;
	gap: 20rpx;
	box-shadow: 0 -4rpx 12rpx rgba(0,0,0,0.05);
	.btn { flex: 1; height: 80rpx; border-radius: 40rpx; font-size: 26rpx; background-color: #f1f5f9; color: #1e293b; }
	.primary { background-color: #f59e0b; color: #ffffff; }
}
</style>
