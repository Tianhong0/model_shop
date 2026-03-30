<template>
  <view class="join-page">
    <scroll-view scroll-y class="scroll-area">
      <!-- 活动信息 -->
      <view class="activity-card">
        <image :src="activity.modelImage || activity.coverImage" class="cover" mode="aspectFill" />
        <view class="info">
          <text class="name">{{ activity.activityName }}</text>
          <view class="price-row">
            <text class="group-price">¥{{ activity.groupPrice }}</text>
            <text class="original-price">¥{{ activity.originalPrice }}</text>
          </view>
        </view>
      </view>

      <!-- 拼团进度 -->
      <view class="progress-card">
        <view class="progress-header">
          <text class="progress-title">拼团进度</text>
          <text class="progress-text">还差{{ group.targetPeople - group.currentPeople }}人成团</text>
        </view>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: (group.currentPeople / group.targetPeople * 100) + '%' }"></view>
        </view>
        <view class="progress-meta">
          <text>{{ group.currentPeople }}/{{ group.targetPeople }}人</text>
          <text class="remaining" v-if="group.remainingSeconds > 0">剩余 {{ formatTime(group.remainingSeconds) }}</text>
        </view>
      </view>

      <!-- 定制参数 -->
      <view class="custom-card">
        <view class="section-title">
          <view class="section-title-left">
            <view class="dot"></view>
            <text>定制参数</text>
          </view>
        </view>

        <view class="options-wrap">
          <!-- 材质选择 -->
          <view class="option-panel" v-if="materials.length > 0">
            <view class="panel-head">
              <text class="label">材料</text>
            </view>
            <view class="option-list">
              <view
                v-for="(item, index) in materials"
                :key="item.id || index"
                class="option-item"
                :class="{ active: selectedMaterial === index, 'eco-material': item.isEco }"
                @click="selectedMaterial = index"
              >
                <text>{{ item.materialName || item.name }}</text>
                <view v-if="item.isEco" class="eco-badge">
                  <uni-icons type="checkbox-filled" size="12" color="#22c55e"></uni-icons>
                  <text class="eco-text">环保</text>
                </view>
              </view>
            </view>
          </view>

          <!-- 颜色选择 -->
          <view class="option-panel">
            <view class="panel-head">
              <text class="label">颜色</text>
              <text class="sub-val">{{ selectedColorValue }}</text>
            </view>
            <view class="color-row">
              <view class="color-preview-circle" :style="{ backgroundColor: selectedColorValue }"></view>
              <text class="color-value">{{ selectedColorName }}</text>
              <view class="color-picker-btn" @click="openColorPanel">选择颜色</view>
            </view>
          </view>

          <!-- 打印精度 -->
          <view class="option-panel">
            <view class="panel-head">
              <text class="label">打印精度</text>
              <input
                class="num-input"
                type="digit"
                :value="precisionInput"
                @input="onPrecisionInput"
                @blur="onPrecisionInputBlur"
              />
            </view>
            <text class="current-text">当前精度 {{ precisionLabel }}</text>
            <slider :value="precisionSliderValue" :min="5" :max="50" @change="onPrecisionChange" activeColor="#ff6b6b" block-size="18" />
            <view class="range-labels">
              <text>0.05mm</text>
              <text>高精细</text>
              <text>0.50mm</text>
            </view>
          </view>

          <!-- 缩放比例 -->
          <view class="option-panel">
            <view class="panel-head">
              <text class="label">缩放比例</text>
              <text class="sub-val">{{ Math.round(scale * 100) }}%</text>
            </view>
            <slider :value="scale * 10" :min="5" :max="20" @change="onScaleChange" activeColor="#ff6b6b" block-size="18" />
            <view class="range-labels">
              <text>50%</text>
              <text>原始</text>
              <text>200%</text>
            </view>
          </view>

          <!-- 填充密度 -->
          <view class="option-panel">
            <view class="panel-head">
              <text class="label">填充密度</text>
              <input
                class="num-input"
                type="number"
                :value="fillDensityInput"
                @input="onFillDensityInput"
                @blur="onFillDensityInputBlur"
              />
            </view>
            <text class="current-text">当前密度 {{ fillDensityLabel }}</text>
            <slider :value="fillDensity" :min="0" :max="100" @change="onFillDensityChange" activeColor="#ff6b6b" block-size="18" />
          </view>

          <!-- 耗材线径 -->
          <view class="option-panel">
            <view class="panel-head">
              <text class="label">耗材线径 (mm)</text>
              <input
                class="num-input"
                type="digit"
                :value="filamentInput"
                @input="onFilamentInput"
                @blur="onFilamentInputBlur"
              />
            </view>
            <text class="current-text">范围 0.50 - 3.00mm</text>
            <slider :value="filamentSliderValue" :min="50" :max="300" @change="onFilamentSliderChange" activeColor="#ff6b6b" block-size="18" />
          </view>

          <!-- 数量选择 -->
          <view class="option-panel">
            <view class="panel-head">
              <text class="label">购买数量</text>
            </view>
            <view class="quantity-wrap">
              <view class="quantity-btn" @click="decreaseQuantity">-</view>
              <text class="quantity-value">{{ quantity }}</text>
              <view class="quantity-btn" @click="increaseQuantity">+</view>
            </view>
          </view>

          <!-- 备注说明 -->
          <view class="option-panel">
            <text class="label">备注说明</text>
            <textarea v-model="customRemark" class="remark-input" placeholder="请输入您的特殊定制要求..." />
          </view>
        </view>
      </view>

      <!-- 继承提示 -->
      <view class="inherit-tip" v-if="hasInheritedParams">
        <uni-icons type="checkbox-filled" size="16" color="#22c55e" />
        <text>已沿用模型定制页的参数设置</text>
      </view>

      <!-- 价格计算 -->
      <view class="price-card">
        <view class="price-row">
          <text class="label">单价</text>
          <text class="value">¥{{ unitPrice }}</text>
        </view>
        <view class="price-row">
          <text class="label">数量</text>
          <text class="value">× {{ quantity }}</text>
        </view>
        <view class="price-row total">
          <text class="label">合计</text>
          <text class="value">¥{{ totalPrice }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="price-info">
        <text class="total-label">合计：</text>
        <text class="total-price">¥{{ totalPrice }}</text>
      </view>
      <button class="submit-btn" @click="handleJoin" :loading="submitting">参与拼团</button>
    </view>

    <!-- 颜色选择弹窗 -->
    <view v-if="showColorPanel" class="color-popup-mask" @click="closeColorPanel">
      <view class="color-popup" @click.stop>
        <view class="popup-title">RGB 颜色设置</view>
        <view class="popup-preview-row">
          <view class="color-preview-circle large" :style="{ backgroundColor: previewColorHex }"></view>
          <text class="popup-color-text">{{ previewColorText }} / {{ previewColorHex }}</text>
        </view>
        <view class="popup-slider-item">
          <text class="popup-label">R</text>
          <slider :value="tempRgbColor.r" :min="0" :max="255" @change="onPopupRgbChange('r', $event)" activeColor="#ef4444" block-size="16" />
          <text class="popup-num">{{ tempRgbColor.r }}</text>
        </view>
        <view class="popup-slider-item">
          <text class="popup-label">G</text>
          <slider :value="tempRgbColor.g" :min="0" :max="255" @change="onPopupRgbChange('g', $event)" activeColor="#22c55e" block-size="16" />
          <text class="popup-num">{{ tempRgbColor.g }}</text>
        </view>
        <view class="popup-slider-item">
          <text class="popup-label">B</text>
          <slider :value="tempRgbColor.b" :min="0" :max="255" @change="onPopupRgbChange('b', $event)" activeColor="#3b82f6" block-size="16" />
          <text class="popup-num">{{ tempRgbColor.b }}</text>
        </view>
        <view class="popup-actions">
          <view class="popup-btn cancel" @click="closeColorPanel">取消</view>
          <view class="popup-btn confirm" @click="confirmColorPanel">确定</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getGroupDetailApi, joinGroupBuyApi, calculateBatchPriceApi } from '@/api/groupBuy'

