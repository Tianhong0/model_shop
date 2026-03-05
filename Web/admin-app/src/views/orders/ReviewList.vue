<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input-number v-model="queryParams.orderId" :min="1" controls-position="right" placeholder="订单ID" style="width: 160px" />
          <el-input-number v-model="queryParams.modelId" :min="1" controls-position="right" placeholder="模型ID" style="width: 160px" />
          <el-input-number v-model="queryParams.userId" :min="1" controls-position="right" placeholder="用户ID" style="width: 160px" />
          <el-select v-model="queryParams.status" placeholder="评价状态" clearable style="width: 140px">
            <el-option label="正常展示" :value="1" />
            <el-option label="已屏蔽" :value="0" />
          </el-select>
          <el-button type="primary" @click="fetchCommentList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
      </div>

      <el-table v-loading="loading" :data="reviewData" stripe border highlight-current-row style="width: 100%">
        <el-table-column prop="id" label="评价ID" width="180" />
        <el-table-column prop="orderId" label="订单ID" width="150" />
        <el-table-column prop="modelName" label="模型名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="userNickname" label="评价用户" width="140" />
        <el-table-column prop="avgScore" label="综合评分" width="120">
          <template #default="scope">
            <el-rate :model-value="scope.row.avgScore" disabled allow-half />
          </template>
        </el-table-column>
        <el-table-column prop="commentText" label="评价内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '已展示' : '已屏蔽' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评价时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleDetail(scope.row)">详情</el-button>
            <el-button link type="primary" @click="toggleStatus(scope.row)">
              {{ scope.row.status === 1 ? '屏蔽' : '恢复' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          :page-count="Math.max(1, Math.ceil((total || 0) / queryParams.pageSize))"
          :hide-on-single-page="false"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="评价详情" width="760px">
      <el-descriptions v-if="currentComment" :column="2" border>
        <el-descriptions-item label="评价ID">{{ currentComment.id }}</el-descriptions-item>
        <el-descriptions-item label="订单ID">{{ currentComment.orderId }}</el-descriptions-item>
        <el-descriptions-item label="模型ID">{{ currentComment.modelId }}</el-descriptions-item>
        <el-descriptions-item label="模型名称">{{ currentComment.modelName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentComment.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户昵称">{{ currentComment.userNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模型评分">{{ currentComment.modelScore }}</el-descriptions-item>
        <el-descriptions-item label="打印评分">{{ currentComment.printScore }}</el-descriptions-item>
        <el-descriptions-item label="服务评分">{{ currentComment.serviceScore }}</el-descriptions-item>
        <el-descriptions-item label="综合评分">{{ currentComment.avgScore }}</el-descriptions-item>
        <el-descriptions-item label="匿名评价">{{ currentComment.isAnonymous === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="展示状态">
          <el-tag :type="currentComment.status === 1 ? 'success' : 'info'">
            {{ currentComment.status === 1 ? '已展示' : '已屏蔽' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="评价内容" :span="2">{{ currentComment.commentText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评价媒体" :span="2">
          <div v-if="commentMediaList.length" class="img-list">
            <template v-for="(media, index) in commentMediaList" :key="index">
              <div v-if="media.type === 'video'" class="video-wrap">
                <video class="comment-video" controls preload="metadata" playsinline webkit-playsinline>
                  <source :src="media.url" :type="getVideoMimeType(media.url)" />
                </video>
                <a class="video-link" :href="media.url" target="_blank" rel="noopener noreferrer">下载视频</a>
              </div>
              <el-image
                v-else
                :src="media.url"
                fit="cover"
                style="width: 72px; height: 72px; border-radius: 8px"
                :preview-src-list="commentImages"
                preview-teleported
              />
            </template>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="设计者回复" :span="2">{{ currentComment.replyContent || '-' }}</el-descriptions-item>
        <el-descriptions-item label="回复时间">{{ currentComment.replyTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评价时间">{{ currentComment.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户追评" :span="2">
          <div v-if="commentReplies.length" class="reply-list">
            <div v-for="item in commentReplies" :key="item.id" class="reply-item">
              <div class="reply-meta">
                <span>{{ item.userNickname || '用户' }}</span>
                <span>{{ item.createTime || '-' }}</span>
              </div>
              <div class="reply-content">{{ item.content }}</div>
            </div>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCommentAdminDetail, getCommentAdminList, getCommentAdminReplyList, updateCommentAdminStatus } from '../../api/order'

const loading = ref(false)
const total = ref(0)
const reviewData = ref([])

const detailVisible = ref(false)
const currentComment = ref(null)
const commentReplies = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderId: null,
  modelId: null,
  userId: null,
  status: null
})

const commentImages = computed(() => {
  const raw = currentComment.value?.commentImages
  if (!raw) return []
  return String(raw).split(',').map(item => item.trim()).filter(item => item && !isVideoUrl(item))
})

const commentMediaList = computed(() => {
  const raw = currentComment.value?.commentImages
  if (!raw) return []
  return String(raw)
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
    .map(url => ({
      url,
      type: isVideoUrl(url) ? 'video' : 'image'
    }))
})

const isVideoUrl = (url) => {
  const lower = String(url || '').toLowerCase().split('?')[0]
  return /\.(mp4|mov|m4v|webm|ogg)$/i.test(lower) || lower.includes('/videos/')
}

const getVideoMimeType = (url) => {
  const lower = String(url || '').toLowerCase().split('?')[0]
  if (lower.endsWith('.mov')) return 'video/quicktime'
  if (lower.endsWith('.webm')) return 'video/webm'
  if (lower.endsWith('.ogg')) return 'video/ogg'
  return 'video/mp4'
}

const fetchCommentList = async () => {
  loading.value = true
  try {
    const response = await getCommentAdminList({ ...queryParams })
    reviewData.value = response.records || []
    total.value = Number(response.total || 0)
    queryParams.pageNum = Number(response.pageNum || queryParams.pageNum)
    queryParams.pageSize = Number(response.pageSize || queryParams.pageSize)
  } catch (error) {
    console.error('获取评价列表失败:', error)
    ElMessage.error('获取评价列表失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: 10,
    orderId: null,
    modelId: null,
    userId: null,
    status: null
  })
  fetchCommentList()
}

const handlePageChange = (pageNum) => {
  queryParams.pageNum = pageNum
  fetchCommentList()
}

const handleSizeChange = (pageSize) => {
  queryParams.pageSize = pageSize
  queryParams.pageNum = 1
  fetchCommentList()
}

const handleDetail = async (row) => {
  try {
    const [detail, replyPage] = await Promise.all([
      getCommentAdminDetail(row.id),
      getCommentAdminReplyList(row.id)
    ])
    currentComment.value = detail
    commentReplies.value = Array.isArray(replyPage?.records) ? replyPage.records : []
    detailVisible.value = true
  } catch (error) {
    console.error('获取评价详情失败:', error)
    ElMessage.error('获取评价详情失败')
  }
}

const toggleStatus = async (row) => {
  const targetStatus = row.status === 1 ? 0 : 1
  try {
    await updateCommentAdminStatus({
      commentId: row.id,
      status: targetStatus
    })
    ElMessage.success(targetStatus === 1 ? '评价已恢复展示' : '评价已屏蔽')
    await fetchCommentList()
  } catch (error) {
    console.error('更新评价状态失败:', error)
    ElMessage.error('更新评价状态失败')
  }
}

onMounted(() => {
  fetchCommentList()
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
}
.img-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.video-wrap { width: 160px; }
.comment-video { width: 160px; height: 96px; border-radius: 8px; border: 1px solid #e2e8f0; background: #000; display: block; }
.video-link { display: block; margin-top: 6px; color: #4f46e5; font-size: 12px; text-decoration: none; }
.reply-list { display: flex; flex-direction: column; gap: 8px; }
.reply-item { padding: 8px 10px; border-radius: 8px; background: #f8fafc; border: 1px solid #e2e8f0; }
.reply-meta { display: flex; justify-content: space-between; font-size: 12px; color: #64748b; margin-bottom: 6px; }
.reply-content { font-size: 13px; color: #334155; line-height: 1.6; white-space: pre-wrap; }
</style>
