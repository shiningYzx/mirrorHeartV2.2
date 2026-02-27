<script setup>
import {
  ref,
  onBeforeMount,
  watch,
  onMounted,
  onBeforeUnmount,
  onActivated,
  onDeactivated
} from 'vue'
import { useRouter } from 'vue-router'

defineOptions({
  name: 'UserCommunity'
})
import {
  getHotPostService,
  getHotPostServiceVisitor,
  getNewPostService,
  getNewPostServiceVisitor,
  getPostDetailService,
  searchPostService,
  searchPostServiceVisitor,
  getSearchHistoryService,
  clearSearchHistoryService,
  likePostService,
  searchUsersService,
  toggleFollowService
} from '@/api/userCommunity.js'
import { CirclePlus, Search, CircleClose, View } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores'
import { ElMessage } from 'element-plus'
import postDrawer from '@/components/community/postDrawer.vue'
import postDetail from '@/components/community/postDetail.vue'

const userStore = useUserStore()
const router = useRouter()

const token = userStore.basicUser.token

const searchKeyword = ref('')
const searchPage = ref(0)
const searchTotalPages = ref(0)
const isSearching = ref(false)
const searchHistory = ref([])
const showSearchHistory = ref(false)
const searchType = ref('post')
const searchResults = ref([])

const handleSearch = async (resetPage = true) => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  // 搜索功能需要登录
  if (!token) {
    ElMessage.warning('登录后才能使用搜索功能哦！')
    userStore.setIsLogin(true)
    return
  }

  showSearchHistory.value = false
  isSearching.value = true
  if (resetPage) {
    searchPage.value = 0
  }

  try {
    if (searchType.value === 'user') {
      const res = await searchUsersService(
        searchKeyword.value.trim(),
        searchPage.value
      )
      console.log('搜索用户结果:', res.data.data)
      if (
        Array.isArray(res.data.data.records) &&
        res.data.data.records.length === 0
      ) {
        ElMessage.info('未找到相关用户')
        searchResults.value = []
        searchTotalPages.value = 0
        return
      }
      searchTotalPages.value = res.data.data.pages || 0
      ElMessage.success('搜索成功')
      searchResults.value = res.data.data.records
      backTop()
    } else {
      if (!token) {
        const res = await searchPostServiceVisitor(
          searchKeyword.value.trim(),
          searchPage.value
        )
        console.log('访客搜索结果:', res.data.data)
        if (
          Array.isArray(res.data.data.records) &&
          res.data.data.records.length === 0
        ) {
          ElMessage.info('未找到相关帖子')
          newPosts.value = []
          searchTotalPages.value = 0
          return
        }
        searchTotalPages.value = res.data.data.pages || 0
        ElMessage.success('搜索成功')
        newPosts.value = res.data.data.records
        backTop()
      } else {
        const res = await searchPostService(
          searchKeyword.value.trim(),
          searchPage.value
        )
        console.log('登录用户搜索结果:', res.data.data)
        if (
          Array.isArray(res.data.data.records) &&
          res.data.data.records.length === 0
        ) {
          ElMessage.info('未找到相关帖子')
          newPosts.value = []
          searchTotalPages.value = 0
          return
        }
        searchTotalPages.value = res.data.data.pages || 0
        ElMessage.success('搜索成功')
        newPosts.value = res.data.data.records
        backTop()
      }
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败，请稍后重试')
  }
}

const handleSearchNext = () => {
  if (searchPage.value < searchTotalPages.value - 1) {
    searchPage.value++
    handleSearch(false)
  }
}

const handleSearchPrev = () => {
  if (searchPage.value > 0) {
    searchPage.value--
    handleSearch(false)
  }
}

const clearSearch = () => {
  searchKeyword.value = ''
  isSearching.value = false
  searchPage.value = 0
  searchTotalPages.value = 0
}

const getSearchHistory = async () => {
  if (!token) {
    searchHistory.value = []
    return
  }
  try {
    const res = await getSearchHistoryService()
    searchHistory.value = res.data.data || []
  } catch (error) {
    console.error('获取搜索历史失败:', error)
    searchHistory.value = []
  }
}

