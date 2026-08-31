package com.litemes.base.feign;

import com.litemes.api.base.dto.WorkflowDTO;
import com.litemes.base.module.workflow.entity.Workflow;
import com.litemes.base.module.workflow.repository.WorkflowRepository;
import com.litemes.common.core.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工艺路线内部接口：实现 litemes-api 的 WorkflowClient 契约（供 litemes-production 创建工单时锁定生效版本）。
 * 不经网关，由 JwtAuthFilter 识别 Feign 透传的 X-User-Id Header 放行。
 */
@RestController
@RequestMapping("/inner")
@RequiredArgsConstructor
public class WorkflowInnerController {

    private final WorkflowRepository workflowRepository;

    /** 对应契约：GET /inner/workflows/{productId}/active */
    @GetMapping("/workflows/{productId}/active")
    public WorkflowDTO getActiveWorkflow(@PathVariable String productId) {
        Workflow workflow = workflowRepository.findFirstByProductIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new BusinessException(404, "该产品暂无生效的工艺版本"));
        WorkflowDTO dto = new WorkflowDTO();
        dto.setId(workflow.getId());
        dto.setProductId(workflow.getProductId());
        dto.setVersion(workflow.getVersion());
        dto.setVersionName(workflow.getVersionName());
        dto.setIsActive(workflow.getIsActive());
        dto.setStatus(workflow.getStatus());
        return dto;
    }

    /** 对应契约：GET /inner/workflows/by-id/{workflowId}（工单锁定的可能是历史版本） */
    @GetMapping("/workflows/by-id/{workflowId}")
    public WorkflowDTO getWorkflow(@PathVariable String workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new BusinessException(404, "工艺路线不存在"));
        WorkflowDTO dto = new WorkflowDTO();
        dto.setId(workflow.getId());
        dto.setProductId(workflow.getProductId());
        dto.setVersion(workflow.getVersion());
        dto.setVersionName(workflow.getVersionName());
        dto.setIsActive(workflow.getIsActive());
        dto.setStatus(workflow.getStatus());
        return dto;
    }
}
