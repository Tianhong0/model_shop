<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.title" clearable placeholder="标题关键词" style="width: 220px" />
          <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
        <el-button type="primary" @click="openCreate">新增轮播</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column prop="title" label="标题" min-width="140" />
        <el-table-column prop="subtitle" label="副标题" min-width="140" show-overflow-tooltip />
        <el-table-column label="图片" width="100">
          <template #default="scope">
            <el-image :src="scope.row.imageUrl" style="width: 60px; height: 36px; border-radius: 6px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="linkType" label="跳转类型" width="100">
          <template #default="scope">
            {{ ['无跳转', '模型详情', '活动页', '外部链接'][scope.row.linkType] || '无跳转' }}
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.status === 1"
              @change="(val) => onStatusChange(scope.row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          :page-count="Math.max(1, Math.ceil((total || 0) / query.pageSize))"
          :hide-on-single-page="false"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑轮播' : '新增轮播'" width="680px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="100" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" maxlength="200" placeholder="显示在轮播图标题下方" />
        </el-form-item>
        <el-form-item label="图片">
          <div style="display: flex; gap: 10px; width: 100%; align-items: center;">
            <el-input v-model="form.imageUrl" placeholder="请上传或粘贴图片URL" />
            <el-button :loading="uploading" @click.prevent="triggerUpload">上传</el-button>
            <input ref="fileRef" type="file" accept="image/*" style="display:none" @change="uploadImage" />
          </div>
        </el-form-item>
        <el-form-item label="跳转类型">
          <el-select v-model="form.linkType" style="width: 100%" @change="onLinkTypeChange">
            <el-option label="无跳转" :value="0" />
            <el-option label="模型详情" :value="1" />
            <el-option label="活动页" :value="2" />
            <el-option label="外部链接" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转值">
          <!-- 无跳转 -->
          <el-input v-if="form.linkType === 0" disabled placeholder="无需填写跳转值" />
          <!-- 模型详情：选择器 -->
          <div v-else-if="form.linkType === 1" style="display: flex; gap: 10px; width: 100%;">
            <el-input :model-value="selectedModelName" disabled placeholder="点击选择模型" />
            <el-button @click="openModelSelector">选择模型</el-button>
          </div>
          <!-- 活动页：选择器 -->
          <div v-else-if="form.linkType === 2" style="display: flex; gap: 10px; width: 100%;">
            <el-input :model-value="selectedEventName" disabled placeholder="点击选择活动" />
            <el-button @click="openEventSelector">选择活动</el-button>
          </div>
          <!-- 外部链接：手动输入 -->
          <el-input v-else-if="form.linkType === 3" v-model="form.linkValue" placeholder="请输入完整URL，如 https://example.com" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 模型选择器对话框 -->
    <el-dialog v-model="modelSelectorVisible" title="选择模型" width="800px">
      <div style="margin-bottom: 16px;">
        <el-input v-model="modelQuery.keyword" clearable placeholder="搜索模型名称" style="width: 300px;" @keyup.enter="searchModels" />
        <el-button type="primary" style="margin-left: 10px;" @click="searchModels">搜索</el-button>
      </div>
      <el-table v-loading="modelLoading" :data="modelRecords" border stripe max-height="400" highlight-current-row @current-change="onModelSelect">
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column label="图片" width="100">
          <template #default="scope">
            <el-image :src="scope.row.mainImageUrl || scope.row.thumbnailUrl" style="width: 60px; height: 60px; border-radius: 6px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="modelName" label="模型名称" min-width="180" />
        <el-table-column prop="basePrice" label="基础价格" width="120">
          <template #default="scope">¥{{ scope.row.basePrice || 0 }}</template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="modelQuery.pageNum"
          v-model:page-size="modelQuery.pageSize"
          :total="modelTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, prev, pager, next"
          small
          @current-change="fetchModelList"
        />
      </div>
      <template #footer>
        <el-button @click="modelSelectorVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!tempSelectedModel" @click="confirmModelSelect">确认选择</el-button>
      </template>
    </el-dialog>

    <!-- 活动选择器对话框 -->
    <el-dialog v-model="eventSelectorVisible" title="选择活动" width="800px">
      <div style="margin-bottom: 16px;">
        <el-input v-model="eventQuery.title" clearable placeholder="搜索活动名称" style="width: 300px;" @keyup.enter="searchEvents" />
        <el-button type="primary" style="margin-left: 10px;" @click="searchEvents">搜索</el-button>
      </div>
      <el-table v-loading="eventLoading" :data="eventRecords" border stripe max-height="400" highlight-current-row @current-change="onEventSelect">
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column label="封面" width="100">
          <template #default="scope">
            <el-image :src="scope.row.coverImage" style="width: 60px; height: 60px; border-radius: 6px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="活动名称" min-width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '进行中' : '已结束' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="eventQuery.pageNum"
          v-model:page-size="eventQuery.pageSize"
          :total="eventTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, prev, pager, next"
          small
          @current-change="fetchEventList"
        />
      </div>
      <template #footer>
        <el-button @click="eventSelectorVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!tempSelectedEvent" @click="confirmEventSelect">确认选择</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadFileWithProgress, getModelList } from '../../api/model'
