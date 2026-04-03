<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="姓名" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入人员姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['intellect:face:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['intellect:face:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['intellect:face:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="faceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" min-width="80" />
      <el-table-column label="人员姓名" align="center" prop="name" min-width="120" show-overflow-tooltip />
      <el-table-column label="照片" align="center" min-width="100">
        <template #default="scope">
          <el-image
            v-if="scope.row.photoUrl"
            :src="scope.row.photoUrl"
            :preview-src-list="[scope.row.photoUrl]"
            fit="cover"
            style="width: 50px; height: 50px; border-radius: 4px;"
            preview-teleported
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" min-width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" min-width="200">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['intellect:face:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['intellect:face:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加人脸注册对话框 -->
    <el-dialog :title="title" v-model="open" width="650px" append-to-body @close="handleDialogClose">
      <el-form ref="faceRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入人员姓名"
            prefix-icon="User"
            maxlength="30"
            show-word-limit
          />
        </el-form-item>
        <!-- 拍照区域仅在新增时显示 -->
        <el-form-item v-if="!form.id" label="拍照" prop="photoUrl">
          <div class="camera-container">
            <div class="camera-preview">
              <video
                v-show="cameraActive && !capturedPhoto"
                ref="videoRef"
                autoplay
                playsinline
                style="width: 320px; height: 240px; border-radius: 4px; background: #000;"
              ></video>
              <canvas
                v-show="capturedPhoto"
                ref="canvasRef"
                width="320"
                height="240"
                style="border-radius: 4px; background: #f5f5f5;"
              ></canvas>
              <div v-if="!cameraActive && !capturedPhoto" class="camera-placeholder">
                <el-icon :size="48"><Camera /></el-icon>
                <p>点击"打开摄像头"开始</p>
              </div>
            </div>
            <div class="camera-actions">
              <el-button v-if="!cameraActive && !capturedPhoto" type="primary" @click="startCamera">打开摄像头</el-button>
              <el-button v-if="cameraActive && !capturedPhoto" type="success" @click="capturePhoto" :loading="detecting">拍照</el-button>
              <el-button v-if="capturedPhoto" type="warning" @click="retakePhoto">重拍</el-button>
              <el-button v-if="cameraActive" type="danger" @click="stopCamera">关闭摄像头</el-button>
            </div>
            <div v-if="faceDetected" class="face-status success">
              <el-icon><CircleCheck /></el-icon>
              <span>已检测到人脸并提取特征</span>
            </div>
            <div v-if="faceError" class="face-status error">
              <el-icon><CircleClose /></el-icon>
              <span>{{ faceError }}</span>
            </div>
          </div>
        </el-form-item>
        <!-- 编辑时显示已有照片 -->
        <el-form-item v-if="form.id && form.photoUrl" label="照片">
          <el-image
            :src="form.photoUrl"
            fit="cover"
            style="width: 160px; height: 120px; border-radius: 4px;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Face">
import { listFace, getFace, delFace, addFace, updateFace } from "@/api/intellect/face"
import { Camera, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import * as faceapi from '@vladmandic/face-api'

const { proxy } = getCurrentInstance()

const faceList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

// 摄像头相关
const videoRef = ref(null)
const canvasRef = ref(null)
const cameraActive = ref(false)
const capturedPhoto = ref(false)
const detecting = ref(false)
const faceDetected = ref(false)
const faceError = ref("")
let mediaStream = null
// face-api.js 通过 npm import 加载，无需动态 script 注入
let modelsLoaded = false

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
  },
  rules: {
    name: { required: true, message: "人员姓名不能为空", trigger: "blur" },
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询人脸列表 */
function getList() {
  loading.value = true
  listFace(queryParams.value).then(response => {
    faceList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    name: null,
    photoUrl: null,
    descriptor: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  }
  capturedPhoto.value = false
  faceDetected.value = false
  faceError.value = ""
  stopCamera()
  proxy.resetForm("faceRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加人脸注册"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getFace(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改人脸注册"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["faceRef"].validate(valid => {
    if (valid) {
      if (!form.value.id && !faceDetected.value) {
        proxy.$modal.msgWarning("请先拍照并检测人脸")
        return
      }
      if (form.value.id != null) {
        updateFace(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addFace(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除人脸注册编号为"' + _ids + '"的数据项？').then(function() {
    return delFace(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 对话框关闭时清理 */
function handleDialogClose() {
  stopCamera()
  capturedPhoto.value = false
  faceDetected.value = false
  faceError.value = ""
}

// ===================== 摄像头与人脸检测 =====================

/** 加载人脸检测模型（从本地 public 目录加载，支持离线内网部署） */
async function loadModels() {
  if (modelsLoaded) return
  const MODEL_URL = '/models/face-api/'
  await faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL)
  await faceapi.nets.faceLandmark68TinyNet.loadFromUri(MODEL_URL)
  await faceapi.nets.faceRecognitionNet.loadFromUri(MODEL_URL)
  modelsLoaded = true
}

/** 打开摄像头 */
async function startCamera() {
  try {
    faceError.value = ""
    // 先加载模型，再打开摄像头，避免拍照时卡顿
    faceError.value = "正在加载人脸模型..."
    await loadModels()
    faceError.value = ""

    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: { width: 320, height: 240, facingMode: "user" }
    })
    cameraActive.value = true
    await nextTick()
    if (videoRef.value) {
      videoRef.value.srcObject = mediaStream
    }
  } catch (err) {
    faceError.value = err.message.includes('加载') ? err.message : "无法访问摄像头: " + err.message
  }
}

/** 关闭摄像头 */
function stopCamera() {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }
  cameraActive.value = false
}

/** 拍照并检测人脸 */
async function capturePhoto() {
  if (!videoRef.value || !canvasRef.value) return
  detecting.value = true
  faceError.value = ""
  faceDetected.value = false

  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  ctx.drawImage(videoRef.value, 0, 0, 320, 240)
  capturedPhoto.value = true

  // 停止摄像头流
  stopCamera()

  try {
    if (!modelsLoaded) {
      await loadModels()
    }

    // 检测人脸并提取描述符（使用轻量级检测器）
    const detection = await faceapi
      .detectSingleFace(canvas, new faceapi.TinyFaceDetectorOptions())
      .withFaceLandmarks(true)
      .withFaceDescriptor()

    if (!detection) {
      faceError.value = "未检测到人脸，请重拍"
      detecting.value = false
      return
    }

    // 存储人脸描述符为 JSON 字符串
    const descriptorArray = Array.from(detection.descriptor)
    form.value.descriptor = JSON.stringify(descriptorArray)

    // 将照片转为 base64 存入 photoUrl
    form.value.photoUrl = canvas.toDataURL('image/jpeg', 0.8)

    faceDetected.value = true
  } catch (err) {
    faceError.value = "人脸检测失败: " + err.message
  } finally {
    detecting.value = false
  }
}

/** 重拍 */
function retakePhoto() {
  capturedPhoto.value = false
  faceDetected.value = false
  faceError.value = ""
  form.value.photoUrl = null
  form.value.descriptor = null
  startCamera()
}

getList()
</script>

<style scoped>
.camera-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.camera-preview {
  width: 320px;
  height: 240px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.camera-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #909399;
}
.camera-placeholder p {
  margin: 0;
  font-size: 14px;
}
.camera-actions {
  display: flex;
  gap: 8px;
}
.face-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.face-status.success {
  color: #67c23a;
}
.face-status.error {
  color: #f56c6c;
}
</style>
