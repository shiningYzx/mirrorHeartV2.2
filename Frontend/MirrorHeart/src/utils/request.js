/**
 * HTTP 请求工具
 * 基于 axios 封装，提供统一的请求拦截、响应处理和错误管理
 * 功能包括：
 * - 请求加载动画
 * - Token 自动注入
 * - Token 自动刷新
 * - 统一错误处理
 * - 登录状态管理
 */
import axios from 'axios'
import { useUserStore } from '@/stores'
import { ElMessage, ElLoading } from 'element-plus'

// API 基础 URL
const baseURL = 'http://localhost:8080/api/v1'

// 全局 loading 实例
let loadingInstance

/**
 * 开始加载动画
 * 显示全局加载状态，防止用户重复操作
 */
const startLoading = () => {
  loadingInstance = ElLoading.service({
    lock: true, // 锁定屏幕
    text: '玩命加载中...', // 加载文本
    background: 'rgba(0, 0, 0, 0.1)' // 背景透明度
  })
}

/**
 * 结束加载动画
 * 关闭全局加载状态
 */
const endLoading = () => {
  if (loadingInstance) {
    loadingInstance.close()
  }
}

// 创建 axios 实例
const instance = axios.create({
  baseURL, // 基础 URL
  timeout: 10000 // 请求超时时间（10秒）
})

// token 刷新状态，防止重复刷新
let isRefreshing = false
// 等待 token 刷新的请求队列，用于存储因 token 过期而暂停的请求
let refreshSubscribers = []

/**
 * 订阅 token 刷新事件
 * @param {Function} callback - token 刷新后的回调函数
 */
const subscribeTokenRefresh = (callback) => {
  refreshSubscribers.push(callback)
}

/**
 * token 刷新完成后通知所有等待的请求
 * @param {string} token - 新的 token
 */
const onRefreshed = (token) => {
  refreshSubscribers.forEach((callback) => callback(token))
  refreshSubscribers = [] // 清空队列
}

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    // 开始加载动画
    startLoading()

    // 获取用户 token 信息
    const userStore = useUserStore()
    const token = userStore.basicUser.token
    const tokenHead = userStore.basicUser.tokenHead || 'Bearer'

    // 添加 token 到请求头
    if (token) {
      config.headers.Authorization = `${tokenHead} ${token}`
      config.originalTokenHead = tokenHead // 保存原始 tokenHead
    }

    return config
  },
  (error) => {
    // 请求错误时结束加载动画
    endLoading()
    return Promise.reject(error)
  }
)

/**
 * 统一错误处理
 * @param {Object} error - 错误对象
 * @returns {Promise} 拒绝的 Promise
 */
function handleErrorResponse(error) {
  endLoading()

  if (error.response) {
    const { status, data } = error.response

    // 401 未授权处理
    if (status === 401) {
      const userStore = useUserStore()

      // 如果是刷新 token 的接口返回 401，说明 token 彻底过期
      if (error.config.url.includes('/auth/refresh')) {
        userStore.removeBasicUser()
        userStore.setIsLoginOut()
        ElMessage.error('登录状态已完全过期，请重新登录')
        return Promise.reject(error)
      }

      // 处理 token 刷新逻辑
      if (!isRefreshing) {
        isRefreshing = true
        const refreshToken = userStore.basicUser.refreshToken

        if (refreshToken) {
          // 调用刷新 token 接口
          return instance
            .post('/auth/refresh', { refreshToken })
            .then((res) => {
              const {
                token,
                refreshToken: newRefreshToken,
                expiresIn
              } = res.data.data

              // 更新用户 token 信息
              userStore.setBasicUser({
                token,
                refreshToken: newRefreshToken,
                expiresIn,
                tokenHead: 'Bearer'
              })

              // 通知所有等待的请求
              onRefreshed(token)
              isRefreshing = false

              // 重试原请求
              const originalTokenHead =
                error.config.originalTokenHead || 'Bearer'
              error.config.headers.Authorization = `${originalTokenHead} ${token}`
              return instance(error.config)
            })
            .catch((err) => {
              console.error('刷新token失败:', err)
              isRefreshing = false
              userStore.removeBasicUser()
              userStore.setIsLoginOut()
              ElMessage.error('登录已过期，请重新登录')
              return Promise.reject(error)
            })
        } else {
          // 没有 refreshToken，直接退出登录
          userStore.removeBasicUser()
          userStore.setIsLoginOut()
          ElMessage.error('登录已过期，请重新登录')
          return Promise.reject(error)
        }
      } else {
        // 等待 token 刷新完成后重试
        return new Promise((resolve) => {
          subscribeTokenRefresh((token) => {
            const originalTokenHead = error.config.originalTokenHead || 'Bearer'
            error.config.headers.Authorization = `${originalTokenHead} ${token}`
            resolve(instance(error.config))
          })
        })
      }
    } else {
      // 其他错误处理
      const message = data?.message || '服务异常'
      ElMessage.error(message)
    }
  } else {
    // 网络错误
    ElMessage.error('网络错误，请检查网络连接')
  }

  return Promise.reject(error)
}

/**
 * 统一成功响应处理
 * @param {Object} response - 响应对象
 * @returns {Object|Promise} 响应对象或拒绝的 Promise
 */
function handleSuccessResponse(response) {
  endLoading()

  // 成功响应 (code: 0)
  if (response.data.code === 0) {
    return response
  }
  // 未登录或登录过期 (code: 2001)
  else if (response.data.code === 2001) {
    const userStore = useUserStore()
    userStore.removeBasicUser()
    ElMessage.error(response.data.message || '未登录或登录已过期')

    // 刷新页面，跳转到登录页
    setTimeout(() => {
      location.reload()
    }, 500)

    return Promise.reject(response)
  }
  // 其他错误
  else {
    return handleErrorResponse(response)
  }
}

// 响应拦截器
instance.interceptors.response.use(
  (response) => handleSuccessResponse(response),
  (error) => handleErrorResponse(error)
)

// 导出 axios 实例
export default instance
