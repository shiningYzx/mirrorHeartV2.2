import { defineStore } from 'pinia'
import { ref } from 'vue'
// 用户模块
export const useRecommendListStore = defineStore(
  'big-user',
  () => {
    const recommendList = ref(null)

    const setRecommendList = (list) => {
      recommendList.value = list
    }
    return {
      recommendList,
      setRecommendList
    }
  },
  {
    persist: {
      enabled: true, // 启用持久化
      storage: localStorage // 使用 localStorage 作为存储
    }
  }
)
