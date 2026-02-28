import request from '@/utils/request'

export const getNewPostService = (page) => {
  return request.get('/post/list/latest', {
    params: {
      pageNo: page,
      pageSize: 10
    }
  })
}

export const getHotPostService = (page) => {
  return request.get('/post/list/hot', {
    params: {
      pageNo: page,
      pageSize: 10
    }
  })
}

export const getNewPostServiceVisitor = (page) => {
  return request.get('/post/list/latest', {
    params: {
      pageNo: page,
      pageSize: 10
    }
  })
}

export const getHotPostServiceVisitor = (page) => {
  return request.get('/post/list/hot', {
    params: {
      pageNo: page,
      pageSize: 10
    }
  })
}


export const submitPostService = (data) => {
  return request.post('/post/publish', data, {
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

export const getPostDetailService = (postId) => {
  return request.get(`/post/${postId}`)
}

export const commentFirstPostService = (postId, content) => {
  return request.post('/comment/publish', {
    postId,
    text: content
  })
}

export const getFirstCommentService = (postId, page) => {
  return request.get('/comment/root', {
    params: {
      postId,
      pageNo: page,
      pageSize: 10
    }
  })
}

export const commentSecondPostService = (
  postId,
  replyFirstLevelCommentId,
  content
) => {
  return request.post('/comment/publish', {
    postId,
    text: content,
    rootId: replyFirstLevelCommentId,
    parentId: replyFirstLevelCommentId
  })
}

export const getSecondCommentService = (page, firstLevelCommentId) => {
  return request.get('/comment/child', {
    params: {
      rootId: firstLevelCommentId,
      pageNo: page,
      pageSize: 10
    }
  })
}

export const likePostService = (postId) => {
  return request.post('/interaction/like', {
    targetType: 'POST',
    targetId: postId
  })
}

export const likeCommentService = (commentId) => {
  return request.post('/interaction/like', {
    targetType: 'COMMENT',
    targetId: commentId
  })
}

export const searchPostService = (keyword, page) => {
  return request.get('/search/posts', {
    params: {
      keyword,
      pageNo: page,
      pageSize: 10
    }
  })
}

export const searchPostServiceVisitor = (keyword, page) => {
  return request.get('/post/search', {
    params: {
      keyword,
      pageNo: page,
      pageSize: 10
    }
  })
}

export const getSearchHistoryService = () => {
  return request.get('/search/history')
}

export const clearSearchHistoryService = () => {
  return request.delete('/search/history')
}

export const deleteCommentService = (commentId) => {
  return request.delete(`/comment/${commentId}`)
}

export const searchUsersService = (keyword, page) => {
  return request.get('/search/users', {
    params: {
      keyword,
      pageNo: page,
      pageSize: 10
    }
  })
}

export const toggleFollowService = (targetUserId) => {
  return request.post('/interaction/follow', { targetUserId })
}

export const getUserFollowingService = (userId, pageNo, pageSize) => {
  return request.get(`/interaction/user/${userId}/following`, {
    params: { pageNo, pageSize }
  })
}

export const getUserFollowersService = (userId, pageNo, pageSize) => {
  return request.get(`/interaction/user/${userId}/followers`, {
    params: { pageNo, pageSize }
  })
}

export const getMyFollowedListService = (pageNo, pageSize) => {
  return request.get('/interaction/follow/my-list', {
    params: { pageNo, pageSize }
  })
}

export const getMyBlockedListService = (pageNo, pageSize) => {
  return request.get('/interaction/block/my-list', {
    params: { pageNo, pageSize }
  })
}

export const toggleBlockService = (targetUserId) => {
  return request.post('/interaction/block', {
    targetUserId
  })
}

export const getUserProfileService = (userId) => {
  return request.get(`/user/${userId}`)
}

// 文件上传
export const uploadFileService = (file, scene) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/system/file/upload', formData, {
    params: { scene },
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const getFollowingListService = (userId, page) => {
  return request.get(`/interaction/user/${userId}/following`, {
    params: {
      pageNo: page,
      pageSize: 10
    }
  })
}

export const getFollowersListService = (userId, page) => {
  return request.get(`/interaction/user/${userId}/followers`, {
    params: {
      pageNo: page,
      pageSize: 10
    }
  })
}

export const getUserPostsService = (userId, page) => {
  return request.get(`/post/user/${userId}`, {
    params: {
      pageNo: page,
      pageSize: 10
    }
  })
}

export const collectPostService = (postId) => {
  return request.post('/interaction/favorite', {
    postId: postId
  })
}

export const updatePostVisibilityService = (postId, visibility) => {
  return request.put(`/post/${postId}/visibility`, null, {
    params: { visibility }
  })
}

export const deletePostService = (postId) => {
  return request.delete(`/post/${postId}`)
}
