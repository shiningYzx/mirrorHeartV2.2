-- ==========================================================
-- Mirror Heart V2.2 - Full Database Design
-- 核心设计：
-- 1. 单一用户表 + RBAC 扩展
-- 2. 垂直分表的内容库 (Content Library)
-- 3. 独立的标签系统 (Tag System)
-- 4. 完整的社区与推荐体系
-- ==========================================================

-- 0) 初始化数据库
CREATE DATABASE IF NOT EXISTS mirrot_heart_v2_2
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE mirrot_heart_v2_2;


-- =========================
-- 1) User & Auth (基础用户与认证)
-- =========================

-- [用户主表]
-- 系统的核心身份表，存储所有用户（包括普通用户和管理员）的基础信息。
-- 通过 role 字段区分是否为管理员身份（0=普通用户, 1=管理员）。
-- 若是管理员，具体的权限细节需查询 admin_user_role 表。
CREATE TABLE IF NOT EXISTS `user` (
  `id`            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `email`         VARCHAR(255) NOT NULL COMMENT '登录邮箱，作为唯一凭证',
  `password_hash` VARCHAR(255) NOT NULL COMMENT 'Bcrypt算法加密后的密码',
  `nickname`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '用户昵称',
  `avatar_url`    VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像URL',
  `bio`           VARCHAR(255) NOT NULL DEFAULT '' COMMENT '个人简介/签名',
  
  -- 权限标识：0=普通用户, 1=管理员 (管理员的具体权限看 admin_user_role)
  `role`          TINYINT NOT NULL DEFAULT 0,
  
  -- 状态：1=正常, 0=禁用, 2=注销中
  `status`        TINYINT NOT NULL DEFAULT 1,
  `is_deleted`    TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  
  `last_login_at` DATETIME(3) NULL COMMENT '最后登录时间',
  `created_at`    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_status` (`status`, `is_deleted`),
  KEY `idx_user_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础表';

ALTER TABLE `user` 
ADD COLUMN `show_following` TINYINT NOT NULL DEFAULT 1 COMMENT '是否展示关注和粉丝: 0=隐藏, 1=展示',
ADD COLUMN `show_favorite` TINYINT NOT NULL DEFAULT 1 COMMENT '是否展示我的收藏: 0=隐藏, 1=展示',
ADD COLUMN `show_post` TINYINT NOT NULL DEFAULT 1 COMMENT '是否展示我的帖子: 0=隐藏, 1=展示';
ALTER TABLE `user`
ADD COLUMN `birthday` DATE NULL COMMENT '生日',
ADD COLUMN `birthday_update_time` DATETIME(3) NULL COMMENT '上次修改生日的时间，用于限制每年只能改一次';

-- [邮箱验证码表]
-- 用于注册、登录、重置密码等场景的邮件验证码 (OTP) 存储。
-- 记录了验证码的用途(scene)、过期时间以及是否已使用。
CREATE TABLE IF NOT EXISTS `email_otp` (
  `id`           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `email`        VARCHAR(255) NOT NULL,
  `scene`        VARCHAR(32)  NOT NULL COMMENT '业务场景: register/login/reset',
  `code`         VARCHAR(16)  NOT NULL COMMENT '验证码字符',
  `expire_at`    DATETIME(3)  NOT NULL COMMENT '过期时间',
  `used_at`      DATETIME(3)  NULL COMMENT '使用时间，非空表示已验证',
  `created_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  KEY `idx_otp_query` (`email`, `scene`, `code`, `expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码';

-- [认证会话表]
-- 实现了服务端对 JWT 的管理能力。
-- 每当用户登录，会生成一条 session 记录和对应的 jti (JWT ID)。
-- 作用：支持“查看当前登录设备”、“踢人下线(Revoke Token)”以及统计在线情况。
CREATE TABLE IF NOT EXISTS `auth_session` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `jti`        VARCHAR(64) NOT NULL COMMENT 'JWT ID (UUID)，对应 Token 中的 jti 字段',
  `device`     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '设备名称',
  `ip`         VARCHAR(64) NOT NULL DEFAULT '' COMMENT '登录IP',
  `expired_at` DATETIME(3) NOT NULL COMMENT 'Token 自然过期时间',
  `revoked_at` DATETIME(3) NULL COMMENT '非空则表示被手动吊销/踢出',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_session_jti` (`jti`),
  KEY `idx_session_user` (`user_id`, `created_at`),
  CONSTRAINT `fk_session_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证会话表';


CREATE TABLE IF NOT EXISTS `account_deletion_request` (
  `id` BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '申请注销的用户ID',
  `reason` VARCHAR(255) DEFAULT '' COMMENT '注销原因',
  `requested_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `execute_after` DATETIME NOT NULL COMMENT '计划执行注销的时间(例如申请的7天后)',
  `canceled_at` DATETIME DEFAULT NULL COMMENT '用户撤销注销的时间',
  `executed_at` DATETIME DEFAULT NULL COMMENT '系统实际执行注销的时间',
  
  KEY `idx_execute_task` (`execute_after`, `canceled_at`, `executed_at`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号注销申请表';


-- =========================
-- 2) Admin RBAC (基于User表的权限扩展)
-- =========================

-- [管理员角色表]
-- 定义后台管理的岗位/角色，例如“超级管理员”、“内容运营”、“审核专员”。
CREATE TABLE IF NOT EXISTS `admin_role` (
  `id`          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `role_name`   VARCHAR(64)  NOT NULL COMMENT '角色显示名: 如 内容管理员',
  `role_key`    VARCHAR(64)  NOT NULL COMMENT '程序用唯一标识: 如 content_admin',
  `description` VARCHAR(255) DEFAULT '' COMMENT '角色职责描述',
  `created_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RBAC角色表';

-- [权限/菜单表]
-- 定义系统中的最小权限粒度，可以是左侧的菜单项，也可以是页面上的具体按钮或API接口。
-- permission_key 是代码中 @PreAuthorize 校验的依据。
CREATE TABLE IF NOT EXISTS `admin_permission` (
  `id`              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `parent_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父ID，用于构建菜单树',
  `permission_name` VARCHAR(64)  NOT NULL COMMENT '权限名: 如 删除帖子',
  `permission_key`  VARCHAR(128) NOT NULL COMMENT '鉴权标识: 如 post:delete',
  `type`            TINYINT NOT NULL COMMENT '1=菜单(Menu), 2=按钮/API(Button)',
  `path`            VARCHAR(255) DEFAULT '' COMMENT '前端路由或后端API路径',
  `created_at`      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY `uk_perm_key` (`permission_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RBAC权限表';

-- [用户-角色关联表]
-- RBAC的核心连接表。将 user 表中 role=1 的管理员用户与 admin_role 关联起来。
-- 一个管理员可以拥有多个角色。
-- 1. 修复 admin_user_role
DROP TABLE IF EXISTS `admin_user_role`;
CREATE TABLE `admin_user_role` (
  `id` BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, -- 新增自增ID
  `user_id` BIGINT UNSIGNED NOT NULL,
  `role_id` BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`), -- 改为唯一索引
  CONSTRAINT `fk_aur_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_aur_role` FOREIGN KEY (`role_id`) REFERENCES `admin_role`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- [角色-权限关联表]
-- 定义每个角色拥有哪些具体权限。
-- 登录时，通过 用户->角色->权限 的链路查出用户的所有可用权限列表。
-- 2. 修复 admin_role_permission
DROP TABLE IF EXISTS `admin_role_permission`;
CREATE TABLE `admin_role_permission` (
  `id` BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, -- 新增自增ID
  `role_id`       BIGINT UNSIGNED NOT NULL,
  `permission_id` BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`), -- 改为唯一索引
  CONSTRAINT `fk_arp_role` FOREIGN KEY (`role_id`) REFERENCES `admin_role`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_arp_perm` FOREIGN KEY (`permission_id`) REFERENCES `admin_permission`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';


-- =========================
-- 3) Common Tags (通用标签系统)
-- =========================

-- [标签库表]
-- 系统的公共标签池。无论是内容库的内容（书/影/音）还是用户发的帖子，都关联到这里的标签 ID。
-- 避免了标签数据的冗余和不一致，方便做全站的“按标签聚合”功能。
CREATE TABLE IF NOT EXISTS `tag` (
  `id`          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `name`        VARCHAR(64) NOT NULL COMMENT '标签名称',
  `group_name`  VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '标签分组: mood(心情), genre(流派)...',
  `use_count`   INT NOT NULL DEFAULT 0 COMMENT '被引用次数统计',
  `created_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY `uk_tag_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签库';


-- =========================
-- 4) Content Library (内容库)
-- =========================

-- [内容主表]
-- 存储所有类型（金句、文章、书、画、音、影）的公共元数据。
-- 采用“主表+垂直分表”的设计策略。
-- id 是全局唯一的内容ID，title/cover/summary 是通用的展示字段。
CREATE TABLE IF NOT EXISTS `content` (
  `id`          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `type`        VARCHAR(16) NOT NULL COMMENT '类型枚举: QUOTE/ARTICLE/BOOK/PAINTING/MUSIC/MOVIE',
  `title`       VARCHAR(255) NOT NULL DEFAULT '' COMMENT '标题',
  `summary`     VARCHAR(512) NOT NULL DEFAULT '' COMMENT '摘要/简介',
  `cover_url`   VARCHAR(512) NOT NULL DEFAULT '' COMMENT '封面图',
  `source`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '来源/出处',
  
  `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=已发布, 0=草稿',
  `like_count`  INT NOT NULL DEFAULT 0 COMMENT '点赞数冗余，用于快速排序',
  `is_deleted`  TINYINT NOT NULL DEFAULT 0,
  
  `created_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

  KEY `idx_content_type` (`type`, `status`),
  KEY `idx_content_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容主表';

-- [内容-标签关联表]
-- 记录内容与标签的多对多关系。
-- 3. 修复 content_tag_relation
DROP TABLE IF EXISTS `content_tag_relation`;
CREATE TABLE `content_tag_relation` (
  `id` BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, -- 新增自增ID
  `content_id` BIGINT UNSIGNED NOT NULL,
  `tag_id`     BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY `uk_content_tag` (`content_id`, `tag_id`), -- 改为唯一索引
  CONSTRAINT `fk_ctr_content` FOREIGN KEY (`content_id`) REFERENCES `content`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ctr_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [内容扩展表：金句]
CREATE TABLE IF NOT EXISTS `content_quote` (
  `content_id` BIGINT UNSIGNED PRIMARY KEY,
  `text`       VARCHAR(1024) NOT NULL COMMENT '金句正文',
  `author`     VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '原作者',
  CONSTRAINT `fk_quote_main` FOREIGN KEY (`content_id`) REFERENCES `content`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [内容扩展表：文章]
CREATE TABLE IF NOT EXISTS `content_article` (
  `content_id` BIGINT UNSIGNED PRIMARY KEY,
  `body`       MEDIUMTEXT NOT NULL COMMENT '文章HTML或Markdown内容',
  `author`     VARCHAR(255) NOT NULL DEFAULT '',
  CONSTRAINT `fk_article_main` FOREIGN KEY (`content_id`) REFERENCES `content`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [内容扩展表：书籍]
CREATE TABLE IF NOT EXISTS `content_book` (
  `content_id` BIGINT UNSIGNED PRIMARY KEY,
  `isbn`       VARCHAR(32)  NOT NULL DEFAULT '' COMMENT 'ISBN号',
  `author`     VARCHAR(255) NOT NULL DEFAULT '',
  `publisher`  VARCHAR(255) NOT NULL DEFAULT '' COMMENT '出版社',
  CONSTRAINT `fk_book_main` FOREIGN KEY (`content_id`) REFERENCES `content`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [内容扩展表：画作]
CREATE TABLE IF NOT EXISTS `content_painting` (
  `content_id` BIGINT UNSIGNED PRIMARY KEY,
  `painter`    VARCHAR(255) NOT NULL DEFAULT '' COMMENT '画家',
  `year`       VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '创作年份',
  CONSTRAINT `fk_painting_main` FOREIGN KEY (`content_id`) REFERENCES `content`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [内容扩展表：音乐]
CREATE TABLE IF NOT EXISTS `content_music` (
  `content_id`  BIGINT UNSIGNED PRIMARY KEY,
  `artist`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '艺术家/歌手',
  `audio_url`   VARCHAR(512) NOT NULL DEFAULT '' COMMENT '音乐文件链接',
  `duration_ms` INT NOT NULL DEFAULT 0 COMMENT '时长(毫秒)',
  `lyric`       MEDIUMTEXT NULL COMMENT '歌词',
  CONSTRAINT `fk_music_main` FOREIGN KEY (`content_id`) REFERENCES `content`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [内容扩展表：电影]
CREATE TABLE IF NOT EXISTS `content_movie` (
  `content_id` BIGINT UNSIGNED PRIMARY KEY,
  `director`   VARCHAR(255) NOT NULL DEFAULT '' COMMENT '导演',
  `year`       VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '上映年份',
  CONSTRAINT `fk_movie_main` FOREIGN KEY (`content_id`) REFERENCES `content`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE mirrot_heart_v2_2;

-- ---------------------------------------------------
-- 0. 迁移前准备：清空 V3 相关表的数据
-- (使用 SET FOREIGN_KEY_CHECKS = 0 配合 DELETE FROM 绕过 InnoDB 外键强校验)
-- ---------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

-- 清空每日推荐相关表 (防止有残留的旧内容关联数据)
DELETE FROM `daily_recommendation_item`;
DELETE FROM `daily_recommendation`;

-- 清空内容主表及其相关子表、标签关联表
DELETE FROM `content_tag_relation`;
DELETE FROM `content_quote`;
DELETE FROM `content_article`;
DELETE FROM `content_book`;
DELETE FROM `content_painting`;
DELETE FROM `content_music`;
DELETE FROM `content_movie`;
DELETE FROM `content`;

SET FOREIGN_KEY_CHECKS = 1;
-- ---------------------------------------------------
-- 1. 迁移文章数据 (Articles -> content + content_article)
-- 策略：文章 ID 统一加 10000
-- ---------------------------------------------------
-- 1.1 插入 Content 主表 (共11个字段)
REPLACE INTO mirrot_heart_v2_2.content (id, type, title, summary, cover_url, source, status, like_count, is_deleted, created_at, updated_at)
SELECT 
    id + 10000, 
    'ARTICLE', 
    title, 
    LEFT(content, 200), -- 截取正文前200字作摘要
    '',                 -- 文章暂无封面
    IFNULL(author, ''), -- 🌟 映射：将文章作者放入 source 字段
    1, 0, 0, NOW(), NOW()
FROM mirror_heart.articles;

-- 1.2 插入 Article 子表
REPLACE INTO mirrot_heart_v2_2.content_article (content_id, body, author)
SELECT 
    id + 10000, 
    content, 
    author
FROM mirror_heart.articles;


-- ---------------------------------------------------
-- 2. 迁移书籍数据 (Books -> content + content_book)
-- 策略：书籍 ID 统一加 20000
-- ---------------------------------------------------
REPLACE INTO mirrot_heart_v2_2.content (id, type, title, summary, cover_url, source, status, like_count, is_deleted, created_at, updated_at)
SELECT 
    id + 20000, 
    'BOOK', 
    title, 
    LEFT(IFNULL(brief, ''), 500), -- 截取前500字，防止旧表数据超长报错
    IFNULL(cover_img, ''), 
    IFNULL(author, ''),           -- 🌟 映射：将书籍作者放入 source 字段
    1, 0, 0, NOW(), NOW()
FROM mirror_heart.books;

REPLACE INTO mirrot_heart_v2_2.content_book (content_id, isbn, author, publisher)
SELECT 
    id + 20000, 
    '',                  -- 旧表无 isbn，赋空字符串
    author, 
    ''                   -- 旧表无出版社，赋空字符串
FROM mirror_heart.books;


-- ---------------------------------------------------
-- 3. 迁移电影数据 (Movies -> content + content_movie)
-- 策略：电影 ID 统一加 30000
-- ---------------------------------------------------
REPLACE INTO mirrot_heart_v2_2.content (id, type, title, summary, cover_url, source, status, like_count, is_deleted, created_at, updated_at)
SELECT 
    id + 30000, 
    'MOVIE', 
    title, 
    LEFT(IFNULL(brief, ''), 500), -- 截取前500字，防止旧表数据超长报错
    IFNULL(cover_img, ''), 
    '',                           -- 电影旧表无导演记录，此处留空
    1, 0, 0, NOW(), NOW()
FROM mirror_heart.movies;

REPLACE INTO mirrot_heart_v2_2.content_movie (content_id, director, year)
SELECT 
    id + 30000, 
    '',                  -- 旧表无 director
    ''                   -- 旧表无 year
FROM mirror_heart.movies;


-- ---------------------------------------------------
-- 4. 迁移音乐数据 (Music -> content + content_music)
-- 策略：音乐 ID 统一加 40000
-- ---------------------------------------------------
REPLACE INTO mirrot_heart_v2_2.content (id, type, title, summary, cover_url, source, status, like_count, is_deleted, created_at, updated_at)
SELECT 
    id + 40000, 
    'MUSIC', 
    title, 
    '',                  -- 音乐无摘要
    IFNULL(cover_img, ''), 
    IFNULL(singer, ''),  -- 🌟 映射：将歌手放入 source 字段
    1, 0, 0, NOW(), NOW()
FROM mirror_heart.music;

REPLACE INTO mirrot_heart_v2_2.content_music (content_id, artist, audio_url, duration_ms, lyric)
SELECT 
    id + 40000, 
    singer, 
    IFNULL(song_address, ''), 
    0,                   -- 旧表无时长
    song_lyric
FROM mirror_heart.music;


-- ---------------------------------------------------
-- 5. 迁移画作数据 (Paintings -> content + content_painting)
-- 策略：画作 ID 统一加 50000
-- ---------------------------------------------------
REPLACE INTO mirrot_heart_v2_2.content (id, type, title, summary, cover_url, source, status, like_count, is_deleted, created_at, updated_at)
SELECT 
    id + 50000, 
    'PAINTING', 
    title, 
    '',                  -- 画作无摘要
    IFNULL(paint_work, ''), 
    IFNULL(painter, ''), -- 🌟 映射：将画家放入 source 字段
    1, 0, 0, NOW(), NOW()
FROM mirror_heart.paintings;

REPLACE INTO mirrot_heart_v2_2.content_painting (content_id, painter, year)
SELECT 
    id + 50000, 
    painter, 
    ''                   -- 旧表无年份
FROM mirror_heart.paintings;


-- ---------------------------------------------------
-- 6. 迁移金句数据 (Sentences -> content + content_quote)
-- 策略：金句 ID 统一加 60000
-- ---------------------------------------------------
REPLACE INTO mirrot_heart_v2_2.content (id, type, title, summary, cover_url, source, status, like_count, is_deleted, created_at, updated_at)
SELECT 
    id + 60000, 
    'QUOTE', 
    LEFT(content, 30),   -- 截取金句前30字作为占位标题
    '', 
    '', 
    IFNULL(author, ''),  -- 🌟 映射：将金句作者放入 source 字段
    1, 0, 0, NOW(), NOW()
FROM mirror_heart.sentences;

REPLACE INTO mirrot_heart_v2_2.content_quote (content_id, text, author)
SELECT 
    id + 60000, 
    content, 
    IFNULL(author, '') 
FROM mirror_heart.sentences;

-- ==========================================================
-- 迁移完成！
-- 所有类型的旧内容已经完美融入 V3 架构的内容主表与子表中，互不干扰。
-- ==========================================================



-- =========================
-- 5) Daily Recommendation (每日推荐)
-- =========================

-- [每日推荐主表]
-- 记录系统每天为用户生成的推荐批次。
-- 这里的 strategy 字段可用于 A/B 测试不同的推荐算法。
CREATE TABLE IF NOT EXISTS `daily_recommendation` (
  `id`           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`      BIGINT UNSIGNED NOT NULL,
  `day`          DATE NOT NULL COMMENT '推荐日期',
  `strategy`     VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT '推荐策略/算法版本',
  `viewed_at`    DATETIME(3) NULL COMMENT '用户查看时间，为空表示未读',
  `created_at`   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_daily_user_day` (`user_id`, `day`),
  CONSTRAINT `fk_daily_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日推荐记录';

ALTER TABLE `daily_recommendation` DROP FOREIGN KEY `fk_daily_user`;

-- [每日推荐明细表]
-- 关联推荐批次与具体的内容ID。
-- rank 字段决定了用户看到的卡片顺序。
CREATE TABLE IF NOT EXISTS `daily_recommendation_item` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `daily_id`   BIGINT UNSIGNED NOT NULL,
  `content_id` BIGINT UNSIGNED NOT NULL COMMENT '关联content表',
  `rank`       INT NOT NULL DEFAULT 1 COMMENT '排序权重',

  KEY `idx_item_daily` (`daily_id`),
  CONSTRAINT `fk_item_daily` FOREIGN KEY (`daily_id`) REFERENCES `daily_recommendation`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_item_content` FOREIGN KEY (`content_id`) REFERENCES `content`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐内容详情';


-- =========================
-- 6) Community (社区)
-- =========================

-- [帖子表]
-- 社区的核心表，存储用户发布的动态。
-- 包含可见性控制(visibility)和热度分(heat_score)等关键业务字段。
CREATE TABLE IF NOT EXISTS `post` (
  `id`             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '发帖人',
  `title`          VARCHAR(255) NOT NULL DEFAULT '' COMMENT '可选标题',
  `text`           MEDIUMTEXT NULL COMMENT '帖子正文',
  
  `visibility`     TINYINT NOT NULL DEFAULT 0 COMMENT '0=公开, 1=粉丝可见, 2=私密',
  `status`         TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常, 0=审核中, 2=封禁',
  
  -- 计数器 (建议通过Redis+定时任务写入)
  `like_count`     INT NOT NULL DEFAULT 0,
  `comment_count`  INT NOT NULL DEFAULT 0,
  `favorite_count` INT NOT NULL DEFAULT 0,
  `view_count`     INT NOT NULL DEFAULT 0,
  `heat_score`     BIGINT NOT NULL DEFAULT 0 COMMENT '算法计算的热度值',

  `is_deleted`     TINYINT NOT NULL DEFAULT 0,
  `created_at`     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

  KEY `idx_post_user` (`user_id`),
  KEY `idx_post_heat` (`heat_score`, `created_at`),
  CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子表';

-- [帖子-标签关联表]
-- 帖子与通用标签的关联。
-- 4. 修复 post_tag_relation
DROP TABLE IF EXISTS `post_tag_relation`;
CREATE TABLE `post_tag_relation` (
  `id` BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, -- 新增自增ID
  `post_id` BIGINT UNSIGNED NOT NULL,
  `tag_id`  BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY `uk_post_tag` (`post_id`, `tag_id`), -- 改为唯一索引
  CONSTRAINT `fk_ptr_post` FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ptr_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [帖子图片表]
-- 支持一个帖子包含多张图片。
CREATE TABLE IF NOT EXISTS `post_image` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `url`        VARCHAR(512) NOT NULL COMMENT '图片地址',
  `sort`       INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
  KEY `idx_img_post` (`post_id`),
  CONSTRAINT `fk_img_post` FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [帖子音频表]
-- 支持帖子携带一条音频（如语音动态）。
CREATE TABLE IF NOT EXISTS `post_audio` (
  `id`          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `post_id`     BIGINT UNSIGNED NOT NULL,
  `url`         VARCHAR(512) NOT NULL COMMENT '音频地址',
  `duration_ms` INT NOT NULL DEFAULT 0 COMMENT '时长',
  UNIQUE KEY `uk_audio_post` (`post_id`),
  CONSTRAINT `fk_audio_post` FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [评论表]
-- 帖子的评论区。支持两级评论结构（通过 root_id 和 parent_id 控制）。
CREATE TABLE IF NOT EXISTS `comment` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '评论人',
  `text`       VARCHAR(1024) NOT NULL COMMENT '评论内容',
  
  `parent_id`  BIGINT UNSIGNED NULL COMMENT '被回复的父评论ID',
  `root_id`    BIGINT UNSIGNED NULL COMMENT '所属的根评论ID',
  
  `like_count` INT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  KEY `idx_comment_post` (`post_id`, `created_at`),
  CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

CREATE TABLE IF NOT EXISTS `post_view_history` (
  `id` BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '浏览者ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '浏览的帖子ID',
  `viewed_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最后浏览时间',
  
  -- 添加复合唯一索引，方便更新相同帖子的浏览时间
  UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
  KEY `idx_user_time` (`user_id`, `viewed_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户帖子浏览历史表';

-- =========================
-- 7) Interactions (通用交互)
-- =========================

-- [通用点赞表]
-- 统一存储所有的点赞行为。
-- 通过 target_type 区分点赞对象是帖子(POST)、评论(COMMENT)还是静态内容(CONTENT)。
CREATE TABLE IF NOT EXISTS `like_action` (
  `id`          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `target_type` VARCHAR(16) NOT NULL COMMENT '类型: POST/COMMENT/CONTENT/DAILY_ANSWER',
  `target_id`   BIGINT UNSIGNED NOT NULL COMMENT '目标表的主键ID',
  `created_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_like` (`user_id`, `target_type`, `target_id`),
  KEY `idx_like_target` (`target_type`, `target_id`),
  CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用点赞表';

-- [收藏表]
-- 用户收藏帖子。后续如果需要收藏内容，可扩展 type 字段或增加新表。
CREATE TABLE IF NOT EXISTS `favorite_action` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_fav` (`user_id`, `post_id`),
  CONSTRAINT `fk_fav_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_fav_post` FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子收藏表';

-- [用户关系表]
-- 统一管理用户之间的关注(Follow)和拉黑(Block)关系。
-- type=1 为关注，type=2 为拉黑。
CREATE TABLE IF NOT EXISTS `user_relation` (
  `id`            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `from_user_id`  BIGINT UNSIGNED NOT NULL COMMENT '发起方',
  `to_user_id`    BIGINT UNSIGNED NOT NULL COMMENT '目标方',
  `type`          TINYINT NOT NULL COMMENT '1=关注(Follow), 2=拉黑(Block)',
  `created_at`    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_relation` (`from_user_id`, `to_user_id`, `type`),
  CONSTRAINT `fk_rel_from` FOREIGN KEY (`from_user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_rel_to` FOREIGN KEY (`to_user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关系表';


-- =========================
-- 8) Feature: Question & AI
-- =========================

-- [题库表] (改造原 daily_question)
-- 这里存储所有准备好的问题。不绑定日期，由算法从中挑选。
CREATE TABLE IF NOT EXISTS `daily_question` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `text`       VARCHAR(512) NOT NULL COMMENT '问题文本',
  `topic`      VARCHAR(64)  NOT NULL DEFAULT 'general' COMMENT '话题/分类',
  `use_count`  INT NOT NULL DEFAULT 0 COMMENT '被推送次数',
  `status`     TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用, 0=停用',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题库(不绑定日期)';
ALTER TABLE `daily_question` ADD UNIQUE KEY `uk_question_content` (`text`);

-- 生成了15条内省/启发式的问题，可以根据实际需要修改日期起点
INSERT IGNORE INTO `daily_question` (`text`) VALUES
('今天有哪些瞬间让你感到平静？'),
('如果你可以对十年前的自己说一句话，那会是什么？'),
('最近让你感到充满力量的一首歌是什么？'),
('如果你的情绪有一种颜色，今天是什么颜色？'),
('你一直想去尝试但又犹豫不决的事情是什么？'),
('描述一个你最近注意到的微小细节。'),
('谁是你生命中最想感谢的人，为什么？'),
('你认为“完美的周末”应该是什么样的？'),
('最近让你开怀大笑的一件事是什么？'),
('你目前最大的压力来源是什么？可以如何缓解？'),
('哪一本书或哪一部电影深刻地改变了你的想法？'),
('描述一次你勇敢面对恐惧的经历。'),
('如果不需要考虑金钱，你最想从事什么职业？'),
('今天有什么事情让你感到自豪？'),
('写下一句你最想对自己说的话。');

-- [每日一问记录表] (推送 + 回答)
-- 1. 每日用户打开APP时，算法选定问题插入此表 (audio_url 为 NULL)。
-- 2. 用户回答后，更新此表的 audio_url 和 answered_at。
-- 3. 保证了用户每天只能看到并回答一道题。
CREATE TABLE IF NOT EXISTS `user_daily_record` (
  `id`          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `day`         DATE NOT NULL COMMENT '日期',
  `question_id` BIGINT UNSIGNED NOT NULL COMMENT '当日推送的问题ID',
  `answer_text` TEXT DEFAULT NULL COMMENT '文本回答内容'，
  
  -- 回答相关字段 (NULL表示未回答)
  `audio_url`   VARCHAR(512) NULL COMMENT '回答语音',
  `duration_ms` INT NOT NULL DEFAULT 0,
  `answered_at` DATETIME(3) NULL COMMENT '回答时间',
  
  -- 社交与热度字段 (新增)
  `visibility`     TINYINT NOT NULL DEFAULT 0 COMMENT '0=公开, 1=粉丝可见, 2=私密',
  `like_count`     INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `heat_score`     BIGINT NOT NULL DEFAULT 0 COMMENT '热度值(用于热门回答排序)',

  `created_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '推送时间',

  UNIQUE KEY `uk_record_user_day` (`user_id`, `day`),
  KEY `idx_record_q_heat` (`question_id`, `visibility`, `heat_score`), -- 用于"查看该问题下的热门回答"
  CONSTRAINT `fk_record_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_record_q` FOREIGN KEY (`question_id`) REFERENCES `daily_question`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日一问记录(推送+回答)';

ALTER TABLE `user_daily_record` DROP FOREIGN KEY `fk_record_user`;
ALTER TABLE `user_daily_record` ADD COLUMN `answer_text` TEXT DEFAULT NULL COMMENT '文本回答内容' AFTER `question_id`;

-- [AI 聊天会话表]
-- 记录用户与 AI 的每一个对话窗口（Session）。
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `title`      VARCHAR(255) NOT NULL DEFAULT 'New Chat' COMMENT '会话标题',
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT `fk_ai_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话列表';

-- [AI 消息记录表]
-- 会话中具体的聊天记录（用户提问 + AI回答）。
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `session_id` BIGINT UNSIGNED NOT NULL,
  `role`       VARCHAR(16) NOT NULL COMMENT '发送方: user/assistant',
  `content`    MEDIUMTEXT NOT NULL COMMENT '消息内容',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT `fk_msg_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天消息';

-- [待办事项表]
-- 简单的 To-Do List 功能。
CREATE TABLE IF NOT EXISTS `todo_item` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `title`      VARCHAR(255) NOT NULL,
  `status`     TINYINT NOT NULL DEFAULT 0 COMMENT '0=未做, 1=已完成',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT `fk_todo_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户待办事项';

ALTER TABLE `todo_item`
ADD COLUMN `completed_at` DATETIME(3) NULL COMMENT '待办完成时间';

-- 微光行动：官方推荐待办（灵感池）
CREATE TABLE IF NOT EXISTS `todo_recommendation` (
  `id` BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `text` VARCHAR(255) NOT NULL COMMENT '推荐待办内容',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用, 1=启用',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微光行动推荐池';

-- 插入一些初始的治愈系默认数据
INSERT INTO `todo_recommendation` (`text`, `status`) VALUES
('去阳台或楼下，找出一株你叫不出名字的植物。', 1),
('去便利店买一样自己想吃的小零食。', 1),
('给好久没联系的朋友发一个可爱的表情包，不强求聊天。', 1),
('在纸上写下 3 件今天做得还不错的小事。', 1),
('听一首自己最喜欢的、时长不超过 3 分钟的歌。', 1),
('给家里的植物浇一点水。', 1),
('拍一张天空的照片。', 1),
('深呼吸 10 次，感受空气流动。', 1),
('整理一下桌面，腾出一块干净的空间。', 1),
('喝一杯温水，慢慢咽下。', 1);

-- =========================
-- 9) Governance (治理与审计)
-- =========================

-- [举报/反馈表]
-- 用于内容治理。用户对帖子、评论或用户进行举报后，数据存入此表，供管理员后台审核。
CREATE TABLE IF NOT EXISTS `report` (
  `id`           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `reporter_id`  BIGINT UNSIGNED NOT NULL COMMENT '举报人ID',
  `target_type`  VARCHAR(16) NOT NULL COMMENT '目标类型: POST/COMMENT/USER',
  `target_id`    BIGINT UNSIGNED NOT NULL COMMENT '目标ID',
  `reason`       VARCHAR(255) NOT NULL COMMENT '举报理由',
  `status`       TINYINT NOT NULL DEFAULT 0 COMMENT '0=待处理, 1=已处理, 2=忽略',
  `admin_note`   VARCHAR(255) DEFAULT '' COMMENT '管理员处理批注',
  `created_at`   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  
  KEY `idx_report_status` (`status`),
  CONSTRAINT `fk_report_user` FOREIGN KEY (`reporter_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报/反馈表';

-- [管理端审计日志]
-- 安全审计核心表。记录管理员的所有关键操作（如删除帖子、封禁用户），用于事后追溯。
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
  `id`             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '操作人ID(管理员)',
  `module`         VARCHAR(64) NOT NULL COMMENT '业务模块: Post, User, System',
  `operation`      VARCHAR(128) NOT NULL COMMENT '具体操作: Delete, Ban',
  `target_id`      VARCHAR(64) DEFAULT NULL COMMENT '被操作对象的ID快照',
  `ip`             VARCHAR(64) DEFAULT '' COMMENT '操作者IP',
  `params`         TEXT COMMENT '请求参数快照(JSON)',
  `created_at`     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  KEY `idx_audit_user` (`user_id`, `created_at`),
  KEY `idx_audit_module` (`module`),
  CONSTRAINT `fk_audit_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端审计日志';


-- =========================
-- 10) System & Tools (系统工具与配置)
-- =========================

-- [消息通知表]
-- 用户的消息中心数据源。
-- 当发生点赞、评论、系统通知时写入此表。前端轮询或长连接拉取未读数。
CREATE TABLE IF NOT EXISTS `notification` (
  `id`           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `receiver_id`  BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
  `sender_id`    BIGINT UNSIGNED NULL COMMENT '发送者ID(系统通知则为空)',
  `type`         VARCHAR(32) NOT NULL COMMENT '类型: LIKE/COMMENT/FOLLOW/SYSTEM',
  `target_type`  VARCHAR(32) NOT NULL DEFAULT '' COMMENT '关联对象类型: POST/COMMENT/DAILY_ANSWER',
  `target_id`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关联对象ID',
  `content`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '通知简述/摘要',
  `is_read`      TINYINT NOT NULL DEFAULT 0 COMMENT '0=未读, 1=已读',
  `created_at`   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  
  KEY `idx_notif_receiver` (`receiver_id`, `is_read`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- [文件资源管理表]
-- 记录上传到 OSS 的文件元数据。
-- 作用：管理文件生命周期。上传时 status=0(临时)，发帖成功后 status=1(已引用)。
-- 定时任务可清理超过24小时仍为 status=0 的孤儿文件，节省存储成本。
CREATE TABLE IF NOT EXISTS `sys_file` (
  `id`           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '上传者',
  `url`          VARCHAR(512) NOT NULL COMMENT '文件访问地址',
  `file_key`     VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'OSS存储Key',
  `size_bytes`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `mime_type`    VARCHAR(128) NOT NULL DEFAULT '',
  `scene`        VARCHAR(32) NOT NULL COMMENT '场景: AVATAR/POST_IMG/AUDIO',
  `status`       TINYINT NOT NULL DEFAULT 0 COMMENT '0=临时(未引用), 1=已引用(持久化)',
  `created_at`   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  
  KEY `idx_file_status` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件资源管理表';

-- [系统配置表]
-- 存储动态开关和配置，无需发版即可更改 App 行为。
-- 如: "daily_question_enabled": "true"
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id`           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `config_key`   VARCHAR(64) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '配置值',
  `description`  VARCHAR(255) DEFAULT '' COMMENT '描述',
  `updated_at`   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- [App版本管理表]
-- 用于 App 启动时的版本检查和强制更新逻辑。
CREATE TABLE IF NOT EXISTS `app_version` (
  `id`           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `platform`     VARCHAR(16) NOT NULL DEFAULT 'ANDROID' COMMENT '平台: ANDROID/IOS',
  `version_code` INT NOT NULL COMMENT '版本号(整数, 如 101)',
  `version_name` VARCHAR(32) NOT NULL COMMENT '版本名(如 1.0.1)',
  `is_force`     TINYINT NOT NULL DEFAULT 0 COMMENT '是否强制更新',
  `download_url` VARCHAR(512) NOT NULL COMMENT '下载/跳转地址',
  `update_log`   TEXT COMMENT '更新日志',
  `created_at`   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  
  KEY `idx_version_code` (`platform`, `version_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='App版本管理';

-- [用户搜索历史表]
-- 记录用户的搜索行为，用于展示"历史搜索"和分析用户兴趣。
CREATE TABLE IF NOT EXISTS `search_history` (
  `id`           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`      BIGINT UNSIGNED NOT NULL,
  `keyword`      VARCHAR(64) NOT NULL COMMENT '搜索关键词',
  `created_at`   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  
  KEY `idx_search_user` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户搜索历史';

-- [敏感词库表]
-- 用于内容安全过滤。用户发布内容时需匹配此表。
CREATE TABLE IF NOT EXISTS `sys_sensitive_word` (
  `id`           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `word`         VARCHAR(64) NOT NULL COMMENT '敏感词',
  `type`         VARCHAR(32) DEFAULT 'general' COMMENT '分类: 涉黄/涉政等',
  `created_at`   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词库';