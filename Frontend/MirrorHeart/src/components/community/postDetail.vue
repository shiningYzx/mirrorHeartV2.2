<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getFirstCommentService,
  getSecondCommentService,
  likeCommentService,
  deleteCommentService,
  likePostService,
  collectPostService,
  updatePostVisibilityService,
  deletePostService
} from '@/api/userCommunity.js'
import { submitReportService } from '@/api/userCenter.js'
import commentInput from '@/components/community/commentInput.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores'

const router = useRouter()
const userStore = useUserStore()
const currentUserId = userStore.basicUser.uid
const token = userStore.basicUser.token

const isPostDetailDrawer = ref(false)

// 🌟 新增：维护详情页本地的独立状态，防止各个帖子状态互相污染
const localLikeCount = ref(0)
const localCommentCount = ref(0)
const localFavoriteCount = ref(0)
const localIsLiked = ref(false)
const localIsFavorited = ref(false)

// 打开抽屉
const openPostDetailDrawer = () => {
  isPostDetailDrawer.value = true
}

//获取评论列表
const commentList = ref([])

//评论列表页数
const commentListPage = ref(0)

//获取评论列表函数
const getFirstCommentList = async () => {
  const res = await getFirstCommentService(
    props.message.postInfo.postId,
    commentListPage.value
  )
  commentList.value = res.data.data.records || []
  showInputs.value = commentList.value.map(() => false)
  showSecondComment.value = commentList.value.map(() => false)
}

//监听message的变化,一旦message有值,就获取评论列表并重置本地状态
onMounted(() => {
  watch(
    () => props.message.postInfo,
    (newValue) => {
      if (newValue.postId) {
        getFirstCommentList()

        // 🌟 初始化当前帖子的本地状态
        localLikeCount.value =
          props.message.likeAndCommentInfo?.postLikeNum || 0
        localCommentCount.value =
          props.message.likeAndCommentInfo?.postCommentNum || 0
        localFavoriteCount.value = props.message.postInfo?.favoriteCount || 0
        localIsLiked.value = props.message.likeAndCommentInfo?.isLiked || false
        localIsFavorited.value =
          props.message.likeAndCommentInfo?.isFavorited || false
      }

      if (newValue.postPictures) {
        postPictures.value = [] //清空配图数组
        //遍历配图
        for (let i = 0; i < props.message.postInfo.postPictures.length; i++) {
          if (props.message.postInfo.postPictures[i] !== null) {
            postPictures.value.push(props.message.postInfo.postPictures[i])
          }
        }
      }
    }
  )
})

// 初始化显示状态数组
const showInputs = ref([])

// 切换评论输入框的显示状态
function toggleCommentInput(index) {
  showInputs.value[index] = !showInputs.value[index]
}

// 更新评论列表
const updateCommend = (id) => {
  if (id === 1) {
    getFirstCommentList()
    localCommentCount.value++
  }
}

//二级评论的页数
const secondCommentPage = ref(0)

//二级评论列表
const secondCommentList = ref([])

//获取二级评论列表的布尔值
const showSecondComment = ref([])

//获取二级评论列表
const getSecondCommentList = async (firstLevelCommentId) => {
  const res = await getSecondCommentService(
    secondCommentPage.value,
    firstLevelCommentId
  )
  if (!res.data.data.records || res.data.data.records.length === 0) {
    ElMessage.warning('暂无回复！')
    return
  }
  secondCommentList.value = res.data.data.records
}

//点击展开获取二级评论
const openSecondCommend = (firstLevelCommentId, index) => {
  secondCommentList.value = [] //清空二级评论列表
  for (let i = 0; i < showSecondComment.value.length; i++) {
    if (i !== index) {
      showSecondComment.value[i] = false
    }
  }
  showSecondComment.value[index] = !showSecondComment.value[index]
  if (showSecondComment.value[index]) {
    getSecondCommentList(firstLevelCommentId)
  }
}

//点赞一级评论
const likeFirstComment = async (commentId) => {
  try {
    const res = await likeCommentService(commentId)
    const isLiked = res.data.data
    const comment = commentList.value.find((item) => item.id === commentId)
    if (comment) {
      comment.isLiked = isLiked
      comment.likeCount = isLiked
        ? (comment.likeCount || 0) + 1
        : Math.max((comment.likeCount || 0) - 1, 0)
    }
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error('点赞失败，请稍后重试')
  }
}

