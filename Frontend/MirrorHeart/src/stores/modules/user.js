import { defineStore } from 'pinia'
import { ref } from 'vue'

// 用户模块
export const useUserStore = defineStore(
  'big-user',
  () => {
    const basicUser = ref({
      uid: '',
      token: '',
      refreshToken: '',
      tokenHead: 'Bearer',
      expiresIn: 0,
      userId: '',
      nickname: '',
      avatar: '',
      subscriber: '',
      signature: '' // 个人基本信息
    })

    const isLogin = ref(false) // 是否登录

    const setIsLogin = () => {
      isLogin.value = true
    }
    const outIsLogin = () => {
      isLogin.value = false
    }

    const isLoginOut = ref(false) // 是否退出登录

    //设置退出登录函数
    const setIsLoginOut = () => {
      isLoginOut.value = true
    }
    //设置退出登录函数
    const outIsLoginOut = () => {
      isLoginOut.value = false
    }

    const isChangeAvatar = ref(false) // 是否修改头像

    const setIsChangeAvatar = () => {
      isChangeAvatar.value = true
    }

    const outIsChangeAvatar = () => {
      isChangeAvatar.value = false
    }

    //是否刷新最新帖子
    const isRefreshNewPost = ref(false)

    const setIsRefreshNewPost = () => {
      isRefreshNewPost.value = true
    }

    const outIsRefreshNewPost = () => {
      isRefreshNewPost.value = false
    }

    //是否刷新最热帖子
    const isRefreshHotPost = ref(false)

    const setIsRefreshHotPost = () => {
      isRefreshHotPost.value = true
    }

    const outIsRefreshHotPost = () => {
      isRefreshHotPost.value = false
    }

    //是否是在最新帖子页面
    const isNew = ref(true)

    const setIsNew = () => {
      isNew.value = true
    }

    const outIsNew = () => {
      isNew.value = false
    }

    //设置个人中心默认跳转页面的页码
    const defaultPage = ref('1-1')

    const setDefaultPage = (page) => {
      defaultPage.value = page
    }

    const setBasicUser = ({
      uid,
      token,
      refreshToken,
      tokenHead,
      expiresIn,
      userId,
      nickname,
      avatar,
      subscriber,
      signature
    }) => {
      basicUser.value = {
        ...basicUser.value,
        uid,
        token,
        refreshToken,
        tokenHead,
        expiresIn,
        userId,
        nickname,
        avatar,
        subscriber,
        signature
      }
    }

    const updateToken = ({ token, refreshToken, expiresIn }) => {
      basicUser.value.token = token
      basicUser.value.refreshToken = refreshToken
      basicUser.value.expiresIn = expiresIn
    }

    const removeBasicUser = () => {
      basicUser.value = {
        uid: '',
        token: '',
        refreshToken: '',
        tokenHead: 'Bearer',
        expiresIn: 0,
        userId: '',
        nickname: '',
        avatar: '',
        subscriber: '',
        signature: ''
      }
      localStorage.removeItem('big-user')
    }

    return {
      basicUser,
      setBasicUser,
      updateToken,
      removeBasicUser,
      isLogin,
      setIsLogin,
      outIsLogin,
      isLoginOut,
      setIsLoginOut,
      outIsLoginOut,
      isChangeAvatar,
      setIsChangeAvatar,
      outIsChangeAvatar,
      defaultPage,
      setDefaultPage,
      isRefreshNewPost,
      setIsRefreshNewPost,
      outIsRefreshNewPost,
      isRefreshHotPost,
      setIsRefreshHotPost,
      outIsRefreshHotPost,
      isNew,
      setIsNew,
      outIsNew
    }
  },
  {
    // 持久化配置
    persist: {
      enabled: true, // 启用持久化
      storage: localStorage, // 使用 localStorage 作为存储
      restoreState: (storedState) => {
        // 从持久化存储中恢复状态
        if (storedState) {
          // 如果持久化存储中有数据，则使用它
          return storedState
        } else {
          // 否则，返回初始状态
          return {
            basicUser: {
              uid: '',
              token: '',
              refreshToken: '',
              tokenHead: 'Bearer',
              expiresIn: 0,
              userId: '',
              nickname: '',
              avatar: '',
              subscriber: '',
              signature: ''
            }
          }
        }
      }
    }
  }
)
