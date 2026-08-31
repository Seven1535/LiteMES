package com.litemes.base.module.workcenter.controller;

import com.litemes.base.module.workcenter.dto.WorkCenterCreateRequest;
import com.litemes.base.module.workcenter.dto.WorkCenterQueryRequest;
import com.litemes.base.module.workcenter.dto.WorkCenterStatusRequest;
import com.litemes.base.module.workcenter.dto.WorkCenterUpdateRequest;
import com.litemes.base.module.workcenter.dto.WorkCenterVO;
import com.litemes.base.module.workcenter.service.WorkCenterService;
import com.litemes.common.core.AjaxResult;
import com.litemes.common.core.PageResult;
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
 * 工位管理接口（经网关：/api/v1/base/workcenters，对应设计规格 7.1）。
 */
@RestController
@RequestMapping("/api/v1/base/workcenters")
@RequiredArgsConstructor
public class WorkCenterController {

    private final WorkCenterService workCenterService;

    /** 分页查询（编码/名称模糊 + 状态精确） */
    @GetMapping
    public AjaxResult page(@Valid WorkCenterQueryRequest query) {
        PageResult<WorkCenterVO> result = workCenterService.page(query);
        return AjaxResult.success(result);
    }

    /** 工位详情 */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable String id) {
        return AjaxResult.success(workCenterService.getById(id));
    }

    /** 新增工位（默认空闲） */
    @PostMapping
    public AjaxResult create(@Valid @RequestBody WorkCenterCreateRequest request) {
        return AjaxResult.success(workCenterService.create(request));
    }

    /** 修改工位（编码不可改） */
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable String id, @Valid @RequestBody WorkCenterUpdateRequest request) {
        return AjaxResult.success(workCenterService.update(id, request));
    }

    /** 更新工位状态（IDLE / BUSY / OFFLINE） */
    @PutMapping("/{id}/status")
    public AjaxResult updateStatus(@PathVariable String id, @Valid @RequestBody WorkCenterStatusRequest request) {
        return AjaxResult.success(workCenterService.updateStatus(id, request));
    }

    /** 删除工位（逻辑删除，有进行中任务时返回 400） */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable String id) {
        workCenterService.delete(id);
        return AjaxResult.success(null);
    }
}
