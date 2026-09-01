package com.litemes.production.module.dashboard.dto;

import lombok.Data;

/** 看板概览（设计规格 6.3.2：工单概览 + 工位状态 + 今日产出） */
@Data
public class DashboardSummaryVO {

    private OrderOverview orderOverview;
    private WorkCenterOverview workCenterOverview;
    private TodayOutput todayOutput;

    /** 工单概览：待下达 = 已计划 + 已下达未投产 */
    @Data
    public static class OrderOverview {
        private long inProgress;
        private long pending;
        private long completed;
    }

    /** 工位状态统计（Feign 聚合 base 工位列表） */
    @Data
    public static class WorkCenterOverview {
        private long idle;
        private long busy;
        private long offline;
    }

    /** 今日产出：完成 = 今日完工任务数量合计；计划 = 今日派出任务数量合计 */
    @Data
    public static class TodayOutput {
        private long completed;
        private long planned;
        /** 达成率（百分比，计划为 0 时取 0） */
        private int percentage;
    }
}
