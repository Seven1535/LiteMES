package com.litemes.production.module.dispatch.controller;

import com.litemes.common.core.AjaxResult;
import com.litemes.common.core.PageResult;
import com.litemes.production.module.dispatch.dto.DispatchTaskCreateRequest;
import com.litemes.production.module.dispatch.dto.DispatchTaskQueryRequest;
import com.litemes.production.module.dispatch.dto.DispatchTaskReportRequest;
import com.litemes.production.module.dispatch.dto.DispatchTaskVO;
import com.litemes.production.module.dispatch.service.DispatchTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 派工任务接口。路径规范见《开发规范说明文档》3.1，接口清单见《设计规格说明书》7.1。
 */
@RestController
@RequestMapping("/api/v1/prod/dispatch-tasks")
@RequiredArgsConstructor
public class DispatchTaskController {

    private final DispatchTaskService dispatchTaskService;

    /** 分页查询派工任务（工单/工位/操作员/状态筛选） */
    @GetMapping
    public AjaxResult<PageResult<DispatchTaskVO>> page(@Valid DispatchTaskQueryRequest query) {
        return AjaxResult.success(dispatchTaskService.page(query));
    }

    /** 任务详情 */
    @GetMapping("/{id}")
    public AjaxResult<DispatchTaskVO> detail(@PathVariable String id) {
        return AjaxResult.success(dispatchTaskService.getById(id));
    }

    /** 派工（业务规则 8.3：工序/工位/操作员必填，数量不超上限） */
    @PostMapping
    public AjaxResult<DispatchTaskVO> create(@Valid @RequestBody DispatchTaskCreateRequest request) {
        return AjaxResult.success(dispatchTaskService.create(request));
    }

    /** 开工：PENDING → PROCESSING，工位置 BUSY */
    @PutMapping("/{id}/start")
    public AjaxResult<DispatchTaskVO> start(@PathVariable String id) {
        return AjaxResult.success(dispatchTaskService.start(id));
    }

    /** 报工：累计达到派工数量任务自动完成，同步更新工单已完工数量 */
    @PutMapping("/{id}/report")
    public AjaxResult<DispatchTaskVO> report(@PathVariable String id,
                                             @Valid @RequestBody DispatchTaskReportRequest request) {
        return AjaxResult.success(dispatchTaskService.report(id, request.getQuantity()));
    }
}
