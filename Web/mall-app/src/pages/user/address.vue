<template>
	<view class="address-container">
		<view class="select-tip" v-if="selectMode">请选择一个收货地址</view>
		<view class="address-list" v-if="addresses.length > 0">
			<view class="address-item fadeInUp" v-for="(item, index) in addresses" :key="item.id || index" @click="handleAddressItemClick(item)">
				<view class="info">
					<view class="user-info">
						<text class="name">{{item.name}}</text>
						<text class="phone">{{item.phone}}</text>
						<text class="tag" v-if="item.isDefault">默认</text>
					</view>
					<view class="address-detail">
						{{item.province}}{{item.city}}{{item.district}}{{item.detail}}
					</view>
					<view class="default-action" v-if="!item.isDefault" @click.stop="setDefault(index)">设为默认</view>
				</view>
				<view class="actions">
					<view class="action-icon" @click.stop="editAddress(item)">
						<uni-icons type="compose" size="20" color="#8a9aaa"></uni-icons>
					</view>
					<view class="action-icon" @click.stop="deleteAddress(index)">
						<uni-icons type="trash" size="20" color="#ff4d6d"></uni-icons>
					</view>
				</view>
			</view>
		</view>
		<view class="empty-state" v-else>
			<uni-icons type="location" size="64" color="#ccc"></uni-icons>
			<text>暂无收货地址</text>
		</view>

		<view class="bottom-btn">
			<button class="add-btn" @click="addAddress">新增收货地址</button>
		</view>

		<view class="editor-mask" v-if="editorVisible" @click="closeEditor"></view>
		<view class="editor-panel" v-if="editorVisible">
			<view class="editor-title">{{editingIndex >= 0 ? '编辑地址' : '新增地址'}}</view>
			<view class="form-item">
				<text class="label">收货人</text>
				<input class="input" v-model="form.name" placeholder="请输入收货人姓名" />
			</view>
			<view class="form-item">
				<text class="label">手机号</text>
				<input class="input" v-model="form.phone" type="number" maxlength="11" placeholder="请输入手机号" />
			</view>
			<view class="form-item">
				<text class="label">省份</text>
				<input class="input" v-model="form.province" placeholder="如：四川省" />
			</view>
			<view class="form-item">
				<text class="label">城市</text>
				<input class="input" v-model="form.city" placeholder="如：成都市" />
			</view>
			<view class="form-item">
				<text class="label">区县</text>
				<input class="input" v-model="form.district" placeholder="如：武侯区" />
			</view>
			<view class="form-item">
				<text class="label">详细地址</text>
				<input class="input" v-model="form.detail" placeholder="街道门牌信息" />
			</view>
			<view class="form-item switch-item">
				<text class="label">设为默认</text>
				<switch :checked="form.isDefault" @change="onDefaultSwitchChange" color="#00bfff" />
			</view>
			<view class="editor-actions">
				<button class="action-btn cancel" @click="closeEditor">取消</button>
				<button class="action-btn save" @click="saveAddress">保存</button>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const ADDRESS_STORAGE_KEY = 'user_addresses'
const CHECKOUT_SELECTED_ADDRESS_KEY = 'checkout_selected_address_id'
const USED_ORDER_SELECTED_ADDRESS_KEY = 'used_order_selected_address_id'
const addresses = ref([])
const editorVisible = ref(false)
const editingIndex = ref(-1)
const selectMode = ref(false)
const usedOrderMode = ref(false)

const emptyForm = () => ({
	id: null,
	name: '',
	phone: '',
	province: '',
	city: '',
	district: '',
	detail: '',
	isDefault: false
})

const form = ref(emptyForm())

const normalizeAddresses = (list) => {
	if (!Array.isArray(list)) return []
	const parsed = list.map((item, index) => ({
		id: item?.id || `addr_${Date.now()}_${index}`,
		name: item?.name || '',
		phone: item?.phone || '',
		province: item?.province || '',
		city: item?.city || '',
		district: item?.district || '',
		detail: item?.detail || '',
		isDefault: Boolean(item?.isDefault)
	}))
	if (parsed.length > 0 && !parsed.some((item) => item.isDefault)) {
		parsed[0].isDefault = true
	}
	return parsed
}

