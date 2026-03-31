<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="设备名称" prop="equipmentName">
        <el-input v-model="queryParams.equipmentName" placeholder="请输入设备名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="设备编号" prop="equipmentCode">
        <el-input v-model="queryParams.equipmentCode" placeholder="请输入设备编号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="所属分区" prop="partitionId">
        <el-select v-model="queryParams.partitionId" placeholder="请选择分区" clearable>
          <el-option v-for="p in partitions" :key="p.id" :label="p.partitionName" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable>
          <el-option label="正常" value="NORMAL" />
          <el-option label="警告" value="WARNING" />
          <el-option label="故障" value="FAULT" />
          <el-option label="停用" value="STOPPED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['crane:equipment:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Upload" @click="handleImport" v-hasPermi="['crane:equipment:import']">导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['crane:equipment:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['crane:equipment:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="equipmentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="equipmentName" label="设备名称" min-width="140" />
      <el-table-column prop="equipmentCode" label="设备编号" width="120" />
      <el-table-column prop="equipmentType" label="设备类型" width="100" />
      <el-table-column prop="model" label="型号" width="100" />
      <el-table-column prop="partitionName" label="所属分区" width="100" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ratedCapacity" label="额定起重量(吨)" width="130" />
      <el-table-column prop="manufacturer" label="制造单位" width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['crane:equipment:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['crane:equipment:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加/修改对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备名称" prop="equipmentName">
              <el-input v-model="form.equipmentName" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备编号" prop="equipmentCode">
              <el-input v-model="form.equipmentCode" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备类型" prop="equipmentType">
              <el-input v-model="form.equipmentType" placeholder="如:桥式/门式/塔式" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备型号" prop="model">
              <el-input v-model="form.model" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分区" prop="partitionId">
              <el-select v-model="form.partitionId" placeholder="请选择">
                <el-option v-for="p in partitions" :key="p.id" :label="p.partitionName" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="额定起重量" prop="ratedCapacity">
              <el-input-number v-model="form.ratedCapacity" :precision="2" :min="0" placeholder="吨" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="制造单位">
              <el-input v-model="form.manufacturer" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出厂日期">
              <el-date-picker v-model="form.manufactureDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="注册登记号">
              <el-input v-model="form.registrationCode" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status">
                <el-option label="正常" value="NORMAL" />
                <el-option label="警告" value="WARNING" />
                <el-option label="故障" value="FAULT" />
                <el-option label="停用" value="STOPPED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog title="设备导入" v-model="upload.open" width="400px" append-to-body>
      <el-upload ref="uploadRef" :limit="1" accept=".xlsx,.xls,.csv" :action="upload.url"
        :headers="upload.headers" :auto-upload="false" :on-success="handleUploadSuccess" drag>
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip><div class="el-upload__tip">仅允许导入xls、xlsx、csv格式文件</div></template>
      </el-upload>
      <template #footer>
        <el-button @click="importTemplate">下载模板</el-button>
        <el-button type="primary" @click="submitUpload">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { listEquipment, getEquipment, addEquipment, updateEquipment, delEquipment } from '@/api/intellect/equipment'
import { listPartition } from '@/api/intellect/partition'
import { getToken } from '@/utils/auth'

const { proxy } = getCurrentInstance()

const equipmentList = ref([])
const partitions = ref([])
const loading = ref(true)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const ids = ref([])
const multiple = ref(true)
const total = ref(0)

const queryParams = ref({ pageNum: 1, pageSize: 10, equipmentName: undefined, equipmentCode: undefined, partitionId: undefined, status: undefined })
const form = ref({})
const rules = {
  equipmentName: [{ required: true, message: '设备名称不能为空', trigger: 'blur' }],
  partitionId: [{ required: true, message: '请选择所属分区', trigger: 'change' }],
}

const upload = reactive({
  open: false,
  url: import.meta.env.VITE_APP_BASE_API + '/intellect/equipment/import',
  headers: { Authorization: 'Bearer ' + getToken() },
})

function statusTagType(status) {
  return { NORMAL: 'success', WARNING: 'warning', FAULT: 'danger', STOPPED: 'info' }[status] || 'info'
}

function getList() {
  loading.value = true
  listEquipment(queryParams.value).then(res => {
    equipmentList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function loadPartitions() {
  listPartition({}).then(res => { partitions.value = res.data })
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection) { ids.value = selection.map(item => item.id); multiple.value = !selection.length }

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加设备'
}

function handleUpdate(row) {
  reset()
  getEquipment(row.id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改设备'
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate(valid => {
    if (!valid) return
    if (form.value.id != null) {
      updateEquipment(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
    } else {
      addEquipment(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
    }
  })
}

function handleDelete(row) {
  const delIds = row.id ? [row.id] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delEquipment(delIds)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

function handleImport() { upload.open = true }
function submitUpload() { proxy.$refs['uploadRef'].submit() }
function handleUploadSuccess(res) { upload.open = false; proxy.$modal.msgSuccess(res.msg); getList() }
function importTemplate() { proxy.download('/intellect/equipment/importTemplate', {}, '设备导入模板.xlsx') }
function handleExport() { proxy.download('/intellect/equipment/export', { ...queryParams.value }, '设备数据.xlsx') }

function reset() {
  form.value = { status: 'NORMAL' }
  proxy.resetForm('formRef')
}

function cancel() { open.value = false; reset() }

loadPartitions()
getList()
</script>
