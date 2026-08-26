package com.litemes.common.code;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 编号生成器：基于 Redis INCR 原子自增（见《设计规格说明书》5.3）。
 * 规则：key 按日期隔离（seq:{类型}:{yyyyMMdd}），INCR 后取序号左补零，
 * 拼接为「前缀-日期-序号」；key 过期时间 2 天自动清理。
 * 业务代码禁止直接操作 Redis 自增，必须通过本组件。
 */
@Component
@RequiredArgsConstructor
public class CodeGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long KEY_TTL_DAYS = 2;

    private final RedisTemplate<String, Object> redisTemplate;

    /** 生成工单编号：WO-yyyyMMdd-NNN（3 位序号） */
    public String generateWorkOrderNo() {
        return "WO-" + generate("seq:WORK_ORDER", 3);
    }

    /** 生成派工任务编号：TK-yyyyMMdd-NNNN（4 位序号） */
    public String generateDispatchTaskNo() {
        return "TK-" + generate("seq:DISPATCH_TASK", 4);
    }

    private String generate(String type, int width) {
        String date = LocalDate.now().format(DATE);
        String key = type + ":" + date;
        Long seq = redisTemplate.opsForValue().increment(key);
        // 首次自增后设置过期时间，避免历史 key 堆积
        if (seq != null && seq == 1L) {
            redisTemplate.expire(key, KEY_TTL_DAYS, TimeUnit.DAYS);
        }
        String seqStr = String.format("%0" + width + "d", seq);
        return date + "-" + seqStr;
    }
}
