import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
})

export async function prepareCsrf() {
  await http.get('/auth/csrf')
}

export function apiMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message ?? '请求失败，请稍后重试'
  }
  return '请求失败，请稍后重试'
}
