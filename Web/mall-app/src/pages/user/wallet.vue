<template>
	<view class="wallet-container">
		<view class="wallet-header">
			<view class="balance-card">
				<text class="label">可用余额 (元)</text>
				<text class="amount">{{ formatMoney(wallet.availableBalance) }}</text>
				<view class="frozen-row" @click="showFrozenDetail">
					<text class="frozen">冻结：￥{{ formatMoney(wallet.frozenBalance) }}</text>
					<text class="frozen-tip" v-if="Number(wallet.frozenBalance) > 0">点击查看详情</text>
				</view>
				<view class="actions">
					<view class="btn outline" @click="goPointPage">积分明细</view>
					<view class="btn solid" @click="submitRecharge">余额充值</view>
					<view class="btn outline" @click="toggleWithdrawForm">{{ showWithdrawForm ? '收起' : '申请提现' }}</view>
				</view>
			</view>
		</view>

		<!-- 充值输入区 -->
		<view class="input-section">
			<view class="input-card">
				<view class="input-header">
					<text class="input-title">余额充值</text>
					<text class="input-subtitle">充值即时到账</text>
				</view>
				<view class="input-row">
					<text class="input-prefix">¥</text>
					<input class="input-field" type="digit" v-model="rechargeAmount" placeholder="输入充值金额" />
				</view>
				<view class="quick-amounts">
					<view class="quick-btn" :class="{ active: rechargeAmount === '50' }" @click="rechargeAmount = '50'">50</view>
					<view class="quick-btn" :class="{ active: rechargeAmount === '100' }" @click="rechargeAmount = '100'">100</view>
					<view class="quick-btn" :class="{ active: rechargeAmount === '200' }" @click="rechargeAmount = '200'">200</view>
					<view class="quick-btn" :class="{ active: rechargeAmount === '500' }" @click="rechargeAmount = '500'">500</view>
				</view>
			</view>
		</view>

		<!-- 提现输入区 -->
		<view class="input-section" v-if="showWithdrawForm">
			<view class="input-card">
				<view class="input-header">
					<text class="input-title">申请提现</text>
					<text class="input-subtitle">提现至支付宝账户</text>
				</view>
				<view class="input-row">
					<text class="input-prefix">¥</text>
					<input class="input-field" type="digit" v-model="withdrawAmount" placeholder="输入提现金额" />
					<view class="input-suffix" @click="withdrawAmount = wallet.availableBalance">全部</view>
				</view>
				<view class="input-label">支付宝收款账号</view>
				<view class="input-row plain">
					<input class="input-field full" v-model="withdrawAlipayAccount" placeholder="请输入支付宝账号" />
				</view>
				<view class="input-label">收款人姓名<text class="optional">（选填）</text></view>
				<view class="input-row plain">
					<input class="input-field full" v-model="withdrawAlipayName" placeholder="请输入真实姓名" />
				</view>
				<view class="submit-btn" @click="submitWithdraw">确认提现</view>
			</view>
		</view>

		<!-- 冻结资金详情弹窗 -->
		<view class="frozen-modal" v-if="frozenModalVisible" @click="closeFrozenModal">
			<view class="frozen-modal-content" @click.stop>
				<view class="frozen-modal-title">冻结资金明细</view>
				<view class="frozen-modal-list" v-if="frozenList.length">
					<view class="frozen-item" v-for="item in frozenList" :key="item.id">
						<view class="frozen-item-left">
							<text class="frozen-item-amount">￥{{ formatMoney(item.amount) }}</text>
							<text class="frozen-item-remark">{{ item.remark || '二手交易收入冻结' }}</text>
							<text class="frozen-item-time">冻结时间：{{ formatTime(item.createTime) }}</text>
						</view>
						<view class="frozen-item-right">
							<text class="frozen-item-date">解冻日期</text>
							<text class="frozen-item-unfreeze">{{ formatDate(item.frozenEndTime) }}</text>
							<text class="frozen-item-countdown">{{ item.remainingDays || 0 }}天{{ item.remainingHours || 0 }}小时后解冻</text>
						</view>
					</view>
				</view>
				<view class="frozen-empty" v-else>暂无冻结资金</view>
				<view class="frozen-modal-close" @click="closeFrozenModal">关闭</view>
			</view>
		</view>

		<view class="grid-section">
			<view class="grid-card">
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

		<view class="history-section">
			<view class="sec-title">钱包流水</view>
			<view class="history-list" v-if="ledgerList.length">
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
			<view v-else class="empty">暂无流水</view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getWalletAccountApi, getWalletLedgerPageApi, applyWalletWithdrawApi, createWalletRechargePayApi, syncWalletRechargeApi, getWalletFrozenListApi } from '../../api/wallet'
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
const frozenModalVisible = ref(false)
const frozenList = ref([])
const showWithdrawForm = ref(false)

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

