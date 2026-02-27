<script setup>
import { ref, markRaw, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import logoBox from '@/components/commonComponents/LogoBox.vue' //引入logo组件
import { Iphone, Key, Warning, ChatDotRound } from '@element-plus/icons-vue' //引入element-plus的图标
import { useUserStore } from '@/stores' //获取用户信息
import {
  getUserBasicInfoService,
  postUserBasicInfoService,
  postLogoutService,
  updateAvatarService,
  updateBirthdayService,
  uploadFileService,
  getUserPostsService,
  getUserFavoritesService,
  getPostHistoryService,
  getUnreadNotificationCountService,
  getNotificationListService,
  getMyReportsService
} from '@/api/userCenter.js'
import {
  getFollowersListService,
  getMyFollowedListService,
  getMyBlockedListService,
  toggleBlockService
} from '@/api/userCommunity.js'
import { userGetRegisterCodeService } from '@/api/login.js' //引入获取个人基本资料接口

import {
  User,
  EditPen,
  Promotion,
  SwitchButton,
  Loading,
  Camera,
  StarFilled,
  Star,
  Delete,
  Connection // 引入社交圈子的图标
} from '@element-plus/icons-vue' //引入element-plus的图标
import { ElMessage, ElMessageBox } from 'element-plus'

//获取用户信息
const userStore = useUserStore()
const router = useRouter()

//获取token
const token = userStore.basicUser.token

//设置抽屉是否显示
const drawerIsShow = ref(false)

//处理关闭抽屉的函数
const handleUserCenter = () => {
  flag.value = userStore.defaultPage
  defaultActive.value = userStore.defaultPage
  drawerIsShow.value = true
  isPrivacySettingsLoaded.value = false
  getUserBasicInfo()
  setTimeout(() => {
    isPrivacySettingsLoaded.value = true
  }, 100)
}

//获取个人基本资料
const userBasicInfo = ref('')

//发送请求获取个人基本资料
const getUserBasicInfo = async () => {
  if (!token) {
    return
  }
  try {
    const res = await getUserBasicInfoService()
    console.log('用户基本信息数据:', res.data.data)
    userBasicInfo.value = res.data.data
    if (userBasicInfo.value) {
      signature.value = userBasicInfo.value.bio || ''
      nickname.value = userBasicInfo.value.nickname || ''
      oldSignature.value = userBasicInfo.value.bio || ''
      oldNickname.value = userBasicInfo.value.nickname || ''
      userId.value = userBasicInfo.value.email || ''
      imgUrl.value = userBasicInfo.value.avatarUrl || ''

      showFollowing.value = userBasicInfo.value.showFollowing ?? 1
      showFavorite.value = userBasicInfo.value.showFavorite ?? 1
      showPost.value = userBasicInfo.value.showPost ?? 1
      birthday.value = userBasicInfo.value.birthday || ''
      birthdayUpdateTime.value = userBasicInfo.value.birthdayUpdateTime || ''

      oldShowFollowing.value = showFollowing.value
      oldShowFavorite.value = showFavorite.value
      oldShowPost.value = showPost.value
      oldBirthday.value = birthday.value

      if (birthdayUpdateTime.value) {
        const lastUpdate = new Date(birthdayUpdateTime.value)
        const now = new Date()
        const oneYear = 365 * 24 * 60 * 60 * 1000
        const timeDiff = now.getTime() - lastUpdate.getTime()
        canChangeBirthday.value = timeDiff >= oneYear
      } else {
        canChangeBirthday.value = true
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error(error.response?.data?.message || '获取用户信息失败')
  }
}

const userId = ref('')
const nickname = ref('')
const signature = ref('')
const oldNickname = ref('')
const oldSignature = ref('')

// 补充缺失的响应式变量
const canChangeName = ref(true)
const canChangeId = ref(false)
const birthday = ref('')
const disabledDate = (time) => time.getTime() > Date.now()
const defaultActive = ref('1-1')

// 隐私设置相关变量
const showFollowing = ref(1)
const showFavorite = ref(1)
const showPost = ref(1)
const oldShowFollowing = ref(1)
const oldShowFavorite = ref(1)
const oldShowPost = ref(1)
const oldBirthday = ref('')
const birthdayUpdateTime = ref('')
const canChangeBirthday = ref(true)
const isPrivacySettingsLoaded = ref(false)

// 头像上传相关
const avatarInputRef = ref(null)
const isUploadingAvatar = ref(false)

// 关注列表相关
const followersList = ref([])
const myFollowedList = ref([])
const myBlockedList = ref([])
const followersPage = ref(0)
const myFollowedPage = ref(0)
const myBlockedPage = ref(0)
const isLoadingFollowing = ref(false)
const isLoadingMyFollowed = ref(false)
const isLoadingMyBlocked = ref(false)

// 我的发布、收藏相关
const myPosts = ref([])
const myFavorites = ref([])
const postsPage = ref(0)
const favoritesPage = ref(0)
const isLoadingPosts = ref(false)
const isLoadingFavorites = ref(false)

// 浏览记录相关
const browsingHistory = ref([])
const historyPage = ref(0)
const isLoadingHistory = ref(false)

// 通知相关
const notifications = ref([])
const notificationPage = ref(0)
const isLoadingNotifications = ref(false)
const unreadCount = ref(0)

// 举报记录相关
const myReports = ref([])
const reportPage = ref(0)
const isLoadingReports = ref(false)

// 头像上传处理
const handleAvatarClick = () => {
  avatarInputRef.value?.click()
}

const handleAvatarChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    return
  }

  try {
    isUploadingAvatar.value = true

    const uploadRes = await uploadFileService(file, 'AVATAR')
    const avatarKey = uploadRes.data.data

    const updateRes = await updateAvatarService(avatarKey)
    if (updateRes.data.code === 0) {
      ElMessage.success('头像更新成功')
      imgUrl.value = URL.createObjectURL(file)
      getUserBasicInfo()
    }
  } catch (error) {
    console.error('头像更新失败:', error)
    ElMessage.error(error.response?.data?.message || '头像更新失败')
  } finally {
    isUploadingAvatar.value = false
    event.target.value = ''
  }
}

// 单独更新生日
const handleBirthdayChange = async () => {
  if (birthday.value === oldBirthday.value) {
    return
  }

  if (!canChangeBirthday.value) {
    ElMessage.warning('生日每年只能修改一次哦！')
    birthday.value = oldBirthday.value
    return
  }

  try {
    const res = await updateBirthdayService(birthday.value)
    if (res.data.code === 0) {
      ElMessage.success('生日更新成功')
      oldBirthday.value = birthday.value
      getUserBasicInfo()
    }
  } catch (error) {
    console.error('生日更新失败:', error)
    ElMessage.error(error.response?.data?.message || '生日更新失败')
    birthday.value = oldBirthday.value
  }
}

// 隐私设置变更处理
const handlePrivacyChange = async (type) => {
  if (!isPrivacySettingsLoaded.value) return

  if (type === 'following') {
    if (showFollowing.value !== oldShowFollowing.value) {
      ElMessage.info('请点击"保存资料修改"按钮保存设置')
    }
  } else if (type === 'favorite') {
    if (showFavorite.value !== oldShowFavorite.value) {
      ElMessage.info('请点击"保存资料修改"按钮保存设置')
    }
  } else if (type === 'post') {
    if (showPost.value !== oldShowPost.value) {
      ElMessage.info('请点击"保存资料修改"按钮保存设置')
    }
  }
}

//提交用户基本信息的修改
const submitAmend = async () => {
  const updateData = {}
  let hasChanges = false

  if (nickname.value !== oldNickname.value) {
    updateData.nickname = nickname.value
    hasChanges = true
  }
  if (signature.value !== oldSignature.value) {
    updateData.bio = signature.value
    hasChanges = true
  }
  if (showFollowing.value !== oldShowFollowing.value) {
    updateData.showFollowing = showFollowing.value
    hasChanges = true
  }
  if (showFavorite.value !== oldShowFavorite.value) {
    updateData.showFavorite = showFavorite.value
    hasChanges = true
  }
  if (showPost.value !== oldShowPost.value) {
    updateData.showPost = showPost.value
    hasChanges = true
  }

  if (!hasChanges) {
    ElMessage.warning('您还没有修改任何信息哦！')
    return
  }

  try {
    const res = await postUserBasicInfoService(updateData)
    ElMessage.success(res.data.message || '修改成功')
    oldNickname.value = nickname.value
    oldSignature.value = signature.value
    oldShowFollowing.value = showFollowing.value
    oldShowFavorite.value = showFavorite.value
    oldShowPost.value = showPost.value
    getUserBasicInfo()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '修改失败，请稍后重试')
  }
}