const clearSearchHistory = async () => {
  if (!token) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await clearSearchHistoryService()
    searchHistory.value = []
    ElMessage.success('清空搜索历史成功')
  } catch (error) {
    console.error('清空搜索历史失败:', error)
    ElMessage.error('清空搜索历史失败，请稍后重试')
  }
}

const handleHistorySearch = (keyword) => {
  searchKeyword.value = keyword
  handleSearch()
}

const handleLogin = () => {
  userStore.setIsLogin(true)
}

const handleSearchTypeChange = (type) => {
  searchType.value = type
  if (searchKeyword.value) {
    handleSearch(true)
  }
}

const handleToggleFollow = async (userId, isFollowed) => {
  if (!token) {
    ElMessage.warning('登录后才能关注用户哦！')
    userStore.setIsLogin(true)
    return
  }

  try {
    const res = await toggleFollowService(userId)
    const isNowFollowed = res.data.data
    const user = searchResults.value.find((u) => u.id === userId)
    if (user) {
      user.isFollowed = isNowFollowed
    }
    ElMessage.success(isNowFollowed ? '关注成功' : '已取消关注')
  } catch (error) {
    console.error('关注失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

const handleViewUserProfile = (userId) => {
  console.log('查看用户主页:', userId)
  // 跳转到用户主页页面
  router.push(`/user-profile/${userId}`)
}

const handleClickOutside = (event) => {
  const searchBox = event.target.closest('.search-box')
  if (!searchBox) {
    showSearchHistory.value = false
  }
}

const handleSearchBoxClick = () => {
  showSearchHistory.value = true
  getSearchHistory()
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onActivated(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

onDeactivated(() => {
  document.removeEventListener('click', handleClickOutside)
})

const backTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

let newPostPage = ref(0)
let newPostTotalPages = ref(0)

const getNewPost = async (page) => {
  try {
    if (!token) {
      const res = await getNewPostServiceVisitor(page)
      console.log('访客最新帖子数据:', res.data.data)
      if (
        Array.isArray(res.data.data.records) &&
        res.data.data.records.length === 0
      ) {
        newPostPage.value--
        ElMessage.error('暂无帖子')
        return
      }
      newPostTotalPages.value = res.data.data.pages || 0
      ElMessage.success('获取最新帖子成功')
      newPosts.value = res.data.data.records
      backTop()
    } else {
      const res = await getNewPostService(page)
      console.log('登录用户最新帖子数据:', res.data.data)
      if (
        Array.isArray(res.data.data.records) &&
        res.data.data.records.length === 0
      ) {
        --newPostPage.value
        ElMessage.error('暂无帖子,请稍后再试试哦！')
        return
      }
      newPostTotalPages.value = res.data.data.pages || 0
      ElMessage.success('获取最新帖子成功')
      newPosts.value = res.data.data.records
      backTop()
    }
  } catch (error) {
    console.error('获取最新帖子失败:', error)
    ElMessage.error('获取帖子失败，请稍后重试')
  }
}

const handleNextNewPost = () => {
  if (newPostPage.value < newPostTotalPages.value - 1) {
    getNewPost(newPostPage.value + 1)
  }
}

const handlePrevNewPost = () => {
  if (newPostPage.value > 0) {
    getNewPost(newPostPage.value - 1)
  }
}

let hotPostPage = ref(0)
let hotPostTotalPages = ref(0)

const getHotPost = async (page) => {
  try {
    if (!token) {
      const res = await getHotPostServiceVisitor(page)
      console.log('访客最热帖子数据:', res.data.data)
      if (
        Array.isArray(res.data.data.records) &&
        res.data.data.records.length === 0
      ) {
        hotPostPage.value--
        ElMessage.error('暂无帖子')
        return
      }
      hotPostTotalPages.value = res.data.data.pages || 0
      ElMessage.success('获取最热帖子成功')
      newPosts.value = res.data.data.records
      backTop()
    } else {
      const res = await getHotPostService(page)
      console.log('登录用户最热帖子数据:', res.data.data)
      if (
        Array.isArray(res.data.data.records) &&
        res.data.data.records.length === 0
      ) {
        hotPostPage.value--
        ElMessage.error('暂无帖子')
        return
      }
      hotPostTotalPages.value = res.data.data.pages || 0
      ElMessage.success('获取最热帖子成功')
      newPosts.value = res.data.data.records
      backTop()
    }
  } catch (error) {
    console.error('获取最热帖子失败:', error)
    ElMessage.error('获取帖子失败，请稍后重试')
  }
}

const handleNextHotPost = () => {
  if (hotPostPage.value < hotPostTotalPages.value - 1) {
    getHotPost(hotPostPage.value + 1)
  }
}

const handlePrevHotPost = () => {
  if (hotPostPage.value > 0) {
    getHotPost(hotPostPage.value - 1)
  }
}

const newPosts = ref(null)

onBeforeMount(() => {
  getNewPost(newPostPage.value)
})

watch(
  () => userStore.isNew,
  (newVal) => {
    if (newVal === true) {
      getNewPost(newPostPage.value)
    } else {
      getHotPost(hotPostPage.value)
    }
  }
)

const getPostDrawer = ref(null)

const message = ref('')

const handleSend = (number) => {
  if (!token) {
    ElMessage.warning('登录后才可以发贴喔！')
    userStore.setIsLogin(true)
    return
  }
  getPostDrawer.value.openPostDrawer()
  message.value = number
}

const getPostDetail = ref(null)

const postDetailData = ref({})

const getPostDetailData = async (id) => {
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
  if (!token) {
    ElMessage.warning('登录后才能查看帖子详情哦！')
    userStore.setIsLogin(true)
    return
  }
  getPostDetailData(id)
}

const handleLikePost = async (postId, event) => {
  event.stopPropagation()
  try {
    const res = await likePostService(postId)
    const isLiked = res.data.data
    const post = newPosts.value.find((item) => item.id === postId)
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

const refreshNewPost = async () => {
  const res = await getNewPostService(newPostPage.value)
  newPostTotalPages.value = res.data.data.pages || 0
  newPosts.value = res.data.data.records
}

const refreshHotPost = async () => {
  const res = await getHotPostService(hotPostPage.value)
  hotPostTotalPages.value = res.data.data.pages || 0
  newPosts.value = res.data.data.records
}

watch(
  () => userStore.isRefreshNewPost,
  (newVal) => {
    if (newVal === true) {
      refreshNewPost()
      userStore.outIsRefreshNewPost()
    }
  }
)

watch(
  () => userStore.isRefreshHotPost,
  (newVal) => {
    if (newVal === true) {
      refreshHotPost()
      userStore.outIsRefreshHotPost()
    }
  }
)

// 监听路由变化，处理 postId 查询参数
watch(
  () => router.currentRoute.value,
  (newRoute) => {
    if (newRoute.query.postId) {
      if (!token) {
        ElMessage.warning('登录后才能查看帖子详情哦！')
        userStore.setIsLogin(true)
        return
      }
      getPostDetailData(newRoute.query.postId)
    }
  },
  { immediate: true }
)

// 处理刷新社区列表
const handleRefresh = () => {
  if (userStore.isNew) {
    refreshNewPost()
  } else {
    refreshHotPost()
  }
}
</script>

<template>
  <div class="communityContainer">
    <!-- ✅ 保留的极光氛围背景 (绝对不可修改) -->
    <div class="ambient-glow glow-1"></div>
    <div class="ambient-glow glow-2"></div>
    <div class="ambient-glow glow-3"></div>

    <el-backtop :right="50" :bottom="100" />

    <div class="communitySocialBox">
      <!-- ✅ 深度优化的顶部导航栏：三栏平衡布局 -->
      <header class="headerNavBar">
        <!-- 左侧栏：占位 1 份空间 -->
        <div class="nav-left">
          <div class="tab-switcher">
            <div
              class="tab-item"
              :class="{ active: userStore.isNew && !isSearching }"
              @click="clearSearch(), userStore.setIsNew()"
            >
              最新
            </div>
            <div
              class="tab-item"
              :class="{ active: !userStore.isNew && !isSearching }"
              @click="clearSearch(), userStore.outIsNew()"
            >
              最热
            </div>
            <!-- 仅在搜索时优雅展现的第三个 Tab -->
            <div v-if="isSearching" class="tab-item active">搜索结果</div>
          </div>
        </div>

        <!-- 中间栏：占位 2 份空间，绝对居中 -->
        <div class="nav-center">
          <div class="search-box" @click.stop="handleSearchBoxClick">
            <div
              class="search-capsule"
              :class="{ 'is-focused': showSearchHistory }"
            >
              <el-icon class="search-prefix"><Search /></el-icon>
              <input
                v-model="searchKeyword"
                class="search-input"
                :placeholder="
                  searchType === 'user'
                    ? '探索有趣的用户...'
                    : '搜索社区的帖子...'
                "
                @keyup.enter="handleSearch"
              />
              <el-icon
                v-if="searchKeyword"
                class="clear-icon"
                @click.stop="searchKeyword = ''"
              >
                <CircleClose />
              </el-icon>
              <button class="mini-search-btn" @click.stop="handleSearch">
                搜索
              </button>
            </div>

            <!-- 收纳好的搜索高级下拉面板 -->
            <div v-if="showSearchHistory" class="search-dropdown">
              <div class="search-type-tabs">
                <span
                  :class="{ active: searchType === 'post' }"
                  @click.stop="handleSearchTypeChange('post')"
                >
                  找帖子
                </span>
                <span
                  :class="{ active: searchType === 'user' }"
                  @click.stop="handleSearchTypeChange('user')"
                >
                  找用户
                </span>
              </div>

              <div v-if="searchHistory.length > 0" class="history-container">
                <div class="history-header">
                  <span class="history-title">搜索历史</span>
                  <span
                    class="clear-history-text"
                    @click.stop="clearSearchHistory"
                    >清空</span
                  >
                </div>
                <div class="history-list">
                  <div
                    v-for="(item, index) in searchHistory"
                    :key="index"
                    class="history-item"
                    @click="handleHistorySearch(item)"
                  >
                    <el-icon class="history-icon"><Search /></el-icon>
                    <span>{{ item }}</span>
                  </div>
                </div>
              </div>

              <div v-else-if="!token" class="history-empty-container">
                <div class="history-empty-content">
                  <el-icon class="history-empty-icon"><Search /></el-icon>
                  <p class="history-empty-text">登录后查看搜索历史</p>
                  <button class="history-login-btn" @click.stop="handleLogin">
                    立即登录
                  </button>
                </div>
              </div>

              <div v-else class="history-empty-container">
                <div class="history-empty-content">
                  <el-icon class="history-empty-icon"><Search /></el-icon>
                  <p class="history-empty-text">暂无搜索历史</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧栏：占位 1 份空间，靠右对齐 -->
        <div class="nav-right">
          <button class="publish-btn" @click="handleSend(1)">
            <el-icon class="publish-icon"><CirclePlus /></el-icon>
            <span>发布图文</span>
          </button>
        </div>
      </header>

      <!-- 内容广场 -->
      <main class="spaceSquare">
        <!-- 空状态 -->
        <div v-if="!newPosts || newPosts.length === 0" class="empty-state">
          <el-empty description="暂无内容，快来抢沙发吧~" />
        </div>

        <!-- 用户搜索结果列表 -->
        <div
          v-if="
            isSearching && searchType === 'user' && searchResults.length > 0
          "
          class="user-search-results"
        >
          <div v-for="user in searchResults" :key="user.id" class="user-card">
            <div
              class="user-card-header"
              @click="handleViewUserProfile(user.id)"
            >
              <el-avatar
                :size="50"
                :src="
                  user.avatarUrl ||
                  'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                "
              />
              <div class="user-info">
                <div class="user-nickname">
                  {{ user.nickname || '未知用户' }}
                </div>
                <div class="user-bio">
                  {{ user.bio || '这个人很懒，什么都没写~' }}
                </div>
              </div>
            </div>
            <div class="user-card-footer">
              <el-button
                v-if="!user.isFollowed && user.id !== userStore.basicUser.uid"
                type="primary"
                size="small"
                round
                @click.stop="handleToggleFollow(user.id, user.isFollowed)"
              >
                + 关注
              </el-button>
              <el-button
                v-if="user.isFollowed && user.id !== userStore.basicUser.uid"
                type="info"
                size="small"
                round
                plain
                @click.stop="handleToggleFollow(user.id, user.isFollowed)"
              >
                已关注
              </el-button>
              <span
                v-if="user.id === userStore.basicUser.uid"
                class="is-me-badge"
                >自己</span
              >
            </div>
          </div>
        </div>

        <!-- 帖子流卡片（仅在非用户搜索时显示） -->
        <div
          v-if="!isSearching || searchType !== 'user'"
          class="posts-container"
        >
          <article
            class="post-card"
            v-for="(item, index) in newPosts"
            :key="index"
            @click="handlePostId(item.id)"
          >
            <div class="post-header">
              <img
                :src="
                  item.author?.avatarUrl ||
                  'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                "
                class="post-avatar"
                alt="avatar"
              />
              <div class="post-user-info">
                <span class="user-name">{{
                  item.author?.nickname || '社区居民'
                }}</span>
                <span class="post-time">{{ item.createdAt || '' }}</span>
              </div>
            </div>

            <div class="post-body">
              <h3 class="post-title">{{ item.title || '' }}</h3>
              <div
                class="post-cover-wrapper"
                v-if="item.imageUrls && item.imageUrls.length > 0"
              >
                <img
                  class="post-cover-img"
                  :src="item.imageUrls[0]"
                  alt="cover"
                />
              </div>
              <p class="post-content" v-if="item.text">{{ item.text }}</p>
            </div>

            <div class="post-footer">
              <div class="action-btn view-btn" @click.stop>
                <el-icon class="action-icon"><View /></el-icon>
                <span>{{ item.viewCount || 0 }}</span>
              </div>
              <div class="action-btn" @click.stop="handlePostId(item.id)">
                <img
                  src="@/assets/评论.png"
                  class="action-icon"
                  alt="comment"
                />
                <span>{{ item.commentCount || 0 }}</span>
              </div>
              <div
                class="action-btn"
                :class="{ 'is-liked': item.isLiked }"
                @click.stop="handleLikePost(item.id, $event)"
              >
                <img src="@/assets/爱心2.png" class="action-icon" alt="like" />
                <span>{{ item.likeCount || 0 }}</span>
              </div>
            </div>
          </article>
        </div>
      </main>

      <!-- 底部翻页 -->
      <footer class="pagingBox" v-if="newPosts && newPosts.length > 0">
        <div class="paging" v-if="isSearching">
          <el-button
            round
            :disabled="searchPage === 0"
            @click="handleSearchPrev"
            >上一页</el-button
          >
          <span class="page-indicator"
            >{{ searchPage + 1 }} / {{ searchTotalPages || 1 }}</span
          >
          <el-button
            round
            type="primary"
            :disabled="searchPage >= searchTotalPages - 1"
            @click="handleSearchNext"
            >下一页</el-button
          >
        </div>
        <div class="paging" v-else-if="userStore.isNew">
          <el-button
            round
            :disabled="newPostPage === 0"
            @click="handlePrevNewPost"
            >上一页</el-button
          >
          <span class="page-indicator"
            >{{ newPostPage + 1 }} / {{ newPostTotalPages || 1 }}</span
          >
          <el-button
            round
            type="primary"
            :disabled="newPostPage >= newPostTotalPages - 1"
            @click="handleNextNewPost"
            >下一页</el-button
          >
        </div>
        <div class="paging" v-else>
          <el-button
            round
            :disabled="hotPostPage === 0"
            @click="handlePrevHotPost"
            >上一页</el-button
          >
          <span class="page-indicator"
            >{{ hotPostPage + 1 }} / {{ hotPostTotalPages || 1 }}</span
          >
          <el-button
            round
            type="primary"
            :disabled="hotPostPage >= hotPostTotalPages - 1"
            @click="handleNextHotPost"
            >下一页</el-button
          >
        </div>
      </footer>
    </div>
  </div>

  <postDetail
    ref="getPostDetail"
    :message="postDetailData"
    :fromCommunity="true"
    @refresh="handleRefresh"
  ></postDetail>
  <postDrawer
    ref="getPostDrawer"
    :message="message"
    @refresh="handleRefresh"
  ></postDrawer>
</template>

<style scoped>
/* ================= 全局背景 (不可修改) ================= */
.communityContainer {
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 30px 20px;
  background: linear-gradient(135deg, #e0f7fa 0%, #f9fbe7 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
  overflow: hidden;
  position: relative;
  z-index: 1;
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
  top: -100px;
  left: 5%;
}
.glow-2 {
  width: 700px;
  height: 700px;
  background: #fed6e3;
  bottom: -100px;
  right: -5%;
  animation-delay: -5s;
}
.glow-3 {
  width: 500px;
  height: 500px;
  background: #e0c3fc;
  top: 40%;
  left: 30%;
  animation-delay: -9s;
}

@keyframes float {
  0% {
    transform: translateY(0) scale(1);
  }
  100% {
    transform: translateY(40px) scale(1.1);
  }
} */

/* ================= 社区主体容器 ================= */
.communitySocialBox {
  width: 100%;
  max-width: 960px; /* 微调：扩大一点宽度，让顶部三栏呼吸感更好 */
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  box-shadow:
    0 10px 40px rgba(0, 0, 0, 0.08),
    inset 0 0 0 1px rgba(255, 255, 255, 0.5);
  box-sizing: border-box;
  background-color: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  overflow: hidden;
}

/* ================= 🌟 深度重构的顶部导航栏：三栏平衡布局 ================= */
.headerNavBar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  min-height: 84px;
  background: rgba(255, 255, 255, 0.45);
  border-bottom: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
  z-index: 10;
  gap: 20px;
}

/* 左侧：Tabs */
.nav-left {
  flex: 1;
  display: flex;
  justify-content: flex-start;
  align-items: center;
}

/* 1. iOS风 Tab切换器 */
.tab-switcher {
  display: inline-flex;
  background-color: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 5px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.03);
}

.tab-item {
  padding: 8px 24px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  white-space: nowrap;
  user-select: none;
}

.tab-item:hover {
  color: #409eff;
  background: rgba(255, 255, 255, 0.4);
}

.tab-item.active {
  background: #ffffff;
  color: #409eff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 中间：绝对居中的搜索区域 */
.nav-center {
  flex: 2;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.search-box {
  width: 100%;
  max-width: 460px; /* 控制搜索框的最大伸展 */
  position: relative;
}

.search-capsule {
  display: flex;
  align-items: center;
  width: 100%;
  height: 48px; /* 加高搜索框显得更修长 */
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow:
    inset 0 2px 6px rgba(0, 0, 0, 0.03),
    0 2px 10px rgba(0, 0, 0, 0.02);
  transition: all 0.3s ease;
  padding: 0 6px 0 20px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-sizing: border-box;
}

.search-capsule:hover,
.search-capsule.is-focused {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.15);
  border-color: rgba(64, 158, 255, 0.3);
}

.search-prefix {
  color: #909399;
  font-size: 18px;
  margin-right: 8px;
}

.search-input {
  flex: 1;
  height: 100%;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #303133;
  outline: none;
}

.search-input::placeholder {
  color: #a8abb2;
}

.clear-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin-right: 6px;
  color: #c0c4cc;
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s;
}

.clear-icon:hover {
  color: #909399;
  background-color: rgba(0, 0, 0, 0.05);
}

.mini-search-btn {
  height: 36px;
  padding: 0 20px;
  border: none;
  background: linear-gradient(135deg, #7f96f9 0%, #c78fff 100%);
  color: white;
  font-size: 14px;
  font-weight: 600;
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.mini-search-btn:hover {
  box-shadow: 0 4px 12px rgba(127, 150, 249, 0.4);
  transform: scale(1.02);
}

/* 下拉搜索面板 */
.search-dropdown {
  position: absolute;
  top: 60px; /* 紧贴搜索框下方 */
  left: 0;
  width: 100%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.9);
  z-index: 100;
  padding: 16px;
  box-sizing: border-box;
  animation: slideDown 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.search-type-tabs {
  display: flex;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 16px;
}

.search-type-tabs span {
  flex: 1;
  text-align: center;
  padding: 6px 0;
  font-size: 13px;
  font-weight: 600;
  color: #909399;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.search-type-tabs span.active {
  background: #ffffff;
  color: #409eff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  margin-bottom: 8px;
}

.history-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.clear-history-text {
  font-size: 12px;
  color: #909399;
  cursor: pointer;
  transition: color 0.2s;
}

.clear-history-text:hover {
  color: #f56c6c;
}

.history-list {
  max-height: 220px;
  overflow-y: auto;
}

.history-list::-webkit-scrollbar {
  width: 4px;
}
.history-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.history-item:hover {
  background: rgba(64, 158, 255, 0.08);
}

.history-icon {
  font-size: 14px;
  color: #909399;
}

.history-item span {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 搜索历史空状态 */
.history-empty-container {
  padding: 30px 20px;
  text-align: center;
}

.history-empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.history-empty-icon {
  font-size: 40px;
  color: #d0d7de;
  margin-bottom: 4px;
}

.history-empty-text {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.history-login-btn {
  margin-top: 8px;
  padding: 8px 24px;
  border: none;
  background: linear-gradient(135deg, #7f96f9 0%, #c78fff 100%);
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(127, 150, 249, 0.25);
}

.history-login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(127, 150, 249, 0.35);
}

/* 右侧：操作区 */
.nav-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.publish-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 48px;
  padding: 0 28px;
  border: none;
  background: linear-gradient(135deg, #7f96f9 0%, #c78fff 100%);
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 4px 15px rgba(127, 150, 249, 0.3);
  white-space: nowrap;
}

.publish-btn:hover {
  background: linear-gradient(135deg, #c78fff 0%, #7f96f9 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(127, 150, 249, 0.4);
}

.publish-btn:active {
  transform: translateY(0);
}

.publish-icon {
  font-size: 18px;
  transition: transform 0.3s ease;
}

.publish-btn:hover .publish-icon {
  transform: rotate(90deg);
}

/* ================= 以下为卡片及列表样式 ================= */

.spaceSquare {
  flex: 1;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 30px 0; /* 增加上下内边距 */
  gap: 24px; /* 使用 gap 代替子元素的 margin-bottom */
  scroll-behavior: smooth;
}

.spaceSquare::-webkit-scrollbar {
  width: 6px;
}
.spaceSquare::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 10px;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

/* 帖子容器 */
.posts-container {
  width: 90%;
  max-width: 720px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 帖子卡片 */
.post-card {
  width: 90%;
  max-width: 720px; /* 适当放宽卡片以配合更宽的主容器 */
  background-color: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border-radius: 20px;
  padding: 24px;
  box-sizing: border-box;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  border: 1px solid rgba(255, 255, 255, 1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.04);
  /* 移除了 margin-bottom，统一使用父级 spaceSquare 的 gap */
}

.post-card:hover {
  transform: translateY(-5px);
  background-color: rgba(255, 255, 255, 0.98);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.08);
  border-color: rgba(255, 255, 255, 1);
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.post-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-right: 14px;
}

.post-user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-time {
  font-size: 12px;
  color: #909399;
}

.post-body {
  margin-bottom: 16px;
}

.post-title {
  font-size: 18px;
  font-weight: 800;
  color: #303133;
  margin: 0 0 14px 0;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.post-content {
  font-size: 15px;
  color: #555;
  line-height: 1.7;
  margin: 0;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: justify;
}

.post-cover-wrapper {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  background-color: #f8f9fa;
  margin-bottom: 15px;
}

.post-cover-img {
  width: 100%;
  max-height: 380px;
  object-fit: cover;
  display: block;
  transition: transform 0.4s ease;
}

.post-card:hover .post-cover-img {
  transform: scale(1.02);
}

.post-footer {
  display: flex;
  align-items: center;
  gap: 30px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 20px;
  transition: background 0.2s;
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

.action-btn .el-icon {
  width: 18px;
  height: 18px;
  opacity: 0.7;
}

.action-btn:hover .action-icon {
  opacity: 1;
}

/* 分页容器 */
.pagingBox {
  width: 100%;
  padding: 20px 0;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.6);
}

.paging {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 24px;
}

.page-indicator {
  font-size: 14px;
  color: #606266;
  font-weight: 600;
  letter-spacing: 1px;
}

/* 用户搜索卡片 */
.user-search-results {
  width: 90%;
  max-width: 720px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(15px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 1);
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
}

.user-card:hover {
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.user-card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  cursor: pointer;
  transition: all 0.3s ease;
}

.user-card-header:hover {
  opacity: 0.8;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-nickname {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-bio {
  font-size: 13px;
  color: #909399;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-card-footer {
  flex-shrink: 0;
}

.is-me-badge {
  display: inline-block;
  padding: 4px 12px;
  background: linear-gradient(135deg, #7f96f9 0%, #c78fff 100%);
  color: white;
  font-size: 12px;
  font-weight: 600;
  border-radius: 12px;
  white-space: nowrap;
}
</style>
