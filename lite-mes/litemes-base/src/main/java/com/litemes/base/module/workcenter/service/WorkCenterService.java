package com.litemes.base.module.workcenter.service;

import com.litemes.api.production.ReferenceCheckClient;
import com.litemes.base.module.workcenter.dto.WorkCenterCreateRequest;
import com.litemes.base.module.workcenter.dto.WorkCenterQueryRequest;
import com.litemes.base.module.workcenter.dto.WorkCenterStatusRequest;
import com.litemes.base.module.workcenter.dto.WorkCenterUpdateRequest;
import com.litemes.base.module.workcenter.dto.WorkCenterVO;
import com.litemes.base.module.workcenter.entity.WorkCenter;
import com.litemes.base.module.workcenter.repository.WorkCenterRepository;
import com.litemes.common.core.BusinessException;
import com.litemes.common.core.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * 工位业务：标准分页查询 / 新增 / 修改 / 状态更新 / 逻辑删除（删除前走 production 引用校验）。
 */
@Service
@RequiredArgsConstructor
public class WorkCenterService {

    private static final String DEL_FLAG_NORMAL = "0";
    private static final String DEL_FLAG_DELETED = "1";
    private static final String STATUS_IDLE = "IDLE";
    private static final Set<String> VALID_STATUS = Set.of("IDLE", "BUSY", "OFFLINE");

    private final WorkCenterRepository workCenterRepository;
    private final ReferenceCheckClient referenceCheckClient;

    /** 分页查询：编码/名称模糊（二选一，编码优先）+ 状态精确 */
    public PageResult<WorkCenterVO> page(WorkCenterQueryRequest query) {
        Pageable pageable = PageRequest.of(query.getPageNum() - 1, query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        String code = trimToNull(query.getCenterCode());
        String name = trimToNull(query.getCenterName());
        String status = trimToNull(query.getStatus());

        Page<WorkCenter> page;
        if (code != null) {
            page = status != null
                    ? workCenterRepository.findByDelFlagAndCenterCodeContainingAndStatus(DEL_FLAG_NORMAL, code, status, pageable)
                    : workCenterRepository.findByDelFlagAndCenterCodeContaining(DEL_FLAG_NORMAL, code, pageable);
        } else if (name != null) {
            page = status != null
                    ? workCenterRepository.findByDelFlagAndCenterNameContainingAndStatus(DEL_FLAG_NORMAL, name, status, pageable)
                    : workCenterRepository.findByDelFlagAndCenterNameContaining(DEL_FLAG_NORMAL, name, pageable);
        } else if (status != null) {
            page = workCenterRepository.findByDelFlagAndStatus(DEL_FLAG_NORMAL, status, pageable);
        } else {
            page = workCenterRepository.findByDelFlag(DEL_FLAG_NORMAL, pageable);
        }
        return PageResult.of(page.getContent().stream().map(this::toVO).toList(),
                page.getTotalElements(), query.getPageNum(), query.getPageSize());
    }

    public WorkCenterVO getById(String id) {
        return toVO(loadActive(id));
    }

    @Transactional
    public WorkCenterVO create(WorkCenterCreateRequest request) {
        String code = request.getCenterCode().trim();
        if (workCenterRepository.existsByCenterCodeAndDelFlag(code, DEL_FLAG_NORMAL)) {
            throw new BusinessException(400, "工位编码已存在：" + code);
        }
        WorkCenter workCenter = new WorkCenter();
        workCenter.setId(UUID.randomUUID().toString());
        workCenter.setCenterCode(code);
        workCenter.setCenterName(request.getCenterName().trim());
        workCenter.setCenterType(trimToNull(request.getCenterType()));
        workCenter.setStatus(STATUS_IDLE);
        return toVO(workCenterRepository.save(workCenter));
    }

    @Transactional
    public WorkCenterVO update(String id, WorkCenterUpdateRequest request) {
        WorkCenter workCenter = loadActive(id);
        workCenter.setCenterName(request.getCenterName().trim());
        workCenter.setCenterType(trimToNull(request.getCenterType()));
        return toVO(workCenterRepository.save(workCenter));
    }

    /** 状态更新：派工开始置 BUSY、任务结束回 IDLE、手工停用置 OFFLINE（对外接口见设计规格 7.1） */
    @Transactional
    public WorkCenterVO updateStatus(String id, WorkCenterStatusRequest request) {
        if (!VALID_STATUS.contains(request.getStatus())) {
            throw new BusinessException(400, "非法的工位状态：" + request.getStatus());
        }
        WorkCenter workCenter = loadActive(id);
        workCenter.setStatus(request.getStatus());
        return toVO(workCenterRepository.save(workCenter));
    }

    /**
     * 逻辑删除：删除保护见《设计规格说明书》8.4（工位有进行中任务时禁止删除）。
     * 通过 ReferenceCheckClient 反查 production 的进行中派工任务数，> 0 拒绝删除；
     * fallback 返回 null 表示 production 不可用（安全优先，同样拒绝删除）。
     */
    @Transactional
    public void delete(String id) {
        WorkCenter workCenter = loadActive(id);
        Integer tasks = referenceCheckClient.countWorkCenterTasks(id);
        if (tasks == null) {
            throw new BusinessException(500, "生产服务暂不可用，无法校验进行中任务，删除已拒绝");
        }
        if (tasks > 0) {
            throw new BusinessException(400, "该工位有 " + tasks + " 个进行中的派工任务，不能删除");
        }
        workCenter.setDelFlag(DEL_FLAG_DELETED);
        workCenterRepository.save(workCenter);
    }

    private WorkCenter loadActive(String id) {
        return workCenterRepository.findById(id)
                .filter(w -> DEL_FLAG_NORMAL.equals(w.getDelFlag()))
                .orElseThrow(() -> new BusinessException(404, "工位不存在"));
    }

    private WorkCenterVO toVO(WorkCenter workCenter) {
        WorkCenterVO vo = new WorkCenterVO();
        vo.setId(workCenter.getId());
        vo.setCenterCode(workCenter.getCenterCode());
        vo.setCenterName(workCenter.getCenterName());
        vo.setCenterType(workCenter.getCenterType());
        vo.setOperatorId(workCenter.getOperatorId());
        vo.setStatus(workCenter.getStatus());
        vo.setCreatedAt(workCenter.getCreatedAt());
        vo.setUpdatedAt(workCenter.getUpdatedAt());
        return vo;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
