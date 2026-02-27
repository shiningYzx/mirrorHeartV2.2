<script lang="ts" setup>
import { ref, nextTick } from 'vue'
import { Iphone, Key, Lock } from '@element-plus/icons-vue'
import { ElMessage, ElSteps, ElForm, ElInput, ElButton } from 'element-plus'
import { userGetForgetPwdCodeService, userResetPwdService } from '@/api/login'
import router from '@/router'

const active = ref(0) // 当前活动的步骤

//初始化页面时，自动聚焦到手机号码输入框
setTimeout(() => {
  nextTick(() => {
    if (phoneNum.value && typeof phoneNum.value.focus === 'function') {
      phoneNum.value.focus()
    }
  })
}, 350)

const steps = [
  {
    title: '确认手机号码',
    template: '请确认您的手机号码'
  },
  {
    title: '输入手机验证码',
    template: '请确认您的手机验证码'
  },
  {
    title: '输入新密码',
    template: '请设置您的新密码'
  },
  {
    title: '再次输入新密码',
    template: '请再次设置您的新密码'
  }
]

const formModel = ref({
  phoneNum: '',
  newPassword: '',
  smsCode: '',
  reNewPassword: ''
})

//配置表单规则
const rules = {
  phoneNum: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '手机号码格式不正确',
      trigger: 'blur'
    }
  ],
  newPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      pattern: /^\S{6,15}$/,
      message: '密码必须是6-15位的非空字符',
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
  reNewPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      pattern: /^\S{6,15}$/,
      message: '密码必须是6-15的非空字符',
      trigger: 'blur'
    },
    {
      validator: (rule, value, callback) => {
        if (value !== formModel.value.newPassword) {
          callback(new Error('两次输入密码不一致!'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}
//为输入框命名
const phoneNum = ref(null)
const smsCode = ref(null)
const newPassword = ref(null)
const reNewPassword = ref(null)

const next = async () => {
  if (active.value === 0) {
    const isTrue = await form.value.validateField('phoneNum')
    if (isTrue) {
      if (timer.value) {
        if (active.value < steps.length - 1) {
          active.value++
          nextTick(() => {
            if (smsCode.value && typeof smsCode.value.focus === 'function') {
              smsCode.value.focus()
            }
          })
        }
      } else {
        ElMessage.error('请先获取手机验证码')
        return
      }
    }
  } else if (active.value === 1) {
    const isTrue = await form.value.validateField('smsCode')
    if (isTrue) {
      if (active.value < steps.length - 1) {
        active.value++
        nextTick(() => {
          if (
            newPassword.value &&
            typeof newPassword.value.focus === 'function'
          ) {
            newPassword.value.focus()
          }
        })
      }
    }
  } else if (active.value === 2) {
    const isTrue = await form.value.validateField('newPassword')
    if (isTrue) {
      if (active.value < steps.length - 1) {
        active.value++
        nextTick(() => {
          if (
            reNewPassword.value &&
            typeof reNewPassword.value.focus === 'function'
          ) {
            reNewPassword.value.focus()
          }
        })
      }
    }
  } else if (active.value === 3) {
    const isTrue = await form.value.validateField('reNewPassword')
    if (isTrue) {
      const res = await userResetPwdService(formModel.value)
      ElMessage.success(res.data.msg)
      router.push('/homePage')
    }
  }
}

const prev = () => {
  if (active.value > 0) {
    active.value--
  }
}
const form = ref() // 表单实例
const countDown = ref(60) // 倒计时秒数
const timer = ref(null) // 定时器
const CodeButton = ref(null) // 获取验证码按钮
//获取短信验证码
const getMsgCode = async () => {
  const isTrue = await form.value.validateField('phoneNum')
  if (isTrue) {
    //判断是否在倒计时,不存在则发送验证码或者倒计时秒数为60
    if (!timer.value && countDown.value === 60) {
      // 发送验证码请求
      const res = await userGetForgetPwdCodeService(formModel.value)
      ElMessage.success(res.data.msg)
      CodeButton.value.style.color = '#9adaee'
      // 防止重复点击
      timer.value = setInterval(() => {
        countDown.value--
        if (countDown.value === 0) {
          clearInterval(timer.value)
          countDown.value = 60
          timer.value = null
          CodeButton.value.style.color = '#a8abb2'
        }
      }, 1000)
    } else {
      return
    }
  }
}

//处理手机号码输入框回车事件
const handlePhoneEnter = () => {
  getMsgCode()
  setTimeout(() => {
    next()
  }, 350)
}
</script>

<template>
  <div class="container">
    <el-steps :active="active" align-center style="width: 650px; height: 80px">
      <el-step
        v-for="(step, index) in steps"
        :key="index"
        :title="step.title"
      />
    </el-steps>
    <div v-if="active === 0" class="stepBox">
      <!-- 第一步：确认手机号码 -->
      <el-form
        @submit.prevent
        label-width="auto"
        style="max-width: 600px"
        ref="form"
        :model="formModel"
        :rules="rules"
      >
        <el-form-item prop="phoneNum">
          <el-input
            :prefix-icon="Iphone"
            size="large"
            class="phone-element"
            placeholder="请输入你的手机号码"
            style="width: 300px"
            v-model="formModel.phoneNum"
            @keyup.enter="handlePhoneEnter"
            ref="phoneNum"
          >
            <template #suffix>
              <div style="margin-right: 10px">|</div>
              <span
                style="cursor: pointer; width: 80px"
                ref="CodeButton"
                @click="getMsgCode"
                >{{
                  countDown === 60 ? '获取验证码' : `重新发送(${countDown})`
                }}</span
              >
            </template></el-input
          >
        </el-form-item>
      </el-form>
    </div>
    <div v-if="active === 1" class="stepBox">
      <!-- 第二步：确认手机验证码 -->
      <el-form
        @submit.prevent
        label-width="auto"
        style="max-width: 600px"
        :model="formModel"
        :rules="rules"
        ref="form"
      >
        <el-form-item prop="smsCode">
          <el-input
            :prefix-icon="Key"
            size="large"
            class="phone-element"
            placeholder="请输入你的手机验证码"
            style="width: 300px"
            v-model="formModel.smsCode"
            ref="smsCode"
            @keyup.enter="next"
          ></el-input>
        </el-form-item>
      </el-form>
    </div>
    <div v-else-if="active === 2" class="stepBox">
      <!-- 第三步：输入新密码 -->
      <el-form
        @submit.prevent
        label-width="auto"
        style="max-width: 600px"
        :model="formModel"
        :rules="rules"
        ref="form"
      >
        <el-form-item prop="newPassword">
          <el-input
            size="large"
            :prefix-icon="Lock"
            type="password"
            placeholder="请输入新密码"
            style="width: 300px"
            v-model="formModel.newPassword"
            ref="newPassword"
            @keyup.enter="next"
          ></el-input>
        </el-form-item>
      </el-form>
    </div>
    <div v-else-if="active === 3" class="stepBox">
      <!-- 第四步：再次输入新密码 -->
      <el-form
        @submit.prevent
        label-width="auto"
        style="max-width: 600px"
        :model="formModel"
        :rules="rules"
        ref="form"
      >
        <el-form-item prop="reNewPassword">
          <el-input
            size="large"
            :prefix-icon="Lock"
            type="password"
            placeholder="请再次输入新密码"
            style="width: 300px"
            v-model="formModel.reNewPassword"
            ref="reNewPassword"
            @keyup.enter="next"
          ></el-input>
        </el-form-item>
      </el-form>
    </div>
    <div style="margin-top: 20px">
      <el-button v-if="active > 0" @click="prev">上一步</el-button>
      <el-button type="primary" @click="next">
        {{ active === 3 ? '提交' : '下一步' }}</el-button
      >
    </div>
  </div>
</template>

<style scoped>
.container {
  width: 100%; /* 设置固定宽度 */
  height: 600px; /* 设置固定高度 */
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  margin: auto; /* 居中容器 */
  padding: 20px; /* 为内容提供一些内边距 */
  box-sizing: border-box; /* 确保宽度和高度包括内边距 */
}

.stepBox {
  width: 520px; /* 使步骤条宽度填满容器宽度 */
  height: 80px; /* 设置固定高度 */
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
