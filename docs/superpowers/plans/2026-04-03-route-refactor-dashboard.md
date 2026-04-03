# Route Refactor + Admin Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize routes so admin lives under `/admin`, business under `/business`, login defaults to `/business`, and admin gets a dashboard homepage with mock data.

**Architecture:** Frontend-only route restructure — backend menu API unchanged, frontend prepends `/admin` when mounting dynamic routes. New dashboard component uses EMDS-V2 design system with 4 stat cards (mock data). No chart libraries, pure CSS bars.

**Tech Stack:** Vue 3, Vue Router, Pinia, Element Plus, SCSS with EMDS-V2 `--ds-*` tokens

---

### File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `src/router/index.js` | Route definitions: `/app` -> `/business`, `/index` -> `/admin` |
| Modify | `src/permission.js` | Nav guards: default redirect `/app` -> `/business` |
| Modify | `src/store/modules/permission.js` | Dynamic route mounting: prepend `/admin` |
| Create | `src/views/admin/dashboard/index.vue` | Dashboard page with 4 cards |
| Modify | `src/views/login.vue` | Default redirect `/app` -> `/business` |
| Modify | `src/views/robot/menu.vue` | Data import link `/app` -> `/business` |
| Modify | `src/layout/AppLayout.vue` | Voice nav callback `/app` -> `/business` |
| Modify | `src/components/VoiceChatPanel/index.vue` | Voice nav `/app` -> `/business` |
| Modify | `src/layout/components/AppNavbar.vue` | Logo link `/index` -> `/admin` |
| Modify | `src/layout/components/Navbar.vue` | Logout redirect `/index` -> `/admin` |
| Modify | `src/layout/components/TagsView/index.vue` | Home path `/index` -> `/admin` |
| Modify | `src/components/Breadcrumb/index.vue` | Root breadcrumb `/index` -> `/admin` |
| Modify | `src/components/TopNav/index.vue` | Hide list `/index` -> `/admin` |
| Modify | `src/views/error/404.vue` | Return home link `/index` -> `/admin` |
| Modify | `src/utils/request.js` | 401 redirect `/index` -> `/admin` |

---

### Task 1: Rename static routes

**Files:**
- Modify: `src/router/index.js`

- [ ] **Step 1: Change `/app` to `/business`**

In `src/router/index.js`, line 72-75, change:

```javascript
  {
    path: '/business',
    component: AppLayout,
    hidden: true
  },
```

- [ ] **Step 2: Change `/index` to `/admin` and point to dashboard**

In `src/router/index.js`, lines 87-98, change:

```javascript
  {
    path: '',
    component: Layout,
    redirect: '/admin',
    children: [
      {
        path: '/admin',
        component: () => import('@/views/admin/dashboard/index'),
        name: 'AdminDashboard',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
```

- [ ] **Step 3: Move `/user/profile` under `/admin`**

In `src/router/index.js`, lines 99-112, change:

```javascript
  {
    path: '/admin/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
```

- [ ] **Step 4: Prepend `/admin` to static dynamic routes**

In `src/router/index.js`, lines 116-187, change all 5 dynamic route paths:

```javascript
export const dynamicRoutes = [
  {
    path: '/admin/system/user-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:user:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/admin/system/user' }
      }
    ]
  },
  {
    path: '/admin/system/role-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/admin/system/role' }
      }
    ]
  },
  {
    path: '/admin/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: '/admin/system/dict' }
      }
    ]
  },
  {
    path: '/admin/monitor/job-log',
    component: Layout,
    hidden: true,
    permissions: ['monitor:job:list'],
    children: [
      {
        path: 'index/:jobId(\\d+)',
        component: () => import('@/views/monitor/job/log'),
        name: 'JobLog',
        meta: { title: '调度日志', activeMenu: '/admin/monitor/job' }
      }
    ]
  },
  {
    path: '/admin/tool/gen-edit',
    component: Layout,
    hidden: true,
    permissions: ['tool:gen:edit'],
    children: [
      {
        path: 'index/:tableId(\\d+)',
        component: () => import('@/views/tool/gen/editTable'),
        name: 'GenEdit',
        meta: { title: '修改生成配置', activeMenu: '/admin/tool/gen' }
      }
    ]
  }
]
```

