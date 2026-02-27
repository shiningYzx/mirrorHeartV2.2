-- =========================
-- 0) Database
-- =========================
CREATE DATABASE IF NOT EXISTS mirrot_heart_2_1
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE mirrot_heart_2_1;

-- =========================
-- 1) User & Auth
-- =========================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id`            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `email`         VARCHAR(255) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `nickname`      VARCHAR(64)  NOT NULL DEFAULT '',
  `avatar_url`    VARCHAR(512) NOT NULL DEFAULT '',
  `bio`           VARCHAR(255) NOT NULL DEFAULT '' COMMENT '个人简介/签名',
  `role`          TINYINT NOT NULL DEFAULT 0 COMMENT '0=user,1=admin',
  `status`        TINYINT NOT NULL DEFAULT 1 COMMENT '1=active,0=disabled,2=deleting',
  `is_deleted`    TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记（一般不用，留作兼容）',
  `created_at`    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_status` (`status`, `is_deleted`),
  KEY `idx_user_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 邮箱验证码（OTP）
CREATE TABLE IF NOT EXISTS `email_otp` (
  `id`           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `email`        VARCHAR(255) NOT NULL,
  `scene`        VARCHAR(32)  NOT NULL COMMENT 'register/login/reset',
  `code`         VARCHAR(16)  NOT NULL,
  `expire_at`    DATETIME(3)  NOT NULL,
  `used_at`      DATETIME(3)  NULL,
  `send_count`   INT NOT NULL DEFAULT 1 COMMENT '当天累计次数（也可迁移到redis）',
  `last_sent_at` DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  KEY `idx_otp_email_scene` (`email`, `scene`, `created_at`),
  KEY `idx_otp_expire` (`expire_at`),
  KEY `idx_otp_used` (`used_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 会话表（用于注销/踢下线/多端会话管理）
CREATE TABLE IF NOT EXISTS `auth_session` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `jti`        VARCHAR(64) NOT NULL COMMENT 'jwt id',
  `device`     VARCHAR(64) NOT NULL DEFAULT '',
  `ip`         VARCHAR(64) NOT NULL DEFAULT '',
  `user_agent` VARCHAR(255) NOT NULL DEFAULT '',
  `expired_at` DATETIME(3) NOT NULL,
  `revoked_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_session_jti` (`jti`),
  KEY `idx_session_user` (`user_id`, `created_at`),
  KEY `idx_session_expired` (`expired_at`),

  CONSTRAINT `fk_session_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 删除账号申请（冷静期）
CREATE TABLE IF NOT EXISTS `account_deletion_request` (
  `id`            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`       BIGINT UNSIGNED NOT NULL,
  `requested_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `execute_after` DATETIME(3) NOT NULL COMMENT '冷静期结束时间（到点可执行）',
  `canceled_at`   DATETIME(3) NULL,
  `executed_at`   DATETIME(3) NULL,
  `reason`        VARCHAR(255) NOT NULL DEFAULT '',

  UNIQUE KEY `uk_del_user` (`user_id`),
  KEY `idx_del_execute` (`execute_after`, `executed_at`),

  CONSTRAINT `fk_del_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================
-- 2) Content Library (quote/article/book/painting/music/movie)
-- =========================

