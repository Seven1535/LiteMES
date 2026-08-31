package com.litemes.base.feign;

import com.litemes.api.base.dto.WorkflowStepDTO;
import com.litemes.base.module.workflow.entity.WorkflowStep;
import com.litemes.base.module.workflow.repository.WorkflowStepRepository;
import com.litemes.common.core.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工序步骤内部接口：实现 litemes-api 的 WorkflowStepClient 契约（供 litemes-production 报工进度汇总）。
 * 不经网关，由 JwtAuthFilter 识别 Feign 透传的 X-User-Id Header 放行。
 */
@RestController
@RequestMapping("/inner")
@RequiredArgsConstructor
public class WorkflowStepInnerController {

    private final WorkflowStepRepository stepRepository;

    /** 对应契约：GET /inner/workflow-steps/{id} */
    @GetMapping("/workflow-steps/{id}")
    public WorkflowStepDTO getStep(@PathVariable String id) {
        WorkflowStep step = stepRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "工序不存在"));
        return toDTO(step);
    }

    /** 对应契约：GET /inner/workflow-steps?workflowId=（按顺序，工单详情/派工列表用） */
    @GetMapping("/workflow-steps")
    public List<WorkflowStepDTO> listByWorkflow(@RequestParam("workflowId") String workflowId) {
        return stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId)
                .stream().map(this::toDTO).toList();
    }

    private WorkflowStepDTO toDTO(WorkflowStep step) {
        WorkflowStepDTO dto = new WorkflowStepDTO();
        dto.setId(step.getId());
        dto.setWorkflowId(step.getWorkflowId());
        dto.setStepCode(step.getStepCode());
        dto.setStepName(step.getStepName());
        dto.setStepOrder(step.getStepOrder());
        dto.setRequiredWorkCenterType(step.getRequiredWorkCenterType());
        dto.setEstimatedMinutes(step.getEstimatedMinutes());
        return dto;
    }
}
