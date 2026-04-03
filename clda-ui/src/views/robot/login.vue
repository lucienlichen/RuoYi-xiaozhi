<template>
  <div class="robot-login ds-dark">
    <!-- Circuit board decorative lines (same as login.vue) -->
    <div class="circuit-lines">
      <svg class="circuit-svg" viewBox="0 0 1376 768" preserveAspectRatio="xMidYMid slice">
        <!-- Bottom-left circuit pattern -->
        <g stroke="rgba(56,189,248,0.12)" stroke-width="1" fill="none">
          <path d="M0,650 L120,650 L120,600 L200,600" />
          <path d="M0,680 L80,680 L80,620 L160,620 L160,580 L240,580" />
          <path d="M0,710 L60,710 L60,660 L140,660 L140,640 L220,640" />
          <circle cx="200" cy="600" r="3" fill="rgba(56,189,248,0.2)" />
          <circle cx="240" cy="580" r="3" fill="rgba(56,189,248,0.2)" />
          <circle cx="220" cy="640" r="3" fill="rgba(56,189,248,0.2)" />
        </g>
        <!-- Top-right circuit pattern -->
        <g stroke="rgba(56,189,248,0.12)" stroke-width="1" fill="none">
          <path d="M1376,120 L1256,120 L1256,170 L1176,170" />
          <path d="M1376,90 L1296,90 L1296,150 L1216,150 L1216,190 L1136,190" />
          <path d="M1376,60 L1316,60 L1316,110 L1236,110 L1236,130 L1156,130" />
          <circle cx="1176" cy="170" r="3" fill="rgba(56,189,248,0.2)" />
          <circle cx="1136" cy="190" r="3" fill="rgba(56,189,248,0.2)" />
          <circle cx="1156" cy="130" r="3" fill="rgba(56,189,248,0.2)" />
        </g>
        <!-- Bottom-right corner accent -->
        <g stroke="rgba(56,189,248,0.08)" stroke-width="1" fill="none">
          <path d="M1376,700 L1300,700 L1300,720 L1240,720" />
          <path d="M1376,730 L1320,730 L1320,740 L1260,740" />
          <circle cx="1240" cy="720" r="2" fill="rgba(56,189,248,0.15)" />
        </g>
        <!-- Top-left corner accent -->
        <g stroke="rgba(56,189,248,0.08)" stroke-width="1" fill="none">
          <path d="M0,60 L76,60 L76,40 L136,40" />
          <path d="M0,30 L56,30 L56,20 L116,20" />
          <circle cx="136" cy="40" r="2" fill="rgba(56,189,248,0.15)" />
        </g>
      </svg>
    </div>

    <!-- Main card -->
    <div class="robot-login__card">
      <!-- Glowing border effect -->
      <div class="card-glow"></div>

      <!-- Robot icon (same as login.vue) -->
      <div class="robot-icon">
        <svg viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect x="20" y="20" width="40" height="32" rx="6" stroke="url(#iconGrad)" stroke-width="2" fill="rgba(14,30,60,0.8)" />
          <circle cx="32" cy="36" r="4" fill="#38bdf8" opacity="0.9" />
          <circle cx="48" cy="36" r="4" fill="#38bdf8" opacity="0.9" />
          <circle cx="32" cy="36" r="6" fill="#38bdf8" opacity="0.15" />
          <circle cx="48" cy="36" r="6" fill="#38bdf8" opacity="0.15" />
          <line x1="40" y1="20" x2="40" y2="12" stroke="url(#iconGrad)" stroke-width="2" />
          <circle cx="40" cy="10" r="3" fill="#38bdf8" opacity="0.7" />
          <circle cx="40" cy="10" r="5" fill="#38bdf8" opacity="0.15" />
          <line x1="20" y1="30" x2="14" y2="26" stroke="url(#iconGrad)" stroke-width="1.5" />
          <circle cx="13" cy="25" r="2" fill="#06b6d4" opacity="0.6" />
          <line x1="60" y1="30" x2="66" y2="26" stroke="url(#iconGrad)" stroke-width="1.5" />
          <circle cx="67" cy="25" r="2" fill="#06b6d4" opacity="0.6" />
          <rect x="26" y="54" width="28" height="14" rx="4" stroke="url(#iconGrad)" stroke-width="1.5" fill="rgba(14,30,60,0.6)" />
          <line x1="33" y1="58" x2="47" y2="58" stroke="#38bdf8" stroke-width="1" opacity="0.4" />
          <line x1="33" y1="62" x2="47" y2="62" stroke="#38bdf8" stroke-width="1" opacity="0.3" />
          <line x1="26" y1="58" x2="18" y2="62" stroke="url(#iconGrad)" stroke-width="1.5" />
          <line x1="54" y1="58" x2="62" y2="62" stroke="url(#iconGrad)" stroke-width="1.5" />
          <circle cx="17" cy="63" r="2" fill="#06b6d4" opacity="0.5" />
          <circle cx="63" cy="63" r="2" fill="#06b6d4" opacity="0.5" />
          <defs>
            <linearGradient id="iconGrad" x1="0" y1="0" x2="80" y2="80">
              <stop offset="0%" stop-color="#38bdf8" />
              <stop offset="100%" stop-color="#06b6d4" />
            </linearGradient>
          </defs>
        </svg>
      </div>

      <!-- Title -->
      <h3 class="title">{{ title }}</h3>

      <!-- Face recognition area -->
      <div class="face-area">
        <!-- Face scan icon -->
        <div class="face-icon" :class="{ recognized: faceMatched, scanning: cameraReady && !faceMatched }">
          <svg v-if="!cameraReady && !faceMatched" viewBox="0 0 48 48" fill="none" class="icon-svg">
            <circle cx="24" cy="24" r="20" stroke="url(#faceGrad)" stroke-width="2" stroke-dasharray="8 4" opacity="0.6">
              <animateTransform attributeName="transform" type="rotate" from="0 24 24" to="360 24 24" dur="3s" repeatCount="indefinite" />
            </circle>
            <defs>
              <linearGradient id="faceGrad" x1="0" y1="0" x2="48" y2="48">
                <stop offset="0%" stop-color="#38bdf8" />
                <stop offset="100%" stop-color="#06b6d4" />
              </linearGradient>
            </defs>
          </svg>
          <svg v-else-if="faceMatched" viewBox="0 0 48 48" fill="none" class="icon-svg">
            <circle cx="24" cy="24" r="20" stroke="#22c55e" stroke-width="2" />
            <path d="M15 24 L21 30 L33 18" stroke="#22c55e" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <svg v-else viewBox="0 0 48 48" fill="none" class="icon-svg">
            <!-- Face outline -->
            <ellipse cx="24" cy="22" rx="12" ry="14" stroke="url(#faceGrad2)" stroke-width="1.5" fill="none" />
            <!-- Eyes -->
            <circle cx="20" cy="20" r="1.5" fill="#38bdf8" />
            <circle cx="28" cy="20" r="1.5" fill="#38bdf8" />
            <!-- Scan corners -->
            <path d="M8 16 L8 8 L16 8" stroke="#38bdf8" stroke-width="2" stroke-linecap="round" />
            <path d="M40 16 L40 8 L32 8" stroke="#38bdf8" stroke-width="2" stroke-linecap="round" />
            <path d="M8 32 L8 40 L16 40" stroke="#38bdf8" stroke-width="2" stroke-linecap="round" />
            <path d="M40 32 L40 40 L32 40" stroke="#38bdf8" stroke-width="2" stroke-linecap="round" />
            <defs>
              <linearGradient id="faceGrad2" x1="12" y1="8" x2="36" y2="36">
                <stop offset="0%" stop-color="#38bdf8" />
                <stop offset="100%" stop-color="#06b6d4" />
              </linearGradient>
            </defs>
          </svg>
        </div>

        <!-- Camera preview -->
        <div class="camera-container" v-show="cameraReady && !faceMatched">
          <video ref="videoRef" autoplay playsinline muted class="camera-video"></video>
          <div class="camera-overlay">
            <!-- Scan frame corners -->
            <div class="scan-corner tl"></div>
            <div class="scan-corner tr"></div>
            <div class="scan-corner bl"></div>
            <div class="scan-corner br"></div>
            <div class="scan-line"></div>
          </div>
        </div>

        <!-- Recognized user display -->
        <div v-if="faceMatched" class="recognized-info">
          <h2>欢迎, {{ recognizedName }}!</h2>
          <p class="recognized-sub">身份验证成功，正在进入系统...</p>
        </div>
      </div>

      <!-- Progress bar -->
      <div class="progress-section">
        <div class="progress-track">
          <div
            class="progress-fill"
            :class="{ success: faceMatched }"
            :style="{ width: progress + '%' }"
          ></div>
        </div>
        <p class="progress-text">{{ statusText }}</p>
      </div>

      <!-- Skip button -->
      <div class="action-area" v-if="!faceMatched && !autoLoginInProgress">
        <button class="skip-btn" @click="skipFaceRecognition" :disabled="autoLoginInProgress">
          跳过识别，手动登录
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken, setToken } from '@/utils/auth'
import useUserStore from '@/store/modules/user'
import request from '@/utils/request'
import * as faceapi from '@vladmandic/face-api'