const flag = ref('1-1')

//获取菜单子页面的index值
const handleOpen = (key) => {
  if (key == 1) {
    flag.value = '1-1'
    defaultActive.value = '1-1'
    return
  }
  if (key == 2 || key == 3) {
    return
  }
  flag.value = key
}

//获取菜单子页面的子页面的index值
const handleMenuItemClick = (command) => {
  flag.value = command.index
  if (flag.value === '2-1') {
    getMyPosts()
  } else if (flag.value === '2-2') {
    getMyFavorites()
  } else if (flag.value === '3-1') {
    // 调整为3-1获取我的关注
    getMyFollowedList()
  } else if (flag.value === '3-2') {
    // 调整为3-2获取粉丝
    getFollowersList()
  } else if (flag.value === '3-3') {
    // 调整为3-3获取我的拉黑
    getMyBlockedList()
  } else if (flag.value === '2-5') {
    // 浏览记录处理
    getBrowsingHistory()
  } else if (flag.value === '4') {
    // 我的消息处理
    getNotifications()
    getUnreadNotificationCount()
  } else if (flag.value === '6') {
    // 举报记录处理
    getMyReports()
  }
}

const getFollowersList = async () => {
  if (!token) {
    return
  }
  try {
    isLoadingFollowing.value = true
    const userId = userBasicInfo.value?.id
    const res = await getFollowersListService(userId, followersPage.value)
    console.log('粉丝列表数据:', res.data.data)
    followersList.value = res.data.data.records || []
  } catch (error) {
    console.error('获取粉丝列表失败:', error)
    ElMessage.error('获取粉丝列表失败，请稍后重试')
  } finally {
    isLoadingFollowing.value = false
  }
}

const getMyFollowedList = async () => {
  if (!token) {
    return
  }
  try {
    isLoadingMyFollowed.value = true
    const res = await getMyFollowedListService(myFollowedPage.value, 10)
    console.log('我的关注列表数据:', res.data.data)
    myFollowedList.value = res.data.data.records || []
  } catch (error) {
    console.error('获取我的关注列表失败:', error)
    ElMessage.error('获取我的关注列表失败，请稍后重试')
  } finally {
    isLoadingMyFollowed.value = false
  }
}

const getMyBlockedList = async () => {
  if (!token) {
    return
  }
  try {
    isLoadingMyBlocked.value = true
    const res = await getMyBlockedListService(myBlockedPage.value, 10)
    console.log('我的拉黑列表数据:', res.data.data)
    myBlockedList.value = res.data.data.records || []
  } catch (error) {
    console.error('获取我的拉黑列表失败:', error)
    ElMessage.error('获取我的拉黑列表失败，请稍后重试')
  } finally {
    isLoadingMyBlocked.value = false
  }
}

const getMyPosts = async () => {
  if (!token) {
    return
  }
  try {
    isLoadingPosts.value = true
    const userId = userBasicInfo.value?.id
    if (!userId) {
      ElMessage.warning('请先获取用户信息')
      return
    }
    const res = await getUserPostsService(userId, postsPage.value, 10)
    console.log('我的发布数据:', res.data.data)
    myPosts.value = res.data.data.records || []
  } catch (error) {
    console.error('获取我的发布失败:', error)
    ElMessage.error('获取我的发布失败，请稍后重试')
  } finally {
    isLoadingPosts.value = false
  }
}

const getMyFavorites = async () => {
  if (!token) {
    return
  }
  try {
    isLoadingFavorites.value = true
    const userId = userBasicInfo.value?.id
    if (!userId) {
      ElMessage.warning('请先获取用户信息')
      return
    }
    const res = await getUserFavoritesService(userId, favoritesPage.value, 10)
    console.log('我的收藏数据:', res.data.data)
    myFavorites.value = res.data.data.records || []
  } catch (error) {
    console.error('获取我的收藏失败:', error)
    ElMessage.error('获取我的收藏失败，请稍后重试')
  } finally {
    isLoadingFavorites.value = false
  }
}

