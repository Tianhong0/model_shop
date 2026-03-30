<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-select v-model="queryParams.status" placeholder="所有状态" clearable style="width: 140px" @change="handleSearch">
            <el-option label="待审批" value="pending" />
            <el-option label="已批准" value="approved" />
            <el-option label="已拒绝" value="rejected" />
          </el-select>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-space>
      </div>

      <div class="table-wrapper">
        <el-table :data="requestData" stripe border highlight-current-row style="width: 100%" v-loading="loading">
          <el-table-column prop="id" label="申请ID" width="100" />
          <el-table-column prop="userId" label="用户ID" width="100" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="phone" label="电话" />
          <el-table-column prop="reason" label="注销原因" min-width="200" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)" effect="light" round>
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="requestTime" label="申请时间" width="180" />
          <el-table-column prop="adminNote" label="管理员备注" width="150" show-overflow-tooltip />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <template v-if="scope.row.status === 'pending'">
                <el-button link type="success" @click="handleApprove(scope.row)">批准</el-button>
                <el-divider direction="vertical" />
                <el-button link type="danger" @click="handleReject(scope.row)">拒绝</el-button>
              </template>
              <template v-else>
                <el-tag :type="scope.row.status === 'approved' ? 'success' : 'info'" size="small">
                  {{ scope.row.status === 'approved' ? '已处理' : '已拒绝' }}
                </el-tag>
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
          :page-count="Math.max(1, Math.ceil((total || 0) / queryParams.size))"
          :page-sizes="[10, 20, 50, 100]"
          :hide-on-single-page="false"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 审批弹窗 -->
    <el-dialog v-model="approveVisible" title="审批注销申请" width="500px">
      <el-form :model="approveForm" label-width="100px">
        <el-form-item label="申请用户">
          <el-input :value="currentRequest?.username" disabled />
        </el-form-item>
        <el-form-item label="注销原因">
          <el-input :value="currentRequest?.reason" type="textarea" :rows="3" disabled />
        </el-form-item>
        <el-form-item label="审批结果">
          <el-tag :type="approveForm.status === 'APPROVED' ? 'success' : 'danger'" effect="light">
            {{ approveForm.status === 'APPROVED' ? '批准注销' : '拒绝申请' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="管理员备注">
          <el-input
            v-model="approveForm.adminNote"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息（可选）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-alert
          v-if="approveForm.status === 'APPROVED'"
          title="警告：批准后将永久删除该用户账户，此操作不可撤销！"
          type="warning"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button :type="approveForm.status === 'APPROVED' ? 'danger' : 'primary'" @click="submitApprove">
          确认{{ approveForm.status === 'APPROVED' ? '批准' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeletionRequests, approveDeletionRequest } from '../../api/user'

const loading = ref(false)
const requestData = ref([])
const total = ref(0)
const approveVisible = ref(false)
const currentRequest = ref(null)

const queryParams = reactive({
  status: '',
  page: 1,
  size: 10
})

const approveForm = reactive({
  id: null,
  status: 'APPROVED',
  adminNote: ''
})

// 获取注销申请列表
const fetchRequests = async () => {
  loading.value = true
  try {
    const params = {
      status: queryParams.status,
      pageNum: queryParams.page,
      pageSize: queryParams.size
    }
    // 移除空值
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) {
        delete params[key]
      }
    })
    const response = await getDeletionRequests(params)
    requestData.value = response?.records || []
    total.value = response?.total || 0
    queryParams.page = response?.pageNum || queryParams.page
    queryParams.size = response?.pageSize || queryParams.size
  } catch (error) {
    console.error('获取注销申请列表失败:', error)
    requestData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  queryParams.page = 1
  fetchRequests()
}

// 重置
const handleReset = () => {
  queryParams.status = ''
  queryParams.page = 1
  fetchRequests()
}

// 页码变化
const handlePageChange = (page) => {
  queryParams.page = page
  fetchRequests()
}

// 每页大小变化
const handleSizeChange = (size) => {
  queryParams.size = size
  queryParams.page = 1
  fetchRequests()
}

// 获取状态类型
const getStatusType = (status) => {
  const map = {
    pending: 'warning',
    approved: 'success',
    rejected: 'info'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    pending: '待审批',
    approved: '已批准',
    rejected: '已拒绝'
  }
  return map[status] || status
}

// 批准
const handleApprove = (row) => {
  currentRequest.value = row
  approveForm.id = row.id
  approveForm.status = 'APPROVED'
  approveForm.adminNote = ''
  approveVisible.value = true
}

// 拒绝
const handleReject = (row) => {
  currentRequest.value = row
  approveForm.id = row.id
  approveForm.status = 'REJECTED'
  approveForm.adminNote = ''
  approveVisible.value = true
}

// 提交审批
const submitApprove = async () => {
  try {
    await approveDeletionRequest({
      id: approveForm.id,
      status: approveForm.status,
      adminNote: approveForm.adminNote
    })
    ElMessage.success(approveForm.status === 'APPROVED' ? '已批准注销申请' : '已拒绝该申请')
    approveVisible.value = false
    fetchRequests()
  } catch (error) {
    console.error('审批失败:', error)
  }
}

onMounted(() => {
  fetchRequests()
})
</script>

<style scoped>
.page-container {
  padding: 0;
}

.table-card {
  background: var(--bg-primary);
  padding: 28px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
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
  overflow-x: auto;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}
</style>
