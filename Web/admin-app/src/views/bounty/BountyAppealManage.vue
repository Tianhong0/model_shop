<template>
  <div class="appeal-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>评价申诉管理</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="申诉状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待处理" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAppeals">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="appealList" v-loading="loading" border>
        <el-table-column prop="id" label="申诉ID" width="180" />
        <el-table-column prop="taskTitle" label="任务标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="designerName" label="申诉设计者" width="120">
          <template #default="{ row }">
            设计者#{{ row.designerId }}
          </template>
        </el-table-column>
        <el-table-column prop="ratingScore" label="原评分" width="80">
          <template #default="{ row }">
            <el-rate v-model="row.ratingScore" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申诉理由" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申诉时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" type="success" link @click="showReviewDialog(row, 1)">通过</el-button>
            <el-button v-if="row.status === 0" type="danger" link @click="showReviewDialog(row, 2)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadAppeals"
        @current-change="loadAppeals"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="申诉详情" width="600px">
      <el-descriptions :column="1" border v-if="currentAppeal">
        <el-descriptions-item label="申诉ID">{{ currentAppeal.id }}</el-descriptions-item>
        <el-descriptions-item label="任务ID">{{ currentAppeal.taskId }}</el-descriptions-item>
        <el-descriptions-item label="任务标题">{{ currentAppeal.taskTitle }}</el-descriptions-item>
        <el-descriptions-item label="设计者ID">{{ currentAppeal.designerId }}</el-descriptions-item>
        <el-descriptions-item label="原评分">
          <el-rate v-model="currentAppeal.ratingScore" disabled />
        </el-descriptions-item>
        <el-descriptions-item label="原评价内容">{{ currentAppeal.ratingComment || '无' }}</el-descriptions-item>
        <el-descriptions-item label="申诉理由">{{ currentAppeal.reason }}</el-descriptions-item>
        <el-descriptions-item label="证据材料">
          <div v-if="currentAppeal.evidence && currentAppeal.evidence.length">
            <el-image
              v-for="(url, idx) in currentAppeal.evidence"
              :key="idx"
              :src="url"
              :preview-src-list="currentAppeal.evidence"
              style="width: 60px; height: 60px; margin-right: 8px"
              fit="cover"
            />
          </div>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[currentAppeal.status]?.type">{{ statusMap[currentAppeal.status]?.text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处理备注" v-if="currentAppeal.status !== 0">{{ currentAppeal.adminRemark || '无' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间" v-if="currentAppeal.status !== 0">{{ currentAppeal.processedTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog v-model="reviewDialogVisible" :title="reviewDecision === 1 ? '通过申诉' : '驳回申诉'" width="400px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审核备注">
          <el-input v-model="reviewForm.adminRemark" type="textarea" :rows="3" placeholder="请输入审核备注（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button :type="reviewDecision === 1 ? 'success' : 'danger'" @click="submitReview" :loading="submitLoading">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBountyAppealList, reviewBountyAppeal } from '@/api/bounty'

const loading = ref(false)
const appealList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  status: null
})

const statusMap = {
  0: { text: '待处理', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' }
}

const detailDialogVisible = ref(false)
const currentAppeal = ref(null)

const reviewDialogVisible = ref(false)
const reviewDecision = ref(1)
const reviewForm = reactive({
  appealId: null,
  decision: null,
  adminRemark: ''
})
const submitLoading = ref(false)

const loadAppeals = async () => {
  loading.value = true
  try {
    const res = await getBountyAppealList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: searchForm.status
    })
    appealList.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.status = null
  pageNum.value = 1
  loadAppeals()
}

const showDetail = (row) => {
  currentAppeal.value = row
  detailDialogVisible.value = true
}

const showReviewDialog = (row, decision) => {
  reviewForm.appealId = row.id
  reviewForm.decision = decision
  reviewForm.adminRemark = ''
  reviewDecision.value = decision
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  submitLoading.value = true
  try {
    await reviewBountyAppeal({
      appealId: reviewForm.appealId,
      decision: reviewForm.decision,
      adminRemark: reviewForm.adminRemark
    })
    ElMessage.success(reviewForm.decision === 1 ? '申诉已通过，评价已标记为无效' : '申诉已驳回')
    reviewDialogVisible.value = false
    loadAppeals()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadAppeals()
})
</script>

<style scoped>
.appeal-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}
</style>
