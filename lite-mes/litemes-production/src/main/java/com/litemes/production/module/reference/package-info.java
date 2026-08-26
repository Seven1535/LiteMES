/**
 * 引用计数查询模块：供 base 服务删除保护调用（唯一反向调用）。
 * TODO: 实现 ReferenceController —— 契约见 com.litemes.api.production.ReferenceCheckClient
 *   GET /inner/references/products/{productId}     产品被工单引用计数
 *   GET /inner/references/workcenters/{workCenterId} 工位进行中任务数
 */
package com.litemes.production.module.reference;
