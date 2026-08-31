package com.litemes.production.module.workorder.controller;

import com.litemes.common.core.AjaxResult;
import com.litemes.production.module.workorder.dto.WorkOrderCreateRequest;
import com.litemes.production.module.workorder.dto.WorkOrderQueryRequest;
import com.litemes.production.module.workorder.dto.WorkOrderUpdateRequest;
import com.litemes.production.module.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 生产工单接口（对应设计规格 7.1 生产工单模块，路径 /api/v1/prod/workorders）。
 */
@RestController
@RequestMapping("/api/v1/prod/workorders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    /** 工单列表（分页 + 编号/状态筛选，产品/工艺名称经 Feign 聚合） */
    @GetMapping
    public AjaxResult page(@Valid WorkOrderQueryRequest query) {
        return AjaxResult.success(workOrderService.page(query));
    }

    /** 工单详情 */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable String id) {
        return AjaxResult.success(workOrderService.getById(id));
    }

    /** 创建工单（Redis INCR 生成工单号，自动锁定生效工艺版本） */
    @PostMapping
    public AjaxResult create(@Valid @RequestBody WorkOrderCreateRequest request) {
        return AjaxResult.success(workOrderService.create(request));
    }

    /** 更新工单（仅已计划状态） */
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable String id, @Valid @RequestBody WorkOrderUpdateRequest request) {
        return AjaxResult.success(workOrderService.update(id, request));
    }

    /** 下达工单（PLANNED → RELEASED） */
    @PutMapping("/{id}/release")
    public AjaxResult release(@PathVariable String id) {
        return AjaxResult.success(workOrderService.release(id));
    }

    /** 关闭工单（终态，不可再操作） */
    @PutMapping("/{id}/close")
    public AjaxResult close(@PathVariable String id) {
        return AjaxResult.success(workOrderService.close(id));
    }

    /** 删除工单（逻辑删除，仅已计划可删） */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable String id) {
        workOrderService.delete(id);
        return AjaxResult.success(null);
    }
}
