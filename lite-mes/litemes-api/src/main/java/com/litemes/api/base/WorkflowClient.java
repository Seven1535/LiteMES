package com.litemes.api.base;

import com.litemes.api.base.dto.WorkflowDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 工艺路线契约（litemes-base 内部接口）。
 * 对应《设计规格说明书》7.2 内部接口表：GET /inner/workflows/{productId}/active
 * 实现方：litemes-base；消费方：litemes-production。
 */
@FeignClient(name = "litemes-base", path = "/inner", contextId = "workflowClient")
public interface WorkflowClient {

    /** 查询产品当前生效的工艺版本（工单创建时锁定） */
    @GetMapping("/workflows/{productId}/active")
    WorkflowDTO getActiveWorkflow(@PathVariable("productId") String productId);

    /** 按版本 ID 查工艺路线（工单锁定的可能是历史版本，列表聚合用） */
    @GetMapping("/workflows/by-id/{workflowId}")
    WorkflowDTO getWorkflow(@PathVariable("workflowId") String workflowId);
}
