<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">清单总数</div>
          <div class="stat-value">{{ Number(statistics.totalCount || 0) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">已发布</div>
          <div class="stat-value" style="color: #67c23a">{{ Number(statistics.publishedCount || 0) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">草稿</div>
          <div class="stat-value" style="color: #909399">{{ Number(statistics.draftCount || 0) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">已下架</div>
          <div class="stat-value" style="color: #f56c6c">{{ Number(statistics.offlineCount || 0) }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="queryParams.keyword" placeholder="搜索标题..." style="width: 200px" clearable @clear="handleSearch">
            <template #prefix><el-icon><search /></el-icon></template>
          </el-input>
          <el-select v-model="queryParams.status" placeholder="所有状态" clearable style="width: 120px" @change="handleSearch">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下架" :value="2" />
          </el-select>
          <el-select v-model="queryParams.orderBy" placeholder="排序方式" style="width: 140px" @change="handleSearch">
            <el-option label="最新创建" value="latest" />
            <el-option label="浏览量" value="viewCount" />
            <el-option label="点赞数" value="likeCount" />
          </el-select>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="loadStatistics">刷新统计</el-button>
        </el-space>
        <el-space>
          <el-button type="warning" :disabled="selectedRows.length === 0" @click="handleBatchPublish">批量发布</el-button>
          <el-button type="info" :disabled="selectedRows.length === 0" @click="handleBatchOffline">批量下架</el-button>
          <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">批量删除</el-button>
        </el-space>
      </div>

      <div class="table-wrapper">
        <el-table :data="tableData" stripe border highlight-current-row v-loading="loading"
          @selection-change="handleSelectionChange" style="width: 100%">
          <el-table-column type="selection" width="50" />
          <el-table-column label="封面" width="80">
            <template #default="scope">
              <el-image
                v-if="scope.row.coverImage"
                :src="scope.row.coverImage"
                style="width:50px; height:50px; border-radius:6px"
                :preview-src-list="[scope.row.coverImage]"
                preview-teleported
              />
              <div v-else class="no-cover">无</div>
            </template>
          </el-table-column>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column label="创建者" width="120">
            <template #default="scope">
              <div class="user-cell">
                <el-avatar v-if="scope.row.userAvatar" :size="24" :src="scope.row.userAvatar" />
                <span class="nickname">{{ scope.row.userNickname || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="modelCount" label="模型数" width="80" align="center" />
          <el-table-column prop="viewCount" label="浏览" width="70" align="center" />
          <el-table-column prop="likeCount" label="点赞" width="70" align="center" />
          <el-table-column prop="collectCount" label="收藏" width="70" align="center" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)" effect="light" round size="small">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170">
            <template #default="scope">
              {{ formatDateTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleViewDetail(scope.row)">详情</el-button>
              <template v-if="scope.row.status === 0">
                <el-divider direction="vertical" />
                <el-button link type="success" @click="handlePublish(scope.row)">发布</el-button>
              </template>
              <template v-if="scope.row.status === 1">
                <el-divider direction="vertical" />
                <el-button link type="warning" @click="handleOffline(scope.row)">下架</el-button>
              </template>
              <el-divider direction="vertical" />
              <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-count="Math.max(1, Math.ceil((total || 0) / queryParams.pageSize))"
          :page-sizes="[10, 20, 50, 100]"
          :hide-on-single-page="false"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="清单详情" width="600px">
      <div v-if="currentDetail" class="detail-content">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-image
              v-if="currentDetail.coverImage"
              :src="currentDetail.coverImage"
              style="width:100%; height:150px; border-radius:8px"
            />
            <div v-else class="detail-cover-empty">无封面</div>
          </el-col>
          <el-col :span="16">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="ID">{{ currentDetail.id }}</el-descriptions-item>
              <el-descriptions-item label="标题">{{ currentDetail.title }}</el-descriptions-item>
              <el-descriptions-item label="描述">{{ currentDetail.description || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建者">{{ currentDetail.userNickname || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(currentDetail.status)" size="small">
                  {{ getStatusText(currentDetail.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDateTime(currentDetail.createTime) }}</el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>
        <el-divider />
        <el-row :gutter="20" class="stat-row-detail">
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-num">{{ currentDetail.modelCount || 0 }}</div>
              <div class="stat-label">模型数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-num">{{ currentDetail.viewCount || 0 }}</div>
              <div class="stat-label">浏览量</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-num">{{ currentDetail.likeCount || 0 }}</div>
              <div class="stat-label">点赞数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-num">{{ currentDetail.collectCount || 0 }}</div>
              <div class="stat-label">收藏数</div>
            </div>
          </el-col>
        </el-row>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModelListAdminPage, getModelListAdminDetail, updateModelListStatus, deleteModelList, batchUpdateModelListStatus, batchDeleteModelList, getModelListStatistics } from '../../api/modelList'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selectedRows = ref([])
const detailVisible = ref(false)
const currentDetail = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: null,
  orderBy: 'latest'
})

const statistics = ref({
  totalCount: 0,
  publishedCount: 0,
  draftCount: 0,
  offlineCount: 0
})

const statusOptions = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已发布' },
  { value: 2, label: '已下架' }
]

const getStatusType = (status) => {
  const map = { 0: 'info', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 0: '草稿', 1: '已发布', 2: '已下架' }
  return map[status] || '-'
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await getModelListAdminPage(queryParams)
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(error?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const data = await getModelListStatistics()
    statistics.value = data || {}
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  loadData()
}

const handlePageChange = (page) => {
  queryParams.pageNum = page
  loadData()
}

const handleSizeChange = (size) => {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  loadData()
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleViewDetail = async (row) => {
  try {
    const data = await getModelListAdminDetail(row.id)
    currentDetail.value = data
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '加载详情失败')
  }
}

const handlePublish = async (row) => {
  try {
    await updateModelListStatus({ listId: row.id, status: 1 })
    ElMessage.success('发布成功')
    loadData()
    loadStatistics()
  } catch (error) {
    ElMessage.error(error?.message || '发布失败')
  }
}

const handleOffline = async (row) => {
  try {
    await updateModelListStatus({ listId: row.id, status: 2 })
    ElMessage.success('下架成功')
    loadData()
    loadStatistics()
  } catch (error) {
    ElMessage.error(error?.message || '下架失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该清单吗？', '提示', { type: 'warning' })
    await deleteModelList(row.id)
    ElMessage.success('删除成功')
    loadData()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const handleBatchPublish = async () => {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要发布选中的 ${selectedRows.value.length} 个清单吗？`, '提示', { type: 'warning' })
    await batchUpdateModelListStatus({
      listIds: selectedRows.value.map(r => r.id),
      status: 1
    })
    ElMessage.success('批量发布成功')
    loadData()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '批量发布失败')
    }
  }
}

const handleBatchOffline = async () => {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要下架选中的 ${selectedRows.value.length} 个清单吗？`, '提示', { type: 'warning' })
    await batchUpdateModelListStatus({
      listIds: selectedRows.value.map(r => r.id),
      status: 2
    })
    ElMessage.success('批量下架成功')
    loadData()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '批量下架失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个清单吗？此操作不可恢复！`, '警告', { type: 'error' })
    await batchDeleteModelList({
      listIds: selectedRows.value.map(r => r.id)
    })
    ElMessage.success('批量删除成功')
    loadData()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '批量删除失败')
    }
  }
}

onMounted(() => {
  loadData()
  loadStatistics()
})
</script>

<style scoped>
.page-container {
  padding: 0;
}

.stat-row {
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 24px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  text-align: center;
  transition: all 0.3s ease;
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
}

.table-card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 28px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
  flex-wrap: wrap;
  gap: 16px;
}

.table-wrapper {
  margin-bottom: 16px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}

.no-cover {
  width: 50px;
  height: 50px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-muted);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nickname {
  font-size: 13px;
  color: var(--text-secondary);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-content {
  padding: 10px 0;
}

.detail-cover-empty {
  width: 100%;
  height: 150px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.stat-row-detail {
  margin-top: 24px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}
</style>
