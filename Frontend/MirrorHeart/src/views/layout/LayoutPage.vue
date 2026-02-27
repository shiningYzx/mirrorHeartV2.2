<script lang="ts" setup>
import { ref, watch } from 'vue'
import loginPage from '@/views/login/LoginPage.vue' //引入登录页面
import smallBox from '@/components/login/SmallBox.vue' //引入小盒子
import userCenter from '@/components/login/UserCenter.vue' //引入用户中心
import { useRoute } from 'vue-router' //获取当前路由
import { useUserStore } from '@/stores' //获取用户信息
import { userLogoutService } from '@/api/login.js' //退出登录接口
import { ElMessage } from 'element-plus' //引入element-plus的消息提示
import logoBox from '@/components/commonComponents/LogoBox.vue' //引入logo组件

const userStore = useUserStore() //获取用户信息

const userImg = ref(userStore.basicUser.avatar || '') //定义用户头像

//监听用户头像变化，如果用户头像变化，就更新用户头像
watch(
  () => userStore.basicUser.avatar,
  (newValue) => {
    userImg.value = newValue
  }
)

const handleLoginPage = ref(null)

const handleLogin = () => {
  if (!userStore.basicUser.token) {
    if (
      handleLoginPage.value &&
      typeof handleLoginPage.value.handleLogin === 'function'
    ) {
      handleLoginPage.value.handleLogin()
    }
  } else {
    return
  }
}

const route = useRoute() //获取当前路由

//监听路由变化，如果从找回密码页面跳转到首页，就触发登录
watch(
  () => route.fullPath,
  (newVal, oldVal) => {
    if (oldVal.includes('/findPassword') && newVal.includes('/homePage')) {
      handleLogin()
    }
  }
)

const isAvatarHover = ref(false)
// 如果鼠标悬停在个人中心上，头像放大
const handleAvatarHover = () => {
  isAvatarHover.value = true
}
// 如果鼠标离开个人中心，头像恢复原状
const handleAvatarLeave = () => {
  isAvatarHover.value = false
}

//退出登录
const handleLoginOut = async () => {
  try {
    await userLogoutService()
    ElMessage.success('退出登录成功')
  } catch (error) {
    if (error.response?.data?.code === 2001) {
      console.warn('Token 已过期，继续执行退出登录流程')
    } else {
      console.error('退出登录接口调用失败:', error)
    }
  } finally {
    userStore.removeBasicUser()
    setTimeout(() => {
      location.reload()
    }, 500)
  }
}

//监听isLoginOut变化，如果isLoginOut=true，就退出登录
watch(
  () => userStore.isLoginOut,
  (newValue) => {
    if (newValue == true) {
      handleLoginOut()
      userStore.outIsLoginOut()
    }
  }
)

//监听isLogin变化，如果isLogin=true，就弹出登录框
watch(
  () => userStore.isLogin,
  (newValue) => {
    if (newValue == true) {
      if (
        handleLoginPage.value &&
        typeof handleLoginPage.value.handleLogin === 'function'
      ) {
        handleLoginPage.value.handleLogin()
      }
      userStore.outIsLogin()
    }
  }
)

//获取抽屉组件
const drawerBox = ref(null)

//处理获取动态函数
const handleDynamic = () => {
  if (!userStore.basicUser.token) {
    ElMessage.warning('请先登录查看动态')
    handleLogin()
    return
  }
  userStore.setDefaultPage('2-1')
  if (
    drawerBox.value &&
    typeof drawerBox.value.handleUserCenter === 'function'
  ) {
    drawerBox.value.handleUserCenter()
  }
}

//处理获取收藏函数
const handleFavorites = () => {
  if (!userStore.basicUser.token) {
    ElMessage.warning('请先登录查看收藏')
    handleLogin()
    return
  }
  userStore.setDefaultPage('2-2')
  if (
    drawerBox.value &&
    typeof drawerBox.value.handleUserCenter === 'function'
  ) {
    drawerBox.value.handleUserCenter()
  }
}

