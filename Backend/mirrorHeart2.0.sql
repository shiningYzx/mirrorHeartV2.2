-- drop database if exists mirrot_heart_2_0;
-- create database if not exists mirrot_heart_2_0 DEFAULT CHARSET utf8 COLLATE utf8_general_ci;
use mirrot_heart_2_0;


-- =========================================================
-- schema.sql  (MySQL 8.x)
-- UUID stored as CHAR(36)
-- charset: utf8mb4
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------
-- Drop tables (reverse-ish order)
-- -----------------------------
DROP TABLE IF EXISTS admin_audit_log;
DROP TABLE IF EXISTS admin_role_permission;
DROP TABLE IF EXISTS admin_user_role;
DROP TABLE IF EXISTS admin_permission;
DROP TABLE IF EXISTS admin_role;
DROP TABLE IF EXISTS admin_user;

DROP TABLE IF EXISTS counter_event;
DROP TABLE IF EXISTS stat_daily;

DROP TABLE IF EXISTS search_document;

DROP TABLE IF EXISTS daily_question_answer;
DROP TABLE IF EXISTS daily_question_pick;
DROP TABLE IF EXISTS daily_question;

DROP TABLE IF EXISTS comment;

DROP TABLE IF EXISTS post_view_log;
DROP TABLE IF EXISTS post_favorite;
DROP TABLE IF EXISTS post_audio;
DROP TABLE IF EXISTS post_image;
DROP TABLE IF EXISTS post;

DROP TABLE IF EXISTS like_action;

DROP TABLE IF EXISTS user_tag_pref;

DROP TABLE IF EXISTS daily_recommendation_item;
DROP TABLE IF EXISTS daily_recommendation;

DROP TABLE IF EXISTS recommend_music_lyric;
DROP TABLE IF EXISTS recommend_music;
DROP TABLE IF EXISTS recommend_painting;
DROP TABLE IF EXISTS recommend_movie;
DROP TABLE IF EXISTS recommend_book;
DROP TABLE IF EXISTS recommend_article;
DROP TABLE IF EXISTS recommend_sentence;
DROP TABLE IF EXISTS recommend_content;

DROP TABLE IF EXISTS tag_map;
DROP TABLE IF EXISTS tag;

DROP TABLE IF EXISTS notification;

DROP TABLE IF EXISTS user_block;
DROP TABLE IF EXISTS user_follow;

DROP TABLE IF EXISTS user_deletion_snapshot;
DROP TABLE IF EXISTS auth_login_log;
DROP TABLE IF EXISTS auth_session;
DROP TABLE IF EXISTS email_otp;
DROP TABLE IF EXISTS user;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 1) User & Auth
-- =========================================================

