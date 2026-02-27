# MirrorHeart 2.0 接口文档（Apifox 导入版）

> 扫描范围：`Backend/mirrorHeart2_0/src/main/java/org/shiningyang/mirrorheart2_0/module/**/controller`。
> 统一响应体：除特别说明外，接口均返回 `ApiResponse<T>`。

## 通用说明

### 统一响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

字段说明：

| 字段    | 类型   | 说明                  |
| ------- | ------ | --------------------- |
| code    | int    | 状态码（`ErrorCode`） |
| message | string | 提示信息              |
| data    | object | 业务数据              |

### 常见请求头

| Header        | 说明                             |
| ------------- | -------------------------------- |
| Authorization | Bearer Token（部分接口需要）     |
| X-User-Id     | 用户 ID（部分社区/关系接口需要） |

---

## 管理端（Admin）

### 管理员登录

- **POST** `/admin/auth/login`
- **描述**：管理员登录
- **Body**：`AdminLoginReq`

```json
{
  "username": "admin",
  "password": "123456"
}
```

- **Response**：`ApiResponse<AdminLoginResp>`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "token"
  }
}
```

### 管理端角色创建

- **POST** `/admin/roles`
- **权限**：`ADMIN`
- **Body**：`AdminRoleCreateReq`

```json
{
  "code": "ROLE_ADMIN",
  "name": "管理员",
  "status": 1
}
```

- **Response**：`ApiResponse<String>`（返回角色 ID）

### 管理端角色授权

- **POST** `/admin/roles/grant-permissions`
- **权限**：`ADMIN`
- **Body**：`GrantRolePermReq`

```json
{
  "roleId": "role-1",
  "permIds": ["perm-1", "perm-2"]
}
```

- **Response**：`ApiResponse<Void>`

### 管理端权限列表

- **GET** `/admin/permissions`
- **权限**：`ADMIN`
- **Response**：`ApiResponse<?>`（暂未实现，返回 `"TODO"`）

### 管理端用户列表

- **GET** `/admin/users`
- **权限**：`ADMIN`
- **Response**：`ApiResponse<?>`（暂未实现，返回 `"TODO"`）

### 管理端审计日志

- **GET** `/admin/audit-logs`
- **权限**：`ADMIN`
- **Response**：`ApiResponse<?>`（暂未实现，返回 `"TODO"`）

### 管理端每日问题分页

- **GET** `/admin/daily-question/page`
- **Query**：`AdminDailyQuestionQueryReq`

| 参数     | 类型   | 必填 | 说明            |
| -------- | ------ | ---- | --------------- |
| status   | int    | 否   | 1 上架 / 2 下架 |
| category | string | 否   | 分类            |
| keyword  | string | 否   | 关键词          |
| page     | int    | 否   | 默认 1          |
| size     | int    | 否   | 默认 20         |

- **Response**：`PageResp<DailyQuestionEntity>`

```json
{
  "total": 100,
  "pages": 5,
  "current": 1,
  "size": 20,
  "records": [
    {
      "id": "q-1",
      "title": "问题",
      "category": "成长",
      "status": 1,
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00",
      "isDeleted": 0,
      "deletedAt": null
    }
  ]
}
```

### 管理端每日问题新增/更新

- **POST** `/admin/daily-question/upsert`
- **Body**：`AdminDailyQuestionUpsertReq`

```json
{
  "id": "q-1",
  "title": "问题",
  "category": "成长",
  "status": 1
}
```

- **Response**：`DailyQuestionEntity`

### 管理端每日问题上下架

- **POST** `/admin/daily-question/status/{id}`
- **Path**：`id` 题目 ID
- **Query**：`status`（1 上架 / 2 下架）
- **Response**：`boolean`

### 管理端帖子列表

- **POST** `/admin/posts/list`
- **Query**：`page`（默认 1）、`size`（默认 10）
- **Body**：`PostQueryReq`

```json
{
  "userId": "u-1",
  "type": 0
}
```

- **Response**：`ApiResponse<Page<PostResp>>`

### 管理端删除帖子

- **DELETE** `/admin/posts/{id}`
- **Response**：`ApiResponse<Void>`

---

## 用户端（Auth）

### 用户注册

- **POST** `/api/auth/register`
- **Body**：`RegisterReq`

```json
{
  "email": "user@example.com",
  "password": "123456",
  "otp": "1234",
  "nickname": "Tom"
}
```

- **Response**：`ApiResponse<Void>`

### 用户登录

- **POST** `/api/auth/login`
- **Body**：`LoginReq`

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

- **Response**：`ApiResponse<LoginResp>`

### 发送验证码

- **POST** `/api/auth/send-otp`
- **Body**：`SendEmailOtpReq`

```json
{
  "email": "user@example.com",
  "scene": "register"
}
```

- **Response**：`ApiResponse<Void>`

### 重置密码

- **POST** `/api/auth/reset-password`
- **Body**：`ResetPasswordReq`

```json
{
  "email": "user@example.com",
  "otp": "1234",
  "newPassword": "newPass"
}
```

- **Response**：`ApiResponse<Void>`

### OTP 登录

- **POST** `/api/auth/login/otp`
- **Body**：`LoginWithOtpReq`

```json
{
  "email": "user@example.com",
  "otp": "1234"
}
```

- **Response**：`ApiResponse<LoginResp>`

### 修改密码

- **POST** `/api/auth/password`
- **Body**：`UpdatePasswordReq`
- **Auth**：`AuthenticationPrincipal` 注入 userId

```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

