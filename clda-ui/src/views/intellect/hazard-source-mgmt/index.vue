<template>
  <div class="app-container">
    <!-- 分类 tabs -->
    <div class="hz-tabs">
      <button
        v-for="cat in topCategories"
        :key="cat.id"
        class="hz-tab"
        :class="{ active: activeCatId === cat.id }"
        @click="switchCategory(cat.id)"
      >{{ cat.name }}</button>
    </div>

    <!-- 子分类选择 -->
    <el-row :gutter="10" class="mb8" style="margin-top:12px">
      <el-col :span="8">
        <el-select v-model="activeSubId" placeholder="选择子分类" @change="loadItems" clearable>
          <el-option-group v-for="sub in currentSubCategories" :key="sub.id" :label="sub.code + ' ' + sub.name">
            <el-option v-if="!sub.children?.length" :label="sub.name" :value="sub.id" />
            <el-option v-for="child in (sub.children || [])" :key="child.id" :label="child.code + ' ' + child.name" :value="child.id" />
          </el-option-group>
        </el-select>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['crane:hazard-source:add']">新增条目</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="ids.length === 0" @click="handleDelete" v-hasPermi="['crane:hazard-source:remove']">删除</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="itemList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="itemNo" label="序号" width="60" />
      <el-table-column prop="description" label="危险源描述" min-width="300" show-overflow-tooltip />
      <el-table-column prop="causeCodes" label="原因编码" width="200" show-overflow-tooltip />
      <el-table-column prop="eventCodes" label="事件编码" width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleEdit(scope.row)" v-hasPermi="['crane:hazard-source:edit']">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDeleteOne(scope.row)" v-hasPermi="['crane:hazard-source:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="loadItems" />

    <!-- 编辑对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择分类">
            <el-option-group v-for="sub in allSubCategories" :key="sub.id" :label="sub.code + ' ' + sub.name">
              <el-option v-if="!sub.children?.length" :label="sub.name" :value="sub.id" />
              <el-option v-for="child in (sub.children || [])" :key="child.id" :label="child.code + ' ' + child.name" :value="child.id" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="序号">
          <el-input-number v-model="form.itemNo" :min="1" />
        </el-form-item>
        <el-form-item label="危险源描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="原因编码">
          <el-input v-model="form.causeCodes" placeholder="逗号分隔，如 8.1.2a,8.1.3b" />
        </el-form-item>
        <el-form-item label="事件编码">
          <el-input v-model="form.eventCodes" placeholder="逗号分隔，如 6a,6c,6j" />
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
import { getHazardSourceTree, listHazardSourceItems, addHazardSourceItem, updateHazardSourceItem, deleteHazardSourceItem } from '@/api/intellect/hazardSource'

const treeData = ref([])
const activeCatId = ref(null)
const activeSubId = ref(null)
const itemList = ref([])
const loading = ref(false)
const total = ref(0)
const ids = ref([])
const queryParams = ref({ pageNum: 1, pageSize: 20 })

const dialogOpen = ref(false)
const dialogTitle = ref('')
const form = ref({})
const formRef = ref(null)
const rules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }]
}

const topCategories = computed(() => treeData.value)

const currentSubCategories = computed(() => {
  const cat = treeData.value.find(c => c.id === activeCatId.value)
  return cat?.children || []
})

const allSubCategories = computed(() => {
  const result = []
  treeData.value.forEach(cat => { if (cat.children) result.push(...cat.children) })
  return result
})

function switchCategory(id) {
  activeCatId.value = id
  activeSubId.value = null
  itemList.value = []
  total.value = 0
}

async function loadItems() {
  if (!activeSubId.value) return
  loading.value = true
  try {
    const res = await listHazardSourceItems({ categoryId: activeSubId.value, ...queryParams.value })
    itemList.value = res.rows || []
    total.value = res.total || 0
  } catch { itemList.value = [] }
  loading.value = false
}

function handleSelectionChange(sel) { ids.value = sel.map(r => r.id) }

function handleAdd() {
  form.value = { categoryId: activeSubId.value, itemNo: (itemList.value.length || 0) + 1, description: '', causeCodes: '', eventCodes: '' }
  dialogTitle.value = '新增危险源条目'
  dialogOpen.value = true
}

function handleEdit(row) {
  form.value = { ...row }
  dialogTitle.value = '编辑危险源条目'
  dialogOpen.value = true
}

async function submitForm() {
  await formRef.value.validate()
  if (form.value.id) {
    await updateHazardSourceItem(form.value)
  } else {
    await addHazardSourceItem(form.value)
  }
  ElMessage.success('操作成功')
  dialogOpen.value = false
  loadItems()
}

async function handleDelete() {
  await ElMessageBox.confirm(`确认删除选中的 ${ids.value.length} 条记录？`, '提示', { type: 'warning' })
  await deleteHazardSourceItem(ids.value.join(','))
  ElMessage.success('删除成功')
  loadItems()
}

async function handleDeleteOne(row) {
  await ElMessageBox.confirm(`确认删除「${row.description}」？`, '提示', { type: 'warning' })
  await deleteHazardSourceItem(row.id)
  ElMessage.success('删除成功')
  loadItems()
}

onMounted(async () => {
  const res = await getHazardSourceTree()
  treeData.value = res.data || []
  if (treeData.value.length > 0) activeCatId.value = treeData.value[0].id
})
</script>

<style lang="scss" scoped>
.hz-tabs {
  display: flex;
  gap: 8px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 0;
}
.hz-tab {
  padding: 8px 16px;
  border: none;
  border-bottom: 3px solid transparent;
  background: none;
  cursor: pointer;
  font-size: 14px;
  color: #475569;
  transition: all 0.15s;
  &:hover { color: #1e293b; }
  &.active { color: #3b82f6; font-weight: 600; border-bottom-color: #3b82f6; }
}
</style>