---

### Task 2: Update permission guard and dynamic route mounting

**Files:**
- Modify: `src/permission.js`
- Modify: `src/store/modules/permission.js`

- [ ] **Step 1: Update permission.js redirects**

In `src/permission.js`, line 27, change `/app` to `/business`:

```javascript
      next({ path: isRobotScreen() ? '/robot/menu' : '/business' })
```

- [ ] **Step 2: Update dynamic route mounting to prepend `/admin`**

In `src/store/modules/permission.js`, modify the `generateRoutes` action (lines 35-54). The key change: wrap backend routes in an `/admin` parent before adding them.

Replace the `generateRoutes` action:

```javascript
      generateRoutes(roles) {
        return new Promise(resolve => {
          // 向后端请求路由数据
          getRouters().then(res => {
            const sdata = JSON.parse(JSON.stringify(res.data))
            const rdata = JSON.parse(JSON.stringify(res.data))
            const defaultData = JSON.parse(JSON.stringify(res.data))
            const sidebarRoutes = filterAsyncRouter(sdata)
            const rewriteRoutes = filterAsyncRouter(rdata, false, true)
            const defaultRoutes = filterAsyncRouter(defaultData)
            const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
            asyncRoutes.forEach(route => { router.addRoute(route) })

            // Wrap backend routes under /admin prefix
            const adminSidebarRoutes = prependAdminPrefix(sidebarRoutes)
            const adminRewriteRoutes = prependAdminPrefix(rewriteRoutes)
            const adminDefaultRoutes = prependAdminPrefix(defaultRoutes)

            this.setRoutes(adminRewriteRoutes)
            this.setSidebarRouters(constantRoutes.concat(adminSidebarRoutes))
            this.setDefaultRoutes(adminSidebarRoutes)
            this.setTopbarRoutes(adminDefaultRoutes)
            resolve(adminRewriteRoutes)
          })
        })
      }
```

Then add the `prependAdminPrefix` helper function after `filterChildren`:

```javascript
function prependAdminPrefix(routes) {
  return routes.map(route => {
    const newRoute = { ...route }
    if (newRoute.path && !newRoute.path.startsWith('/admin')) {
      newRoute.path = '/admin' + (newRoute.path.startsWith('/') ? '' : '/') + newRoute.path
    }
    // Update activeMenu references in meta
    if (newRoute.children) {
      newRoute.children = newRoute.children.map(child => {
        const newChild = { ...child }
        if (newChild.meta && newChild.meta.activeMenu && !newChild.meta.activeMenu.startsWith('/admin')) {
          newChild.meta = { ...newChild.meta, activeMenu: '/admin' + newChild.meta.activeMenu }
        }
        return newChild
      })
    }
    return newRoute
  })
}
```

---

### Task 3: Update all hardcoded path references

**Files:**
- Modify: `src/views/login.vue:203`
- Modify: `src/views/robot/menu.vue:106`
- Modify: `src/layout/AppLayout.vue:137,143`
- Modify: `src/components/VoiceChatPanel/index.vue:199`
- Modify: `src/layout/components/AppNavbar.vue:70`
- Modify: `src/layout/components/Navbar.vue:88`
- Modify: `src/layout/components/TagsView/index.vue:102`
- Modify: `src/components/Breadcrumb/index.vue:37`
- Modify: `src/components/TopNav/index.vue:47`
- Modify: `src/views/error/404.vue:20`
- Modify: `src/utils/request.js:90`

- [ ] **Step 1: Replace `/app` references with `/business`**

`src/views/login.vue` line 203:
```javascript
        router.push({ path: redirect.value || "/business", query: otherQueryParams })
```

`src/views/robot/menu.vue` line 106:
```javascript
  router.push('/business')
```

`src/layout/AppLayout.vue` line 137:
```javascript
      router.replace('/business')
```

`src/layout/AppLayout.vue` line 143:
```javascript
        router.replace({ path: '/business', query: { service } })
```

`src/components/VoiceChatPanel/index.vue` line 199:
```javascript
      router.replace('/business')
```

- [ ] **Step 2: Replace `/index` references with `/admin`**

`src/layout/components/AppNavbar.vue` line 70:
```javascript
  router.push('/admin')
```

