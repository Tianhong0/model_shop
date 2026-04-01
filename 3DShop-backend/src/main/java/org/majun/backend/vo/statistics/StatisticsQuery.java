package org.majun.backend.vo.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "统计查询参数")
public class StatisticsQuery {

    @Schema(description = "开始日期", example = "2026-03-01")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2026-03-31")
    private LocalDate endDate;

    public static StatisticsQuery of(LocalDate start, LocalDate end) {
        StatisticsQuery query = new StatisticsQuery();
        LocalDate today = LocalDate.now();
        query.startDate = start != null ? start : today.minusDays(30);
        query.endDate = end != null ? end : today;
        return query;
    }

    public LocalDateTime getStartDateTime() {
        return startDate.atStartOfDay();
    }

    public LocalDateTime getEndDateTime() {
        return endDate.plusDays(1).atStartOfDay();
    }

    public String getDateRangeKey() {
        return startDate.toString() + ":" + endDate.toString();
    }
}