//处理获取消息函数
const handleMessages = () => {
  if (!userStore.basicUser.token) {
    ElMessage.warning('请先登录查看消息')
    handleLogin()
    return
  }
  userStore.setDefaultPage('4')
  if (
    drawerBox.value &&
    typeof drawerBox.value.handleUserCenter === 'function'
  ) {
    drawerBox.value.handleUserCenter()
  }
}

//处理获取历史记录函数
const handleHistory = () => {
  if (!userStore.basicUser.token) {
    ElMessage.warning('请先登录查看历史记录')
    handleLogin()
    return
  }
  userStore.setDefaultPage('2-5')
  if (
    drawerBox.value &&
    typeof drawerBox.value.handleUserCenter === 'function'
  ) {
    drawerBox.value.handleUserCenter()
  }
}

//处理获取用户中心函数
const handleUserCenter = () => {
  if (!userStore.basicUser.token) {
    ElMessage.warning('请先登录进入个人中心')
    handleLogin()
    return
  }
  userStore.setDefaultPage('1-1')
  if (
    drawerBox.value &&
    typeof drawerBox.value.handleUserCenter === 'function'
  ) {
    drawerBox.value.handleUserCenter()
  }
}
</script>

<template>
  <el-container class="container">
    <el-header class="container-header">
      <el-menu
        :default-active="$route.path"
        class="el-menu-demo"
        mode="horizontal"
        :ellipsis="false"
        router
      >
        <logoBox></logoBox>
        <el-menu-item index="/homePage" class="nav-item">
          <img style="width: 30px" src="@/assets/light.png" />
          <span class="nav-text">首页</span>
        </el-menu-item>
        <el-menu-item index="/userCommunity" class="nav-item">
          <img style="width: 30px" src="@/assets/community.png" />
          <span class="nav-text">社区</span>
        </el-menu-item>
        <el-menu-item index="/CBT" class="nav-item">
          <img style="width: 30px" src="@/assets/health.png" />
          <span class="nav-text">CBT</span>
        </el-menu-item>
        <div class="flex-grow" />
        <ul class="userModules">
          <el-avatar
            :size="42"
            :src="userImg"
            class="avatar"
            :class="{
              'add-hover-effect': isAvatarHover,
              'avatar-hover-effect': userStore.basicUser.token
            }"
            @click="handleLogin"
          >
            登录
          </el-avatar>
          <div
            class="userCenter"
            v-if="userStore.basicUser.token"
            @mouseenter="handleAvatarHover"
            @mouseleave="handleAvatarLeave"
          >
            <div class="userNickname">{{ userStore.basicUser.nickname }}</div>
            <div class="userSocialBox">
              <div class="socialBox">
                1
                <span>关注</span>
              </div>
              <div class="socialBox">
                2
                <span>粉丝</span>
              </div>
              <div class="socialBox">
                3
                <span>动态</span>
              </div>
            </div>
            <smallBox @click="handleUserCenter">
              <img src="@/assets/my.png" /> <span>个人中心</span>
            </smallBox>
            <smallBox @click="handleLoginOut">
              <img src="@/assets/退出登录.png" /> <span>退出登录</span>
            </smallBox>
          </div>
          <li @click="handleDynamic">
            <img src="@/assets/动态.png" /><span>动态</span>
          </li>
          <li @click="handleFavorites">
            <img src="@/assets/收藏.png" /><span>收藏</span>
          </li>
          <li @click="handleHistory">
            <img src="@/assets/历史记录.png" /><span>历史</span>
          </li>
          <li @click="handleMessages">
            <img src="@/assets/消息.png" /><span>消息</span>
          </li>
          <li @click="handleUserCenter">
            <img src="@/assets/设置.png" /><span>设置</span>
          </li>
        </ul>
      </el-menu>
    </el-header>
    <el-main class="container-main">
      <loginPage ref="handleLoginPage"></loginPage>
      <userCenter ref="drawerBox"></userCenter>
      <router-view v-slot="{ Component }">
        <keep-alive :include="['HomePage', 'UserCommunity', 'CBT']">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </el-main>
  </el-container>