- **Response**：`ApiResponse<Void>`

### 刷新 Token

- **POST** `/api/auth/refresh`
- **Body**：`RefreshTokenReq`

```json
{
  "refreshToken": "refresh-token"
}
```

- **Response**：`ApiResponse<LoginResp>`

### 获取用户资料

- **GET** `/api/auth/profile`
- **Query**：`userId`
- **Response**：`ApiResponse<UserProfileResp>`

---

## 管理员 Auth（API）

### 管理员登录

- **POST** `/api/admin/auth/login`
- **Body**：`LoginReq`
- **Response**：`ApiResponse<LoginResp>`

### 管理员刷新 Token

- **POST** `/api/admin/auth/refresh`
- **Header**：`Authorization`
- **Response**：`ApiResponse<LoginResp>`（目前返回空对象）

### 管理员登出

- **POST** `/api/admin/auth/logout`
- **Header**：`Authorization`
- **Response**：`ApiResponse<Void>`

---

## 社区（Community）

### 发布帖子

- **POST** `/api/posts/create`
- **Header**：`X-User-Id`
- **Body**：`PostCreateReq`

```json
{
  "content": "hello",
  "type": 0,
  "visibility": 1
}
```

- **Response**：`ApiResponse<PostResp>`

### 删除帖子

- **DELETE** `/api/posts/{id}`
- **Header**：`X-User-Id`
- **Response**：`ApiResponse<Void>`

### 帖子流

- **POST** `/api/posts/feed`
- **Query**：`page`（默认 1）、`size`（默认 10）
- **Header**：`X-User-Id`（可选）
- **Body**：`PostQueryReq`

- **Response**：`ApiResponse<Page<PostResp>>`

### 帖子详情

- **GET** `/api/posts/{id}`
- **Header**：`X-User-Id`（可选）
- **Response**：`ApiResponse<PostResp>`

### 点赞帖子/评论

- **POST** `/api/posts/like`
- **Header**：`X-User-Id`
- **Body**：`LikeReq`

```json
{
  "targetId": 1,
  "targetType": 0,
  "action": true
}
```

- **Response**：`ApiResponse<Void>`

### 收藏帖子

- **POST** `/api/posts/favorite`
- **Header**：`X-User-Id`
- **Body**：`FavoriteReq`

```json
{
  "postId": 1,
  "action": true
}
```

- **Response**：`ApiResponse<Void>`

### 发布评论

- **POST** `/api/comments/create`
- **Header**：`X-User-Id`
- **Body**：`CommentCreateReq`

```json
{
  "postId": 1,
  "replyToCommentId": 2,
  "content": "nice"
}
```

- **Response**：`ApiResponse<CommentResp>`

### 删除评论

