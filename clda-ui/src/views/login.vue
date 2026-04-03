<template>
  <div class="login">
    <!-- Circuit board decorative lines -->
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
      </svg>
    </div>

    <!-- Login card -->
    <div class="login-card">
      <!-- Glowing border effect -->
      <div class="card-glow"></div>

      <!-- Robot icon -->
      <div class="robot-icon">
        <svg viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
          <!-- Robot head -->
          <rect x="20" y="20" width="40" height="32" rx="6" stroke="url(#iconGrad)" stroke-width="2" fill="rgba(14,30,60,0.8)" />
          <!-- Eyes -->
          <circle cx="32" cy="36" r="4" fill="#38bdf8" opacity="0.9" />
          <circle cx="48" cy="36" r="4" fill="#38bdf8" opacity="0.9" />
          <!-- Eye glow -->
          <circle cx="32" cy="36" r="6" fill="#38bdf8" opacity="0.15" />
          <circle cx="48" cy="36" r="6" fill="#38bdf8" opacity="0.15" />
          <!-- Antenna -->
          <line x1="40" y1="20" x2="40" y2="12" stroke="url(#iconGrad)" stroke-width="2" />
          <circle cx="40" cy="10" r="3" fill="#38bdf8" opacity="0.7" />
          <circle cx="40" cy="10" r="5" fill="#38bdf8" opacity="0.15" />
          <!-- Side antennas -->
          <line x1="20" y1="30" x2="14" y2="26" stroke="url(#iconGrad)" stroke-width="1.5" />
          <circle cx="13" cy="25" r="2" fill="#06b6d4" opacity="0.6" />
          <line x1="60" y1="30" x2="66" y2="26" stroke="url(#iconGrad)" stroke-width="1.5" />
          <circle cx="67" cy="25" r="2" fill="#06b6d4" opacity="0.6" />
          <!-- Body -->
          <rect x="26" y="54" width="28" height="14" rx="4" stroke="url(#iconGrad)" stroke-width="1.5" fill="rgba(14,30,60,0.6)" />
          <!-- Body detail lines -->
          <line x1="33" y1="58" x2="47" y2="58" stroke="#38bdf8" stroke-width="1" opacity="0.4" />
          <line x1="33" y1="62" x2="47" y2="62" stroke="#38bdf8" stroke-width="1" opacity="0.3" />
          <!-- Arms -->
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

      <!-- Login form -->
      <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            size="large"
            auto-complete="off"
            placeholder="用户名"
          >
            <template #prefix>
              <svg-icon icon-class="user" class="input-icon" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            size="large"
            auto-complete="off"
            placeholder="密码"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <svg-icon icon-class="password" class="input-icon" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="code" v-if="captchaEnabled">
          <div class="captcha-row">
            <el-input
              v-model="loginForm.code"
              size="large"
              auto-complete="off"
              placeholder="验证码"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <svg-icon icon-class="validCode" class="input-icon" />
              </template>
            </el-input>
            <div class="login-code" @click="getCode">
              <img :src="codeUrl" class="login-code-img" />
            </div>
          </div>
        </el-form-item>
        <el-form-item class="remember-row">
          <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button
            :loading="loading"
            size="large"
            class="login-btn"
            @click.prevent="handleLogin"
          >
            <span v-if="!loading">登录</span>
            <span v-else>登录中...</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'

const title = import.meta.env.VITE_APP_TITLE
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: "",
  password: "",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)
const register = ref(false)
const redirect = ref(undefined)

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/business", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCode()
getCookie()
</script>

<style lang="scss" scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100vw;
  background: radial-gradient(ellipse at 50% 50%, #0c1f3d 0%, #070e1a 100%);
  position: relative;
  overflow: hidden;
}

/* Circuit board decorative lines */
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

/* Login card */
.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 40px 36px 28px;
  background: rgba(10, 22, 46, 0.85);
  border: 1px solid rgba(56, 189, 248, 0.15);
  border-radius: 16px;
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

/* Robot icon */
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

/* Title */
.title {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: #e0f2fe;
  margin: 0 0 28px 0;
  letter-spacing: 2px;
  text-shadow: 0 0 20px rgba(56, 189, 248, 0.3);
}

/* Form styling */
.login-form {
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-input) {
    --el-input-bg-color: rgba(15, 30, 58, 0.8);
    --el-input-border-color: rgba(56, 189, 248, 0.2);
    --el-input-hover-border-color: rgba(56, 189, 248, 0.4);
    --el-input-focus-border-color: #38bdf8;
    --el-input-text-color: #e0f2fe;
    --el-input-placeholder-color: rgba(148, 163, 184, 0.6);

    .el-input__wrapper {
      background: var(--el-input-bg-color);
      border-radius: 8px;
      box-shadow: 0 0 0 1px var(--el-input-border-color) inset;
      padding: 4px 12px;
      transition: all 0.3s ease;

      &:hover {
        box-shadow: 0 0 0 1px var(--el-input-hover-border-color) inset;
      }

      &.is-focus {
        box-shadow: 0 0 0 1px var(--el-input-focus-border-color) inset,
                    0 0 12px rgba(56, 189, 248, 0.15);
      }
    }
  }

  .input-icon {
    color: rgba(56, 189, 248, 0.6);
    width: 16px;
    height: 16px;
  }

  /* Captcha row */
  .captcha-row {
    display: flex;
    gap: 12px;
    width: 100%;

    .el-input {
      flex: 1;
    }
  }

  .login-code {
    width: 110px;
    height: 40px;
    flex-shrink: 0;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid rgba(56, 189, 248, 0.2);
    cursor: pointer;
    transition: border-color 0.3s;

    &:hover {
      border-color: rgba(56, 189, 248, 0.5);
    }
  }

  .login-code-img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
  }

  /* Remember password */
  .remember-row {
    margin-bottom: 8px !important;

    :deep(.el-checkbox) {
      --el-checkbox-text-color: rgba(148, 163, 184, 0.8);
      --el-checkbox-input-border: rgba(56, 189, 248, 0.3);

      .el-checkbox__label {
        color: rgba(148, 163, 184, 0.8);
        font-size: 13px;
      }

      .el-checkbox__inner {
        background: rgba(15, 30, 58, 0.8);
        border-color: rgba(56, 189, 248, 0.3);
      }

      .el-checkbox__input.is-checked .el-checkbox__inner {
        background: #38bdf8;
        border-color: #38bdf8;
      }
    }
  }

  /* Login button */
  .login-btn {
    width: 100%;
    height: 44px;
    border: none;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 4px;
    color: #fff;
    background: linear-gradient(135deg, #1e6baf 0%, #0ea5e9 40%, #06b6d4 70%, #22d3ee 100%);
    box-shadow: 0 4px 20px rgba(14, 165, 233, 0.3);
    transition: all 0.3s ease;
    cursor: pointer;

    &:hover {
      box-shadow: 0 6px 28px rgba(14, 165, 233, 0.45);
      transform: translateY(-1px);
    }

    &:active {
      transform: translateY(0);
      box-shadow: 0 2px 12px rgba(14, 165, 233, 0.3);
    }
  }
}

/* Responsive */
@media (max-width: 480px) {
  .login-card {
    width: calc(100vw - 32px);
    padding: 32px 24px 24px;
  }
}
</style>
