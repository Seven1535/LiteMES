/**
 * 生产工单模块：工单 CRUD + 状态流转 + 进度跟踪。
 * TODO: 实现 WorkOrderController / WorkOrderService / WorkOrder 实体 —— 见《设计规格说明书》7.2、8.2
 *   创建工单：Redis INCR 生成工单号 + Feign 校验产品并锁定生效工艺版本；
 *   状态机：PLANNED → RELEASED → IN_PROGRESS → COMPLETED；RELEASED/IN_PROGRESS → CLOSED（见 4.2）。
 */
package com.litemes.production.module.workorder;