const title = import.meta.env.VITE_APP_TITLE
const router = useRouter()
const userStore = useUserStore()

// Refs
const videoRef = ref(null)

// State
const cameraReady = ref(false)
const faceMatched = ref(false)
const recognizedName = ref('')
const progress = ref(0)
const statusText = ref('正在初始化...')
const autoLoginInProgress = ref(false)

// Internal state
let faceStream = null
let faceDetectionTimer = null
let progressTimer = null
let faceModelsLoaded = false

// Face API configuration — 模型从本地 public 目录加载，支持离线内网部署
const FACE_MODEL_URL = '/models/face-api/'
const FACE_API_URL = `${import.meta.env.VITE_APP_BASE_API}/api/face/list`

// Check if already logged in
onMounted(async () => {
  if (getToken()) {
    router.replace('/robot/menu')
    return
  }
  startProgressTimer()
  await startFaceRecognition()
})

onUnmounted(() => {
  cleanup()
})

function startProgressTimer() {
  const duration = 5000 // 5 seconds
  const interval = 50
  const step = (100 / duration) * interval
  let current = 0

  progressTimer = setInterval(() => {
    current += step
    if (current >= 100) {
      current = 100
      clearInterval(progressTimer)
      progressTimer = null
      // If no face matched by end of timer, redirect to login page
      if (!faceMatched.value && !autoLoginInProgress.value) {
        cleanup()
        router.push('/login')
      }
    }
    progress.value = Math.min(current, 100)
  }, interval)
}