const groupId = ref(null)
const group = ref({})
const activity = ref({})
const materials = ref([])
const selectedMaterial = ref(0)
const quantity = ref(1)
const unitPrice = ref('0.00')
const submitting = ref(false)
const hasInheritedParams = ref(false)

// 定制参数
const rgbColor = ref({ r: 79, g: 70, b: 229 })
const tempRgbColor = ref({ r: 79, g: 70, b: 229 })
const showColorPanel = ref(false)
const scale = ref(1.0)
const precisionMm = ref(0.1)
const precisionInput = ref('0.10')
const fillDensity = ref(20)
const fillDensityInput = ref('20')
const filamentDiameter = ref(1.75)
const filamentInput = ref('1.75')
const customRemark = ref('')

const totalPrice = computed(() => {
  const price = parseFloat(unitPrice.value) || 0
  return (price * quantity.value).toFixed(2)
})

const selectedColorValue = computed(() => rgbToHex(rgbColor.value))
const selectedColorName = computed(() => `RGB(${rgbColor.value.r}, ${rgbColor.value.g}, ${rgbColor.value.b})`)
const precisionSliderValue = computed(() => Math.round((precisionMm.value || 0.1) * 100))
const precisionLabel = computed(() => `${precisionMm.value.toFixed(2)}mm`)
const fillDensityLabel = computed(() => `${Math.round(fillDensity.value)}%`)
const filamentSliderValue = computed(() => Math.round(filamentDiameter.value * 100))
const previewColorHex = computed(() => rgbToHex(tempRgbColor.value))
const previewColorText = computed(() => `RGB(${tempRgbColor.value.r}, ${tempRgbColor.value.g}, ${tempRgbColor.value.b})`)

