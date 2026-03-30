<template>
	<view class="detail-container">
		<view class="status-header">
			<view class="header-bg"></view>
			<view class="header-content">
				<view class="left">
					<text class="status">{{detail.statusText}}</text>
					<text class="title">{{detail.title}}</text>
				</view>
				<view class="right">
					<text class="price">￥{{detail.price}}</text>
					<text class="deadline">截止 {{detail.deadline}}</text>
				</view>
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
						<text class="name" @click="goDesignerProfile(bid.designerId)">{{bid.name}}</text>
						<text class="profile-link" @click="goDesignerProfile(bid.designerId)">查看档案</text>
						<text
							v-if="bid.reputationScore != null"
							class="rep-badge"
							:class="{ 'rep-low': bid.reputationScore < 60, 'rep-normal': bid.reputationScore >= 60 }"
						>信誉 {{ bid.reputationScore }}</text>
						<text class="status-pill" :class="bid.statusClass">{{ bid.statusText }}</text>
					</view>
					<text class="price">￥{{bid.quote}}</text>
				</view>
				<text v-if="bid.reputationScore != null && bid.reputationScore < 60" class="rep-warning">⚠ 该设计者信誉较低，请谨慎选择</text>
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

		<!-- 交付成果 -->
		<view class="section card delivery-card" v-if="detail.deliveryInfo">
			<view class="sec-title">交付成果（第{{detail.deliveryInfo.deliveryRound}}轮）</view>
			<view class="delivery-status-row">
				<text class="label">状态</text>
				<text :class="['delivery-tag', deliveryStatusClass]">{{deliveryStatusText}}</text>
			</view>
			<view class="delivery-desc" v-if="detail.deliveryInfo.description">
				<text class="label">交付说明</text>
				<text class="desc-text">{{detail.deliveryInfo.description}}</text>
			</view>
			<!-- 版权信息 -->
			<view class="license-section">
				<view class="license-title">版权授权</view>
				<view class="license-row">
					<text class="label">许可类型</text>
					<text class="val">{{detail.deliveryInfo.licenseType || '未指定'}}</text>
				</view>
				<view class="license-row">
					<text class="label">商业使用</text>
					<text :class="['val', detail.deliveryInfo.allowCommercialUse === 1 ? 'allow' : 'deny']">
						{{detail.deliveryInfo.allowCommercialUse === 1 ? '允许' : '不允许'}}
					</text>
				</view>
				<view class="license-row">
					<text class="label">修改权限</text>
					<text :class="['val', detail.deliveryInfo.allowModification === 1 ? 'allow' : 'deny']">
						{{detail.deliveryInfo.allowModification === 1 ? '允许' : '不允许'}}
					</text>
				</view>
			</view>
			<!-- 文件列表 -->
			<view class="file-section">
				<view class="license-title">交付文件</view>
				<view v-if="detail.deliveryInfo.files && detail.deliveryInfo.files.length > 0" class="file-list">
					<view class="file-item" v-for="(file, idx) in detail.deliveryInfo.files" :key="idx" @click="previewDeliveryFile(file)">
						<text class="file-icon">{{ getModelFileIcon(file) }}</text>
						<text class="file-name">{{ file.name || ('模型文件 ' + (idx + 1)) }}</text>
						<text class="file-ext">{{ getFileExt(file) }}</text>
						<text class="file-action">{{ isModelFile(file) ? '预览' : '查看' }}</text>
					</view>
				</view>
				<view v-else class="file-locked">
					<text class="lock-icon">🔒</text>
					<text class="lock-text">模型文件将在验收通过后解锁</text>
				</view>
			</view>
		</view>

		<!-- 取消申请审核中 -->
		<view class="section card cancel-pending-card" v-if="detail.cancelRequested === 1">
			<text class="cancel-pending-text">取消申请审核中，请等待管理员处理</text>
		</view>

		<view class="section card rejected-card" v-if="isRejected && isOwner">
			<view class="sec-title">审核驳回</view>
			<view class="reject-reason">
				<text class="label">驳回原因：</text>
				<text class="reason-text">{{detail.closeReason || '平台审核未通过'}}</text>
			</view>
			<view class="reject-tip">您可以修改悬赏内容后重新提交审核</view>
			<button class="btn primary resubmit-btn" @click="goEditPage">修改并重新提交</button>
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

		<view class="section card" v-if="needEscrowPay">
			<view class="sec-title">托管金支付</view>
			<view class="row"><text class="label">托管金额</text><text class="val">￥{{detail.price}}</text></view>
			<view class="row"><text class="label">支付状态</text><text class="val">待支付</text></view>
			<view class="pay-actions">
				<button class="btn" @click="syncEscrowStatus">刷新状态</button>
				<button class="btn primary" @click="payEscrow">立即支付托管金</button>
			</view>
		</view>

		<view class="bottom-bar" v-if="canConfirm || canGoBidPage || canRate || needEscrowPay || (isRejected && isOwner) || canSubmitDelivery || canChat || canCancel">
			<button v-if="isRejected && isOwner" class="btn primary" @click="goEditPage">修改并重新提交</button>
			<button v-if="needEscrowPay" class="btn primary" @click="payEscrow">支付托管金</button>
			<button v-if="canCancel" class="btn danger" @click="requestCancel">取消悬赏</button>
			<button v-if="canGoBidPage" class="btn" @click="goBidPage()">提交竞标方案</button>
			<button v-if="canChat" class="btn" @click="goMessagesPage">在线沟通</button>
			<button v-if="canSubmitDelivery" class="btn primary" @click="goDeliveryPage">提交交付</button>
			<button v-if="canConfirm" class="btn danger" @click="rejectDelivery">驳回交付</button>
			<button v-if="canConfirm" class="btn primary" @click="confirmDelivery">确认验收</button>
			<button v-if="canRate" class="btn primary" @click="goRatingPage()">评价设计者</button>
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
	cancelBountyTaskApi,
	createBountyEscrowPayApi,
	createBountyPriceIncreasePayApi,
	getBountyEscrowPayStatusApi,
	getBountyPriceIncreasePayStatusByTaskApi,
	getBountyRatingApi,
	getDesignerReputationApi,
	syncBountyEscrowPayStatusApi,
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
	pendingDeliveryId: null,
	closeReason: '',
	cancelRequested: 0,
	deliveryInfo: null
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
	'-1': '待审核',
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
		const publisherId = data.publisherId ? String(data.publisherId) : ''
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
		// Load reputation for each bidder
		const uniqueDesignerIds = [...new Set(mappedBids.map(b => b.designerId).filter(Boolean))]
		const repResults = await Promise.allSettled(
			uniqueDesignerIds.map(did => getDesignerReputationApi(did))
		)
		const repMap = {}
		uniqueDesignerIds.forEach((did, i) => {
			if (repResults[i].status === 'fulfilled' && repResults[i].value) {
				repMap[did] = repResults[i].value.reputationScore
			}
		})
		mappedBids.forEach(bid => {
			bid.reputationScore = repMap[bid.designerId] ?? null
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
			pendingDeliveryId: data.pendingDeliveryId || null,
			closeReason: data.closeReason || '',
			cancelRequested: data.cancelRequested || 0,
			deliveryInfo: data.deliveryInfo || null
		}
		await fillPublisherNickname()
		await loadPriceIncreasePayStatus(id)
		await checkRating()
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
const hasRating = ref(false)
const canRate = computed(() => !isDesigner.value && isOwner.value && detail.value.status === 5 && !hasRating.value)
const needEscrowPay = computed(() => !isDesigner.value && isOwner.value && detail.value.status === 0)
const isRejected = computed(() => detail.value.status === 6 && !!detail.value.closeReason)
const canCancel = computed(() => isOwner.value && [1, 2].includes(detail.value.status) && detail.value.cancelRequested !== 1)

const deliveryStatusMap = { 1: '已提交', 2: '需返工', 3: '已验收' }
const deliveryStatusText = computed(() => deliveryStatusMap[detail.value.deliveryInfo?.status] || '未知')
const deliveryStatusClass = computed(() => {
	const s = detail.value.deliveryInfo?.status
	if (s === 3) return 'tag-success'
	if (s === 2) return 'tag-warning'
	return 'tag-info'
})

const MODEL_EXTS = ['stl', 'obj', 'fbx', 'gltf', 'glb', '3mf', 'step', 'stp', 'iges', 'igs', 'blend', 'max', 'ma', 'mb', 'c4d', 'ply', 'amf']
const IMG_EXTS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp']

const getFileExt = (file) => {
	const name = String(file?.name || file?.url || '')
	const clean = name.split('?')[0].split('#')[0]
	return clean.includes('.') ? clean.split('.').pop().toLowerCase() : ''
}

const isModelFile = (file) => MODEL_EXTS.includes(getFileExt(file))

const getModelFileIcon = (file) => {
	const ext = getFileExt(file)
	if (MODEL_EXTS.includes(ext)) return '🧊'
	if (IMG_EXTS.includes(ext)) return '🖼️'
	return '📎'
}

const previewDeliveryFile = (file) => {
	if (!file?.url) return
	const ext = getFileExt(file)
	if (MODEL_EXTS.includes(ext)) {
		openModelPreviewPage({ url: file.url, name: file.name || '模型文件', type: ext })
		return
	}
	if (IMG_EXTS.includes(ext)) {
		uni.previewImage({ urls: [file.url], current: file.url })
		return
	}
	// #ifdef APP-PLUS
	plus.runtime.openURL(file.url)
	// #endif
	// #ifdef H5
	window.open(file.url, '_blank')
	// #endif
	// #ifndef APP-PLUS || H5
	uni.setClipboardData({ data: file.url, success: () => uni.showToast({ title: '链接已复制', icon: 'success' }) })
	// #endif
}

const winningDesignerId = computed(() => {
	const winner = detail.value.bidList.find(b => b.status === 1)
	return winner ? String(winner.designerId) : null
})
const canSubmitDelivery = computed(() => {
	return isDesigner.value && winningDesignerId.value === currentUserId.value && [2, 3].includes(detail.value.status)
})
const canChat = computed(() => {
	const status = detail.value.status
	if (![2, 3, 4].includes(status)) return false
	if (isOwner.value) return true
	if (isDesigner.value && winningDesignerId.value === currentUserId.value) return true
	return false
})

const checkRating = async () => {
	if (!detail.value.id) return
	try {
		const rating = await getBountyRatingApi(detail.value.id)
		hasRating.value = !!rating
	} catch (_) {
		hasRating.value = false
	}
}

const goRatingPage = () => {
	uni.navigateTo({ url: `/pages/reward/rating?taskId=${detail.value.id}` })
}

const goEditPage = () => {
	uni.navigateTo({ url: `/pages/reward/publish?taskId=${detail.value.id}` })
}

const goDesignerProfile = (designerId) => {
	if (!designerId) return
	uni.navigateTo({ url: `/pages/reward/designer-profile?designerId=${designerId}` })
}

const goDeliveryPage = () => {
	uni.navigateTo({ url: `/pages/reward/delivery?taskId=${detail.value.id}` })
}

const goMessagesPage = () => {
	uni.navigateTo({ url: `/pages/reward/messages?taskId=${detail.value.id}` })
}

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
	// #ifdef APP-PLUS
	if (typeof plus === 'undefined') return false
	if (plus?.os?.name !== 'Android') return true
	try {
		const EnvUtils = plus.android.importClass('com.alipay.sdk.app.EnvUtils')
		EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX)
		return true
	} catch (error) {
		console.error('设置支付宝沙箱环境失败:', error)
		return false
	}
	// #endif
	// #ifndef APP-PLUS
	return false
	// #endif
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