const getBrowsingHistory = async () => {
  if (!token) {
    return
  }
  try {
    isLoadingHistory.value = true
    const userId = userBasicInfo.value?.id
    if (!userId) {
      ElMessage.warning('请先获取用户信息')
      return
    }
    const res = await getPostHistoryService(historyPage.value, 10, userId)
    console.log('浏览记录数据:', res.data.data)
    browsingHistory.value = res.data.data.records || []
  } catch (error) {
    console.error('获取浏览记录失败:', error)
    ElMessage.error('获取浏览记录失败，请稍后重试')
  } finally {
    isLoadingHistory.value = false
  }
}

const getUnreadNotificationCount = async () => {
  if (!token) {
    return
  }
  try {
    const res = await getUnreadNotificationCountService()
    console.log('未读消息数:', res.data.data)
    unreadCount.value = res.data.data || 0
  } catch (error) {
    console.error('获取未读消息数失败:', error)
  }
}

const getNotifications = async () => {
  if (!token) {
    return
  }
  try {
    isLoadingNotifications.value = true
    const res = await getNotificationListService(notificationPage.value, 10)
    console.log('通知列表数据:', res.data.data)
    notifications.value = res.data.data.records || []
  } catch (error) {
    console.error('获取通知列表失败:', error)
    ElMessage.error('获取通知列表失败，请稍后重试')
  } finally {
    isLoadingNotifications.value = false
  }
}

const getMyReports = async () => {
  if (!token) {
    return
  }
  try {
    isLoadingReports.value = true
    const res = await getMyReportsService(reportPage.value, 10)
    console.log('举报记录数据:', res.data.data)
    myReports.value = res.data.data.records || []
  } catch (error) {
    console.error('获取举报记录失败:', error)
    ElMessage.error('获取举报记录失败，请稍后重试')
  } finally {
    isLoadingReports.value = false
  }
}

// 删除帖子
const handleDeletePost = async (postId) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这篇帖子吗？删除后无法恢复。',
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    console.log('删除帖子ID:', postId)
    // const res = await deletePostService(postId)
    // if (res.data.code === 0) {
    ElMessage.success('帖子删除成功')
    getMyPosts() // 重新获取帖子列表
    // } else {
    //   ElMessage.error(res.data.message || '删除失败')
    // }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除帖子失败:', error)
      ElMessage.error('删除帖子失败，请稍后重试')
    }
  }
}

const handleViewUserProfile = (userId) => {
  console.log('查看用户主页:', userId)
  // 跳转到用户主页页面
  router.push(`/user-profile/${userId}`)
}

const handleViewPost = (postId) => {
  console.log('查看帖子详情:', postId)
  // 跳转到社区页面并打开帖子详情
  router.push({ path: '/userCommunity', query: { postId } })
}

const handleToggleFollow = async (userId) => {
  if (!token) {
    ElMessage.warning('登录后才能关注用户哦！')
    userStore.setIsLogin(true)
    return
  }

  try {
    const { toggleFollowService } = await import('@/api/userCommunity.js')
    const res = await toggleFollowService(userId)
    const isNowFollowed = res.data.data

    // 同步更新我的关注列表的状态
    const myFollowedUser = myFollowedList.value.find((u) => u.id === userId)
    if (myFollowedUser) myFollowedUser.isFollowed = isNowFollowed

    // 同步更新粉丝列表的状态
    const followerUser = followersList.value.find((u) => u.id === userId)
    if (followerUser) followerUser.isFollowed = isNowFollowed

    ElMessage.success(isNowFollowed ? '关注成功' : '已取消关注')

    // 如果在我的关注页面操作，可以选择刷新列表
    if (flag.value === '3-1') getMyFollowedList()
  } catch (error) {
    console.error('关注失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

const handleUnblockUser = async (userId) => {
  if (!token) {
    ElMessage.warning('登录后才能操作哦！')
    userStore.setIsLogin(true)
    return
  }

  try {
    await ElMessageBox.confirm('确定要解除拉黑吗？', '解除拉黑', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await toggleBlockService(userId)
    const isBlocked = res.data.data

    if (!isBlocked) {
      ElMessage.success('已解除拉黑')
      // 从拉黑列表中移除该用户
      myBlockedList.value = myBlockedList.value.filter((u) => u.id !== userId)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('解除拉黑失败:', error)
      ElMessage.error('操作失败，请稍后重试')
    }
  }
}

const imgUrl = ref(null)
const formModel = ref({
  email: '',
  code: '',
  reason: ''
})

const form = ref(null)

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
      message: '邮箱格式不正确',
      trigger: 'blur'
    }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    {
      pattern: /^\d{6}$/,
      message: '验证码必须是6位数字',
      trigger: 'blur'
    }
  ],
  reason: [{ required: true, message: '请输入注销原因', trigger: 'blur' }]
}

const countDownTwo = ref(60)
const timerTwo = ref(null)

const getLogoutCode = async () => {
  if (formModel.value.email === '') {
    ElMessage.error('请输入邮箱')
    return
  }
  try {
    const res = await userGetRegisterCodeService({
      email: formModel.value.email,
      scene: 'CANCEL_ACCOUNT'
    })
    ElMessage.success(res.data.message || '验证码已发送')
    let count = 60
    timerTwo.value = setInterval(() => {
      count--
      countDownTwo.value = count
      if (count === 0) {
        clearInterval(timerTwo.value)
        countDownTwo.value = 60
        timerTwo.value = null
      }
    }, 1000)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '发送验证码失败')
  }
}

const handleMsgLogout = async () => {
  try {
    await form.value.validate()
    const res = await postLogoutService(
      formModel.value.email,
      formModel.value.code,
      formModel.value.reason
    )
    ElMessage.success(res.data.message || '注销成功')
    userStore.setIsLoginOut()
    location.reload()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '注销失败')
  }
}

const handleMsgDelete = () => {
  ElMessageBox.confirm(
    '一旦注销账号，所有的记忆与记录将被永久清空，请谨慎选择。',
    '即将离开解忧镜...',
    {
      confirmButtonText: '遗憾离去',
      cancelButtonText: '再想一下',
      type: 'warning',
      icon: markRaw(Warning),
      confirmButtonClass: 'danger-btn'
    }
  )
    .then(() => {
      handleMsgLogout()
    })
    .catch(() => {
      ElMessage({
        type: 'info',
        message: '欢迎留下，愿这里能继续陪伴你。'
      })
    })
}