- **DELETE** `/api/comments/{id}`
- **Header**：`X-User-Id`
- **Response**：`ApiResponse<Void>`

### 查询帖子评论

- **GET** `/api/comments/posts/{postId}`
- **Header**：`X-User-Id`（可选）
- **Response**：`ApiResponse<List<CommentResp>>`

### 评论点赞

- **POST** `/api/comments/like`
- **Header**：`X-User-Id`
- **Body**：`LikeReq`
- **Response**：`ApiResponse<Void>`

---

## 每日问题（Question）

### 获取当天问题

- **GET** `/api/daily-question/today`
- **Query**：`date`（可选，格式 `YYYY-MM-DD`）
- **Auth**：`AuthenticationPrincipal` 注入 `UserPrincipal`
- **Response**：`DailyQuestionResp`

### 提交当天回答

- **POST** `/api/daily-question/answer`
- **Query**：`date`（可选，格式 `YYYY-MM-DD`）
- **Auth**：`AuthenticationPrincipal` 注入 `UserPrincipal`
- **Body**：`DailyQuestionAnswerReq`

```json
{
  "pickId": "pick-1",
  "audioUrl": "https://...",
  "durationMs": 12345,
  "visibility": 1
}
```

- **Response**：`DailyQuestionAnswerResp`

### 获取当天回答

- **GET** `/api/daily-question/my-answer`
- **Query**：`date`（可选，格式 `YYYY-MM-DD`）
- **Auth**：`AuthenticationPrincipal` 注入 `UserPrincipal`
- **Response**：`DailyQuestionAnswerResp`

---

## 推荐（Recommend）

### 内容详情

- **GET** `/api/recommend/content/{contentId}`
- **Query**：`includeLyric`（可选，0/1）
- **Response**：`RecommendContentResp`

### 每日推荐

- **GET** `/api/recommend/daily`
- **Query**：`date`（可选，格式 `YYYY-MM-DD`）、`includeLyric`（可选，0/1）
- **Auth**：`AuthenticationPrincipal` 注入 `UserPrincipal`
- **Response**：`DailyRecommendationResp`

---

## 关系（Relation）

### 关注/取消关注

- **POST** `/api/relation/follow`
- **Header**：`X-User-Id`
- **Body**：`FollowReq`

```json
{
  "targetId": "u-2",
  "follow": true
}
```

- **Response**：`ApiResponse<Void>`

### 拉黑/取消拉黑

- **POST** `/api/relation/block`
- **Header**：`X-User-Id`
- **Query**：`targetId`、`block`
- **Response**：`ApiResponse<Void>`

### 关系状态

- **GET** `/api/relation/state`
- **Header**：`X-User-Id`
- **Query**：`targetId`
- **Response**：`ApiResponse<FollowStateResp>`

---

## 数据结构速查

### AdminRoleCreateReq

| 字段   | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| code   | string | 是   | 角色编码 |
| name   | string | 是   | 角色名称 |
| status | int    | 是   | 状态     |

### GrantRolePermReq

| 字段    | 类型     | 必填 | 说明         |
| ------- | -------- | ---- | ------------ |
| roleId  | string   | 是   | 角色 ID      |
| permIds | string[] | 否   | 权限 ID 列表 |

### LoginResp

| 字段         | 类型   | 说明       |
| ------------ | ------ | ---------- |
| accessToken  | string | 访问令牌   |
| refreshToken | string | 刷新令牌   |
| tokenType    | string | Bearer     |
| expiresAt    | long   | 过期时间戳 |

### UserProfileResp

| 字段          | 类型    | 说明     |
| ------------- | ------- | -------- |
| id            | string  | 用户 ID  |
| email         | string  | 邮箱     |
| emailVerified | boolean | 是否验证 |
| nickname      | string  | 昵称     |
| avatarUrl     | string  | 头像     |
| bio           | string  | 简介     |
| status        | int     | 状态     |

### PostResp

