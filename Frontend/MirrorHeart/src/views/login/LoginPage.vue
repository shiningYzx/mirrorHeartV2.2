<script setup>
import { h, ref, watch, nextTick } from 'vue'
import { Close, Lock, Iphone, Key, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores'
import otherWayLoginBox from '@/components/login/OtherWayLoginBox.vue'
import { useRouter } from 'vue-router'
import {
  userGetRegisterCodeService,
  userGetLoginCodeService,
  userMsgLoginService,
  userRegisterService,
  userCodeLoginService
} from '@/api/login.js'
import { getUserBasicInfoService } from '@/api/userCenter.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const showLogin = ref(false)

const handleClose = () => {
  showLogin.value = false
}
const handleLogin = () => {
  showLogin.value = true
  //初始化表单时，自动聚焦到邮箱输入框
  setTimeout(() => {
    nextTick(() => {
      email.value?.focus()
    })
  }, 350)
}

const formModel = ref({
  email: '',
  password: '',
  smsCode: '',
  rePassword: '',
  nickname: ''
})

// 设置登录和注册的逻辑转换
const isRegister = ref(false)

//监听是否注册，如果注册则清空表单
watch(isRegister, () => {
  if (isRegister.value === true) {
    nextTick(() => {
      emailRegister.value?.focus()
    })
  } else {
    nextTick(() => {
      email.value?.focus()
    })
  }
  formModel.value = {
    email: '',
    password: '',
    smsCode: '',
    rePassword: '',
    nickname: ''
  }
})

//配置表单规则
const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
      message: '邮箱格式不正确',
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[0-9])(?=.*[a-zA-Z])[0-9a-zA-Z]{8,15}$/,
      message: '密码长度8至15位，必须同时包含字母和数字。',
      trigger: 'blur'
    }
  ],
  smsCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    {
      pattern: /^\d{6}$/,
      message: '验证码必须是6位数字',
      trigger: 'blur'
    }
  ],
  rePassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[0-9])(?=.*[a-zA-Z])[0-9a-zA-Z]{8,15}$/,
      message: '密码长度8至15位，必须同时包含字母和数字。',
      trigger: 'blur'
    },
    {
      validator: (rule, value, callback) => {
        if (value !== formModel.value.password) {
          callback(new Error('两次输入密码不一致!'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const form = ref()
const CodeButtonOne = ref()
const CodeButtonTwo = ref()

//对注册账号时的获取验证码秒数进行设置
const countDownOne = ref(60)
const timerOne = ref(null)

// 发送验证码并且校验
const handleCode = async () => {
  const isTrue = await form.value.validateField('email')
  if (isTrue) {
    if (!timerOne.value && countDownOne.value === 60) {
      try {
        const res = await userGetRegisterCodeService(formModel.value)
        ElMessage.success(res.data.message || '验证码已发送')
        timerOne.value = setInterval(() => {
          countDownOne.value--
          if (countDownOne.value === 0) {
            clearInterval(timerOne.value)
            countDownOne.value = 60
            timerOne.value = null
          }
        }, 1000)
      } catch (error) {
        ElMessage.error(
          error.response?.data?.message || '发送验证码失败，请稍后重试'
        )
      }
    }
  }
}

//处理注册代码
const handleRegister = async () => {
  try {
    const isTrue = await form.value.validate()
    if (isTrue) {
      const res = await userRegisterService(formModel.value)
      ElMessage.success(res.data.message || '注册成功')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '注册失败，请稍后重试')
    return
  }
  isRegister.value = false
  clearInterval(timerOne.value)
  countDownOne.value = 60
  formModel.value = {
    email: '',
    password: '',
    smsCode: '',
    rePassword: '',
    nickname: ''
  }
}

//处理登录代码
const isPhoneLogin = ref(false)

//处理密码登录的代码
const handleCodeLogin = async () => {
  try {
    await form.value.validate()
    const res = await userCodeLoginService(formModel.value)
    const { token, refreshToken, tokenHead, expiresIn } = res.data.data
    userStore.setBasicUser({ token, refreshToken, tokenHead, expiresIn })

    const userInfoRes = await getUserBasicInfoService()
    userStore.setBasicUser({
      token,
      refreshToken,
      tokenHead,
      expiresIn,
      uid: userInfoRes.data.data.id,
      userId: userInfoRes.data.data.email,
      nickname: userInfoRes.data.data.nickname,
      avatar: userInfoRes.data.data.avatarUrl,
      subscriber: userInfoRes.data.data.role,
      signature: userInfoRes.data.data.bio
    })

    ElMessage.success('登录成功')
    showLogin.value = false
    location.reload()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败，请稍后重试')
  }
}

//监听密码登录与短信登录切换并清空表单
watch(isPhoneLogin, () => {
  if (isPhoneLogin.value === true) {
    nextTick(() => emailLogin.value?.focus())
  } else {
    nextTick(() => email.value?.focus())
  }
  formModel.value = {
    email: '',
    password: '',
    smsCode: '',
    rePassword: '',
    nickname: ''
  }
})

const countDownTwo = ref(60)
const timerTwo = ref(null)

//处理获取短信验证码
const getLoginCode = async () => {
  const isTrue = await form.value.validateField('email')
  if (isTrue) {
    if (!timerTwo.value && countDownTwo.value === 60) {
      try {
        const res = await userGetLoginCodeService(formModel.value)
        ElMessage.success(res.data.message || '验证码已发送')
        timerTwo.value = setInterval(() => {
          countDownTwo.value--
          if (countDownTwo.value === 0) {
            clearInterval(timerTwo.value)
            countDownTwo.value = 60
            timerTwo.value = null
          }
        }, 1000)
      } catch (error) {
        ElMessage.error(
          error.response?.data?.message || '发送验证码失败，请稍后重试'
        )
      }
    }
  }
}

//处理短信登录的代码
const handleMsgLogin = async () => {
  try {
    await form.value.validate()
    const res = await userMsgLoginService(formModel.value)
    const { token, refreshToken, tokenHead, expiresIn } = res.data.data
    userStore.setBasicUser({ token, refreshToken, tokenHead, expiresIn })

    const userInfoRes = await getUserBasicInfoService()
    userStore.setBasicUser({
      token,
      refreshToken,
      tokenHead,
      expiresIn,
      uid: userInfoRes.data.data.id,
      userId: userInfoRes.data.data.email,
      nickname: userInfoRes.data.data.nickname,
      avatar: userInfoRes.data.data.avatarUrl,
      subscriber: userInfoRes.data.data.role,
      signature: userInfoRes.data.data.bio
    })

    ElMessage.success('登录成功')
    showLogin.value = false
    clearInterval(timerTwo.value)
    countDownTwo.value = 60
    location.reload()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败，请稍后重试')
  }
}

//处理忘记密码的代码
const handleCommand = (command) => {
  if (command === 'a') {
    isPhoneLogin.value = true
  } else if (command === 'b') {
    router.push('/findPassword')
    showLogin.value = false
  }
}

//给输入框设置ref
const emailRegister = ref(null)
const emailLogin = ref(null)
const email = ref(null)
const smsCode = ref(null)
const password = ref(null)
const rePassword = ref(null)
const nickname = ref(null)

//处理回车事件
const handleEnter = (oldValue, newValue) => {
  if (oldValue === emailRegister.value) handleCode()
  else if (oldValue === emailLogin.value) getLoginCode()

  if (oldValue && typeof oldValue.blur === 'function') oldValue.blur()
  if (newValue && typeof newValue.focus === 'function') newValue.focus()
}

defineExpose({
  handleLogin
})
</script>

<template>
  <div class="loginShade" :class="{ show: showLogin }">
    <div class="loginBox">
      <!-- 关闭按钮 -->
      <div class="close-btn-wrapper" @click="handleClose">
        <el-icon :size="20"><Close /></el-icon>
      </div>

      <!-- 右侧表单区 (利用 flex 布局挤到右边) -->
      <div class="form-panel">
        <!-- 🌟 注册面板 -->
        <transition name="fade-slide" mode="out-in">
          <el-form
            v-if="isRegister"
            :model="formModel"
            :rules="rules"
            ref="form"
            class="auth-form"
            key="register"
          >
            <div class="form-header">
              <h2>欢迎加入</h2>
              <p>探索属于你的内心世界</p>
            </div>

            <el-form-item prop="email">
              <el-input
                size="large"
                class="modern-input"
                :prefix-icon="Iphone"
                v-model="formModel.email"
                placeholder="请输入邮箱"
                ref="emailRegister"
                @keyup.enter="handleEnter(emailRegister, smsCode)"
              >
                <template #suffix>
                  <div class="divider-line"></div>
                  <span
                    class="send-code-btn"
                    :class="{ 'is-disabled': countDownOne !== 60 }"
                    @click="handleCode"
                  >
                    {{
                      countDownOne === 60
                        ? '获取验证码'
                        : `${countDownOne}s后重新获取`
                    }}
                  </span>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="smsCode">
              <el-input
                size="large"
                class="modern-input"
                :prefix-icon="Key"
                v-model="formModel.smsCode"
                placeholder="请输入验证码"
                ref="smsCode"
                @keyup.enter="handleEnter(smsCode, password)"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                size="large"
                class="modern-input"
                :prefix-icon="Lock"
                v-model="formModel.password"
                placeholder="请输入密码"
                type="password"
                ref="password"
                show-password
                @keyup.enter="handleEnter(password, rePassword)"
              />
            </el-form-item>

            <el-form-item prop="rePassword">
              <el-input
                size="large"
                class="modern-input"
                :prefix-icon="Lock"
                v-model="formModel.rePassword"
                placeholder="请再次输入密码"
                ref="rePassword"
                show-password
                @keyup.enter="handleEnter(rePassword, nickname)"
                type="password"
              />
            </el-form-item>

            <el-form-item prop="nickname">
              <el-input
                size="large"
                class="modern-input"
                :prefix-icon="User"
                v-model="formModel.nickname"
                placeholder="给自起个好听的昵称吧"
                ref="nickname"
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <el-button class="submit-btn" type="primary" @click="handleRegister"
              >注 册</el-button
            >

            <div class="form-footer">
              <span>已有账号？</span>
              <a class="link-text" @click="isRegister = false">去登录</a>
            </div>
          </el-form>

          <!-- 🌟 登录面板 -->
          <el-form
            v-else
            class="auth-form"
            :model="formModel"
            :rules="rules"
            ref="form"
            key="login"
          >
            <!-- 现代化的 Tab 切换 -->
            <div class="modern-tabs">
              <div
                class="tab-item"
                :class="{ active: !isPhoneLogin }"
                @click="isPhoneLogin = false"
              >
                密码登录
              </div>
              <div
                class="tab-item"
                :class="{ active: isPhoneLogin }"
                @click="isPhoneLogin = true"
              >
                验证码登录
              </div>
            </div>

            <!-- 短信登录内容 -->
            <div v-if="isPhoneLogin" class="tab-content">
              <el-form-item prop="email">
                <el-input
                  v-model="formModel.email"
                  size="large"
                  class="modern-input"
                  :prefix-icon="Iphone"
                  placeholder="请输入邮箱"
                  ref="emailLogin"
                  @keyup.enter="handleEnter(emailLogin, smsCode)"
                >
                  <template #suffix>
                    <div class="divider-line"></div>
                    <span
                      class="send-code-btn"
                      :class="{ 'is-disabled': countDownTwo !== 60 }"
                      @click="getLoginCode"
                    >
                      {{
                        countDownTwo === 60
                          ? '获取验证码'
                          : `${countDownTwo}s后重发`
                      }}
                    </span>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item prop="smsCode">
                <el-input
                  v-model="formModel.smsCode"
                  size="large"
                  class="modern-input"
                  :prefix-icon="Key"
                  placeholder="请输入验证码"
                  ref="smsCode"
                  @keyup.enter="handleMsgLogin"
                />
              </el-form-item>
              <el-button
                class="submit-btn"
                type="primary"
                @click="handleMsgLogin"
                >登 录</el-button
              >
            </div>

            <!-- 密码登录内容 -->
            <div v-else class="tab-content">
              <el-form-item prop="email">
                <el-input
                  size="large"
                  class="modern-input"
                  v-model="formModel.email"
                  :prefix-icon="Iphone"
                  placeholder="请输入邮箱"
                  ref="email"
                  @keyup.enter="handleEnter(email, password)"
                />
              </el-form-item>

              <el-form-item prop="password">
                <el-input
                  size="large"
                  class="modern-input"
                  :prefix-icon="Lock"
                  v-model="formModel.password"
                  placeholder="请输入密码"
                  type="password"
                  ref="password"
                  show-password
                  @keyup.enter="handleCodeLogin"
                />
              </el-form-item>

              <div class="extra-actions">
                <el-dropdown @command="handleCommand" trigger="click">
                  <span class="forgot-pwd-text">忘记密码？</span>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="a"
                        >使用验证码快速登录</el-dropdown-item
                      >
                      <el-dropdown-item command="b"
                        >去找回密码</el-dropdown-item
                      >
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>

              <el-button
                class="submit-btn"
                type="primary"
                @click="handleCodeLogin"
                >登 录</el-button
              >
            </div>

            <otherWayLoginBox />

            <div class="form-footer" style="margin-top: 20px">
              <span>还没账号？</span>
              <a class="link-text" @click="isRegister = true">立即注册</a>
            </div>
          </el-form>
        </transition>
      </div>
    </div>
  </div>
</template>

<style scoped>
.loginShade {
  position: fixed;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  background-color: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px); /* 整体毛玻璃遮罩 */
  opacity: 0;
  visibility: hidden;
  transition:
    opacity 0.4s cubic-bezier(0.25, 0.8, 0.25, 1),
    visibility 0.4s;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loginShade.show {
  opacity: 1;
  visibility: visible;
}

/* 🌟 核心弹窗容器：放弃硬编码的 padding-left，采用 Flex 排列 */
.loginBox {
  width: 860px;
  min-height: 520px;
  background-image: url('@/assets/loginBac.png');
  background-size: cover;
  background-position: center left;
  background-color: #fff;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  position: relative;
  display: flex;
  justify-content: flex-end; /* 把表单推到右侧 */
  overflow: hidden;
  transform: scale(0.95);
  transition: transform 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.loginShade.show .loginBox {
  transform: scale(1);
}

.close-btn-wrapper {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 32px;
  height: 32px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  color: #606266;
  transition: all 0.3s;
}

.close-btn-wrapper:hover {
  background: #f56c6c;
  color: #fff;
  transform: rotate(90deg);
}

/* 🌟 右侧表单区面板：毛玻璃质感 */
.form-panel {
  width: 420px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(15px);
  padding: 40px 45px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  border-left: 1px solid rgba(255, 255, 255, 0.5);
}

.auth-form {
  width: 100%;
}

/* --- 注册表头 --- */
.form-header {
  margin-bottom: 25px;
  text-align: center;
}
.form-header h2 {
  font-size: 24px;
  color: #2c3e50;
  margin: 0 0 5px 0;
  font-weight: 800;
}
.form-header p {
  color: #909399;
  font-size: 13px;
  margin: 0;
}

/* --- 现代化的 Tab 切换 --- */
.modern-tabs {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
  border-bottom: 2px solid #ebeef5;
}

.tab-item {
  font-size: 18px;
  color: #909399;
  padding-bottom: 12px;
  cursor: pointer;
  position: relative;
  transition: all 0.3s;
  font-weight: 500;
}

.tab-item:hover {
  color: #66b1ff;
}

.tab-item.active {
  color: #409eff;
  font-weight: bold;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 100%;
  height: 3px;
  background-color: #409eff;
  border-radius: 3px 3px 0 0;
}

/* --- 输入框美化 --- */
.modern-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 6px 15px;
  background-color: #f5f7fa;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.3s;
}

.modern-input :deep(.el-input__wrapper.is-focus) {
  background-color: #ffffff;
  border: 1px solid #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1) !important;
}

.modern-input :deep(.el-input__inner) {
  height: 36px;
}

/* 验证码分割线与按钮 */
.divider-line {
  height: 16px;
  width: 1px;
  background-color: #dcdfe6;
  margin: 0 12px;
}

.send-code-btn {
  color: #409eff;
  font-size: 13px;
  cursor: pointer;
  user-select: none;
  transition: color 0.3s;
  font-weight: 500;
  min-width: 75px;
  text-align: right;
}

.send-code-btn:hover {
  color: #79bbff;
}

.send-code-btn.is-disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}

/* --- 额外操作区 (忘记密码) --- */
.extra-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: -10px;
  margin-bottom: 20px;
}

.forgot-pwd-text {
  font-size: 13px;
  color: #909399;
  cursor: pointer;
  transition: color 0.2s;
}
.forgot-pwd-text:hover {
  color: #409eff;
}

/* --- 提交大按钮 --- */
.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 100%);
  border: none;
  font-weight: bold;
  letter-spacing: 2px;
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.25);
  transition: all 0.3s;
  margin-top: 10px;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.35);
}

.submit-btn:active {
  transform: translateY(0);
}

/* --- 底部注册/登录跳转 --- */
.form-footer {
  text-align: center;
  font-size: 14px;
  color: #606266;
  margin-top: 15px;
}

.link-text {
  color: #409eff;
  cursor: pointer;
  font-weight: bold;
  transition: color 0.2s;
}

.link-text:hover {
  color: #79bbff;
  text-decoration: underline;
}

/* --- 切换动画 --- */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