`src/layout/components/Navbar.vue` line 88:
```javascript
      location.href = '/admin'
```

`src/layout/components/TagsView/index.vue` line 102:
```javascript
    return selectedTag.value.fullPath === '/admin' || selectedTag.value.fullPath === visitedViews.value[1].fullPath
```

`src/components/Breadcrumb/index.vue` line 37:
```javascript
    matched = [{ path: "/admin", meta: { title: "首页" } }].concat(matched)
```

`src/components/TopNav/index.vue` line 47:
```javascript
const hideList = ['/admin', '/admin/user/profile']
```

`src/views/error/404.vue` line 20:
```html
        <router-link to="/admin" class="bullshit__return-home">
```

`src/utils/request.js` line 90:
```javascript
            location.href = '/admin'
```

- [ ] **Step 3: Verify no remaining `/app` or `/index` route references**

Run:
```bash
cd clda-ui/src && grep -rn "'/app'" --include="*.vue" --include="*.js" | grep -v node_modules | grep -v dist
cd clda-ui/src && grep -rn "'/index'" --include="*.vue" --include="*.js" | grep -v node_modules | grep -v dist
```

Expected: Only `router/index.js` definition of `/business` and `/admin`. No stale `/app` or `/index` references.

- [ ] **Step 4: Commit**

```bash
git add -A clda-ui/src/
git commit -m "refactor: reorganize routes — /app -> /business, /index -> /admin with prefix"
```

---

### Task 4: Create admin dashboard page

**Files:**
- Create: `src/views/admin/dashboard/index.vue`

- [ ] **Step 1: Create the dashboard directory**

```bash
mkdir -p clda-ui/src/views/admin/dashboard
```

- [ ] **Step 2: Create the dashboard component**

Create `src/views/admin/dashboard/index.vue`:

