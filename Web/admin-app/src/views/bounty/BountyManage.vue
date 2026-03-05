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
            <el-tag :type="scope.row.status === '招募中' ? 'warning' : 'primary'" effect="light" round>
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="scope">
            <el-space>
              <el-button type="primary" link @click="openDetail(scope.row)">详情</el-button>
              <el-button v-if="canReview(scope.row)" type="success" link @click="review(scope.row, 1)">审核通过</el-button>
              <el-button v-if="canReview(scope.row)" type="danger" link @click="review(scope.row, 2)">驳回</el-button>
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
import { getBountyTaskDetail, getBountyTaskPage, reviewBountyTask } from '../../api/bounty'

const search = ref('')
const loading = ref(false)
const bountyData = ref([])
const detailVisible = ref(false)
const currentDetail = ref(null)
const REVIEW_PENDING_STATUS = 0

const statusMap = {
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
      status: statusMap[item.status] || `状态${item.status}`
    }))
  } catch (error) {
    ElMessage.error(error?.message || '加载悬赏列表失败')
  } finally {
    loading.value = false
  }
}

const canReview = (row) => Number(row?.rawStatus) === REVIEW_PENDING_STATUS

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

onMounted(() => {
  loadList()
})
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
</style>