import { getEventAdminList } from '../../api/event'
import {
  createBanner,
  deleteBanner,
  getBannerAdminList,
  updateBanner,
  updateBannerStatus
} from '../../api/operation'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const total = ref(0)
const records = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  status: null
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const fileRef = ref(null)
const form = reactive({
  id: null,
  title: '',
  subtitle: '',
  imageUrl: '',
  linkType: 0,
  linkValue: '',
  sortNo: 0,
  status: 1
})

// 模型选择器相关
const modelSelectorVisible = ref(false)
const modelLoading = ref(false)
const modelRecords = ref([])
const modelTotal = ref(0)
const tempSelectedModel = ref(null)
const selectedModelName = ref('')
const modelQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: 1
})

// 活动选择器相关
const eventSelectorVisible = ref(false)
const eventLoading = ref(false)
const eventRecords = ref([])
const eventTotal = ref(0)
const tempSelectedEvent = ref(null)
const selectedEventName = ref('')
const eventQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  status: null
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getBannerAdminList({ ...query })
    records.value = res.records || []
    total.value = Number(res.total || 0)
    query.pageNum = Number(res.pageNum || query.pageNum)
    query.pageSize = Number(res.pageSize || query.pageSize)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (pageNum) => {
  query.pageNum = pageNum
  fetchList()
}

const handleSizeChange = (pageSize) => {
  query.pageSize = pageSize
  query.pageNum = 1
  fetchList()
}

const resetQuery = () => {
  query.pageNum = 1
  query.pageSize = 10
  query.title = ''
  query.status = null
  fetchList()
}

const resetForm = () => {
  form.id = null
  form.title = ''
  form.subtitle = ''
  form.imageUrl = ''
  form.linkType = 0
  form.linkValue = ''
  form.sortNo = 0
  form.status = 1
  selectedModelName.value = ''
  selectedEventName.value = ''
}

