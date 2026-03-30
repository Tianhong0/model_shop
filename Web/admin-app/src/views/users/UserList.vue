<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="queryParams.userName" placeholder="搜索用户名..." style="width: 200px" clearable @clear="handleSearch">
            <template #prefix><el-icon><search /></el-icon></template>
          </el-input>
          <el-input v-model="queryParams.nickname" placeholder="搜索昵称..." style="width: 200px" clearable @clear="handleSearch">
            <template #prefix><el-icon><search /></el-icon></template>
          </el-input>
          <el-input v-model="queryParams.mobile" placeholder="搜索手机号..." style="width: 180px" clearable @clear="handleSearch">
            <template #prefix><el-icon><search /></el-icon></template>
          </el-input>
          <el-select v-model="queryParams.status" placeholder="所有状态" clearable style="width: 120px" @change="handleSearch">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-select v-model="queryParams.sex" placeholder="所有性别" clearable style="width: 120px" @change="handleSearch">
            <el-option label="男" :value="1" />
            <el-option label="女" :value="0" />
          </el-select>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
        </el-space>
      </div>

      <div class="table-wrapper">
        <el-table :data="userData" stripe border highlight-current-row style="width: 100%" v-loading="loading">
          <el-table-column prop="id" label="UID" width="100" />
          <el-table-column label="头像" width="80">
            <template #default="scope">
              <el-avatar :size="40" :src="scope.row.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + scope.row.id" />
            </template>
          </el-table-column>
          <el-table-column prop="userName" label="用户名" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column prop="mobile" label="手机号" />
          <el-table-column prop="sex" label="性别" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.sex === 1 ? 'primary' : 'danger'" effect="plain" size="small">
                {{ scope.row.sex === 1 ? '男' : '女' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="light" round>
                {{ scope.row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="注册时间" width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleViewDetail(scope.row)">详情</el-button>
              <el-divider direction="vertical" />
              <el-button link type="warning" @click="handleEditStatus(scope.row)">
                {{ scope.row.status === 1 ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-count="Math.max(1, Math.ceil((total || 0) / queryParams.size))"
          :page-sizes="[10, 20, 50, 100]"
          :hide-on-single-page="false"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="600px">
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.userName }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.mobile || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ currentUser.sex === 1 ? '男' : currentUser.sex === 0 ? '女' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentUser.status === 1 ? 'success' : 'info'" effect="light" round>
            {{ currentUser.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">{{ currentUser.createTime }}</el-descriptions-item>
        <el-descriptions-item label="头像" :span="2">
          <el-avatar :size="60" :src="currentUser.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + currentUser.id" />
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, getUserDetail, updateUserStatus } from '../../api/user'

const loading = ref(false)
const userData = ref([])
const total = ref(0)
const detailVisible = ref(false)
const currentUser = ref(null)

const queryParams = reactive({
  userName: '',
  nickname: '',
  mobile: '',
  status: null,
  sex: null,
  page: 1,
  size: 10
})

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    // 移除空值
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) {
        delete params[key]
      }
    })
    const response = await getUserList(params)
    userData.value = response?.records || []
    total.value = response?.total || 0
    queryParams.page = response?.pageNum || queryParams.page
    queryParams.size = response?.pageSize || queryParams.size
  } catch (error) {
    console.error('获取用户列表失败:', error)
    userData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  queryParams.page = 1
  fetchUserList()
}

// 页码变化
const handlePageChange = (page) => {
  queryParams.page = page
  fetchUserList()
}

// 每页大小变化
const handleSizeChange = (size) => {
  queryParams.size = size
  queryParams.page = 1
  fetchUserList()
}

// 查看详情
const handleViewDetail = async (row) => {
  try {
    const response = await getUserDetail(row.id)
    currentUser.value = response.data || row
    detailVisible.value = true
  } catch (error) {
    console.error('获取用户详情失败:', error)
    currentUser.value = row
    detailVisible.value = true
  }
}

// 修改状态
const handleEditStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '启用' : '禁用'

  ElMessageBox.confirm(`确定要${statusText}用户 ${row.userName} 吗？`, '警告', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await updateUserStatus(row.id, newStatus)
      ElMessage.success(`${statusText}成功`)
      fetchUserList()
    } catch (error) {
      console.error('修改状态失败:', error)
      ElMessage.error(`${statusText}失败`)
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchUserList()
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
  display: flex;
  flex-direction: column;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}

.header-actions :deep(.el-input__wrapper) {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
}

.header-actions :deep(.el-input__wrapper:hover) {
  border-color: var(--border-dark);
}

.header-actions :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px var(--primary-lighter);
}

.header-actions :deep(.el-select .el-input__wrapper) {
  background: var(--bg-secondary);
}

.table-wrapper {
  overflow-x: auto;
  margin: 0 -4px;
  padding: 0 4px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}

/* 对话框内的描述列表样式 */
:deep(.el-descriptions) {
  border-radius: var(--radius-md);
  overflow: hidden;
}

:deep(.el-descriptions__label) {
  font-weight: 500;
  background: var(--bg-secondary) !important;
  color: var(--text-secondary);
}

:deep(.el-descriptions__content) {
  color: var(--text-primary);
}
</style>
