<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-select v-model="query.eventId" clearable placeholder="选择活动" style="width: 220px">
            <el-option v-for="event in events" :key="event.id" :label="event.title" :value="event.id" />
          </el-select>
          <el-select v-model="query.status" clearable placeholder="审核状态" style="width: 140px">
            <el-option label="待审核" :value="1" />
            <el-option label="已通过" :value="2" />
            <el-option label="已拒绝" :value="3" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column prop="title" label="作品标题" min-width="180" />
        <el-table-column label="作品图片" width="120">
          <template #default="scope">
            <el-image
              v-if="scope.row.imageUrls && scope.row.imageUrls.length"
              :src="scope.row.imageUrls[0]"
              style="width: 80px; height: 60px; border-radius: 6px"
              fit="cover"
              :preview-src-list="scope.row.imageUrls"
            />
            <span v-else style="color: #999">无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="authorName" label="作者" width="120" />
        <el-table-column prop="eventTitle" label="所属活动" width="180">
          <template #default="scope">
            <span>{{ getEventTitle(scope.row.eventId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="点赞/评论" width="100">
          <template #default="scope">
            {{ scope.row.likeCount || 0 }} / {{ scope.row.commentCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">查看</el-button>
            <el-button v-if="scope.row.status === 1" link type="success" @click="openReview(scope.row, 2)">通过</el-button>
            <el-button v-if="scope.row.status === 1" link type="danger" @click="openReview(scope.row, 3)">拒绝</el-button>
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

    <!-- 作品详情弹窗 -->
    <el-dialog v-model="detailVisible" title="作品详情" width="800px" @close="closeDetail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="作品标题" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="作者">{{ detail.authorName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detail.status)">{{ detail.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属活动" :span="2">{{ getEventTitle(detail.eventId) }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="点赞/评论">{{ detail.likeCount || 0 }} / {{ detail.commentCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="作品描述" :span="2">{{ detail.description || '无' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="detail.imageUrls && detail.imageUrls.length" style="margin-top: 20px">
        <div style="font-weight: 600; margin-bottom: 10px">作品图片</div>
        <el-image
          v-for="(img, idx) in detail.imageUrls"
          :key="idx"
          :src="img"
          style="width: 120px; height: 90px; margin-right: 10px; border-radius: 8px"
          fit="cover"
          :preview-src-list="detail.imageUrls"
        />
      </div>

      <div v-if="detail.fileUrls && detail.fileUrls.length" style="margin-top: 20px">
        <div style="font-weight: 600; margin-bottom: 10px">模型文件</div>
        <div v-for="(file, idx) in detail.fileUrls" :key="idx" style="margin-bottom: 8px">
          <el-link :href="file" target="_blank" type="primary">{{ getFileName(file) }}</el-link>
        </div>
      </div>

      <!-- 评论列表 -->
      <div style="margin-top: 20px">
        <div style="font-weight: 600; margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center;">
          <span>评论列表 ({{ comments.length }})</span>
        </div>

        <div v-if="loadingComments" style="text-align: center; padding: 20px; color: #999;">
          加载中...
        </div>
        <div v-else-if="!comments.length" style="text-align: center; padding: 20px; color: #999;">
          暂无评论
        </div>
        <div v-else class="comment-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <span class="comment-user">{{ comment.userName || '匿名用户' }}</span>
              <span class="comment-time">{{ formatDateTime(comment.createTime) }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-footer">
              <span class="comment-likes">
                <el-icon><Star /></el-icon>
                {{ comment.likeCount || 0 }}
              </span>
              <el-button link type="danger" size="small" @click="handleDeleteComment(comment)">删除</el-button>
            </div>

            <!-- 子评论 -->
            <div v-if="comment.children && comment.children.length" class="child-comments">
              <div v-for="child in comment.children" :key="child.id" class="child-comment-item">
                <div class="comment-header">
                  <span class="comment-user">{{ child.userName || '匿名用户' }}</span>
                  <span v-if="child.replyToUserName" class="reply-to">回复 {{ child.replyToUserName }}</span>
                  <span class="comment-time">{{ formatDateTime(child.createTime) }}</span>
                </div>
                <div class="comment-content">{{ child.content }}</div>
                <div class="comment-footer">
                  <span class="comment-likes">
                    <el-icon><Star /></el-icon>
                    {{ child.likeCount || 0 }}
                  </span>
                  <el-button link type="danger" size="small" @click="handleDeleteComment(child)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="reviewVisible" :title="reviewForm.status === 2 ? '通过审核' : '拒绝作品'" width="500px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审核结果">
          <el-tag :type="reviewForm.status === 2 ? 'success' : 'danger'">
            {{ reviewForm.status === 2 ? '通过' : '拒绝' }}
          </el-tag>
        </el-form-item>
        <el-form-item v-if="reviewForm.status === 2" label="评分">
          <el-rate v-model="reviewForm.score" :allow-half="true" />
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="reviewForm.reviewRemark" type="textarea" :rows="3" placeholder="可选填写审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star } from '@element-plus/icons-vue'
import { getAllAdminSubmissions, reviewSubmission, getEventAdminList, getAdminSubmissionComments, adminDeleteComment } from '../../api/event'

const loading = ref(false)
const reviewing = ref(false)
const total = ref(0)
const records = ref([])
const events = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null,
  eventId: null
})

const detailVisible = ref(false)
const detail = ref({})
const comments = ref([])
const loadingComments = ref(false)

const reviewVisible = ref(false)
const reviewForm = reactive({
  submissionId: null,
  status: 2,
  score: 0,
  reviewRemark: ''
})

const formatDateTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const getStatusType = (status) => {
  const map = { 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status] || 'info'
}

const getFileName = (url) => {
  if (!url) return '文件'
  const parts = url.split('/')
  return decodeURIComponent(parts[parts.length - 1]) || '模型文件'
}

const getEventTitle = (eventId) => {
  if (!eventId) return '未知活动'
  const event = events.value.find(e => e.id === eventId || e.id === String(eventId))
  return event ? event.title : '未知活动'
}

const fetchEvents = async () => {
  try {
    // 获取所有类型的活动，不限制 eventType
    const res = await getEventAdminList({ pageNum: 1, pageSize: 100 })
    events.value = res.records || []
  } catch (error) {
    console.error('获取活动列表失败:', error)
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const params = {}
    // 只传递有值的参数
    if (query.pageNum) params.pageNum = query.pageNum
    if (query.pageSize) params.pageSize = query.pageSize
    if (query.status !== null && query.status !== undefined) {
      params.status = String(query.status)
    }
    if (query.eventId !== null && query.eventId !== undefined) {
      params.eventId = query.eventId
    }
    const res = await getAllAdminSubmissions(params)
    records.value = res.records || []
    total.value = Number(res.total || 0)
  } finally {
    loading.value = false
  }
}

const fetchComments = async (submissionId) => {
  loadingComments.value = true
  try {
    const res = await getAdminSubmissionComments(submissionId, { pageNum: 1, pageSize: 100 })
    comments.value = res.records || []
  } catch (error) {
    console.error('获取评论失败:', error)
    comments.value = []
  } finally {
    loadingComments.value = false
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

const openDetail = (row) => {
  detail.value = row
  detailVisible.value = true
  fetchComments(row.id)
}

const closeDetail = () => {
  comments.value = []
}

const handleDeleteComment = (comment) => {
  ElMessageBox.confirm(
    '确定要删除该评论吗？此操作不可恢复。',
    '删除评论',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await adminDeleteComment(comment.id)
      ElMessage.success('删除成功')
      // 刷新评论列表
      fetchComments(detail.value.id)
      // 更新列表中的评论数
      fetchList()
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

const openReview = (row, status) => {
  reviewForm.submissionId = row.id
  reviewForm.status = status
  reviewForm.score = 0
  reviewForm.reviewRemark = ''
  reviewVisible.value = true
}

const submitReview = async () => {
  reviewing.value = true
  try {
    await reviewSubmission({
      submissionId: reviewForm.submissionId,
      status: reviewForm.status,
      score: reviewForm.status === 2 && reviewForm.score > 0 ? reviewForm.score * 2 : null,
      reviewRemark: reviewForm.reviewRemark
    })
    ElMessage.success('审核成功')
    reviewVisible.value = false
    fetchList()
  } catch (error) {
    ElMessage.error(error.message || '审核失败')
  } finally {
    reviewing.value = false
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

/* 评论列表样式 */
.comment-list {
  max-height: 400px;
  overflow-y: auto;
}
.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.comment-user {
  font-weight: 600;
  color: #303133;
}
.reply-to {
  color: #409eff;
  font-size: 13px;
}
.comment-time {
  color: #909399;
  font-size: 12px;
}
.comment-content {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
}
.comment-footer {
  display: flex;
  align-items: center;
  gap: 15px;
}
.comment-likes {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
}
.child-comments {
  margin-left: 30px;
  margin-top: 12px;
  padding-left: 15px;
  border-left: 2px solid #e4e7ed;
}
.child-comment-item {
  padding: 10px 0;
  border-bottom: 1px dashed #f0f0f0;
}
.child-comment-item:last-child {
  border-bottom: none;
}
</style>