const formatTime = (seconds) => {
  if (seconds <= 0) return ''
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}小时${minutes}分`
  return `${minutes}分钟`
}

const clamp = (value, min, max) => Math.min(max, Math.max(min, value))
const toSafeNumber = (value, fallback = 0) => {
  const num = Number(value)
  return Number.isFinite(num) ? num : fallback
}

const rgbToHex = (rgb = {}) => {
  const r = clamp(Math.round(toSafeNumber(rgb.r, 79)), 0, 255)
  const g = clamp(Math.round(toSafeNumber(rgb.g, 70)), 0, 255)
  const b = clamp(Math.round(toSafeNumber(rgb.b, 229)), 0, 255)
  const toHex = (num) => num.toString(16).padStart(2, '0').toUpperCase()
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`
}

const sanitizeDecimalInput = (value, decimalPlaces = 2) => {
  const clean = String(value || '').replace(/[^\d.]/g, '')
  const [intPart, ...rest] = clean.split('.')
  const decimalPart = rest.join('')
  if (rest.length === 0) return intPart
  return `${intPart}.${decimalPart.slice(0, decimalPlaces)}`
}

// 精度
const onPrecisionChange = (event) => {
  const raw = toSafeNumber(event?.detail?.value, 10)
  precisionMm.value = clamp(raw / 100, 0.05, 0.5)
  precisionInput.value = precisionMm.value.toFixed(2)
}

const onPrecisionInput = (event) => {
  precisionInput.value = sanitizeDecimalInput(event?.detail?.value, 2)
}

const onPrecisionInputBlur = () => {
  const next = clamp(toSafeNumber(precisionInput.value, 0.1), 0.05, 0.5)
  precisionMm.value = Number(next.toFixed(2))
  precisionInput.value = precisionMm.value.toFixed(2)
}