// 组件卸载时清理定时器
onUnmounted(() => {
  if (timerTwo.value) {
    clearInterval(timerTwo.value)
    timerTwo.value = null
  }
})

defineExpose({
  handleUserCenter
})
</script>

<template>
  <el-drawer
    v-model="drawerIsShow"
    :with-header="false"
    size="50%"
    class="custom-drawer"
  >
    <div class="user-center-container">
      <!-- 添加柔和的氛围光斑 (坚决保留) -->
      <div class="ambient-glow glow-1"></div>
      <div class="ambient-glow glow-2"></div>

      <!-- 左侧：沉浸式导航栏 -->
      <aside class="sidebar">
        <div class="logo-wrapper">
          <logoBox></logoBox>
        </div>

        <el-menu
          class="custom-menu"
          :default-active="defaultActive"
          @open="handleOpen"
          :unique-opened="true"
        >
          <el-sub-menu index="1">
            <template #title>
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </template>
            <el-menu-item index="1-1" @click="handleMenuItemClick">
              <template #title>
                <el-icon><User /></el-icon>
                <span>基本资料</span>
              </template>
            </el-menu-item>
            <el-menu-item index="1-2" @click="handleMenuItemClick">
              <template #title>
                <el-icon><EditPen /></el-icon>
                <span>安全设置</span>
              </template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="2">
            <template #title>
              <el-icon><Promotion /></el-icon>
              <span>我的轨迹</span>
            </template>
            <el-menu-item index="2-1" @click="handleMenuItemClick"
              >个人发布</el-menu-item
            >
            <el-menu-item index="2-2" @click="handleMenuItemClick"
              >我的收藏</el-menu-item
            >
            <el-menu-item index="2-5" @click="handleMenuItemClick"
              >浏览记录</el-menu-item
            >
          </el-sub-menu>

          <!-- 新增分组：社交圈子 -->
          <el-sub-menu index="3">
            <template #title>
              <el-icon><Connection /></el-icon>
              <span>社交圈子</span>
            </template>
            <el-menu-item index="3-1" @click="handleMenuItemClick"
              >我的关注</el-menu-item
            >
            <el-menu-item index="3-2" @click="handleMenuItemClick"
              >我的粉丝</el-menu-item
            >
            <el-menu-item index="3-3" @click="handleMenuItemClick"
              >我的拉黑</el-menu-item
            >
          </el-sub-menu>

          <el-menu-item index="4" @click="handleMenuItemClick">
            <el-icon><ChatDotRound /></el-icon>
            <span>我的消息</span>
          </el-menu-item>

          <el-menu-item index="6" @click="handleMenuItemClick">
            <el-icon><Warning /></el-icon>
            <span>举报记录</span>
          </el-menu-item>

          <el-menu-item
            index="5"
            @click="handleMenuItemClick"
            class="danger-item"
          >
            <el-icon><SwitchButton /></el-icon>
            <span>注销账号</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 右侧：玻璃态内容区 -->
      <main class="main-content">
        <div class="glass-card">
          <!-- ================== 基本资料 ================== -->
          <div v-if="flag === '1-1'" class="content-section basic-info">
            <div class="avatar-display">
              <input
                ref="avatarInputRef"
                type="file"
                accept="image/*"
                style="display: none"
                @change="handleAvatarChange"
              />
              <div class="avatar-wrapper" @click="handleAvatarClick">
                <img
                  :src="
                    imgUrl ||
                    'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                  "
                  alt="头像"
                />
                <div v-if="isUploadingAvatar" class="avatar-loading">
                  <el-icon class="is-loading"><Loading /></el-icon>
                </div>
                <div class="avatar-edit-overlay">
                  <el-icon><Camera /></el-icon>
                </div>
              </div>
            </div>

            <div class="info-form">
              <div class="info-item">
                <span class="label">用户昵称</span>
                <el-tooltip
                  content="昵称一个月只能修改一次哦！"
                  placement="right"
                  effect="dark"
                >
                  <el-input
                    v-model="nickname"
                    class="custom-input"
                    clearable
                    :placeholder="userBasicInfo.nickname"
                    :disabled="!canChangeName"
                  />
                </el-tooltip>
              </div>

              <div class="info-item">
                <span class="label">用户 ID</span>
                <el-tooltip
                  content="用户ID作为用户唯一标识，只能修改一次哦！"
                  placement="right"
                  effect="dark"
                >
                  <el-input
                    v-model="userId"
                    class="custom-input"
                    clearable
                    :placeholder="userBasicInfo.id"
                    :disabled="!canChangeId"
                  />
                </el-tooltip>
              </div>

              <div class="info-item">
                <div class="label-with-hint">
                  <span class="label">生日时间</span>
                  <el-tooltip
                    v-if="!canChangeBirthday && birthdayUpdateTime"
                    :content="`上次修改时间：${new Date(birthdayUpdateTime).toLocaleString('zh-CN')}`"
                    placement="right"
                    effect="dark"
                  >
                    <el-icon class="hint-icon"><Warning /></el-icon>
                  </el-tooltip>
                </div>
                <el-date-picker
                  v-model="birthday"
                  type="date"
                  placeholder="选择你的生日"
                  size="large"
                  :disabled="!canChangeBirthday"
                  :disabled-date="disabledDate"
                  value-format="YYYY-MM-DD"
                  class="custom-date-picker"
                  @change="handleBirthdayChange"
                />
              </div>

              <div class="info-item full-width">
                <span class="label">个性签名</span>
                <el-input
                  v-model="signature"
                  class="custom-textarea"
                  resize="none"
                  :autosize="{ minRows: 4, maxRows: 6 }"
                  type="textarea"
                  :placeholder="
                    userBasicInfo.signature || '写下你的座右铭，世界会听见...'
                  "
                />
              </div>

              <div class="info-item">
                <span class="label">展示关注</span>
                <el-switch
                  v-model="showFollowing"
                  :active-value="1"
                  :inactive-value="0"
                  class="custom-switch"
                  @change="handlePrivacyChange('following')"
                />
              </div>

              <div class="info-item">
                <span class="label">展示收藏</span>
                <el-switch
                  v-model="showFavorite"
                  :active-value="1"
                  :inactive-value="0"
                  class="custom-switch"
                  @change="handlePrivacyChange('favorite')"
                />
              </div>

              <div class="info-item">
                <span class="label">展示帖子</span>
                <el-switch
                  v-model="showPost"
                  :active-value="1"
                  :inactive-value="0"
                  class="custom-switch"
                  @change="handlePrivacyChange('post')"
                />
              </div>
            </div>

            <div class="action-bar">
              <el-button
                type="primary"
                round
                class="save-btn"
                @click="submitAmend"
                >保存资料修改</el-button
              >
            </div>
          </div>

          <!-- ================== 未开放功能占位 ================== -->
          <!-- 修复了此处v-if的重叠渲染问题 -->
          <div v-if="flag === '1-2'" class="content-section empty-state">
            <el-empty
              :image-size="120"
              description="功能正在精心打磨中，敬请期待..."
            />
          </div>

          <!-- ================== 浏览记录 ================== -->
          <div v-if="flag === '2-5'" class="content-section history-section">
            <div class="section-header">
              <h3>浏览记录</h3>
            </div>

            <div v-if="isLoadingHistory" class="loading-state">
              <el-skeleton :rows="5" animated />
            </div>

            <div
              v-else-if="browsingHistory && browsingHistory.length > 0"
              class="history-list"
            >
              <div
                class="post-item clickable-card"
                v-for="post in browsingHistory"
                :key="post.id"
                @click="handleViewPost(post.id)"
              >
                <div class="post-content">
                  <h4 class="post-title">{{ post.title || '无标题' }}</h4>
                  <p class="post-text">{{ post.text || '' }}</p>
                  <div class="post-author">
                    <el-avatar
                      :size="30"
                      :src="
                        post.author?.avatarUrl ||
                        'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                      "
                    />
                    <span class="author-name">{{
                      post.author?.nickname || '未知用户'
                    }}</span>
                  </div>
                  <div class="post-stats">
                    <span class="stat-item">
                      <el-icon><ChatDotRound /></el-icon>
                      {{ post.commentCount || 0 }} 评论
                    </span>
                    <span class="stat-item">
                      <el-icon><StarFilled /></el-icon>
                      {{ post.likeCount || 0 }} 点赞
                    </span>
                    <span class="stat-item">
                      <el-icon><Star /></el-icon>
                      {{ post.favoriteCount || 0 }} 收藏
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="empty-state">
              <el-empty description="还没有浏览记录哦~" />
            </div>
          </div>

          <!-- ================== 我的发布 ================== -->
          <div v-if="flag === '2-1'" class="content-section posts-section">
            <div class="section-header">
              <h3>我的发布</h3>
            </div>

            <div v-if="isLoadingPosts" class="loading-state">
              <el-skeleton :rows="5" animated />
            </div>

            <div v-else-if="myPosts && myPosts.length > 0" class="posts-list">
              <!-- 增加 @click 跳转与 cursor: pointer -->
              <div
                class="post-item clickable-card"
                v-for="post in myPosts"
                :key="post.id"
                @click="handleViewPost(post.id)"
              >
                <div class="post-content">
                  <h4 class="post-title">{{ post.title || '无标题' }}</h4>
                  <p class="post-text">{{ post.text || '' }}</p>
                  <div class="post-footer">
                    <div class="post-stats">
                      <span class="stat-item">
                        <el-icon><ChatDotRound /></el-icon>
                        {{ post.commentCount || 0 }} 评论
                      </span>
                      <span class="stat-item">
                        <el-icon><StarFilled /></el-icon>
                        {{ post.likeCount || 0 }} 点赞
                      </span>
                      <span class="stat-item">
                        <el-icon><Star /></el-icon>
                        {{ post.favoriteCount || 0 }} 收藏
                      </span>
                    </div>
                    <div class="post-actions" @click.stop>
                      <el-button
                        type="danger"
                        size="small"
                        :icon="Delete"
                        @click="handleDeletePost(post.id)"
                        circle
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="empty-state">
              <el-empty description="还没有发布过帖子哦~" />
            </div>
          </div>

          <!-- ================== 我的收藏 ================== -->
          <div v-if="flag === '2-2'" class="content-section favorites-section">
            <div class="section-header">
              <h3>我的收藏</h3>
            </div>

            <div v-if="isLoadingFavorites" class="loading-state">
              <el-skeleton :rows="5" animated />
            </div>

            <div
              v-else-if="myFavorites && myFavorites.length > 0"
              class="favorites-list"
            >
              <!-- 增加 @click 跳转与 cursor: pointer -->
              <div
                class="post-item clickable-card"
                v-for="post in myFavorites"
                :key="post.id"
                @click="handleViewPost(post.id)"
              >
                <div class="post-content">
                  <h4 class="post-title">{{ post.title || '无标题' }}</h4>
                  <p class="post-text">{{ post.text || '' }}</p>
                  <div class="post-author">
                    <el-avatar
                      :size="30"
                      :src="
                        post.author?.avatarUrl ||
                        'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                      "
                    />
                    <span class="author-name">{{
                      post.author?.nickname || '未知用户'
                    }}</span>
                  </div>
                  <div class="post-stats">
                    <span class="stat-item">
                      <el-icon><ChatDotRound /></el-icon>
                      {{ post.commentCount || 0 }} 评论
                    </span>
                    <span class="stat-item">
                      <el-icon><StarFilled /></el-icon>
                      {{ post.likeCount || 0 }} 点赞
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="empty-state">
              <el-empty description="还没有收藏过帖子哦~" />
            </div>
          </div>

          <!-- ================== 我的关注 ================== -->
          <div v-if="flag === '3-1'" class="content-section following-section">
            <div class="section-header">
              <h3>我的关注</h3>
            </div>

            <div v-if="isLoadingMyFollowed" class="loading-state">
              <el-skeleton :rows="5" animated />
            </div>

            <div
              v-else-if="myFollowedList && myFollowedList.length > 0"
              class="following-list"
            >
              <div
                class="following-item"
                v-for="user in myFollowedList"
                :key="user.id"
              >
                <div class="user-info" @click="handleViewUserProfile(user.id)">
                  <el-avatar
                    :size="50"
                    :src="
                      user.avatarUrl ||
                      'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                    "
                  />
                  <div class="user-details">
                    <div class="user-nickname">
                      {{ user.nickname || '未知用户' }}
                    </div>
                    <div class="user-bio">
                      {{ user.bio || '这个人很懒，什么都没写~' }}
                    </div>
                  </div>
                </div>
                <div class="action-buttons">
                  <el-button
                    v-if="user.isFollowed"
                    type="info"
                    size="small"
                    round
                    plain
                    @click.stop="handleToggleFollow(user.id)"
                  >
                    已关注
                  </el-button>
                  <el-button
                    v-else
                    type="primary"
                    size="small"
                    round
                    @click.stop="handleToggleFollow(user.id)"
                  >
                    关注
                  </el-button>
                </div>
              </div>
            </div>

            <div v-else class="empty-state">
              <el-empty description="还没有关注任何人" />
            </div>
          </div>

          <!-- ================== 我的粉丝 ================== -->
          <div v-if="flag === '3-2'" class="content-section following-section">
            <div class="section-header">
              <h3>我的粉丝</h3>
            </div>

            <div v-if="isLoadingFollowing" class="loading-state">
              <el-skeleton :rows="5" animated />
            </div>

            <div
              v-else-if="followersList && followersList.length > 0"
              class="following-list"
            >
              <div
                class="following-item"
                v-for="user in followersList"
                :key="user.id"
              >
                <div class="user-info" @click="handleViewUserProfile(user.id)">
                  <el-avatar
                    :size="50"
                    :src="
                      user.avatarUrl ||
                      'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                    "
                  />
                  <div class="user-details">
                    <div class="user-nickname">
                      {{ user.nickname || '未知用户' }}
                    </div>
                    <div class="user-bio">
                      {{ user.bio || '这个人很懒，什么都没写~' }}
                    </div>
                  </div>
                </div>
                <div class="action-buttons">
                  <el-button
                    v-if="user.isFollowed"
                    type="info"
                    size="small"
                    round
                    plain
                    @click.stop="handleToggleFollow(user.id)"
                  >
                    已关注
                  </el-button>
                  <el-button
                    v-else
                    type="primary"
                    size="small"
                    round
                    @click.stop="handleToggleFollow(user.id)"
                  >
                    关注
                  </el-button>
                </div>
              </div>
            </div>

            <div v-else class="empty-state">
              <el-empty description="还没有粉丝哦" />
            </div>
          </div>

          <!-- ================== 我的拉黑 ================== -->
          <div v-if="flag === '3-3'" class="content-section blocked-section">
            <div class="section-header">
              <h3>我的拉黑</h3>
            </div>

            <div v-if="isLoadingMyBlocked" class="loading-state">
              <el-skeleton :rows="5" animated />
            </div>

            <div
              v-else-if="myBlockedList && myBlockedList.length > 0"
              class="following-list"
            >
              <div
                class="following-item"
                v-for="user in myBlockedList"
                :key="user.id"
              >
                <div class="user-info" @click="handleViewUserProfile(user.id)">
                  <el-avatar
                    :size="50"
                    :src="
                      user.avatarUrl ||
                      'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                    "
                  />
                  <div class="user-details">
                    <div class="user-nickname">
                      {{ user.nickname || '未知用户' }}
                    </div>
                    <div class="user-bio">
                      {{ user.bio || '这个人很懒，什么都没写~' }}
                    </div>
                  </div>
                </div>
                <div class="action-buttons">
                  <el-button
                    type="success"
                    size="small"
                    round
                    plain
                    @click.stop="handleUnblockUser(user.id)"
                  >
                    解除拉黑
                  </el-button>
                </div>
              </div>
            </div>

            <div v-else class="empty-state">
              <el-empty description="还没有拉黑任何人" />
            </div>
          </div>

          <!-- ================== 我的消息 ================== -->
          <div v-if="flag === '4'" class="content-section messages-section">
            <div class="messages-header">
              <el-icon class="messages-icon"><ChatDotRound /></el-icon>
              <h3>我的消息</h3>
              <el-badge
                v-if="unreadCount > 0"
                :value="unreadCount"
                class="message-badge"
              />
            </div>

            <div v-if="isLoadingNotifications" class="loading-state">
              <el-skeleton :rows="5" animated />
            </div>

            <div
              v-else-if="notifications && notifications.length > 0"
              class="messages-list"
            >
              <div
                class="message-item"
                v-for="notification in notifications"
                :key="notification.id"
              >
                <div class="message-avatar">
                  <el-avatar
                    :size="40"
                    :src="
                      notification.sender?.avatarUrl ||
                      'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                    "
                  />
                  <el-badge
                    v-if="notification.isRead === 0"
                    type="primary"
                    class="unread-badge"
                  />
                </div>
                <div class="message-content">
                  <div class="message-header">
                    <span class="message-user">{{
                      notification.sender?.nickname || '系统'
                    }}</span>
                    <span class="message-time">{{
                      notification.createdAt || ''
                    }}</span>
                  </div>
                  <div class="message-text">
                    {{ notification.content || '' }}
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="empty-state">
              <el-empty description="暂无消息通知" />
            </div>
          </div>

          <!-- ================== 举报记录 ================== -->
          <div v-if="flag === '6'" class="content-section reports-section">
            <div class="reports-header">
              <el-icon class="reports-icon"><Warning /></el-icon>
              <h3>举报记录</h3>
            </div>

            <div v-if="isLoadingReports" class="loading-state">
              <el-skeleton :rows="5" animated />
            </div>

            <div
              v-else-if="myReports && myReports.length > 0"
              class="reports-list"
            >
              <div
                class="report-item"
                v-for="report in myReports"
                :key="report.id"
              >
                <div class="report-info">
                  <div class="report-header">
                    <span class="report-type">
                      {{
                        report.targetType === 'POST' ? '帖子举报' : '用户举报'
                      }}
                    </span>
                    <span class="report-time">{{
                      report.createdAt || ''
                    }}</span>
                  </div>
                  <div class="report-reason">
                    <span class="reason-label">举报原因：</span>
                    <span class="reason-text">{{ report.reason || '' }}</span>
                  </div>
                  <div class="report-status">
                    <el-tag
                      :type="
                        report.status === 1
                          ? 'success'
                          : report.status === 2
                            ? 'danger'
                            : 'info'
                      "
                      size="small"
                    >
                      {{
                        report.status === 1
                          ? '已处理'
                          : report.status === 2
                            ? '已驳回'
                            : '待处理'
                      }}
                    </el-tag>
                  </div>
                  <div v-if="report.adminNote" class="admin-note">
                    <span class="note-label">管理员备注：</span>
                    <span class="note-text">{{ report.adminNote }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="empty-state">
              <el-empty description="暂无举报记录" />
            </div>
          </div>

          <!-- ================== 注销账号 ================== -->
          <div v-if="flag === '5'" class="content-section cancel-account">
            <div class="warning-header">
              <el-icon class="warning-icon"><Warning /></el-icon>
              <h3>账号注销申请</h3>
              <p>
                请谨慎操作！账号注销后，您的所有数据和足迹将被永久抹除且不可恢复。
              </p>
            </div>

            <el-form
              class="cancel-form"
              :model="formModel"
              :rules="rules"
              ref="form"
              label-position="top"
            >
              <el-form-item prop="email">
                <el-input
                  v-model="formModel.email"
                  size="large"
                  class="custom-input with-suffix"
                  :prefix-icon="Iphone"
                  placeholder="请输入绑定的邮箱"
                >
                  <template #suffix>
                    <div class="divider">|</div>
                    <span
                      class="code-btn"
                      :class="{ 'is-disabled': countDownTwo !== 60 }"
                      @click="getLogoutCode"
                    >
                      {{
                        countDownTwo === 60
                          ? '获取验证码'
                          : `重新发送(${countDownTwo}s)`
                      }}
                    </span>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="code">
                <el-input
                  v-model="formModel.code"
                  size="large"
                  class="custom-input"
                  :prefix-icon="Key"
                  placeholder="请输入6位验证码"
                ></el-input>
              </el-form-item>

              <el-form-item prop="reason">
                <el-input
                  v-model="formModel.reason"
                  size="large"
                  class="custom-textarea"
                  type="textarea"
                  :rows="3"
                  placeholder="请告诉我们您离开的原因..."
                ></el-input>
              </el-form-item>

              <el-form-item class="action-row">
                <el-button
                  class="danger-submit-btn"
                  round
                  @click="handleMsgDelete"
                >
                  我已确认，申请注销
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </main>
    </div>
  </el-drawer>
</template>

<style scoped>
/* --- 抽屉样式穿透覆盖 --- */
:deep(.el-drawer) {
  background: #f7fbfd;
}
:deep(.el-drawer__body) {
  padding: 0;
}

/* --- 整体布局与氛围光晕 --- */
.user-center-container {
  display: flex;
  height: 100%;
  width: 100%;
  border-radius: 24px;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #e0f7fa 0%, #f9fbe7 100%);
  z-index: 1;
}

.ambient-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  z-index: -1;
  opacity: 0.65;
  pointer-events: none;
  animation: float 12s infinite alternate ease-in-out;
}

