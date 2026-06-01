package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.OperationLogQueryRequest;
import org.majun.backend.service.OperationLogService;
import org.majun.backend.vo.OperationLogVO;
import org.majun.backend.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志管理控制器
 */
@Tag(name = "操作日志管理", description = "操作日志管理接口")
@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    /** 分页查询操作日志 */
    @Operation(summary = "分页查询操作日志", description = "管理员分页查询操作日志")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/page")
    public Result<PageResult<OperationLogVO>> queryPage(@RequestBody(required = false) OperationLogQueryRequest request) {
        if (request == null) {
            request = new OperationLogQueryRequest();
        }
        return Result.success(operationLogService.queryPage(request));
    }

    /** 查询日志详情 */
    @Operation(summary = "查询日志详情", description = "管理员查询日志详情")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{id}")
    public Result<OperationLogVO> getDetail(@PathVariable Long id) {
        OperationLogVO detail = operationLogService.getDetail(id);
        return detail != null ? Result.success(detail) : Result.fail("日志不存在");
    }

    /** 清理历史日志 */
    @Operation(summary = "清理历史日志", description = "管理员清理指定天数前的日志")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/clean")
    public Result<Integer> cleanOldLogs(@RequestParam(defaultValue = "90") Integer days) {
        if (days < 30) {
            return Result.fail("保留天数不能少于30天");
        }
        int count = operationLogService.cleanOldLogs(days);
        return Result.success("清理完成，共删除" + count + "条日志", count);
    }
}
