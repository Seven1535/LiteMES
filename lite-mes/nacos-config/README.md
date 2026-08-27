# Nacos 配置快照说明

本目录是 Nacos 配置中心的内容快照（与 Nacos 中的配置一一对应），用于环境重建或配置核对。

## 命名空间

| 命名空间 ID | 名称 | 用途 |
|---|---|---|
| `litemes-dev` | LiteMES 开发环境 | 本地/开发环境全部配置 |

## 配置清单（Group 均为 DEFAULT_GROUP）

| Data ID | 类型 | 引用方 | 内容 |
|---|---|---|---|
| `litemes-common.yml` | 共享配置（shared-configs） | gateway / base / production | Redis 连接、JWT 密钥与有效期 |
| `litemes-gateway.yml` | 主配置（按服务名匹配） | litemes-gateway | 路由规则、网关鉴权白名单 |
| `litemes-base.yml` | 主配置（按服务名匹配） | litemes-base | 达梦数据源（LITEMES_BASE）、JPA 方言、白名单 |
| `litemes-production.yml` | 主配置（按服务名匹配） | litemes-production | 达梦数据源（LITEMES_PRODUCTION）、JPA 方言、白名单 |

## 配置优先级

Nacos 主配置 > Nacos 共享配置（litemes-common.yml）> 本地 application.yml（兜底）。

## 修改约定

- 改 Nacos 配置后，同步更新本目录对应文件，保持快照一致；
- 各服务本地 `application.yml` 仅作 Nacos 不可用时的兜底，不作为主配置源。
