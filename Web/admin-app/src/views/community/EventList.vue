<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.postId" clearable placeholder="帖子ID" style="width: 140px" />
          <el-input v-model="query.userId" clearable placeholder="回复用户ID" style="width: 160px" />
          <el-select v-model="query.status" clearable placeholder="回复状态" style="width: 140px">
            <el-option label="正常" :value="1" />
            <el-option label="屏蔽" :value="0" />
          </el-select>
          <el-select v-model="query.isAdopted" clearable placeholder="采纳状态" style="width: 140px">
            <el-option label="已采纳" :value="1" />
            <el-option label="未采纳" :value="0" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
      </div>

      <el-table v-loading="loading" :data="records" stripe border highlight-current-row style="width: 100%">
        <el-table-column prop="id" label="回复ID" width="120" />
        <el-table-column prop="postId" label="帖子ID" width="120" />
        <el-table-column prop="userNickname" label="回复者" width="140" />
        <el-table-column prop="content" label="回复内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="isAdopted" label="采纳" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.isAdopted === 1 ? 'success' : 'info'" effect="light">
              {{ scope.row.isAdopted === 1 ? '已采纳' : '未采纳' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isExcellent" label="优质" width="120">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.isExcellent === 1"
              @change="(val) => onExcellentChange(scope.row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.status === 1"
              active-text="正常"
              inactive-text="屏蔽"
              inline-prompt
              @change="(val) => onStatusChange(scope.row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="showDetail(scope.row)">详情</el-button>
            <el-button link type="danger" @click="removeReply(scope.row)">删除</el-button>
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

    <el-dialog v-model="detailVisible" title="回复详情" width="760px">
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border v-if="currentReply">
          <el-descriptions-item label="回复ID">{{ currentReply.id }}</el-descriptions-item>
          <el-descriptions-item label="帖子ID">{{ currentReply.postId }}</el-descriptions-item>
          <el-descriptions-item label="回复者">{{ currentReply.userNickname }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ currentReply.userId }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentReply.status }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentReply.createTime }}</el-descriptions-item>
          <el-descriptions-item label="优质">{{ currentReply.isExcellent }}</el-descriptions-item>
          <el-descriptions-item label="采纳">{{ currentReply.isAdopted }}</el-descriptions-item>
          <el-descriptions-item label="内容" :span="2">{{ currentReply.content || '暂无内容' }}</el-descriptions-item>
        </el-descriptions>
        <el-divider />
        <div class="detail-title">关联帖子媒体</div>
        <div v-if="relatedPostMedia.length" class="media-grid">
          <div v-for="media in relatedPostMedia" :key="media.id" class="media-item">
            <el-image
              v-if="media.mediaType === 1"
              :src="media.mediaUrl"
              fit="cover"
              :preview-src-list="[media.mediaUrl]"
              preview-teleported
              class="media-preview"
            />
            <video v-else controls class="media-preview">
              <source :src="media.mediaUrl" />
              当前浏览器不支持视频播放
            </video>
          </div>
        </div>
        <el-empty v-else description="无图片/视频" :image-size="80" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteAdminReply,
  getAdminPostDetail,
  getAdminReplyPage,
  updateReplyExcellent,
  updateReplyStatus
} from '../../api/community'

const loading = ref(false)
const total = ref(0)
const records = ref([])
const detailVisible = ref(false)
const currentReply = ref(null)
const detailLoading = ref(false)
const relatedPostMedia = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  postId: '',
  userId: '',
  status: null,
  isAdopted: null
})

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      postId: query.postId === '' ? undefined : Number(query.postId),
      userId: query.userId === '' ? undefined : Number(query.userId),
      status: query.status,
      isAdopted: query.isAdopted
    }
    Object.keys(params).forEach((key) => {
      if (params[key] === null || params[key] === undefined || Number.isNaN(params[key])) {
        delete params[key]
      }
    })
    const res = await getAdminReplyPage(params)
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
  query.pageSize = 10
  query.postId = ''
  query.userId = ''
  query.status = null
  query.isAdopted = null
  fetchList()
}

const onStatusChange = async (row, value) => {
  await updateReplyStatus({
    replyId: row.id,
    status: value ? 1 : 0
  })
  ElMessage.success('回复状态已更新')
  fetchList()
}

const onExcellentChange = async (row, value) => {
  await updateReplyExcellent({
    replyId: row.id,
    isExcellent: value ? 1 : 0
  })
  ElMessage.success('优质状态已更新')
  fetchList()
}

const showDetail = async (row) => {
  currentReply.value = { ...row }
  detailVisible.value = true
  detailLoading.value = true
  try {
    const detail = await getAdminPostDetail(row.postId)
    relatedPostMedia.value = detail?.post?.mediaList || []
  } finally {
    detailLoading.value = false
  }
}

const removeReply = async (row) => {
  await ElMessageBox.confirm(`确认删除该回复（ID: ${row.id}）吗？`, '提示', { type: 'warning' })
  await deleteAdminReply(row.id)
  ElMessage.success('回复已删除')
  fetchList()
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
}
.header-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  overflow-x: auto;
}
.detail-title {
  margin-bottom: 8px;
  font-weight: 600;
}
.media-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.media-item {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}
.media-preview {
  width: 100%;
  height: 140px;
  object-fit: cover;
  display: block;
}
</style>
