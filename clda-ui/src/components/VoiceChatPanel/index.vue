<template>
  <div class="voice-chat-panel">
    <!-- Collapsed state -->
    <div v-if="!isExpanded" class="voice-collapsed">
      <!-- Center bubble -->
      <transition name="bubble">
        <div v-if="bubbleText" class="voice-bubble-center" @click="isExpanded = true">
          <span class="bubble-label">{{ bubbleType === 'user' ? '我' : 'AI' }}</span>
          <span class="bubble-text">{{ bubbleText }}</span>
        </div>
      </transition>

      <!-- FAB button -->
      <button
        class="voice-fab"
        :class="{ connected, recording: isRecording, playing: isPlaying }"
        @click="isExpanded = true"
      >
        <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
          <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm-1-9c0-.55.45-1 1-1s1 .45 1 1v6c0 .55-.45 1-1 1s-1-.45-1-1V5zm6 6c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
        </svg>
        <span v-if="connected" class="fab-dot"></span>
      </button>
    </div>

    <!-- Expanded panel -->
    <div v-if="isExpanded" class="voice-panel">
      <!-- Header -->
      <div class="panel-header">
        <div class="header-left">
          <span class="conn-dot" :class="{ connected }"></span>
          <span class="conn-text">{{ statusText }}</span>
        </div>
        <div class="header-right">
          <!-- Mode toggle -->
          <div class="mode-switch">
            <button
              class="mode-btn"
              :class="{ active: listenMode === 'auto' }"
              @click="setListenMode('auto')"
              title="持续对话"
            >持续</button>
            <button
              class="mode-btn"
              :class="{ active: listenMode === 'manual' }"
              @click="setListenMode('manual')"
              title="按键说话"
            >按键</button>
          </div>
          <button class="close-btn" @click="isExpanded = false" title="收起">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6z"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- Chat messages -->
      <div class="panel-messages" ref="messagesContainer">
        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="chat-msg"
          :class="msg.type"
        >
          {{ msg.text }}
        </div>
        <div v-if="messages.length === 0" class="chat-empty">
          语音助手已就绪
        </div>
      </div>

      <!-- Controls -->
      <div class="panel-controls">
        <!-- Auto mode: toggle button -->
        <template v-if="listenMode === 'auto'">
          <button
            class="mic-button"
            :class="{ active: isAutoListening, playing: isPlaying, disabled: !connected }"
            :disabled="!connected"
            @click="toggleAutoListen"
          >
            <svg v-if="!isAutoListening" viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
              <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm-1-9c0-.55.45-1 1-1s1 .45 1 1v6c0 .55-.45 1-1 1s-1-.45-1-1V5zm6 6c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
            </svg>
            <!-- Stop icon when listening -->
            <svg v-else viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
              <path d="M6 6h12v12H6z"/>
            </svg>
          </button>
          <span class="mic-hint">
            {{ isAutoListening ? '聆听中 · 点击停止' : isPlaying ? '正在回复...' : '点击开始对话' }}
          </span>
        </template>

        <!-- Manual mode: push-to-talk -->
        <template v-else>
          <button
            class="mic-button"
            :class="{ active: isRecording, playing: isPlaying, disabled: !connected }"
            :disabled="!connected"
            @mousedown.prevent="startListening"
            @mouseup.prevent="stopListening"
            @mouseleave="onMouseLeave"
            @touchstart.prevent="startListening"
            @touchend.prevent="stopListening"
          >
            <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
              <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm-1-9c0-.55.45-1 1-1s1 .45 1 1v6c0 .55-.45 1-1 1s-1-.45-1-1V5zm6 6c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
            </svg>
          </button>
          <span class="mic-hint">
            {{ isRecording ? '松开发送' : isPlaying ? '正在回复...' : '按住说话' }}
          </span>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useVoiceChat } from '@/composables/useVoiceChat'

const props = defineProps({
  username: { type: String, default: '' },
  autoConnect: { type: Boolean, default: true },
  autoListen: { type: Boolean, default: true },
  defaultMode: { type: String, default: 'auto' },
})

const router = useRouter()
const isExpanded = ref(false) // collapsed by default
const messagesContainer = ref(null)
const bubbleText = ref('')
const bubbleType = ref('ai') // 'user' | 'ai'
let bubbleTimer = null

const {
  connected,
  isRecording,
  isPlaying,
  isAutoListening,
  listenMode,
  messages,
  statusText,
  connect,
  startListening,
  stopListening,
  toggleAutoListen,
  setListenMode,
  setNavigateCallback,
  setAutoListenOnConnect,
} = useVoiceChat()

