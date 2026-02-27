<script setup>
import { ref, onBeforeMount, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  getUserProfileService,
  getUserPostsService,
  likePostService,
  getPostDetailService,
  toggleFollowService,
  getUserFollowingService,
  getUserFollowersService,
  toggleBlockService
} from '@/api/userCommunity.js'
import { submitReportService } from '@/api/userCenter.js'
import { useUserStore } from '@/stores'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import postDetail from './postDetail.vue'

const props = defineProps({
  userId: {
    type: Number,
    default: null
  }
})

const userStore = useUserStore()
const token = userStore.basicUser.token
const router = useRouter()

const userProfile = ref({})
const userPosts = ref([])
const userFollowing = ref([])
const userFollowers = ref([])
const isLoading = ref(true)
const activeTab = ref('posts') // posts, followers, following
const postsPage = ref(0)
const followingPage = ref(0)
const followersPage = ref(0)
const postsTotalPages = ref(0)
const followingTotalPages = ref(0)
const followersTotalPages = ref(0)

const getPostDetail = ref(null)
const postDetailData = ref({})

const getUserProfileData = async (userId) => {
  if (!userId) {
    ElMessage.error('用户ID不能为空')
    return
  }

  try {
    isLoading.value = true
    const res = await getUserProfileService(userId)
    console.log('用户主页数据:', res.data.data)
    userProfile.value = res.data.data
  } catch (error) {
    console.error('获取用户主页失败:', error)
    ElMessage.error('获取用户主页失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

const getUserPostsData = async (userId, page) => {
  if (!userId) {
    ElMessage.error('用户ID不能为空')
    return
  }

  try {
    isLoading.value = true
    const res = await getUserPostsService(userId, page)
    console.log('用户帖子数据:', res.data.data)
    userPosts.value = res.data.data.records || []
    postsTotalPages.value = res.data.data.pages || 0
  } catch (error) {
    console.error('获取用户帖子失败:', error)
    ElMessage.error('获取用户帖子失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

const handlePostsNext = () => {
  if (postsPage.value < postsTotalPages.value - 1) {
    postsPage.value++
    getUserPostsData(props.userId, postsPage.value)
  }
}

const handlePostsPrev = () => {
  if (postsPage.value > 0) {
    postsPage.value--
    getUserPostsData(props.userId, postsPage.value)
  }
}

const getUserFollowingData = async (userId, page) => {
  if (!userId) {
    ElMessage.error('用户ID不能为空')
    return
  }

  try {
    isLoading.value = true
    const res = await getUserFollowingService(userId, page, 10)
    console.log('关注列表数据:', res.data.data)
    userFollowing.value = res.data.data.records || []
    followingTotalPages.value = res.data.data.pages || 0
  } catch (error) {
    console.error('获取关注列表失败:', error)
    ElMessage.error('获取关注列表失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

const getUserFollowersData = async (userId, page) => {
  if (!userId) {
    ElMessage.error('用户ID不能为空')
    return
  }

  try {
    isLoading.value = true
    const res = await getUserFollowersService(userId, page, 10)
    console.log('粉丝列表数据:', res.data.data)
    userFollowers.value = res.data.data.records || []
    followersTotalPages.value = res.data.data.pages || 0
  } catch (error) {
    console.error('获取粉丝列表失败:', error)
    ElMessage.error('获取粉丝列表失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

const handleFollowingNext = () => {
  if (followingPage.value < followingTotalPages.value - 1) {
    followingPage.value++
    getUserFollowingData(props.userId, followingPage.value)
  }
}

const handleFollowingPrev = () => {
  if (followingPage.value > 0) {
    followingPage.value--
    getUserFollowingData(props.userId, followingPage.value)
  }
}

const handleFollowersNext = () => {
  if (followersPage.value < followersTotalPages.value - 1) {
    followersPage.value++
    getUserFollowersData(props.userId, followersPage.value)
  }
}

const handleFollowersPrev = () => {
  if (followersPage.value > 0) {
    followersPage.value--
    getUserFollowersData(props.userId, followersPage.value)
  }
}

const getPostDetailData = async (id) => {
  if (!id) {
    ElMessage.error('帖子ID不能为空')
    return
  }

  try {
    const res = await getPostDetailService(id)
    console.log('帖子详情数据:', res.data.data)

    postDetailData.value = {
      postInfo: {
        postId: res.data.data.id,
        postTitle: res.data.data.title,
        postContent: res.data.data.text,
        postTime: res.data.data.createdAt,
        postPictures: res.data.data.imageUrls || [],
        favoriteCount: res.data.data.favoriteCount || 0
      },
      postUserInfo: {
        postUserAvatar: res.data.data.author?.avatar || '',
        postUserNickname: res.data.data.author?.nickname || '未知用户',
        postUserId: res.data.data.author?.id
      },
      likeAndCommentInfo: {
        postLikeNum: res.data.data.likeCount || 0,
        postCommentNum: res.data.data.commentCount || 0,
        isLiked: res.data.data.isLiked || false,
        isFavorited: res.data.data.isFavorited || false
      }
    }

    getPostDetail.value.openPostDetailDrawer()
  } catch (error) {
    console.error('获取帖子详情失败:', error)
    ElMessage.error('获取帖子详情失败，请稍后重试')
  }
}

const handlePostId = (id) => {
  getPostDetailData(id)
}

const handleBack = () => {
  router.back()
}

const handleFollow = async () => {
  if (!token) {
    ElMessage.warning('登录后才能关注哦！')
    userStore.setIsLogin(true)
    return
  }

  if (!props.userId) {
    ElMessage.error('用户ID不能为空')
    return
  }

  try {
    const res = await toggleFollowService(props.userId)
    const isFollowed = res.data.data
    if (isFollowed) {
      ElMessage.success('关注成功')
    } else {
      ElMessage.info('已取消关注')
    }
    // 更新关注状态
    if (userProfile.value) {
      userProfile.value.isFollowed = isFollowed
    }
  } catch (error) {
    console.error('关注失败:', error)
    ElMessage.error('关注失败，请稍后重试')
  }
}

const handleLikePost = async (postId, event) => {
  if (!token) {
    ElMessage.warning('登录后才能点赞哦！')
    userStore.setIsLogin(true)
    return
  }

  event.stopPropagation()
  try {
    const res = await likePostService(postId)
    const isLiked = res.data.data
    const post = userPosts.value.find((item) => item.id === postId)
    if (post) {
      post.isLiked = isLiked
      post.likeCount = isLiked
        ? (post.likeCount || 0) + 1
        : Math.max((post.likeCount || 0) - 1, 0)
    }
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error('点赞失败，请稍后重试')
  }
}

const handleReportUser = async () => {
  if (!token) {
    ElMessage.warning('登录后才能举报哦！')
    userStore.setIsLogin(true)
    return
  }

  if (!props.userId) {
    ElMessage.error('用户ID不能为空')
    return
  }

  try {
    const { value: reason } = await ElMessageBox.prompt(
      '请输入举报原因',
      '举报用户',
      {
        confirmButtonText: '提交',
        cancelButtonText: '取消',
        inputPattern: /.+/,
        inputErrorMessage: '举报原因不能为空'
      }
    )

    await submitReportService('USER', props.userId, reason)
    ElMessage.success('举报成功，我们会尽快处理')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('举报失败:', error)
      ElMessage.error('举报失败，请稍后重试')
    }
  }
}

const handleBlockUser = async () => {
  if (!token) {
    ElMessage.warning('登录后才能拉黑哦！')
    userStore.setIsLogin(true)
    return
  }

  if (!props.userId) {
    ElMessage.error('用户ID不能为空')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要拉黑该用户吗？拉黑后将无法看到该用户的帖子。',
      '拉黑用户',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await toggleBlockService(props.userId)
    const isBlocked = res.data.data

    if (isBlocked) {
      ElMessage.success('拉黑成功')
    } else {
      ElMessage.info('已取消拉黑')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('拉黑失败:', error)
      ElMessage.error('拉黑失败，请稍后重试')
    }
  }
}

const handleFollowUser = async (userId) => {
  if (!token) {
    ElMessage.warning('登录后才能关注哦！')
    userStore.setIsLogin(true)
    return
  }

  if (!userId) {
    ElMessage.error('用户ID不能为空')
    return
  }

  try {
    const res = await toggleFollowService(userId)
    const isFollowed = res.data.data
    if (isFollowed) {
      ElMessage.success('关注成功')
    } else {
      ElMessage.info('已取消关注')
    }

    // 更新列表中的关注状态
    if (activeTab.value === 'followers') {
      const user = userFollowers.value.find((item) => item.id === userId)
      if (user) {
        user.isFollowed = isFollowed
      }
    } else if (activeTab.value === 'following') {
      const user = userFollowing.value.find((item) => item.id === userId)
      if (user) {
        user.isFollowed = isFollowed
      }
    }
  } catch (error) {
    console.error('关注失败:', error)
    ElMessage.error('关注失败，请稍后重试')
  }
}

watch(activeTab, (newTab) => {
  if (newTab === 'posts' && props.userId) {
    postsPage.value = 0
    getUserPostsData(props.userId, postsPage.value)
  } else if (newTab === 'following' && props.userId) {
    followingPage.value = 0
    getUserFollowingData(props.userId, followingPage.value)
  } else if (newTab === 'followers' && props.userId) {
    followersPage.value = 0
    getUserFollowersData(props.userId, followersPage.value)
  }
})

onBeforeMount(() => {
  if (props.userId) {
    getUserProfileData(props.userId)
    getUserPostsData(props.userId, postsPage.value)
  }
})
</script>

<template>
  <div class="user-profile-container">
    <el-backtop :right="50" :bottom="100" />

    <!-- 极光氛围背景 -->
    <div class="ambient-glow glow-1"></div>
    <div class="ambient-glow glow-2"></div>
    <div class="ambient-glow glow-3"></div>

    <!-- 左上角返回按钮 -->
    <div class="back-button-container">
      <el-button class="glass-back-btn" @click="handleBack" circle>
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
    </div>

    <!-- 页面核心包装区 -->
    <div class="profile-main-wrapper">
      <!-- 左侧：个人卡片 -->
      <aside class="profile-sidebar">
        <!-- 头部：玻璃态个人卡片 -->
        <header class="profile-glass-card">
          <!-- 封面图 -->
          <div class="cover-image">
            <div class="cover-gradient"></div>
            <div class="cover-pattern"></div>
          </div>

          <!-- 资料区域 -->
          <div class="profile-info-section">
            <!-- 悬浮头像 -->
            <div class="avatar-container">
              <el-avatar
                :size="120"
                class="profile-avatar"
                :src="
                  userProfile.avatarUrl ||
                  'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                "
              />
            </div>

            <!-- 昵称与签名 -->
            <div class="user-basic-info">
              <h1 class="user-nickname">
                {{ userProfile.nickname || '未知用户' }}
              </h1>
              <p class="user-bio">
                {{ userProfile.bio || '这个人很懒，什么都没写~' }}
              </p>

              <!-- 统计数据 -->
              <div class="stats-container">
                <div class="stat-item">
                  <span class="stat-value">{{
                    userProfile.postCount || 0
                  }}</span>
                  <span class="stat-label">帖子</span>
                </div>
                <div class="divider"></div>
                <div class="stat-item">
                  <span class="stat-value">{{
                    userProfile.followerCount || 0
                  }}</span>
                  <span class="stat-label">粉丝</span>
                </div>
                <div class="divider"></div>
                <div class="stat-item">
                  <span class="stat-value">{{
                    userProfile.followingCount || 0
                  }}</span>
                  <span class="stat-label">关注</span>
                </div>
              </div>

              <!-- 关注按钮 -->
              <div
                v-if="userStore.basicUser.uid !== userProfile.id"
                class="follow-button-wrapper"
              >
                <el-button
                  class="follow-button"
                  :type="userProfile.isFollowed ? 'default' : 'primary'"
                  @click="handleFollow"
                >
                  {{ userProfile.isFollowed ? '已关注' : '关注' }}
                </el-button>
                <el-button
                  class="report-button"
                  type="warning"
                  plain
                  round
                  @click="handleReportUser"
                >
                  举报用户
                </el-button>
                <el-button
                  class="block-button"
                  type="danger"
                  plain
                  round
                  @click="handleBlockUser"
                >
                  拉黑用户
                </el-button>
              </div>
            </div>
          </div>
        </header>
      </aside>

      <!-- 右侧：内容展示区域 -->
      <main class="profile-content-wrapper">
        <!-- 标签页导航：胶囊风 -->
        <nav class="profile-tabs-wrapper">
          <div class="tab-switcher">
            <div
              class="tab-item"
              :class="{ active: activeTab === 'posts' }"
              @click="activeTab = 'posts'"
            >
              全部帖子
            </div>
            <div
              class="tab-item"
              :class="{ active: activeTab === 'followers' }"
              @click="activeTab = 'followers'"
            >
              粉丝 ({{ userProfile.followerCount || 0 }})
            </div>
            <div
              class="tab-item"
              :class="{ active: activeTab === 'following' }"
              @click="activeTab = 'following'"
            >
              关注 ({{ userProfile.followingCount || 0 }})
            </div>
          </div>
        </nav>

        <!-- 内容展示区域 -->
        <div class="profile-content-area">
          <!-- 加载状态 -->
          <div v-if="isLoading" class="glass-panel loading-state">
            <el-skeleton :rows="5" animated />
          </div>

          <!-- 帖子列表 -->
          <div v-else-if="activeTab === 'posts'" class="posts-list">
            <div
              v-if="!userPosts || userPosts.length === 0"
              class="glass-panel empty-state"
            >
              <el-empty description="暂无帖子" />
            </div>
            <div v-else>
              <div class="posts-grid">
                <article
                  v-for="post in userPosts"
                  :key="post.id"
                  class="post-card"
                  @click="handlePostId(post.id)"
                >
                  <div class="post-header">
                    <img
                      :src="
                        post.author?.avatarUrl ||
                        'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                      "
                      class="post-avatar"
                      alt="avatar"
                    />
                    <div class="post-user-info">
                      <span class="user-name">{{
                        post.author?.nickname || '社区居民'
                      }}</span>
                      <span class="post-time">{{ post.createdAt || '' }}</span>
                    </div>
                  </div>

                  <div class="post-body">
                    <h3 class="post-title">{{ post.title || '' }}</h3>
                    <div
                      class="post-cover-wrapper"
                      v-if="post.imageUrls && post.imageUrls.length > 0"
                    >
                      <img
                        class="post-cover-img"
                        :src="post.imageUrls[0]"
                        alt="cover"
                      />
                    </div>
                    <p class="post-content" v-if="post.text">{{ post.text }}</p>
                  </div>

                  <div class="post-footer">
                    <div class="action-btn view-btn" @click.stop>
                      <svg
                        width="18"
                        height="18"
                        viewBox="0 0 24 24"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                      >
                        <path
                          d="M12 4.5C7 4.5 2.75 7.25 2.75 12c0 3.75 2.25 6.5 6.5 7.5h1.5c1.75 0 3.25-.75 4.25-1.75l1.75-3.5c.5-1 .5-2.25-.5-3.25s-1.25-1.25-2.25-.75l-3.5-1.75C9.75 2.75 7 4.5 7 9v1.5c0 3.75 2.75 6.5 6.5 7.5l3.5 1.75c1 .5 1.75 1.25 2.25.75s.5 2.25-.5 3.25l-1.75 3.5C16.25 20.5 15.25 21.5 13.5 21.5H12c-3.75 0-6.5-2.75-6.5-6.5V9c0-3.75 2.75-6.5 6.5-7.5z"
                          fill="currentColor"
                        />
                      </svg>
                      <span>{{ post.viewCount || 0 }}</span>
                    </div>
                    <div class="action-btn" @click.stop="handlePostId(post.id)">
                      <img
                        src="@/assets/评论.png"
                        class="action-icon"
                        alt="comment"
                      />
                      <span>{{ post.commentCount || 0 }}</span>
                    </div>
                    <div
                      class="action-btn"
                      :class="{ 'is-liked': post.isLiked }"
                      @click.stop="handleLikePost(post.id, $event)"
                    >
                      <img
                        src="@/assets/爱心2.png"
                        class="action-icon"
                        alt="like"
                      />
                      <span>{{ post.likeCount || 0 }}</span>
                    </div>
                  </div>
                </article>
              </div>

              <!-- 分页 -->
              <div class="pagination-wrapper">
                <el-button
                  round
                  :disabled="postsPage === 0"
                  @click="handlePostsPrev"
                  >上一页</el-button
                >
                <span class="page-indicator"
                  >{{ postsPage + 1 }} / {{ postsTotalPages || 1 }}</span
                >
                <el-button
                  round
                  type="primary"
                  :disabled="postsPage >= postsTotalPages - 1"
                  @click="handlePostsNext"
                >
                  下一页
                </el-button>
              </div>
            </div>
          </div>

          <!-- 粉丝列表 -->
          <div v-else-if="activeTab === 'followers'" class="followers-list">
            <div v-if="isLoading" class="glass-panel loading-state">
              <el-skeleton :rows="5" animated />
            </div>

            <div
              v-else-if="userFollowers && userFollowers.length > 0"
              class="glass-panel"
            >
              <div class="users-grid">
                <div
                  v-for="user in userFollowers"
                  :key="user.id"
                  class="user-card"
                  @click="navigateToUserProfile(user.id)"
                >
                  <div class="user-avatar">
                    <el-avatar
                      :size="60"
                      :src="
                        user.avatarUrl ||
                        'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                      "
                    />
                  </div>
                  <div class="user-info">
                    <h4 class="user-name">{{ user.nickname || '未知用户' }}</h4>
                    <p class="user-bio">
                      {{ user.bio || '这个人很懒，什么都没写~' }}
                    </p>
                    <div class="user-stats">
                      <span class="stat">{{ user.postCount || 0 }} 帖子</span>
                      <span class="stat"
                        >{{ user.followerCount || 0 }} 粉丝</span
                      >
                    </div>
                  </div>
                  <div class="user-actions">
                    <el-button
                      :type="user.isFollowed ? 'default' : 'primary'"
                      size="small"
                      round
                      @click.stop="handleFollowUser(user.id)"
                    >
                      {{ user.isFollowed ? '已关注' : '关注' }}
                    </el-button>
                  </div>
                </div>
              </div>

              <!-- 分页 -->
              <div class="pagination-wrapper">
                <el-button
                  round
                  :disabled="followersPage === 0"
                  @click="handleFollowersPrev"
                  >上一页</el-button
                >
                <span class="page-indicator"
                  >{{ followersPage + 1 }} /
                  {{ followersTotalPages || 1 }}</span
                >
                <el-button
                  round
                  type="primary"
                  :disabled="followersPage >= followersTotalPages - 1"
                  @click="handleFollowersNext"
                >
                  下一页
                </el-button>
              </div>
            </div>

            <div v-else class="glass-panel empty-state">
              <el-empty description="暂无粉丝" />
            </div>
          </div>

          <!-- 关注列表 -->
          <div v-else-if="activeTab === 'following'" class="following-list">
            <div v-if="isLoading" class="glass-panel loading-state">
              <el-skeleton :rows="5" animated />
            </div>

            <div
              v-else-if="userFollowing && userFollowing.length > 0"
              class="glass-panel"
            >
              <div class="users-grid">
                <div
                  v-for="user in userFollowing"
                  :key="user.id"
                  class="user-card"
                  @click="navigateToUserProfile(user.id)"
                >
                  <div class="user-avatar">
                    <el-avatar
                      :size="60"
                      :src="
                        user.avatarUrl ||
                        'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                      "
                    />
                  </div>
                  <div class="user-info">
                    <h4 class="user-name">{{ user.nickname || '未知用户' }}</h4>
                    <p class="user-bio">
                      {{ user.bio || '这个人很懒，什么都没写~' }}
                    </p>
                    <div class="user-stats">
                      <span class="stat">{{ user.postCount || 0 }} 帖子</span>
                      <span class="stat"
                        >{{ user.followerCount || 0 }} 粉丝</span
                      >
                    </div>
                  </div>
                  <div class="user-actions">
                    <el-button
                      :type="user.isFollowed ? 'default' : 'primary'"
                      size="small"
                      round
                      @click.stop="handleFollowUser(user.id)"
                    >
                      {{ user.isFollowed ? '已关注' : '关注' }}
                    </el-button>
                  </div>
                </div>
              </div>

              <!-- 分页 -->
              <div class="pagination-wrapper">
                <el-button
                  round
                  :disabled="followingPage === 0"
                  @click="handleFollowingPrev"
                  >上一页</el-button
                >
                <span class="page-indicator"
                  >{{ followingPage + 1 }} /
                  {{ followingTotalPages || 1 }}</span
                >
                <el-button
                  round
                  type="primary"
                  :disabled="followingPage >= followingTotalPages - 1"
                  @click="handleFollowingNext"
                >
                  下一页
                </el-button>
              </div>
            </div>

            <div v-else class="glass-panel empty-state">
              <el-empty description="暂无关注" />
            </div>
          </div>
        </div>
      </main>
    </div>

    <postDetail ref="getPostDetail" :message="postDetailData"></postDetail>
  </div>
</template>

<style scoped>
/* ================= 全局背景与极光效果 ================= */
.user-profile-container {
  height: 100%;
  background: linear-gradient(135deg, #e0f7fa 0%, #f9fbe7 100%);
  padding: 0;
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
  overflow: hidden;
  z-index: 1;
  display: flex;
  flex-direction: column;
}

/* .ambient-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  z-index: -1;
  opacity: 0.6;
  pointer-events: none;
  animation: float 14s infinite alternate ease-in-out;
}
.glow-1 {
  width: 600px;
  height: 600px;
  background: #a8edea;
  top: -10%;
  left: 0%;
}
.glow-2 {
  width: 700px;
  height: 700px;
  background: #fed6e3;
  top: 20%;
  right: -10%;
  animation-delay: -5s;
}
.glow-3 {
  width: 500px;
  height: 500px;
  background: #e0c3fc;
  bottom: -10%;
  left: 20%;
  animation-delay: -9s;
} */

@keyframes float {
  0% {
    transform: translateY(0) scale(1);
  }
  100% {
    transform: translateY(40px) scale(1.1);
  }
}

/* ================= 返回按钮 ================= */
.back-button-container {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 100;
}

.glass-back-btn {
  width: 40px;
  height: 40px;
  font-size: 18px;
  color: #606266;
  border: none;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.glass-back-btn:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.15);
  color: #409eff;
}

/* ================= 核心内容包装 ================= */
.profile-main-wrapper {
  flex: 1;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  padding: 80px 30px 30px;
  position: relative;
  z-index: 10;
  display: flex;
  gap: 30px;
  overflow: hidden;
}

/* ================= 左侧个人卡片 ================= */
.profile-sidebar {
  flex-shrink: 0;
  width: 400px;
  display: flex;
  flex-direction: column;
}

/* ================= 玻璃态个人主页头部 ================= */
.profile-glass-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow:
    0 10px 40px rgba(0, 0, 0, 0.06),
    inset 0 0 0 1px rgba(255, 255, 255, 0.5);
  overflow: hidden;
  position: sticky;
  top: 0;
}

.cover-image {
  height: 240px;
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  overflow: hidden;
}

.cover-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(
      circle at 20% 30%,
      rgba(255, 255, 255, 0.3) 0%,
      transparent 50%
    ),
    radial-gradient(
      circle at 80% 70%,
      rgba(255, 255, 255, 0.2) 0%,
      transparent 50%
    );
  animation: shimmer 8s ease-in-out infinite;
}

.cover-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: radial-gradient(
      circle at 25% 25%,
      rgba(255, 255, 255, 0.1) 2px,
      transparent 2px
    ),
    radial-gradient(
      circle at 75% 75%,
      rgba(255, 255, 255, 0.1) 2px,
      transparent 2px
    );
  background-size: 40px 40px;
  animation: floatPattern 10s linear infinite;
}

@keyframes shimmer {
  0%,
  100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

@keyframes floatPattern {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 40px 40px;
  }
}

.profile-info-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 40px 40px;
  margin-top: -60px; /* 让头像上浮，盖住封面 */
  position: relative;
  z-index: 2;
}

.avatar-container {
  border: 5px solid rgba(255, 255, 255, 0.85);
  border-radius: 50%;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  background: #fff;
  transition: transform 0.3s ease;
}
.avatar-container:hover {
  transform: scale(1.05);
}
.profile-avatar {
  display: block;
}

.user-basic-info {
  text-align: center;
  margin-top: 16px;
  width: 100%;
}

.user-nickname {
  font-size: 26px;
  font-weight: 800;
  color: #2c3e50;
  margin: 0 0 8px 0;
}

.user-bio {
  font-size: 15px;
  color: #7f8c8d;
  line-height: 1.6;
  max-width: 600px;
  margin: 0 auto 24px;
}

.stats-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 35px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.5);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
  color: #2c3e50;
}

.stat-label {
  font-size: 13px;
  font-weight: 600;
  color: #909399;
}

.divider {
  width: 1px;
  height: 30px;
  background: rgba(0, 0, 0, 0.08);
}

/* ================= 关注按钮 ================= */
.follow-button-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
}

.follow-button {
  width: 140px;
  height: 40px;
  border-radius: 20px;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.follow-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.report-button {
  width: 140px;
  height: 40px;
  border-radius: 20px;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.report-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(230, 162, 60, 0.3);
}

/* ================= 右侧内容区域 ================= */
.profile-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
  overflow: hidden;
}

/* ================= 胶囊风 Tab 切换器 ================= */
.profile-tabs-wrapper {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(15px);
  border-radius: 18px;
  padding: 6px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.02);
}

.tab-switcher {
  display: flex;
  gap: 4px;
}

.tab-item {
  padding: 10px 32px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  white-space: nowrap;
}

.tab-item:hover {
  background: rgba(255, 255, 255, 0.4);
  color: #409eff;
}

.tab-item.active {
  background: #ffffff;
  color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

/* ================= 底部内容面板 ================= */
.profile-content-area {
  flex: 1;
  width: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 8px;
}

.profile-content-area::-webkit-scrollbar {
  width: 6px;
}

.profile-content-area::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

.profile-content-area::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.profile-content-area::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}

.glass-panel {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.04);
  padding: 40px;
}

.loading-state,
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 250px;
}

.posts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.post-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  min-height: 250px;
}