.glow-1 {
  width: 500px;
  height: 500px;
  background: #a8edea;
  top: -100px;
  left: 10%;
}

.glow-2 {
  width: 600px;
  height: 600px;
  background: #fed6e3;
  bottom: -150px;
  right: 5%;
  animation-delay: -6s;
}

@keyframes float {
  0% {
    transform: translateY(0) scale(1);
  }
  100% {
    transform: translateY(40px) scale(1.1);
  }
}

/* ================= 左侧导航栏 ================= */
.sidebar {
  width: 240px;
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(12px);
  border-right: 1px solid rgba(255, 255, 255, 0.6);
  display: flex;
  flex-direction: column;
  padding-top: 20px;
  z-index: 2;
}

.logo-wrapper {
  padding: 0 20px 20px;
  display: flex;
  justify-content: center;
}

.custom-menu {
  border-right: none !important;
  background-color: transparent !important;
  padding: 0 10px;
  border-radius: 12px;
}

:deep(.custom-menu .el-sub-menu__title),
:deep(.custom-menu .el-menu-item) {
  height: 50px;
  line-height: 50px;
  border-radius: 12px;
  margin-bottom: 6px;
  color: #34495e;
  font-weight: 500;
  transition: all 0.3s ease;
}

:deep(.custom-menu .el-menu-item:hover),
:deep(.custom-menu .el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.7) !important;
  color: #409eff;
}

