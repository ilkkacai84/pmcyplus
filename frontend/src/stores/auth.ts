import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http, prepareCsrf } from '@/api/http'
import type { CurrentUser } from '@/api/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<CurrentUser | null>(null)
  const initialized = ref(false)

  async function fetchMe() {
    try {
      const { data } = await http.get<CurrentUser>('/auth/me')
      user.value = data
    } catch {
      user.value = null
    } finally {
      initialized.value = true
    }
  }

  async function login(username: string, password: string) {
    await prepareCsrf()
    const { data } = await http.post<CurrentUser>('/auth/login', { username, password })
    user.value = data
    initialized.value = true
  }

  async function logout() {
    await prepareCsrf()
    await http.post('/auth/logout')
    user.value = null
  }

  return { user, initialized, fetchMe, login, logout }
})
