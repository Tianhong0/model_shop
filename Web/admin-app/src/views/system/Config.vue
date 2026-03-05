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
          />
        </el-form-item>

        <el-divider content-position="left">界面风格设置</el-divider>
        <el-form-item label="主题颜色">
          <el-color-picker v-model="configStore.themeColor" />
        </el-form-item>
        <el-form-item label="深色侧边栏">
          <el-switch v-model="configStore.sidebarDark" />
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
import { useConfigStore } from '../../store/config'
import { getAdminOperationStatus, getHomeConfig, updateAdminOperationStatus } from '../../api/operation'

const router = useRouter()
const configStore = useConfigStore()

const loading = ref(false)
const saving = ref(false)
const homeBanners = ref([])
const homeNotices = ref([])

const fetchOperationStatus = async () => {
  try {
    const data = await getAdminOperationStatus()
    configStore.siteStatus = Boolean(data?.operating)
  } catch (e) {
    ElMessage.error('获取运营状态失败')
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
    ElMessage.success('界面配置已应用')
  } catch (e) {
    ElMessage.error(e?.message || '保存配置失败')
  } finally {
    saving.value = false
  }
}

const handleReset = () => {
  configStore.$reset()
  ElMessage.info('界面配置已重置')
}

onMounted(fetchHomeConfig)
onMounted(fetchOperationStatus)
</script>

<style scoped>
.page-container { padding: 0; }
.modern-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}
.card-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 32px;
  color: #1e293b;
  text-align: center;
}
.ops-header {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
</style>
