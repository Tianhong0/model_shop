<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-select v-model="query.targetType" clearable placeholder="举报对象" style="width: 160px">
            <el-option label="商品" value="LISTING" />
            <el-option label="订单" value="ORDER" />
            <el-option label="消息" value="MESSAGE" />
          </el-select>
          <el-select v-model="query.status" clearable placeholder="处理状态" style="width: 160px">
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已处理" :value="2" />
            <el-option label="已驳回" :value="3" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
      </div>

      <el-table v-loading="loading" :data="records" stripe border highlight-current-row>
        <el-table-column prop="id" label="举报ID" width="140" />
        <el-table-column prop="reporterNickname" label="举报人" width="140" />
        <el-table-column prop="targetType" label="对象类型" width="120" />
        <el-table-column prop="targetId" label="对象ID" width="140" />
        <el-table-column prop="reasonType" label="原因类型" width="140" />
        <el-table-column prop="reasonText" label="举报说明" min-width="260" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope"><el-tag :type="statusType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openHandle(scope.row, true)">成立</el-button>
            <el-button link type="danger" @click="openHandle(scope.row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          :page-count="Math.max(1, Math.ceil((total || 0) / query.pageSize))"
          :hide-on-single-page="false"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="处理举报" width="520px">
      <el-form :model="handleForm" label-width="100px">
        <el-form-item label="举报ID"><span>{{ handleForm.reportId }}</span></el-form-item>
        <el-form-item label="处理结果"><span>{{ handleForm.approved ? '举报成立' : '驳回举报' }}</span></el-form-item>
        <el-form-item v-if="handleForm.approved" label="处理动作">
          <el-select v-model="handleForm.handleAction" style="width: 100%">
            <el-option label="仅记录" value="NONE" />
            <el-option label="下架商品" value="OFF_SHELF" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理备注"><el-input v-model="handleForm.remark" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminUsedReportPage, handleAdminUsedReport } from '../../api/used'

const loading = ref(false)
const total = ref(0)
const records = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  targetType: '',
  status: null
})

const handleForm = reactive({
  reportId: null,
  approved: true,
  handleAction: 'NONE',
  remark: ''
})

const statusText = (status) => ({ 0: '待处理', 1: '处理中', 2: '已处理', 3: '已驳回' }[Number(status)] || '未知')
const statusType = (status) => ({ 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }[Number(status)] || '')

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getAdminUsedReportPage({ ...query })
    records.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '获取举报失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  Object.assign(query, { pageNum: 1, pageSize: 10, targetType: '', status: null })
  fetchList()
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

const openHandle = (row, approved) => {
  handleForm.reportId = row.id
  handleForm.approved = approved
  handleForm.handleAction = approved ? 'NONE' : ''
  handleForm.remark = ''
  dialogVisible.value = true
}

const submitHandle = async () => {
  submitting.value = true
  try {
    await handleAdminUsedReport({ ...handleForm })
    ElMessage.success('举报处理成功')
    dialogVisible.value = false
    fetchList()
  } catch (error) {
    ElMessage.error(error.message || '处理失败')
  } finally {
    submitting.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.page-container { padding: 0; }
.table-card { background: #fff; padding: 24px; border-radius: 16px; border: 1px solid #e2e8f0; }
.header-actions { display: flex; justify-content: space-between; margin-bottom: 20px; }
.pagination-container { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>