const syncEscrowStatus = async () => {
	try {
		await syncBountyEscrowPayStatusApi(detail.value.id)
		await loadDetail(detail.value.id)
		uni.showToast({ title: '状态已更新', icon: 'success' })
	} catch (error) {
		uni.showToast({ title: error?.message || '刷新失败', icon: 'none' })
	}
}

const pollEscrowPayStatus = async (taskId, maxRetry = 8) => {
	for (let i = 0; i < maxRetry; i++) {
		try {
			const status = await syncBountyEscrowPayStatusApi(taskId)
			if (status?.paid) {
				return true
			}
		} catch (_) {}
		await sleep(2000)
	}
	return false
}

const payEscrow = async () => {
	try {
		const alipayReady = await ensureAlipayAvailable()
		if (!alipayReady) {
			return
		}
		if (!ensureAlipaySandboxEnv()) {
			return
		}

		uni.showLoading({ title: '创建支付中...' })
		const payResult = await createBountyEscrowPayApi(detail.value.id)
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
		const paid = await pollEscrowPayStatus(detail.value.id)
		uni.hideLoading()
		if (!paid) {
			uni.showToast({ title: '支付结果确认超时，请稍后刷新', icon: 'none' })
			return
		}
		uni.showToast({ title: '托管金支付成功，任务已开始招募', icon: 'success' })
		await loadDetail(detail.value.id)
	} catch (error) {
		uni.hideLoading()
		const message = error?.errMsg || error?.message || '支付失败'
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
		content: '确认已收到成果并完成验收？验收后文件将解锁下载，资金将结算至设计者钱包。',
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
						decision: 1,
						remark: '用户确认验收通过'
					})
					uni.showToast({ title: '验收完成，资金已结算', icon: 'success' })
					loadDetail(detail.value.id)
				} catch (error) {
					uni.showToast({ title: error?.message || '验收失败', icon: 'none' })
				}
			}
		}
	})
}