// 填充密度
const onFillDensityChange = (event) => {
  const raw = toSafeNumber(event?.detail?.value, 20)
  fillDensity.value = Math.round(clamp(raw, 0, 100))
  fillDensityInput.value = String(fillDensity.value)
}

const onFillDensityInput = (event) => {
  fillDensityInput.value = String(event?.detail?.value || '').replace(/[^\d]/g, '')
}

const onFillDensityInputBlur = () => {
  const next = Math.round(clamp(toSafeNumber(fillDensityInput.value, 20), 0, 100))
  fillDensity.value = next
  fillDensityInput.value = String(next)
}

// 耗材线径
const onFilamentInput = (event) => {
  filamentInput.value = sanitizeDecimalInput(event?.detail?.value, 2)
}

const onFilamentInputBlur = () => {
  const next = clamp(toSafeNumber(filamentInput.value, 1.75), 0.5, 3)
  filamentDiameter.value = Number(next.toFixed(2))
  filamentInput.value = filamentDiameter.value.toFixed(2)
}

const onFilamentSliderChange = (event) => {
  const raw = toSafeNumber(event?.detail?.value, 175)
  const next = clamp(raw / 100, 0.5, 3)
  filamentDiameter.value = Number(next.toFixed(2))
  filamentInput.value = filamentDiameter.value.toFixed(2)
}

// 缩放
const onScaleChange = (e) => {
  const sliderValue = toSafeNumber(e?.detail?.value, 10)
  scale.value = clamp(sliderValue / 10, 0.5, 2)
}

// 颜色选择
const onPopupRgbChange = (channel, event) => {
  const next = clamp(toSafeNumber(event?.detail?.value, 0), 0, 255)
  tempRgbColor.value = { ...tempRgbColor.value, [channel]: Math.round(next) }
}

const openColorPanel = () => {
  tempRgbColor.value = { ...rgbColor.value }
  showColorPanel.value = true
}

const closeColorPanel = () => {
  showColorPanel.value = false
}

const confirmColorPanel = () => {
  rgbColor.value = { ...tempRgbColor.value }
  showColorPanel.value = false
}

const decreaseQuantity = () => {
  if (quantity.value > 1) quantity.value--
}

const increaseQuantity = () => {
  quantity.value++
}