const openCreate = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEdit = async (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    title: row.title,
    subtitle: row.subtitle ?? '',
    imageUrl: row.imageUrl,
    linkType: row.linkType ?? 0,
    linkValue: row.linkValue ?? '',
    sortNo: row.sortNo ?? 0,
    status: row.status ?? 1
  })
  // 解析已有的 linkValue，显示对应的名称
  selectedModelName.value = ''
  selectedEventName.value = ''
  if (row.linkType === 1 && row.linkValue) {
    // 模型详情，从 linkValue 中提取模型ID并获取名称
    const match = row.linkValue.match(/id=(\d+)/)
    if (match) {
      try {
        const res = await getModelList({ pageNum: 1, pageSize: 1, id: match[1] })
        if (res.records?.[0]) {
          selectedModelName.value = res.records[0].modelName
        }
      } catch (e) {
        // ignore
      }
    }
  } else if (row.linkType === 2 && row.linkValue) {
    // 活动页，从 linkValue 中提取活动ID并获取名称
    const match = row.linkValue.match(/id=(\d+)/)
    if (match) {
      try {
        const res = await getEventAdminList({ pageNum: 1, pageSize: 1, id: match[1] })
        if (res.records?.[0]) {
          selectedEventName.value = res.records[0].title
        }
      } catch (e) {
        // ignore
      }
    }
  }
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.title || !form.imageUrl) {
    ElMessage.warning('请填写标题并上传图片')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title,
      subtitle: form.subtitle,
      imageUrl: form.imageUrl,
      linkType: form.linkType,
      linkValue: form.linkValue,
      sortNo: form.sortNo,
      status: form.status
    }
    if (isEdit.value) {
      await updateBanner({ id: form.id, ...payload })
      ElMessage.success('更新成功')
    } else {
      await createBanner(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

const onStatusChange = async (row, value) => {
  await updateBannerStatus({ id: row.id, status: value ? 1 : 0 })
  ElMessage.success('状态已更新')
  fetchList()
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除轮播【${row.title}】吗？`, '提示', { type: 'warning' })
  await deleteBanner(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

const triggerUpload = () => {
  fileRef.value?.click()
}

const resolveUploadUrl = (response) => {
  if (!response) return ''
  if (typeof response === 'string') return response
  if (typeof response === 'object') {
    if (typeof response.data === 'string') return response.data
    if (typeof response.url === 'string') return response.url
    if (typeof response.message === 'string') return response.message
  }
  return ''
}

const uploadImage = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const response = await uploadFileWithProgress(file, 'banner', () => {})
    const uploadUrl = resolveUploadUrl(response)
    if (!uploadUrl) {
      throw new Error('未获取到图片地址')
    }
    form.imageUrl = uploadUrl
    ElMessage.success('图片上传成功')
  } catch (error) {
    console.error('轮播图上传失败:', error)
    ElMessage.error('上传失败，请重试')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

// ==================== 跳转类型变更处理 ====================

const onLinkTypeChange = () => {
  form.linkValue = ''
  selectedModelName.value = ''
  selectedEventName.value = ''
}

// ==================== 模型选择器 ====================

const openModelSelector = () => {
  modelSelectorVisible.value = true
  tempSelectedModel.value = null
  fetchModelList()
}

const fetchModelList = async () => {
  modelLoading.value = true
  try {
    const res = await getModelList({
      pageNum: modelQuery.pageNum,
      pageSize: modelQuery.pageSize,
      modelName: modelQuery.keyword || undefined,
      status: modelQuery.status
    })
    modelRecords.value = res.records || []
    modelTotal.value = res.total || 0
  } catch (error) {
    console.error('获取模型列表失败:', error)
  } finally {
    modelLoading.value = false
  }
}

const searchModels = () => {
  modelQuery.pageNum = 1
  fetchModelList()
}

const onModelSelect = (row) => {
  tempSelectedModel.value = row
}

const confirmModelSelect = () => {
  if (!tempSelectedModel.value) return
  form.linkValue = `/pages/custom/detail?id=${tempSelectedModel.value.id}`
  selectedModelName.value = tempSelectedModel.value.modelName
  modelSelectorVisible.value = false
}

// ==================== 活动选择器 ====================

const openEventSelector = () => {
  eventSelectorVisible.value = true
  tempSelectedEvent.value = null
  fetchEventList()
}

const fetchEventList = async () => {
  eventLoading.value = true
  try {
    const res = await getEventAdminList({
      pageNum: eventQuery.pageNum,
      pageSize: eventQuery.pageSize,
      title: eventQuery.title || undefined,
      status: eventQuery.status
    })
    eventRecords.value = res.records || []
    eventTotal.value = res.total || 0
  } catch (error) {
    console.error('获取活动列表失败:', error)
  } finally {
    eventLoading.value = false
  }
}

const searchEvents = () => {
  eventQuery.pageNum = 1
  fetchEventList()
}

const onEventSelect = (row) => {
  tempSelectedEvent.value = row
}

const confirmEventSelect = () => {
  if (!tempSelectedEvent.value) return
  form.linkValue = `/pages/event/event-detail?id=${tempSelectedEvent.value.id}`
  selectedEventName.value = tempSelectedEvent.value.title
  eventSelectorVisible.value = false
}

onMounted(fetchList)
</script>

<style scoped>
.page-container { padding: 0; }
.table-card {
  background: #fff;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}
.header-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  overflow-x: auto;
}
</style>
