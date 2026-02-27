<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  commentFirstPostService, //一级评论帖子接口
  commentSecondPostService //二级评论帖子接口
} from '@/api/userCommunity.js'
import { useUserStore } from '@/stores' //获取用户信息

// 获取用户信息
const userStore = useUserStore()

//输入框是否聚焦
const isFocused = ref(false)

const row = ref(1)

const handleFocus = () => {
  isFocused.value = true
  row.value = 4 // 聚焦时优雅展开
}

const handleBlur = () => {
  // 延迟失去焦点，防止点击发送按钮时由于失焦导致按钮先消失
  setTimeout(() => {
    if (!commentContent.value) {
      isFocused.value = false
      row.value = 1
    }
  }, 200)
}

const props = defineProps({
  message: Number,
  firstCommendId: Number
})

//处理子传父
const emit = defineEmits(['updateCommend'])

//评论内容
const commentContent = ref('')

//处理评论提交
const submitFirstComment = async () => {
  if (props.message && props.firstCommendId) {
    if (commentContent.value.trim() === '') {
      ElMessage.warning('评论内容不能为空')
      return
    }
    const res = await commentSecondPostService(
      props.message,
      props.firstCommendId,
      commentContent.value
    )
    if (userStore.isNew) {
      userStore.setIsRefreshNewPost()
    } else {
      userStore.setIsRefreshHotPost()
    }

    ElMessage.success(res.data.msg)
    commentContent.value = '' //清空评论内容
    isFocused.value = false
    row.value = 1
  } else {
    if (commentContent.value.trim() === '') {
      ElMessage.warning('评论内容不能为空')
      return
    }
    const res = await commentFirstPostService(
      props.message,
      commentContent.value
    )
    if (userStore.isNew) {
      userStore.setIsRefreshNewPost()
    } else {
      userStore.setIsRefreshHotPost()
    }
    ElMessage.success(res.data.msg)
    commentContent.value = '' //清空评论内容
    isFocused.value = false
    row.value = 1
    emit('updateCommend', 1)
  }
}
</script>

<template>
  <div
    class="comment-box-wrapper"
    :class="{ 'is-focused': isFocused || commentContent }"
  >
    <el-input
      placeholder="留下你的足迹吧..."
      v-model="commentContent"
      class="custom-comment-input"
      type="textarea"
      resize="none"
      :rows="row"
      @focus="handleFocus"
      @blur="handleBlur"
      @keydown.enter.prevent="submitFirstComment"
    ></el-input>

    <div class="action-footer" v-show="isFocused || commentContent">
      <el-button
        type="primary"
        round
        class="submit-btn"
        @click="submitFirstComment"
        :disabled="!commentContent.trim()"
      >
        发表评论
      </el-button>
    </div>
  </div>
</template>

<style scoped>
/* 沉浸式一体化输入框容器 */
.comment-box-wrapper {
  width: 100%;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 12px;
  padding: 10px;
  border: 1px solid #ebeef5;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.02);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.comment-box-wrapper.is-focused {
  background: #ffffff;
  border-color: rgba(64, 158, 255, 0.5);
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.1);
}

/* 输入框样式穿透去边框 */
.custom-comment-input {
  width: 100%;
}
:deep(.custom-comment-input .el-textarea__inner) {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 10px;
  font-size: 15px;
  color: #303133;
  font-family: inherit;
  line-height: 1.6;
}
:deep(.custom-comment-input .el-textarea__inner:focus) {
  box-shadow: none;
}
:deep(.custom-comment-input .el-textarea__inner::-webkit-scrollbar) {
  width: 6px;
}
:deep(.custom-comment-input .el-textarea__inner::-webkit-scrollbar-thumb) {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 10px;
}

/* 底部操作区 */
.action-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  animation: fadeIn 0.3s ease;
}

.submit-btn {
  padding: 8px 20px;
  font-weight: 600;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border: none;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.submit-btn:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
