/**
 * Voice Chat Composable (全局单例)
 * 封装与 clda-chat WebSocket 服务器的完整语音交互逻辑
 * 包括：WebSocket连接、Opus编解码(WebCodecs)、麦克风采集、TTS播放
 *
 * 单例模式：页面切换时保持连接不断开，避免重复问候
 */
import { ref } from 'vue'

const FRAME_DURATION = 0.06 // 60ms per Opus frame

// Opus header for WebCodecs decoder
function createOpusHead() {
  const h = new Uint8Array(19)
  h.set([0x4F, 0x70, 0x75, 0x73, 0x48, 0x65, 0x61, 0x64]) // "OpusHead"
  h[8] = 1   // version
  h[9] = 1   // channels
  h[12] = 0x80; h[13] = 0xBB // 48000 LE
  h[18] = 0  // mapping
  return h
}

// ── 全局单例状态 ──
const connected = ref(false)
const isRecording = ref(false)
const isPlaying = ref(false)
const messages = ref([])
const statusText = ref('未连接')

let ws = null
let audioCtx = null
let stream = null
let encoder = null
let decoder = null
let scriptNode = null
let pcmBuffer = null
let pcmBufferIdx = 0
let encodeTs = 0
let decodeTs = 0
let nextPlayTime = 0
let sessionId = null
let ttsTimeout = null
let actualRate = 48000
let frameSize = 960
let currentUsername = null
let reconnectTimer = null
let destroyed = false
let navigateCallback = null
let autoListenOnConnect = true // 连接后是否自动开启持续聆听

function addMessage(type, text) {
  messages.value.push({ type, text, time: Date.now() })
}

// ── Audio Setup ──
async function initAudio() {
  if (audioCtx && audioCtx.state !== 'closed') return // already initialized

  audioCtx = new AudioContext()
  actualRate = audioCtx.sampleRate
  frameSize = Math.round(actualRate * FRAME_DURATION)

  encoder = new AudioEncoder({
    output: (chunk) => {
      if (!ws || ws.readyState !== 1 || !isRecording.value) return
      const data = new ArrayBuffer(chunk.byteLength)
      chunk.copyTo(data)
      ws.send(data)
    },
    error: (e) => {
      console.warn('Encoder error:', e.message)
      try {
        encoder.configure({
          codec: 'opus',
          sampleRate: actualRate,
          numberOfChannels: 1,
          bitrate: 24000,
        })
      } catch (ex) { /* ignore */ }
    }
  })

  encoder.configure({
    codec: 'opus',
    sampleRate: actualRate,
    numberOfChannels: 1,
    bitrate: 24000,
  })

  decoder = new AudioDecoder({
    output: (audioData) => {
      const frames = audioData.numberOfFrames
      const decRate = audioData.sampleRate
      const pcm = new Float32Array(frames)
      audioData.copyTo(pcm, { planeIndex: 0 })
      audioData.close()

      const buffer = audioCtx.createBuffer(1, frames, decRate)
      buffer.getChannelData(0).set(pcm)

      const src = audioCtx.createBufferSource()
      src.buffer = buffer
      src.connect(audioCtx.destination)

      const now = audioCtx.currentTime
      if (nextPlayTime < now) nextPlayTime = now + 0.02
      src.start(nextPlayTime)
      nextPlayTime += buffer.duration
    },
    error: (e) => console.warn('Decoder error:', e.message)
  })

  decoder.configure({
    codec: 'opus',
    sampleRate: 48000,
    numberOfChannels: 1,
    description: createOpusHead(),
  })
}

// ── Microphone ──
async function startMic() {
  if (stream) return // already started

  pcmBuffer = new Float32Array(frameSize)
  pcmBufferIdx = 0
  stream = await navigator.mediaDevices.getUserMedia({
    audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true }
  })
  const source = audioCtx.createMediaStreamSource(stream)
  scriptNode = audioCtx.createScriptProcessor(4096, 1, 1)
  scriptNode.onaudioprocess = (e) => {
    if (!isRecording.value) return
    const input = e.inputBuffer.getChannelData(0)
    for (let i = 0; i < input.length; i++) {
      pcmBuffer[pcmBufferIdx++] = input[i]
      if (pcmBufferIdx >= frameSize) {
        try {
          const ad = new AudioData({
            format: 'f32-planar',
            sampleRate: actualRate,
            numberOfFrames: frameSize,
            numberOfChannels: 1,
            timestamp: encodeTs,
            data: pcmBuffer.slice().buffer,
          })
          encoder.encode(ad)
          ad.close()
          encodeTs += FRAME_DURATION * 1000000
        } catch (err) { /* ignore */ }
        pcmBufferIdx = 0
      }
    }
  }
  source.connect(scriptNode)
  scriptNode.connect(audioCtx.destination)
}

