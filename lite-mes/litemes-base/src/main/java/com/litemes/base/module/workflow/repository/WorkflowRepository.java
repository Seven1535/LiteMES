package com.litemes.base.module.workflow.repository;

import com.litemes.base.module.workflow.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 工艺路线仓库（DRAFT/ACTIVE/ARCHIVED 均为有效数据，无逻辑删除） */
public interface WorkflowRepository extends JpaRepository<Workflow, String> {

    List<Workflow> findByProductIdOrderByVersionDesc(String productId);

    Optional<Workflow> findByProductIdAndVersion(String productId, Integer version);

    Optional<Workflow> findFirstByProductIdAndIsActiveTrue(String productId);

    List<Workflow> findByProductIdAndStatus(String productId, String status);

    Optional<Integer> findTopByProductIdOrderByVersionDesc(String productId);
}
