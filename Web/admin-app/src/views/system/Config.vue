<template>
  <div class="page-container">
    <div class="modern-card" style="max-width: 980px; margin: 0 auto;">
      <div class="card-title">系统核心配置</div>
      <el-form :model="configStore" label-width="140px" label-position="left">
        <el-divider content-position="left">基础显示设置</el-divider>
        <el-form-item label="系统标题">
          <el-input v-model="configStore.siteName" placeholder="管理系统名称" />
        </el-form-item>
        <el-form-item label="运营状态">
          <el-switch
            v-model="configStore.siteStatus"
            active-text="正常营业"
            inactive-text="系统维护"
            active-color="#10b981"
            :loading="statusUpdating"
            @change="handleStatusChange"
          />
        </el-form-item>

        <el-divider content-position="left">界面风格设置</el-divider>
        <el-form-item label="主题颜色">
          <el-color-picker v-model="configStore.themeColor" />
        </el-form-item>
        <el-form-item label="深色侧边栏">
          <el-switch v-model="configStore.sidebarDark" />
        </el-form-item>
        <el-form-item label="系统图标">
          <div class="icon-config">
            <div class="icon-preview">
              <div class="preview-box">
                <img v-if="configStore.siteIconUrl" :src="configStore.siteIconUrl" class="preview-image" alt="icon" />
                <el-icon v-else :size="24"><component :is="configStore.siteIcon || 'Cpu'" /></el-icon>
              </div>
              <span class="preview-label">预览</span>
            </div>
            <div class="icon-options">
              <el-select v-model="configStore.siteIcon" placeholder="选择内置图标" style="width: 200px" clearable>
                <el-option
                  v-for="icon in iconList"
                  :key="icon"
                  :label="icon"
                  :value="icon"
                >
                  <div class="icon-option-item">
                    <el-icon :size="18"><component :is="icon" /></el-icon>
                    <span>{{ icon }}</span>
                  </div>
                </el-option>
              </el-select>
              <div class="or-divider">或</div>
              <el-upload
                class="icon-upload"
                :show-file-list="false"
                :before-upload="beforeIconUpload"
                :http-request="handleIconUpload"
                accept="image/*"
              >
                <el-button type="primary" plain :loading="iconUploading">
                  <el-icon><Upload /></el-icon>
                  {{ iconUploading ? '上传中...' : '上传自定义图标' }}
                </el-button>
              </el-upload>
              <el-button
                v-if="configStore.siteIconUrl"
                type="danger"
                plain
                @click="clearCustomIcon"
              >
                清除自定义图标
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-divider content-position="left">小程序内容配置（后端实时）</el-divider>
        <div class="ops-header">
          <el-button type="primary" plain @click="goBannerManage">进入轮播管理</el-button>
          <el-button type="primary" plain @click="goNoticeManage">进入公告管理</el-button>
          <el-button :loading="loading" @click="fetchHomeConfig">刷新数据</el-button>
        </div>

        <el-form-item label="首页轮播预览">
          <el-table :data="homeBanners" border stripe style="width: 100%">
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column label="图片" width="140">
              <template #default="scope">
                <el-image :src="scope.row.imageUrl" style="width: 100px; height: 56px; border-radius: 6px" fit="cover" />
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
                  {{ scope.row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sortNo" label="排序" width="100" />
          </el-table>
        </el-form-item>

        <el-form-item label="首页公告预览">
          <el-table :data="homeNotices" border stripe style="width: 100%">
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="level" label="级别" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
                  {{ scope.row.status === 1 ? '发布' : '草稿' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
          </el-table>
        </el-form-item>

        <el-form-item style="margin-top: 40px">
          <el-button :loading="saving" type="primary" size="large" style="padding: 0 40px" @click="handleSave">应用界面配置</el-button>
          <el-button size="large" @click="handleReset">恢复默认</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Upload } from '@element-plus/icons-vue'
import { useConfigStore } from '../../store/config'
import { getAdminOperationStatus, getHomeConfig, updateAdminOperationStatus } from '../../api/operation'
import { uploadFile } from '../../api/model'

const router = useRouter()
const configStore = useConfigStore()

const loading = ref(false)
const saving = ref(false)
const statusUpdating = ref(false)
const iconUploading = ref(false)
const homeBanners = ref([])
const homeNotices = ref([])

// 常用图标列表（Element Plus 图标）
const iconList = [
  'Cpu', 'Monitor', 'HomeFilled', 'House', 'Shop', 'Goods', 'GoodsFilled',
  'Box', 'Grid', 'Menu', 'Apps', 'List',
  'Document', 'DocumentFilled', 'Folder', 'FolderFilled', 'Files',
  'Star', 'StarFilled', 'Collection', 'CollectionFilled',
  'TrendCharts', 'DataAnalysis', 'DataLine', 'DataBoard', 'PieChart',
  'User', 'UserFilled', 'Avatar', 'People', 'Management',
  'Setting', 'SettingFilled', 'Tools', 'Operation',
  'Bell', 'BellFilled', 'Notification', 'Message', 'ChatDotRound',
  'Search', 'ZoomIn', 'View', 'Hide', 'Filter',
  'Download', 'Upload', 'UploadFilled', 'Link', 'Connection',
  'Location', 'LocationFilled', 'MapLocation', 'Compass',
  'Calendar', 'CalendarFilled', 'Clock', 'Timer',
  'Phone', 'PhoneFilled', 'Iphone', 'Cellphone',
  'Promotion', 'Position', 'Flag', 'FlagFilled', 'Medal',
  'Trophy', 'TrophyFilled', 'Present', 'PresentFilled', 'Gift',
  'Wallet', 'WalletFilled', 'CreditCard', 'Money', 'Coin',
  'Picture', 'PictureFilled', 'PictureRounded', 'Camera', 'CameraFilled',
  'VideoCamera', 'VideoCameraFilled', 'Film', 'Microphone',
  'Headset', 'Service'
]

// 图标上传前验证
const beforeIconUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 上传自定义图标
const handleIconUpload = async (options) => {
  iconUploading.value = true
  try {
    const url = await uploadFile(options.file, 'icon')
    configStore.siteIconUrl = url
    configStore.saveToStorage()
    ElMessage.success('图标上传成功')
  } catch (error) {
    ElMessage.error(error?.message || '上传失败')
  } finally {
    iconUploading.value = false
  }
}

// 清除自定义图标
const clearCustomIcon = () => {
  configStore.siteIconUrl = ''
  configStore.saveToStorage()
  ElMessage.success('已清除自定义图标')
}

const fetchOperationStatus = async () => {
  try {
    const data = await getAdminOperationStatus()
    configStore.siteStatus = Boolean(data?.operating)
  } catch (e) {
    ElMessage.error('获取运营状态失败')
  }
}

const handleStatusChange = async (val) => {
  statusUpdating.value = true
  try {
    await updateAdminOperationStatus({ operating: val })
    ElMessage.success(val ? '系统已开启正常营业' : '系统已进入维护模式')
  } catch (e) {
    // 恢复原状态
    configStore.siteStatus = !val
    ElMessage.error(e?.message || '更新运营状态失败')
  } finally {
    statusUpdating.value = false
  }
}

const fetchHomeConfig = async () => {
  loading.value = true
  try {
    const data = await getHomeConfig()
    homeBanners.value = data?.banners || []
    homeNotices.value = data?.notices || []
  } catch (e) {
    ElMessage.error('获取首页配置失败')
  } finally {
    loading.value = false
  }
}

const goBannerManage = () => {
  router.push('/operation/banners')
}

const goNoticeManage = () => {
  router.push('/operation/notices')
}

const handleSave = async () => {
  saving.value = true
  try {
    await updateAdminOperationStatus({ operating: Boolean(configStore.siteStatus) })
    // 应用主题颜色
    configStore.applyTheme()
    ElMessage.success('界面配置已应用')
  } catch (e) {
    ElMessage.error(e?.message || '保存配置失败')
  } finally {
    saving.value = false
  }
}

const handleReset = () => {
  configStore.$reset()
  // 重置后应用默认主题
  configStore.applyTheme()
  ElMessage.info('界面配置已重置')
}

onMounted(fetchHomeConfig)
onMounted(fetchOperationStatus)
</script>

<style scoped>
.page-container {
  padding: 0;
}

.modern-card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 32px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.card-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 32px;
  color: var(--text-primary);
  text-align: center;
}

.ops-header {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.icon-config {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.icon-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.preview-box {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--primary-color) 0%, #818cf8 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
  overflow: hidden;
  flex-shrink: 0;
}

.preview-box .el-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-image {
  width: 28px;
  height: 28px;
  object-fit: contain;
  display: block;
}

.preview-label {
  font-size: 12px;
  color: var(--text-muted);
}

.icon-options {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.or-divider {
  color: var(--text-muted);
  font-size: 14px;
}

.icon-option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 0;
}

.icon-option-item .el-icon {
  flex-shrink: 0;
}

:deep(.el-select-dropdown__item) {
  height: auto !important;
  padding: 8px 12px !important;
}
</style>
