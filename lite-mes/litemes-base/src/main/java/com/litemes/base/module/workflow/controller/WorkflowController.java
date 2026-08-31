package com.litemes.base.module.workflow.controller;

import com.litemes.base.module.workflow.dto.SaveLinksRequest;
import com.litemes.base.module.workflow.dto.SaveStepsRequest;
import com.litemes.base.module.workflow.dto.WorkflowCreateRequest;
import com.litemes.base.module.workflow.dto.WorkflowUpdateRequest;
import com.litemes.base.module.workflow.service.WorkflowService;
import com.litemes.common.core.AjaxResult;
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
 * 工艺路线接口（对应设计规格 7.1 工艺路线模块，另补生效版本查询与删除）。
 */
@RestController
@RequestMapping("/api/v1/base")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    /** 产品的工艺路线版本列表 */
    @GetMapping("/products/{productId}/workflows")
    public AjaxResult listByProduct(@PathVariable String productId) {
        return AjaxResult.success(workflowService.listByProduct(productId));
    }

    /** 新建工艺路线版本（版本号自增，可选复制已有版本） */
    @PostMapping("/products/{productId}/workflows")
    public AjaxResult create(@PathVariable String productId,
                             @Valid @RequestBody(required = false) WorkflowCreateRequest request) {
        return AjaxResult.success(workflowService.create(productId,
                request != null ? request : new WorkflowCreateRequest()));
    }

    /** 产品当前生效工艺版本（含工序与连线） */
    @GetMapping("/workflows/products/{productId}/active")
    public AjaxResult activeByProduct(@PathVariable String productId) {
        return AjaxResult.success(workflowService.activeByProduct(productId));
    }

    /** 工艺路线详情（含工序步骤和连线） */
    @GetMapping("/workflows/{id}")
    public AjaxResult detail(@PathVariable String id) {
        return AjaxResult.success(workflowService.detail(id));
    }

    /** 修改工艺路线元数据（仅草稿） */
    @PutMapping("/workflows/{id}")
    public AjaxResult update(@PathVariable String id, @Valid @RequestBody WorkflowUpdateRequest request) {
        return AjaxResult.success(workflowService.update(id, request));
    }

    /** 批量保存工序（画布保存，仅草稿） */
    @PutMapping("/workflows/{id}/steps")
    public AjaxResult saveSteps(@PathVariable String id, @Valid @RequestBody SaveStepsRequest request) {
        return AjaxResult.success(workflowService.saveSteps(id, request));
    }

    /** 批量保存连线关系（画布保存，仅草稿） */
    @PutMapping("/workflows/{id}/links")
    public AjaxResult saveLinks(@PathVariable String id, @Valid @RequestBody SaveLinksRequest request) {
        return AjaxResult.success(workflowService.saveLinks(id, request));
    }

    /** 发布/激活某版本（旧生效版本自动归档） */
    @PutMapping("/workflows/{id}/activate")
    public AjaxResult activate(@PathVariable String id) {
        return AjaxResult.success(workflowService.activate(id));
    }

    /** 删除工艺版本（仅草稿） */
    @DeleteMapping("/workflows/{id}")
    public AjaxResult delete(@PathVariable String id) {
        workflowService.delete(id);
        return AjaxResult.success(null);
    }
}