CREATE TABLE IF NOT EXISTS `content` (
  `id`          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `type`        VARCHAR(16) NOT NULL COMMENT 'QUOTE/ARTICLE/BOOK/PAINTING/MUSIC/MOVIE',
  `title`       VARCHAR(255) NOT NULL DEFAULT '',
  `summary`     VARCHAR(512) NOT NULL DEFAULT '',
  `cover_url`   VARCHAR(512) NOT NULL DEFAULT '',
  `source`      VARCHAR(255) NOT NULL DEFAULT '',
  `tags`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '逗号分隔或json字符串',
  `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '1=published,0=draft',
  `like_count`  INT NOT NULL DEFAULT 0 COMMENT '内容被点赞次数（支持点赞句/文/书/画/乐/影）',
  `is_deleted`  TINYINT NOT NULL DEFAULT 0,

  `created_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

  KEY `idx_content_type` (`type`, `status`, `is_deleted`),
  KEY `idx_content_like` (`like_count`, `created_at`),
  KEY `idx_content_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `content_quote` (
  `content_id` BIGINT UNSIGNED PRIMARY KEY,
  `text`       VARCHAR(1024) NOT NULL,
  `author`     VARCHAR(255)  NOT NULL DEFAULT '',

  CONSTRAINT `fk_quote_content`
    FOREIGN KEY (`content_id`) REFERENCES `content`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `content_article` (
  `content_id` BIGINT UNSIGNED PRIMARY KEY,
  `body`       MEDIUMTEXT NOT NULL,
  `author`     VARCHAR(255) NOT NULL DEFAULT '',

  CONSTRAINT `fk_article_content`
    FOREIGN KEY (`content_id`) REFERENCES `content`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `content_book` (
  `content_id` BIGINT UNSIGNED PRIMARY KEY,
  `isbn`       VARCHAR(32)  NOT NULL DEFAULT '',
  `author`     VARCHAR(255) NOT NULL DEFAULT '',
  `publisher`  VARCHAR(255) NOT NULL DEFAULT '',

  CONSTRAINT `fk_book_content`
    FOREIGN KEY (`content_id`) REFERENCES `content`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `content_painting` (
  `content_id` BIGINT UNSIGNED PRIMARY KEY,
  `painter`    VARCHAR(255) NOT NULL DEFAULT '',
  `year`       VARCHAR(16)  NOT NULL DEFAULT '',

  CONSTRAINT `fk_painting_content`
    FOREIGN KEY (`content_id`) REFERENCES `content`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `content_music` (
  `content_id`  BIGINT UNSIGNED PRIMARY KEY,
  `artist`      VARCHAR(255) NOT NULL DEFAULT '',
  `audio_url`   VARCHAR(512) NOT NULL DEFAULT '',
  `duration_ms` INT NOT NULL DEFAULT 0,
  `lyric`       MEDIUMTEXT NULL,

  CONSTRAINT `fk_music_content`
    FOREIGN KEY (`content_id`) REFERENCES `content`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `content_movie` (
  `content_id` BIGINT UNSIGNED PRIMARY KEY,
  `director`   VARCHAR(255) NOT NULL DEFAULT '',
  `year`       VARCHAR(16)  NOT NULL DEFAULT '',

  CONSTRAINT `fk_movie_content`
    FOREIGN KEY (`content_id`) REFERENCES `content`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================
-- 3) Daily Recommendation (history by day)
-- =========================

CREATE TABLE IF NOT EXISTS `daily_recommendation` (
  `id`           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,

  `user_id`      BIGINT UNSIGNED NOT NULL,
  `day`          DATE NOT NULL,

  `strategy`     VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT '推荐策略/版本，用于回溯与AB',
  `generated_by` VARCHAR(32) NOT NULL DEFAULT 'system' COMMENT 'system/admin/abtest',
  `generated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '服务端生成时间',
  `viewed_at`    DATETIME(3) NULL COMMENT '用户首次打开可见时间',

  `created_at`   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_user_day` (`user_id`, `day`),
  KEY `idx_user_day` (`user_id`, `day`),
  KEY `idx_day` (`day`),
  KEY `idx_user_viewed` (`user_id`, `viewed_at`),

  CONSTRAINT `fk_daily_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `daily_recommendation_item` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,

  `daily_id`   BIGINT UNSIGNED NOT NULL,
  `content_id` BIGINT UNSIGNED NOT NULL,

  `type`       VARCHAR(16) NOT NULL COMMENT 'QUOTE/ARTICLE/BOOK/PAINTING/MUSIC/MOVIE',
  `rank`       INT NOT NULL DEFAULT 1 COMMENT '同类型内排序：1..n',

  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_daily_type_rank` (`daily_id`, `type`, `rank`),
  KEY `idx_item_daily` (`daily_id`),
  KEY `idx_item_content` (`content_id`),

  CONSTRAINT `fk_item_daily`
    FOREIGN KEY (`daily_id`) REFERENCES `daily_recommendation`(`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_item_content`
    FOREIGN KEY (`content_id`) REFERENCES `content`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================
-- 4) Daily Question & Audio Answer
-- =========================

CREATE TABLE IF NOT EXISTS `daily_question` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `day`        DATE NOT NULL,
  `question`   VARCHAR(1024) NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_question_day` (`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `daily_answer` (
  `id`          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `question_id` BIGINT UNSIGNED NOT NULL,

  `audio_url`   VARCHAR(512) NOT NULL,
  `duration_ms` INT NOT NULL DEFAULT 0,
  `codec`       VARCHAR(32) NOT NULL DEFAULT '' COMMENT 'aac/mp3/wav',
  `size_bytes`  BIGINT UNSIGNED NOT NULL DEFAULT 0,

  `created_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_answer_user_question` (`user_id`, `question_id`),
  KEY `idx_answer_question` (`question_id`, `created_at`),

  CONSTRAINT `fk_answer_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_answer_question`
    FOREIGN KEY (`question_id`) REFERENCES `daily_question`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================
-- 5) Community (post/images/audio/comments/like/favorite)
-- =========================

CREATE TABLE IF NOT EXISTS `post` (
  `id`             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`        BIGINT UNSIGNED NOT NULL,
  `title`          VARCHAR(255) NOT NULL DEFAULT '',
  `text`           MEDIUMTEXT NULL,

  `visibility`     TINYINT NOT NULL DEFAULT 0 COMMENT '0=public,1=followers,2=private',

  `like_count`     INT NOT NULL DEFAULT 0,
  `comment_count`  INT NOT NULL DEFAULT 0,
  `favorite_count` INT NOT NULL DEFAULT 0,

  `pv`             INT NOT NULL DEFAULT 0 COMMENT '观看量/浏览量（view count）',
  `uv`             INT NOT NULL DEFAULT 0 COMMENT '独立访客量（unique visitors，建议redis去重）',

  `raw_heat`       BIGINT NOT NULL DEFAULT 0 COMMENT '权重热度（不含时间衰减）',
  `heat`           BIGINT NOT NULL DEFAULT 0 COMMENT '最终热度（含时间衰减后落库）',

  `is_deleted`     TINYINT NOT NULL DEFAULT 0,
  `created_at`     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

  KEY `idx_post_user` (`user_id`, `created_at`),
  KEY `idx_post_heat` (`heat`, `created_at`),
  KEY `idx_post_created` (`created_at`),

  CONSTRAINT `fk_post_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `post_image` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `url`        VARCHAR(512) NOT NULL,
  `sort`       INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  KEY `idx_post_image_post` (`post_id`, `sort`),

  CONSTRAINT `fk_post_image_post`
    FOREIGN KEY (`post_id`) REFERENCES `post`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `post_audio` (
  `id`          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `post_id`     BIGINT UNSIGNED NOT NULL,
  `url`         VARCHAR(512) NOT NULL,
  `duration_ms` INT NOT NULL DEFAULT 0,
  `codec`       VARCHAR(32) NOT NULL DEFAULT '',
  `size_bytes`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `created_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_post_audio_post` (`post_id`),

  CONSTRAINT `fk_post_audio_post`
    FOREIGN KEY (`post_id`) REFERENCES `post`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `comment` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `text`       VARCHAR(1024) NOT NULL,

  `parent_id`  BIGINT UNSIGNED NULL,
  `root_id`    BIGINT UNSIGNED NULL,

  `like_count` INT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  KEY `idx_comment_post` (`post_id`, `created_at`),
  KEY `idx_comment_root` (`root_id`, `created_at`),

  CONSTRAINT `fk_comment_post`
    FOREIGN KEY (`post_id`) REFERENCES `post`(`id`),
  CONSTRAINT `fk_comment_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 收藏：只收藏 post（如需收藏 content 可另加 content_favorite_action）
CREATE TABLE IF NOT EXISTS `favorite_action` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_fav_user_post` (`user_id`, `post_id`),
  KEY `idx_fav_post` (`post_id`, `created_at`),

  CONSTRAINT `fk_fav_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_fav_post`
    FOREIGN KEY (`post_id`) REFERENCES `post`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通用点赞：post/comment/content
CREATE TABLE IF NOT EXISTS `like_action` (
  `id`          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `target_type` VARCHAR(16) NOT NULL COMMENT 'post/comment/content',
  `target_id`   BIGINT UNSIGNED NOT NULL,
  `created_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_like_user_target` (`user_id`, `target_type`, `target_id`),
  KEY `idx_like_target` (`target_type`, `target_id`, `created_at`),

  CONSTRAINT `fk_like_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),

  CONSTRAINT `ck_like_target_type`
    CHECK (`target_type` IN ('post','comment','content'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================
-- 6) AI Chat
-- =========================

CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `title`      VARCHAR(255) NOT NULL DEFAULT '',
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

  KEY `idx_ai_session_user` (`user_id`, `updated_at`),

  CONSTRAINT `fk_ai_session_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `session_id` BIGINT UNSIGNED NOT NULL,
  `role`       VARCHAR(16) NOT NULL COMMENT 'user/assistant/system',
  `content`    MEDIUMTEXT NOT NULL,
  `token_usage` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  KEY `idx_ai_msg_session` (`session_id`, `created_at`),

  CONSTRAINT `fk_ai_msg_session`
    FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================
-- 7) Todo
-- =========================

CREATE TABLE IF NOT EXISTS `todo_item` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `title`      VARCHAR(255) NOT NULL,
  `note`       VARCHAR(1024) NOT NULL DEFAULT '',
  `due_at`     DATETIME(3) NULL,
  `status`     TINYINT NOT NULL DEFAULT 0 COMMENT '0=todo,1=done',
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

  KEY `idx_todo_user` (`user_id`, `status`, `due_at`),

  CONSTRAINT `fk_todo_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================
-- 8) Relation (follow/block)
-- =========================

CREATE TABLE IF NOT EXISTS `user_follow` (
  `id`          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `follower_id` BIGINT UNSIGNED NOT NULL,
  `followee_id` BIGINT UNSIGNED NOT NULL,
  `created_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_follow_pair` (`follower_id`, `followee_id`),
  KEY `idx_follow_follower` (`follower_id`, `created_at`),
  KEY `idx_follow_followee` (`followee_id`, `created_at`),

  CONSTRAINT `fk_follow_follower`
    FOREIGN KEY (`follower_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_follow_followee`
    FOREIGN KEY (`followee_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_block` (
  `id`         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `blocker_id` BIGINT UNSIGNED NOT NULL,
  `blocked_id` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  UNIQUE KEY `uk_block_pair` (`blocker_id`, `blocked_id`),
  KEY `idx_block_blocker` (`blocker_id`, `created_at`),
  KEY `idx_block_blocked` (`blocked_id`, `created_at`),

  CONSTRAINT `fk_block_blocker`
    FOREIGN KEY (`blocker_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_block_blocked`
    FOREIGN KEY (`blocked_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