async function startFaceRecognition() {
  statusText.value = '正在请求摄像头权限...'

  // Request camera permission
  try {
    faceStream = await navigator.mediaDevices.getUserMedia({
      video: { width: 480, height: 360, facingMode: 'user' }
    })
  } catch (err) {
    console.warn('Camera permission denied:', err.message)
    statusText.value = '摄像头权限被拒绝，等待倒计时完成...'
    return
  }

  // Show camera preview
  if (videoRef.value) {
    videoRef.value.srcObject = faceStream
    cameraReady.value = true
  }

  // Load face-api.js models
  statusText.value = '正在加载人脸识别模型...'
  try {
    await loadFaceModels()
  } catch (err) {
    console.warn('Failed to load face models:', err.message)
    statusText.value = '人脸模型加载失败，等待倒计时...'
    return
  }

  // Fetch registered faces
  statusText.value = '正在获取已注册人脸...'
  const registeredFaces = await fetchRegisteredFaces()
  if (registeredFaces.length === 0) {
    statusText.value = '暂无已注册人脸，等待倒计时...'
    return
  }

  // Start face detection loop
  statusText.value = '正在识别身份...'
  startFaceDetection(registeredFaces)
}

async function loadFaceModels() {
  if (faceModelsLoaded) return
  await Promise.all([
    faceapi.nets.tinyFaceDetector.loadFromUri(FACE_MODEL_URL),
    faceapi.nets.faceLandmark68TinyNet.loadFromUri(FACE_MODEL_URL),
    faceapi.nets.faceRecognitionNet.loadFromUri(FACE_MODEL_URL),
  ])
  faceModelsLoaded = true
}

async function fetchRegisteredFaces() {
  try {
    const resp = await fetch(FACE_API_URL)
    const json = await resp.json()
    if (json.code !== 200 || !json.data || json.data.length === 0) {
      return []
    }
    return json.data.map(f => ({
      name: f.name,
      descriptor: new Float32Array(JSON.parse(f.descriptor)),
    }))
  } catch (err) {
    console.warn('Failed to fetch faces:', err.message)
    return []
  }
}

