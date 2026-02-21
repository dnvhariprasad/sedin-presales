import axios from "axios"

const api = axios.create({
  baseURL: "/api/v1",
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("presales.auth.token")
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      localStorage.removeItem("presales.auth.token")
      localStorage.removeItem("presales.auth.user")
      window.location.href = "/login"
    }
    return Promise.reject(error)
  },
)

export { api }
