/**
 * Feign 客户端 fallback 实现。
 * TODO: 实现 ReferenceCheckClient 的 fallback —— production 服务不可用时，
 *   删除请求直接拒绝（安全优先，宁可拒绝不可误删，见《设计规格说明书》8.4）。
 */
package com.litemes.base.feign;