const showFrozenDetail = async () => {
	if (Number(wallet.value.frozenBalance) <= 0) {
		uni.showToast({ title: '暂无冻结资金', icon: 'none' })
		return
	}
	try {
		const data = await getWalletFrozenListApi()
		frozenList.value = Array.isArray(data) ? data : []
		frozenModalVisible.value = true
	} catch (error) {
		uni.showToast({ title: error?.message || '获取冻结详情失败', icon: 'none' })
	}
}

const closeFrozenModal = () => {
	frozenModalVisible.value = false
}

const toggleWithdrawForm = () => {
	showWithdrawForm.value = !showWithdrawForm.value
}

const formatMoney = (value) => {
	const amount = Number(value)
	if (!Number.isFinite(amount)) return '0.00'
	return amount.toFixed(2)
}

const formatBiz = (item) => {
	const map = {
		BOUNTY_RELEASE: '悬赏结算冻结',
		BOUNTY_RELEASE_UNFREEZE: '悬赏结算解冻',
		USED_ORDER_SELL_INCOME: '二手交易收入冻结',
		USED_ORDER_SELL_INCOME_UNFREEZE: '二手交易收入解冻',
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
	// 1: 收入, 3: 冻结(也是收入，只是暂时冻结), 4: 解冻
	return direction === 1 || direction === 3 || direction === 4
}

const formatTime = (value) => {
	if (!value) return '--'
	return String(value).replace('T', ' ').slice(0, 19)
}

const formatDate = (value) => {
	if (!value) return '--'
	return String(value).replace('T', ' ').slice(0, 10)
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
$text1: #1a2030;
$text2: #5a6a7a;
$text3: #8a9aaa;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

.wallet-container {
	min-height: 100vh;
	background: $bg;
	padding-bottom: 40rpx;
}

.wallet-header {
	background: $gradient;
	padding: 60rpx 40rpx 80rpx;
	border-bottom-left-radius: 40rpx;
	border-bottom-right-radius: 40rpx;
	.balance-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		color: #ffffff;
		.label { font-size: 26rpx; opacity: 0.9; }
		.amount { font-size: 72rpx; font-weight: 700; margin: 20rpx 0 12rpx; letter-spacing: 2rpx; }
		.frozen-row {
			display: flex;
			align-items: center;
			gap: 16rpx;
		}
		.frozen { font-size: 24rpx; opacity: 0.85; }
		.frozen-tip { font-size: 22rpx; opacity: 0.7; text-decoration: underline; }
		.actions {
			display: flex;
			gap: 24rpx;
			margin-top: 28rpx;
			.btn {
				width: 180rpx;
				height: 70rpx;
				display: flex;
				align-items: center;
				justify-content: center;
				border-radius: 999rpx;
				font-size: 28rpx;
				transition: transform 0.15s;
				&:active { transform: scale(0.96); }
				&.outline { border: 2rpx solid rgba(255,255,255,0.6); }
				&.solid { background-color: #ffffff; color: $primary; font-weight: 600; }
			}
		}
	}
}

// 输入区域样式
.input-section {
	padding: 28rpx;
	margin-top: -20rpx;
}

.input-card {
	background: $card;
	border-radius: 24rpx;
	padding: 32rpx;
	box-shadow: $shadow;
}

.input-header {
	margin-bottom: 24rpx;
	.input-title {
		font-size: 32rpx;
		font-weight: 700;
		color: $text1;
	}
	.input-subtitle {
		font-size: 24rpx;
		color: $text3;
		margin-left: 16rpx;
	}
}

.input-row {
	display: flex;
	align-items: center;
	background: #f5f7fa;
	border-radius: 16rpx;
	padding: 0 24rpx;
	height: 100rpx;
	&.plain {
		background: #f5f7fa;
		height: 88rpx;
	}
}

.input-prefix {
	font-size: 44rpx;
	font-weight: 700;
	color: $text1;
	margin-right: 12rpx;
}

.input-field {
	flex: 1;
	font-size: 36rpx;
	color: $text1;
	font-weight: 500;
	&.full {
		font-size: 30rpx;
	}
}

.input-suffix {
	font-size: 26rpx;
	color: $primary;
	font-weight: 500;
	padding: 8rpx 16rpx;
}

.input-label {
	font-size: 26rpx;
	color: $text2;
	margin-top: 24rpx;
	margin-bottom: 12rpx;
	.optional {
		font-size: 24rpx;
		color: $text3;
	}
}

// 快捷金额
.quick-amounts {
	display: flex;
	gap: 16rpx;
	margin-top: 24rpx;
}

.quick-btn {
	flex: 1;
	height: 72rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: #f5f7fa;
	border-radius: 12rpx;
	font-size: 28rpx;
	color: $text2;
	font-weight: 500;
	transition: all 0.2s;
	&.active {
		background: rgba($primary, 0.1);
		color: $primary;
		font-weight: 600;
	}
	&:active {
		transform: scale(0.96);
	}
}

// 提交按钮
.submit-btn {
	margin-top: 32rpx;
	height: 96rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: $gradient;
	border-radius: 48rpx;
	font-size: 32rpx;
	color: #fff;
	font-weight: 600;
	&:active {
		opacity: 0.9;
	}
}

.container {
	padding: 0 28rpx;
}

.grid-section {
	margin-top: -60rpx;
	padding: 0 28rpx;
	.grid-card {
		display: flex;
		padding: 40rpx 0;
		background: $card;
		border-radius: 24rpx;
		box-shadow: $shadow;
	}
	.grid-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		.val { font-size: 32rpx; font-weight: 700; color: $text1; }
		.lab { font-size: 24rpx; color: $text2; margin-top: 10rpx; }
	}
}

.history-section {
	margin-top: 36rpx;
	padding: 0 28rpx;
	.sec-title { font-size: 30rpx; font-weight: 700; color: $text1; margin-bottom: 20rpx; }
	.history-list {
		padding: 8rpx 32rpx;
		background: $card;
		border-radius: 24rpx;
		box-shadow: $shadow;
	}
	.empty {
		padding: 40rpx 32rpx;
		color: $text3;
		font-size: 28rpx;
		background: $card;
		border-radius: 24rpx;
		box-shadow: $shadow;
	}
	.history-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 28rpx 0;
		& + .history-item { border-top: 1rpx solid rgba(0,0,0,0.04); }
		.left {
			.title { font-size: 28rpx; color: $text1; display: block; font-weight: 500; }
			.time { font-size: 22rpx; color: $text3; margin-top: 8rpx; display: block; }
		}
		.right {
			font-size: 32rpx;
			font-weight: 700;
			&.in { color: $success; }
			&.out { color: $danger; }
		}
	}
}

