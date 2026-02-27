import { defineStore } from 'pinia'
import { ref } from 'vue'
// 用户模块
export const useRecommendListStore = defineStore(
  'big-user',
  () => {
    //是否刷新最新帖子
    const isRefresh = ref(false)

    const setIsRefresh = () => {
      isRefresh.value = true
    }

    const outIsRefresh = () => {
      isRefresh.value = false
    }

    return {
      isRefresh,
      setIsRefresh,
      outIsRefresh
    }
  },
  {
    persist: {
      enabled: true, // 启用持久化
      storage: localStorage // 使用 localStorage 作为存储
    }
  }
)
