/**
 * 派工管理模块：任务分配到工位 + 报工。
 * TODO: 实现 DispatchTaskController / DispatchTaskService / DispatchTask 实体 —— 见《设计规格说明书》7.2、8.3
 *   派工时经 Feign 校验工位/操作员；报工累计式（completedQty += 本次数量），
 *   累计达到派工数量自动置 COMPLETED，Redisson 分布式锁防并发重复报工。
 */
package com.litemes.production.module.dispatch;
