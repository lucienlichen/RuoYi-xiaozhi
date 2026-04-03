<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="分区名称" prop="partitionName">
        <el-input v-model="queryParams.partitionName" placeholder="请输入分区名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable>
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['crane:partition:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="partitionList" row-key="id" default-expand-all
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">
      <el-table-column prop="partitionName" label="分区名称" />
      <el-table-column prop="partitionCode" label="分区编码" width="160" />
      <el-table-column prop="orderNum" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['crane:partition:edit']">修改</el-button>
          <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['crane:partition:add']">新增</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['crane:partition:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/修改对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="上级分区" prop="parentId">
          <el-tree-select v-model="form.parentId" :data="partitionOptions" :props="{ value: 'id', label: 'partitionName', children: 'children' }"
            value-key="id" placeholder="选择上级分区" check-strictly />
        </el-form-item>
        <el-form-item label="分区名称" prop="partitionName">
          <el-input v-model="form.partitionName" placeholder="请输入分区名称" />
        </el-form-item>
        <el-form-item label="分区编码" prop="partitionCode">
          <el-input v-model="form.partitionCode" placeholder="请输入分区编码" />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { listPartition, getPartition, addPartition, updatePartition, delPartition } from '@/api/intellect/partition'

const { proxy } = getCurrentInstance()

const partitionList = ref([])
const partitionOptions = ref([])
const loading = ref(true)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')

const queryParams = ref({ partitionName: undefined, status: undefined })
const form = ref({})
const rules = {
  partitionName: [{ required: true, message: '分区名称不能为空', trigger: 'blur' }]
}

function getList() {
  loading.value = true
  listPartition(queryParams.value).then(res => {
    partitionList.value = proxy.handleTree(res.data, 'id', 'parentId')
    loading.value = false
  })
}

function getTreeSelect() {
  listPartition({}).then(res => {
    partitionOptions.value = [{ id: 0, partitionName: '顶级分区', children: proxy.handleTree(res.data, 'id', 'parentId') }]
  })
}

function handleQuery() { getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

function handleAdd(row) {
  reset()
  getTreeSelect()
  if (row != null && row.id) {
    form.value.parentId = row.id
  } else {
    form.value.parentId = 0
  }
  open.value = true
  title.value = '添加分区'
}

function handleUpdate(row) {
  reset()
  getTreeSelect()
  getPartition(row.id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改分区'
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate(valid => {
    if (!valid) return
    if (form.value.id != null) {
      updatePartition(form.value).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addPartition(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除分区"' + row.partitionName + '"？').then(() => {
    return delPartition(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function reset() {
  form.value = { parentId: 0, orderNum: 0, status: '0' }
  proxy.resetForm('formRef')
}

function cancel() { open.value = false; reset() }

getList()
</script>
