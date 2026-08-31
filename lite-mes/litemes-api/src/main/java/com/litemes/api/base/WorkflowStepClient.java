package com.litemes.api.base;

import com.litemes.api.base.dto.WorkflowStepDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 工序步骤契约（litemes-base 内部接口）。
 * 对应《设计规格说明书》7.2 内部接口表：GET /inner/workflow-steps/{id}
 * 实现方：litemes-base；消费方：litemes-production（报工进度汇总）。
 */
@FeignClient(name = "litemes-base", path = "/inner", contextId = "workflowStepClient")
public interface WorkflowStepClient {

    /** 查询工序详情（报工时获取工序信息用于进度汇总） */
    @GetMapping("/workflow-steps/{id}")
    WorkflowStepDTO getStep(@PathVariable("id") String id);

    /** 按工艺版本查工序列表（按顺序，工单详情/派工列表用） */
    @GetMapping("/workflow-steps")
    List<WorkflowStepDTO> listByWorkflow(@RequestParam("workflowId") String workflowId);
}