```vue
<template>
  <div class="dashboard">
    <h2 class="dashboard-title">
      <span class="title-text">CLDA 运营概览</span>
      <span class="title-date">{{ currentDate }}</span>
    </h2>

    <div class="dashboard-grid">
      <!-- Card 1: Equipment Overview -->
      <div class="dash-card">
        <div class="card-header">
          <el-icon :size="20" class="card-icon equip"><Monitor /></el-icon>
          <span class="card-label">设备统计概览</span>
        </div>
        <div class="card-body">
          <div class="big-number">{{ mock.equipment.total }}</div>
          <div class="big-label">设备总数</div>
          <div class="status-row">
            <div class="status-item" v-for="s in mock.equipment.statuses" :key="s.label">
              <span class="status-dot" :style="{ background: s.color }"></span>
              <span class="status-count">{{ s.count }}</span>
              <span class="status-label">{{ s.label }}</span>
            </div>
          </div>
          <div class="bar-track">
            <div
              v-for="s in mock.equipment.statuses" :key="'bar-'+s.label"
              class="bar-seg"
              :style="{ width: (s.count / mock.equipment.total * 100) + '%', background: s.color }"
            ></div>
          </div>
        </div>
      </div>

      <!-- Card 2: Data Processing -->
      <div class="dash-card">
        <div class="card-header">
          <el-icon :size="20" class="card-icon data"><DataAnalysis /></el-icon>
          <span class="card-label">数据处理统计</span>
        </div>
        <div class="card-body">
          <div class="metric-row">
            <div class="metric" v-for="m in mock.data.metrics" :key="m.label">
              <div class="metric-value">{{ m.value }}</div>
              <div class="metric-label">{{ m.label }}</div>
            </div>
          </div>
          <div class="trend-label">近7日上传趋势</div>
          <div class="trend-bars">
            <div v-for="(v, i) in mock.data.trend" :key="i" class="trend-col">
              <div class="trend-bar" :style="{ height: (v / mock.data.trendMax * 100) + '%' }"></div>
              <span class="trend-day">{{ mock.data.days[i] }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Card 3: AI Assistant Usage -->
      <div class="dash-card">
        <div class="card-header">
          <el-icon :size="20" class="card-icon ai"><Cpu /></el-icon>
          <span class="card-label">AI助手使用情况</span>
        </div>
        <div class="card-body">
          <div class="ai-row" v-for="a in mock.ai" :key="a.name">
            <span class="ai-name">{{ a.name }}</span>
            <div class="ai-bar-track">
              <div class="ai-bar" :style="{ width: (a.count / mock.aiMax * 100) + '%', background: a.color }"></div>
            </div>
            <span class="ai-count">{{ a.count }}</span>
          </div>
        </div>
      </div>

      <!-- Card 4: System Status -->
      <div class="dash-card">
        <div class="card-header">
          <el-icon :size="20" class="card-icon sys"><Odometer /></el-icon>
          <span class="card-label">系统运行状态</span>
        </div>
        <div class="card-body">
          <div class="sys-grid">
            <div class="sys-item" v-for="s in mock.system" :key="s.label">
              <span class="sys-dot" :class="s.status"></span>
              <div class="sys-info">
                <div class="sys-value">{{ s.value }}</div>
                <div class="sys-label">{{ s.label }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="AdminDashboard">
import { computed } from 'vue'
import { Monitor, DataAnalysis, Cpu, Odometer } from '@element-plus/icons-vue'

const currentDate = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

const mock = {
  equipment: {
    total: 128,
    statuses: [
      { label: '正常', count: 96, color: 'var(--ds-success)' },
      { label: '警告', count: 18, color: 'var(--ds-warning)' },
      { label: '故障', count: 8, color: 'var(--ds-error)' },
      { label: '停用', count: 6, color: 'var(--ds-outline)' },
    ]
  },
  data: {
    metrics: [
      { label: '今日上传', value: '47' },
      { label: 'OCR成功率', value: '94.2%' },
      { label: 'AI结构化率', value: '87.5%' },
    ],
    trend: [32, 28, 41, 35, 52, 47, 39],
    trendMax: 52,
    days: ['一', '二', '三', '四', '五', '六', '日'],
  },
  ai: [
    { name: '数据服务', count: 342, color: 'var(--ds-emerald)' },
    { name: '问题处理', count: 156, color: 'var(--ds-indigo)' },
    { name: '隐患排查', count: 89, color: 'var(--ds-amber)' },
    { name: '风险服务', count: 67, color: 'var(--ds-orange)' },
    { name: '前沿知识', count: 234, color: 'var(--ds-rose)' },
    { name: '法规标准', count: 278, color: 'var(--ds-slate)' },
  ],
  aiMax: 342,
  system: [
    { label: '在线用户', value: '12', status: 'ok' },
    { label: 'CPU使用率', value: '34%', status: 'ok' },
    { label: '内存使用率', value: '62%', status: 'ok' },
    { label: 'Redis键数', value: '1.2k', status: 'ok' },
  ]
}
</script>

<style lang="scss" scoped>
.dashboard {
  padding: var(--ds-space-6);
  background: var(--ds-surface);
  min-height: 100%;
}

.dashboard-title {
  display: flex;
  align-items: baseline;
  gap: var(--ds-space-3);
  margin: 0 0 var(--ds-space-6) 0;
}

.title-text {
  font-size: 22px;
  font-weight: 700;
  color: var(--ds-on-surface);
  font-family: var(--ds-font-display);
}

.title-date {
  font-size: 13px;
  color: var(--ds-outline);
  font-weight: 400;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--ds-space-5);
}

@media (max-width: 900px) {
  .dashboard-grid { grid-template-columns: 1fr; }
}

/* ===== Card ===== */
.dash-card {
  background: var(--ds-surface-container-lowest);
  border-radius: var(--ds-radius-lg);
  box-shadow: var(--ds-shadow-sm);
  padding: var(--ds-space-5);
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
  margin-bottom: var(--ds-space-5);
}

.card-icon {
  padding: 6px;
  border-radius: var(--ds-radius);
  color: #fff;
  &.equip { background: var(--ds-primary); }
  &.data { background: var(--ds-emerald); }
  &.ai { background: var(--ds-indigo); }
  &.sys { background: var(--ds-slate); }
}

.card-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--ds-on-surface);
  font-family: var(--ds-font-display);
}

/* ===== Equipment Card ===== */
.big-number {
  font-size: 40px;
  font-weight: 800;
  color: var(--ds-on-surface);
  font-family: var(--ds-font-display);
  line-height: 1;
}

.big-label {
  font-size: 13px;
  color: var(--ds-outline);
  margin-bottom: var(--ds-space-4);
}

.status-row {
  display: flex;
  gap: var(--ds-space-5);
  margin-bottom: var(--ds-space-3);
}

.status-item {
  display: flex;
  align-items: center;
  gap: var(--ds-space-1);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-count {
  font-size: 15px;
  font-weight: 700;
  color: var(--ds-on-surface);
}

.status-label {
  font-size: 12px;
  color: var(--ds-on-surface-variant);
}

.bar-track {
  display: flex;
  height: 8px;
  border-radius: var(--ds-radius-full);
  overflow: hidden;
  background: var(--ds-surface-container-low);
}

.bar-seg {
  height: 100%;
  transition: width 0.3s;
}

/* ===== Data Processing Card ===== */
.metric-row {
  display: flex;
  gap: var(--ds-space-5);
  margin-bottom: var(--ds-space-5);
}

.metric {
  flex: 1;
  text-align: center;
  padding: var(--ds-space-3);
  background: var(--ds-surface-container-low);
  border-radius: var(--ds-radius);
}

.metric-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--ds-on-surface);
  font-family: var(--ds-font-display);
}

.metric-label {
  font-size: 12px;
  color: var(--ds-on-surface-variant);
  margin-top: 2px;
}

.trend-label {
  font-size: 12px;
  color: var(--ds-outline);
  margin-bottom: var(--ds-space-2);
}

.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: var(--ds-space-2);
  height: 80px;
}

.trend-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.trend-bar {
  width: 100%;
  background: var(--ds-primary);
  border-radius: var(--ds-radius-sm) var(--ds-radius-sm) 0 0;
  min-height: 4px;
  transition: height 0.3s;
}

.trend-day {
  font-size: 11px;
  color: var(--ds-outline);
  margin-top: 4px;
}

/* ===== AI Assistant Card ===== */
.ai-row {
  display: flex;
  align-items: center;
  gap: var(--ds-space-3);
  margin-bottom: var(--ds-space-3);
  &:last-child { margin-bottom: 0; }
}

.ai-name {
  width: 56px;
  font-size: 13px;
  color: var(--ds-on-surface-variant);
  flex-shrink: 0;
  text-align: right;
}

.ai-bar-track {
  flex: 1;
  height: 18px;
  background: var(--ds-surface-container-low);
  border-radius: var(--ds-radius-full);
  overflow: hidden;
}

.ai-bar {
  height: 100%;
  border-radius: var(--ds-radius-full);
  transition: width 0.3s;
}

.ai-count {
  width: 36px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-on-surface);
  text-align: right;
}

/* ===== System Status Card ===== */
.sys-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--ds-space-4);
}

.sys-item {
  display: flex;
  align-items: center;
  gap: var(--ds-space-3);
  padding: var(--ds-space-3) var(--ds-space-4);
  background: var(--ds-surface-container-low);
  border-radius: var(--ds-radius);
}

.sys-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  &.ok { background: var(--ds-success); }
  &.warn { background: var(--ds-warning); }
  &.error { background: var(--ds-error); }
}

.sys-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--ds-on-surface);
  font-family: var(--ds-font-display);
}

.sys-label {
  font-size: 12px;
  color: var(--ds-on-surface-variant);
}
</style>
```

- [ ] **Step 3: Commit**

```bash
git add clda-ui/src/views/admin/
git commit -m "feat: add admin dashboard page with 4 mock data cards"
```

---

### Task 5: Build verification and final check

- [ ] **Step 1: Build the frontend**

```bash
cd clda-ui && npx vite build
```

Expected: Build succeeds with no errors.

- [ ] **Step 2: Verify no stale route references**

```bash
cd clda-ui/src && grep -rn "'/app'" --include="*.vue" --include="*.js"
cd clda-ui/src && grep -rn "'/index'" --include="*.vue" --include="*.js"
```

Expected: No matches (only `/business` and `/admin` references remain).

- [ ] **Step 3: Final commit**

```bash
git add -A clda-ui/
git commit -m "refactor: complete route reorganization /admin + /business + dashboard"
```
