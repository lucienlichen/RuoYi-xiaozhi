# Route Refactor + Admin Dashboard Design

## Problem

The current routing is disorganized:
- Business app is at `/app`, admin is at `/index`, dynamic admin routes (`/system/*`, `/monitor/*`) have no common prefix
- After login, redirect targets are inconsistent
- Admin homepage is empty (just a placeholder)

## Goals

1. Unify admin routes under `/admin`, business routes under `/business`
2. All logins default-redirect to `/business`
3. Admin homepage gets a dashboard with 4 data panels (mock data for now)

---

## Route Structure

### Before vs After

| Before | After | Layout |
|--------|-------|--------|
| `/login` | `/login` | (none) |
| `/register` | `/register` | (none) |
| `/app` | `/business` | AppLayout |
| `/index` | `/admin` | Layout (dashboard) |
| `/system/user` | `/admin/system/user` | Layout |
| `/monitor/online` | `/admin/monitor/online` | Layout |
| `/tool/gen` | `/admin/tool/gen` | Layout |
| `/user/profile` | `/admin/user/profile` | Layout |
| `/robot/*` | `/robot/*` | RobotLayout (unchanged) |
| `/:pathMatch(.*)*` | `/:pathMatch(.*)*` | 404 (unchanged) |

### Implementation: Frontend prefix injection

Backend menu data returns paths like `/system/user`. The frontend will prepend `/admin` when generating routes, so backend is unaffected.

In `store/modules/permission.js`, the `generateRoutes` action will:
1. Fetch backend routes via `GET /getRouters`
2. Process component mappings as before
3. Mount all backend routes as children of a new `/admin` parent route (using `Layout` component)

This replaces the current approach where backend routes are mounted at `/` with `Layout`.

---

## Files to Modify

### Router (`src/router/index.js`)
- Rename `/app` route to `/business` (same AppLayout component)
- Rename `/index` route to `/admin` with new dashboard component
- Move `/user/profile` under `/admin/user/profile`
- Remove the old `/index` constant route (it becomes part of admin)
- Keep `/redirect`, `/login`, `/register`, `/robot/*`, `/401`, `404` unchanged

### Permission guard (`src/permission.js`)
- Change default redirect: `/app` -> `/business`
- Change logged-in-but-at-login redirect: `/app` -> `/business`
- WhiteList unchanged (`/login`, `/register`, `/robot/login`)

### Permission store (`src/store/modules/permission.js`)
- Dynamic routes from backend: mount under `/admin` parent instead of `/`
- Static dynamic routes (role-auth, dict-data, job-log, gen-edit): prepend `/admin`

### Login (`src/views/login.vue`)
- Default redirect: `redirect.value || "/business"`

### All hardcoded path references
Search and replace across codebase:
- `'/app'` -> `'/business'` (in navigation, voice chat callbacks, etc.)
- `'/index'` -> `'/admin'` (in any admin redirects)

Key files with `/app` references:
- `src/layout/AppLayout.vue` (voice chat navigate callback)
- `src/layout/components/AppNavbar.vue` (logo link)
- `src/views/robot/menu.vue` (data import link)
- `src/permission.js` (multiple redirect targets)
- `src/views/login.vue` (default redirect)

---

## Admin Dashboard

### Component
**File**: `src/views/admin/dashboard/index.vue` (new)

### Layout
2x2 card grid using EMDS-V2 design tokens:
```
+----------------------------+----------------------------+
|   Equipment Overview       |   Data Processing Stats    |
|   (设备统计概览)             |   (数据处理统计)             |
+----------------------------+----------------------------+
|   AI Assistant Usage       |   System Status            |
|   (AI助手使用情况)           |   (系统运行状态)             |
+----------------------------+----------------------------+
```

### Card 1: Equipment Overview (设备统计概览)
- Large number: total equipment count
- 4 status badges: NORMAL (green), WARNING (amber), FAULT (red), STOPPED (slate)
- Horizontal bar showing status distribution
- Mock data: 128 total, 96 normal, 18 warning, 8 fault, 6 stopped

### Card 2: Data Processing Stats (数据处理统计)
- 3 metric tiles: Today uploads, OCR success rate, AI structuring rate
- Simple bar chart showing last 7 days trend (CSS-only, no chart library)
- Mock data: 47 today, 94.2% OCR, 87.5% AI

### Card 3: AI Assistant Usage (AI助手使用情况)
- 6 horizontal bars, one per AI assistant, using their EMDS-V2 accent colors
- Shows relative usage count
- Mock data: Data Service highest, Regulations second, others varied

### Card 4: System Status (系统运行状态)
- 4 metric items: Online users, CPU, Memory, Redis keys
- Status indicators (green dot = healthy)
- Mock data: 12 online, CPU 34%, Memory 62%, Redis 1.2k keys

### Styling
- Cards use `var(--ds-surface-container-lowest)` background, `var(--ds-shadow-sm)`, `border-radius: var(--ds-radius-lg)`
- No-Line Rule: no borders between cards, rely on gap + background contrast
- Typography: `var(--ds-font-display)` for large numbers, `var(--ds-font-body)` for labels
- Colors: EMDS-V2 palette throughout, AI bars use their assigned colors
- Dark mode: fully supported via `--ds-*` tokens (no hardcoded colors)
- Responsive: 2x2 on desktop, single column on mobile/portrait

---

## Verification

1. `npm run dev` -> navigate to `/login` -> login -> redirects to `/business`
2. `/business` loads AppLayout with equipment sidebar + AI panels
3. Navigate to `/admin` -> loads dashboard with 4 cards
4. `/admin/system/user` -> loads user management (dynamic route with prefix)
5. `/robot/login` -> face login -> `/robot/menu` (unchanged)
6. All hardcoded `/app` references replaced
7. Build passes with no errors
