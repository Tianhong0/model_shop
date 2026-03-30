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
        <el-table-column prop="result" label="参与结果" min-width="150">
          <template #default="scope">
            {{ scope.row.result || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">修改状态</el-button>
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
          <el-input v-model="form.awardRank" placeholder="请输入获奖奖项" />
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAllAdminParticipations,
  getEventAdminList,
  updateParticipationStatus
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
const form = reactive({
  id: null,
  userId: null,
  userName: '',
  userAvatar: '',
  eventTitle: '',
  status: 1,
  awardRank: '',
  result: ''
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

const openEdit = (row) => {
  form.id = row.id
  form.userId = row.userId
  form.userName = row.userName || '未知用户'
  form.userAvatar = row.userAvatar
  form.eventTitle = row.eventTitle
  form.status = row.status
  form.awardRank = row.awardRank || ''
  form.result = row.result || ''
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
