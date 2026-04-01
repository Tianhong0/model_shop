<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="queryParams.roleName" placeholder="搜索角色名称..." style="width: 200px" clearable @clear="handleSearch">
            <template #prefix><el-icon><search /></el-icon></template>
          </el-input>
          <el-select v-model="queryParams.status" placeholder="所有状态" clearable style="width: 120px" @change="handleSearch">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button type="success" icon="Plus" @click="handleCreate">新建角色</el-button>
        </el-space>
      </div>

      <div class="table-wrapper">
        <el-table :data="roleData" stripe border highlight-current-row style="width: 100%" v-loading="loading">
          <el-table-column prop="id" label="ID" width="100" />
          <el-table-column prop="roleName" label="角色名称" />
          <el-table-column prop="roleDesc" label="角色描述" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="light" round>
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
              <el-divider direction="vertical" />
              <el-button link type="warning" @click="handlePermissions(scope.row)">权限</el-button>
              <el-divider direction="vertical" />
              <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-count="Math.max(1, Math.ceil((total || 0) / queryParams.pageSize))"
          :page-sizes="[10, 20, 50, 100]"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 新建/编辑角色弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新建角色'" width="500px">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="如：ROLE_MANAGER" />
        </el-form-item>
        <el-form-item label="角色描述" prop="roleDesc">
          <el-input v-model="formData.roleDesc" type="textarea" :rows="3" placeholder="角色描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="isEdit">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置弹窗 -->
    <el-dialog v-model="permissionVisible" title="配置权限" width="600px">
      <el-tree
        ref="permissionTreeRef"
        :data="permissionTree"
        :props="{ label: 'permissionName', children: 'children' }"
        show-checkbox
        node-key="id"
        default-expand-all
        :check-strictly="false"
      />
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermissions" :loading="permissionLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRolePage, createRole, updateRole, deleteRole, getAllPermissions, getRolePermissionIds, updateRole as updateRoleApi } from '../../api/role'

const loading = ref(false)
const roleData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const permissionVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const permissionLoading = ref(false)
const formRef = ref(null)
const permissionTree = ref([])
const permissionTreeRef = ref(null)
const currentRoleId = ref(null)

const queryParams = reactive({
  roleName: '',
  status: null,
  pageNum: 1,
  pageSize: 10
})

const formData = reactive({
  id: null,
  roleName: '',
  roleDesc: '',
  status: 1
})

const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

// 获取角色列表
const fetchRoleList = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) {
        delete params[key]
      }
    })
    const response = await getRolePage(params)
    roleData.value = response?.records || []
    total.value = response?.total || 0
  } catch (error) {
    console.error('获取角色列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  fetchRoleList()
}

// 页码变化
const handlePageChange = (page) => {
  queryParams.pageNum = page
  fetchRoleList()
}

// 每页大小变化
const handleSizeChange = (size) => {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  fetchRoleList()
}

// 新建角色
const handleCreate = () => {
  isEdit.value = false
  formData.id = null
  formData.roleName = ''
  formData.roleDesc = ''
  formData.status = 1
  dialogVisible.value = true
}

// 编辑角色
const handleEdit = (row) => {
  isEdit.value = true
  formData.id = row.id
  formData.roleName = row.roleName
  formData.roleDesc = row.roleDesc
  formData.status = row.status
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await updateRole(formData)
        ElMessage.success('更新成功')
      } else {
        await createRole(formData)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchRoleList()
    } catch (error) {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 删除角色
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除角色 ${row.roleName} 吗？`, '警告', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteRole(row.id)
      ElMessage.success('删除成功')
      fetchRoleList()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 配置权限
const handlePermissions = async (row) => {
  currentRoleId.value = row.id
  // 加载权限树
  if (permissionTree.value.length === 0) {
    try {
      const response = await getAllPermissions()
      permissionTree.value = response || []
    } catch (error) {
      console.error('获取权限树失败:', error)
    }
  }
  // 加载角色当前权限
  try {
    const permissionIds = await getRolePermissionIds(row.id)
    permissionTreeRef.value.setCheckedKeys(permissionIds || [])
  } catch (error) {
    console.error('获取角色权限失败:', error)
  }
  permissionVisible.value = true
}

// 保存权限
const handleSavePermissions = async () => {
  permissionLoading.value = true
  try {
    const permissionIds = permissionTreeRef.value.getCheckedKeys()
    await updateRole({
      id: currentRoleId.value,
      permissionIds: permissionIds
    })
    ElMessage.success('权限保存成功')
    permissionVisible.value = false
  } catch (error) {
    console.error('保存权限失败:', error)
    ElMessage.error('保存权限失败')
  } finally {
    permissionLoading.value = false
  }
}

onMounted(() => {
  fetchRoleList()
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
}

.table-wrapper {
  overflow-x: auto;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}
</style>
