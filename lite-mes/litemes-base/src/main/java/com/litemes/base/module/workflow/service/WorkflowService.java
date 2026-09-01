package com.litemes.base.module.workflow.service;

import com.litemes.base.module.product.entity.Product;
import com.litemes.base.module.product.repository.ProductRepository;
import com.litemes.base.module.workflow.dto.SaveLinksRequest;
import com.litemes.base.module.workflow.dto.SaveStepsRequest;
import com.litemes.base.module.workflow.dto.WorkflowCreateRequest;
import com.litemes.base.module.workflow.dto.WorkflowDetailVO;
import com.litemes.base.module.workflow.dto.WorkflowUpdateRequest;
import com.litemes.base.module.workflow.dto.WorkflowVO;
import com.litemes.base.module.workflow.entity.Workflow;
import com.litemes.base.module.workflow.entity.WorkflowStep;
import com.litemes.base.module.workflow.entity.WorkflowStepLink;
import com.litemes.base.module.workflow.repository.WorkflowRepository;
import com.litemes.base.module.workflow.repository.WorkflowStepLinkRepository;
import com.litemes.base.module.workflow.repository.WorkflowStepRepository;
import com.litemes.common.core.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 工艺路线业务：多版本管理 + 工序/连线画布保存 + 版本生效切换。
 * 核心规则（见设计规格 5.2 与 7.1）：
 * - 版本号产品内自增；同一产品至多一个 ACTIVE 版本，激活时旧 ACTIVE 自动归档；
 * - 仅草稿（DRAFT）允许编辑工序/连线与元数据；
 * - 激活要求至少一个工序；已发布版本不可删除（工单上线后追加引用校验）。
 */
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowStepLinkRepository linkRepository;
    private final ProductRepository productRepository;

    /** 产品的工艺版本列表（版本号倒序，冗余产品编码/名称） */
    public List<WorkflowVO> listByProduct(String productId) {
        Product product = loadProduct(productId);
        return workflowRepository.findByProductIdOrderByVersionDesc(productId).stream()
                .map(w -> toVO(w, product))
                .toList();
    }

    /** 详情：版本元数据 + 工序（按顺序） + 连线 */
    public WorkflowDetailVO detail(String workflowId) {
        Workflow workflow = loadWorkflow(workflowId);
        Product product = loadProduct(workflow.getProductId());

        WorkflowDetailVO vo = new WorkflowDetailVO();
        vo.setWorkflow(toVO(workflow, product));
        vo.setSteps(stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId).stream().map(s -> {
            WorkflowDetailVO.StepVO step = new WorkflowDetailVO.StepVO();
            step.setId(s.getId());
            step.setStepCode(s.getStepCode());
            step.setStepName(s.getStepName());
            step.setStepOrder(s.getStepOrder());
            step.setDescription(s.getDescription());
            step.setRequiredWorkCenterType(s.getRequiredWorkCenterType());
            step.setEstimatedMinutes(s.getEstimatedMinutes());
            step.setPosX(s.getPosX());
            step.setPosY(s.getPosY());
            return step;
        }).toList());
        vo.setLinks(linkRepository.findByWorkflowId(workflowId).stream().map(l -> {
            WorkflowDetailVO.LinkVO link = new WorkflowDetailVO.LinkVO();
            link.setId(l.getId());
            link.setSourceStepId(l.getSourceStepId());
            link.setTargetStepId(l.getTargetStepId());
            return link;
        }).toList());
        return vo;
    }

    /** 产品当前生效版本（工单创建时锁定；无生效版本返回 404） */
    public WorkflowDetailVO activeByProduct(String productId) {
        loadProduct(productId);
        Workflow active = workflowRepository.findFirstByProductIdAndIsActive(productId, Boolean.TRUE)
                .orElseThrow(() -> new BusinessException(404, "该产品暂无生效的工艺版本"));
        return detail(active.getId());
    }

    /** 新建版本：版本号自增，默认草稿；可选从已有版本复制工序与连线 */
    @Transactional
    public WorkflowVO create(String productId, WorkflowCreateRequest request) {
        Product product = loadProduct(productId);
        int nextVersion = workflowRepository.findTopByProductIdOrderByVersionDesc(productId)
                .map(Workflow::getVersion).orElse(0) + 1;

        Workflow workflow = new Workflow();
        workflow.setId(UUID.randomUUID().toString());
        workflow.setProductId(productId);
        workflow.setVersion(nextVersion);
        workflow.setVersionName(trimToNull(request.getVersionName()) != null
                ? request.getVersionName().trim() : "V" + nextVersion + ".0");
        workflow.setDescription(trimToNull(request.getDescription()));
        workflow.setIsActive(false);
        workflow.setStatus(STATUS_DRAFT);
        workflowRepository.save(workflow);

        // 从指定版本复制工序与连线（新版本全部换新 ID）
        if (request.getCopyFromVersion() != null) {
            Workflow source = workflowRepository.findByProductIdAndVersion(productId, request.getCopyFromVersion())
                    .orElseThrow(() -> new BusinessException(400, "复制来源版本不存在：V" + request.getCopyFromVersion()));
            copyStepsAndLinks(source.getId(), workflow.getId());
        }
        return toVO(workflow, product);
    }

    /** 修改版本元数据（仅草稿） */
    @Transactional
    public WorkflowVO update(String workflowId, WorkflowUpdateRequest request) {
        Workflow workflow = loadDraft(workflowId);
        workflow.setVersionName(request.getVersionName().trim());
        workflow.setDescription(trimToNull(request.getDescription()));
        return toVO(workflowRepository.save(workflow), loadProduct(workflow.getProductId()));
    }

    /** 批量保存工序（画布保存，全量覆盖：新增/更新/删除差异工序，被删工序的连线一并清理） */
    @Transactional
    public WorkflowDetailVO saveSteps(String workflowId, SaveStepsRequest request) {
        Workflow workflow = loadDraft(workflowId);

        Map<String, WorkflowStep> existing = new HashMap<>();
        stepRepository.findByWorkflowId(workflowId).forEach(s -> existing.put(s.getId(), s));

        Set<String> submittedIds = new HashSet<>();
        Set<String> seenCodes = new HashSet<>();
        int order = 1;
        for (SaveStepsRequest.StepItem item : request.getSteps()) {
            if (!seenCodes.add(item.getStepCode())) {
                throw new BusinessException(400, "工序编码重复：" + item.getStepCode());
            }
            WorkflowStep step = item.getId() != null ? existing.get(item.getId()) : new WorkflowStep();
            if (step == null) {
                throw new BusinessException(400, "工序不存在：" + item.getId());
            }
            if (step.getId() == null) {
                step.setId(UUID.randomUUID().toString());
                step.setWorkflowId(workflowId);
            }
            step.setStepCode(item.getStepCode().trim());
            step.setStepName(item.getStepName().trim());
            step.setStepOrder(item.getStepOrder() != null ? item.getStepOrder() : order);
            step.setDescription(trimToNull(item.getDescription()));
            step.setRequiredWorkCenterType(trimToNull(item.getRequiredWorkCenterType()));
            step.setEstimatedMinutes(item.getEstimatedMinutes());
            step.setPosX(item.getPosX() != null ? item.getPosX() : 0.0);
            step.setPosY(item.getPosY() != null ? item.getPosY() : 0.0);
            stepRepository.save(step);
            submittedIds.add(step.getId());
            order++;
        }

        // 删除本次未提交的旧工序及其连线
        List<String> removedIds = existing.keySet().stream()
                .filter(id -> !submittedIds.contains(id))
                .toList();
        if (!removedIds.isEmpty()) {
            linkRepository.deleteBySourceStepIdInOrTargetStepIdIn(removedIds, removedIds);
            stepRepository.deleteByIdIn(removedIds);
        }
        return detail(workflowId);
    }

    /** 批量保存连线（画布连线全量覆盖；端点必须属于本版本工序） */
    @Transactional
    public WorkflowDetailVO saveLinks(String workflowId, SaveLinksRequest request) {
        loadDraft(workflowId);

        Set<String> stepIds = new HashSet<>();
        stepRepository.findByWorkflowId(workflowId).forEach(s -> stepIds.add(s.getId()));
        for (SaveLinksRequest.LinkItem item : request.getLinks()) {
            if (!stepIds.contains(item.getSourceStepId()) || !stepIds.contains(item.getTargetStepId())) {
                throw new BusinessException(400, "连线端点不属于当前工艺版本的工序");
            }
            if (item.getSourceStepId().equals(item.getTargetStepId())) {
                throw new BusinessException(400, "工序不能连接到自身");
            }
        }

        linkRepository.deleteByWorkflowId(workflowId);
        for (SaveLinksRequest.LinkItem item : request.getLinks()) {
            WorkflowStepLink link = new WorkflowStepLink();
            link.setId(UUID.randomUUID().toString());
            link.setWorkflowId(workflowId);
            link.setSourceStepId(item.getSourceStepId());
            link.setTargetStepId(item.getTargetStepId());
            linkRepository.save(link);
        }
        return detail(workflowId);
    }

    /** 激活版本：草稿 + 至少一个工序；同一产品的旧 ACTIVE 版本自动归档 */
    @Transactional
    public WorkflowVO activate(String workflowId) {
        Workflow workflow = loadDraft(workflowId);
        if (stepRepository.findByWorkflowId(workflowId).isEmpty()) {
            throw new BusinessException(400, "工艺路线至少需要一个工序才能激活");
        }
        workflowRepository.findByProductIdAndStatus(workflow.getProductId(), STATUS_ACTIVE)
                .forEach(old -> {
                    old.setIsActive(false);
                    old.setStatus(STATUS_ARCHIVED);
                    workflowRepository.save(old);
                });
        workflow.setIsActive(true);
        workflow.setStatus(STATUS_ACTIVE);
        return toVO(workflowRepository.save(workflow), loadProduct(workflow.getProductId()));
    }

    /** 删除版本：仅草稿允许（已发布版本可能被工单引用，工单模块上线后追加引用校验） */
    @Transactional
    public void delete(String workflowId) {
        Workflow workflow = loadDraft(workflowId);
        linkRepository.deleteByWorkflowId(workflowId);
        stepRepository.deleteByWorkflowId(workflowId);
        workflowRepository.delete(workflow);
    }

    /** 复制工序与连线到新版本（工序换新 ID，连线按映射重建） */
    private void copyStepsAndLinks(String sourceWorkflowId, String targetWorkflowId) {
        List<WorkflowStep> sourceSteps = stepRepository.findByWorkflowIdOrderByStepOrderAsc(sourceWorkflowId);
        Map<String, String> idMapping = new HashMap<>();
        for (WorkflowStep source : sourceSteps) {
            WorkflowStep copy = new WorkflowStep();
            copy.setId(UUID.randomUUID().toString());
            copy.setWorkflowId(targetWorkflowId);
            copy.setStepCode(source.getStepCode());
            copy.setStepName(source.getStepName());
            copy.setStepOrder(source.getStepOrder());
            copy.setDescription(source.getDescription());
            copy.setRequiredWorkCenterType(source.getRequiredWorkCenterType());
            copy.setEstimatedMinutes(source.getEstimatedMinutes());
            copy.setPosX(source.getPosX());
            copy.setPosY(source.getPosY());
            stepRepository.save(copy);
            idMapping.put(source.getId(), copy.getId());
        }
        for (WorkflowStepLink sourceLink : linkRepository.findByWorkflowId(sourceWorkflowId)) {
            WorkflowStepLink copy = new WorkflowStepLink();
            copy.setId(UUID.randomUUID().toString());
            copy.setWorkflowId(targetWorkflowId);
            copy.setSourceStepId(idMapping.get(sourceLink.getSourceStepId()));
            copy.setTargetStepId(idMapping.get(sourceLink.getTargetStepId()));
            linkRepository.save(copy);
        }
    }

    private Workflow loadWorkflow(String workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new BusinessException(404, "工艺路线不存在"));
    }

    private Workflow loadDraft(String workflowId) {
        Workflow workflow = loadWorkflow(workflowId);
        if (!STATUS_DRAFT.equals(workflow.getStatus())) {
            throw new BusinessException(400, "仅草稿状态的工艺版本可编辑，请先新建版本");
        }
        return workflow;
    }

    private Product loadProduct(String productId) {
        return productRepository.findById(productId)
                .filter(p -> "0".equals(p.getDelFlag()))
                .orElseThrow(() -> new BusinessException(404, "产品不存在"));
    }

    private WorkflowVO toVO(Workflow workflow, Product product) {
        WorkflowVO vo = new WorkflowVO();
        vo.setId(workflow.getId());
        vo.setProductId(workflow.getProductId());
        vo.setProductCode(product.getProductCode());
        vo.setProductName(product.getProductName());
        vo.setVersion(workflow.getVersion());
        vo.setVersionName(workflow.getVersionName());
        vo.setDescription(workflow.getDescription());
        vo.setIsActive(workflow.getIsActive());
        vo.setStatus(workflow.getStatus());
        vo.setCreatedAt(workflow.getCreatedAt());
        vo.setUpdatedAt(workflow.getUpdatedAt());
        return vo;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
