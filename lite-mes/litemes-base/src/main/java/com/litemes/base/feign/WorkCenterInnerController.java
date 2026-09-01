package com.litemes.base.feign;

import com.litemes.api.base.dto.WorkCenterDTO;
import com.litemes.base.module.workcenter.entity.WorkCenter;
import com.litemes.base.module.workcenter.repository.WorkCenterRepository;
import com.litemes.common.core.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 工位内部接口：实现 litemes-api 的 WorkCenterClient 契约（供 litemes-production 派工时校验/推荐工位）。
 * 不经网关，由 JwtAuthFilter 识别 Feign 透传的 X-User-Id Header 放行。
 */
@RestController
@RequestMapping("/inner")
@RequiredArgsConstructor
public class WorkCenterInnerController {

    private static final String DEL_FLAG_NORMAL = "0";
    private static final Set<String> VALID_STATUS = Set.of("IDLE", "BUSY", "OFFLINE");

    private final WorkCenterRepository workCenterRepository;

    /** 对应契约：GET /inner/workcenters（可按类型筛选，用于派工推荐） */
    @GetMapping("/workcenters")
    public List<WorkCenterDTO> listWorkCenters(@RequestParam(value = "type", required = false) String type) {
        List<WorkCenter> list = (type == null || type.isBlank())
                ? workCenterRepository.findByDelFlag(DEL_FLAG_NORMAL)
                : workCenterRepository.findByDelFlagAndCenterType(DEL_FLAG_NORMAL, type.trim());
        return list.stream().map(this::toDTO).toList();
    }

    /** 对应契约：GET /inner/workcenters/{id} */
    @GetMapping("/workcenters/{id}")
    public WorkCenterDTO getWorkCenter(@PathVariable String id) {
        WorkCenter workCenter = workCenterRepository.findById(id)
                .filter(w -> DEL_FLAG_NORMAL.equals(w.getDelFlag()))
                .orElseThrow(() -> new BusinessException(404, "工位不存在"));
        return toDTO(workCenter);
    }

    /** 对应契约：PUT /inner/workcenters/{id}/status（派工开工置 BUSY、任务结束回 IDLE） */
    @PutMapping("/workcenters/{id}/status")
    public WorkCenterDTO changeStatus(@PathVariable String id, @RequestParam String status) {
        if (!VALID_STATUS.contains(status)) {
            throw new BusinessException(400, "非法的工位状态：" + status);
        }
        WorkCenter workCenter = workCenterRepository.findById(id)
                .filter(w -> DEL_FLAG_NORMAL.equals(w.getDelFlag()))
                .orElseThrow(() -> new BusinessException(404, "工位不存在"));
        workCenter.setStatus(status);
        return toDTO(workCenterRepository.save(workCenter));
    }

    private WorkCenterDTO toDTO(WorkCenter workCenter) {
        WorkCenterDTO dto = new WorkCenterDTO();
        dto.setId(workCenter.getId());
        dto.setCenterCode(workCenter.getCenterCode());
        dto.setCenterName(workCenter.getCenterName());
        dto.setCenterType(workCenter.getCenterType());
        dto.setStatus(workCenter.getStatus());
        return dto;
    }
}
