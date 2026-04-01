<template>
  <div class="page-container">
    <div class="table-card">
      <div class="header-actions">
        <el-space>
          <el-select v-model="currentGroup" placeholder="选择配置分组" clearable style="width: 180px" @change="handleGroupChange">
            <el-option label="全部" value="" />
            <el-option label="系统配置" value="SYSTEM" />
            <el-option label="订单配置" value="ORDER" />
            <el-option label="物流配置" value="DELIVERY" />
            <el-option label="财务配置" value="FINANCE" />
            <el-option label="支付配置" value="PAYMENT" />
            <el-option label="存储配置" value="STORAGE" />
            <el-option label="积分配置" value="POINT" />
            <el-option label="悬赏配置" value="BOUNTY" />
            <el-option label="水印配置" value="WATERMARK" />
          </el-select>
          <el-button type="primary" icon="Refresh" @click="fetchConfigs">刷新</el-button>
          <el-button type="success" icon="Plus" @click="handleCreate">新增配置</el-button>
        </el-space>
      </div>

      <div class="table-wrapper">
        <el-table :data="configData" stripe border highlight-current-row style="width: 100%" v-loading="loading">
          <el-table-column prop="configKey" label="配置键" width="200" />
          <el-table-column prop="configValue" label="配置值" min-width="200">
            <template #default="scope">
              <el-text v-if="scope.row.configValue && scope.row.configValue.length > 50" truncated>
                {{ scope.row.configValue }}
              </el-text>
              <span v-else>{{ scope.row.configValue }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="configType" label="类型" width="100">
            <template #default="scope">
              <el-tag size="small">{{ scope.row.configType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="configGroup" label="分组" width="100" />
          <el-table-column prop="description" label="说明" min-width="150" />
          <el-table-column prop="isPublic" label="公开" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.isPublic === 1 ? 'success' : 'info'" size="small">
                {{ scope.row.isPublic === 1 ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
              <el-divider direction="vertical" />
              <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 新建/编辑配置弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑配置' : '新增配置'" width="550px">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="formData.configKey" placeholder="如：system.name" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="formData.configValue" type="textarea" :rows="3" placeholder="配置值" />
        </el-form-item>
        <el-form-item label="配置类型" prop="configType">
          <el-select v-model="formData.configType" style="width: 100%">
            <el-option label="字符串" value="STRING" />
            <el-option label="数字" value="NUMBER" />
            <el-option label="布尔" value="BOOLEAN" />
            <el-option label="JSON" value="JSON" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置分组" prop="configGroup">
          <el-select v-model="formData.configGroup" style="width: 100%">
            <el-option label="系统配置" value="SYSTEM" />
            <el-option label="订单配置" value="ORDER" />
            <el-option label="物流配置" value="DELIVERY" />
            <el-option label="财务配置" value="FINANCE" />
            <el-option label="支付配置" value="PAYMENT" />
            <el-option label="存储配置" value="STORAGE" />
            <el-option label="积分配置" value="POINT" />
            <el-option label="悬赏配置" value="BOUNTY" />
            <el-option label="水印配置" value="WATERMARK" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置说明" prop="description">
          <el-input v-model="formData.description" placeholder="配置说明" />
        </el-form-item>
        <el-form-item label="是否公开" prop="isPublic">
          <el-switch v-model="formData.isPublic" :active-value="1" :inactive-value="0" />
          <span style="margin-left: 10px; color: var(--text-muted); font-size: 12px;">公开配置无需权限即可访问</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllConfigs, getConfigsByGroup, setConfig, deleteConfig } from '../../api/config'

const loading = ref(false)
const configData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const currentGroup = ref('')

const formData = reactive({
  configKey: '',
  configValue: '',
  configType: 'STRING',
  configGroup: 'SYSTEM',
  description: '',
  isPublic: 0
})

const formRules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }]
}

// 获取配置列表
const fetchConfigs = async () => {
  loading.value = true
  try {
    const response = currentGroup.value
      ? await getConfigsByGroup(currentGroup.value)
      : await getAllConfigs()
    configData.value = response || []
  } catch (error) {
    console.error('获取配置列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 分组变化
const handleGroupChange = () => {
  fetchConfigs()
}

// 新增配置
const handleCreate = () => {
  isEdit.value = false
  formData.configKey = ''
  formData.configValue = ''
  formData.configType = 'STRING'
  formData.configGroup = 'SYSTEM'
  formData.description = ''
  formData.isPublic = 0
  dialogVisible.value = true
}

// 编辑配置
const handleEdit = (row) => {
  isEdit.value = true
  formData.configKey = row.configKey
  formData.configValue = row.configValue
  formData.configType = row.configType
  formData.configGroup = row.configGroup
  formData.description = row.description
  formData.isPublic = row.isPublic
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      await setConfig(formData)
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      fetchConfigs()
    } catch (error) {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 删除配置
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除配置 ${row.configKey} 吗？`, '警告', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteConfig(row.configKey)
      ElMessage.success('删除成功')
      fetchConfigs()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchConfigs()
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
</style>
