<template>
  <div class="app-navbar">
    <div class="navbar-left">
      <h1 class="navbar-title">起重装备全生命周期数据智AI能体</h1>
    </div>
    <div class="navbar-center">
      <div class="navbar-search-wrap">
        <el-icon class="search-icon"><Search /></el-icon>
        <input
          v-model="searchKeyword"
          class="navbar-search-input"
          placeholder="搜索设备或数据..."
          @input="onSearch"
        />
      </div>
    </div>
    <div class="navbar-right">
      <button class="nav-icon-btn" @click="goAdmin" v-if="isAdmin" title="高级管理员">
        <el-icon><Setting /></el-icon>
      </button>
      <div class="nav-divider"></div>
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <span class="user-name">{{ nickName }}</span>
          <img :src="avatar" class="user-avatar" />
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Setting, Search, ArrowDown } from '@element-plus/icons-vue'
import useUserStore from '@/store/modules/user'
import useEquipmentStore from '@/store/modules/equipment'
import defAva from '@/assets/images/profile.jpg'

const router = useRouter()
const userStore = useUserStore()
const equipmentStore = useEquipmentStore()

const searchKeyword = ref('')
const nickName = computed(() => userStore.nickName || userStore.name || '用户')
const avatar = computed(() => userStore.avatar || defAva)
const isAdmin = computed(() => userStore.roles.includes('admin'))

function onSearch() {
  equipmentStore.equipSearch = searchKeyword.value
}

function goAdmin() {
  router.push('/index')
}

function handleCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' }).then(() => {
      userStore.logOut().then(() => router.push('/login'))
    }).catch(() => {})
  } else if (command === 'profile') {
    router.push('/user/profile')
  }
}
</script>

<style lang="scss" scoped>
.app-navbar {
  height: 64px;
  background: #0D47A1;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 16px;
  box-shadow: 0 4px 12px rgba(13, 71, 161, 0.2);
  z-index: 50;
}

.navbar-left { flex-shrink: 0; }

.navbar-title {
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.navbar-center {
  flex: 1;
  max-width: 360px;
}

.navbar-search-wrap {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 6px 12px;
  gap: 8px;
  transition: background 0.2s;

  &:focus-within {
    background: rgba(255, 255, 255, 0.18);
  }
}

.search-icon {
  color: rgba(187, 222, 251, 0.5);
  font-size: 16px;
  flex-shrink: 0;
}

.navbar-search-input {
  border: none;
  background: transparent;
  color: #fff;
  font-size: 14px;
  width: 100%;
  outline: none;

  &::placeholder {
    color: rgba(187, 222, 251, 0.5);
  }
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-left: auto;
}

.nav-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #fff;
  cursor: pointer;
  transition: background 0.2s;
  font-size: 18px;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }
}

.nav-divider {
  width: 1px;
  height: 24px;
  background: rgba(255, 255, 255, 0.2);
  margin: 0 4px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;

  &:hover { background: rgba(255, 255, 255, 0.1); }
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
}
</style>
