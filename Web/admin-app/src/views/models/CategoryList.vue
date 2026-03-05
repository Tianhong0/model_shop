<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-input v-model="queryParams.categoryName" placeholder="搜索分类名称..." style="width: 200px" clearable @clear="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="queryParams.status" placeholder="所有状态" clearable style="width: 120px" @change="handleSearch">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
        </el-space>
        <el-button type="primary" icon="Plus" @click="handleAddMain">新增主分类</el-button>
      </div>

      <div class="table-wrapper">
        <el-table :data="categoryData" stripe border highlight-current-row style="width: 100%" v-loading="loading" row-key="id" :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">
          <el-table-column prop="id" label="分类ID" width="100" />
          <el-table-column prop="categoryName" label="分类名称" min-width="150" />
          <el-table-column prop="categoryCode" label="分类编码" width="120" />
          <el-table-column prop="parentName" label="上级分类" width="150">
            <template #default="scope">
              {{ scope.row.parentName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="sortNo" label="排序" width="80" align="center" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-switch
                v-model="scope.row.status"
                :active-value="1"
                :inactive-value="0"
                active-text="启用"
                inactive-text="禁用"
                inline-prompt
                @change="handleStatusChange(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="scope">
              <el-button link type="success" @click="handleAddChild(scope.row)">新增子类</el-button>
              <el-divider direction="vertical" />
              <el-button link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
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
          :page-sizes="[10, 20, 50, 100]"
          :hide-on-single-page="false"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 分类编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="分类名称" required>
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类编码" required>
          <el-input v-model="form.categoryCode" placeholder="请输入分类编码，如: anime" />
        </el-form-item>
        <el-form-item label="上级分类" v-if="dialogType === 'add-main'">
          <span style="color: #909399;">主分类无上级分类</span>
        </el-form-item>
        <el-form-item label="上级分类" v-else-if="dialogType === 'add-child'">
          <span style="color: #606266;">{{ parentCategoryName }}</span>
        </el-form-item>
        <el-form-item label="排序编号">
          <el-input-number v-model="form.sortNo" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModelCategories, createCategory, updateCategory, deleteCategory } from '../../api/model'

const loading = ref(false)
const categoryData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogType = ref('add-main')
const parentCategoryName = ref('')

const queryParams = reactive({
  categoryName: '',
  parentId: null,
  status: null,
  pageNum: 1,
  pageSize: 10
})

const form = reactive({
  id: null,
  categoryName: '',
  categoryCode: '',
  parentId: 0,
  sortNo: 0,
  status: 1
})

// 弹窗标题
const dialogTitle = computed(() => {
  switch (dialogType.value) {
    case 'add-main':
      return '新增主分类'
    case 'add-child':
      return '新增子分类'
    case 'edit':
      return '编辑分类'
    default:
      return '分类'
  }
})

// 构建树形选择器数据（不包括自己和子节点）
const categoryTreeOptions = computed(() => {
  const buildTree = (items, parentId = 0) => {
    return items
      .filter(item => item.parentId === parentId)
      .map(item => ({
        ...item,
        children: buildTree(items, item.id)
      }))
  }
  return buildTree(categoryData.value)
})

// 获取分类列表
const fetchCategories = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    // 移除空值
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) {
        delete params[key]
      }
    })
    const response = await getModelCategories(params)
    categoryData.value = response?.records || []
    total.value = Number(response?.total || 0)
    queryParams.pageNum = Number(response?.current || queryParams.pageNum)
    queryParams.pageSize = Number(response?.size || queryParams.pageSize)
  } catch (error) {
    console.error('获取分类列表失败:', error)
    categoryData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  fetchCategories()
}

const handlePageChange = (pageNum) => {
  queryParams.pageNum = pageNum
  fetchCategories()
}

const handleSizeChange = (pageSize) => {
  queryParams.pageSize = pageSize
  queryParams.pageNum = 1
  fetchCategories()
}

// 新增主分类
const handleAddMain = () => {
  dialogType.value = 'add-main'
  parentCategoryName.value = ''
  Object.assign(form, {
    id: null,
    categoryName: '',
    categoryCode: '',
    parentId: 0,
    sortNo: 0,
    status: 1
  })
  dialogVisible.value = true
}

// 新增子分类
const handleAddChild = (row) => {
  dialogType.value = 'add-child'
  parentCategoryName.value = row.categoryName
  Object.assign(form, {
    id: null,
    categoryName: '',
    categoryCode: '',
    parentId: row.id,
    sortNo: 0,
    status: 1
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  dialogType.value = 'edit'
  Object.assign(form, {
    id: row.id,
    categoryName: row.categoryName,
    categoryCode: row.categoryCode,
    parentId: row.parentId || 0,
    sortNo: row.sortNo || 0,
    status: row.status
  })
  dialogVisible.value = true
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除分类 ${row.categoryName} 吗？如果该分类下有模型或子分类，将无法删除。`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteCategory(row.id)
      ElMessage.success('删除成功')
      fetchCategories()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  })
}

// 状态切换
const handleStatusChange = async (row) => {
  try {
    await updateCategory({
      id: row.id,
      categoryName: row.categoryName,
      categoryCode: row.categoryCode,
      parentId: row.parentId || 0,
      sortNo: row.sortNo || 0,
      status: row.status
    })
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } catch (error) {
    console.error('更新状态失败:', error)
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('操作失败')
  }
}

// 提交表单
const submitForm = async () => {
  if (!form.categoryName || !form.categoryCode) {
    ElMessage.warning('请填写分类名称和编码')
    return
  }

  try {
    if (dialogType.value === 'add-main' || dialogType.value === 'add-child') {
      await createCategory(form)
      ElMessage.success(dialogType.value === 'add-main' ? '新增主分类成功' : '新增子分类成功')
    } else {
      await updateCategory(form)
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    fetchCategories()
  } catch (error) {
    console.error('操作失败:', error)
    const typeMap = {
      'add-main': '新增主分类',
      'add-child': '新增子分类',
      'edit': '更新'
    }
    ElMessage.error(`${typeMap[dialogType.value]}失败`)
  }
}

onMounted(() => {
  fetchCategories()
})
</script>

<style scoped>
.page-container { padding: 0; }
.table-card {
  background: #fff;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
}
.header-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}
.table-wrapper {
  max-height: calc(100vh - 300px);
  overflow: auto;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  overflow-x: auto;
  position: sticky;
  bottom: 0;
  background: #fff;
  z-index: 5;
  padding-top: 8px;
}
</style>