function startFaceDetection(registeredFaces) {
  const labeledDescriptors = []
  const nameMap = {}
  registeredFaces.forEach((f, i) => {
    const label = `face_${i}`
    nameMap[label] = f.name
    labeledDescriptors.push(new faceapi.LabeledFaceDescriptors(label, [f.descriptor]))
  })
  const faceMatcher = new faceapi.FaceMatcher(labeledDescriptors, 0.6)

  let matchCount = 0
  let lastMatchName = null

  faceDetectionTimer = setInterval(async () => {
    if (!videoRef.value || faceMatched.value) return

    try {
      const detections = await faceapi
        .detectAllFaces(videoRef.value, new faceapi.TinyFaceDetectorOptions())
        .withFaceLandmarks(true)
        .withFaceDescriptors()

      if (detections.length === 0) {
        matchCount = 0
        lastMatchName = null
        statusText.value = '未检测到人脸，请面对摄像头...'
        return
      }

      const best = detections.reduce((a, b) =>
        a.detection.box.area > b.detection.box.area ? a : b
      )

      const match = faceMatcher.findBestMatch(best.descriptor)
      if (match.label === 'unknown') {
        matchCount = 0
        lastMatchName = null
        statusText.value = '人脸不匹配，请确认已注册...'
        return
      }

      const name = nameMap[match.label]
      if (name === lastMatchName) {
        matchCount++
      } else {
        matchCount = 1
        lastMatchName = name
      }

      const confidence = Math.round((1 - match.distance) * 100)
      statusText.value = `识别中: ${name} (${confidence}% 置信度)`

      // Require 3 consecutive matches
      if (matchCount >= 3) {
        onFaceRecognized(name)
      }
    } catch (err) {
      console.warn('Face detection error:', err.message)
    }
  }, 500)
}

async function onFaceRecognized(name) {
  faceMatched.value = true
  recognizedName.value = name
  progress.value = 100
  statusText.value = `识别成功: ${name}，正在登录...`
  autoLoginInProgress.value = true

  // Stop camera and progress
  stopFaceCamera()
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }

  // Face login - call dedicated /faceLogin endpoint (no password needed)
  try {
    const res = await request({
      url: '/faceLogin',
      headers: { isToken: false, repeatSubmit: false },
      method: 'post',
      data: { username: name }
    })
    setToken(res.token)
    userStore.token = res.token
    ElMessage.success(`欢迎回来, ${name}!`)
    setTimeout(() => {
      router.replace('/robot/menu')
    }, 1000)
  } catch (err) {
    // If auto-login fails, redirect to login page
    cleanup()
    router.push('/login')
  }
}

function skipFaceRecognition() {
  cleanup()
  router.push('/login')
}

function stopFaceCamera() {
  if (faceDetectionTimer) {
    clearInterval(faceDetectionTimer)
    faceDetectionTimer = null
  }
  if (faceStream) {
    faceStream.getTracks().forEach(t => t.stop())
    faceStream = null
  }
  cameraReady.value = false
}

function cleanup() {
  stopFaceCamera()
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}
</script>

<style lang="scss" scoped>
.robot-login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100vw;
  background: radial-gradient(ellipse at 50% 50%, var(--ds-surface-container) 0%, var(--ds-surface-container-lowest) 100%);
  position: relative;
  overflow: hidden;
}

/* Circuit board decorative lines — identical to login.vue */
.circuit-lines {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.circuit-svg {
  width: 100%;
  height: 100%;
}

/* Login card — same glassmorphism as login.vue */
.robot-login__card {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 40px 36px 28px;
  background: var(--ds-glass-bg);
  border: 1px solid var(--ds-glass-border);
  border-radius: var(--ds-radius-lg);
  backdrop-filter: blur(20px);
  box-shadow:
    0 0 40px rgba(56, 189, 248, 0.06),
    0 0 80px rgba(6, 182, 212, 0.04),
    inset 0 1px 0 rgba(56, 189, 248, 0.1);
}

.card-glow {
  position: absolute;
  inset: -1px;
  border-radius: 17px;
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.12) 0%, transparent 40%, transparent 60%, rgba(6, 182, 212, 0.08) 100%);
  z-index: -1;
  pointer-events: none;
}

/* Robot icon — same as login.vue */
.robot-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 8px;
  svg {
    width: 72px;
    height: 72px;
    filter: drop-shadow(0 0 12px rgba(56, 189, 248, 0.3));
  }
}

/* Title — same as login.vue */
.title {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: var(--ds-on-surface);
  margin: 0 0 24px 0;
  letter-spacing: 2px;
  text-shadow: 0 0 20px rgba(56, 189, 248, 0.3);
}

/* Face area */
.face-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
}

