<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const username = ref('admin')
const password = ref('Admin@123456')
const loading = ref(false)
const error = ref('')
const auth = useAuthStore()
const router = useRouter()

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    await router.push('/')
  } catch (e) {
    error.value = apiMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-story">
      <div class="story-label">PROJECT DELIVERY OS</div>
      <h1>让每一次协作<br />都有清晰的下一步</h1>
      <p>统一需求、里程碑、任务与交付验收，让跨部门项目真正透明。</p>
      <div class="story-stats">
        <div><strong>12h</strong><span>逾期升级周期</span></div>
        <div><strong>1</strong><span>统一项目视图</span></div>
        <div><strong>100%</strong><span>关键操作留痕</span></div>
      </div>
    </section>
    <section class="login-panel">
      <form class="login-card" @submit.prevent="submit">
        <span class="brand-mark large">PM</span>
        <div><h2>欢迎回来</h2><p>使用企业账号进入项目空间</p></div>
        <label>账号<input v-model="username" autocomplete="username" required /></label>
        <label>密码<input v-model="password" type="password" autocomplete="current-password" required /></label>
        <p v-if="error" class="error-message">{{ error }}</p>
        <button class="primary-button full" :disabled="loading">{{ loading ? '正在登录…' : '登录' }}</button>
        <small class="login-hint">初始账号由管理员创建，首次启动可使用页面预填的演示管理员。</small>
      </form>
    </section>
  </main>
</template>
