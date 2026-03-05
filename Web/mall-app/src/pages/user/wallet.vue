<template>
	<view class="wallet-container">
		<view class="wallet-header">
			<view class="balance-card">
				<text class="label">可用余额 (元)</text>
				<text class="amount">{{ formatMoney(wallet.availableBalance) }}</text>
				<text class="frozen">冻结：￥{{ formatMoney(wallet.frozenBalance) }}</text>
				<view class="actions">
					<view class="btn outline" @click="goPointPage">积分明细</view>
					<view class="btn outline" @click="submitRecharge">余额充值</view>
					<view class="btn solid" @click="submitWithdraw">申请提现</view>
				</view>
				<view class="withdraw-form">
					<input class="withdraw-input" type="digit" v-model="rechargeAmount" placeholder="输入充值金额，如 100.00" />
				</view>
				<view class="withdraw-form">
					<input class="withdraw-input" type="digit" v-model="withdrawAmount" placeholder="输入提现金额，如 100.00" />
				</view>
				<view class="withdraw-form">
					<input class="withdraw-input" v-model="withdrawAlipayAccount" placeholder="输入提现支付宝账号(沙箱买家账号)" />
				</view>
				<view class="withdraw-form">
					<input class="withdraw-input" v-model="withdrawAlipayName" placeholder="输入支付宝收款人姓名(选填)" />
				</view>
			</view>
		</view>

		<view class="grid-section container">
			<view class="grid-card card">
				<view class="grid-item" @click="goPointPage">
					<text class="val">{{ point.availablePoints }}</text>
					<text class="lab">可用积分</text>
				</view>
				<view class="grid-item">
					<text class="val">{{ point.totalEarned }}</text>
					<text class="lab">累计获得</text>
				</view>
				<view class="grid-item">
					<text class="val">{{ point.totalSpent }}</text>
					<text class="lab">累计消耗</text>
				</view>
			</view>
		</view>

		<view class="history-section container">
			<view class="sec-title">钱包流水</view>
			<view class="history-list card" v-if="ledgerList.length">
				<view class="history-item" v-for="item in ledgerList" :key="item.id">
					<view class="left">
						<text class="title">{{ formatBiz(item) }}</text>
						<text class="time">{{ formatTime(item.createTime) }}</text>
					</view>
					<view class="right" :class="isIncome(item) ? 'in' : 'out'">
						{{ isIncome(item) ? '+' : '-' }}{{ formatMoney(item.amount) }}
					</view>
				</view>
			</view>
			<view v-else class="empty card">暂无流水</view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getWalletAccountApi, getWalletLedgerPageApi, applyWalletWithdrawApi, createWalletRechargePayApi, syncWalletRechargeApi } from '../../api/wallet'
import { getPointAccountApi } from '../../api/point'

const wallet = ref({
	availableBalance: '0.00',
	frozenBalance: '0.00',
	status: 1
})

const point = ref({
	availablePoints: 0,
	totalEarned: 0,
	totalSpent: 0,
	status: 1
})

const withdrawAmount = ref('')
const rechargeAmount = ref('')
const withdrawAlipayAccount = ref('')
const withdrawAlipayName = ref('')
const ledgerList = ref([])

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const ensureAlipayAvailable = async () => {
	// #ifdef APP-PLUS
	if (typeof uni.getProvider !== 'function') return false
	return await new Promise((resolve) => {
		uni.getProvider({
			service: 'payment',
			success: (res) => {
				const providers = Array.isArray(res?.provider) ? res.provider : []
				resolve(providers.includes('alipay'))
			},
			fail: () => resolve(false)
		})
	})
	// #endif
	// #ifndef APP-PLUS
	return false
	// #endif
}

