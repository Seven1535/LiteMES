package com.litemes.base.module.workflow.repository;

import com.litemes.base.module.workflow.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 工序步骤仓库 */
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, String> {

    List<WorkflowStep> findByWorkflowIdOrderByStepOrderAsc(String workflowId);

    List<WorkflowStep> findByWorkflowId(String workflowId);

    boolean existsByWorkflowIdAndStepCode(String workflowId, String stepCode);

    void deleteByWorkflowId(String workflowId);

    void deleteByIdIn(List<String> ids);
}
