<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-select v-model="queryParams.status" placeholder="所有状态" clearable style="width: 150px" @change="handleSearch">
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
          </el-select>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-space>
        <el-space v-if="selectedRows.length > 0">
          <el-button type="success" @click="handleBatchReview('APPROVED')">批量通过 ({{ selectedRows.length }})</el-button>
          <el-button type="danger" @click="handleBatchReview('REJECTED')">批量拒绝 ({{ selectedRows.length }})</el-button>
        </el-space>
      </div>

      <div class="table-wrapper">
        <el-table :data="tableData" stripe border style="width: 100%" v-loading="loading" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" :selectable="row => row.status === 'pending'" />
          <el-table-column prop="id" label="申请ID" width="110" />
          <el-table-column prop="userName" label="登录账户" min-width="140" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
          <el-table-column prop="mobile" label="手机号" width="140" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="requestTime" label="申请时间" width="180" />
          <el-table-column prop="reviewRemark" label="审核备注" min-width="180" show-overflow-tooltip />
          <el-table-column prop="retryAfter" label="可重提时间" width="180" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="scope">
              <template v-if="scope.row.status === 'pending'">
                <el-button link type="success" @click="openReview(scope.row, 'APPROVED')">通过</el-button>
                <el-divider direction="vertical" />
                <el-button link type="danger" @click="openReview(scope.row, 'REJECTED')">拒绝</el-button>
              </template>
              <template v-else>
                <el-tag size="small" type="info">已处理</el-tag>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="reviewVisible" :title="reviewForm.status === 'APPROVED' ? '通过申请' : '拒绝申请'" width="500px">
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="登录账户">
          <el-input :model-value="currentRow?.userName" disabled />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input :model-value="currentRow?.email" disabled />
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="reviewForm.reviewRemark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入审核备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button :type="reviewForm.status === 'APPROVED' ? 'success' : 'danger'" @click="submitReview">
          确认
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchReviewVisible" :title="batchReviewStatus === 'APPROVED' ? '批量通过' : '批量拒绝'" width="520px">
      <el-form label-width="100px">
        <el-form-item label="已选数量">
          <el-tag>{{ selectedRows.length }} 条申请</el-tag>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="batchReviewRemark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入审核备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchReviewVisible = false">取消</el-button>
        <el-button :type="batchReviewStatus === 'APPROVED' ? 'success' : 'danger'" @click="submitBatchReview" :loading="batchLoading">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminRegisterRequestsApi, reviewAdminRegisterRequestApi, batchReviewAdminRegisterRequestApi } from '../../api/auth'

const loading = ref(false)
const total = ref(0)
const tableData = ref([])
const selectedRows = ref([])
const reviewVisible = ref(false)
const batchReviewVisible = ref(false)
const batchReviewStatus = ref('APPROVED')
const batchReviewRemark = ref('')
const batchLoading = ref(false)
const currentRow = ref(null)

const queryParams = reactive({
  status: '',
  page: 1,
  size: 10
})

const reviewForm = reactive({
  id: null,
  status: 'APPROVED',
  reviewRemark: ''
})

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      status: queryParams.status || undefined,
      pageNum: queryParams.page,
      pageSize: queryParams.size
    }
    const data = await getAdminRegisterRequestsApi(params)
    tableData.value = data?.records || []
    total.value = data?.total || 0
    queryParams.page = data?.pageNum || queryParams.page
    queryParams.size = data?.pageSize || queryParams.size
  } catch (error) {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchList()
}

const handleReset = () => {
  queryParams.status = ''
  queryParams.page = 1
  fetchList()
}

const handlePageChange = (page) => {
  queryParams.page = page
  fetchList()
}

const handleSizeChange = (size) => {
  queryParams.size = size
  queryParams.page = 1
  fetchList()
}

const openReview = (row, status) => {
  currentRow.value = row
  reviewForm.id = row.id
  reviewForm.status = status
  reviewForm.reviewRemark = ''
  reviewVisible.value = true
}

const submitReview = async () => {
  try {
    await reviewAdminRegisterRequestApi({
      id: reviewForm.id,
      status: reviewForm.status,
      reviewRemark: reviewForm.reviewRemark
    })
    ElMessage.success('审核完成')
    reviewVisible.value = false
    fetchList()
  } catch (error) {
    console.error('审核失败', error)
  }
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleBatchReview = (status) => {
  batchReviewStatus.value = status
  batchReviewRemark.value = ''
  batchReviewVisible.value = true
}

const submitBatchReview = async () => {
  batchLoading.value = true
  try {
    const result = await batchReviewAdminRegisterRequestApi({
      ids: selectedRows.value.map(row => row.id),
      reviewStatus: batchReviewStatus.value,
      reviewRemark: batchReviewRemark.value
    })
    ElMessage.success(`批量审核完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条`)
    batchReviewVisible.value = false
    selectedRows.value = []
    fetchList()
  } catch (error) {
    console.error('批量审核失败', error)
    ElMessage.error('批量审核失败')
  } finally {
    batchLoading.value = false
  }
}

const getStatusType = (status) => {
  const map = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝'
  }
  return map[status] || status
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
  display: flex;
  flex-direction: column;
}
.header-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}
.table-wrapper {
  overflow-x: auto;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  overflow-x: auto;
}
</style>