:deep(.custom-menu .el-menu-item.is-active) {
  background: linear-gradient(
    135deg,
    rgba(240, 249, 255, 0.8) 0%,
    rgba(224, 242, 254, 0.8) 100%
  ) !important;
  color: #409eff;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

:deep(.custom-menu .el-menu-item.danger-item:hover) {
  color: #f56c6c;
  background-color: rgba(253, 226, 226, 0.7) !important;
}
:deep(.custom-menu .el-menu-item.danger-item.is-active) {
  color: #f56c6c;
  background: linear-gradient(
    135deg,
    rgba(254, 240, 240, 0.8) 0%,
    rgba(253, 226, 226, 0.8) 100%
  ) !important;
}

/* ================= 右侧内容区 ================= */
.main-content {
  flex: 1;
  padding: 30px 40px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  overflow-y: auto;
  z-index: 2;
}

.glass-card {
  width: 100%;
  max-width: 600px;
  min-height: 500px;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.7) 0%,
    rgba(255, 255, 255, 0.5) 100%
  );
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow:
    0 10px 40px rgba(0, 0, 0, 0.06),
    inset 0 0 0 1px rgba(255, 255, 255, 0.5);
  padding: 40px 50px;
  box-sizing: border-box;
  animation: fadeIn 0.4s ease-out;
}