.frozen-modal {
	position: fixed;
	inset: 0;
	background: rgba(0, 0, 0, 0.5);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 100;
}

.frozen-modal-content {
	width: 90%;
	max-height: 70vh;
	background: $card;
	border-radius: 24rpx;
	padding: 32rpx;
	box-sizing: border-box;
}

.frozen-modal-title {
	font-size: 34rpx;
	font-weight: 700;
	color: $text1;
	text-align: center;
	margin-bottom: 24rpx;
}

.frozen-modal-list {
	max-height: 50vh;
	overflow-y: auto;
}

.frozen-item {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 24rpx 0;
	border-bottom: 1rpx solid rgba(0, 0, 0, 0.04);

	&:last-child {
		border-bottom: none;
	}
}

.frozen-item-left {
	flex: 1;
}

.frozen-item-amount {
	font-size: 32rpx;
	font-weight: 700;
	color: $text1;
	display: block;
}

.frozen-item-remark {
	font-size: 24rpx;
	color: $text2;
	margin-top: 8rpx;
	display: block;
}

.frozen-item-time {
	font-size: 22rpx;
	color: $text3;
	margin-top: 6rpx;
	display: block;
}

.frozen-item-right {
	text-align: right;
	padding-left: 20rpx;
}

.frozen-item-date {
	font-size: 22rpx;
	color: $text3;
	display: block;
}

.frozen-item-unfreeze {
	font-size: 30rpx;
	font-weight: 600;
	color: $primary;
	margin-top: 6rpx;
	display: block;
}

.frozen-item-countdown {
	font-size: 22rpx;
	color: $success;
	margin-top: 6rpx;
	display: block;
	background: rgba($success, 0.1);
	padding: 6rpx 12rpx;
	border-radius: 8rpx;
}

.frozen-empty {
	padding: 40rpx;
	text-align: center;
	color: $text3;
	font-size: 28rpx;
}

.frozen-modal-close {
	margin-top: 24rpx;
	height: 80rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: $gradient;
	color: #fff;
	border-radius: 999rpx;
	font-size: 28rpx;
	font-weight: 600;
}
</style>
