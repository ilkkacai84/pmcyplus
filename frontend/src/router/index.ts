import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/components/AppLayout.vue'
import LoginView from '@/views/LoginView.vue'
import DashboardView from '@/views/DashboardView.vue'
import ProjectsView from '@/views/ProjectsView.vue'
import ProjectDetailView from '@/views/ProjectDetailView.vue'
import RequirementsView from '@/views/RequirementsView.vue'
import UsersView from '@/views/UsersView.vue'
import DepartmentView from '@/views/DepartmentView.vue'
import AuditView from '@/views/AuditView.vue'
import type { Role } from '@/api/types'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    {
      path: '/', component: AppLayout,
      children: [
        { path: '', component: DashboardView },
        { path: 'projects', component: ProjectsView },
        { path: 'projects/:id', component: ProjectDetailView },
        { path: 'requirements', component: RequirementsView },
        { path: 'department', component: DepartmentView, meta: { roles: ['DEPARTMENT_MANAGER'] } },
        { path: 'users', component: UsersView, meta: { roles: ['ADMIN'] } },
        { path: 'audit', component: AuditView, meta: { roles: ['ADMIN'] } },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized) await auth.fetchMe()
  if (!to.meta.public && !auth.user) return '/login'
  if (to.path === '/login' && auth.user) return '/'
  const requiredRoles = to.meta.roles as Role[] | undefined
  if (requiredRoles && !requiredRoles.some(role => auth.user?.roles.includes(role))) return '/'
})

export default router
