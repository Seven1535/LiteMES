package com.litemes.base.module.workflow.repository;

import com.litemes.base.module.workflow.entity.WorkflowStepLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 工序连线仓库 */
public interface WorkflowStepLinkRepository extends JpaRepository<WorkflowStepLink, String> {

    List<WorkflowStepLink> findByWorkflowId(String workflowId);

    void deleteByWorkflowId(String workflowId);

    void deleteBySourceStepIdInOrTargetStepIdIn(List<String> sourceIds, List<String> targetIds);
}