//点赞二级评论
const likeSecondComment = async (commentId) => {
  try {
    const res = await likeCommentService(commentId)
    const isLiked = res.data.data
    const comment = secondCommentList.value.find(
      (item) => item.id === commentId
    )
    if (comment) {
      comment.isLiked = isLiked
      comment.likeCount = isLiked
        ? (comment.likeCount || 0) + 1
        : Math.max((comment.likeCount || 0) - 1, 0)
    }
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error('点赞失败，请稍后重试')
  }
}

//删除一级评论
const deleteFirstComment = async (commentId, index) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCommentService(commentId)
    ElMessage.success('删除成功')
    commentList.value.splice(index, 1)
    localCommentCount.value = Math.max(localCommentCount.value - 1, 0)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除评论失败:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

//删除二级评论
const deleteSecondComment = async (commentId, index) => {
  try {
    await ElMessageBox.confirm('确定要删除这条回复吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCommentService(commentId)
    ElMessage.success('删除成功')
    secondCommentList.value.splice(index, 1)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除回复失败:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

// 🌟 点赞帖子 (更新本地独立状态)
const likePost = async () => {
  try {
    const res = await likePostService(props.message.postInfo.postId)
    const isLiked = res.data.data
    localIsLiked.value = isLiked
    localLikeCount.value = isLiked
      ? (localLikeCount.value || 0) + 1
      : Math.max((localLikeCount.value || 0) - 1, 0)
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error('点赞失败，请稍后重试')
  }
}

// 🌟 收藏帖子 (更新本地独立状态)
const collectPost = async () => {
  if (!token) {
    ElMessage.warning('登录后才能收藏哦！')
    userStore.setIsLogin(true)
    return
  }

  if (!props.message?.postInfo?.postId) {
    return
  }

  try {
    const res = await collectPostService(props.message.postInfo.postId)
    const isCollected = res.data.data
    localIsFavorited.value = isCollected
    localFavoriteCount.value = isCollected
      ? (localFavoriteCount.value || 0) + 1
      : Math.max((localFavoriteCount.value || 0) - 1, 0)
    if (isCollected) {
      ElMessage.success('收藏成功')
    } else {
      ElMessage.info('已取消收藏')
    }
  } catch (error) {
    console.error('收藏失败:', error)
    ElMessage.error('收藏失败，请稍后重试')
  }
}

//接受父组件的属性
const props = defineProps({
  message: Object,
  fromCommunity: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['refresh'])

//遍历配图将不为null的图片放入数组
const postPictures = ref([])

// 跳转到用户主页
const navigateToUserProfile = (userId) => {
  if (userId) {
    router.push(`/user-profile/${userId}`)
  }
}

// 删除帖子
const deletePost = async () => {
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

    await deletePostService(props.message.postInfo.postId)
    ElMessage.success('帖子删除成功')
    isPostDetailDrawer.value = false

    // 刷新社区列表
    if (props.fromCommunity) {
      emit('refresh')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除帖子失败:', error)
      ElMessage.error('删除帖子失败，请稍后重试')
    }
  }
}

// 修改帖子可见度
const changePostVisibility = async () => {
  try {
    await ElMessageBox.confirm(
      `<div class="visibility-container">
        <div class="visibility-option">
          <input type="radio" id="visibility-1" name="visibility" value="1" ${(props.message.postInfo.showPost || 1) === 1 ? 'checked' : ''}>
          <label for="visibility-1">公开</label>
          <span class="visibility-desc">所有人可见</span>
        </div>
        <div class="visibility-option">
          <input type="radio" id="visibility-2" name="visibility" value="2" ${(props.message.postInfo.showPost || 1) === 2 ? 'checked' : ''}>
          <label for="visibility-2">仅粉丝可见</label>
          <span class="visibility-desc">只有关注你的用户可见</span>
        </div>
        <div class="visibility-option">
          <input type="radio" id="visibility-0" name="visibility" value="0" ${(props.message.postInfo.showPost || 1) === 0 ? 'checked' : ''}>
          <label for="visibility-0">仅自己可见</label>
          <span class="visibility-desc">只有你自己可以看到</span>
        </div>
      </div>`,
      '修改可见度',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info',
        dangerouslyUseHTMLString: true,
        customClass: 'visibility-dialog',
        beforeClose: (action, instance, done) => {
          if (action === 'confirm') {
            const selectedValue = instance.$el.querySelector(
              'input[name="visibility"]:checked'
            )?.value
            if (selectedValue) {
              instance.confirmButtonLoading = true
              setTimeout(() => {
                instance.confirmButtonLoading = false
                done()
              }, 300)
            } else {
              ElMessage.warning('请选择可见度选项')
              return false
            }
          } else {
            done()
          }
        }
      }
    )

    const showPostValue = parseInt(
      document.querySelector('input[name="visibility"]:checked')?.value || '1'
    )
    let visibilityText = ''

    switch (showPostValue) {
      case 0:
        visibilityText = '仅自己可见'
        break
      case 1:
        visibilityText = '公开'
        break
      case 2:
        visibilityText = '仅粉丝可见'
        break
    }

    await updatePostVisibilityService(
      props.message.postInfo.postId,
      showPostValue
    )
    ElMessage.success(`已设置为${visibilityText}`)
    props.message.postInfo.showPost = showPostValue
  } catch (error) {
    if (error !== 'cancel') {
      console.error('修改可见度失败:', error)
      ElMessage.error('修改可见度失败，请稍后重试')
    }
  }
}

// 举报帖子
const reportPost = async () => {
  if (!token) {
    ElMessage.warning('登录后才能举报哦！')
    userStore.setIsLogin(true)
    return
  }

  try {
    const { value: reason } = await ElMessageBox.prompt(
      '请输入举报原因',
      '举报帖子',
      {
        confirmButtonText: '提交',
        cancelButtonText: '取消',
        inputPattern: /.+/,
        inputErrorMessage: '举报原因不能为空'
      }
    )

    await submitReportService('POST', props.message.postInfo.postId, reason)
    ElMessage.success('举报成功，我们会尽快处理')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('举报失败:', error)
      ElMessage.error('举报失败，请稍后重试')
    }
  }
}

// 暴露给父组件的属性和方法
defineExpose({
  openPostDetailDrawer
})
</script>

<template>
  <el-drawer
    v-model="isPostDetailDrawer"
    :with-header="false"
    size="45%"
    class="custom-drawer"
  >
    <div class="detail-container">
      <!-- 极光氛围光斑 -->
      <div class="ambient-glow glow-1"></div>
      <div class="ambient-glow glow-2"></div>

      <!-- 核心内容区 -->
      <div class="scrollable-content">
        <!-- 帖子内容区 -->
        <article class="post-glass-card">
          <!-- 头部作者信息 -->
          <header
            class="author-header"
            :class="{ clickable: props.fromCommunity }"
            @click="
              props.fromCommunity &&
                navigateToUserProfile(props.message?.postUserInfo?.postUserId)
            "
          >
            <img
              :src="
                props.message?.postUserInfo?.postUserAvatar ||
                'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
              "
              alt="贴主头像"
              class="author-avatar"
            />
            <div class="author-meta">
              <span class="author-name">{{
                props.message?.postUserInfo?.postUserNickname || '匿名用户'
              }}</span>
              <span class="post-time">{{
                props.message?.postInfo?.postTime || ''
              }}</span>
            </div>
          </header>

          <h1 class="post-title">
            {{ props.message?.postInfo?.postTitle || '无标题' }}
          </h1>

          <div
            class="post-content"
            v-html="props.message?.postInfo?.postContent || ''"
          ></div>

          <!-- 帖子配图网格化处理 -->
          <div class="post-image-grid" v-if="postPictures.length > 0">
            <img
              v-for="(item, index) in postPictures"
              :key="index"
              :src="item"
              alt="帖子配图"
              class="grid-img"
            />
          </div>

          <!-- 互动数据与操作 (胶囊样式) -->
          <div class="interaction-bar">
            <div class="interaction-left">
              <!-- 🌟 绑定本地点赞状态 -->
              <div
                class="action-capsule"
                :class="{ 'is-liked': localIsLiked }"
                @click="likePost"
              >
                <img src="@/assets/爱心2.png" alt="点赞" class="action-icon" />
                <span>{{ localLikeCount }}</span>
              </div>

              <!-- 🌟 绑定本地收藏状态及高光样式 -->
              <div
                class="action-capsule"
                :class="{ 'is-collected': localIsFavorited }"
                @click="collectPost"
              >
                <svg
                  width="18"
                  height="18"
                  viewBox="0 0 24 24"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                    fill="currentColor"
                  />
                </svg>
                <span>{{ localFavoriteCount }}</span>
              </div>
            </div>

            <!-- 删除和修改可见度按钮（仅作者可见） -->
            <div
              v-if="props.message?.postUserInfo?.postUserId === currentUserId"
              class="post-management-buttons"
            >
              <el-button
                type="primary"
                size="small"
                round
                plain
                @click="changePostVisibility"
              >
                可见度
              </el-button>
              <el-button
                type="danger"
                size="small"
                round
                plain
                @click="deletePost"
              >
                删除
              </el-button>
            </div>

            <!-- 举报按钮（非作者可见） -->
            <el-button
              v-if="props.message?.postUserInfo?.postUserId !== currentUserId"
              type="warning"
              size="small"
              round
              plain
              @click="reportPost"
            >
              举报
            </el-button>
          </div>
        </article>

        <!-- 评论区域 -->
        <section class="comments-glass-card">
          <h3 class="section-title">
            全部评论 <span class="comment-count">{{ localCommentCount }}</span>
          </h3>

          <!-- 顶部评论输入框 -->
          <div class="commentBox-wrapper">
            <commentInput
              :message="props.message?.postInfo?.postId"
              @updateCommend="updateCommend"
            ></commentInput>
          </div>

          <!-- 评论列表 -->
          <div class="comment-list">
            <div
              class="comment-item"
              v-for="(item, index) in commentList"
              :key="item?.id || index"
            >
              <!-- 一级评论 -->
              <div class="first-level-comment">
                <img
                  :src="
                    item?.author?.avatarUrl ||
                    'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                  "
                  alt="头像"
                  class="comment-avatar clickable"
                  @click="navigateToUserProfile(item?.author?.id)"
                />
                <div class="comment-main">
                  <div class="comment-header">
                    <span
                      class="comment-user clickable"
                      @click="navigateToUserProfile(item?.author?.id)"
                    >
                      {{ item?.author?.nickname || '匿名用户' }}
                    </span>
                    <span class="comment-time">{{
                      item?.createdAt || ''
                    }}</span>
                  </div>

                  <div class="comment-text">
                    {{ item?.text || '' }}
                  </div>

                  <div class="comment-footer">
                    <div class="comment-actions">
                      <div
                        class="action-text like-btn"
                        :class="{ 'is-liked': item?.isLiked }"
                        @click="likeFirstComment(item?.id)"
                      >
                        <img
                          src="@/assets/like.png"
                          alt="点赞"
                          class="micro-icon"
                        />
                        <span>{{ item?.likeCount || 0 }}</span>
                      </div>
                      <span
                        class="action-text"
                        @click="toggleCommentInput(index)"
                        >回复</span
                      >
                      <span
                        v-if="item?.author?.id === currentUserId"
                        class="action-text delete-text"
                        @click="deleteFirstComment(item?.id, index)"
                      >
                        删除
                      </span>
                    </div>

                    <!-- 🌟 去掉严格判断，默认展现二级评论展开按钮 -->
                    <span
                      class="action-text expand-text"
                      v-if="item.childCount > 0"
                      @click="openSecondCommend(item?.id, index)"
                    >
                      {{
                        showSecondComment[index]
                          ? '收起回复'
                          : `展开 ${item.childCount} 条回复`
                      }}
                    </span>
                  </div>

                  <!-- 回复输入框 -->
                  <div class="reply-input-area" v-show="showInputs[index]">
                    <commentInput
                      :message="props.message?.postInfo?.postId"
                      :firstCommendId="item?.id"
                      @updateCommend="updateCommend"
                    ></commentInput>
                  </div>

                  <!-- 二级评论列表 -->
                  <div
                    class="second-level-list"
                    v-show="showSecondComment[index]"
                  >
                    <div
                      class="second-comment-item"
                      v-for="(subItem, subIndex) in secondCommentList"
                      :key="subItem?.id || subIndex"
                    >
                      <img
                        :src="
                          subItem?.author?.avatarUrl ||
                          'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                        "
                        alt="头像"
                        class="sub-avatar clickable"
                        @click="navigateToUserProfile(subItem?.author?.id)"
                      />
                      <div class="sub-main">
                        <div class="comment-header">
                          <span
                            class="comment-user clickable"
                            @click="navigateToUserProfile(subItem?.author?.id)"
                          >
                            {{ subItem?.author?.nickname || '匿名用户' }}
                          </span>
                          <span class="comment-time">{{
                            subItem?.createdAt || ''
                          }}</span>
                        </div>
                        <div class="comment-text">
                          <!-- 如果有回复对象，则渲染 回复 @xxx -->
                          <span
                            v-if="subItem.replyToUser"
                            style="color: #409eff; margin-right: 5px"
                          >
                            回复 @{{ subItem.replyToUser.nickname }} :
                          </span>
                          <span>{{ subItem.text }}</span>
                        </div>
                        <div class="comment-footer">
                          <div class="comment-actions">
                            <div
                              class="action-text like-btn"
                              :class="{ 'is-liked': subItem?.isLiked }"
                              @click="likeSecondComment(subItem?.id)"
                            >
                              <img
                                src="@/assets/like.png"
                                alt="点赞"
                                class="micro-icon"
                              />
                              <span>{{ subItem?.likeCount || 0 }}</span>
                            </div>
                            <span
                              v-if="subItem?.author?.id === currentUserId"
                              class="action-text delete-text"
                              @click="
                                deleteSecondComment(subItem?.id, subIndex)
                              "
                            >
                              删除
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
/* 抽屉样式穿透覆盖 */
:deep(.el-drawer) {
  background: transparent !important;
  box-shadow: none !important;
}
:deep(.el-drawer__body) {
  padding: 0;
  background: transparent;
}

/* 抽屉主容器 */
.detail-container {
  height: 100%;
  position: relative;
  background: linear-gradient(135deg, #f0f7f4 0%, #fdfbf7 100%);
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
}

/* 极光氛围光斑 */
.ambient-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  z-index: 0;
  opacity: 0.5;
  pointer-events: none;
  animation: float 12s infinite alternate ease-in-out;
}
.glow-1 {
  width: 400px;
  height: 400px;
  background: #a8edea;
  top: -50px;
  left: -50px;
}
.glow-2 {
  width: 500px;
  height: 500px;
  background: #fed6e3;
  bottom: -100px;
  right: -50px;
  animation-delay: -5s;
}

@keyframes float {
  0% {
    transform: translateY(0) scale(1);
  }
  100% {
    transform: translateY(30px) scale(1.05);
  }
}

/* 核心滚动区 */
.scrollable-content {
  position: relative;
  z-index: 1;
  height: 100%;
  overflow-y: auto;
  padding: 30px 40px;
  box-sizing: border-box;
}
.scrollable-content::-webkit-scrollbar {
  width: 6px;
}
.scrollable-content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 10px;
}