-- 用户 
CREATE TABLE user (
  id              CHAR(36) PRIMARY KEY,
  email           VARCHAR(255) NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,
  email_verified  TINYINT(1) NOT NULL DEFAULT 0,

  nickname        VARCHAR(64)  NOT NULL DEFAULT '',
  avatar_url      VARCHAR(512) NOT NULL DEFAULT '',
  bio             VARCHAR(256) NOT NULL DEFAULT '',

  status          TINYINT NOT NULL DEFAULT 1, -- 1正常 2封禁 3注销中 4已注销
  anonymized_at   DATETIME NULL,
  cooldown_until  DATETIME NULL,

  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted      TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at      DATETIME NULL,

  UNIQUE KEY uk_user_email (email),
  UNIQUE KEY uk_user_nickname (nickname),
  KEY idx_user_status (status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户删除快照
CREATE TABLE user_deletion_snapshot (
  id                 CHAR(36) PRIMARY KEY,
  user_id            CHAR(36) NOT NULL,
  original_email     VARCHAR(255) NOT NULL,
  original_nickname  VARCHAR(64)  NOT NULL,
  original_avatar_url VARCHAR(512) NOT NULL DEFAULT '',
  original_bio       VARCHAR(256) NOT NULL DEFAULT '',
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_snapshot_user (user_id),
  CONSTRAINT fk_snapshot_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--  电子邮件验证码
CREATE TABLE email_otp (
  id         CHAR(36) PRIMARY KEY,
  email      VARCHAR(255) NOT NULL,
  scene      VARCHAR(32)  NOT NULL, -- LOGIN/REGISTER/RESET_PASSWORD/VERIFY_EMAIL
  code_hash  VARCHAR(255) NOT NULL, -- 不存明文
  expires_at DATETIME NOT NULL,
  used_at    DATETIME NULL,
  ip         VARCHAR(64) NULL,
  ua         VARCHAR(256) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY idx_otp_email_scene_time (email, scene, created_at),
  KEY idx_otp_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 认证
CREATE TABLE auth_session (
  id                  CHAR(36) PRIMARY KEY,
  user_id             CHAR(36) NOT NULL,
  refresh_token_hash  VARCHAR(255) NOT NULL,
  device_id           VARCHAR(128) NULL,
  device_name         VARCHAR(128) NULL,
  ip                  VARCHAR(64) NULL,
  ua                  VARCHAR(256) NULL,
  issued_at           DATETIME NOT NULL,
  expires_at          DATETIME NOT NULL,
  revoked_at          DATETIME NULL,
  last_seen_at        DATETIME NULL,

  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted          TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at          DATETIME NULL,

  UNIQUE KEY uk_session_refresh_hash (refresh_token_hash),
  KEY idx_session_user (user_id, revoked_at, expires_at),
  KEY idx_session_expires (expires_at),
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 登录认证日志 
CREATE TABLE auth_login_log (
  id          CHAR(36) PRIMARY KEY,
  user_id     CHAR(36) NULL,
  email       VARCHAR(255) NOT NULL,
  result      TINYINT NOT NULL, -- 1成功 2失败
  fail_reason VARCHAR(64) NULL,
  ip          VARCHAR(64) NULL,
  ua          VARCHAR(256) NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY idx_login_email_time (email, created_at),
  KEY idx_login_user_time (user_id, created_at),
  CONSTRAINT fk_login_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 2) User relations: follow / block
-- =========================================================

-- 用户关注 
CREATE TABLE user_follow (
  id          CHAR(36) PRIMARY KEY,
  follower_id CHAR(36) NOT NULL,
  followee_id CHAR(36) NOT NULL,
  status      TINYINT NOT NULL DEFAULT 1, -- 1关注 2取消
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_follow_pair (follower_id, followee_id),
  KEY idx_follow_follower (follower_id, status),
  KEY idx_follow_followee (followee_id, status),
  CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES user(id),
  CONSTRAINT fk_follow_followee FOREIGN KEY (followee_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户屏蔽 
CREATE TABLE user_block (
  id         CHAR(36) PRIMARY KEY,
  blocker_id CHAR(36) NOT NULL,
  blocked_id CHAR(36) NOT NULL,
  status     TINYINT NOT NULL DEFAULT 1, -- 1屏蔽 2取消
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_block_pair (blocker_id, blocked_id),
  KEY idx_block_blocker (blocker_id, status),
  KEY idx_block_blocked (blocked_id, status),
  CONSTRAINT fk_block_blocker FOREIGN KEY (blocker_id) REFERENCES user(id),
  CONSTRAINT fk_block_blocked FOREIGN KEY (blocked_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 3) Notification (reserved)
-- =========================================================

CREATE TABLE notification (
  id          CHAR(36) PRIMARY KEY,
  user_id     CHAR(36) NOT NULL,      -- 被通知的人
  type        VARCHAR(32) NOT NULL,   -- LIKE_POST/COMMENT_POST/REPLY_COMMENT/FOLLOW/...
  actor_id    CHAR(36) NULL,          -- 触发者
  target_type VARCHAR(32) NOT NULL,   -- POST/COMMENT/RECOMMEND_CONTENT/DAILY_QUESTION_ANSWER/...
  target_id   CHAR(36) NOT NULL,
  payload_json JSON NULL,
  read_at     DATETIME NULL,

  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at  DATETIME NULL,

  KEY idx_notif_user_read (user_id, read_at, created_at),
  KEY idx_notif_target (target_type, target_id),
  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES user(id),
  CONSTRAINT fk_notif_actor FOREIGN KEY (actor_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 4) Tag system (shared)
-- =========================================================
-- 通用标签 
CREATE TABLE tag (
  id         CHAR(36) PRIMARY KEY,
  name       VARCHAR(64) NOT NULL,
  type       VARCHAR(32) NOT NULL DEFAULT 'GENERAL', -- TOPIC/PSYCHO/...
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME NULL,

  UNIQUE KEY uk_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- polymorphic mapping (no FK to target)
CREATE TABLE tag_map (
  id          CHAR(36) PRIMARY KEY,
  tag_id      CHAR(36) NOT NULL,
  target_type VARCHAR(32) NOT NULL, -- POST/RECOMMEND_CONTENT/DAILY_QUESTION/...
  target_id   CHAR(36) NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_tagmap (tag_id, target_type, target_id),
  KEY idx_tagmap_target (target_type, target_id),
  KEY idx_tagmap_tag (tag_id),
  CONSTRAINT fk_tagmap_tag FOREIGN KEY (tag_id) REFERENCES tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 5) Recommend content (base + detail tables)
-- =========================================================

CREATE TABLE recommend_content (
  id           CHAR(36) PRIMARY KEY,
  content_type TINYINT NOT NULL, -- 1句 2文 3书 4影 5画 6乐

  title        VARCHAR(255) NOT NULL DEFAULT '',
  cover_url    VARCHAR(512) NOT NULL DEFAULT '',
  summary      VARCHAR(512) NOT NULL DEFAULT '',
  author_text  VARCHAR(255) NOT NULL DEFAULT '',
  external_url VARCHAR(512) NOT NULL DEFAULT '',

  lang         VARCHAR(16)  NOT NULL DEFAULT 'zh',
  status       TINYINT NOT NULL DEFAULT 1, -- 1上架 2下架

  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted   TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at   DATETIME NULL,

  KEY idx_rc_type_status (content_type, status, is_deleted),
  FULLTEXT KEY ft_rc (title, summary, author_text)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Quote
-- 句子
-- 重命名单个表
-- RENAME TABLE old_table_name TO new_table_name;
-- RENAME TABLE recommend_quote TO recommend_sentence;
 
CREATE TABLE recommend_sentence (
  content_id CHAR(36) PRIMARY KEY,
  text       TEXT NOT NULL,
  author     VARCHAR(255) NOT NULL DEFAULT '',
  CONSTRAINT fk_quote_content FOREIGN KEY (content_id) REFERENCES recommend_content(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Article
CREATE TABLE recommend_article (
  content_id CHAR(36) PRIMARY KEY,
  title      VARCHAR(255) NOT NULL,
  abstract   VARCHAR(1024) NOT NULL DEFAULT '',
  body       MEDIUMTEXT NOT NULL,
  author     VARCHAR(255) NOT NULL DEFAULT '',
  CONSTRAINT fk_article_content FOREIGN KEY (content_id) REFERENCES recommend_content(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Book
CREATE TABLE recommend_book (
  content_id   CHAR(36) PRIMARY KEY,
  book_name    VARCHAR(255) NOT NULL,
  author       VARCHAR(255) NOT NULL DEFAULT '',
  cover_url    VARCHAR(512) NOT NULL DEFAULT '',
  intro        TEXT NOT NULL,
  external_url VARCHAR(512) NOT NULL DEFAULT '',
  CONSTRAINT fk_book_content FOREIGN KEY (content_id) REFERENCES recommend_content(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Movie
CREATE TABLE recommend_movie (
  content_id   CHAR(36) PRIMARY KEY,
  movie_name   VARCHAR(255) NOT NULL,
  director     VARCHAR(255) NOT NULL DEFAULT '',
  year         INT NOT NULL DEFAULT 0,
  poster_url   VARCHAR(512) NOT NULL DEFAULT '',
  intro        TEXT NOT NULL,
  rating       DECIMAL(3,1) NOT NULL DEFAULT 0.0,
  external_url VARCHAR(512) NOT NULL DEFAULT '',
  CONSTRAINT fk_movie_content FOREIGN KEY (content_id) REFERENCES recommend_content(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Painting
CREATE TABLE recommend_painting (
  content_id      CHAR(36) PRIMARY KEY,
  painting_title  VARCHAR(255) NOT NULL,
  author          VARCHAR(255) NOT NULL DEFAULT '',
  year            INT NOT NULL DEFAULT 0,
  image_url       VARCHAR(512) NOT NULL DEFAULT '',
  description     TEXT NOT NULL,
  CONSTRAINT fk_painting_content FOREIGN KEY (content_id) REFERENCES recommend_content(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Music
CREATE TABLE recommend_music (
  content_id  CHAR(36) PRIMARY KEY,
  song_name   VARCHAR(255) NOT NULL,
  singer      VARCHAR(255) NOT NULL DEFAULT '',
  cover_url   VARCHAR(512) NOT NULL DEFAULT '',
  audio_url   VARCHAR(512) NOT NULL DEFAULT '',
  CONSTRAINT fk_music_content FOREIGN KEY (content_id) REFERENCES recommend_content(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 歌词 
CREATE TABLE recommend_music_lyric (
  id        CHAR(36) PRIMARY KEY,
  content_id CHAR(36) NOT NULL,
  ts_ms     INT NOT NULL,            -- 毫秒
  line      VARCHAR(512) NOT NULL,
  line_order INT NOT NULL,

  UNIQUE KEY uk_lyric_order (content_id, line_order),
  KEY idx_lyric_content_ts (content_id, ts_ms),
  CONSTRAINT fk_lyric_music FOREIGN KEY (content_id) REFERENCES recommend_music(content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 6) Personal daily recommendation (archive by date)
-- =========================================================
-- 每日推荐
CREATE TABLE daily_recommendation (
  id             CHAR(36) PRIMARY KEY,
  user_id        CHAR(36) NOT NULL,
  recommend_date DATE NOT NULL, -- 北京时间
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_dr_user_date (user_id, recommend_date),
  KEY idx_dr_user_date (user_id, recommend_date),
  CONSTRAINT fk_dr_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE daily_recommendation_item (
  id           CHAR(36) PRIMARY KEY,
  daily_id     CHAR(36) NOT NULL,
  content_type TINYINT NOT NULL, -- 1~6
  content_id   CHAR(36) NOT NULL,
  rank_no      INT NOT NULL DEFAULT 1,
  reason       VARCHAR(256) NOT NULL DEFAULT '',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_dri_daily_type (daily_id, content_type),
  KEY idx_dri_daily (daily_id),
  KEY idx_dri_content (content_id),
  CONSTRAINT fk_dri_daily FOREIGN KEY (daily_id) REFERENCES daily_recommendation(id),
  CONSTRAINT fk_dri_content FOREIGN KEY (content_id) REFERENCES recommend_content(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user tag preference for simple recommendation v1
-- 用户偏好标签
CREATE TABLE user_tag_pref (
  id         CHAR(36) PRIMARY KEY,
  user_id    CHAR(36) NOT NULL,
  tag_id     CHAR(36) NOT NULL,
  score      INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_utp_user_tag (user_id, tag_id),
  KEY idx_utp_user_score (user_id, score),
  CONSTRAINT fk_utp_user FOREIGN KEY (user_id) REFERENCES user(id),
  CONSTRAINT fk_utp_tag  FOREIGN KEY (tag_id)  REFERENCES tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 7) Like (unified)
-- =========================================================

CREATE TABLE like_action (
  id          CHAR(36) PRIMARY KEY,
  user_id     CHAR(36) NOT NULL,
  target_type VARCHAR(32) NOT NULL, -- POST/COMMENT/RECOMMEND_CONTENT/DAILY_RECOMMEND_ITEM/DAILY_QUESTION_ANSWER
  target_id   CHAR(36) NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_like (user_id, target_type, target_id),
  KEY idx_like_target (target_type, target_id, created_at),
  KEY idx_like_user (user_id, created_at),
  CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 8) Post (voice community): post + images + audio + favorite + view log
-- =========================================================
-- 帖子
CREATE TABLE post (
  id             CHAR(36) PRIMARY KEY,
  user_id        CHAR(36) NOT NULL,
  text           TEXT NOT NULL,
  visibility     TINYINT NOT NULL, -- 1公开 2仅粉丝 3仅自己
  status         TINYINT NOT NULL DEFAULT 1, -- 1正常 2隐藏(预留)

  like_count     INT NOT NULL DEFAULT 0,
  comment_count  INT NOT NULL DEFAULT 0,
  favorite_count INT NOT NULL DEFAULT 0,
  pv             BIGINT NOT NULL DEFAULT 0,
  uv             BIGINT NOT NULL DEFAULT 0,

  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at     DATETIME NULL,

  KEY idx_post_user_time (user_id, created_at),
  KEY idx_post_visibility_time (visibility, created_at),
  FULLTEXT KEY ft_post (text),
  CONSTRAINT fk_post_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 帖子图片
CREATE TABLE post_image (
  id      CHAR(36) PRIMARY KEY,
  post_id CHAR(36) NOT NULL,
  url     VARCHAR(512) NOT NULL,
  width   INT NOT NULL DEFAULT 0,
  height  INT NOT NULL DEFAULT 0,
  sort_no INT NOT NULL,

  UNIQUE KEY uk_postimg (post_id, sort_no),
  KEY idx_postimg_post (post_id),
  CONSTRAINT fk_postimg_post FOREIGN KEY (post_id) REFERENCES post(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 帖子语音
CREATE TABLE post_audio (
  id          CHAR(36) PRIMARY KEY,
  post_id     CHAR(36) NOT NULL,
  url         VARCHAR(512) NOT NULL,
  duration_ms INT NOT NULL DEFAULT 0,
  codec       VARCHAR(32) NOT NULL DEFAULT '',
  size_bytes  BIGINT NOT NULL DEFAULT 0,

  UNIQUE KEY uk_postaudio_post (post_id),
  CONSTRAINT fk_postaudio_post FOREIGN KEY (post_id) REFERENCES post(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 帖子点赞
CREATE TABLE post_favorite (
  id         CHAR(36) PRIMARY KEY,
  user_id    CHAR(36) NOT NULL,
  post_id    CHAR(36) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_fav (user_id, post_id),
  KEY idx_fav_post (post_id, created_at),
  CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES user(id),
  CONSTRAINT fk_fav_post FOREIGN KEY (post_id) REFERENCES post(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- View log reserved for UV (daily distinct), counters aggregated async to post.uv/post.pv
CREATE TABLE post_view_log (
  id         CHAR(36) PRIMARY KEY,
  post_id    CHAR(36) NOT NULL,
  user_id    CHAR(36) NULL,
  anon_id    VARCHAR(64) NULL,
  view_date  DATE NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY idx_view_post_date (post_id, view_date),
  KEY idx_view_user_date (user_id, view_date),
  KEY idx_view_anon_date (anon_id, view_date),
  CONSTRAINT fk_view_post FOREIGN KEY (post_id) REFERENCES post(id),
  CONSTRAINT fk_view_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 9) Comment (2-level thread)
-- =========================================================
-- 评论
CREATE TABLE comment (
  id         CHAR(36) PRIMARY KEY,
  post_id    CHAR(36) NOT NULL,
  user_id    CHAR(36) NOT NULL,

  parent_id  CHAR(36) NULL,   -- depth=1 必填
  root_id    CHAR(36) NULL,   -- depth=1 指向一级评论
  depth      TINYINT NOT NULL DEFAULT 0, -- 0一级 1回复

  content    TEXT NOT NULL,
  status     TINYINT NOT NULL DEFAULT 1, -- 预留隐藏
  like_count INT NOT NULL DEFAULT 0,

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME NULL,

  KEY idx_comment_post_time (post_id, created_at),
  KEY idx_comment_post_root (post_id, root_id, created_at),
  KEY idx_comment_parent (parent_id, created_at),

  CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES post(id),
  CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 10) Daily question (bank + pick + answer)
-- =========================================================
-- 每日一问
CREATE TABLE daily_question (
  id         CHAR(36) PRIMARY KEY,
  title      VARCHAR(255) NOT NULL,      -- 问题文本
  category   VARCHAR(64)  NOT NULL DEFAULT '',
  status     TINYINT NOT NULL DEFAULT 1, -- 1上架 2下架
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at DATETIME NULL,

  FULLTEXT KEY ft_dq (title),
  KEY idx_dq_status (status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- one pick per user per day (calendar + history)
CREATE TABLE daily_question_pick (
  id          CHAR(36) PRIMARY KEY,
  user_id     CHAR(36) NOT NULL,
  pick_date   DATE NOT NULL,
  question_id CHAR(36) NOT NULL,
  algo_version VARCHAR(32) NOT NULL DEFAULT 'v1',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_dqp_user_date (user_id, pick_date),
  KEY idx_dqp_user_date (user_id, pick_date),
  KEY idx_dqp_question (question_id),

  CONSTRAINT fk_dqp_user FOREIGN KEY (user_id) REFERENCES user(id),
  CONSTRAINT fk_dqp_question FOREIGN KEY (question_id) REFERENCES daily_question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- one answer per user per day (default private)
CREATE TABLE daily_question_answer (
  id          CHAR(36) PRIMARY KEY,
  user_id     CHAR(36) NOT NULL,
  pick_id     CHAR(36) NOT NULL,
  question_id CHAR(36) NOT NULL,
  answer_date DATE NOT NULL,

  audio_url   VARCHAR(512) NOT NULL,
  duration_ms INT NOT NULL DEFAULT 0,
  visibility  TINYINT NOT NULL DEFAULT 3, -- 1公开 2仅粉丝 3仅自己
  like_count  INT NOT NULL DEFAULT 0,
  featured    TINYINT(1) NOT NULL DEFAULT 0,

  post_id     CHAR(36) NULL, -- 预留：回答发布为帖子后关联

  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at  DATETIME NULL,

  UNIQUE KEY uk_dqa_user_date (user_id, answer_date),
  UNIQUE KEY uk_dqa_pick (pick_id),
  KEY idx_dqa_question_date (question_id, answer_date),
  KEY idx_dqa_visibility (visibility, created_at),

  CONSTRAINT fk_dqa_user FOREIGN KEY (user_id) REFERENCES user(id),
  CONSTRAINT fk_dqa_pick FOREIGN KEY (pick_id) REFERENCES daily_question_pick(id),
  CONSTRAINT fk_dqa_question FOREIGN KEY (question_id) REFERENCES daily_question(id),
  CONSTRAINT fk_dqa_post FOREIGN KEY (post_id) REFERENCES post(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 11) Search (MySQL FULLTEXT now, ES later)
-- =========================================================

CREATE TABLE search_document (
  id          CHAR(36) PRIMARY KEY,
  target_type VARCHAR(32) NOT NULL, -- POST/RECOMMEND_CONTENT/DAILY_QUESTION
  target_id   CHAR(36) NOT NULL,
  title       VARCHAR(255) NOT NULL DEFAULT '',
  content     MEDIUMTEXT NOT NULL,
  extra_json  JSON NULL,
  status      TINYINT NOT NULL DEFAULT 1,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_search_target (target_type, target_id),
  FULLTEXT KEY ft_search (title, content),
  KEY idx_search_type_status (target_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 12) Stats + Async counters (reserved)
-- =========================================================

CREATE TABLE stat_daily (
  id               CHAR(36) PRIMARY KEY,
  stat_date        DATE NOT NULL,
  new_user_count   INT NOT NULL DEFAULT 0,
  active_user_count INT NOT NULL DEFAULT 0,
  post_count       INT NOT NULL DEFAULT 0,
  comment_count    INT NOT NULL DEFAULT 0,
  like_count       INT NOT NULL DEFAULT 0,
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Counter events for async aggregation (recommended)
CREATE TABLE counter_event (
  id          CHAR(36) PRIMARY KEY,
  target_type VARCHAR(32) NOT NULL, -- POST/COMMENT/RECOMMEND_CONTENT/DAILY_QUESTION_ANSWER/...
  target_id   CHAR(36) NOT NULL,
  event_type  VARCHAR(32) NOT NULL, -- LIKE_ADD/LIKE_DEL/COMMENT_ADD/FAV_ADD/VIEW_PV/VIEW_UV/...
  delta       INT NOT NULL DEFAULT 1,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY idx_ce_target_time (target_type, target_id, created_at),
  KEY idx_ce_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 13) Admin platform (RBAC + audit)
-- =========================================================
-- 管理员
CREATE TABLE admin_user (
  id            CHAR(36) PRIMARY KEY,
  email         VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status        TINYINT NOT NULL DEFAULT 1, -- 1正常 2禁用
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_admin_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_role (
  id          CHAR(36) PRIMARY KEY,
  name        VARCHAR(64) NOT NULL,
  description VARCHAR(255) NOT NULL DEFAULT '',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_admin_role_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_permission (
  id          CHAR(36) PRIMARY KEY,
  code        VARCHAR(64) NOT NULL, -- e.g. CONTENT_EDIT/USER_BAN/RECOMMEND_PUBLISH
  description VARCHAR(255) NOT NULL DEFAULT '',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_admin_perm_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_user_role (
  id        CHAR(36) PRIMARY KEY,
  admin_id  CHAR(36) NOT NULL,
  role_id   CHAR(36) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_admin_user_role (admin_id, role_id),
  KEY idx_admin_user (admin_id),
  KEY idx_admin_role (role_id),
  CONSTRAINT fk_aur_admin FOREIGN KEY (admin_id) REFERENCES admin_user(id),
  CONSTRAINT fk_aur_role  FOREIGN KEY (role_id)  REFERENCES admin_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_role_permission (
  id           CHAR(36) PRIMARY KEY,
  role_id      CHAR(36) NOT NULL,
  permission_id CHAR(36) NOT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_role_perm (role_id, permission_id),
  KEY idx_arp_role (role_id),
  KEY idx_arp_perm (permission_id),
  CONSTRAINT fk_arp_role FOREIGN KEY (role_id) REFERENCES admin_role(id),
  CONSTRAINT fk_arp_perm FOREIGN KEY (permission_id) REFERENCES admin_permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_audit_log (
  id          CHAR(36) PRIMARY KEY,
  admin_id    CHAR(36) NOT NULL,
  action      VARCHAR(64) NOT NULL,  -- e.g. UPDATE_CONTENT/DELETE_POST/BAN_USER
  target_type VARCHAR(32) NULL,
  target_id   CHAR(36) NULL,
  detail_json JSON NULL,
  ip          VARCHAR(64) NULL,
  ua          VARCHAR(256) NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY idx_audit_admin_time (admin_id, created_at),
  KEY idx_audit_target (target_type, target_id),
  CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id) REFERENCES admin_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


SELECT
  p.id,
  p.user_id,
  p.text,
  p.visibility,
  p.like_count,
  p.comment_count,
  p.favorite_count,
  p.pv,
  p.uv,
  p.created_at,
  p.updated_at,

  u.nickname AS author_nickname,
  u.avatar_url AS author_avatar_url,
  u.bio AS author_bio,

  CASE WHEN la.id IS NULL THEN 0 ELSE 1 END AS liked_by_viewer,
  CASE WHEN pf.id IS NULL THEN 0 ELSE 1 END AS favorited_by_viewer


-- 帖子 Offset 分页 
SET @viewer_id = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee';

SELECT
  p.id,
  p.user_id,
  p.text,
  p.visibility,
  p.like_count,
  p.comment_count,
  p.favorite_count,
  p.pv,
  p.uv,
  p.created_at,
  p.updated_at,

  u.nickname AS author_nickname,
  u.avatar_url AS author_avatar_url,
  u.bio AS author_bio,

  CASE WHEN la.id IS NULL THEN 0 ELSE 1 END AS liked_by_viewer,
  CASE WHEN pf.id IS NULL THEN 0 ELSE 1 END AS favorited_by_viewer
FROM post p
JOIN user u
  ON u.id = p.user_id
 AND u.is_deleted = 0
LEFT JOIN user_block b1
  ON b1.status = 1
 AND b1.blocker_id = @viewer_id
 AND b1.blocked_id = p.user_id
LEFT JOIN user_block b2
  ON b2.status = 1
 AND b2.blocker_id = p.user_id
 AND b2.blocked_id = @viewer_id
LEFT JOIN user_follow f
  ON f.status = 1
 AND f.follower_id = @viewer_id
 AND f.followee_id = p.user_id
LEFT JOIN like_action la
  ON la.user_id = @viewer_id
 AND la.target_type = 'POST'
 AND la.target_id = p.id
LEFT JOIN post_favorite pf
  ON pf.user_id = @viewer_id
 AND pf.post_id = p.id
WHERE
  p.is_deleted = 0
  AND p.status = 1
  AND b1.id IS NULL
  AND b2.id IS NULL
  AND (
       p.visibility = 1
    OR (p.visibility = 2 AND f.id IS NOT NULL)
    OR (p.visibility = 3 AND p.user_id = @viewer_id)
  )
ORDER BY p.created_at DESC, p.id DESC
LIMIT 0, 20;



-- 帖子游标分页更稳
ALTER TABLE post ADD KEY idx_post_created_id (created_at, id);

-- 黑名单和关注 join 已经有索引；like_action / favorite 也有 unique，可用
-- 若 like_action 未来大，可以加：
ALTER TABLE like_action ADD KEY idx_like_user_type_target (user_id, target_type, target_id);




-- =========================================================
-- END
-- =========================================================