.content-section {
  display: flex;
  flex-direction: column;
  height: 100%;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.empty-state {
  justify-content: center;
  align-items: center;
  color: #909399;
}

/* ================= 基本资料页面 ================= */
.basic-info {
  align-items: center;
}

.avatar-display {
  margin-bottom: 40px;
}

.avatar-wrapper {
  position: relative;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
}

.avatar-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border: 4px solid #fff;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.2);
  transition: transform 0.3s ease;
}

.avatar-wrapper:hover img {
  transform: scale(1.05);
}

.avatar-edit-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  color: #fff;
  font-size: 24px;
}

.avatar-wrapper:hover .avatar-edit-overlay {
  opacity: 1;
}

.avatar-loading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
}

.info-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.info-item.full-width {
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.info-item .label {
  font-size: 15px;
  font-weight: 600;
  color: #34495e;
  width: 100px;
  flex-shrink: 0;
}

.label-with-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  width: 100px;
  flex-shrink: 0;
}

.hint-icon {
  color: #e6a23c;
  cursor: pointer;
  font-size: 16px;
}

/* 输入框统一美化 */
:deep(.custom-input .el-input__wrapper),
:deep(.custom-date-picker .el-input__wrapper),
:deep(.custom-textarea .el-textarea__inner) {
  background: rgba(255, 255, 255, 0.7);
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e4e7ed inset;
  transition: all 0.3s;
}
:deep(.custom-input .el-input__wrapper:hover),
:deep(.custom-date-picker .el-input__wrapper:hover),
:deep(.custom-textarea .el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}
:deep(.custom-input .el-input__wrapper.is-focus),
:deep(.custom-date-picker .el-input__wrapper.is-focus),
:deep(.custom-textarea .el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #409eff inset !important;
  background: #fff;
}
:deep(.custom-input) {
  flex: 1;
}