const loadDetail = async () => {
  if (!groupId.value) return

  try {
    const res = await getGroupDetailApi(groupId.value)
    group.value = res || {}
    activity.value = res?.activity || {}
    materials.value = res?.activity?.materials || []

    if (res?.expireTime) {
      const expire = new Date(res.expireTime).getTime()
      const now = Date.now()
      group.value.remainingSeconds = Math.max(0, Math.floor((expire - now) / 1000))
    }

    // 尝试从 localStorage 读取模型定制页保存的参数
    applyStoredCustomParams()

    calculatePrice()
  } catch (e) {
    console.error('加载拼团详情失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

const applyStoredCustomParams = () => {
  const storedParams = uni.getStorageSync('group_buy_custom_params')
  if (!storedParams) return

  // 检查是否匹配当前模型
  if (storedParams.modelId && activity.value.modelId && storedParams.modelId !== activity.value.modelId) {
    return
  }

  hasInheritedParams.value = true

  // 材质
  if (storedParams.materialId && materials.value.length > 0) {
    const idx = materials.value.findIndex(m => m.id === storedParams.materialId)
    if (idx >= 0) selectedMaterial.value = idx
  }

  // 颜色
  if (storedParams.colorRgb) {
    rgbColor.value = { ...storedParams.colorRgb }
    tempRgbColor.value = { ...storedParams.colorRgb }
  }

  // 缩放
  if (storedParams.scale) {
    scale.value = Number(storedParams.scale)
  }

  // 打印精度
  if (storedParams.precision) {
    precisionMm.value = Number(storedParams.precision)
    precisionInput.value = precisionMm.value.toFixed(2)
  }

  // 填充密度
  if (storedParams.fillDensity !== undefined) {
    fillDensity.value = Number(storedParams.fillDensity)
    fillDensityInput.value = String(fillDensity.value)
  }

  // 耗材线径
  if (storedParams.filamentDiameter) {
    filamentDiameter.value = Number(storedParams.filamentDiameter)
    filamentInput.value = filamentDiameter.value.toFixed(2)
  }

  // 备注
  if (storedParams.remark) {
    customRemark.value = storedParams.remark
  }

  // 清除已使用的存储
  uni.removeStorageSync('group_buy_custom_params')
}

const calculatePrice = async () => {
  try {
    const res = await calculateBatchPriceApi({
      modelId: activity.value.modelId,
      materialId: materials.value[selectedMaterial.value]?.id || null,
      quantity: quantity.value
    })
    unitPrice.value = res?.discountedUnitPrice || activity.value.groupPrice
  } catch (e) {
    unitPrice.value = activity.value.groupPrice || '0.00'
  }
}

watch([selectedMaterial, quantity], () => {
  calculatePrice()
})

const handleJoin = async () => {
  if (submitting.value) return

  submitting.value = true
  try {
    const res = await joinGroupBuyApi({
      groupId: groupId.value,
      materialId: materials.value[selectedMaterial.value]?.id || null,
      color: selectedColorValue.value,
      scale: Number(scale.value.toFixed(2)),
      fillPercent: fillDensity.value,
      precision: Number(precisionMm.value.toFixed(2)),
      filamentDiameter: Number(filamentDiameter.value.toFixed(2)),
      quantity: quantity.value,
      note: customRemark.value.trim() || ''
    })

    uni.showToast({ title: '参与成功', icon: 'success' })

    setTimeout(() => {
      uni.redirectTo({
        url: `/pages/group-buy/group-detail?id=${groupId.value}`
      })
    }, 1000)
  } catch (e) {
    console.error('参与拼团失败', e)
    uni.showToast({ title: e?.message || '参与失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onLoad((options) => {
  groupId.value = options?.groupId
  loadDetail()
})
</script>

<style lang="scss" scoped>
$primary: #ff6b6b;
$primary-light: #ff8e53;

.join-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 120rpx;
}

.scroll-area {
  height: calc(100vh - 120rpx);
}

.activity-card {
  display: flex;
  background: #fff;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.cover {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.name {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.activity-card .price-row {
  display: flex;
  align-items: baseline;
  margin-top: 12rpx;
}

.group-price {
  font-size: 32rpx;
  font-weight: bold;
  color: $primary;
}

.original-price {
  font-size: 24rpx;
  color: #999;
  text-decoration: line-through;
  margin-left: 12rpx;
}

.progress-card {
  background: #fff;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.progress-title {
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
}

.progress-text {
  font-size: 26rpx;
  color: $primary;
}

.progress-card .progress-bar {
  height: 12rpx;
  background: #f5f6f8;
  border-radius: 6rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(135deg, $primary 0%, $primary-light 100%);
  border-radius: 6rpx;
  transition: width 0.3s;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.remaining {
  color: #faad14;
}

.custom-card {
  background: #fff;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.section-title-left {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.dot {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  background: $primary;
}

.options-wrap {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.option-panel {
  padding: 24rpx;
  border-radius: 20rpx;
  background: #f8f8f8;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14rpx;
}

.option-panel .label {
  font-size: 26rpx;
  font-weight: 600;
  color: #333;
}

.sub-val {
  font-size: 24rpx;
  color: #5a6a7a;
}

.option-list {
  margin-top: 16rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.option-item {
  padding: 12rpx 28rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  color: #5a6a7a;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 8rpx;

  &:active { transform: scale(0.96); }

  &.active {
    color: $primary;
    background: rgba(255, 107, 107, 0.1);
    box-shadow: inset 0 0 0 2rpx rgba(255, 107, 107, 0.3);
  }

  &.eco-material {
    background: rgba(34, 197, 94, 0.08);
    &.active {
      background: rgba(34, 197, 94, 0.15);
      box-shadow: inset 0 0 0 2rpx rgba(34, 197, 94, 0.5);
    }
  }
}

.eco-badge {
  display: flex;
  align-items: center;
  gap: 4rpx;
  margin-left: 4rpx;
}

.eco-text {
  font-size: 20rpx;
  color: #22c55e;
  font-weight: 500;
}

.current-text {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #5a6a7a;
}

.range-labels {
  margin-top: 6rpx;
  display: flex;
  justify-content: space-between;
  font-size: 20rpx;
  color: #94a3b8;
}

.num-input {
  width: 170rpx;
  height: 58rpx;
  border-radius: 999rpx;
  background: #fff;
  padding: 0 18rpx;
  font-size: 24rpx;
  text-align: right;
  box-sizing: border-box;
}

.color-row {
  margin-top: 14rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.color-preview-circle {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);

  &.large { width: 52rpx; height: 52rpx; }
}

.color-value {
  flex: 1;
  font-size: 24rpx;
  color: #5a6a7a;
}

.color-picker-btn {
  height: 56rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 107, 107, 0.1);
  color: $primary;
  font-size: 24rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;

  &:active { transform: scale(0.96); }
}

.quantity-wrap {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
}

.quantity-btn {
  width: 60rpx;
  height: 60rpx;
  background: #fff;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  color: #333;
}

.quantity-value {
  width: 100rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.remark-input {
  margin-top: 16rpx;
  width: 100%;
  height: 160rpx;
  padding: 20rpx;
  font-size: 24rpx;
  border-radius: 16rpx;
  background: #fff;
  box-sizing: border-box;
}

.inherit-tip {
  display: flex;
  align-items: center;
  gap: 8rpx;
  background: rgba(34, 197, 94, 0.1);
  padding: 16rpx 24rpx;
  margin: 0 24rpx 20rpx;
  border-radius: 12rpx;
}

.inherit-tip text {
  font-size: 24rpx;
  color: #22c55e;
}

.price-card {
  background: #fff;
  padding: 24rpx;
}

.price-card .price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.price-card .label {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 0;
}

.price-card .value {
  font-size: 28rpx;
  color: #333;
}

.price-card .price-row.total {
  padding-top: 16rpx;
  border-top: 1rpx solid #eee;
  margin-bottom: 0;
}

.price-card .total .label {
  font-weight: bold;
  color: #333;
}

.price-card .total .value {
  font-size: 36rpx;
  font-weight: bold;
  color: $primary;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
}

.price-info {
  flex: 1;
}

.total-label {
  font-size: 28rpx;
  color: #666;
}

.total-price {
  font-size: 40rpx;
  font-weight: bold;
  color: $primary;
}

.submit-btn {
  width: 260rpx;
  height: 88rpx;
  background: linear-gradient(135deg, $primary 0%, $primary-light 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
  border-radius: 44rpx;
}

.color-popup-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.color-popup {
  width: 620rpx;
  background-color: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-sizing: border-box;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.12);
}

.popup-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #333;
}

.popup-preview-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 20rpx;
}

.popup-color-text {
  font-size: 24rpx;
  color: #5a6a7a;
}

.popup-slider-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;

  slider { flex: 1; }
}

.popup-label {
  width: 24rpx;
  font-size: 24rpx;
  font-weight: 700;
  color: #333;
}

.popup-num {
  width: 64rpx;
  text-align: right;
  font-size: 24rpx;
  color: #5a6a7a;
}

.popup-actions {
  margin-top: 28rpx;
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
}

.popup-btn {
  min-width: 130rpx;
  height: 68rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 600;

  &:active { transform: scale(0.96); }

  &.cancel {
    background: #f5f6f8;
    color: #5a6a7a;
  }

  &.confirm {
    background: linear-gradient(135deg, $primary 0%, $primary-light 100%);
    color: #fff;
    box-shadow: 0 4rpx 16rpx rgba(255, 107, 107, 0.35);
  }
}
</style>