.post-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.9);
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 16px 12px;
}

.post-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.post-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
}

.post-time {
  font-size: 12px;
  color: #909399;
}

.post-body {
  padding: 0 16px 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.post-title {
  font-size: 16px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 12px 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.post-cover-wrapper {
  margin-bottom: 12px;
  border-radius: 12px;
  overflow: hidden;
}

.post-cover-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

.post-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0;
  flex: 1;
  min-height: 0;
}

.post-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  margin-top: auto;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  transition: background 0.2s;
  color: #606266;
  font-size: 13px;
}

.action-btn:hover {
  background-color: #f0f2f5;
  color: #409eff;
}

.action-btn.is-liked {
  color: #f56c6c;
}

.action-btn.is-liked:hover {
  background-color: #fef0f0;
  color: #f56c6c;
}

.action-icon {
  width: 18px;
  height: 18px;
  opacity: 0.7;
}

.action-btn:hover .action-icon {
  opacity: 1;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 24px;
  padding: 24px 0;
}

.page-indicator {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  letter-spacing: 1px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .user-profile-container {
    height: 100vh;
    width: 100vw;
  }

  .profile-main-wrapper {
    flex-direction: column;
    padding: 70px 15px 15px;
    gap: 20px;
  }

  .profile-sidebar {
    width: 100%;
    flex-shrink: 0;
  }

  .profile-glass-card {
    position: relative;
    top: 0;
  }

  .back-button-container {
    top: 15px;
    left: 15px;
  }

  .glass-back-btn {
    width: 36px;
    height: 36px;
    font-size: 16px;
  }

  .cover-image {
    height: 180px;
  }

  .profile-info-section {
    padding: 0 20px 30px;
  }

  .tab-item {
    padding: 8px 20px;
    font-size: 14px;
  }

  .profile-content-wrapper {
    overflow: hidden;
  }

  .profile-content-area {
    padding-right: 4px;
  }

  .posts-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .post-card {
    min-height: 250px;
  }

  .post-cover-img {
    height: 200px;
  }

  .post-title {
    -webkit-line-clamp: 2;
  }

  .post-content {
    -webkit-line-clamp: 2;
  }

  .pagination-wrapper {
    padding: 16px 0;
    gap: 16px;
  }
}

/* ================= 粉丝和关注列表 ================= */
.users-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
  cursor: pointer;
}

.user-card:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.user-avatar {
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 16px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 4px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-bio {
  font-size: 13px;
  color: #7f8c8d;
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.user-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #95a5a6;
}

.stat {
  display: flex;
  align-items: center;
}

.user-actions {
  flex-shrink: 0;
}

/* ================= 加载状态 ================= */
.loading-state {
  padding: 40px;
  display: flex;
  justify-content: center;
}

/* ================= 空状态 ================= */
.empty-state {
  padding: 60px 20px;
  text-align: center;
}
</style>
