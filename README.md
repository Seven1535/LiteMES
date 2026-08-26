# LiteMES — 轻量级制造执行系统

> 面向 10–50 人小型机械加工厂，**用得起、学得会、看得懂**的轻量级 MES（Manufacturing Execution System）。
>
> 技术栈：Spring Cloud 微服务 + Nacos + Redis + Vue 3 + Element Plus | 单租户，一台云服务器即可部署

---

## 目录

- [项目简介](#项目简介)
- [核心特性](#核心特性)
- [功能模块](#功能模块)
- [技术架构](#技术架构)
- [快速开始](#快速开始)
- [Docker 部署](#docker-部署)
- [目录结构](#目录结构)
- [相关文档](#相关文档)

---

## 项目简介

小型工厂现状：Excel 排产 + 纸质流转 + 微信群问进度。LiteMES 提供一套完整闭环：

- **工艺可视化**：拖拽画图定义工序流程，替代手写工艺卡
- **工单可追踪**：每张单走到哪一步，打开系统就知道
- **派工有记录**：谁干的、干了多久、合格多少，全部留痕
- **看板实时看**：车间大屏一挂，进度一目了然

系统形态：PC Web 应用（响应式兼容平板），浏览器访问、免安装客户端，单租户部署。

## 核心特性

| 特性 | 说明 |
|------|------|
| 🗺️ 工艺流程图拖拽设计 | 基于 Vue Flow 的可视化工序建模，前端无感知保存为节点-边数据 |
| 📋 工单全生命周期跟踪 | 创建 → 排产 → 派工 → 报工 → 完工，状态机严格流转 |
| 👷 派工与报工 | 任务分配到工位，支持合格数/不良数/工时填报 |
| 📊 实时生产看板 | WebSocket 推送，聚合基础与业务两服务数据，车间大屏即插即用 |
| 🔐 统一鉴权 | Gateway 全局 JWT 过滤 + Nacos 动态白名单 + Feign 内部调用透传 |
| 🐳 一键部署 | 提供 Dockerfile 镜像构建与多环境配置模板 |

## 功能模块

```
LiteMES
├─ litemes-base（基础数据服务 :8081）
│   ├── 系统管理（System）      ← 用户/角色/权限 + JWT 认证签发
│   ├── 产品管理（Product）     ← 产品档案，关联工艺路线
│   ├── 工艺建模（Process）     ← 核心亮点：可视化流程图设计
│   └── 工位管理（WorkCenter）  ← 车间资源建模
│
└─ litemes-production（业务数据服务 :8082）
    ├── 生产工单（WorkOrder）   ← 生产任务下达与跟踪
    ├── 派工管理（Dispatch）    ← 任务分配到工位 + 报工
    └── 生产看板（Dashboard）   ← 实时进度可视化（聚合两服务数据）
```

划分原则：低频变化、被多方引用的主数据归基础数据服务；高频写入的交易数据归业务数据服务。两服务各自独立数据库，业务服务通过 OpenFeign 调用基础服务获取主数据。

## 技术架构

| 层 | 技术选型 |
|----|----------|
| 后端框架 | Spring Boot 3 + Spring Cloud 2023.0.2 + Spring Cloud Alibaba 2023.0.1.2（Java 17） |
| 注册/配置中心 | Nacos（服务发现 + 配置中心，敏感配置生产环境收口 Nacos） |
| 服务通信 | OpenFeign + LoadBalancer（Gateway → 下游服务，Feign 头透传） |
| 鉴权 | JWT（Gateway 统一过滤校验，接口白名单可配置） |
| 缓存/编号/锁 | Redis（缓存、序列号 INCR 生成、分布式锁） |
| 持久层 | Spring Data JPA（物理命名 SNAKE_CASE，逻辑删除规范） |
| 网关 | Spring Cloud Gateway（:8080 统一入口、路由转发、跨域处理） |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Vue Router + Vue Flow + Axios |
| 数据库 | MySQL（达梦兼容预留，通过驱动切换） |
| 部署 | Docker（基础镜像 / Gateway / 生产多阶段构建） |

## 快速开始

### 环境要求

- JDK 17+、Maven 3.8+
- Node.js 18+（推荐 20 LTS）
- MySQL 8.0+（数据库：`litemes_base`、`litemes_production`）
- Redis 6+（默认 `localhost:6379`）
- Nacos 2.x（默认 `localhost:8848`，账号 `nacos/nacos`）

### 1. 启动基础设施

```bash
# 启动 Nacos（注册/配置中心）与 Redis
docker run -d --name nacos -p 8848:8848 -e MODE=standalone nacos/nacos-server:v2.3.2
docker run -d --name redis -p 6379:6379 redis:7
```

### 2. 初始化数据库

```sql
CREATE DATABASE litemes_base CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE litemes_production CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 学习项目使用 JPA `ddl-auto: update` 自动同步表结构；生产环境需改为 `validate` + 独立迁移脚本。

### 3. 启动后端服务

本地开发默认直连本地 MySQL/Redis（`NACOS_CONFIG_ENABLED=false`，即不从 Nacos 拉配置），按依赖顺序启动：

```bash
cd lite-mes

# ① 编译公共模块（litemes-common / litemes-api）
mvn clean install -pl litemes-common,litemes-api -am -DskipTests

# ② 启动网关（:8080）
mvn spring-boot:run -pl litemes-gateway

# ③ 启动基础数据服务（:8081）
mvn spring-boot:run -pl litemes-base

# ④ 启动业务数据服务（:8082）
mvn spring-boot:run -pl litemes-production
```

数据库连接等参数通过环境变量覆盖（均已带默认值）：

```bash
DB_HOST=localhost DB_USERNAME=root DB_PASSWORD=yourpass REDIS_PASSWORD= \
  NACOS_ADDR=localhost:8848 mvn spring-boot:run -pl litemes-base
```

### 4. 启动前端

```bash
cd lite-mes/lite-mes-ui

npm install
npm run dev        # 开发模式，默认 http://localhost:5173
npm run build      # 生产构建，产物输出到 dist/
```

前端已配置代理，开发时将 API 请求转发到网关 `http://localhost:8080`。

### 5. 访问系统

| 入口 | 地址 |
|------|------|
| 前端页面 | http://localhost:5173 |
| 网关统一入口 | http://localhost:8080 |
| Swagger 文档（各服务） | http://localhost:8081/swagger-ui.html |

## Docker 部署

`lite-mes/docker/` 目录提供镜像构建文件：

| 文件 | 用途 |
|------|------|
| `Dockerfile.base` | 基础运行镜像（JDK 17 环境） |
| `Dockerfile.gateway` | 网关服务镜像 |
| `Dockerfile.production` | 生产多阶段构建（编译 + 瘦身 + 运行） |

生产环境部署前需将数据源、Redis 密码、JWT 密钥等敏感配置收口到 Nacos 配置中心（`litemes-base.yml` / `litemes-production.yml` / `litemes-gateway.yml`），并通过环境变量注入 Nacos 凭据。

## 目录结构

```
LiteMES
├── lite-mes/                    # 后端微服务工程（Maven 多模块）
│   ├── litemes-common/          # 公共模块：通用响应、异常、Redis、JWT、审计
│   ├── litemes-api/             # Feign 客户端与 DTO 定义
│   ├── litemes-gateway/         # 网关（:8080）：路由、鉴权、跨域
│   ├── litemes-base/            # 基础数据服务（:8081）
│   ├── litemes-production/      # 业务数据服务（:8082）
│   ├── lite-mes-ui/             # 前端（Vue 3 + Vite + Element Plus）
│   └── docker/                  # Docker 镜像构建文件
├── 设计规格说明书.md             # 产品与功能设计
├── 架构设计说明书.md             # 系统架构设计
├── 开发规范说明文档.md           # 前后端开发规范
└── .gitignore
```

## 相关文档

- 📘 [设计规格说明书](./设计规格说明书.md) — 产品定位、功能模块、页面与交互规格
- 🏗️ [架构设计说明书](./架构设计说明书.md) — 微服务划分、技术选型、部署架构
- 📐 [开发规范说明文档](./开发规范说明文档.md) — 分层职责、命名约定、状态机与接口规范

---

**许可**：本项目仅供学习交流，未配置开源许可证（All Rights Reserved）。
