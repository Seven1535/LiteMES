/**
 * Feign 客户端 fallback 实现。
 * TODO: 实现 ProductClient / WorkflowClient / WorkflowStepClient / WorkCenterClient / UserClient 的 fallback：
 *   展示类调用失败 → 降级返回空（不阻塞主流程）；
 *   主数据查询配合 Redis 缓存兜底（cache:product:{id} 等，见《架构设计说明书》5.4）。
 */
package com.litemes.production.feign;
