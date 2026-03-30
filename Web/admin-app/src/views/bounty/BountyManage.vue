<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="search" placeholder="搜索悬赏..." style="width: 240px" />
        </el-space>
      </div>

      <el-table :data="displayList" v-loading="loading" stripe border highlight-current-row style="width: 100%">
        <el-table-column prop="id" label="TID" width="100" />
        <el-table-column prop="title" label="任务标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="publisher" label="发布人" width="120" />
        <el-table-column prop="reward" label="赏金金额" width="120">
          <template #default="scope">
            <span style="color: #ef4444; font-weight: 600">{{ scope.row.reward }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止日期" width="150" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.rawStatus)" effect="light" round>
              {{ scope.row.status }}
            </el-tag>
            <el-tag v-if="scope.row.cancelRequested === 1" type="warning" effect="dark" round size="small" style="margin-left: 6px">
              取消待审
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="scope">
            <el-space>
              <el-button type="primary" link @click="openDetail(scope.row)">详情</el-button>
              <el-button v-if="canReview(scope.row)" type="success" link @click="review(scope.row, 1)">审核通过</el-button>
              <el-button v-if="canReview(scope.row)" type="danger" link @click="review(scope.row, 2)">驳回</el-button>
              <el-button v-if="scope.row.cancelRequested === 1" type="warning" link @click="reviewCancel(scope.row)">审核取消</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="detailVisible" title="悬赏详情" width="760px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentDetail">
        <el-descriptions-item label="任务号">{{ currentDetail.taskSn || currentDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusMap[currentDetail.status] || currentDetail.status }}</el-descriptions-item>
        <el-descriptions-item label="发布人">{{ currentDetail.publisherId }}</el-descriptions-item>
        <el-descriptions-item label="预算">￥{{ currentDetail.finalAmount ?? currentDetail.budgetAmount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ currentDetail.title }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentDetail.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标签" :span="2">{{ currentDetail.tags || '-' }}</el-descriptions-item>
        <el-descriptions-item label="附件" :span="2">
          <el-space wrap v-if="(currentDetail.attachments || []).length">
            <el-link v-for="(item, idx) in currentDetail.attachments" :key="idx" :href="item" target="_blank" type="primary">
              附件{{ idx + 1 }}
            </el-link>
          </el-space>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="驳回原因" :span="2">{{ currentDetail.closeReason || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBountyTaskDetail, getBountyTaskPage, reviewBountyTask, reviewBountyCancelTask } from '../../api/bounty'

const search = ref('')
const loading = ref(false)
const bountyData = ref([])
const detailVisible = ref(false)
const currentDetail = ref(null)
const REVIEW_PENDING_STATUS = -1

const statusMap = {
  '-1': '待审核',
  0: '待支付托管',
  1: '招募中',
  2: '已选标',
  3: '交付中',
  4: '待验收',
  5: '已完成',
  6: '已关闭',
  7: '争议中'
}

const displayList = computed(() => {
  const keyword = String(search.value || '').trim().toLowerCase()
  if (!keyword) {
    return bountyData.value
  }
  return bountyData.value.filter(item => String(item.title || '').toLowerCase().includes(keyword))
})

const loadList = async () => {
  loading.value = true
  try {
    const data = await getBountyTaskPage({ pageNum: 1, pageSize: 50 })
    const records = data?.records || []
    bountyData.value = records.map(item => ({
      id: item.taskSn || item.id,
      taskId: item.id,
      rawStatus: item.status,
      title: item.title,
      publisher: item.publisherId,
      reward: `￥${item.finalAmount ?? item.budgetAmount ?? 0}`,
      deadline: item.deadlineTime || '-',
      status: statusMap[item.status] || `状态${item.status}`,
      cancelRequested: item.cancelRequested || 0
    }))
  } catch (error) {
    ElMessage.error(error?.message || '加载悬赏列表失败')
  } finally {
    loading.value = false
  }
}

const canReview = (row) => Number(row?.rawStatus) === REVIEW_PENDING_STATUS

const getStatusType = (status) => {
  const statusNum = Number(status)
  if (statusNum === -1) return 'warning'  // 待审核
  if (statusNum === 0) return 'info'      // 待支付托管
  if (statusNum === 1) return 'success'   // 招募中
  if (statusNum === 5) return 'success'   // 已完成
  if (statusNum === 6) return 'danger'    // 已关闭
  return 'primary'
}

const openDetail = async (row) => {
  if (!row?.taskId) {
    ElMessage.error('任务ID无效')
    return
  }
  try {
    const detail = await getBountyTaskDetail(row.taskId)
    currentDetail.value = detail
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '加载详情失败')
  }
}

const review = async (row, decision) => {
  if (!row?.taskId) {
    ElMessage.error('任务ID无效')
    return
  }
  const isReject = decision === 2
  const title = isReject ? '驳回任务' : '审核通过'
  const prompt = isReject ? '请输入驳回原因' : '请输入审核备注（可选）'

  try {
    const { value } = await ElMessageBox.prompt(prompt, title, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: isReject ? '请填写驳回原因' : '可不填',
      inputValidator: (val) => {
        if (isReject && !String(val || '').trim()) {
          return '驳回原因不能为空'
        }
        return true
      }
    })

    await reviewBountyTask({
      taskId: row.taskId,
      decision,
      remark: String(value || '').trim() || undefined
    })
    ElMessage.success('审核操作成功')
    await loadList()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error?.message || '审核操作失败')
  }
}

const reviewCancel = async (row) => {
  if (!row?.taskId) {
    ElMessage.error('任务ID无效')
    return
  }
  try {
    const action = await ElMessageBox.confirm(
      '该发布者申请取消悬赏，同意后托管金将退回发布者钱包。',
      '审核取消申请',
      {
        confirmButtonText: '同意取消（退款）',
        cancelButtonText: '拒绝取消',
        distinguishCancelAndClose: true,
        type: 'warning'
      }
    )
    // 用户点了"同意取消"
    const { value: remark } = await ElMessageBox.prompt('请输入审核备注（可选）', '同意取消', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '可不填'
    })
    await reviewBountyCancelTask({ taskId: row.taskId, decision: 1, remark: String(remark || '').trim() || undefined })
    ElMessage.success('已同意取消，托管金已退回')
    await loadList()
  } catch (error) {
    if (error === 'cancel') {
      // 用户点了"拒绝取消"
      try {
        const { value: remark } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝取消申请', {
          confirmButtonText: '确认拒绝',
          cancelButtonText: '返回',
          inputType: 'textarea',
          inputPlaceholder: '请填写拒绝原因',
          inputValidator: (val) => String(val || '').trim() ? true : '拒绝原因不能为空'
        })
        await reviewBountyCancelTask({ taskId: row.taskId, decision: 2, remark: String(remark || '').trim() })
        ElMessage.success('已拒绝取消申请')
        await loadList()
      } catch (innerErr) {
        if (innerErr === 'cancel' || innerErr === 'close') return
        ElMessage.error(innerErr?.message || '操作失败')
      }
      return
    }
    if (error === 'close') return
    ElMessage.error(error?.message || '操作失败')
  }
}

onMounted(() => {
  loadList()
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
</style>