</template>

<style scoped>
div {
  box-sizing: border-box;
}
.flex-grow {
  flex-grow: 1;
}

/* 设置容器的最小宽度和最大宽度 */
.container {
  margin: 0;
  width: 100%;
  height: 100%;
}

.el-header {
  width: 100%;
  padding: 0;
  height: 60px;
  background: linear-gradient(135deg, #fff3e2 0%, #ccfcfa 100%);
}

.el-menu-demo {
  width: 100%;
  border-bottom: none;
  background: transparent !important;
}

/* 导航项样式 */
.nav-item {
  position: relative;
  transition: all 0.3s ease;
  border-radius: 12px;
  margin: 0 8px;
  padding: 8px 16px !important;
}

.nav-item::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.nav-item:hover::before {
  width: 80%;
}

.nav-item.is-active::before {
  width: 87%;
}

.nav-item.is-active {
  background: linear-gradient(
    135deg,
    rgba(102, 126, 234, 0.1) 0%,
    rgba(118, 75, 162, 0.1) 100%
  );
  transform: translateY(-2px);
}

.nav-item.is-active .nav-text {
  color: #667eea;
  font-weight: 600;
}

.nav-text {
  margin-left: 8px;
  user-select: none;
  font-size: 15px;
  font-weight: 500;
  color: #333;
  transition: all 0.3s ease;
}

.nav-item:hover .nav-text {
  color: #667eea;
}

.container-header .userModules {
  position: relative;
  display: flex;
  align-items: center;
}

.container-header .userModules li {
  display: flex;
  height: 100%;
  width: 50px;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  list-style-type: none;
  cursor: pointer;
  user-select: none;
}
.container-header .userModules .avatar {
  margin-right: 10px;
  cursor: pointer;
  user-select: none;
  transition: transform 0.3s;
  border: 2px solid #e1e4eb;
}

/* 设置头像的hover效果  */
.avatar-hover-effect:hover {
  transform: translate(-40%, 70%) scale(2);
  z-index: 999;
}

/* 当鼠标悬停在个人中心时，头像加上这个类名  */
.add-hover-effect {
  transform: translate(-40%, 70%) scale(2);
  z-index: 999;
}

/* 用户登录后的个人中心 */
.userCenter {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding-top: 50px;
  padding-bottom: 20px;
  box-sizing: border-box;
  width: 260px;
  height: 0;
  top: 100%;
  left: -21%;
  border-radius: 8px;
  z-index: 998;
  opacity: 0;
  visibility: hidden;
  transition:
    opacity 0.3s,
    visibility 0.3s,
    height 0.3s ease;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.1);
  background: linear-gradient(#d6f7ff, #fdfae8);
}

/* 用户登录后的个人中心hover效果 */
.avatar-hover-effect:hover + .userCenter,
.userCenter:hover {
  opacity: 1;
  visibility: visible;
  height: 310px;
}

/* 设置用户个人中心功能 */
/* 设置用户社交功能，例如关注、粉丝、动态 */
.userSocialBox {
  display: flex;
  justify-content: space-around;
  width: 100%;
  margin-top: 15px;
  margin-bottom: 15px;
}

.socialBox {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.container-header .userModules li:hover {
  background-color: #ecf5ff;
}
.container-header .userModules li img {
  height: 25px;
  margin-bottom: 2px;
}
.container-header .userModules li span {
  font-size: 12px;
}
.container-header .userModules li:last-child {
  margin-right: 20px;
}

.container-main {
  padding: 0;
  margin: 0;
  height: calc(100% - 60px);
  overflow: auto;
}
</style>