function onMouseLeave() {
  if (isRecording.value && listenMode.value === 'manual') stopListening()
}

// Show bubble for latest non-system message, auto-hide after 5s
watch(messages, (msgs) => {
  if (msgs.length === 0) return
  const last = msgs[msgs.length - 1]
  if (last.type === 'system') return

  // Only show bubble when panel is collapsed
  if (!isExpanded.value) {
    bubbleText.value = last.text
    bubbleType.value = last.type
    clearTimeout(bubbleTimer)
    bubbleTimer = setTimeout(() => {
      bubbleText.value = ''
    }, 6000)
  }
}, { deep: true })

// Clear bubble when panel opens
watch(isExpanded, (expanded) => {
  if (expanded) {
    bubbleText.value = ''
    clearTimeout(bubbleTimer)
  }
})

// Auto-scroll messages
watch(messages, async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}, { deep: true })

onMounted(() => {
  setNavigateCallback((service) => {
    if (service === 'menu') {
      router.replace('/robot/menu')
    } else if (service === 'data_import') {
      router.replace('/business')
    } else {
      router.replace({ path: '/robot/app', query: { service } })
    }
  })
  setAutoListenOnConnect(props.autoListen)
  setListenMode(props.defaultMode)
  if (props.autoConnect) {
    connect(props.username)
  }
})
</script>

<style lang="scss" scoped>
// ── Collapsed state ──
.voice-collapsed {
  position: fixed;
  inset: 0;
  z-index: 998;
  pointer-events: none; // allow click-through except on children
}

// Center bubble
.voice-bubble-center {
  position: fixed;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 999;
  max-width: 80%;
  min-width: 200px;
  padding: 16px 24px;
  background: rgba(20, 20, 36, 0.92);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  color: #e8e8f0;
  font-size: 17px;
  line-height: 1.6;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  cursor: pointer;
  pointer-events: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;

  &:hover {
    background: rgba(28, 28, 48, 0.95);
  }

  @media (max-width: 820px) and (orientation: portrait) {
    max-width: 90%;
    font-size: 16px;
    padding: 14px 20px;
    bottom: 90px;
  }
}

.bubble-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 1px;
  text-transform: uppercase;
}

.bubble-text {
  word-break: break-word;
}

.bubble-enter-active { animation: bubbleIn 0.35s ease-out; }
.bubble-leave-active { animation: bubbleOut 0.3s ease-in; }

@keyframes bubbleIn {
  from { opacity: 0; transform: translateX(-50%) translateY(16px) scale(0.92); }
  to { opacity: 1; transform: translateX(-50%) translateY(0) scale(1); }
}
@keyframes bubbleOut {
  from { opacity: 1; transform: translateX(-50%) scale(1); }
  to { opacity: 0; transform: translateX(-50%) scale(0.92); }
}

// FAB button (bottom-right)
.voice-fab {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 999;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.15);
  background: var(--ds-surface-container-low);
  color: rgba(255,255,255,0.5);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  transition: all 0.3s;
  pointer-events: auto;

  &:hover {
    transform: scale(1.05);
    border-color: rgba(0, 228, 160, 0.4);
  }

  &.connected {
    border-color: rgba(0, 228, 160, 0.4);
    color: #00e4a0;
  }

  &.recording {
    border-color: #00e4a0;
    box-shadow: 0 0 0 4px rgba(0, 228, 160, 0.15), 0 4px 16px rgba(0, 0, 0, 0.3);
    animation: fab-pulse 1.5s infinite;
  }

  &.playing {
    border-color: var(--ds-primary);
    color: var(--ds-primary);
  }

  .fab-dot {
    position: absolute;
    top: 3px;
    right: 3px;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #00e4a0;
    border: 2px solid var(--ds-surface-container-low);
  }

  @media (max-width: 820px) and (orientation: portrait) {
    bottom: 16px;
    right: 16px;
  }
}

@keyframes fab-pulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(0, 228, 160, 0.15), 0 4px 16px rgba(0, 0, 0, 0.3); }
  50% { box-shadow: 0 0 0 8px rgba(0, 228, 160, 0), 0 4px 16px rgba(0, 0, 0, 0.3); }
}

