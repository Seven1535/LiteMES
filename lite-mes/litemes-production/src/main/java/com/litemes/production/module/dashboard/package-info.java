/**
 * 生产看板模块：实时进度可视化。
 * TODO: 实现 DashboardController / DashboardService —— 见《设计规格说明书》7.2
 *   聚合方式：Feign 拉取主数据 + 本地业务数据 + Redis 缓存（dashboard:summary）；
 *   实时性：报工/状态变更后经 ProductionWebSocketHandler 广播事件。
 */
package com.litemes.production.module.dashboard;
