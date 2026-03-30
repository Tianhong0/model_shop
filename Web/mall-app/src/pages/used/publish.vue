<template>
  <view class="page">
    <!-- Hero -->
    <view class="hero-panel fadeInUp">
      <view class="hero-copy">
        <text class="hero-kicker">FLEA MARKET STUDIO</text>
        <text class="hero-title">像挂闲鱼一样，轻松把闲置发出去</text>
        <text class="hero-sub">不再死填参数，先选成色、入手价和出售风格，再快速生成更自然的转卖信息。</text>
      </view>
      <view class="hero-badge">
        <text class="hero-badge-value">{{ form.imageUrls.length }}/6</text>
        <text class="hero-badge-label">已上传图片</text>
      </view>
    </view>

    <view class="sheet">
      <!-- Basic Info Section -->
      <view class="section fadeInUp">
        <view class="section-head">
          <text class="section-title">基础信息</text>
          <text class="section-tip">先把商品说清楚，后面价格推荐会更准确</text>
        </view>
        <view class="field-card field-card-strong">
          <text class="field-label">标题</text>
          <input v-model="form.title" class="text-input" maxlength="30" placeholder="例如：3D打印龙蛋摆件，展示收藏都好看" />
          <text class="field-foot">{{ form.title.length }}/30，尽量写出材质 / 风格 / 使用场景</text>
        </view>

        <view class="field-card">
          <text class="field-label">分类</text>
          <view class="chip-group">
            <view
              v-for="item in categoryOptions"
              :key="item"
              class="chip"
              :class="{ active: form.categoryName === item }"
              @click="selectCategory(item)"
            >
              {{ item }}
            </view>
          </view>
          <input v-model="form.categoryName" class="text-input compact" maxlength="20" placeholder="也可以自定义分类，如：机械键帽摆件" />
        </view>

        <view class="field-card">
          <text class="field-label">描述</text>
          <textarea
            v-model="form.description"
            class="textarea"
            maxlength="300"
            placeholder="补充尺寸、用途、是否有瑕疵、是否可小刀、适合什么场景。像聊天一样自然描述更容易成交。"
          />
          <view class="suggestion-strip">
            <text class="suggestion-label">快捷短语</text>
            <scroll-view scroll-x class="suggestion-scroll" show-scrollbar="false">
              <view class="suggestion-row">
                <view
                  v-for="item in descriptionSnippets"
                  :key="item"
                  class="snippet-pill"
                  @click="appendSnippet(item)"
                >
                  {{ item }}
                </view>
              </view>
            </scroll-view>
          </view>
        </view>
      </view>

      <!-- Condition & Price Section -->
      <view class="section fadeInUp">
        <view class="section-head">
          <text class="section-title">成色与价格</text>
          <text class="section-tip">参考闲鱼常见发布方式，优先选状态，再定入手价和转卖价</text>
        </view>

        <view class="condition-grid">
          <view
            v-for="item in conditionOptions"
            :key="item.value"
            class="condition-card"
            :class="{ active: form.conditionLevel === item.value }"
            @click="selectCondition(item.value)"
          >
            <text class="condition-title">{{ item.value }}</text>
            <text class="condition-desc">{{ item.desc }}</text>
            <text class="condition-rate">建议保值 {{ item.ratioLabel }}</text>
          </view>
        </view>

        <view class="field-card price-card">
          <view class="price-head">
            <view>
              <text class="field-label">入手价 / 原价</text>
              <text class="field-tip">可手填，也可一键选择常见价位</text>
            </view>
            <text class="price-insight">{{ originalPriceInsight }}</text>
          </view>
          <input v-model="form.originalPrice" class="text-input price-input" type="digit" placeholder="例如 199" />
          <scroll-view scroll-x class="tag-scroll" show-scrollbar="false">
            <view class="tag-row">
              <view
                v-for="item in originalPriceOptions"
                :key="item"
                class="price-tag"
                :class="{ active: String(form.originalPrice) === String(item) }"
                @click="fillOriginalPrice(item)"
              >
                ￥{{ item }}
              </view>
            </view>
          </scroll-view>
        </view>

        <view class="field-card price-card highlight-card">
          <view class="price-head">
            <view>
              <text class="field-label">想卖多少钱</text>
              <text class="field-tip">按成色和入手价给你几档更像真实卖家的报价</text>
            </view>
            <text class="price-insight emphasis">{{ salePriceInsight }}</text>
          </view>
          <input v-model="form.price" class="text-input price-input prominent" type="digit" placeholder="例如 129" />

          <view class="strategy-group">
            <view
              v-for="item in priceStrategies"
              :key="item.key"
              class="strategy-card"
              :class="{ active: selectedStrategy === item.key }"
              @click="applyPriceStrategy(item.key)"
            >
              <text class="strategy-title">{{ item.label }}</text>
              <text class="strategy-desc">{{ item.desc }}</text>
            </view>
          </view>

          <view class="smart-board">
            <view class="smart-board-head">
              <text class="smart-title">智能推荐价</text>
              <text class="smart-sub">点一下直接带入</text>
            </view>
            <view class="recommend-grid">
              <view
                v-for="item in priceRecommendations"
                :key="item.key"
                class="recommend-card"
                :class="{ selected: Number(form.price || 0) === item.price }"
                @click="fillSalePrice(item.price, item.key)"
              >
                <text class="recommend-label">{{ item.label }}</text>
                <text class="recommend-price">￥{{ item.price }}</text>
                <text class="recommend-note">{{ item.note }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- Location & Images Section -->
      <view class="section fadeInUp">
        <view class="section-head">
          <text class="section-title">发货信息与图片</text>
          <text class="section-tip">让买家快速知道你从哪里发、实物长什么样</text>
        </view>

        <view class="field-card">
          <text class="field-label">所在地</text>
          <input v-model="form.location" class="text-input compact" maxlength="20" placeholder="例如：杭州 / 上海浦东 / 深圳龙岗" />
        </view>

        <view class="media-panel">
          <view class="media-head">
            <view>
              <text class="field-label">商品图片</text>
              <text class="field-tip">首图会自动作为封面，建议先放最吸引人的角度</text>
            </view>
            <view class="media-add" @click="chooseImages">+ 添加</view>
          </view>

          <view class="upload-grid">
            <view v-for="(item, index) in form.imageUrls" :key="item + index" class="upload-item">
              <image :src="item" class="upload-img" mode="aspectFill" />
              <view v-if="index === 0" class="cover-badge">封面</view>
              <view class="remove-badge" @click="removeImage(index)">×</view>
            </view>
            <view v-if="form.imageUrls.length < 6" class="upload-add" @click="chooseImages">
              <text class="upload-plus">＋</text>
              <text class="upload-copy">继续上传</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- Footer -->
    <view class="footer-bar">
      <view class="footer-summary">
        <text class="summary-label">当前发布价</text>
        <text class="summary-price">￥{{ summaryPrice }}</text>
      </view>
      <button class="submit-btn" :loading="submitting" @click="submit">立即发布</button>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { createUsedListingApi, uploadUsedImageApi } from '../../api/used'

const categoryOptions = ['桌面摆件', '机甲模型', '收纳工具', '节日礼物', '宠物周边', '游戏周边', '创意配件']

const conditionOptions = [
  { value: '近乎全新', desc: '基本仅展示，边角和表面状态很好', ratio: 0.88, ratioLabel: '88%' },
  { value: '轻微使用痕迹', desc: '有少量把玩或摆放痕迹，不影响观感', ratio: 0.72, ratioLabel: '72%' },
  { value: '明显使用痕迹', desc: '有划痕或掉漆，适合介意不高的买家', ratio: 0.56, ratioLabel: '56%' },
  { value: '瑕疵特价', desc: '存在破损或修补，适合捡漏清仓', ratio: 0.38, ratioLabel: '38%' }
]

const originalPriceOptions = [49, 79, 99, 129, 159, 199, 299, 399, 599, 899]

const priceStrategies = [
  { key: 'fast', label: '急出', desc: '更容易快速成交' },
  { key: 'normal', label: '常规出', desc: '平衡价格与成交率' },
  { key: 'premium', label: '精选价', desc: '适合成色好或稀缺款' }
]

const descriptionSnippets = [
  '实物比照片更精致',
  '仅摆柜展示，几乎没用过',
  '有轻微打印纹路，介意慎拍',
  '支持小刀，爽快可聊',
  '出给真正喜欢的人',
  '打包仔细，发货很快'
]

const submitting = ref(false)
const selectedStrategy = ref('normal')
const form = reactive({
  title: '',
  description: '',
  price: '',
  originalPrice: '',
  conditionLevel: '轻微使用痕迹',
  categoryName: '桌面摆件',
  location: '',
  coverUrl: '',
  imageUrls: []
})

const currentCondition = computed(() => conditionOptions.find(item => item.value === form.conditionLevel) || conditionOptions[1])
const numericOriginalPrice = computed(() => {
  const value = Number(form.originalPrice)
  return Number.isFinite(value) && value > 0 ? value : 0
})

const summaryPrice = computed(() => {
  const value = Number(form.price)
  return Number.isFinite(value) && value > 0 ? value.toFixed(2) : '待设置'
})

const originalPriceInsight = computed(() => {
  if (!numericOriginalPrice.value) return '选一个常见价位会更快'
  return `参考入手价 ￥${numericOriginalPrice.value.toFixed(0)}`
})

const baseSuggestedPrice = computed(() => {
  if (!numericOriginalPrice.value) return 0
  return Math.max(1, Math.round(numericOriginalPrice.value * currentCondition.value.ratio))
})

const salePriceInsight = computed(() => {
  if (!numericOriginalPrice.value) return `当前成色建议保值约 ${currentCondition.value.ratioLabel}`
  return `按${form.conditionLevel}估算，推荐核心价约 ￥${baseSuggestedPrice.value}`
})

const priceRecommendations = computed(() => {
  const base = baseSuggestedPrice.value || 69
  return [
    { key: 'fast', label: '快速出', price: Math.max(1, Math.round(base * 0.9)), note: '更容易当天被问' },
    { key: 'normal', label: '参考价', price: Math.max(1, Math.round(base)), note: '成交率和利润更均衡' },
    { key: 'premium', label: '心动价', price: Math.max(1, Math.round(base * 1.12)), note: '适合成色优或图好看' }
  ]
})

const selectCategory = (value) => {
  form.categoryName = value
}

const appendSnippet = (text) => {
  if (!text) return
  form.description = form.description ? `${form.description}${form.description.endsWith('，') || form.description.endsWith('。') ? '' : '，'}${text}` : text
}

const selectCondition = (value) => {
  form.conditionLevel = value
  applyPriceStrategy(selectedStrategy.value)
}

const fillOriginalPrice = (value) => {
  form.originalPrice = String(value)
  applyPriceStrategy(selectedStrategy.value)
}

const fillSalePrice = (value, strategyKey = selectedStrategy.value) => {
  form.price = String(value)
  selectedStrategy.value = strategyKey
}

const applyPriceStrategy = (strategyKey) => {
  selectedStrategy.value = strategyKey
  const target = priceRecommendations.value.find(item => item.key === strategyKey) || priceRecommendations.value[1]
  if (target) {
    form.price = String(target.price)
  }
}

const chooseImages = () => {
  const remain = 6 - form.imageUrls.length
  if (remain <= 0) {
    uni.showToast({ title: '最多上传6张图片', icon: 'none' })
    return
  }
  uni.chooseImage({
    count: remain,
    success: async (res) => {
      try {
        uni.showLoading({ title: '上传中' })
        const urls = []
        for (const filePath of res.tempFilePaths || []) {
          const url = await uploadUsedImageApi(filePath)
          urls.push(url)
        }
        form.imageUrls = [...form.imageUrls, ...urls].slice(0, 6)
        if (!form.coverUrl && form.imageUrls.length) {
          form.coverUrl = form.imageUrls[0]
        }
      } catch (error) {
        uni.showToast({ title: error.message || '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

const removeImage = (index) => {
  form.imageUrls.splice(index, 1)
  form.coverUrl = form.imageUrls[0] || ''
}

const submit = async () => {
  if (!form.title || !form.description || !form.price || !form.conditionLevel || form.imageUrls.length === 0) {
    uni.showToast({ title: '请完整填写信息', icon: 'none' })
    return
  }
  if (numericOriginalPrice.value && Number(form.price) > numericOriginalPrice.value) {
    uni.showToast({ title: '售价通常不应高于原价，请确认', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await createUsedListingApi({
      title: form.title.trim(),
      description: form.description.trim(),
      price: Number(form.price),
      originalPrice: form.originalPrice ? Number(form.originalPrice) : null,
      conditionLevel: form.conditionLevel,
      categoryName: form.categoryName?.trim(),
      location: form.location?.trim(),
      coverUrl: form.coverUrl || form.imageUrls[0],
      imageUrls: form.imageUrls
    })
    uni.showToast({ title: '发布成功', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: '/pages/used/my-sales' })
    }, 400)
  } catch (error) {
    uni.showToast({ title: error.message || '发布失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$success: #10b981;
$danger: #ff4d6d;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$bg: #f8f8f8;
$card: #ffffff;
$text1: #1a2030;
$text2: #5a6a7a;
$text3: #8a9aaa;
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24rpx); }
  to { opacity: 1; transform: translateY(0); }
}

.fadeInUp { animation: fadeInUp 0.4s ease both; }

.page {
  min-height: 100vh;
  padding: 28rpx 28rpx 200rpx;
  background: $bg;
}

.hero-panel {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 20rpx;
  padding: 28rpx 8rpx 36rpx;
}

.hero-copy { flex: 1; }

.hero-kicker {
  display: block;
  font-size: 20rpx;
  letter-spacing: 4rpx;
  color: $deep;
  opacity: 0.82;
}

.hero-title {
  display: block;
  margin-top: 10rpx;
  color: $text1;
  font-size: 36rpx;
  line-height: 1.2;
  font-weight: 700;
}

.hero-sub {
  display: block;
  margin-top: 14rpx;
  color: $text2;
  font-size: 24rpx;
  line-height: 1.7;
}

.hero-badge {
  width: 174rpx;
  border-radius: 24rpx;
  padding: 22rpx 18rpx;
  background: $card;
  box-shadow: $shadow;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.hero-badge-value {
  display: block;
  color: $deep;
  font-size: 40rpx;
  font-weight: 700;
}

.hero-badge-label {
  display: block;
  margin-top: 8rpx;
  color: $deep;
  font-size: 22rpx;
  line-height: 1.5;
}

.sheet {
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}

.section {
  padding: 32rpx;
  border-radius: 24rpx;
  background: $card;
  box-shadow: $shadow;
}

.section-head { margin-bottom: 20rpx; }

.section-title {
  display: block;
  color: $text1;
  font-size: 30rpx;
  font-weight: 700;
}

.section-tip {
  display: block;
  margin-top: 8rpx;
  color: $text2;
  font-size: 22rpx;
  line-height: 1.6;
}

.field-card {
  margin-top: 20rpx;
  padding: 22rpx;
  border-radius: 24rpx;
  background: #fafbfc;
}

.field-card-strong, .highlight-card {
  background: linear-gradient(180deg, #ffffff 0%, rgba(0, 191, 255, 0.03) 100%);
}

.field-label {
  display: block;
  color: $text1;
  font-size: 28rpx;
  font-weight: 700;
}

.field-tip, .field-foot {
  display: block;
  margin-top: 8rpx;
  color: $text2;
  font-size: 22rpx;
  line-height: 1.6;
}

.text-input, .textarea {
  width: 100%;
  box-sizing: border-box;
  margin-top: 16rpx;
  border-radius: 20rpx;
  background: $card;
  color: $text1;
  font-size: 28rpx;
  box-shadow: inset 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}

.text-input {
  height: 88rpx;
  padding: 0 24rpx;
}

.text-input.compact { height: 82rpx; }

.textarea {
  min-height: 210rpx;
  padding: 22rpx 24rpx;
}

.chip-group, .strategy-group, .recommend-grid, .condition-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 16rpx;
}

.chip {
  padding: 14rpx 24rpx;
  border-radius: 999rpx;
  background: rgba(0, 191, 255, 0.06);
  color: $text2;
  font-size: 24rpx;
  &:active { transform: scale(0.96); }
}

.chip.active {
  background: $deep;
  color: #ffffff;
}

.suggestion-strip { margin-top: 16rpx; }

.suggestion-label, .smart-sub {
  color: $text3;
  font-size: 22rpx;
}

.suggestion-scroll, .tag-scroll {
  white-space: nowrap;
  margin-top: 14rpx;
}

.suggestion-row, .tag-row {
  display: inline-flex;
  gap: 14rpx;
  padding-right: 20rpx;
}

.snippet-pill, .price-tag {
  flex-shrink: 0;
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
  background: $card;
  color: $text2;
  font-size: 24rpx;
  box-shadow: $shadow;
  &:active { transform: scale(0.96); }
}

.price-tag.active {
  background: rgba(0, 191, 255, 0.1);
  color: $deep;
}

.condition-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.condition-card, .strategy-card, .recommend-card {
  border-radius: 24rpx;
  padding: 20rpx;
  background: $card;
  box-shadow: $shadow;
}

.condition-card.active, .strategy-card.active, .recommend-card.selected {
  background: rgba(0, 191, 255, 0.06);
  box-shadow: 0 8rpx 28rpx rgba(0, 191, 255, 0.12);
}

.condition-title, .strategy-title, .recommend-label, .smart-title {
  display: block;
  color: $text1;
  font-size: 26rpx;
  font-weight: 700;
}

.condition-desc, .strategy-desc, .recommend-note {
  display: block;
  margin-top: 8rpx;
  color: $text2;
  font-size: 22rpx;
  line-height: 1.6;
}

.condition-rate {
  display: inline-flex;
  margin-top: 12rpx;
  color: $deep;
  font-size: 22rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(0, 191, 255, 0.08);
}

.price-card { overflow: hidden; }

.price-head, .media-head, .smart-board-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.price-insight {
  color: $text3;
  font-size: 22rpx;
  text-align: right;
}

.price-insight.emphasis { color: $deep; }

.price-input {
  font-size: 34rpx;
  font-weight: 700;
}

.price-input.prominent { color: $deep; }

.smart-board {
  margin-top: 18rpx;
  padding: 20rpx;
  border-radius: 20rpx;
  background: rgba(0, 191, 255, 0.03);
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.recommend-price {
  display: block;
  margin-top: 10rpx;
  color: $deep;
  font-size: 30rpx;
  font-weight: 700;
}

.media-panel {
  margin-top: 20rpx;
  padding: 22rpx;
  border-radius: 24rpx;
  background: #fafbfc;
}

.media-add {
  flex-shrink: 0;
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  background: $deep;
  color: #ffffff;
  font-size: 24rpx;
  &:active { transform: scale(0.96); }
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18rpx;
  margin-top: 18rpx;
}

.upload-item, .upload-add {
  position: relative;
  aspect-ratio: 1;
  border-radius: 24rpx;
  overflow: hidden;
}

.upload-item { background: #e0f2fe; }

.upload-img {
  width: 100%;
  height: 100%;
  opacity: 0;
  animation: fadeInUp 0.4s ease both;
}

.upload-add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 191, 255, 0.04);
  box-shadow: inset 0 0 0 2rpx rgba(0, 191, 255, 0.15);
}

.upload-plus {
  color: $deep;
  font-size: 48rpx;
  line-height: 1;
}

.upload-copy {
  margin-top: 10rpx;
  color: $text2;
  font-size: 22rpx;
}

.cover-badge, .remove-badge {
  position: absolute;
  z-index: 2;
  border-radius: 999rpx;
  font-size: 20rpx;
}

.cover-badge {
  left: 10rpx;
  top: 10rpx;
  padding: 8rpx 14rpx;
  background: rgba(0, 102, 153, 0.76);
  color: #ffffff;
}

.remove-badge {
  right: 10rpx;
  top: 10rpx;
  width: 42rpx;
  height: 42rpx;
  line-height: 42rpx;
  text-align: center;
  background: rgba(255, 255, 255, 0.92);
  color: $danger;
}

.footer-bar {
  position: fixed;
  left: 24rpx; right: 24rpx; bottom: 28rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 18rpx 24rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
  box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.08);
}

.footer-summary { flex: 1; min-width: 0; }

.summary-label {
  display: block;
  color: $text2;
  font-size: 22rpx;
}

.summary-price {
  display: block;
  margin-top: 6rpx;
  color: $deep;
  font-size: 34rpx;
  font-weight: 700;
}

.submit-btn {
  margin: 0;
  width: 260rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  background: $gradient;
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
  box-shadow: 0 8rpx 24rpx rgba(0, 191, 255, 0.22);
  &:active { transform: scale(0.96); }
}
</style>
