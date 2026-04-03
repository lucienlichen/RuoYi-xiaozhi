<template>
  <div class="app-container">
    <!-- 筛选 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="4">
        <el-select v-model="queryParams.category" placeholder="大类筛选" clearable @change="handleQuery">
          <el-option label="重大隐患" value="重大隐患" />
          <el-option label="其他隐患" value="其他隐患" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-select v-model="queryParams.subCategory" placeholder="子类筛选" clearable @change="handleQuery">
          <el-option v-for="s in subCategories" :key="s" :label="s" :value="s" />
        </el-select>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['crane:inspection:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="ids.length === 0" @click="handleDelete" v-hasPermi="['crane:inspection:remove']">删除</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="itemList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="itemNo" label="序号" width="60" />
      <el-table-column prop="category" label="大类" width="100" />
      <el-table-column prop="subCategory" label="排查项目" width="160" />
      <el-table-column prop="content" label="排查内容" min-width="300" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleEdit(scope.row)" v-hasPermi="['crane:inspection:edit']">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDeleteOne(scope.row)" v-hasPermi="['crane:inspection:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="loadList" />

    <!-- 编辑对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="大类" prop="category">
          <el-select v-model="form.category" placeholder="选择大类">
            <el-option label="重大隐患" value="重大隐患" />
            <el-option label="其他隐患" value="其他隐患" />
          </el-select>
        </el-form-item>
        <el-form-item label="排查项目" prop="subCategory">
          <el-input v-model="form.subCategory" placeholder="如：检验相关、安全装置" />
        </el-form-item>
        <el-form-item label="序号" prop="itemNo">
          <el-input-number v-model="form.itemNo" :min="1" />
        </el-form-item>
        <el-form-item label="排查内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="dialogOpen = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listInspectionItems, addInspectionItem, updateInspectionItem, deleteInspectionItem } from '@/api/intellect/inspection'

const itemList = ref([])
const loading = ref(false)
const total = ref(0)
const ids = ref([])
const queryParams = ref({ pageNum: 1, pageSize: 20, category: '', subCategory: '' })

const dialogOpen = ref(false)
const dialogTitle = ref('')
const form = ref({})
const formRef = ref(null)
const rules = {
  category: [{ required: true, message: '请选择大类', trigger: 'change' }],
  subCategory: [{ required: true, message: '请输入排查项目', trigger: 'blur' }],
  content: [{ required: true, message: '请输入排查内容', trigger: 'blur' }],
  itemNo: [{ required: true, message: '请输入序号', trigger: 'blur' }]
}

const subCategories = computed(() => {
  const set = new Set(itemList.value.map(i => i.subCategory))
  return [...set]
})

function handleQuery() {
  queryParams.value.pageNum = 1
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const res = await listInspectionItems(queryParams.value)
    itemList.value = res.rows || []
    total.value = res.total || 0
  } catch { itemList.value = [] }
  loading.value = false
}

function handleSelectionChange(sel) { ids.value = sel.map(r => r.id) }

function handleAdd() {
  form.value = { category: '', subCategory: '', itemNo: (total.value || 0) + 1, content: '', orderNum: (total.value || 0) + 1 }
  dialogTitle.value = '新增检查项'
  dialogOpen.value = true
}

function handleEdit(row) {
  form.value = { ...row }
  dialogTitle.value = '编辑检查项'
  dialogOpen.value = true
}

async function submitForm() {
  await formRef.value.validate()
  if (form.value.id) {
    await updateInspectionItem(form.value)
  } else {
    await addInspectionItem(form.value)
  }
  ElMessage.success('操作成功')
  dialogOpen.value = false
  loadList()
}

async function handleDelete() {
  await ElMessageBox.confirm(`确认删除选中的 ${ids.value.length} 条记录？`, '提示', { type: 'warning' })
  await deleteInspectionItem(ids.value.join(','))
  ElMessage.success('删除成功')
  loadList()
}

async function handleDeleteOne(row) {
  await ElMessageBox.confirm(`确认删除序号 ${row.itemNo} 的检查项？`, '提示', { type: 'warning' })
  await deleteInspectionItem(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(() => loadList())
</script>