.face-icon {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  filter: drop-shadow(0 0 12px rgba(56, 189, 248, 0.3));
  transition: all 0.5s ease;

  .icon-svg {
    width: 48px;
    height: 48px;
  }

  &.scanning {
    animation: pulse-glow 2s ease-in-out infinite;
  }

  &.recognized {
    filter: drop-shadow(0 0 16px rgba(34, 197, 94, 0.4));
  }
}

@keyframes pulse-glow {
  0%, 100% { filter: drop-shadow(0 0 12px rgba(56, 189, 248, 0.3)); }
  50% { filter: drop-shadow(0 0 24px rgba(56, 189, 248, 0.6)); }
}

/* Camera preview */
.camera-container {
  position: relative;
  width: 280px;
  height: 210px;
  border-radius: var(--ds-radius-lg);
  overflow: hidden;
  border: 1px solid var(--ds-outline-variant);
  box-shadow: 0 0 20px rgba(56, 189, 248, 0.08);
}

.camera-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
}

.camera-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

/* Scan frame corners */
.scan-corner {
  position: absolute;
  width: 24px;
  height: 24px;
  border-color: #38bdf8;
  border-style: solid;
  border-width: 0;

  &.tl { top: 12px; left: 12px; border-top-width: 2px; border-left-width: 2px; border-radius: 4px 0 0 0; }
  &.tr { top: 12px; right: 12px; border-top-width: 2px; border-right-width: 2px; border-radius: 0 4px 0 0; }
  &.bl { bottom: 12px; left: 12px; border-bottom-width: 2px; border-left-width: 2px; border-radius: 0 0 0 4px; }
  &.br { bottom: 12px; right: 12px; border-bottom-width: 2px; border-right-width: 2px; border-radius: 0 0 4px 0; }
}

.scan-line {
  position: absolute;
  left: 12px;
  right: 12px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #38bdf8, transparent);
  animation: scan 2s ease-in-out infinite;
  box-shadow: 0 0 8px rgba(56, 189, 248, 0.4);
}

@keyframes scan {
  0%, 100% { top: 15%; opacity: 0.3; }
  50% { top: 80%; opacity: 0.8; }
}

/* Recognized info */
.recognized-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  animation: fadeInUp 0.5s ease;

  h2 {
    color: #22c55e;
    font-size: 20px;
    font-weight: 600;
    margin: 0;
    text-shadow: 0 0 20px rgba(34, 197, 94, 0.3);
  }

  .recognized-sub {
    color: var(--ds-on-surface-variant);
    font-size: 13px;
    margin: 0;
  }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Custom progress bar — matches login.vue color scheme */
.progress-section {
  margin-bottom: 24px;
}

.progress-track {
  width: 100%;
  height: 6px;
  background: var(--ds-surface-container-low);
  border-radius: 3px;
  overflow: hidden;
  border: 1px solid var(--ds-outline-variant);
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #1e6baf, #0ea5e9, #06b6d4);
  transition: width 0.1s linear;
  box-shadow: 0 0 8px rgba(14, 165, 233, 0.4);

  &.success {
    background: linear-gradient(90deg, #16a34a, #22c55e);
    box-shadow: 0 0 8px rgba(34, 197, 94, 0.4);
  }
}

.progress-text {
  text-align: center;
  color: var(--ds-on-surface-variant);
  font-size: 13px;
  margin: 12px 0 0 0;
  min-height: 20px;
  letter-spacing: 0.5px;
}

/* Skip button — styled to match login.vue button aesthetic */
.action-area {
  text-align: center;
}

.skip-btn {
  background: transparent;
  border: 1px solid var(--ds-outline-variant);
  color: var(--ds-on-surface-variant);
  padding: 10px 28px;
  border-radius: var(--ds-radius);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  letter-spacing: 1px;

  &:hover {
    border-color: rgba(56, 189, 248, 0.5);
    color: var(--ds-on-surface);
    box-shadow: 0 0 16px rgba(56, 189, 248, 0.1);
    background: rgba(56, 189, 248, 0.05);
  }

  &:active {
    transform: scale(0.98);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

/* Portrait screen optimization for 8-inch robot display */
@media (max-width: 820px) and (orientation: portrait) {
  .robot-login__card {
    width: calc(100vw - 32px);
    max-width: 380px;
    padding: 32px 24px 24px;
  }

  .camera-container {
    width: 240px;
    height: 180px;
  }
}

/* Small phone screens */
@media (max-width: 480px) {
  .robot-login__card {
    width: calc(100vw - 32px);
    padding: 28px 20px 20px;
  }

  .camera-container {
    width: 220px;
    height: 165px;
  }
}
</style>
