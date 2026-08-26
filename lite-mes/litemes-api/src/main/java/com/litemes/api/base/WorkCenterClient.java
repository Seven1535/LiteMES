package com.litemes.api.base;

import com.litemes.api.base.dto.WorkCenterDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 工位契约（litemes-base 内部接口）。
 * 对应《设计规格说明书》7.2 内部接口表：GET /inner/workcenters（可按类型筛选，用于派工推荐）
 * 实现方：litemes-base；消费方：litemes-production。
 */
@FeignClient(name = "litemes-base", path = "/inner", contextId = "workCenterClient")
public interface WorkCenterClient {

    /** 工位列表（可按类型筛选，用于派工推荐） */
    @GetMapping("/workcenters")
    List<WorkCenterDTO> listWorkCenters(@RequestParam(value = "type", required = false) String type);

    /** 工位详情（派工时校验工位状态） */
    @GetMapping("/workcenters/{id}")
    WorkCenterDTO getWorkCenter(@PathVariable("id") String id);
}
