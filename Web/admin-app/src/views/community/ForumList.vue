<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.keyword" clearable placeholder="标题/内容关键词" style="width: 240px" />
          <el-select v-model="query.categoryId" clearable placeholder="全部分类" style="width: 160px">
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下架" :value="2" />
          </el-select>
          <el-select v-model="query.isTop" clearable placeholder="全部置顶" style="width: 140px">
            <el-option label="置顶" :value="1" />
            <el-option label="普通" :value="0" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
      </div>

      <el-table v-loading="loading" :data="records" stripe border highlight-current-row style="width: 100%">
        <el-table-column prop="id" label="帖子ID" width="120" />
        <el-table-column prop="title" label="帖子标题" min-width="250" show-overflow-tooltip />
        <el-table-column prop="userNickname" label="发布者" width="140" />
        <el-table-column label="分类" width="180">
          <template #default="scope">
            <el-select
              :model-value="scope.row.categoryId"
              style="width: 150px"
              @change="(val) => onCategoryChange(scope.row, val)"
            >
              <el-option
                v-for="item in categoryOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="90" />
        <el-table-column prop="likeCount" label="点赞" width="90" />
        <el-table-column prop="collectCount" label="收藏" width="90" />
        <el-table-column prop="replyCount" label="回复" width="90" />
        <el-table-column prop="isTop" label="置顶" width="120">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.isTop === 1"
              @change="(val) => onTopChange(scope.row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="140">
          <template #default="scope">
            <el-select
              :model-value="scope.row.status"
              style="width: 110px"
              @change="(val) => onStatusChange(scope.row, val)"
            >
              <el-option label="草稿" :value="0" />
              <el-option label="已发布" :value="1" />
              <el-option label="已下架" :value="2" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="showDetail(scope.row)">详情</el-button>
            <el-button link type="danger" @click="removePost(scope.row)">删除</el-button>
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

    <el-dialog v-model="detailVisible" title="帖子详情" width="900px">
      <div v-loading="detailLoading">
        <template v-if="postDetail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="帖子ID">{{ postDetail.post?.id }}</el-descriptions-item>
            <el-descriptions-item label="标题">{{ postDetail.post?.title }}</el-descriptions-item>
            <el-descriptions-item label="发布者">{{ postDetail.post?.userNickname }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ postDetail.post?.categoryName }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ postDetail.post?.status }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ postDetail.post?.createTime }}</el-descriptions-item>
          </el-descriptions>
          <el-divider />
          <div class="detail-title">正文内容</div>
          <div class="detail-content">{{ postDetail.content || '暂无内容' }}</div>
          <el-divider />
          <div class="detail-title">媒体内容</div>
          <div v-if="(postDetail.post?.mediaList || []).length" class="media-grid">
            <div v-for="media in postDetail.post.mediaList" :key="media.id" class="media-item">
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
          <el-divider />
          <div class="detail-title">回复列表（{{ postDetail.replies?.length || 0 }}）</div>
          <el-table :data="postDetail.replies || []" size="small" border>
            <el-table-column prop="id" label="回复ID" width="110" />
            <el-table-column prop="userNickname" label="回复者" width="140" />
            <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80" />
            <el-table-column prop="isExcellent" label="优质" width="80" />
            <el-table-column prop="isAdopted" label="采纳" width="80" />
            <el-table-column prop="createTime" label="时间" width="160" />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="scope">
                <el-popconfirm title="确认删除该回复？" @confirm="removeReply(scope.row)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </template>
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
  deleteAdminPost,
  deleteAdminReply,
  getAdminCategoryList,
  getAdminPostDetail,
  getAdminPostPage,
  updateAdminPostCategory,
  updatePostStatus,
  updatePostTop
} from '../../api/community'

const loading = ref(false)
const total = ref(0)
const records = ref([])
const categoryOptions = ref([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const postDetail = ref(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: null,
  status: null,
  isTop: null
})

const fetchCategoryOptions = async () => {
  categoryOptions.value = await getAdminCategoryList()
}

const fetchList = async () => {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((key) => {
      if (params[key] === '' || params[key] === null || params[key] === undefined) {
        delete params[key]
      }
    })
    const res = await getAdminPostPage(params)
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
  query.keyword = ''
  query.categoryId = null
  query.status = null
  query.isTop = null
  fetchList()
}

const onTopChange = async (row, value) => {
  await updatePostTop({
    postId: row.id,
    isTop: value ? 1 : 0
  })
  ElMessage.success('置顶状态已更新')
  fetchList()
}

const onStatusChange = async (row, value) => {
  await updatePostStatus({
    postId: row.id,
    status: value
  })
  ElMessage.success('帖子状态已更新')
  fetchList()
}

const onCategoryChange = async (row, value) => {
  await updateAdminPostCategory({
    postId: row.id,
    categoryId: value
  })
  ElMessage.success('帖子分类已更新')
  fetchList()
}

const showDetail = async (row) => {
  detailLoading.value = true
  detailVisible.value = true
  try {
    postDetail.value = await getAdminPostDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

const removePost = async (row) => {
  await ElMessageBox.confirm(`确认删除帖子【${row.title}】吗？`, '提示', { type: 'warning' })
  await deleteAdminPost(row.id)
  ElMessage.success('帖子已删除')
  fetchList()
}

const removeReply = async (row) => {
  await deleteAdminReply(row.id)
  ElMessage.success('回复已删除')
  // 刷新帖子详情中的回复列表
  if (postDetail.value?.post?.id) {
    postDetail.value = await getAdminPostDetail(postDetail.value.post.id)
  }
}

onMounted(async () => {
  await fetchCategoryOptions()
  await fetchList()
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
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}

.detail-title {
  margin-bottom: 10px;
  font-weight: 600;
  color: var(--text-primary);
}

.detail-content {
  white-space: pre-wrap;
  color: var(--text-primary);
  line-height: 1.6;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.media-item {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all 0.2s ease;
}

.media-item:hover {
  box-shadow: var(--shadow-md);
}

.media-preview {
  width: 100%;
  height: 160px;
  object-fit: cover;
  display: block;
}
</style>