const rejectDelivery = () => {
	uni.showModal({
		title: '驳回交付',
		content: '确定驳回此次交付？设计者可修改后重新提交。',
		editable: true,
		placeholderText: '请输入驳回原因（可选）',
		success: async (res) => {
			if (res.confirm) {
				if (!detail.value.pendingDeliveryId) {
					uni.showToast({ title: '当前没有可驳回交付', icon: 'none' })
					return
				}
				try {
					await acceptBountyDeliveryApi({
						taskId: detail.value.id,
						deliveryId: detail.value.pendingDeliveryId,
						decision: 2,
						remark: res.content || '发布者驳回交付'
					})
					uni.showToast({ title: '已驳回交付', icon: 'success' })
					loadDetail(detail.value.id)
				} catch (error) {
					uni.showToast({ title: error?.message || '驳回失败', icon: 'none' })
				}
			}
		}
	})
}

const requestCancel = () => {
	uni.showModal({
		title: '取消悬赏',
		content: '取消后托管金将在管理员审核通过后退回您的钱包。',
		editable: true,
		placeholderText: '请输入取消原因',
		success: async (res) => {
			if (res.confirm) {
				try {
					await cancelBountyTaskApi({
						taskId: detail.value.id,
						reason: res.content || '用户主动取消'
					})
					uni.showToast({ title: '取消申请已提交', icon: 'success' })
					loadDetail(detail.value.id)
				} catch (error) {
					uni.showToast({ title: error?.message || '取消失败', icon: 'none' })
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

.detail-container {
	min-height: 100vh;
	background-color: $bg;
	padding-bottom: 140rpx;
}

.status-header {
	position: relative;
	overflow: hidden;
	.header-bg {
		position: absolute;
		inset: 0;
		background: $gradient;
	}
	.header-content {
		position: relative;
		padding: 48rpx 40rpx;
		color: #ffffff;
		display: flex;
		justify-content: space-between;
	}
	.status {
		font-size: 26rpx;
		opacity: 0.9;
		background: rgba(255,255,255,0.2);
		padding: 4rpx 20rpx;
		border-radius: 999rpx;
		display: inline-block;
	}
	.title { font-size: 36rpx; font-weight: 700; margin-top: 12rpx; display: block; }
	.price { font-size: 36rpx; font-weight: 700; display: block; text-align: right; }
	.deadline { font-size: 22rpx; opacity: 0.85; margin-top: 8rpx; display: block; text-align: right; }
}

.section {
	margin: 28rpx 32rpx;
	padding: 32rpx;
	background-color: $card;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.4s ease-out both;
	.sec-title { font-size: 30rpx; font-weight: 700; color: $text-primary; margin-bottom: 20rpx; }
	.desc { font-size: 28rpx; color: $text-secondary; line-height: 1.6; }
	.tags { margin-top: 20rpx; display: flex; gap: 12rpx; flex-wrap: wrap; }
	.tag {
		font-size: 22rpx; color: $text-muted; background-color: $bg;
		padding: 6rpx 20rpx; border-radius: 999rpx;
	}
	.row {
		display: flex; justify-content: space-between; padding: 14rpx 0;
		font-size: 28rpx; color: $text-secondary;
	}
	.pay-actions { display: flex; gap: 16rpx; margin-top: 24rpx; }
	.label { color: $text-muted; }
	.val { color: $text-primary; font-weight: 500; }
	.file {
		display: flex; align-items: center; gap: 10rpx;
		font-size: 24rpx; color: $primary; margin-bottom: 10rpx;
	}
	.file-list { display: flex; gap: 16rpx; flex-wrap: wrap; }
	.thumb-img {
		width: 140rpx;
		height: 140rpx;
		border-radius: 16rpx;
		background-color: rgba(0, 191, 255, 0.06);
		opacity: 0;
		animation: fadeInUp 0.4s ease-out 0.2s forwards;
	}
	.bid-item {
		padding-top: 24rpx; margin-top: 24rpx;
		background-color: $bg;
		border-radius: 20rpx;
		padding: 24rpx;
		margin-top: 16rpx;
	}
	.bid-header {
		display: flex; justify-content: space-between;
		font-size: 28rpx; font-weight: 700; color: $text-primary;
	}
	.bid-left { display: flex; align-items: center; gap: 10rpx; flex-wrap: wrap; }
	.profile-link { font-size: 22rpx; color: $primary; font-weight: 400; }
	.rep-badge {
		font-size: 20rpx;
		padding: 4rpx 16rpx;
		border-radius: 999rpx;
		font-weight: 500;
		&.rep-normal { background-color: rgba(0, 191, 255, 0.1); color: $primary; }
		&.rep-low { background-color: rgba(255,77,109,0.1); color: $danger; }
	}
	.rep-warning {
		display: block;
		font-size: 22rpx;
		color: $danger;
		margin-top: 8rpx;
		font-weight: 400;
	}
	.status-pill {
		font-size: 20rpx;
		padding: 4rpx 16rpx;
		border-radius: 999rpx;
		font-weight: 500;
		&.submitted { background-color: rgba(0, 191, 255, 0.1); color: $primary; animation: breathGlow 2s ease-in-out infinite; }
		&.winner { background-color: rgba(16,185,129,0.1); color: $success; }
		&.lost { background-color: rgba(255,77,109,0.1); color: $danger; }
		&.withdrawn { background-color: $bg; color: $text-secondary; }
	}
	.content { font-size: 26rpx; color: $text-secondary; margin-top: 10rpx; line-height: 1.5; }
	.bid-assets {
		margin-top: 16rpx;
		display: flex;
		flex-wrap: wrap;
		gap: 12rpx;
	}
	.asset-item {
		position: relative;
		width: 140rpx;
		height: 140rpx;
		border-radius: 16rpx;
		overflow: hidden;
		background-color: rgba(0, 191, 255, 0.06);
	}
	.asset-image { width: 100%; height: 100%; }
	.asset-file {
		display: flex;
		flex-direction: column;
		justify-content: center;
		height: 100%;
		padding: 10rpx;
	}
	.asset-type { font-size: 20rpx; color: $primary; }
	.asset-name { font-size: 20rpx; color: $text-secondary; margin-top: 6rpx; }
	.bid-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 16rpx; }
	.bid-actions { display: flex; gap: 12rpx; }
	.time { font-size: 22rpx; color: $text-muted; }
	.btn {
		height: 56rpx; padding: 0 24rpx; font-size: 22rpx;
		border-radius: 999rpx; background: $gradient; color: #ffffff;
		&:active { transform: scale(0.96); }
	}
	.danger { background: linear-gradient(135deg, $danger 0%, #ff6b8a 100%); }
	.step { display: flex; gap: 20rpx; padding: 14rpx 0; }
	.dot {
		width: 14rpx; height: 14rpx; background: $gradient;
		border-radius: 50%; margin-top: 8rpx;
		animation: breathGlow 2s ease-in-out infinite;
	}
	.step-content { flex: 1; }
	.s-title { font-size: 26rpx; color: $text-primary; font-weight: 700; }
	.s-desc { font-size: 24rpx; color: $text-secondary; margin-top: 6rpx; display: block; }
	.s-time { font-size: 22rpx; color: $text-muted; margin-top: 6rpx; display: block; }
}

.card {
	background-color: $card;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
}

.rejected-card {
	.reject-reason {
		display: flex;
		flex-wrap: wrap;
		font-size: 26rpx;
		padding: 12rpx 0;
		.label { color: $text-muted; }
		.reason-text { color: $danger; font-weight: 500; }
	}
	.reject-tip {
		font-size: 24rpx;
		color: $text-secondary;
		margin: 8rpx 0 20rpx;
	}
	.resubmit-btn {
		width: 100%;
		height: 76rpx;
		border-radius: 999rpx;
		font-size: 28rpx;
		background: $gradient;
		color: #ffffff;
		&:active { transform: scale(0.96); }
	}
}

.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	background: rgba(255,255,255,0.72);
	backdrop-filter: blur(24px);
	padding: 20rpx 32rpx;
	padding-bottom: calc(env(safe-area-inset-bottom) + 20rpx);
	display: flex;
	gap: 16rpx;
	box-shadow: 0 -4rpx 24rpx rgba(0,0,0,0.06);
	.btn {
		flex: 1; height: 80rpx; border-radius: 999rpx;
		font-size: 26rpx; background-color: $bg; color: $text-primary;
		&:active { transform: scale(0.96); }
	}
	.primary { background: $gradient; color: #ffffff; }
	.danger { background: linear-gradient(135deg, $danger 0%, #ff6b8a 100%); color: #ffffff; }
}

.delivery-card {
	.delivery-status-row {
		display: flex;
		align-items: center;
		gap: 16rpx;
		margin-bottom: 16rpx;
		.label { font-size: 24rpx; color: $text-muted; }
	}
	.delivery-tag {
		font-size: 22rpx;
		padding: 6rpx 20rpx;
		border-radius: 999rpx;
		font-weight: 500;
	}
	.tag-success { background: rgba(16, 185, 129, 0.12); color: $success; }
	.tag-warning { background: rgba(255, 152, 0, 0.12); color: #ff9800; }
	.tag-info { background: rgba(0, 191, 255, 0.12); color: $primary; animation: breathGlow 2s ease-in-out infinite; }
	.delivery-desc {
		margin-bottom: 20rpx;
		.label { font-size: 24rpx; color: $text-muted; display: block; margin-bottom: 8rpx; }
		.desc-text { font-size: 26rpx; color: $text-primary; line-height: 1.6; }
	}
	.license-section {
		background: rgba(0, 191, 255, 0.04);
		border-radius: 20rpx;
		padding: 24rpx;
		margin-bottom: 20rpx;
	}
	.license-title {
		font-size: 28rpx;
		font-weight: 700;
		color: $text-primary;
		margin-bottom: 16rpx;
	}
	.license-row {
		display: flex;
		justify-content: space-between;
		padding: 10rpx 0;
		.label { font-size: 24rpx; color: $text-muted; }
		.val { font-size: 24rpx; color: $text-primary; }
		.allow { color: $success; font-weight: 500; }
		.deny { color: $danger; font-weight: 500; }
	}
	.file-section {
		margin-top: 4rpx;
	}
	.file-list {
		display: flex;
		flex-direction: column;
		gap: 12rpx;
	}
	.file-item {
		display: flex;
		align-items: center;
		gap: 16rpx;
		padding: 20rpx 24rpx;
		background: rgba(0, 191, 255, 0.04);
		border-radius: 16rpx;
	}
	.file-icon { font-size: 28rpx; }
	.file-name {
		flex: 1; font-size: 26rpx; color: $text-primary;
		overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
	}
	.file-ext {
		font-size: 20rpx; color: $primary;
		background: rgba(0, 191, 255, 0.1);
		padding: 4rpx 16rpx; border-radius: 999rpx;
		text-transform: uppercase; margin-right: 8rpx;
	}
	.file-action { font-size: 24rpx; color: $primary; font-weight: 500; flex-shrink: 0; }
	.file-locked {
		display: flex;
		align-items: center;
		gap: 12rpx;
		padding: 24rpx;
		background: rgba(0, 0, 0, 0.02);
		border-radius: 16rpx;
	}
	.lock-icon { font-size: 32rpx; }
	.lock-text { font-size: 24rpx; color: $text-muted; }
}

.cancel-pending-card {
	.cancel-pending-text {
		font-size: 26rpx;
		color: #ff9800;
		font-weight: 500;
	}
}
</style>