const withSandboxOrderInfo = (orderInfo) => {
	const raw = String(orderInfo || '').trim()
	if (!raw) return raw
	if (/([?&])bizcontext=/.test(raw)) {
		return raw
	}
	const bizContext = encodeURIComponent(JSON.stringify({ appenv: 'system' }))
	return `${raw}&bizcontext=${bizContext}`
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

const pollRechargeStatus = async (outTradeNo, maxRetry = 6) => {
	for (let i = 0; i < maxRetry; i += 1) {
		try {
			const status = await syncWalletRechargeApi(outTradeNo)
			if (Number(status?.status) === 1) {
				return true
			}
		} catch (_) {
			// 忽略单次异常继续重试
		}
		await sleep(2000)
	}
	return false
}

onShow(async () => {
	if (!ensureLoginOrRedirect()) return
	await Promise.all([loadWallet(), loadPoint(), loadLedger()])
})

const loadWallet = async () => {
	try {
		const data = await getWalletAccountApi()
		wallet.value = {
			availableBalance: data?.availableBalance || '0.00',
			frozenBalance: data?.frozenBalance || '0.00',
			status: Number(data?.status ?? 1)
		}
	} catch (error) {
		uni.showToast({ title: error?.message || '钱包加载失败', icon: 'none' })
	}
}

const loadPoint = async () => {
	try {
		const data = await getPointAccountApi()
		point.value = {
			availablePoints: Number(data?.availablePoints || 0),
			totalEarned: Number(data?.totalEarned || 0),
			totalSpent: Number(data?.totalSpent || 0),
			status: Number(data?.status ?? 1)
		}
	} catch (_) {
		// 积分接口异常不阻塞钱包页
	}
}

const loadLedger = async () => {
	try {
		const data = await getWalletLedgerPageApi({ pageNum: 1, pageSize: 20 })
		ledgerList.value = Array.isArray(data?.records) ? data.records : []
	} catch (error) {
		uni.showToast({ title: error?.message || '流水加载失败', icon: 'none' })
	}
}

const submitWithdraw = async () => {
	const amount = Number(withdrawAmount.value || 0)
	if (!Number.isFinite(amount) || amount <= 0) {
		uni.showToast({ title: '请输入有效提现金额', icon: 'none' })
		return
	}
	if (!String(withdrawAlipayAccount.value || '').trim()) {
		uni.showToast({ title: '请输入支付宝收款账号', icon: 'none' })
		return
	}
	try {
		await applyWalletWithdrawApi({
			amount: amount.toFixed(2),
			alipayAccount: String(withdrawAlipayAccount.value || '').trim(),
			alipayRealName: String(withdrawAlipayName.value || '').trim()
		})
		uni.showToast({ title: '提现申请成功', icon: 'success' })
		withdrawAmount.value = ''
		withdrawAlipayAccount.value = ''
		withdrawAlipayName.value = ''
		await Promise.all([loadWallet(), loadLedger()])
	} catch (error) {
		uni.showToast({ title: error?.message || '提现申请失败', icon: 'none' })
	}
}

const submitRecharge = async () => {
	const amount = Number(rechargeAmount.value || 0)
	if (!Number.isFinite(amount) || amount <= 0) {
		uni.showToast({ title: '请输入有效充值金额', icon: 'none' })
		return
	}
	try {
		const alipayReady = await ensureAlipayAvailable()
		if (!alipayReady) {
			throw new Error('当前设备未检测到支付宝支付能力')
		}
		if (!ensureAlipaySandboxEnv()) {
			throw new Error('支付宝沙箱环境初始化失败')
		}
		const payResult = await createWalletRechargePayApi({ amount: amount.toFixed(2) })
		await new Promise((resolve, reject) => {
			uni.requestPayment({
				provider: 'alipay',
				orderInfo: withSandboxOrderInfo(payResult.orderString),
				success: resolve,
				fail: reject
			})
		})
		const paid = await pollRechargeStatus(payResult.outTradeNo, 6)
		if (!paid) {
			uni.showToast({ title: '支付处理中，请稍后刷新', icon: 'none' })
		} else {
			uni.showToast({ title: '充值成功', icon: 'success' })
		}
		rechargeAmount.value = ''
		await Promise.all([loadWallet(), loadLedger()])
	} catch (error) {
		const message = error?.errMsg || error?.message || ''
		if (String(message).includes('requestPayment:fail cancel')) {
			uni.showToast({ title: '已取消支付', icon: 'none' })
			return
		}
		uni.showToast({ title: error?.message || '充值失败', icon: 'none' })
	}
}

const goPointPage = () => {
	uni.navigateTo({ url: '/pages/user/points' })
}

const formatMoney = (value) => {
	const amount = Number(value)
	if (!Number.isFinite(amount)) return '0.00'
	return amount.toFixed(2)
}

const formatBiz = (item) => {
	const map = {
		BOUNTY_RELEASE: '悬赏结算收入',
		WALLET_RECHARGE: '钱包充值',
		ORDER_PAY_BALANCE: '订单余额支付',
		WITHDRAW_FREEZE: '提现冻结',
		WITHDRAW_PAY: '提现打款',
		WITHDRAW_REJECT_UNFREEZE: '提现拒绝解冻',
		WITHDRAW_PAY_FAIL_UNFREEZE: '提现失败解冻',
		BOUNTY_PRICE_REFUND: '悬赏改价退款',
		BOUNTY_PRICE_INCREASE: '悬赏改价补差'
	}
	return map[item?.bizType] || item?.remark || item?.bizType || '余额变动'
}

const isIncome = (item) => {
	const direction = Number(item?.direction)
	return direction === 1 || direction === 4
}

const formatTime = (value) => {
	if (!value) return '--'
	return String(value).replace('T', ' ').slice(0, 19)
}
</script>

<style scoped lang="scss">
.wallet-container {
	min-height: 100vh;
	background-color: #f8fafc;
}

.wallet-header {
	background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
	padding: 60rpx 40rpx 100rpx;
	.balance-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		color: #ffffff;
		.label { font-size: 26rpx; opacity: 0.9; }
		.amount { font-size: 72rpx; font-weight: 700; margin: 20rpx 0 12rpx; }
		.frozen { font-size: 24rpx; opacity: 0.85; }
		.actions {
			display: flex;
			gap: 30rpx;
			margin-top: 24rpx;
			.btn {
				width: 180rpx;
				height: 70rpx;
				display: flex;
				align-items: center;
				justify-content: center;
				border-radius: 35rpx;
				font-size: 28rpx;
				&.outline { border: 2rpx solid rgba(255,255,255,0.6); }
				&.solid { background-color: #ffffff; color: #4f46e5; }
			}
		}
		.withdraw-form {
			margin-top: 20rpx;
			width: 100%;
			max-width: 560rpx;
		}
		.withdraw-input {
			height: 72rpx;
			background: rgba(255,255,255,0.18);
			border-radius: 36rpx;
			padding: 0 24rpx;
			font-size: 26rpx;
			color: #fff;
		}
	}
}

.grid-section {
	margin-top: -60rpx;
	.grid-card {
		display: flex;
		padding: 40rpx 0;
	}
	.grid-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		border-right: 2rpx solid #f1f5f9;
		&:last-child { border-right: none; }
		.val { font-size: 32rpx; font-weight: 700; color: #1e293b; }
		.lab { font-size: 24rpx; color: #64748b; margin-top: 10rpx; }
	}
}

.history-section {
	margin-top: 40rpx;
	.sec-title { font-size: 30rpx; font-weight: 700; color: #1e293b; margin-bottom: 20rpx; }
	.history-list { padding: 0 30rpx; }
	.empty { padding: 30rpx; color: #94a3b8; font-size: 24rpx; }
	.history-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 30rpx 0;
		border-bottom: 2rpx solid #f1f5f9;
		&:last-child { border-bottom: none; }
		.left {
			.title { font-size: 28rpx; color: #1e293b; display: block; }
			.time { font-size: 22rpx; color: #94a3b8; margin-top: 8rpx; display: block; }
		}
		.right {
			font-size: 32rpx;
			font-weight: 700;
			&.in { color: #10b981; }
			&.out { color: #ef4444; }
		}
	}
}
</style>
