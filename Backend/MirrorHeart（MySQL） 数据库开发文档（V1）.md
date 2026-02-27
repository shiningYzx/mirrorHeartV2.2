# MirrorHeart（MySQL） 数据库开发文档（V1）

## 1. 文档目的

本文档用于规范本项目的 MySQL 数据库设计与开发实践，确保：

- 数据结构清晰、可扩展
- 查询性能可控
- 与 Java（Spring Boot + MyBatis）开发体验友好
- 支撑 Feed、推荐系统、声音社区等核心业务

------

## 2. 技术选型与约定

### 2.1 数据库

- **数据库**：MySQL 8.x
- **存储引擎**：InnoDB
- **字符集**：`utf8mb4`
- **排序规则**：`utf8mb4_general_ci`（或 `utf8mb4_0900_ai_ci`）

### 2.2 主键策略

- 所有业务表主键统一使用：

```
CHAR(36)  -- UUID 字符串
```

**原因**：

- 与 Java / JSON / 日志天然一致
- 避免 BINARY(16) 带来的类型转换与调试成本
- 当前业务规模下性能可接受

> UUID 由 **Java 层生成**，数据库不负责生成。

------

## 3. 表设计通用规范

### 3.1 通用字段（强制）

除少数日志/中间表外，所有业务表统一包含：

```
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
is_deleted TINYINT(1) NOT NULL DEFAULT 0
deleted_at DATETIME NULL
```

### 3.2 软删除规范

- **所有删除操作默认软删**
- 查询时必须显式加：

```
WHERE is_deleted = 0
```

### 3.3 状态字段（推荐）

涉及可控生命周期的表（user / post / content 等）：

```
status TINYINT NOT NULL
```

通过 `status` 控制：

- 正常 / 下架 / 隐藏 / 预留审核

------

## 4. 命名规范

### 4.1 表命名

- 全小写
- 单数
- 下划线分隔

示例：

```
user
post
comment
daily_recommendation
```

### 4.2 字段命名

- 下划线风格
- 外键统一为 `xxx_id`

示例：

```
user_id
post_id
created_at
```

### 4.3 MyBatis 映射

- 数据库：`snake_case`
- Java：`camelCase`
- 通过字段别名或 MyBatis 自动映射完成

------

## 5. UUID 使用规范（Java + MyBatis）

### 5.1 Java 层

推荐：

```
String id = UUID.randomUUID().toString();
```

### 5.2 MyBatis

- Mapper 中直接使用 `String`
- 不要在 SQL 中生成 UUID

❌ 不推荐：

```
UUID()
```

------

## 6. 核心业务模型说明（简要）

### 6.1 用户体系

- `user`：基础用户信息
- `auth_session`：refresh token 管理
- `email_otp`：邮箱验证码
- `user_follow`：关注关系
- `user_block`：黑名单（双向不可见）

### 6.2 推荐系统

- `recommend_content`：推荐内容基表
- `recommend_*`：按类型拆分的详情表
- `daily_recommendation`：用户每日推荐归档
- `user_tag_pref`：用户标签偏好（简单推荐算法 v1）

### 6.3 声音社区

- `post`：帖子（文字/音频/图片混合）
- `post_image` / `post_audio`
- `comment`：两层评论
- `like_action`：统一点赞表
- `post_favorite`：收藏

------

## 7. Feed（拉模式）设计规范

### 7.1 拉模式原则

- 不做写扩散（不维护 feed_inbox）
- 每次查询时动态过滤：
  - 软删
  - 状态
  - 可见范围
  - **双向黑名单**

### 7.2 可见范围规则

| visibility | 含义   |
| ---------- | ------ |
| 1          | 所有人 |
| 2          | 仅粉丝 |
| 3          | 仅自己 |

### 7.3 黑名单规则（非常重要）

用户 A 看不到用户 B 的内容，当且仅当：

- A 屏蔽了 B
   **或**
- B 屏蔽了 A

即 **双向 block 均不可见**

------

## 8. 分页规范

### 8.1 推荐分页方式

✅ **Keyset 分页（游标分页）**

```
ORDER BY created_at DESC, id DESC
AND (
  created_at < :cursorCreatedAt
  OR (created_at = :cursorCreatedAt AND id < :cursorId)
)
LIMIT :limit
```

### 8.2 Offset 分页使用场景

- 后台管理
- 数据量小
- 明确知道不会深翻页

------

## 9. 索引设计原则

### 9.1 基本原则

- **为查询服务，不为表完整性服务**
- 少而精准
- 联合索引顺序 = 查询条件顺序

### 9.2 Feed 必备索引

```
post(created_at, id)
user_follow(follower_id, followee_id, status)
user_block(blocker_id, blocked_id, status)
like_action(user_id, target_type, target_id)
post_favorite(user_id, post_id)
```

------

## 10. 计数冗余与异步更新

### 10.1 冗余字段

- `post.like_count`
- `post.comment_count`
- `post.favorite_count`
- `post.pv`
- `post.uv`

### 10.2 更新原则

- **写入行为 ≠ 立即更新计数**
- 写事件 → `counter_event` 或 MQ
- 后台任务批量聚合更新

优点：

- 避免热点行锁
- 提高并发性能

------

## 11. 搜索设计

### 11.1 当前阶段

- 使用 MySQL `FULLTEXT`
- 表：`search_document`
- 统一索引入口

### 11.2 后续升级

- Elasticsearch / OpenSearch
- 数据结构保持一致
- 只替换搜索实现

------

## 12. 事务使用规范

### 12.1 必须使用事务的场景

- 发帖 + 附件
- 删除（软删）+ 关联数据
- 点赞/取消点赞 + 事件记录
- 注销用户（匿名化 + 快照）

### 12.2 不建议事务的场景

- 单表简单查询
- 统计查询

------

## 13. 注销与数据合规策略

### 13.1 注销流程

1. 用户申请注销
2. 立即匿名化（email / nickname）
3. 冷静期 30 天
4. 过期后：
   - 彻底删除
   - 或保留匿名内容（按产品策略）

### 13.2 原始数据保留

- 使用 `user_deletion_snapshot`
- 冷静期内可恢复
- 到期必须删除

------

## 14. MyBatis 使用建议

### 14.1 占位符

- 使用 `#{}`，不要用 `${}`
- 避免 SQL 注入

### 14.2 大查询拆分

❌ 不推荐：

```
post JOIN post_image JOIN post_audio
```

✅ 推荐：

- 主查询分页
- IN 查询批量拉附件

------

## 15. 常见坑位总结（非常重要）

1. ❌ 在数据库生成 UUID
2. ❌ 深分页 Offset
3. ❌ Feed 写扩散过早
4. ❌ 忽略黑名单双向规则
5. ❌ 计数实时更新
6. ❌ 一条 SQL 拉所有一对多数据
7. ❌ 删除不用软删

------

## 16. 后续可扩展方向

- 推荐算法升级（协同过滤 / Embedding）
- Feed 热度排序（时间 × 互动权重）
- 内容审核系统接入
- ES 搜索替换 FULLTEXT
- 冷数据归档

