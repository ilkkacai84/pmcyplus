<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const menuOpen = ref(false)
const isAdmin = computed(() => auth.user?.roles.includes('ADMIN'))
const isDepartmentManager = computed(() => auth.user?.roles.includes('DEPARTMENT_MANAGER'))

async function signOut() {
  await auth.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ open: menuOpen }">
      <div class="brand">
        <span class="brand-mark">PM</span>
        <div><strong>项目管理平台</strong><small>Delivery Workspace</small></div>
      </div>
      <nav @click="menuOpen = false">
        <RouterLink to="/" exact-active-class="active"><span>◫</span> 工作台</RouterLink>
        <RouterLink to="/requirements" active-class="active"><span>◇</span> 需求中心</RouterLink>
        <RouterLink to="/projects" active-class="active"><span>▦</span> 项目中心</RouterLink>
        <RouterLink v-if="isDepartmentManager" to="/department" active-class="active"><span>◉</span> 部门视图</RouterLink>
        <RouterLink v-if="isAdmin" to="/users" active-class="active"><span>◎</span> 账号管理</RouterLink>
        <RouterLink v-if="isAdmin" to="/audit" active-class="active"><span>◌</span> 审计日志</RouterLink>
      </nav>
      <div class="sidebar-note">
        <strong>第一版 MVP</strong>
        <span>需求、项目、任务和验收主链路</span>
      </div>
    </aside>
    <main class="main-area">
      <header class="topbar">
        <button class="menu-button" @click="menuOpen = !menuOpen">☰</button>
        <div class="breadcrumb">企业项目协作空间</div>
        <div class="user-menu">
          <span class="avatar">{{ auth.user?.displayName.slice(0, 1) }}</span>
          <div><strong>{{ auth.user?.displayName }}</strong><small>{{ auth.user?.roles.join(' · ') }}</small></div>
          <button class="link-button" @click="signOut">退出</button>
        </div>
      </header>
      <section class="page-container"><RouterView /></section>
    </main>
  </div>
</template>
