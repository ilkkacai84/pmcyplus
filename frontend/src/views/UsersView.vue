<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { apiMessage, http } from '@/api/http'
import type { Role, UserSummary } from '@/api/types'

const users = ref<UserSummary[]>([])
const error = ref('')
const form = reactive({ username: '', password: '', displayName: '', userType: 'INTERNAL', roles: ['MEMBER'] as Role[] })
const roleOptions: { value: Role; label: string }[] = [{ value: 'ADMIN', label: '管理员' }, { value: 'PROJECT_MANAGER', label: '项目经理' }, { value: 'MEMBER', label: '项目成员' }, { value: 'DEPARTMENT_MANAGER', label: '部门负责人' }, { value: 'CUSTOMER', label: '客户' }]
async function load() { users.value = (await http.get('/users')).data }
async function createUser() {
  error.value = ''
  try { await http.get('/auth/csrf'); await http.post('/users', form); form.username = ''; form.password = ''; form.displayName = ''; await load() }
  catch (e) { error.value = apiMessage(e) }
}
onMounted(load)
</script>

<template>
  <div class="page-heading"><div><span class="eyebrow">ACCOUNTS</span><h1>账号管理</h1><p>第一版由管理员创建内部员工和客户账号。</p></div></div>
  <form class="panel form-grid" @submit.prevent="createUser">
    <label>登录账号<input v-model="form.username" required /></label><label>显示名称<input v-model="form.displayName" required /></label><label>初始密码<input v-model="form.password" type="password" minlength="10" required /></label>
    <label>账号类型<select v-model="form.userType"><option value="INTERNAL">内部用户</option><option value="CUSTOMER">外部客户</option></select></label>
    <fieldset class="span-2"><legend>角色（可多选）</legend><label v-for="role in roleOptions" :key="role.value" class="check-label"><input v-model="form.roles" type="checkbox" :value="role.value" />{{ role.label }}</label></fieldset>
    <p v-if="error" class="error-message span-2">{{ error }}</p><div class="form-actions span-2"><button class="primary-button">创建账号</button></div>
  </form>
  <section class="panel table-panel"><div class="table-row user-row table-head"><span>用户</span><span>账号</span><span>类型</span><span>角色</span></div><div v-for="user in users" :key="user.id" class="table-row user-row"><span><strong>{{ user.displayName }}</strong></span><span>{{ user.username }}</span><span>{{ user.userType === 'INTERNAL' ? '内部' : '客户' }}</span><span>{{ user.roles.join(' · ') }}</span></div></section>
</template>
