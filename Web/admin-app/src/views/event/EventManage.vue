<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.keyword" clearable placeholder="活动标题" style="width: 220px" />
          <el-select v-model="query.eventType" clearable placeholder="活动类型" style="width: 140px">
            <el-option label="设计竞赛" :value="1" />
            <el-option label="线下活动" :value="2" />
            <el-option label="其他" :value="3" />
          </el-select>
          <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
            <el-option label="未开始" :value="0" />
            <el-option label="报名中" :value="1" />
            <el-option label="进行中" :value="2" />
            <el-option label="评审中" :value="3" />
            <el-option label="已结束" :value="4" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
        <el-button type="primary" @click="openCreate">新增活动</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column prop="title" label="活动标题" min-width="200" />
        <el-table-column label="图片" width="120">
          <template #default="scope">
            <el-image :src="scope.row.bannerUrl" style="width: 80px; height: 45px; border-radius: 6px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="eventTypeName" label="类型" width="100" />
        <el-table-column label="时间" width="200">
          <template #default="scope">
            {{formatTime(scope.row.startTime)}} ~ {{formatTime(scope.row.endTime)}}
          </template>
        </el-table-column>
        <el-table-column label="报名人数" width="100">
          <template #default="scope">
            {{scope.row.currentParticipants}}{{scope.row.maxParticipants ? '/' + scope.row.maxParticipants : ''}}
          </template>
        </el-table-column>
        <el-table-column prop="statusName" label="状态" width="100" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
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

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑活动' : '新增活动'" width="800px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="活动标题">
              <el-input v-model="form.title" maxlength="200" placeholder="请输入活动标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="活动类型">
              <el-select v-model="form.eventType" style="width: 100%">
                <el-option label="设计竞赛" :value="1" />
                <el-option label="线下活动" :value="2" />
                <el-option label="其他" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="活动地点">
              <el-input v-model="form.location" placeholder="线下活动必填" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Banner图">
              <div style="display: flex; gap: 10px; width: 100%; align-items: center;">
                <el-input v-model="form.bannerUrl" placeholder="请上传或粘贴图片URL" />
                <el-button :loading="uploading" @click.prevent="triggerUpload">上传</el-button>
                <input ref="fileRef" type="file" accept="image/*" style="display:none" @change="uploadImage" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报名开始">
              <el-date-picker v-model="form.signupStart" type="datetime" placeholder="选择报名开始时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报名截止">
              <el-date-picker v-model="form.signupEnd" type="datetime" placeholder="选择报名截止时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人数上限">
              <el-input-number v-model="form.maxParticipants" :min="0" placeholder="不填则不限" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="活动状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="未开始" :value="0" />
                <el-option label="报名中" :value="1" />
                <el-option label="进行中" :value="2" />
                <el-option label="评审中" :value="3" />
                <el-option label="已结束" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="活动介绍">
              <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入活动介绍" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="参赛规则">
              <el-input v-model="form.rules" type="textarea" :rows="3" placeholder="请输入参赛规则" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 奖励配置 -->
        <el-divider content-position="left">奖励设置</el-divider>
        <div v-for="(reward, index) in form.rewards" :key="index" style="margin-bottom: 16px; padding: 12px; background: #f9fafb; border-radius: 8px;">
          <div style="display: flex; gap: 10px; align-items: center;">
            <div style="width: 140px;">
              <div style="font-size: 12px; color: #909399; margin-bottom: 4px;">奖项名称</div>
              <el-input v-model="reward.rankName" placeholder="如：一等奖" />
            </div>
            <div style="width: 100px;">
              <div style="font-size: 12px; color: #909399; margin-bottom: 4px;">获奖人数</div>
              <el-input-number v-model="reward.winnerCount" :min="1" style="width: 100%" />
            </div>
            <div style="flex: 1;">
              <div style="font-size: 12px; color: #909399; margin-bottom: 4px;">奖品内容</div>
              <el-input v-model="reward.prizeContent" placeholder="如：证书+奖杯" />
            </div>
            <div style="width: 120px;">
              <div style="font-size: 12px; color: #909399; margin-bottom: 4px;">奖励积分</div>
              <el-input-number v-model="reward.points" :min="0" style="width: 100%" />
            </div>
            <el-button type="danger" @click="removeReward(index)" :icon="Delete" circle style="margin-top: 20px;" />
          </div>
        </div>
        <el-button type="primary" link @click="addReward">+ 添加奖项</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { uploadFileWithProgress } from '../../api/model'
