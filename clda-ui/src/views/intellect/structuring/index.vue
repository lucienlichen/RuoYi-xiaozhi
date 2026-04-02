<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="分类编码" prop="categoryCode">
        <el-input v-model="queryParams.categoryCode" placeholder="请输入分类编码" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="enabled">
        <el-select v-model="queryParams.enabled" placeholder="请选择" clearable>
          <el-option label="启用" value="1" />
          <el-option label="禁用" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['intellect:structuring:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="templateList">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="分类编码" prop="categoryCode" width="140" />
      <el-table-column label="模板名称" prop="templateName" width="200" />
      <el-table-column label="LLM Prompt" prop="llmPrompt" show-overflow-tooltip />
      <el-table-column label="字段数" width="80" align="center">
        <template #default="{ row }">
          {{ parseFieldCount(row.fieldSchema) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="enabled" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled === '1' ? 'success' : 'danger'" size="small">
            {{ row.enabled === '1' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(row)" v-hasPermi="['intellect:structuring:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)" v-hasPermi="['intellect:structuring:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="700px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="分类编码" prop="categoryCode">
          <el-select v-model="form.categoryCode" placeholder="请选择分类">
            <el-option v-for="cat in categoryOptions" :key="cat.categoryCode" :label="cat.categoryName" :value="cat.categoryCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="LLM Prompt" prop="llmPrompt">
          <el-input v-model="form.llmPrompt" type="textarea" :rows="4" placeholder="请输入LLM提取提示词" />
        </el-form-item>
        <el-form-item label="字段定义" prop="fieldSchema">
          <el-input v-model="form.fieldSchema" type="textarea" :rows="6" placeholder='JSON数组: [{"key":"fieldName","label":"字段名","type":"string","required":true}]' />
        </el-form-item>
        <el-form-item label="规则配置" prop="ruleConfig">
          <el-input v-model="form.ruleConfig" type="textarea" :rows="3" placeholder="可选: 正则/关键词规则JSON" />
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio value="1">启用</el-radio>
            <el-radio value="0">禁用</el-radio>
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
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { listTemplates, getTemplate, addTemplate, updateTemplate, delTemplate } from '@/api/intellect/structuring'
import { listCategories } from '@/api/intellect/equipdata'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const templateList = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const categoryOptions = ref([])

const queryParams = reactive({ pageNum: 1, pageSize: 10, categoryCode: undefined, enabled: undefined })
const form = ref({})
const rules = {
  categoryCode: [{ required: true, message: '请选择分类', trigger: 'change' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  llmPrompt: [{ required: true, message: '请输入LLM Prompt', trigger: 'blur' }],
}

onMounted(() => {
  getList()
  loadCategories()
})

async function getList() {
  loading.value = true
  try {
    const res = await listTemplates(queryParams)
    templateList.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  const res = await listCategories()
  categoryOptions.value = (res.data || []).filter(c => String(c.parentId) === '0')
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { queryParams.categoryCode = undefined; queryParams.enabled = undefined; handleQuery() }

function resetForm() {
  form.value = { categoryCode: undefined, templateName: '', llmPrompt: '', fieldSchema: '', ruleConfig: '', enabled: '1' }
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增结构化模板'
  dialogVisible.value = true
}

async function handleUpdate(row) {
  resetForm()
  const res = await getTemplate(row.id)
  form.value = res.data
  dialogTitle.value = '修改结构化模板'
  dialogVisible.value = true
}

function submitForm() {
  proxy.$refs.formRef.validate(async (valid) => {
    if (!valid) return
    if (form.value.id) {
      await updateTemplate(form.value)
      proxy.$modal.msgSuccess('修改成功')
    } else {
      await addTemplate(form.value)
      proxy.$modal.msgSuccess('新增成功')
    }
    dialogVisible.value = false
    getList()
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('确认删除模板"' + row.templateName + '"？').then(() => delTemplate(row.id)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function parseFieldCount(schema) {
  try { return JSON.parse(schema)?.length || 0 } catch { return 0 }
}
</script>
