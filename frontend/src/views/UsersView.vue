<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { apiMessage, http, prepareCsrf } from '@/api/http'
import type { DepartmentSummary, Role, UserSummary } from '@/api/types'

const users = ref<UserSummary[]>([])
const departments = ref<DepartmentSummary[]>([])
const error = ref('')
const form = reactive({ username: '', password: '', displayName: '', email: '', userType: 'INTERNAL', roles: ['MEMBER'] as Role[], departmentId: undefined as number | undefined })
const departmentForm = reactive({ name: '', parentId: undefined as number | undefined, managerId: undefined as number | undefined })
const departmentManagers = computed(() => users.value.filter(user => user.userType === 'INTERNAL' && user.roles.includes('DEPARTMENT_MANAGER')))
const roleOptions: { value: Role; label: string }[] = [
  { value: 'ADMIN', label: '管理员' }, { value: 'PROJECT_MANAGER', label: '项目经理' },
  { value: 'MEMBER', label: '项目成员' }, { value: 'DEPARTMENT_MANAGER', label: '部门负责人' },
  { value: 'CUSTOMER', label: '客户' },
]

async function load() {
  const [userResult, departmentResult] = await Promise.all([http.get('/users'), http.get('/departments')])
  users.value = userResult.data
  departments.value = departmentResult.data
}

async function save(path: string, payload: object, reset: () => void) {
  error.value = ''
  try {
    await prepareCsrf()
    await http.post(path, payload)
    reset()
    await load()
  } catch (e) { error.value = apiMessage(e) }
}

function createUser() {
  return save('/users', { ...form, departmentId: form.userType === 'CUSTOMER' ? null : form.departmentId ?? null }, () => {
    form.username = ''; form.password = ''; form.displayName = ''; form.email = ''
  })
}

function createDepartment() {
  return save('/departments', {
    name: departmentForm.name,
    parentId: departmentForm.parentId ?? null,
    managerId: departmentForm.managerId ?? null,
  }, () => { departmentForm.name = ''; departmentForm.parentId = undefined; departmentForm.managerId = undefined })
}

onMounted(load)
</script>

<template>
  <div class="page-heading"><div><span class="eyebrow">ORGANIZATION</span><h1>组织与账号</h1><p>维护部门层级，并由管理员创建内部员工和客户账号。</p></div></div>

  <form class="panel form-grid" @submit.prevent="createDepartment">
    <div class="span-2"><span class="eyebrow">DEPARTMENT</span><h2>创建部门</h2></div>
    <label>部门名称<input v-model="departmentForm.name" required /></label>
    <label>上级部门<select v-model="departmentForm.parentId"><option :value="undefined">顶级部门</option><option v-for="item in departments" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
    <label>部门负责人<select v-model="departmentForm.managerId"><option :value="undefined">暂不指定</option><option v-for="user in departmentManagers" :key="user.id" :value="user.id">{{ user.displayName }}</option></select></label>
    <div class="form-actions span-2"><button class="primary-button">创建部门</button></div>
  </form>

  <form class="panel form-grid" @submit.prevent="createUser">
    <div class="span-2"><span class="eyebrow">ACCOUNT</span><h2>创建账号</h2></div>
    <label>登录账号<input v-model="form.username" required /></label><label>显示名称<input v-model="form.displayName" required /></label>
    <label>邮箱<input v-model="form.email" type="email" /></label><label>初始密码<input v-model="form.password" type="password" minlength="10" required /></label>
    <label>账号类型<select v-model="form.userType"><option value="INTERNAL">内部用户</option><option value="CUSTOMER">外部客户</option></select></label>
    <label>所属部门<select v-model="form.departmentId" :disabled="form.userType === 'CUSTOMER'"><option :value="undefined">未分配</option><option v-for="item in departments" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
    <fieldset class="span-2"><legend>角色（可多选）</legend><label v-for="role in roleOptions" :key="role.value" class="check-label"><input v-model="form.roles" type="checkbox" :value="role.value" />{{ role.label }}</label></fieldset>
    <p v-if="error" class="error-message span-2">{{ error }}</p><div class="form-actions span-2"><button class="primary-button">创建账号</button></div>
  </form>

  <section class="panel table-panel organization-table">
    <div class="table-row department-row table-head"><span>部门</span><span>上级</span><span>负责人</span></div>
    <div v-for="item in departments" :key="item.id" class="table-row department-row"><span><strong>{{ item.name }}</strong></span><span>{{ departments.find(parent => parent.id === item.parentId)?.name || '—' }}</span><span>{{ item.managerName || '待指定' }}</span></div>
    <div v-if="!departments.length" class="empty-state"><strong>尚未维护部门</strong><span>先创建组织的第一个部门。</span></div>
  </section>

  <section class="panel table-panel">
    <div class="table-row user-row table-head"><span>用户</span><span>账号</span><span>部门</span><span>类型</span><span>角色</span></div>
    <div v-for="user in users" :key="user.id" class="table-row user-row"><span><strong>{{ user.displayName }}</strong><small>{{ user.email || '未设置邮箱' }}</small></span><span>{{ user.username }}</span><span>{{ user.departmentName || '—' }}</span><span>{{ user.userType === 'INTERNAL' ? '内部' : '客户' }}</span><span>{{ user.roles.join(' · ') }}</span></div>
  </section>
</template>
