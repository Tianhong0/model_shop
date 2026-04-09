<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input-number v-model="query.userId" :min="1" controls-position="right" placeholder="用户ID" style="width: 160px" />
          <el-select v-model="query.status" placeholder="提现状态" clearable style="width: 180px">
            <el-option label="待审核" :value="0" />
            <el-option label="待打款" :value="1" />
            <el-option label="已拒绝" :value="2" />
            <el-option label="已打款" :value="3" />
            <el-option label="打款失败" :value="4" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
      </div>

     

      <el-table v-loading="loading" :data="tableData" stripe border highlight-current-row style="width: 100%">
        <el-table-column prop="id" label="提现ID" width="180" />
        <el-table-column prop="withdrawSn" label="提现单号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="180" />
        <el-table-column prop="amount" label="金额(元)" width="120">
          <template #default="scope">￥{{ Number(scope.row.amount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyRemark" label="申请备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="alipayAccount" label="支付宝账号" min-width="220" show-overflow-tooltip />
        <el-table-column prop="alipayRealName" label="收款人姓名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="auditRemark" label="审核备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="payRemark" label="打款备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="申请时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button
              link
              type="primary"
              @click="openAuditDialog(scope.row, 1)"
              v-if="Number(scope.row.status) === 0"
            >通过</el-button>
            <el-button
              link
              type="danger"
              @click="openAuditDialog(scope.row, 2)"
              v-if="Number(scope.row.status) === 0"
            >拒绝</el-button>
            <el-button
              link
              type="primary"
              @click="openPayDialog(scope.row, 1)"
              v-if="Number(scope.row.status) === 1"
            >打款成功</el-button>
            <el-button
              link
              type="warning"
              @click="openPayDialog(scope.row, 2)"
              v-if="Number(scope.row.status) === 1"
            >打款失败</el-button>
            <span v-if="Number(scope.row.status) !== 0 && Number(scope.row.status) !== 1">-</span>
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

    <el-dialog v-model="auditVisible" :title="auditForm.decision === 1 ? '提现审核通过' : '提现审核拒绝'" width="520px">
      <el-form :model="auditForm" label-width="90px">
        <el-form-item label="提现ID">{{ auditForm.withdrawId }}</el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.remark" type="textarea" :rows="4" placeholder="请输入备注" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAudit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="payVisible" :title="payForm.payResult === 1 ? '提现打款成功' : '提现打款失败'" width="520px">
      <el-form :model="payForm" label-width="90px">
        <el-form-item label="提现ID">{{ payForm.withdrawId }}</el-form-item>
        <el-form-item label="打款备注">
          <el-input v-model="payForm.remark" type="textarea" :rows="4" placeholder="请输入备注" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPay">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminWithdrawPage, auditWithdraw, payWithdraw } from '../../api/wallet'

const loading = ref(false)
const submitting = ref(false)
const total = ref(0)
const tableData = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  userId: null,
  status: null
})

const auditVisible = ref(false)
const payVisible = ref(false)

const auditForm = reactive({
  withdrawId: null,
  decision: 1,
  remark: ''
})

const payForm = reactive({
  withdrawId: null,
  payResult: 1,
  remark: ''
})

const statusLabel = (status) => {
  const map = {
    0: '待审核',
    1: '待打款',
    2: '已拒绝',
    3: '已打款',
    4: '打款失败'
  }
  return map[status] || `未知(${status})`
}

const statusType = (status) => {
  if (status === 0) return 'warning'
  if (status === 1) return 'info'
  if (status === 2) return 'danger'
  if (status === 3) return 'success'
  if (status === 4) return 'warning'
  return ''
}

const fetchList = async () => {
  loading.value = true
  try {
    const response = await getAdminWithdrawPage({ ...query })
    tableData.value = (response?.records || []).map((item) => ({
      ...item,
      userId: item?.userId || '-'
    }))
    total.value = Number(response?.total || 0)
    query.pageNum = Number(response?.pageNum || query.pageNum)
    query.pageSize = Number(response?.pageSize || query.pageSize)
  } catch (error) {
    console.error('获取提现列表失败:', error)
    ElMessage.error('获取提现列表失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    status: null
  })
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

const openAuditDialog = (row, decision) => {
  Object.assign(auditForm, {
    withdrawId: row.id,
    decision,
    remark: ''
  })
  auditVisible.value = true
}

const openPayDialog = (row, payResult) => {
  Object.assign(payForm, {
    withdrawId: row.id,
    payResult,
    remark: ''
  })
  payVisible.value = true
}

const submitAudit = async () => {
  if (!auditForm.withdrawId) return
  submitting.value = true
  try {
    await auditWithdraw({
      withdrawId: auditForm.withdrawId,
      decision: auditForm.decision,
      remark: auditForm.remark || ''
    })
    ElMessage.success('审核操作成功')
    auditVisible.value = false
    await fetchList()
  } catch (error) {
    console.error('审核操作失败:', error)
    ElMessage.error('审核操作失败')
  } finally {
    submitting.value = false
  }
}

const submitPay = async () => {
  if (!payForm.withdrawId) return
  submitting.value = true
  try {
    await payWithdraw({
      withdrawId: payForm.withdrawId,
      payResult: payForm.payResult,
      remark: payForm.remark || ''
    })
    ElMessage.success('打款操作成功')
    payVisible.value = false
    await fetchList()
  } catch (error) {
    console.error('打款操作失败:', error)
    ElMessage.error('打款操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchList()
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

.pagination-container {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
  display: flex;
  justify-content: flex-end;
}
</style>
