<script setup>
import { ref, watch, nextTick } from 'vue'
import { useUserStore } from '@/stores' //获取用户信息
const userStore = useUserStore() //获取用户信息

const avatar = userStore.basicUser.avatar //获取用户头像
import axios from 'axios'
import { ElMessage } from 'element-plus'

const drawer = ref(false)
const inputText = ref('')
const messages = ref([])

const chatList = ref(null)

const scrollToBottom = () => {
  nextTick(() => {
    if (chatList.value) {
      // 尝试使用 setTimeout 来确保 DOM 更新
      // setTimeout(() => {
      chatList.value.scrollTop = chatList.value.scrollHeight
      // console.log(chatList.value.scrollHeight)
      // console.log(chatList.value.scrollTop)
      // }, 0)
    }
  })
}

//设置一个定时器
const timer = ref(null)

//获取输入框
const inputRef = ref(null)

const sendMessage = () => {
  if (timer.value) {
    ElMessage.warning('请等待回答后才可发送哦！')
    return
  }
  if (inputText.value.trim()) {
    timer.value = setInterval(() => {}, 1000)
    const newMessage = {
      text: inputText.value,
      isMine: true // 表示这是我发送的消息
    }
    messages.value.push(newMessage)
    simulateServerResponse(newMessage.text)
    inputText.value = '' // 清空输入框
    scrollToBottom()
  }
}

const simulateServerResponse = (userInput) => {
  // 真实的服务器请求
  axios({
    url: 'http://hmajax.itheima.net/api/robot',
    params: {
      spoken: userInput
    }
  })
    .then((result) => {
      // 假设服务器返回了一个包含文本的消息对象
      if (result.data.data.info.text) {
        clearInterval(timer.value)
        timer.value = null
        const serverResponseMessage = {
          text: result.data.data.info.text,
          isMine: false
        }
        messages.value.push(serverResponseMessage)
        scrollToBottom()
      }
      // console.log(result)
    })
    .catch(() => {
      clearInterval(timer.value)
      timer.value = null
      ElMessage.error('服务器错误，请稍后重试')
    })
}

// 监听输入框，优化滚动
watch(inputText, () => {
  if (inputText.value && messages.value.length > 0) {
    scrollToBottom()
  }
})

//设置打开抽屉函数
const open = () => {
  drawer.value = true
  //一进页面就获取焦点
  nextTick(() => {
    setTimeout(() => {
      if (inputRef.value && typeof inputRef.value.focus === 'function') {
        inputRef.value.focus()
      }
    }, 300)
  })
}

defineExpose({
  open
})
</script>

<template>
  <el-drawer v-model="drawer" :with-header="false" size="35%">
    <div class="container">
      <!-- 顶部标题 -->
      <div class="topTitle">
        <span>心灵沟通室</span>
      </div>

      <!-- 聊天区域 -->
      <div class="chat" ref="chatList">
        <div class="chat_list">
          <li>
            <img
              src="@/assets/logo.png"
              alt="接收者"
              style="width: 50px; border-radius: 50%; border: 2px #fff solid"
            />
            <span class="leftChat">今天过的怎么样呀？</span>
          </li>
          <!-- 消息列表渲染 -->
          <li
            v-for="(message, index) in messages"
            :key="index"
            :class="message.isMine ? 'right' : 'left'"
          >
            <img
              v-if="!message.isMine"
              src="@/assets/logo.png"
              alt="接收者"
              style="width: 50px; border-radius: 50%; border: 2px #fff solid"
            />
            <span :class="message.isMine ? 'rightChat' : 'leftChat'">{{
              message.text
            }}</span>
            <img
              v-if="message.isMine"
              :src="avatar"
              alt="发送者"
              style="width: 50px; border-radius: 50%; border: 2px #fff solid"
            />
          </li>
        </div>
      </div>

      <!-- 底部固定 -->
      <div class="bottom_div">
        <!-- 发送消息 -->
        <div class="send_box">
          <div class="input_bg">
            <input
              v-model="inputText"
              class="chat_input"
              ref="inputRef"
              type="text"
              placeholder="说点什么吧"
              @keyup.enter="sendMessage"
            />
          </div>

          <img
            class="send_img"
            src="@/assets/send.png"
            alt="发送"
            @click="sendMessage"
          />
        </div>
        <!-- 底部黑条 -->
        <div class="black_border">
          <span></span>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
li {
  list-style: none;
}
.container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(#d6f7ff, #fdfae8);
  box-sizing: border-box;
  padding: 0 20px;
  border-radius: 8px;
}

/* 对顶部标题进行处理 */
.topTitle {
  height: 50px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #000;
  font-size: 20px;
  box-sizing: border-box;
}

/* 对聊天区域进行处理 */
.chat {
  width: 100%;
  height: 670px;
  overflow-y: auto;
  box-sizing: border-box;
  padding: 20px 0;
  overflow-y: scroll;
}

/* 隐藏滚动条 */
.chat::-webkit-scrollbar {
  display: none;
}

.chat_list li {
  display: flex;
  margin-top: 10px;
}
.chat .right {
  display: flex;
  justify-content: flex-end;
}

/* 对左边的聊天信息背景设置 */
.leftChat {
  margin-left: 10px;
  border-radius: 1px 10px 1px 10px;
  display: inline-block;
  padding: 12px 16px;
  background-image: linear-gradient(180deg, #b1e393 0%, #50d287 100%);
  box-shadow: 2px 2px 10px 0px rgba(201, 201, 201, 0.1);
  color: #ffffff;
}

.rightChat {
  margin-right: 10px;
  border-radius: 1px 10px 1px 10px;
  display: inline-block;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid rgba(247, 247, 247, 1);
  color: #000000;
}
/* 对底部区域进行设置 */
.bottom_div {
  width: 100%;
  height: 89px;
  margin-top: 15px;
  background-color: #fff;
  box-shadow: 0px -5px 7px 0px rgba(168, 168, 168, 0.05);
  border-radius: 25px 25px 0px 0px;
  padding: 15px 15px 0px 15px;
  box-sizing: border-box;
}

/* 外框盒子 */
.send_box {
  display: flex;
}

.send_box img {
  width: 34px;
  height: 34px;
  background-color: transparent;
}

/* 输入框背景 */
.input_bg {
  height: 35px;
  background: #f3f3f3;
  border-radius: 50px;
  padding-left: 17px;
  flex: 1;
  margin-right: 15px;
  /* 让input宽度高度 */
  display: flex;
}

.input_bg input {
  border: 0;
  outline: 0;
  background-color: transparent;
  display: inline-block;
  width: 100%;
}

/* 底部黑色小条 */
.black_border {
  margin-top: 10px;
  height: 34px;
  text-align: center;
}

.black_border span {
  display: inline-block;
  background-color: #000;
  width: 105px;
  height: 4px;
  border-radius: 50px;
}
</style>
