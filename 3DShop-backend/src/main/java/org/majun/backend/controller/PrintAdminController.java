package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.PrintJobAdjustRequest;
import org.majun.backend.dto.PrintJobDispatchRequest;
import org.majun.backend.dto.PrintJobQueryRequest;
import org.majun.backend.dto.PrintPrinterCreateRequest;
import org.majun.backend.dto.PrintPrinterUpdateRequest;
import org.majun.backend.service.PrintJobService;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PrintJobEventVO;
import org.majun.backend.vo.PrintJobVO;
import org.majun.backend.vo.PrintPrinterVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Print Admin", description = "打印排产管理")
@RestController
@RequestMapping("/api/print/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class PrintAdminController {

    private final PrintJobService printJobService;

    @Operation(summary = "任务列表")
    @PostMapping("/jobs")
    public Result<PageResult<PrintJobVO>> list(@RequestBody(required = false) PrintJobQueryRequest request) {
        if (request == null) {
            request = new PrintJobQueryRequest();
        }
        return Result.success(printJobService.adminList(request));
    }

    @Operation(summary = "打印机列表")
    @GetMapping("/printers")
    public Result<PageResult<PrintPrinterVO>> printers(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(printJobService.listPrinters(status, keyword, pageNum, pageSize));
    }

    @Operation(summary = "新增打印机")
    @PostMapping("/printers")
    public Result<Void> createPrinter(@Valid @RequestBody PrintPrinterCreateRequest request) {
        printJobService.createPrinter(request);
        return Result.success();
    }

    @Operation(summary = "更新打印机")
    @PutMapping("/printers")
    public Result<Void> updatePrinter(@Valid @RequestBody PrintPrinterUpdateRequest request) {
        printJobService.updatePrinter(request);
        return Result.success();
    }

    @Operation(summary = "删除打印机")
    @DeleteMapping("/printers/{id}")
    public Result<Void> deletePrinter(@PathVariable Long id) {
        printJobService.deletePrinter(id);
        return Result.success();
    }

    @Operation(summary = "手动排单")
    @PostMapping("/dispatch")
    public Result<Void> dispatch(@Valid @RequestBody PrintJobDispatchRequest request) {
        printJobService.dispatchManual(request);
        return Result.success();
    }

    @Operation(summary = "调整任务")
    @PutMapping("/adjust")
    public Result<Void> adjust(@Valid @RequestBody PrintJobAdjustRequest request) {
        printJobService.adjustJob(request);
        return Result.success();
    }

    @Operation(summary = "终止任务")
    @PostMapping("/stop/{jobId}")
    public Result<Void> stop(@PathVariable Long jobId) {
        printJobService.stopJob(jobId);
        return Result.success();
    }

    @Operation(summary = "重试任务")
    @PostMapping("/retry/{jobId}")
    public Result<Void> retry(@PathVariable Long jobId) {
        printJobService.retryJob(jobId);
        return Result.success();
    }

    @Operation(summary = "任务事件日志")
    @GetMapping("/jobs/{jobId}/events")
    public Result<List<PrintJobEventVO>> events(@PathVariable Long jobId,
                                                @RequestParam(required = false, defaultValue = "30") Integer limit) {
        return Result.success(printJobService.listJobEvents(jobId, limit));
    }
}
