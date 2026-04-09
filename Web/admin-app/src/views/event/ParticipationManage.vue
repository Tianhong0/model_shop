<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-select v-model="query.eventId" clearable placeholder="选择活动" style="width: 220px">
            <el-option v-for="event in events" :key="event.id" :label="event.title" :value="event.id" />
          </el-select>
          <el-select v-model="query.status" clearable placeholder="报名状态" style="width: 140px">
            <el-option label="已报名" :value="1" />
            <el-option label="已签到" :value="2" />
            <el-option label="已提交作品" :value="3" />
            <el-option label="已获奖" :value="4" />
            <el-option label="已取消" :value="5" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
        <el-button type="success" @click="openBatchAward" :disabled="!canBatchAward">批量颁奖</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column label="用户" min-width="150">
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 10px;">
              <el-avatar :src="scope.row.userAvatar" :size="36" />
              <span>{{ scope.row.userName || '未知用户' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="eventTitle" label="所属活动" min-width="180" />
        <el-table-column prop="eventTypeName" label="活动类型" width="100" />
        <el-table-column label="报名时间" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.signupTime) }}
          </template>
        </el-table-column>
        <el-table-column label="签到时间" width="180">
          <template #default="scope">
            {{ scope.row.checkinTime ? formatDateTime(scope.row.checkinTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="awardRank" label="获奖奖项" width="120">
          <template #default="scope">
            {{ scope.row.awardRank || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="积分发放" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 4" :type="scope.row.pointsSent ? 'success' : 'warning'">
              {{ scope.row.pointsSent ? '已发放' : '未发放' }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="参与结果" min-width="150">
          <template #default="scope">
            {{ scope.row.result || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">修改状态</el-button>
            <el-button v-if="scope.row.status === 4 && !scope.row.pointsSent" link type="success" @click="openAward(scope.row)">颁奖</el-button>
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

    <!-- 修改状态弹窗 -->
    <el-dialog v-model="dialogVisible" title="修改报名状态" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户">
          <div style="display: flex; align-items: center; gap: 10px;">
            <el-avatar :src="form.userAvatar" :size="36" />
            <span>{{ form.userName }}</span>
          </div>
        </el-form-item>
        <el-form-item label="所属活动">
          <span>{{ form.eventTitle }}</span>
        </el-form-item>
        <el-form-item label="报名状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="已报名" :value="1" />
            <el-option label="已签到" :value="2" />
            <el-option label="已提交作品" :value="3" />
            <el-option label="已获奖" :value="4" />
            <el-option label="已取消" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.status === 4" label="获奖奖项">
          <el-select v-model="form.awardRank" placeholder="请选择获奖奖项" style="width: 100%">
            <el-option v-for="r in formRewards" :key="r.rankName" :label="r.rankName" :value="r.rankName">
              <span>{{ r.rankName }}</span>
              <span v-if="r.points" style="color: #909399; font-size: 12px; margin-left: 8px;">({{ r.points }}积分)</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="参与结果">
          <el-input v-model="form.result" type="textarea" :rows="2" placeholder="可选填写参与结果" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 颁奖弹窗 -->
    <el-dialog v-model="awardDialogVisible" title="颁发积分奖励" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户">
          <div style="display: flex; align-items: center; gap: 10px;">
            <el-avatar :src="awardForm.userAvatar" :size="36" />
            <span>{{ awardForm.userName }}</span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="活动">{{ awardForm.eventTitle }}</el-descriptions-item>
        <el-descriptions-item label="获奖奖项">{{ awardForm.awardRank }}</el-descriptions-item>
        <el-descriptions-item label="奖励积分">
          <el-tag type="warning" size="large">{{ awardForm.points }} 积分</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="awardDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="awarding" @click="submitAward">确认发放</el-button>
      </template>
    </el-dialog>

    <!-- 批量颁奖弹窗 -->
    <el-dialog v-model="batchAwardDialogVisible" title="批量颁奖确认" width="500px">
      <el-alert type="warning" :closable="false" style="margin-bottom: 16px;">
        <template #title>
          将为活动【{{ batchAwardForm.eventTitle }}】的所有获奖者发放积分奖励
        </template>
      </el-alert>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="待发放人数">{{ batchAwardForm.pendingCount }} 人</el-descriptions-item>
        <el-descriptions-item label="预计发放积分">{{ batchAwardForm.totalPoints }} 积分</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="batchAwardDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="awarding" @click="submitBatchAward">确认发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAllAdminParticipations,
  getEventAdminList,
  updateParticipationStatus,
  awardPoints,
  awardAllWinners,
  getEventDetail
} from '../../api/event'

const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const records = ref([])
const events = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null,
  eventId: null
})

const dialogVisible = ref(false)
const formRewards = ref([])
const form = reactive({
  id: null,
  eventId: null,
  userId: null,
  userName: '',
  userAvatar: '',
  eventTitle: '',
  status: 1,
  awardRank: '',
  result: ''
})

// 颁奖相关
const awardDialogVisible = ref(false)
const batchAwardDialogVisible = ref(false)
const awarding = ref(false)
const awardForm = reactive({
  participationId: null,
  eventId: null,
  userId: null,
  userName: '',
  userAvatar: '',
  eventTitle: '',
  awardRank: '',
  points: 0
})
const batchAwardForm = reactive({
  eventId: null,
  eventTitle: '',
  pendingCount: 0,
  totalPoints: 0
})

const formatDateTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const getStatusType = (status) => {
  const map = { 1: 'warning', 2: 'success', 3: 'info', 4: 'success', 5: 'danger' }
  return map[status] || 'info'
}

const fetchEvents = async () => {
  try {
    const res = await getEventAdminList({ pageNum: 1, pageSize: 100 })
    events.value = res.records || []
  } catch (error) {
    console.error('获取活动列表失败:', error)
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (query.status !== null && query.status !== undefined) {
      params.status = String(query.status)
    }
    if (query.eventId !== null && query.eventId !== undefined) {
      params.eventId = query.eventId
    }
    const res = await getAllAdminParticipations(params)
    records.value = res.records || []
    total.value = Number(res.total || 0)
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
  query.status = null
  query.eventId = null
  fetchList()
}

const openEdit = async (row) => {
  form.id = row.id
  form.eventId = row.eventId
  form.userId = row.userId
  form.userName = row.userName || '未知用户'
  form.userAvatar = row.userAvatar
  form.eventTitle = row.eventTitle
  form.status = row.status
  form.awardRank = row.awardRank || ''
  form.result = row.result || ''

  // 获取活动的奖励列表
  formRewards.value = []
  if (row.eventId) {
    try {
      const eventDetail = await getEventDetail(row.eventId)
      formRewards.value = eventDetail.rewards || []
    } catch (error) {
      console.error('获取活动奖励失败:', error)
    }
  }

  dialogVisible.value = true
}

const submit = async () => {
  saving.value = true
  try {
    await updateParticipationStatus({
      id: form.id,
      status: form.status,
      awardRank: form.status === 4 ? form.awardRank : null,
      result: form.result || null
    })
    ElMessage.success('修改成功')
    dialogVisible.value = false
    fetchList()
  } catch (error) {
    ElMessage.error(error.message || '修改失败')
  } finally {
    saving.value = false
  }
}

// 计算是否可以批量颁奖
const canBatchAward = computed(() => {
  return query.eventId && records.value.some(r => r.status === 4 && !r.pointsSent)
})

// 打开单个颁奖弹窗
const openAward = async (row) => {
  awardForm.participationId = row.id
  awardForm.eventId = row.eventId
  awardForm.userId = row.userId
  awardForm.userName = row.userName || '未知用户'
  awardForm.userAvatar = row.userAvatar
  awardForm.eventTitle = row.eventTitle
  awardForm.awardRank = row.awardRank

  // 获取对应奖项的积分
  try {
    const eventDetail = await getEventDetail(row.eventId)
    const reward = (eventDetail.rewards || []).find(r => r.rankName === row.awardRank)
    awardForm.points = reward?.points || 0
  } catch (error) {
    awardForm.points = 0
  }

  if (awardForm.points <= 0) {
    ElMessage.warning('该奖项未配置积分奖励')
    return
  }

  awardDialogVisible.value = true
}

// 提交单个颁奖
const submitAward = async () => {
  awarding.value = true
  try {
    await awardPoints({
      eventId: awardForm.eventId,
      participationIds: [awardForm.participationId]
    })
    ElMessage.success('积分发放成功')
    awardDialogVisible.value = false
    fetchList()
  } catch (error) {
    ElMessage.error(error.message || '发放失败')
  } finally {
    awarding.value = false
  }
}

// 打开批量颁奖弹窗
const openBatchAward = async () => {
  if (!query.eventId) {
    ElMessage.warning('请先选择活动')
    return
  }

  // 获取待发放人数和总积分
  const pendingWinners = records.value.filter(r => r.status === 4 && !r.pointsSent)
  if (pendingWinners.length === 0) {
    ElMessage.warning('没有需要发放积分的获奖者')
    return
  }

  batchAwardForm.eventId = query.eventId
  batchAwardForm.eventTitle = events.value.find(e => e.id === query.eventId)?.title || ''
  batchAwardForm.pendingCount = pendingWinners.length

  // 计算总积分
  try {
    const eventDetail = await getEventDetail(query.eventId)
    const pointsMap = new Map((eventDetail.rewards || []).map(r => [r.rankName, r.points || 0]))
    batchAwardForm.totalPoints = pendingWinners.reduce((sum, w) => {
      return sum + (pointsMap.get(w.awardRank) || 0)
    }, 0)
  } catch (error) {
    batchAwardForm.totalPoints = 0
  }

  batchAwardDialogVisible.value = true
}

// 提交批量颁奖
const submitBatchAward = async () => {
  awarding.value = true
  try {
    const result = await awardAllWinners(batchAwardForm.eventId)
    batchAwardDialogVisible.value = false
    fetchList()
    ElMessage.success(`颁奖完成: 成功${result.successCount}人, 跳过${result.skippedCount}人, 发放${result.totalPoints}积分`)
  } catch (error) {
    ElMessage.error(error.message || '批量颁奖失败')
  } finally {
    awarding.value = false
  }
}

onMounted(() => {
  fetchEvents()
  fetchList()
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
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  overflow-x: auto;
}
</style>