function stopMic() {
  if (stream) {
    stream.getTracks().forEach(t => t.stop())
    stream = null
  }
  if (scriptNode) {
    scriptNode.disconnect()
    scriptNode = null
  }
  pcmBufferIdx = 0
}

// ── WebSocket ──
function connectWs(wsBaseUrl, deviceMac) {
  if (destroyed) return
  const url = currentUsername
    ? `${wsBaseUrl}?device_mac=${deviceMac}&username=${encodeURIComponent(currentUsername)}`
    : `${wsBaseUrl}?device_mac=${deviceMac}`

  statusText.value = '连接中...'
  ws = new WebSocket(url)
  ws.binaryType = 'arraybuffer'

  ws.onopen = () => {
    wsSend({ type: 'hello' })
  }

  ws.onclose = (e) => {
    connected.value = false
    isPlaying.value = false
    isRecording.value = false
    statusText.value = '已断开'
    if (!destroyed) {
      reconnectTimer = setTimeout(() => connectWs(wsBaseUrl, deviceMac), 5000)
    }
  }

  ws.onerror = () => {
    statusText.value = '连接失败'
  }

  ws.onmessage = (event) => {
    if (event.data instanceof ArrayBuffer) {
      const bytes = new Uint8Array(event.data)
      if (bytes.length < 2) return
      try {
        const chunk = new EncodedAudioChunk({
          type: 'key',
          timestamp: decodeTs,
          data: event.data,
        })
        decoder.decode(chunk)
        decodeTs += 60000
      } catch (err) {
        try {
          decoder.reset()
          decoder.configure({
            codec: 'opus',
            sampleRate: 48000,
            numberOfChannels: 1,
            description: createOpusHead(),
          })
          decodeTs = 0
        } catch (e) { /* ignore */ }
      }
      return
    }

    let msg
    try { msg = JSON.parse(event.data) } catch { return }

    switch (msg.type) {
      case 'hello':
        sessionId = msg.session_id
        connected.value = true
        statusText.value = '已连接'
        addMessage('system', '语音助手已就绪')
        // 根据配置决定是否自动开启持续聆听
        if (autoListenOnConnect) {
          setTimeout(() => {
            if (connected.value && !isAutoListening.value) {
              listenMode.value = 'auto'
              startAutoListen()
            }
          }, 500)
        } else {
          listenMode.value = 'manual'
        }
        break

      case 'stt':
        if (msg.text) addMessage('user', msg.text)
        break

      case 'tts':
        if (msg.state === 'start') {
          isPlaying.value = true
          nextPlayTime = 0
          decodeTs = 0
          statusText.value = '正在回复...'
          clearTimeout(ttsTimeout)
          ttsTimeout = setTimeout(() => {
            if (isPlaying.value) {
              isPlaying.value = false
              statusText.value = '已连接'
            }
          }, 30000)
        }
        if (msg.state === 'sentence_start' && msg.text) {
          addMessage('ai', msg.text)
        }
        if (msg.state === 'stop') {
          clearTimeout(ttsTimeout)
          isPlaying.value = false
          // In auto mode, resume listening after TTS finishes
          if (isAutoListening.value) {
            isRecording.value = true
            statusText.value = '持续聆听中...'
          } else {
            statusText.value = '已连接'
          }
        }
        break

      case 'llm':
        break

      case 'navigate':
        if (msg.service && navigateCallback) {
          navigateCallback(msg.service)
        }
        break
    }
  }
}

function wsSend(obj) {
  if (ws && ws.readyState === 1) {
    ws.send(JSON.stringify(obj))
  }
}

// ── Listen mode: 'manual' (push-to-talk) or 'auto' (continuous, VAD-based) ──
const listenMode = ref('auto') // default to auto/continuous

function setListenMode(mode) {
  listenMode.value = mode
  // If switching away from auto while recording, stop
  if (mode === 'manual' && isRecording.value && isAutoListening.value) {
    stopAutoListen()
  }
}

