<template>
  <view class="page">
    <view class="hero-panel">
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
      <view class="section">
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

      <view class="section">
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

      <view class="section">
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
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 180rpx;
  background:
    radial-gradient(circle at top right, rgba(255, 208, 145, 0.55), transparent 28%),
    radial-gradient(circle at left 20%, rgba(255, 236, 196, 0.72), transparent 22%),
    linear-gradient(180deg, #fff8ed 0%, #fffdf9 30%, #f7f3eb 100%);
}

.hero-panel {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 20rpx;
  padding: 22rpx 4rpx 30rpx;
}

.hero-copy {
  flex: 1;
}

.hero-kicker {
  display: block;
  font-size: 20rpx;
  letter-spacing: 4rpx;
  color: #b45309;
  opacity: 0.82;
}

.hero-title {
  display: block;
  margin-top: 10rpx;
  color: #2f1b12;
  font-size: 46rpx;
  line-height: 1.18;
  font-weight: 700;
}

.hero-sub {
  display: block;
  margin-top: 14rpx;
  color: #7c5a41;
  font-size: 24rpx;
  line-height: 1.7;
}

.hero-badge {
  width: 174rpx;
  border-radius: 28rpx;
  padding: 22rpx 18rpx;
  background: linear-gradient(180deg, #fff7df 0%, #fff1cc 100%);
  border: 1px solid rgba(180, 83, 9, 0.12);
  box-shadow: 0 16rpx 32rpx rgba(168, 97, 22, 0.12);
}

.hero-badge-value {
  display: block;
  color: #9a3412;
  font-size: 40rpx;
  font-weight: 700;
}

.hero-badge-label {
  display: block;
  margin-top: 8rpx;
  color: #9a3412;
  font-size: 22rpx;
  line-height: 1.5;
}

.sheet {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.section {
  padding: 26rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(120, 53, 15, 0.08);
  box-shadow: 0 18rpx 46rpx rgba(78, 39, 8, 0.08);
  backdrop-filter: blur(16rpx);
}

.section-head {
  margin-bottom: 18rpx;
}

.section-title {
  display: block;
  color: #2f1b12;
  font-size: 31rpx;
  font-weight: 700;
}

.section-tip {
  display: block;
  margin-top: 8rpx;
  color: #8a6b54;
  font-size: 22rpx;
  line-height: 1.6;
}

.field-card {
  margin-top: 18rpx;
  padding: 22rpx;
  border-radius: 24rpx;
  background: #fffdf9;
  border: 1px solid #f2e6d8;
}

.field-card-strong,
.highlight-card {
  background: linear-gradient(180deg, #fffdf8 0%, #fff7ee 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.86);
}

.field-label {
  display: block;
  color: #3b2417;
  font-size: 27rpx;
  font-weight: 700;
}

.field-tip,
.field-foot {
  display: block;
  margin-top: 8rpx;
  color: #8a6b54;
  font-size: 21rpx;
  line-height: 1.6;
}

.text-input,
.textarea {
  width: 100%;
  box-sizing: border-box;
  margin-top: 16rpx;
  border-radius: 20rpx;
  background: #fff;
  border: 1px solid #eadac8;
  color: #2f1b12;
  font-size: 27rpx;
}

.text-input {
  height: 88rpx;
  padding: 0 24rpx;
}

.text-input.compact {
  height: 82rpx;
}

.textarea {
  min-height: 210rpx;
  padding: 22rpx 24rpx;
}

.chip-group,
.strategy-group,
.recommend-grid,
.condition-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 16rpx;
}

.chip {
  padding: 14rpx 24rpx;
  border-radius: 999rpx;
  background: #fff6ea;
  border: 1px solid #efd7bb;
  color: #7c5a41;
  font-size: 24rpx;
}

.chip.active {
  background: #2f1b12;
  color: #fff4e7;
  border-color: #2f1b12;
}

.suggestion-strip {
  margin-top: 16rpx;
}

.suggestion-label,
.smart-sub {
  color: #9a7b65;
  font-size: 21rpx;
}

.suggestion-scroll,
.tag-scroll {
  white-space: nowrap;
  margin-top: 14rpx;
}

.suggestion-row,
.tag-row {
  display: inline-flex;
  gap: 14rpx;
  padding-right: 20rpx;
}

.snippet-pill,
.price-tag {
  flex-shrink: 0;
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
  background: #fff;
  border: 1px solid #eedfcd;
  color: #7a5b44;
  font-size: 23rpx;
}

.price-tag.active {
  background: #fff0d5;
  color: #9a3412;
  border-color: #f59e0b;
}

.condition-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.condition-card,
.strategy-card,
.recommend-card {
  border-radius: 24rpx;
  padding: 20rpx;
  border: 1px solid #eedcca;
  background: #fff;
}

.condition-card.active,
.strategy-card.active,
.recommend-card.selected {
  background: linear-gradient(180deg, #fff3db 0%, #fff8ef 100%);
  border-color: #e78b2f;
  box-shadow: 0 12rpx 24rpx rgba(231, 139, 47, 0.14);
}

.condition-title,
.strategy-title,
.recommend-label,
.smart-title {
  display: block;
  color: #2f1b12;
  font-size: 26rpx;
  font-weight: 700;
}

.condition-desc,
.strategy-desc,
.recommend-note {
  display: block;
  margin-top: 8rpx;
  color: #8a6b54;
  font-size: 22rpx;
  line-height: 1.6;
}

.condition-rate {
  display: inline-flex;
  margin-top: 12rpx;
  color: #b45309;
  font-size: 21rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: #fff7e7;
}

.price-card {
  overflow: hidden;
}

.price-head,
.media-head,
.smart-board-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.price-insight {
  color: #9a7b65;
  font-size: 21rpx;
  text-align: right;
}

.price-insight.emphasis {
  color: #b45309;
}

.price-input {
  font-size: 34rpx;
  font-weight: 700;
}

.price-input.prominent {
  color: #b45309;
}

.smart-board {
  margin-top: 18rpx;
  padding: 20rpx;
  border-radius: 22rpx;
  background: #fffdfa;
  border: 1px dashed #efd2a9;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.recommend-price {
  display: block;
  margin-top: 10rpx;
  color: #b45309;
  font-size: 30rpx;
  font-weight: 700;
}

.media-panel {
  margin-top: 18rpx;
  padding: 22rpx;
  border-radius: 24rpx;
  background: linear-gradient(180deg, #fffefb 0%, #fff8f0 100%);
  border: 1px solid #f0dfca;
}

.media-add {
  flex-shrink: 0;
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  background: #2f1b12;
  color: #fff3e3;
  font-size: 23rpx;
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18rpx;
  margin-top: 18rpx;
}

.upload-item,
.upload-add {
  position: relative;
  height: 200rpx;
  border-radius: 24rpx;
  overflow: hidden;
}

.upload-item {
  background: #f3e6d8;
}

.upload-img {
  width: 100%;
  height: 100%;
}

.upload-add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1.5px dashed #d8b48d;
  background: linear-gradient(180deg, #fff9f1 0%, #fff3df 100%);
}

.upload-plus {
  color: #b45309;
  font-size: 48rpx;
  line-height: 1;
}

.upload-copy {
  margin-top: 10rpx;
  color: #8a6b54;
  font-size: 22rpx;
}

.cover-badge,
.remove-badge {
  position: absolute;
  z-index: 2;
  border-radius: 999rpx;
  font-size: 20rpx;
}

.cover-badge {
  left: 10rpx;
  top: 10rpx;
  padding: 8rpx 14rpx;
  background: rgba(47, 27, 18, 0.76);
  color: #fff5e8;
}

.remove-badge {
  right: 10rpx;
  top: 10rpx;
  width: 42rpx;
  height: 42rpx;
  line-height: 42rpx;
  text-align: center;
  background: rgba(255, 255, 255, 0.9);
  color: #b91c1c;
}

.footer-bar {
  position: fixed;
  left: 20rpx;
  right: 20rpx;
  bottom: 22rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 18rpx;
  border-radius: 28rpx;
  background: rgba(47, 27, 18, 0.92);
  box-shadow: 0 18rpx 44rpx rgba(47, 27, 18, 0.26);
}

.footer-summary {
  flex: 1;
  min-width: 0;
}

.summary-label {
  display: block;
  color: rgba(255, 243, 227, 0.72);
  font-size: 21rpx;
}

.summary-price {
  display: block;
  margin-top: 6rpx;
  color: #fff6ea;
  font-size: 34rpx;
  font-weight: 700;
}

.submit-btn {
  margin: 0;
  width: 260rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ffb347 0%, #ff8c37 48%, #ff6b2c 100%);
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
  box-shadow: 0 14rpx 28rpx rgba(255, 125, 45, 0.24);
}
</style>