// ── Expanded panel ──
.voice-panel {
  position: fixed;
  bottom: 0;
  right: 0;
  width: 380px;
  height: 480px;
  z-index: 999;
  display: flex;
  flex-direction: column;
  background: var(--ds-surface-container-low);
  border-radius: 16px 16px 0 0;
  overflow: hidden;
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.35);
  animation: slideUp 0.3s ease-out;

  @media (max-width: 820px) and (orientation: portrait) {
    width: 100%;
    height: 55vh;
  }
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.conn-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #909399;
  transition: all 0.3s;

  &.connected {
    background: #00e4a0;
    box-shadow: 0 0 8px rgba(0, 228, 160, 0.5);
  }
}

.conn-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

// Mode toggle
.mode-switch {
  display: flex;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 6px;
  overflow: hidden;
}

.mode-btn {
  padding: 4px 10px;
  font-size: 11px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all 0.2s;

  &.active {
    background: rgba(0, 228, 160, 0.2);
    color: #00e4a0;
  }

  &:hover:not(.active) {
    color: rgba(255, 255, 255, 0.7);
  }
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;

  &:hover {
    color: rgba(255, 255, 255, 0.7);
    background: rgba(255, 255, 255, 0.08);
  }
}

// Messages
.panel-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  scroll-behavior: smooth;

  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.08); border-radius: 3px; }
}

.chat-msg {
  max-width: 85%;
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  animation: msgIn 0.25s ease-out;

  &.user {
    align-self: flex-end;
    background: rgba(0, 228, 160, 0.12);
    border: 1px solid rgba(0, 228, 160, 0.25);
    color: #00e4a0;
  }

  &.ai {
    align-self: flex-start;
    background: rgba(255, 255, 255, 0.06);
    color: #d8d8e4;
  }

  &.system {
    align-self: center;
    font-size: 11px;
    color: rgba(255, 255, 255, 0.25);
    padding: 4px 0;
  }
}

@keyframes msgIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.chat-empty {
  text-align: center;
  color: rgba(255, 255, 255, 0.2);
  font-size: 13px;
  margin-top: 40px;
}

// Controls
.panel-controls {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.mic-button {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  position: relative;
  -webkit-user-select: none;
  user-select: none;
  touch-action: none;

  &:hover:not(.disabled) {
    border-color: rgba(0, 228, 160, 0.35);
    color: rgba(255, 255, 255, 0.65);
  }

  &.active {
    border-color: #00e4a0;
    color: #00e4a0;
    box-shadow: 0 0 0 4px rgba(0, 228, 160, 0.12), 0 0 20px rgba(0, 228, 160, 0.2);

    &::after {
      content: '';
      position: absolute;
      inset: -8px;
      border-radius: 50%;
      border: 1.5px solid #00e4a0;
      opacity: 0;
      animation: mic-pulse 1.5s ease-out infinite;
    }
  }

  &.playing {
    border-color: var(--ds-primary);
    color: var(--ds-primary);
    opacity: 0.6;
  }

  &.disabled {
    opacity: 0.25;
    pointer-events: none;
  }
}

@keyframes mic-pulse {
  0% { transform: scale(1); opacity: 0.5; }
  100% { transform: scale(1.3); opacity: 0; }
}

.mic-hint {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 0.5px;
}

// ── Portrait (8-inch robot screen) overrides ──
@media (max-width: 820px) and (orientation: portrait) {
  .voice-fab {
    width: 64px;
    height: 64px;
    bottom: 20px;
    right: 20px;

    svg { width: 28px; height: 28px; }

    .fab-dot {
      width: 12px;
      height: 12px;
      top: 4px;
      right: 4px;
    }
  }

  .voice-panel {
    height: 60vh;
  }

  .panel-header {
    padding: 12px 16px;
  }

  .conn-text {
    font-size: 14px;
  }

  .mode-btn {
    padding: 6px 14px;
    font-size: 13px;
    min-height: 36px;
  }

  .close-btn {
    padding: 8px;
    svg { width: 20px; height: 20px; }
  }

  .panel-messages {
    padding: 14px 16px;
    gap: 10px;
  }

  .chat-msg {
    font-size: 16px;
    padding: 10px 14px;
    border-radius: 14px;
    max-width: 90%;

    &.system { font-size: 13px; }
  }

  .chat-empty {
    font-size: 15px;
  }

  .panel-controls {
    padding: 16px;
    gap: 8px;
  }

  .mic-button {
    width: 80px;
    height: 80px;

    svg { width: 36px; height: 36px; }

    &.active::after {
      inset: -10px;
    }
  }

  .mic-hint {
    font-size: 14px;
  }

  .voice-bubble-center {
    font-size: 18px;
    padding: 18px 24px;
    border-radius: 24px;
    max-width: 88%;
    bottom: 110px;
  }

  .bubble-label {
    font-size: 12px;
  }
}
</style>