/* --- 帖子主体区域 (Glass Card) --- */
.post-glass-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-radius: 24px;
  padding: 35px 40px;
  box-shadow:
    0 10px 40px rgba(0, 0, 0, 0.05),
    inset 0 0 0 1px rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.9);
  margin-bottom: 24px;
}

.author-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}
.author-header.clickable {
  cursor: pointer;
  transition:
    transform 0.2s,
    opacity 0.2s;
}
.author-header.clickable:hover {
  opacity: 0.85;
  transform: translateX(2px);
}

.author-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  margin-right: 16px;
}
.author-meta {
  display: flex;
  flex-direction: column;
}
.author-name {
  font-size: 16px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 4px;
}
.post-time {
  font-size: 13px;
  color: #95a5a6;
}

.post-title {
  font-size: 26px;
  font-weight: 900;
  color: #1d2129;
  margin: 0 0 20px 0;
  line-height: 1.5;
  overflow-wrap: break-word;
  word-wrap: break-word;
  word-break: break-word;
}

.post-content {
  font-size: 16px;
  color: #4e5969;
  line-height: 1.8;
  margin-bottom: 25px;
  text-align: justify;
  overflow-wrap: break-word;
  word-wrap: break-word;
  word-break: break-word;
}
:deep(.post-content p) {
  margin-bottom: 1.2em;
}
:deep(.post-content img),
:deep(.post-content video) {
  max-width: 100%;
  height: auto;
  border-radius: 12px;
  margin: 10px 0;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

/* 图片网格处理 */
.post-image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
  margin-bottom: 30px;
}
.grid-img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 16px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
  transition:
    transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1),
    box-shadow 0.3s;
  cursor: zoom-in;
}
.grid-img:hover {
  transform: scale(1.03);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
}