import {
  getEventAdminList,
  getEventDetail,
  createEvent,
  updateEvent,
  deleteEvent
} from '../../api/event'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const total = ref(0)
const records = ref([])
const fileRef = ref(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  eventType: null,
  status: null
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({
  id: null,
  title: '',
  bannerUrl: '',
  eventType: 1,
  description: '',
  rules: '',
  location: '',
  startTime: null,
  endTime: null,
  signupStart: null,
  signupEnd: null,
  maxParticipants: null,
  status: 0,
  rewards: []
})

const formatTime = (time) => {
  if (!time) return ''
  return time.substring(0, 10)
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getEventAdminList({ ...query })
    records.value = res.records || []
    total.value = Number(res.total || 0)
    query.pageNum = Number(res.pageNum || query.pageNum)
    query.pageSize = Number(res.pageSize || query.pageSize)
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
  query.keyword = ''
  query.eventType = null
  query.status = null
  fetchList()
}

const resetForm = () => {
  form.id = null
  form.title = ''
  form.bannerUrl = ''
  form.eventType = 1
  form.description = ''
  form.rules = ''
  form.location = ''
  form.startTime = null
  form.endTime = null
  form.signupStart = null
  form.signupEnd = null
  form.maxParticipants = null
  form.status = 0
  form.rewards = []
}

const openCreate = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEdit = async (row) => {
  isEdit.value = true
  try {
    const res = await getEventDetail(row.id)
    form.id = res.id
    form.title = res.title
    form.bannerUrl = res.bannerUrl
    form.eventType = res.eventType
    form.description = res.description
    form.rules = res.rules
    form.location = res.location
    form.startTime = res.startTime
    form.endTime = res.endTime
    form.signupStart = res.signupStart
    form.signupEnd = res.signupEnd
    form.maxParticipants = res.maxParticipants
    form.status = res.status
    form.rewards = (res.rewards || []).map(r => ({
      rankName: r.rankName,
      winnerCount: r.winnerCount || 1,
      prizeContent: r.prizeContent,
      points: r.points || 0
    }))
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取活动详情失败')
  }
}

const addReward = () => {
  form.rewards.push({
    rankName: '',
    winnerCount: 1,
    prizeContent: '',
    points: 0
  })
}

const removeReward = (index) => {
  form.rewards.splice(index, 1)
}

const submit = async () => {
  if (!form.title || !form.bannerUrl || !form.startTime || !form.endTime) {
    ElMessage.warning('请填写必填项')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title,
      bannerUrl: form.bannerUrl,
      eventType: form.eventType,
      description: form.description,
      rules: form.rules,
      location: form.location,
      startTime: form.startTime,
      endTime: form.endTime,
      signupStart: form.signupStart,
      signupEnd: form.signupEnd,
      maxParticipants: form.maxParticipants,
      status: form.status,
      rewards: form.rewards.filter(r => r.rankName && r.prizeContent)
    }
    if (isEdit.value) {
      await updateEvent({ id: form.id, ...payload })
      ElMessage.success('更新成功')
    } else {
      await createEvent(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除活动【${row.title}】吗？`, '提示', { type: 'warning' })
  await deleteEvent(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

const triggerUpload = () => {
  fileRef.value?.click()
}

const resolveUploadUrl = (response) => {
  if (!response) return ''
  if (typeof response === 'string') return response
  if (typeof response === 'object') {
    if (typeof response.data === 'string') return response.data
    if (typeof response.url === 'string') return response.url
    if (typeof response.message === 'string') return response.message
  }
  return ''
}

const uploadImage = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const response = await uploadFileWithProgress(file, 'event', () => {})
    const uploadUrl = resolveUploadUrl(response)
    if (!uploadUrl) {
      throw new Error('未获取到图片地址')
    }
    form.bannerUrl = uploadUrl
    ElMessage.success('图片上传成功')
  } catch (error) {
    console.error('图片上传失败:', error)
    ElMessage.error('上传失败，请重试')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

onMounted(fetchList)
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
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}
</style>