| 字段         | 类型     | 说明             |
| ------------ | -------- | ---------------- |
| id           | long     | 帖子 ID          |
| userId       | string   | 用户 ID          |
| userNickname | string   | 昵称             |
| userAvatar   | string   | 头像             |
| content      | string   | 内容             |
| type         | int      | 类型             |
| visibility   | int      | 可见性           |
| viewCount    | int      | 浏览数           |
| likeCount    | int      | 点赞数           |
| commentCount | int      | 评论数           |
| shareCount   | int      | 分享数           |
| isLiked      | boolean  | 当前用户点赞状态 |
| isFavorited  | boolean  | 当前用户收藏状态 |
| createdAt    | datetime | 创建时间         |

### CommentResp

| 字段                | 类型     | 说明             |
| ------------------- | -------- | ---------------- |
| id                  | long     | 评论 ID          |
| postId              | long     | 帖子 ID          |
| userId              | string   | 用户 ID          |
| userNickname        | string   | 昵称             |
| userAvatar          | string   | 头像             |
| replyToCommentId    | long     | 回复评论 ID      |
| replyToUserNickname | string   | 被回复用户昵称   |
| content             | string   | 内容             |
| likeCount           | int      | 点赞数           |
| isLiked             | boolean  | 当前用户点赞状态 |
| createdAt           | datetime | 创建时间         |

### DailyQuestionResp

| 字段            | 类型    | 说明        |
| --------------- | ------- | ----------- |
| questionId      | string  | 题目 ID     |
| title           | string  | 题目        |
| category        | string  | 分类        |
| pickId          | string  | 抽题记录 ID |
| pickDate        | date    | 抽题日期    |
| alreadyAnswered | boolean | 是否已答    |
| answerId        | string  | 答案 ID     |

### DailyQuestionAnswerResp

| 字段       | 类型     | 说明        |
| ---------- | -------- | ----------- |
| id         | string   | 答案 ID     |
| questionId | string   | 题目 ID     |
| pickId     | string   | 抽题记录 ID |
| answerDate | date     | 答题日期    |
| audioUrl   | string   | 音频链接    |
| durationMs | int      | 时长 ms     |
| visibility | int      | 可见性      |
| likeCount  | int      | 点赞数      |
| featured   | int      | 是否精选    |
| postId     | string   | 关联帖子 ID |
| createdAt  | datetime | 创建时间    |

### DailyRecommendationResp

| 字段          | 类型                          | 说明     |
| ------------- | ----------------------------- | -------- |
| id            | string                        | ID       |
| userId        | string                        | 用户 ID  |
| recommendDate | date                          | 推荐日期 |
| createdAt     | datetime                      | 创建时间 |
| items         | DailyRecommendationItemResp[] | 推荐内容 |

### DailyRecommendationItemResp

| 字段        | 类型                 | 说明                    |
| ----------- | -------------------- | ----------------------- |
| id          | string               | ID                      |
| dailyId     | string               | daily_recommendation ID |
| contentType | int                  | 内容类型                |
| contentId   | string               | 内容 ID                 |
| rankNo      | int                  | 排序                    |
| reason      | string               | 推荐理由                |
| createdAt   | datetime             | 创建时间                |
| content     | RecommendContentResp | 聚合内容                |

### RecommendContentResp

| 字段        | 类型                  | 说明     |
| ----------- | --------------------- | -------- |
| id          | string                | 内容 ID  |
| contentType | int                   | 类型     |
| title       | string                | 标题     |
| coverUrl    | string                | 封面     |
| summary     | string                | 摘要     |
| authorText  | string                | 作者     |
| externalUrl | string                | 外部链接 |
| lang        | string                | 语言     |
| status      | int                   | 状态     |
| createdAt   | datetime              | 创建时间 |
| updatedAt   | datetime              | 更新时间 |
| sentence    | RecommendSentenceResp | 句子详情 |
| article     | RecommendArticleResp  | 文章详情 |
| book        | RecommendBookResp     | 图书详情 |
| movie       | RecommendMovieResp    | 电影详情 |
| painting    | RecommendPaintingResp | 绘画详情 |
| music       | RecommendMusicResp    | 音乐详情 |

### FollowStateResp

| 字段      | 类型    | 说明     |
| --------- | ------- | -------- |
| following | boolean | 是否关注 |
| blocked   | boolean | 是否拉黑 |