/* 互动栏 (点赞、收藏、管理) */
.interaction-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  margin-top: 30px;
}

.interaction-left {
  display: flex;
  gap: 16px;
}

.action-capsule {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  background: #ffffff;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  color: #4e5969;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}
.action-capsule:hover {
  background: #f0f7ff;
  color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

/* 🌟 爱心高光样式 */
.action-capsule.is-liked {
  color: #f56c6c;
  background: #fef0f0;
}
.action-capsule.is-liked:hover {
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.15);
}

/* 🌟 收藏高光样式 */
.action-capsule.is-collected {
  color: #e6a23c;
  background: #fdf6ec;
}
.action-capsule.is-collected:hover {
  box-shadow: 0 4px 12px rgba(230, 162, 60, 0.15);
}

.action-icon {
  width: 18px;
  height: 18px;
}

.post-management-buttons {
  display: flex;
  gap: 12px;
}

/* --- 评论区域 (Glass Card) --- */
.comments-glass-card {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 35px 40px;
  box-shadow:
    0 8px 30px rgba(0, 0, 0, 0.04),
    inset 0 0 0 1px rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.section-title {
  font-size: 20px;
  font-weight: 800;
  color: #1d2129;
  margin: 0 0 24px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.comment-count {
  font-size: 14px;
  font-weight: 600;
  color: #86909c;
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 10px;
  border-radius: 12px;
}

.commentBox-wrapper {
  margin-bottom: 35px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 一级评论 */
.first-level-comment {
  display: flex;
  align-items: flex-start;
  background: rgba(255, 255, 255, 0.85);
  padding: 24px;
  border-radius: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 1);
}
.first-level-comment:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.05);
}

.comment-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 16px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.comment-avatar.clickable {
  cursor: pointer;
  transition:
    opacity 0.2s,
    transform 0.2s;
}
.comment-avatar.clickable:hover {
  opacity: 0.85;
  transform: scale(1.05);
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.comment-user {
  font-size: 15px;
  font-weight: 700;
  color: #4e5969;
}
.comment-user.clickable {
  cursor: pointer;
  transition: color 0.2s;
}
.comment-user.clickable:hover {
  color: #409eff;
}

.comment-time {
  font-size: 12px;
  color: #c9cdd4;
}

.comment-text {
  font-size: 15px;
  color: #1d2129;
  line-height: 1.6;
  margin-bottom: 12px;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.comment-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.action-text {
  font-size: 13px;
  font-weight: 500;
  color: #86909c;
  cursor: pointer;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}
.action-text:hover {
  color: #409eff;
}
.action-text.delete-text:hover {
  color: #f53f3f;
}

.like-btn.is-liked {
  color: #f53f3f;
}

.micro-icon {
  width: 16px;
  height: 16px;
}

.expand-text {
  font-size: 13px;
  color: #409eff;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 12px;
  background: rgba(64, 158, 255, 0.08);
  border-radius: 12px;
  transition: background 0.2s;
}
.expand-text:hover {
  background: rgba(64, 158, 255, 0.15);
}

.reply-input-area {
  margin-top: 16px;
}

/* 二级评论列表 (楼中楼) */
.second-level-list {
  margin-top: 16px;
  background: rgba(242, 243, 245, 0.6);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  border-left: 3px solid rgba(64, 158, 255, 0.4);
}

.second-comment-item {
  display: flex;
  align-items: flex-start;
}

.sub-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-right: 12px;
  flex-shrink: 0;
}
.sub-avatar.clickable {
  cursor: pointer;
  transition:
    opacity 0.2s,
    transform 0.2s;
}
.sub-avatar.clickable:hover {
  opacity: 0.85;
  transform: scale(1.05);
}

.sub-main {
  flex: 1;
  min-width: 0;
}

/* 可见度弹窗样式覆盖 */
:deep(.visibility-dialog) {
  border-radius: 16px;
  overflow: hidden;
}
:deep(.visibility-dialog .el-message-box__header) {
  background: linear-gradient(135deg, #4ecdc4 0%, #45b7d1 100%);
  padding: 20px;
}
:deep(.visibility-dialog .el-message-box__title) {
  font-size: 16px;
  font-weight: 600;
  color: white;
}
:deep(.visibility-dialog .el-message-box__content) {
  padding: 30px 20px;
}
:deep(.visibility-dialog .el-message-box__btns) {
  padding: 0 20px 20px;
}
:deep(.visibility-container) {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
:deep(.visibility-option) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border-radius: 12px;
  background: #f8f9fa;
  cursor: pointer;
  transition: all 0.2s ease;
}
:deep(.visibility-option:hover) {
  background: #e9ecef;
  transform: translateX(4px);
}
:deep(.visibility-option label) {
  font-size: 14px;
  font-weight: 600;
  color: #343a40;
  cursor: pointer;
}
:deep(.visibility-desc) {
  font-size: 12px;
  color: #6c757d;
  margin-left: 22px;
}
</style>