const saveLocalAddresses = () => {
	uni.setStorageSync(ADDRESS_STORAGE_KEY, addresses.value)
}

onLoad((query = {}) => {
	selectMode.value = String(query.select || '') === '1'
	usedOrderMode.value = String(query.used_order || '') === '1'
})

onMounted(() => {
	const saved = uni.getStorageSync(ADDRESS_STORAGE_KEY)
	if (Array.isArray(saved) && saved.length) {
		addresses.value = normalizeAddresses(saved)
	} else {
		addresses.value = [
			{
				id: `addr_${Date.now()}_0`,
				name: '马军',
				phone: '13800138000',
				province: '四川省',
				city: '成都市',
				district: '武侯区',
				detail: '科华北路 100 号',
				isDefault: true
			}
		]
	}
	saveLocalAddresses()
})

const chooseAddress = (item) => {
	if (!item) {
		return
	}
	if (usedOrderMode.value) {
		uni.setStorageSync(USED_ORDER_SELECTED_ADDRESS_KEY, item.id)
	} else {
		uni.setStorageSync(CHECKOUT_SELECTED_ADDRESS_KEY, item.id)
	}
	uni.navigateBack()
}

const handleAddressItemClick = (item) => {
	if (editorVisible.value) {
		return
	}
	if (selectMode.value) {
		chooseAddress(item)
	}
}

const addAddress = () => {
	editingIndex.value = -1
	form.value = emptyForm()
	form.value.isDefault = addresses.value.length === 0
	editorVisible.value = true
}

const editAddress = (item) => {
	const index = addresses.value.findIndex((addr) => addr.id === item.id)
	if (index < 0) {
		return
	}
	editingIndex.value = index
	form.value = { ...addresses.value[index] }
	editorVisible.value = true
}

const deleteAddress = (index) => {
	uni.showModal({
		title: '提示',
		content: '确定要删除该地址吗？',
		success: (res) => {
			if (res.confirm) {
				const deletingDefault = addresses.value[index]?.isDefault
				addresses.value.splice(index, 1)
				if (deletingDefault && addresses.value.length > 0) {
					addresses.value[0].isDefault = true
				}
				saveLocalAddresses()
				uni.showToast({ title: '删除成功' })
			}
		}
	})
}

const onDefaultSwitchChange = (e) => {
	form.value.isDefault = !!e.detail.value
}

const closeEditor = () => {
	editorVisible.value = false
	editingIndex.value = -1
	form.value = emptyForm()
}

const isPhoneValid = (phone) => /^1[3-9]\d{9}$/.test(String(phone || '').trim())

const setDefault = (index) => {
	addresses.value = addresses.value.map((item, i) => ({
		...item,
		isDefault: i === index
	}))
	saveLocalAddresses()
	uni.showToast({ title: '已设置默认地址', icon: 'none' })
}

