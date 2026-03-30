<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="query.keyword" clearable placeholder="标题/描述关键词" style="width: 240px" />
          <el-input-number v-model="query.sellerId" :min="1" controls-position="right" placeholder="卖家ID" style="width: 160px" />
          <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="草稿" :value="0" />
            <el-option label="在售" :value="1" />
            <el-option label="已下架" :value="2" />
            <el-option label="已成交" :value="3" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-space>
      </div>

      <el-table v-loading="loading" :data="records" stripe border highlight-current-row>
        <el-table-column prop="id" label="商品ID" width="150" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sellerNickname" label="卖家" width="140" />
        <el-table-column label="售价" width="120">
          <template #default="scope">￥{{ Number(scope.row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="conditionLevel" label="成色" width="120" />
        <el-table-column prop="location" label="所在地" width="140" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="showDetail(scope.row)">详情</el-button>
            <el-dropdown @command="(status) => changeStatus(scope.row, status)">
              <el-button link type="primary">改状态</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="1">上架</el-dropdown-item>
                  <el-dropdown-item :command="2">下架</el-dropdown-item>
                  <el-dropdown-item :command="3">标记成交</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          :page-count="Math.max(1, Math.ceil((total || 0) / query.pageSize))"
          :hide-on-single-page="false"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="二手商品详情" width="900px">
      <div v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商品ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="卖家">{{ detail.sellerNickname }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
          <el-descriptions-item label="售价">￥{{ Number(detail.price || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="原价">{{ detail.originalPrice ? `￥${Number(detail.originalPrice).toFixed(2)}` : '-' }}</el-descriptions-item>
          <el-descriptions-item label="成色">{{ detail.conditionLevel }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.categoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所在地">{{ detail.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ detail.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-divider />
        <div class="detail-title">图片列表</div>
        <div class="media-grid">
          <el-image
            v-for="(item, index) in detail.imageUrls || []"
            :key="index"
            :src="item"
            fit="cover"
            :preview-src-list="detail.imageUrls || []"
            preview-teleported
            class="media-item"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminUsedListingDetail, getAdminUsedListingPage, updateAdminUsedListingStatus } from '../../api/used'

const loading = ref(false)
const total = ref(0)
const records = ref([])
const detailVisible = ref(false)
const detail = ref(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  sellerId: null,
  status: null
})

const statusText = (status) => ({ 0: '草稿', 1: '在售', 2: '已下架', 3: '已成交' }[Number(status)] || '未知')
const statusType = (status) => ({ 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }[Number(status)] || '')

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getAdminUsedListingPage({ ...query })
    records.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '获取二手商品失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', sellerId: null, status: null })
  fetchList()
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

const showDetail = async (row) => {
  detail.value = await getAdminUsedListingDetail(row.id)
  detailVisible.value = true
}

const changeStatus = async (row, status) => {
  try {
    await updateAdminUsedListingStatus({ listingId: row.id, status })
    ElMessage.success('状态更新成功')
    fetchList()
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
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

.detail-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 14px;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.media-item {
  width: 100%;
  height: 140px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
}
</style>
