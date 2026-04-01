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
          <el-select v-model="queryParams.status" placeholder="所有状态" clearable style="width: 120px" @change="handleSearch">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
        </el-space>
      </div>

      <div class="table-wrapper">
        <el-table :data="adminData" stripe border highlight-current-row style="width: 100%" v-loading="loading">
          <el-table-column prop="id" label="UID" width="100" />
          <el-table-column label="头像" width="80">
            <template #default="scope">
              <el-avatar :size="40" :src="scope.row.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + scope.row.id" />
            </template>
          </el-table-column>
          <el-table-column prop="userName" label="用户名" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column label="角色" min-width="180">
            <template #default="scope">
              <el-tag v-for="name in scope.row.roleNames" :key="name"
                      :type="name === 'ROLE_ADMIN' ? 'danger' : 'primary'"
                      effect="plain"
                      style="margin-right: 4px">
                {{ formatRoleName(name) }}
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
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleViewDetail(scope.row)">详情</el-button>
              <el-divider direction="vertical" />
              <el-button link type="warning" @click="handleAssignRole(scope.row)">分配角色</el-button>
              <el-divider direction="vertical" />
              <el-button link :type="scope.row.status === 1 ? 'danger' : 'success'" @click="handleEditStatus(scope.row)">
                {{ scope.row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-divider direction="vertical" />
              <el-button link type="info" @click="handleViewLogs(scope.row)">操作日志</el-button>
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
    <el-dialog v-model="detailVisible" title="管理员详情" width="600px">
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.userName }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.mobile || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentUser.status === 1 ? 'success' : 'info'" effect="light" round>
            {{ currentUser.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="角色" :span="2">
          <el-tag v-for="name in currentUser.roleNames" :key="name"
                  :type="name === 'ROLE_ADMIN' ? 'danger' : 'primary'"
                  style="margin-right: 4px">
            {{ formatRoleName(name) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentUser.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 角色分配弹窗 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="500px">
      <div style="margin-bottom: 16px;">
        <span style="color: var(--text-secondary);">为用户 <b>{{ currentUser?.userName }}</b> 分配角色：</span>
      </div>
      <el-checkbox-group v-model="selectedRoleIds">
        <div v-for="role in enabledRoles" :key="role.id" style="margin-bottom: 12px;">
          <el-checkbox :label="role.id">
            <span>{{ role.roleName?.replace('ROLE_', '') }}</span>
            <span style="color: var(--text-secondary); margin-left: 8px; font-size: 12px;">{{ role.roleDesc }}</span>
          </el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRoles" :loading="roleSaving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 操作日志弹窗 -->
    <el-dialog v-model="logDialogVisible" title="操作日志" width="900px">
      <el-table :data="operationLogs" max-height="400" v-loading="logLoading">
        <el-table-column prop="operationType" label="操作类型" width="100" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="logQueryParams.pageNum"
          v-model:page-size="logQueryParams.pageSize"
          layout="total, prev, pager, next"
          :total="logTotal"
          :page-sizes="[10, 20]"
          background
          small
          @current-change="fetchOperationLogs"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, updateUserStatus } from '../../api/user'
import { getAllEnabledRoles, getUserRoleIds, assignUserRoles } from '../../api/role'
import { getOperationLogPage } from '../../api/operationLog'

const loading = ref(false)
const adminData = ref([])
const total = ref(0)
const detailVisible = ref(false)
const currentUser = ref(null)

const queryParams = reactive({
  userName: '',
  nickname: '',
  status: null,
  isAdmin: true,
  page: 1,
  size: 10
})

// 角色分配相关
const roleDialogVisible = ref(false)
const enabledRoles = ref([])
const selectedRoleIds = ref([])
const roleSaving = ref(false)

// 操作日志相关
const logDialogVisible = ref(false)
const operationLogs = ref([])
const logLoading = ref(false)
const logTotal = ref(0)
const logQueryParams = reactive({
  operatorId: null,
  pageNum: 1,
  pageSize: 10
})

// 角色名称格式化
const formatRoleName = (name) => {
  if (!name) return ''
  return name.replace('ROLE_', '')
}

// 获取管理员列表
const fetchAdminList = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) {
        delete params[key]
      }
    })
    const response = await getUserList(params)
    adminData.value = response?.records || []
    total.value = response?.total || 0
    queryParams.page = response?.pageNum || queryParams.page
    queryParams.size = response?.pageSize || queryParams.size
  } catch (error) {
    console.error('获取管理员列表失败:', error)
    adminData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  queryParams.page = 1
  fetchAdminList()
}

// 页码变化
const handlePageChange = (page) => {
  queryParams.page = page
  fetchAdminList()
}

// 每页大小变化
const handleSizeChange = (size) => {
  queryParams.size = size
  queryParams.page = 1
  fetchAdminList()
}

// 查看详情
const handleViewDetail = (row) => {
  currentUser.value = row
  detailVisible.value = true
}

// 修改状态
const handleEditStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '启用' : '禁用'

  ElMessageBox.confirm(`确定要${statusText}管理员 ${row.userName} 吗？`, '警告', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await updateUserStatus(row.id, newStatus)
      ElMessage.success(`${statusText}成功`)
      fetchAdminList()
    } catch (error) {
      console.error('修改状态失败:', error)
      ElMessage.error(`${statusText}失败`)
    }
  }).catch(() => {})
}

// 分配角色
const handleAssignRole = async (row) => {
  currentUser.value = row
  selectedRoleIds.value = []

  // 获取可用角色列表
  try {
    enabledRoles.value = await getAllEnabledRoles()
  } catch (error) {
    console.error('获取角色列表失败:', error)
    enabledRoles.value = []
  }

  // 获取用户当前角色
  try {
    const roleIds = await getUserRoleIds(row.id)
    selectedRoleIds.value = roleIds || []
  } catch (error) {
    console.error('获取用户角色失败:', error)
  }

  roleDialogVisible.value = true
}

// 保存角色分配
const handleSaveRoles = async () => {
  roleSaving.value = true
  try {
    await assignUserRoles({
      userId: currentUser.value.id,
      roleIds: selectedRoleIds.value
    })
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    fetchAdminList()
  } catch (error) {
    console.error('角色分配失败:', error)
    ElMessage.error('角色分配失败')
  } finally {
    roleSaving.value = false
  }
}

// 查看操作日志
const handleViewLogs = (row) => {
  currentUser.value = row
  logQueryParams.operatorId = row.id
  logQueryParams.pageNum = 1
  logDialogVisible.value = true
  fetchOperationLogs()
}

// 获取操作日志
const fetchOperationLogs = async () => {
  logLoading.value = true
  try {
    const response = await getOperationLogPage({
      operatorId: logQueryParams.operatorId,
      pageNum: logQueryParams.pageNum,
      pageSize: logQueryParams.pageSize
    })
    operationLogs.value = response?.records || []
    logTotal.value = response?.total || 0
  } catch (error) {
    console.error('获取操作日志失败:', error)
    operationLogs.value = []
  } finally {
    logLoading.value = false
  }
}

onMounted(() => {
  fetchAdminList()
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
