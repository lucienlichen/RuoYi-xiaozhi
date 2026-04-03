<template>
  <div class="app-container">
    <!-- 分类 tabs -->
    <div class="reg-tabs">
      <button
        v-for="cat in categories"
        :key="cat.id"
        class="reg-tab"
        :class="{ active: queryParams.category === cat.id }"
        @click="switchCategory(cat.id)"
      >{{ cat.label }}</button>
    </div>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8" style="margin-top:12px">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Upload" @click="uploadDialog.open = true"
          v-hasPermi="['crane:regulation:upload']">上传法规文件</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="ids.length === 0"
          @click="handleDelete" v-hasPermi="['crane:regulation:remove']">删除</el-button>
      </el-col>
      <el-form :inline="true" style="margin-left:auto">
        <el-form-item>
          <el-input v-model="queryParams.title" placeholder="搜索标题" clearable
            @keyup.enter="handleQuery" style="width:200px" />
          <el-button type="primary" icon="Search" @click="handleQuery" style="margin-left:8px">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="docList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="docNo" label="文号/编号" width="160" show-overflow-tooltip />
      <el-table-column prop="publishDate" label="发布日期" width="110" />
      <el-table-column prop="fileName" label="文件" width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handlePreview(scope.row)">预览</el-button>
          <el-button link type="primary" icon="Edit" @click="handleEdit(scope.row)"
            v-hasPermi="['crane:regulation:edit']">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDeleteOne(scope.row)"
            v-hasPermi="['crane:regulation:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 上传对话框 -->
    <el-dialog title="上传法规文件" v-model="uploadDialog.open" width="520px" append-to-body>
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="90px">
        <el-form-item label="文档标题" prop="title">
          <el-input v-model="uploadForm.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="uploadForm.category" placeholder="请选择分类">
            <el-option v-for="c in categories" :key="c.id" :label="c.label" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="文号/编号">
          <el-input v-model="uploadForm.docNo" placeholder="如: 主席令第88号" />
        </el-form-item>
        <el-form-item label="发布日期">
          <el-date-picker v-model="uploadForm.publishDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="文件" prop="file">
          <el-upload
            ref="fileUploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.docx,.doc,.txt"
            :on-change="onFileChange"
            :on-remove="() => uploadForm.file = null"
          >
            <el-button icon="Upload">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、Word（.docx/.doc）、TXT</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitUpload" :loading="uploadDialog.loading">上 传</el-button>
        <el-button @click="uploadDialog.open = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog title="编辑法规" v-model="editDialog.open" width="860px" append-to-body :close-on-click-modal="false">
      <el-form ref="editFormRef" :model="editForm" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="文档标题" :rules="[{required:true,message:'标题不能为空'}]" prop="title">
              <el-input v-model="editForm.title" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="分类">
              <el-select v-model="editForm.category">
                <el-option v-for="c in categories" :key="c.id" :label="c.label" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="发布日期">
              <el-date-picker v-model="editForm.publishDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文号/编号">
              <el-input v-model="editForm.docNo" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitEdit" :loading="editDialog.saving">保 存</el-button>
        <el-button @click="editDialog.open = false">取 消</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRegulations, getRegulation, updateRegulation, deleteRegulation } from '@/api/intellect/regulation'
import { getToken } from '@/utils/auth'

const categories = [
  { id: 'laws', label: '法律法规' },
  { id: 'market_rules', label: '市场监管规章' },
  { id: 'tsg', label: 'TSG技术规范' },
  { id: 'standards', label: '标准' }
]

const docList = ref([])
const loading = ref(false)
const total = ref(0)
const ids = ref([])
const queryParams = ref({ pageNum: 1, pageSize: 10, category: 'laws', title: '' })

// Upload
const uploadDialog = reactive({ open: false, loading: false })
const uploadForm = ref({ title: '', category: 'laws', docNo: '', publishDate: '', file: null })
const uploadRules = {
  title: [{ required: true, message: '文档标题不能为空', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  file: [{ required: true, message: '请选择文件', trigger: 'change' }]
}
const uploadFormRef = ref(null)
const fileUploadRef = ref(null)

// Edit
const editDialog = reactive({ open: false, saving: false })
const editForm = ref({})
const editFormRef = ref(null)


function getList() {
  loading.value = true
  listRegulations(queryParams.value).then(res => {
    docList.value = res.rows || []
    total.value = res.total
    loading.value = false
  })
}

function switchCategory(cat) {
  queryParams.value.category = cat
  queryParams.value.pageNum = 1
  getList()
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function handleSelectionChange(sel) {
  ids.value = sel.map(r => r.id)
}

function onFileChange(file) {
  uploadForm.value.file = file.raw
}

async function submitUpload() {
  await uploadFormRef.value.validate()
  if (!uploadForm.value.file) { ElMessage.warning('请选择文件'); return }
  uploadDialog.loading = true
  const fd = new FormData()
  fd.append('file', uploadForm.value.file)
  fd.append('title', uploadForm.value.title)
  fd.append('category', uploadForm.value.category)
  if (uploadForm.value.docNo) fd.append('docNo', uploadForm.value.docNo)
  if (uploadForm.value.publishDate) fd.append('publishDate', uploadForm.value.publishDate)
  try {
    const res = await fetch(import.meta.env.VITE_APP_BASE_API + '/intellect/regulation/upload', {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + getToken() },
      body: fd
    })
    const json = await res.json()
    if (json.code === 200) {
      ElMessage.success('上传成功')
      uploadDialog.open = false
      uploadForm.value = { title: '', category: queryParams.value.category, docNo: '', publishDate: '', file: null }
      fileUploadRef.value?.clearFiles()
      getList()
    } else {
      ElMessage.error(json.msg || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传请求失败')
  } finally {
    uploadDialog.loading = false
  }
}

async function handleEdit(row) {
  const res = await getRegulation(row.id)
  editForm.value = { ...res.data }
  editDialog.open = true
}

async function submitEdit() {
  editDialog.saving = true
  try {
    await updateRegulation(editForm.value)
    ElMessage.success('保存成功')
    editDialog.open = false
    getList()
  } finally {
    editDialog.saving = false
  }
}

async function handlePreview(row) {
  if (row.filePath) {
    window.open(import.meta.env.VITE_APP_BASE_API + row.filePath, '_blank')
  } else {
    ElMessage.warning('该文档没有关联文件')
  }
}

async function handleDelete() {
  await ElMessageBox.confirm(`确认删除所选 ${ids.value.length} 条法规？`, '提示', { type: 'warning' })
  await deleteRegulation(ids.value.join(','))
  ElMessage.success('删除成功')
  getList()
}

async function handleDeleteOne(row) {
  await ElMessageBox.confirm(`确认删除「${row.title}」？`, '提示', { type: 'warning' })
  await deleteRegulation(row.id)
  ElMessage.success('删除成功')
  getList()
}

onMounted(() => getList())
</script>

<style lang="scss" scoped>
.reg-tabs {
  display: flex; gap: 8px; border-bottom: 1px solid #e2e8f0; padding-bottom: 0;
}
.reg-tab {
  padding: 8px 16px; border: none; border-bottom: 3px solid transparent;
  background: none; cursor: pointer; font-size: 14px; color: #475569;
  transition: all 0.15s;
  &:hover { color: #1e293b; }
  &.active { color: #64748b; font-weight: 600; border-bottom-color: #64748b; }
}
</style>
