# Blog Backend

博客平台后端服务，基于 Spring Boot 3 构建，提供文章管理、用户认证、评论审核、AI 写作辅助等 RESTful API。

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.5 (Java 17) |
| ORM | MyBatis + MyBatis-Spring |
| 分页 | PageHelper |
| 认证授权 | Sa-Token (Redis Session) |
| 对象映射 | MapStruct |
| 对象存储 | 阿里云 OSS |
| AI 服务 | Spring AI (OpenAI 兼容协议) |
| 验证码 | Hutool Captcha |
| 中文转拼音 | pinyin4j |
| API 文档 | Knife4j (OpenAPI 3) |
| 数据库 | MySQL 8.x |
| 缓存 | Redis (Lettuce) |

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p < src/main/resources/blog_new.sql
```

### 2. 配置环境变量（可选）

默认使用 `application-dev.yaml`，可通过环境变量覆盖：

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your_redis_password

# AI 服务（可选）
export MIMO_BASE_URL=https://your-ai-endpoint
export MIMO_API_KEY=your_api_key

# 阿里云 OSS（可选）
export OSS_ACCESS_KEY_ID=your_key
export OSS_ACCESS_KEY_SECRET=your_secret
export OSS_BUCKET_NAME=your_bucket
```

### 3. 启动服务

```bash
mvn spring-boot:run
```

服务默认运行在 `http://localhost:8080`

### 4. 访问 API 文档

- Knife4j 文档：http://localhost:8080/doc.html
- Swagger UI：http://localhost:8080/swagger-ui/index.html

## 项目结构

```
src/main/java/com/blog/
├── common/          # 通用类（Result、异常、状态码）
├── config/          # 配置类（安全、AI、OSS、异常处理）
├── controller/
│   ├── admin/       # 后台管理接口（需 ADMIN 角色）
│   └── front/       # 前台公开接口
├── converter/       # MapStruct 对象转换器
├── dto/             # 请求 DTO
├── entity/          # 数据库实体
├── mapper/          # MyBatis Mapper 接口
├── service/         # 业务逻辑层
│   └── impl/        # 服务实现
├── util/            # 工具类（Slug 生成、限流等）
└── vo/              # 响应 VO
```

## 主要功能模块

- **用户认证**：注册、登录、JWT Token、验证码、忘记密码
- **文章管理**：CRUD、草稿/发布/下架状态流转、Slug 路由、SEO 字段
- **分类/标签**：CRUD、Slug 自动生成（中文转拼音）、文章关联
- **评论系统**：多级嵌套评论、人工审核、AI 审核（脏话检测）、30 秒自动审核定时任务
- **文件上传**：阿里云 OSS 图片上传
- **AI 助手**：文章生成、润色、标签建议、流式对话
- **后台管理**：仪表盘统计、用户管理（角色/状态）

## 构建部署

```bash
# 打包
mvn clean package -DskipTests

# 生产环境启动
java -jar target/blog-backend-1.0.0.jar --spring.profiles.active=prod
```
