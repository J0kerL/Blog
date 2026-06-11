-- ============================================================
-- 博客系统数据库 Schema（MySQL 8）
-- ============================================================

CREATE DATABASE IF NOT EXISTS blog_new DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE blog_new;

-- ============================================================
-- 用户表：存储注册用户及管理员信息
-- ============================================================
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID，自增主键',
    username    VARCHAR(50)  NOT NULL UNIQUE             COMMENT '登录用户名，唯一',
    password    VARCHAR(128) NOT NULL                    COMMENT '登录密码，MD5 加密存储',
    nickname    VARCHAR(50)  DEFAULT ''                  COMMENT '用户昵称，用于前台展示',
    email       VARCHAR(100) DEFAULT ''                  COMMENT '电子邮箱',
    avatar      VARCHAR(500) DEFAULT ''                  COMMENT '头像 URL 地址',
    bio         VARCHAR(500) DEFAULT ''                  COMMENT '个人简介',
    role        VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER' COMMENT '角色标识：ROLE_USER=普通用户，ROLE_ADMIN=管理员',
    status      TINYINT      NOT NULL DEFAULT 1          COMMENT '账号状态：0=禁用，1=正常',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 文章表：存储博客文章的全部内容及元信息
-- ============================================================
CREATE TABLE IF NOT EXISTS t_post (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '文章ID，自增主键',
    user_id        BIGINT       NOT NULL                   COMMENT '作者ID，关联 t_user.id',
    title          VARCHAR(200) NOT NULL                   COMMENT '文章标题',
    slug           VARCHAR(200) NOT NULL UNIQUE            COMMENT 'URL 别名，用于前台友好的访问地址',
    summary        VARCHAR(500) DEFAULT ''                 COMMENT '文章摘要，用于列表页展示',
    content        MEDIUMTEXT   NOT NULL                   COMMENT '文章正文，Markdown 格式',
    cover_image    VARCHAR(500) DEFAULT ''                 COMMENT '封面图片 URL',
    status         TINYINT      NOT NULL DEFAULT 0         COMMENT '发布状态：0=草稿，1=已发布，2=已下架',
    is_top         TINYINT      NOT NULL DEFAULT 0         COMMENT '是否置顶：0=否，1=是',
    allow_comment  TINYINT      NOT NULL DEFAULT 1         COMMENT '是否允许评论：0=否，1=是',
    view_count     BIGINT       NOT NULL DEFAULT 0         COMMENT '阅读量计数',
    published_at   DATETIME     DEFAULT NULL               COMMENT '首次发布时间',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_published_at (published_at),
    INDEX idx_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- ============================================================
-- 分类表：文章分类，支持多级（当前为一级）
-- ============================================================
CREATE TABLE IF NOT EXISTS t_category (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID，自增主键',
    name        VARCHAR(50)  NOT NULL UNIQUE             COMMENT '分类名称',
    slug        VARCHAR(50)  NOT NULL UNIQUE             COMMENT 'URL 别名',
    description VARCHAR(200) DEFAULT ''                  COMMENT '分类描述',
    sort_order  INT          NOT NULL DEFAULT 0          COMMENT '排序值，越小越靠前',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分类表';

-- ============================================================
-- 标签表：文章标签，用于文章聚合检索
-- ============================================================
CREATE TABLE IF NOT EXISTS t_tag (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID，自增主键',
    name       VARCHAR(50) NOT NULL UNIQUE             COMMENT '标签名称',
    slug       VARCHAR(50) NOT NULL UNIQUE             COMMENT 'URL 别名',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签表';

-- ============================================================
-- 文章-分类关联表：多对多关系
-- ============================================================
CREATE TABLE IF NOT EXISTS t_post_category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID，自增主键',
    post_id     BIGINT NOT NULL                   COMMENT '文章ID，关联 t_post.id',
    category_id BIGINT NOT NULL                   COMMENT '分类ID，关联 t_category.id',
    UNIQUE KEY uk_post_category (post_id, category_id),
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章-分类关联表';

-- ============================================================
-- 文章-标签关联表：多对多关系
-- ============================================================
CREATE TABLE IF NOT EXISTS t_post_tag (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID，自增主键',
    post_id BIGINT NOT NULL                   COMMENT '文章ID，关联 t_post.id',
    tag_id  BIGINT NOT NULL                   COMMENT '标签ID，关联 t_tag.id',
    UNIQUE KEY uk_post_tag (post_id, tag_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章-标签关联表';

-- ============================================================
-- 评论表：支持父子嵌套的多级评论
-- ============================================================
CREATE TABLE IF NOT EXISTS t_comment (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID，自增主键',
    post_id    BIGINT      NOT NULL                   COMMENT '所属文章ID，关联 t_post.id',
    parent_id  BIGINT      DEFAULT NULL               COMMENT '父评论ID，顶级评论为 NULL，关联 t_comment.id',
    user_id    BIGINT      DEFAULT NULL               COMMENT '评论者用户ID（已登录），关联 t_user.id；匿名评论为 NULL',
    nickname   VARCHAR(50) DEFAULT '匿名'              COMMENT '评论者昵称（未登录时使用）',
    email      VARCHAR(100) DEFAULT ''                COMMENT '评论者邮箱（未登录时填写，不公开展示）',
    content    TEXT        NOT NULL                   COMMENT '评论正文内容',
    status     TINYINT     NOT NULL DEFAULT 0         COMMENT '审核状态：0=待审核，1=已通过，2=已拒绝',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    INDEX idx_post_id (post_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ============================================================
-- 初始管理员账号（密码: admin123 的 MD5）
-- ============================================================
INSERT INTO t_user (username, password, nickname, role, status)
VALUES ('admin', '0192023a7bbd73250516f069df18b500', '管理员', 'ROLE_ADMIN', 1)
ON DUPLICATE KEY UPDATE id = id;
