<template>
  <div class="page-container">
    <!-- 海报预览卡片 -->
    <div class="preview-card">
      <div class="preview-header">
        <span class="title">海报预览</span>
        <el-button type="primary" size="small" @click="handleSaveAll" :loading="saveLoading">
          保存所有配置
        </el-button>
      </div>
      <div class="preview-content">
        <div class="poster-preview" :style="posterStyle">
          <div class="poster-title" :style="{ color: configForm.POSTER_TITLE_COLOR }">
            {{ configForm.POSTER_TITLE || '印力无限' }}
          </div>
          <div class="poster-subtitle">{{ configForm.POSTER_SUBTITLE || '邀请好友注册获积分奖励' }}</div>
          <div class="qrcode-area">
            <div class="qrcode-placeholder">
              <span>二维码</span>
            </div>
          </div>
          <div class="invite-code-label">邀请码</div>
          <div class="invite-code-box" :style="{ backgroundColor: configForm.POSTER_BG_COLOR_START + '20' }">
            <span :style="{ color: configForm.POSTER_CODE_COLOR }">ABC123</span>
          </div>
          <div class="poster-tips">{{ configForm.POSTER_TIPS_TEXT || '长按保存图片分享给好友' }}</div>
        </div>
      </div>
    </div>

    <!-- 配置表单 -->
    <div class="config-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="海报配置" name="poster">
          <el-form :model="configForm" label-width="120px" class="config-form">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="海报标题">
                  <el-input v-model="configForm.POSTER_TITLE" placeholder="海报标题" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="副标题">
                  <el-input v-model="configForm.POSTER_SUBTITLE" placeholder="副标题" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="标题颜色">
                  <el-color-picker v-model="configForm.POSTER_TITLE_COLOR" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="邀请码颜色">
                  <el-color-picker v-model="configForm.POSTER_CODE_COLOR" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="背景起始色">
                  <el-color-picker v-model="configForm.POSTER_BG_COLOR_START" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="背景结束色">
                  <el-color-picker v-model="configForm.POSTER_BG_COLOR_END" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="背景图片">
              <div class="image-upload-container">
                <el-input v-model="configForm.POSTER_BG_IMAGE" placeholder="背景图片URL（为空则使用渐变色）" style="flex: 1" />
                <el-button type="primary" @click="triggerImageUpload" :loading="uploadLoading">
                  上传图片
                </el-button>
                <input
                  ref="imageInputRef"
                  type="file"
                  accept="image/*"
                  style="display: none"
                  @change="handleImageChange"
                />
              </div>
              <div v-if="configForm.POSTER_BG_IMAGE" class="image-preview-container">
                <img :src="configForm.POSTER_BG_IMAGE" class="image-preview" @error="handleImageError" />
                <el-button type="danger" size="small" @click="clearBgImage" class="clear-btn">清除</el-button>
              </div>
            </el-form-item>
            <el-form-item label="底部提示文字">
              <el-input v-model="configForm.POSTER_TIPS_TEXT" placeholder="底部提示文字" />
            </el-form-item>
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="海报宽度">
                  <el-input-number v-model="configForm.POSTER_WIDTH" :min="100" :max="500" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="海报高度">
                  <el-input-number v-model="configForm.POSTER_HEIGHT" :min="100" :max="800" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="二维码尺寸">
                  <el-input-number v-model="configForm.POSTER_QRCODE_SIZE" :min="30" :max="200" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="奖励配置" name="reward">
          <el-form :model="configForm" label-width="120px" class="config-form">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="邀请注册奖励">
                  <el-input-number v-model="configForm.INVITE_REGISTER_POINTS" :min="0" :max="1000" />
                  <span class="unit">积分</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="首单奖励">
                  <el-input-number v-model="configForm.FIRST_ORDER_POINTS" :min="0" :max="1000" />
                  <span class="unit">积分</span>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="消费返积分比例">
                  <el-input-number v-model="configForm.CONSUME_REBATE_RATE" :min="0" :max="1" :step="0.01" :precision="2" />
                  <span class="unit">（如0.01表示1%）</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="单笔最大返积分">
                  <el-input-number v-model="configForm.MAX_REBATE_POINTS" :min="0" :max="10000" />
                  <span class="unit">积分</span>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="邀请码长度">
                  <el-input-number v-model="configForm.INVITE_CODE_LENGTH" :min="4" :max="10" />
                  <span class="unit">位</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="推广链接域名">
                  <el-input v-model="configForm.PROMOTION_BASE_URL" placeholder="为空则使用默认域名" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="全部配置" name="all">
          <el-table :data="configList" stripe border>
            <el-table-column prop="configKey" label="配置键" width="200" />
            <el-table-column prop="configValue" label="配置值" min-width="200">
              <template #default="scope">
                <el-input v-model="scope.row.configValue" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="configDesc" label="说明" width="200" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                  {{ scope.row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="scope">
                <el-button link type="primary" @click="handleUpdateConfig(scope.row)">保存</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPromotionConfigs, updatePromotionConfig, batchUpdatePromotionConfigs } from '../../api/promotion'
import { uploadFile } from '../../api/model'

const loading = ref(false)
const saveLoading = ref(false)
const uploadLoading = ref(false)
const activeTab = ref('poster')
const configList = ref([])
const imageInputRef = ref(null)

const configForm = reactive({
  POSTER_TITLE: '印力无限',
  POSTER_SUBTITLE: '邀请好友注册，双方均可获得积分奖励',
  POSTER_BG_COLOR_START: '#00bfff',
  POSTER_BG_COLOR_END: '#0099cc',
  POSTER_BG_IMAGE: '',
  POSTER_TITLE_COLOR: '#1a2030',
  POSTER_CODE_COLOR: '#00bfff',
  POSTER_TIPS_TEXT: '长按保存图片，分享给好友',
  POSTER_WIDTH: 200,
  POSTER_HEIGHT: 280,
  POSTER_QRCODE_SIZE: 70,
  INVITE_REGISTER_POINTS: 50,
  FIRST_ORDER_POINTS: 100,
  CONSUME_REBATE_RATE: 0.01,
  MAX_REBATE_POINTS: 500,
  INVITE_CODE_LENGTH: 6,
  PROMOTION_BASE_URL: ''
})

const posterStyle = computed(() => {
  const bgImage = configForm.POSTER_BG_IMAGE
  if (bgImage) {
    return {
      backgroundImage: `url(${bgImage})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    }
  }
  return {
    background: `linear-gradient(180deg, ${configForm.POSTER_BG_COLOR_START} 0%, ${configForm.POSTER_BG_COLOR_END} 100%)`
  }
})

onMounted(() => {
  fetchConfigs()
})

const fetchConfigs = async () => {
  loading.value = true
  try {
    const res = await getPromotionConfigs()
    configList.value = res || []

    // 填充表单
    configList.value.forEach(item => {
      if (configForm.hasOwnProperty(item.configKey)) {
        // 数值类型转换
        if (['POSTER_WIDTH', 'POSTER_HEIGHT', 'POSTER_QRCODE_SIZE', 'INVITE_REGISTER_POINTS', 'FIRST_ORDER_POINTS', 'MAX_REBATE_POINTS', 'INVITE_CODE_LENGTH'].includes(item.configKey)) {
          configForm[item.configKey] = parseInt(item.configValue) || 0
        } else if (['CONSUME_REBATE_RATE'].includes(item.configKey)) {
          configForm[item.configKey] = parseFloat(item.configValue) || 0
        } else {
          configForm[item.configKey] = item.configValue
        }
      }
    })
  } catch (error) {
    ElMessage.error(error.message || '获取配置失败')
  } finally {
    loading.value = false
  }
}

const handleSaveAll = async () => {
  saveLoading.value = true
  try {
    const updates = Object.keys(configForm).map(key => ({
      configKey: key,
      configValue: String(configForm[key])
    }))
    await batchUpdatePromotionConfigs(updates)
    ElMessage.success('保存成功')
    fetchConfigs()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

const handleUpdateConfig = async (row) => {
  try {
    await updatePromotionConfig({
      configKey: row.configKey,
      configValue: row.configValue
    })
    ElMessage.success('保存成功')
    fetchConfigs()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

const triggerImageUpload = () => {
  imageInputRef.value?.click()
}

const handleImageChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }

  // 验证文件大小（最大5MB）
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    return
  }

  uploadLoading.value = true
  try {
    const url = await uploadFile(file, 'poster')
    configForm.POSTER_BG_IMAGE = url
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    uploadLoading.value = false
    // 清空input以便重复选择同一文件
    event.target.value = ''
  }
}

const clearBgImage = () => {
  configForm.POSTER_BG_IMAGE = ''
}

const handleImageError = () => {
  ElMessage.warning('背景图片加载失败，请检查URL是否正确')
}
</script>

<style scoped lang="scss">
.page-container {
  padding: 20px;
}

.preview-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .title {
    font-size: 16px;
    font-weight: 600;
  }
}

.preview-content {
  display: flex;
  justify-content: center;
}

.poster-preview {
  width: 200px;
  height: 280px;
  border-radius: 8px;
  padding: 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #fff;
  position: relative;
}

.poster-title {
  font-size: 16px;
  font-weight: 600;
  text-align: center;
}

.poster-subtitle {
  font-size: 10px;
  margin-top: 5px;
  opacity: 0.8;
  text-align: center;
}

.qrcode-area {
  margin-top: 15px;
  background: #fff;
  border-radius: 8px;
  padding: 10px;
}

.qrcode-placeholder {
  width: 70px;
  height: 70px;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #999;
}

.invite-code-label {
  margin-top: 15px;
  font-size: 11px;
}

.invite-code-box {
  margin-top: 8px;
  padding: 5px 15px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

.poster-tips {
  position: absolute;
  bottom: 15px;
  font-size: 9px;
  opacity: 0.6;
}

.config-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.config-form {
  margin-top: 20px;
}

.unit {
  margin-left: 10px;
  color: #999;
  font-size: 12px;
}

.image-upload-container {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
}

.image-preview-container {
  margin-top: 10px;
  position: relative;
  display: inline-block;
}

.image-preview {
  max-width: 200px;
  max-height: 150px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.clear-btn {
  position: absolute;
  top: 5px;
  right: 5px;
}
</style>