// -- Auto listen (continuous, server-side VAD) --
const isAutoListening = ref(false)

function startAutoListen() {
  if (!connected.value || isAutoListening.value) return
  if (isPlaying.value) {
    wsSend({ type: 'abort', reason: 'user_interrupted' })
    isPlaying.value = false
    nextPlayTime = 0
  }
  isAutoListening.value = true
  isRecording.value = true
  encodeTs = 0
  statusText.value = '持续聆听中...'
  wsSend({ type: 'listen', mode: 'auto', state: 'start' })
}

function stopAutoListen() {
  if (!isAutoListening.value) return
  isAutoListening.value = false
  isRecording.value = false
  statusText.value = '已连接'
  wsSend({ type: 'listen', mode: 'auto', state: 'stop' })
}

function toggleAutoListen() {
  if (isAutoListening.value) {
    stopAutoListen()
  } else {
    startAutoListen()
  }
}

// -- Manual push-to-talk --
function startListening() {
  if (!connected.value) return
  if (isPlaying.value) {
    wsSend({ type: 'abort', reason: 'user_interrupted' })
    isPlaying.value = false
    nextPlayTime = 0
  }
  isRecording.value = true
  encodeTs = 0
  statusText.value = '正在录音...'
  wsSend({ type: 'listen', mode: 'manual', state: 'start' })
}

function stopListening() {
  if (!isRecording.value) return
  isRecording.value = false
  statusText.value = '处理中...'
  wsSend({ type: 'listen', mode: 'manual', state: 'stop' })
}

// ── Lifecycle ──
/**
 * 构建 WebSocket URL：
 * - 生产环境：走 nginx 反代同源路径，自动适配 ws/wss
 * - 开发环境：走 vite proxy 转发到 8082
 * - 可通过 VITE_APP_WS_URL 环境变量完全覆盖
 */
function buildDefaultWsUrl() {
  const envUrl = import.meta.env.VITE_APP_WS_URL
  if (envUrl) return envUrl
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${location.host}/clda/v1`
}

async function connectVoice(name, options = {}) {
  const {
    wsBaseUrl = buildDefaultWsUrl(),
    deviceMac = 'AA:BB:CC:DD:EE:FF',
  } = options

  // 如果已经连接且用户名相同，跳过重连
  if (connected.value && ws && ws.readyState === 1 && currentUsername === (name || null)) {
    return
  }

  // 如果有旧连接（用户名不同或重新登录），先断开
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    ws.onclose = null
    ws.close()
    ws = null
  }

  currentUsername = name || null
  destroyed = false

  try {
    await initAudio()
    await startMic()
    // 只在没有活跃连接时才新建WebSocket
    if (!ws || ws.readyState === WebSocket.CLOSED || ws.readyState === WebSocket.CLOSING) {
      connectWs(wsBaseUrl, deviceMac)
    }
  } catch (err) {
    statusText.value = '初始化失败'
    addMessage('system', `初始化失败: ${err.message}`)
  }
}

function disconnectVoice() {
  destroyed = true
  clearTimeout(reconnectTimer)
  clearTimeout(ttsTimeout)
  if (ws) {
    ws.onclose = null
    ws.close()
    ws = null
  }
  stopMic()
  if (encoder && encoder.state !== 'closed') {
    try { encoder.close() } catch { /* ignore */ }
  }
  if (decoder && decoder.state !== 'closed') {
    try { decoder.close() } catch { /* ignore */ }
  }
  if (audioCtx && audioCtx.state !== 'closed') {
    audioCtx.close().catch(() => {})
  }
  audioCtx = null
  connected.value = false
  isRecording.value = false
  isPlaying.value = false
  messages.value = []
}

/**
 * 设置导航回调（页面切换时更新，不需要重连）
 */
function setNavigateCallback(cb) {
  navigateCallback = cb
}

function setAutoListenOnConnect(val) {
  autoListenOnConnect = val
}

export function useVoiceChat() {
  return {
    // State (全局共享)
    connected,
    isRecording,
    isPlaying,
    isAutoListening,
    listenMode,
    messages,
    statusText,
    // Actions
    connect: connectVoice,
    disconnect: disconnectVoice,
    startListening,
    stopListening,
    toggleAutoListen,
    setListenMode,
    setNavigateCallback,
    setAutoListenOnConnect,
  }
}
