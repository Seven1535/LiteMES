package com.litemes.production.feign;

import com.litemes.production.module.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 引用计数内部接口：实现 litemes-api 的 ReferenceCheckClient 契约（唯一的反向调用：base → production）。
 * 用途：基础数据删除保护（设计规格 8.4）——删除产品前查工单引用，删除工位前查进行中任务。
 * 不经网关，由 JwtAuthFilter 识别 Feign 透传的 X-User-Id Header 放行。
 */
@RestController
@RequestMapping("/inner/references")
@RequiredArgsConstructor
public class ReferenceCheckInnerController {

    private final WorkOrderRepository workOrderRepository;

    /** 对应契约：产品被工单引用计数（含逻辑删除的历史工单，安全优先） */
    @GetMapping("/products/{productId}")
    public Integer countProductReferences(@PathVariable String productId) {
        return (int) workOrderRepository.countByProductId(productId);
    }

    /** 对应契约：工位进行中的派工任务数（派工模块开发后接入，当前无任务恒为 0） */
    @GetMapping("/workcenters/{workCenterId}")
    public Integer countWorkCenterTasks(@PathVariable String workCenterId) {
        // TODO 派工模块：dispatchTaskRepository.countByWorkCenterIdAndStatus(workCenterId, "PROCESSING")
        return 0;
    }
}