const saveAddress = () => {
	const payload = {
		...form.value,
		name: String(form.value.name || '').trim(),
		phone: String(form.value.phone || '').trim(),
		province: String(form.value.province || '').trim(),
		city: String(form.value.city || '').trim(),
		district: String(form.value.district || '').trim(),
		detail: String(form.value.detail || '').trim()
	}

	if (!payload.name || !payload.phone || !payload.province || !payload.city || !payload.district || !payload.detail) {
		uni.showToast({ title: '请完整填写地址信息', icon: 'none' })
		return
	}
	if (!isPhoneValid(payload.phone)) {
		uni.showToast({ title: '手机号格式不正确', icon: 'none' })
		return
	}

	if (!payload.id) {
		payload.id = `addr_${Date.now()}`
	}

	if (editingIndex.value >= 0) {
		addresses.value.splice(editingIndex.value, 1, payload)
	} else {
		addresses.value.unshift(payload)
	}

	if (payload.isDefault) {
		const defaultId = payload.id
		addresses.value = addresses.value.map((item) => ({
			...item,
			isDefault: item.id === defaultId
		}))
	} else if (!addresses.value.some((item) => item.isDefault)) {
		addresses.value[0].isDefault = true
	}

	saveLocalAddresses()
	if (selectMode.value) {
		const selected = addresses.value.find((item) => item.id === payload.id) || payload
		chooseAddress(selected)
		return
	}
	uni.showToast({ title: '保存成功', icon: 'success' })
	closeEditor()
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

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.fadeInUp { animation: fadeInUp 0.4s ease both; }

.address-container {
	min-height: 100vh;
	background-color: $bg;
	padding: 28rpx;
	padding-bottom: 160rpx;
}

.select-tip {
	font-size: 24rpx;
	color: $primary;
	margin-bottom: 20rpx;
	font-weight: 600;
}

.address-item {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 32rpx;
	margin-bottom: 24rpx;
	background: $card;
	border-radius: 24rpx;
	box-shadow: $shadow;

	.info {
		flex: 1;
		.user-info {
			display: flex;
			align-items: center;
			margin-bottom: 14rpx;
			.name { font-size: 32rpx; font-weight: 700; color: $text1; margin-right: 20rpx; }
			.phone { font-size: 28rpx; color: $text2; }
			.tag {
				font-size: 20rpx;
				color: $primary;
				background-color: rgba(0, 191, 255, 0.1);
				padding: 4rpx 16rpx;
				border-radius: 999rpx;
				margin-left: 16rpx;
				font-weight: 600;
			}
		}
		.address-detail {
			font-size: 26rpx;
			color: $text2;
			line-height: 1.5;
		}
		.default-action {
			font-size: 24rpx;
			color: $primary;
			margin-top: 12rpx;
			font-weight: 500;
		}
	}

	.actions {
		display: flex;
		gap: 30rpx;
		margin-left: 20rpx;
		.action-icon {
			display: flex;
			align-items: center;
			justify-content: center;
			width: 48rpx;
			height: 48rpx;
			border-radius: 50%;
			background: $bg;
		}
	}
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 240rpx;
	color: $text3;
	text { margin-top: 24rpx; font-size: 28rpx; }
}

.bottom-btn {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 24rpx 40rpx calc(env(safe-area-inset-bottom) + 24rpx);
	background: rgba(255,255,255,0.72);
	backdrop-filter: blur(24px);
	-webkit-backdrop-filter: blur(24px);
	.add-btn {
		background: $gradient;
		color: #ffffff;
		border-radius: 999rpx;
		font-size: 30rpx;
		font-weight: 600;
		box-shadow: 0 10rpx 30rpx rgba(0, 191, 255, 0.25);
		&:active { transform: scale(0.96); }
	}
}

.editor-mask {
	position: fixed;
	inset: 0;
	background-color: rgba(0, 0, 0, 0.35);
	z-index: 1000;
}

.editor-panel {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	background-color: $card;
	border-radius: 32rpx 32rpx 0 0;
	padding: 36rpx 32rpx calc(env(safe-area-inset-bottom) + 32rpx);
	z-index: 1001;
	box-shadow: 0 -8rpx 40rpx rgba(0, 0, 0, 0.08);

	.editor-title {
		font-size: 36rpx;
		font-weight: 700;
		color: $text1;
		margin-bottom: 24rpx;
	}
	.form-item {
		display: flex;
		align-items: center;
		min-height: 84rpx;
		.label {
			width: 140rpx;
			font-size: 28rpx;
			color: $text2;
			font-weight: 500;
		}
		.input {
			flex: 1;
			font-size: 28rpx;
			color: $text1;
			height: 72rpx;
			background: $bg;
			border-radius: 16rpx;
			padding: 0 20rpx;
		}
		&.switch-item {
			justify-content: space-between;
			.label { width: auto; }
		}
	}
	.editor-actions {
		display: flex;
		gap: 20rpx;
		margin-top: 28rpx;
		.action-btn {
			flex: 1;
			height: 84rpx;
			border-radius: 999rpx;
			font-size: 30rpx;
			font-weight: 600;
			&:active { transform: scale(0.96); }
			&.cancel {
				background-color: $bg;
				color: $text2;
			}
			&.save {
				background: $gradient;
				color: #ffffff;
				box-shadow: 0 6rpx 20rpx rgba(0, 191, 255, 0.25);
			}
		}
	}
}
</style>