:deep(.custom-switch) {
  flex: 1;
}

:deep(.custom-switch .el-switch__wrapper) {
  width: 100%;
}

.action-bar {
  margin-top: 40px;
  width: 100%;
  display: flex;
  justify-content: center;
}

.save-btn {
  padding: 12px 40px;
  font-size: 16px;
  font-weight: bold;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
  transition: transform 0.2s;
}
.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
}

/* ================= 我的消息页面 ================= */
.messages-section {
  padding: 20px 0;
}

.messages-header {
  text-align: center;
  margin-bottom: 40px;
}

.messages-icon {
  font-size: 50px;
  color: #409eff;
  margin-bottom: 15px;
}

.messages-header h3 {
  font-size: 22px;
  font-weight: 800;
  color: #2c3e50;
  margin: 0 0 10px 0;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 500px;
  overflow-y: auto;
}

.message-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  transition: all 0.3s;
}

.message-item:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateX(4px);
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.message-user {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.message-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

/* ================= 举报记录页面 ================= */
.reports-section {
  padding: 20px 0;
}

.reports-header {
  text-align: center;
  margin-bottom: 40px;
}

.reports-icon {
  font-size: 50px;
  color: #e6a23c;
  margin-bottom: 15px;
}

.reports-header h3 {
  font-size: 22px;
  font-weight: 800;
  color: #2c3e50;
  margin: 0 0 10px 0;
}

.reports-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.report-item {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.report-item:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.report-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.report-type {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.report-time {
  font-size: 12px;
  color: #909399;
}

.report-reason {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.reason-label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  flex-shrink: 0;
}

.reason-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
}

.report-status {
  display: flex;
  align-items: center;
}

.admin-note {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.note-label {
  font-size: 13px;
  font-weight: 600;
  color: #909399;
  flex-shrink: 0;
}

.note-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

/* ================= 注销账号页面 ================= */
.cancel-account {
  padding: 20px 0;
}

.warning-header {
  text-align: center;
  margin-bottom: 40px;
}

.warning-icon {
  font-size: 50px;
  color: #f56c6c;
  margin-bottom: 15px;
}

.warning-header h3 {
  font-size: 22px;
  font-weight: 800;
  color: #2c3e50;
  margin: 0 0 10px 0;
}

.warning-header p {
  font-size: 14px;
  color: #7f8c8d;
  line-height: 1.6;
  margin: 0;
}

.cancel-form {
  width: 100%;
}

.custom-input.with-suffix :deep(.el-input__wrapper) {
  padding-right: 5px;
}

.divider {
  color: #dcdfe6;
  margin: 0 10px;
}

.code-btn {
  color: #409eff;
  cursor: pointer;
  font-weight: 500;
  padding: 0 10px;
  transition: color 0.3s;
}
.code-btn:hover {
  color: #66b1ff;
}
.code-btn.is-disabled {
  color: #a8abb2;
  cursor: not-allowed;
}

.action-row {
  margin-top: 40px;
}

.danger-submit-btn {
  width: 100%;
  padding: 12px 0;
  font-size: 16px;
  font-weight: bold;
  background: linear-gradient(135deg, #ff4d4f 0%, #e83e8c 100%);
  color: #fff;
  border: none;
  box-shadow: 0 4px 15px rgba(245, 108, 108, 0.3);
  transition: transform 0.2s;
}
.danger-submit-btn:hover {
  background: linear-gradient(135deg, #ff7875 0%, #f759ab 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(245, 108, 108, 0.4);
  color: #fff;
}
.section-header {
  margin-bottom: 24px;
}

.section-header h3 {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
}

/* ================= 关注与粉丝列表 ================= */
.following-section {
  padding: 24px;
}

.following-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.following-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.following-item:hover {
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  cursor: pointer;
  transition: all 0.3s ease;
}

.user-info:hover {
  opacity: 0.8;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-nickname {
  font-size: 16px;
  font-weight: 600;
  color: #34495e;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-bio {
  font-size: 13px;
  color: #95a5a6;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.action-buttons {
  flex-shrink: 0;
}

/* ================= 我的发布、收藏样式 ================= */
.posts-section,
.favorites-section {
  padding: 24px;
}

.posts-list,
.favorites-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ✨ 修复帖子卡片难以点击：增加 cursor: pointer */
.post-item {
  padding: 20px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  cursor: pointer; /* 确保整张卡片显示可点击手势 */
}

.post-item.clickable-card:hover {
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.post-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-title {
  font-size: 18px;
  font-weight: 600;
  color: #34495e;
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.post-text {
  font-size: 14px;
  color: #7f8c8d;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.post-stats {
  display: flex;
  gap: 20px;
  flex: 1;
}

.post-actions {
  flex-shrink: 0;
  margin-left: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #95a5a6;
  transition: color 0.3s ease;
}

.stat-item:hover {
  color: #409eff;
}

.post-author {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.author-name {
  font-size: 14px;
  color: #34495e;
  font-weight: 500;
}

/* ================= 加载状态 ================= */
.loading-state {
  padding: 20px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  border-radius: 12px;
}
</style>
