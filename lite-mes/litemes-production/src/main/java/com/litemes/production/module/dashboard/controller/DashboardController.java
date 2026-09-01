package com.litemes.production.module.dashboard.controller;

import com.litemes.common.core.AjaxResult;
import com.litemes.production.module.dashboard.dto.DashboardSummaryVO;
import com.litemes.production.module.dashboard.dto.WorkCenterLoadVO;
import com.litemes.production.module.dashboard.dto.WorkOrderProgressVO;
import com.litemes.production.module.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 生产看板接口（设计规格 7.1 看板模块）。
 * 实时推送走 WebSocket（/ws/production），本接口负责首次加载与断线重连后的全量拉取。
 */
@RestController
@RequestMapping("/api/v1/prod/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 概览数据（在制数/待下达数/完成数、工位状态统计、今日产出，30s 缓存） */
    @GetMapping("/summary")
    public AjaxResult<DashboardSummaryVO> summary() {
        return AjaxResult.success(dashboardService.summary());
    }

    /** 在制工单进度列表 */
    @GetMapping("/workorders")
    public AjaxResult<List<WorkOrderProgressVO>> workOrders() {
        return AjaxResult.success(dashboardService.workOrderProgress());
    }

    /** 工位负载列表 */
    @GetMapping("/workcenters")
    public AjaxResult<List<WorkCenterLoadVO>> workCenters() {
        return AjaxResult.success(dashboardService.workCenterLoad());
    }
}
