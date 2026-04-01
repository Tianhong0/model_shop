package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.PrintJobAdjustRequest;
import org.majun.backend.dto.PrintJobDispatchRequest;
import org.majun.backend.dto.PrintJobQueryRequest;
import org.majun.backend.dto.PrintPrinterCreateRequest;
import org.majun.backend.dto.PrintPrinterUpdateRequest;
import org.majun.backend.entity.PrintJob;
import org.majun.backend.entity.PrintJobEvent;
import org.majun.backend.entity.PrintPrinter;
import org.majun.backend.entity.SysModel;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.enums.PrintJobStatus;
import org.majun.backend.enums.PrintPrinterStatus;
import org.majun.backend.event.PrintJobDoneEvent;
import org.majun.backend.event.PrintJobRetryEvent;
import org.majun.backend.repository.PrintJobEventRepository;
import org.majun.backend.repository.PrintJobRepository;
import org.majun.backend.repository.PrintPrinterRepository;
import org.majun.backend.repository.SysModelRepository;
import org.majun.backend.repository.SysOrderRepository;
import org.majun.backend.service.OctoPrintService;
import org.majun.backend.service.PrintJobService;
import org.majun.backend.service.PrintWebSocketService;
import org.majun.backend.service.SlicerService;
import org.majun.backend.util.MinioUtil;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PrintJobEventVO;
import org.majun.backend.vo.PrintJobProgressVO;
import org.majun.backend.vo.PrintJobVO;
import org.majun.backend.vo.PrintPrinterVO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrintJobServiceImpl implements PrintJobService {

    private final PrintJobRepository printJobRepository;
    private final PrintPrinterRepository printPrinterRepository;
    private final PrintJobEventRepository printJobEventRepository;
    private final SysOrderRepository orderRepository;
    private final SysModelRepository modelRepository;
    private final SlicerService slicerService;
    private final OctoPrintService octoPrintService;
    private final PrintWebSocketService printWebSocketService;
    private final MinioUtil minioUtil;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void createAndDispatchFromPaidOrder(Long orderId) {
        if (orderId == null) {
            return;
        }
        PrintJob existed = printJobRepository.selectOne(
                new LambdaQueryWrapper<PrintJob>()
                        .eq(PrintJob::getOrderId, orderId)
                        .eq(PrintJob::getIsDelete, 0)
                        .last("limit 1")
        );
        if (existed != null) {
            return;
        }

        SysOrder order = orderRepository.selectById(orderId);
        if (order == null || Objects.equals(order.getIsDelete(), 1)) {
            return;
        }

        SysModel model = modelRepository.selectById(order.getModelId());
        if (model == null || Objects.equals(model.getIsDelete(), 1)) {
            throw new BusinessException("模型不存在，无法创建打印任务");
        }

        SliceParams params = extractSliceParams(order.getCustomParams());
        ModelSource modelSource = extractModelSource(order.getCustomParams(), model.getFilePath());

        PrintJob job = new PrintJob();
        job.setOrderId(order.getId());
        job.setOrderSn(order.getOrderSn());
        job.setModelId(model.getId());
        job.setModelName(model.getModelName());
        job.setModelFileName(buildOrderModelFileName(order.getOrderSn(), modelSource.fileType()));
        job.setModelFileType(modelSource.fileType());
        job.setLayerHeight(params.layerHeight());
        job.setFillDensity(params.fillDensity());
        job.setFilamentDiameter(params.filamentDiameter());
        job.setPriority(params.priority());
        job.setStatus(PrintJobStatus.QUEUED.getCode());
        job.setProgress(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        job.setIsDelete(0);
        printJobRepository.insert(job);
        saveEvent(job.getId(), "QUEUED", "支付成功自动创建任务", null);
        pushProgress(job);

        runPipeline(job.getId(), order.getPrinterId());
    }

    @Override
    public PageResult<PrintJobVO> adminList(PrintJobQueryRequest request) {
        LambdaQueryWrapper<PrintJob> query = new LambdaQueryWrapper<PrintJob>()
                .eq(PrintJob::getIsDelete, 0)
                .orderByDesc(PrintJob::getPriority)
                .orderByAsc(PrintJob::getCreateTime);

        if (request.getStatus() != null) {
            query.eq(PrintJob::getStatus, request.getStatus());
        }
        if (request.getPrinterId() != null) {
            query.eq(PrintJob::getPrinterId, request.getPrinterId());
        }
        if (StringUtils.hasText(request.getOrderSn())) {
            query.like(PrintJob::getOrderSn, request.getOrderSn());
        }

        Page<PrintJob> page = new Page<>(request.getPageNum(), request.getPageSize());
        printJobRepository.selectPage(page, query);
        if (page.getRecords() == null || page.getRecords().isEmpty()) {
            return PageResult.<PrintJobVO>builder()
                    .records(Collections.emptyList())
                    .total(0L)
                    .pageNum(request.getPageNum())
                    .pageSize(request.getPageSize())
                    .pages(0)
                    .build();
        }

        Map<Long, String> printerNameMap = loadPrinterNames(page.getRecords().stream()
                .map(PrintJob::getPrinterId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        List<PrintJobVO> records = page.getRecords().stream()
                .map(job -> toVo(job, printerNameMap.get(job.getPrinterId())))
                .collect(Collectors.toList());

        return PageResult.<PrintJobVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public PageResult<PrintPrinterVO> listPrinters(Integer status, String keyword, Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<PrintPrinter> queryWrapper = new LambdaQueryWrapper<PrintPrinter>()
                .eq(PrintPrinter::getIsDelete, 0)
                .orderByAsc(PrintPrinter::getSort)
                .orderByAsc(PrintPrinter::getId);

        if (status != null) {
            queryWrapper.eq(PrintPrinter::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            String keywordValue = keyword.trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(PrintPrinter::getPrinterName, keywordValue)
                    .or()
                    .like(PrintPrinter::getBaseUrl, keywordValue)
                    .or()
                    .like(PrintPrinter::getPrinterCode, keywordValue));
        }

        Page<PrintPrinter> page = new Page<>(currentPage, currentSize);
        printPrinterRepository.selectPage(page, queryWrapper);

        List<PrintPrinterVO> result = new ArrayList<>();
        for (PrintPrinter printer : page.getRecords()) {
            PrintPrinterVO vo = new PrintPrinterVO();
            vo.setId(printer.getId());
            vo.setPrinterCode(printer.getPrinterCode());
            vo.setPrinterName(printer.getPrinterName());
            vo.setBaseUrl(printer.getBaseUrl());
            vo.setAuthHeaderKey(printer.getAuthHeaderKey());
            vo.setStatus(printer.getStatus());
            vo.setStatusDesc(printPrinterStatusText(printer.getStatus()));
            vo.setCurrentJobId(printer.getCurrentJobId());
            vo.setSort(printer.getSort());
            result.add(vo);
        }

        return PageResult.<PrintPrinterVO>builder()
                .records(result)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPrinter(PrintPrinterCreateRequest request) {
        String ip = request.getIp() == null ? "" : request.getIp().trim();
        if (!isValidIp(ip)) {
            throw new BusinessException("IP地址格式不正确");
        }

        int port = request.getPort() == null ? 5000 : request.getPort();
        String baseUrl = buildBaseUrl(ip, port, request.getHttps());

        long exists = printPrinterRepository.selectCount(
                new LambdaQueryWrapper<PrintPrinter>()
                        .eq(PrintPrinter::getBaseUrl, baseUrl)
                        .eq(PrintPrinter::getIsDelete, 0)
        );
        if (exists > 0) {
            throw new BusinessException("该打印机地址已存在");
        }

        String authHeaderKey = StringUtils.hasText(request.getAuthHeaderKey()) ? request.getAuthHeaderKey().trim() : "X-Api-Key";
        String authHeaderValue = StringUtils.hasText(request.getAuthHeaderValue()) ? request.getAuthHeaderValue().trim() : null;
        octoPrintService.verifyConnection(baseUrl, authHeaderKey, authHeaderValue);

        PrintPrinter printer = new PrintPrinter();
        printer.setPrinterName(request.getPrinterName());
        printer.setPrinterCode(generatePrinterCode(ip, port));
        printer.setBaseUrl(baseUrl);
        printer.setAuthHeaderKey(authHeaderKey);
        printer.setAuthHeaderValue(authHeaderValue);
        printer.setStatus(PrintPrinterStatus.IDLE.getCode());
        printer.setSort(request.getSort() == null ? 0 : request.getSort());
        printer.setIsDelete(0);
        printPrinterRepository.insert(printer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePrinter(PrintPrinterUpdateRequest request) {
        PrintPrinter printer = printPrinterRepository.selectById(request.getId());
        if (printer == null || Objects.equals(printer.getIsDelete(), 1)) {
            throw new BusinessException("打印机不存在");
        }

        String ip = request.getIp() == null ? "" : request.getIp().trim();
        if (!isValidIp(ip)) {
            throw new BusinessException("IP地址格式不正确");
        }
        int port = request.getPort() == null ? 5000 : request.getPort();
        String baseUrl = buildBaseUrl(ip, port, request.getHttps());

        long exists = printPrinterRepository.selectCount(
                new LambdaQueryWrapper<PrintPrinter>()
                        .eq(PrintPrinter::getBaseUrl, baseUrl)
                        .eq(PrintPrinter::getIsDelete, 0)
                        .ne(PrintPrinter::getId, request.getId())
        );
        if (exists > 0) {
            throw new BusinessException("该打印机地址已被其他设备使用");
        }

        String authHeaderKey = StringUtils.hasText(request.getAuthHeaderKey()) ? request.getAuthHeaderKey().trim() : "X-Api-Key";
        String authHeaderValue = StringUtils.hasText(request.getAuthHeaderValue()) ? request.getAuthHeaderValue().trim() : null;
        octoPrintService.verifyConnection(baseUrl, authHeaderKey, authHeaderValue);

        printer.setPrinterName(request.getPrinterName());
        printer.setBaseUrl(baseUrl);
        printer.setAuthHeaderKey(authHeaderKey);
        printer.setAuthHeaderValue(authHeaderValue);
        if (request.getSort() != null) {
            printer.setSort(request.getSort());
        }
        if (request.getStatus() != null) {
            printer.setStatus(request.getStatus());
        }
        printPrinterRepository.updateById(printer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePrinter(Long id) {
        PrintPrinter printer = printPrinterRepository.selectById(id);
        if (printer == null || Objects.equals(printer.getIsDelete(), 1)) {
            throw new BusinessException("打印机不存在");
        }
        if (Objects.equals(printer.getStatus(), PrintPrinterStatus.BUSY.getCode())) {
            throw new BusinessException("打印机正在工作中，无法删除");
        }
        long running = printJobRepository.selectCount(
                new LambdaQueryWrapper<PrintJob>()
                        .eq(PrintJob::getPrinterId, id)
                        .eq(PrintJob::getIsDelete, 0)
                        .in(PrintJob::getStatus,
                                PrintJobStatus.SLICING.getCode(),
                                PrintJobStatus.READY_TO_PRINT.getCode(),
                                PrintJobStatus.PRINTING.getCode(),
                                PrintJobStatus.PAUSED.getCode())
        );
        if (running > 0) {
            throw new BusinessException("该打印机关联任务正在执行，无法删除");
        }

        printPrinterRepository.update(null,
                new LambdaUpdateWrapper<PrintPrinter>()
                        .eq(PrintPrinter::getId, id)
                        .set(PrintPrinter::getIsDelete, 1)
        );
    }

    @Override
    public void dispatchManual(PrintJobDispatchRequest request) {
        SysOrder order = orderRepository.selectById(request.getOrderId());
        if (order == null || Objects.equals(order.getIsDelete(), 1)) {
            throw new BusinessException("订单不存在");
        }

        PrintJob job = printJobRepository.selectOne(
                new LambdaQueryWrapper<PrintJob>()
                        .eq(PrintJob::getOrderId, request.getOrderId())
                        .eq(PrintJob::getIsDelete, 0)
                        .last("limit 1")
        );

        if (job == null) {
            SysModel model = modelRepository.selectById(order.getModelId());
            if (model == null) {
                throw new BusinessException("模型不存在");
            }
            job = new PrintJob();
            job.setOrderId(order.getId());
            job.setOrderSn(order.getOrderSn());
            job.setModelId(model.getId());
            job.setModelName(model.getModelName());
            ModelSource source = extractModelSource(order.getCustomParams(), model.getFilePath());
            job.setModelFileName(buildOrderModelFileName(order.getOrderSn(), source.fileType()));
            job.setModelFileType(source.fileType());
            job.setIsDelete(0);
        }

        job.setLayerHeight(BigDecimal.valueOf(request.getLayerHeight()).setScale(3, RoundingMode.HALF_UP));
        job.setFillDensity(request.getFillDensity());
        job.setFilamentDiameter(BigDecimal.valueOf(request.getFilamentDiameter()).setScale(3, RoundingMode.HALF_UP));
        job.setPriority(request.getPriority() == null ? 1 : request.getPriority());
        job.setPrinterId(request.getPrinterId());
        job.setStatus(PrintJobStatus.QUEUED.getCode());
        job.setErrorMessage(null);
        if (job.getId() == null) {
            printJobRepository.insert(job);
        } else {
            printJobRepository.updateById(job);
        }
        pushProgress(job);

        runPipeline(job.getId(), request.getPrinterId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustJob(PrintJobAdjustRequest request) {
        PrintJob job = getJobOrThrow(request.getJobId());
        if (!Objects.equals(job.getStatus(), PrintJobStatus.QUEUED.getCode())) {
            throw new BusinessException("仅排队中的任务允许调整");
        }
        if (request.getPriority() != null) {
            job.setPriority(request.getPriority());
        }
        if (request.getPrinterId() != null) {
            job.setPrinterId(request.getPrinterId());
        }
        printJobRepository.updateById(job);
        saveEvent(job.getId(), "ADJUSTED", "任务参数已调整", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stopJob(Long jobId) {
        PrintJob job = getJobOrThrow(jobId);

        if (Objects.equals(job.getStatus(), PrintJobStatus.PRINTING.getCode())) {
            PrintPrinter printer = job.getPrinterId() == null ? null : printPrinterRepository.selectById(job.getPrinterId());
            if (printer != null && StringUtils.hasText(printer.getBaseUrl())) {
                octoPrintService.cancelCurrent(printer.getBaseUrl(), printer.getAuthHeaderKey(), printer.getAuthHeaderValue());
            }
        }

        releasePrinter(job.getPrinterId());
        job.setStatus(PrintJobStatus.CANCELED.getCode());
        job.setFinishedAt(LocalDateTime.now());
        printJobRepository.updateById(job);
        saveEvent(job.getId(), "CANCELED", "管理员终止任务", null);
        pushProgress(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJob(Long jobId) {
        PrintJob job = getJobOrThrow(jobId);

        // 如果正在打印，先取消 OctoPrint 任务
        if (Objects.equals(job.getStatus(), PrintJobStatus.PRINTING.getCode())) {
            PrintPrinter printer = job.getPrinterId() == null ? null : printPrinterRepository.selectById(job.getPrinterId());
            if (printer != null && StringUtils.hasText(printer.getBaseUrl())) {
                try {
                    octoPrintService.cancelCurrent(printer.getBaseUrl(), printer.getAuthHeaderKey(), printer.getAuthHeaderValue());
                } catch (Exception ex) {
                    log.warn("删除任务时取消 OctoPrint 打印失败 jobId={}", jobId, ex);
                }
            }
        }

        releasePrinter(job.getPrinterId());
        job.setFinishedAt(LocalDateTime.now());
        printJobRepository.updateById(job);
        printJobRepository.deleteById(jobId);
        saveEvent(job.getId(), "DELETED", "管理员删除任务", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryJob(Long jobId) {
        PrintJob job = getJobOrThrow(jobId);
        Integer status = job.getStatus();
        if (!Objects.equals(status, PrintJobStatus.SLICE_FAILED.getCode())
                && !Objects.equals(status, PrintJobStatus.FAILED.getCode())
                && !Objects.equals(status, PrintJobStatus.CANCELED.getCode())) {
            throw new BusinessException("仅失败/已取消任务允许重试");
        }

        job.setStatus(PrintJobStatus.QUEUED.getCode());
        job.setErrorMessage(null);
        job.setProgress(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        job.setEstimatedSecondsLeft(null);
        job.setToolTempActual(null);
        job.setToolTempTarget(null);
        job.setBedTempActual(null);
        job.setBedTempTarget(null);
        job.setGcodeFileName(null);
        job.setOctoprintJobId(null);
        job.setStartedAt(null);
        job.setFinishedAt(null);
        printJobRepository.updateById(job);

        saveEvent(job.getId(), "RETRY", "管理员重试任务", null);
        pushProgress(job);
        applicationEventPublisher.publishEvent(new PrintJobRetryEvent(job.getId(), job.getPrinterId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncAndBroadcastRunningJobs() {
        List<PrintJob> jobs = printJobRepository.selectList(
                new LambdaQueryWrapper<PrintJob>()
                        .eq(PrintJob::getIsDelete, 0)
                        .in(PrintJob::getStatus,
                                PrintJobStatus.PRINTING.getCode(),
                                PrintJobStatus.PAUSED.getCode(),
                                PrintJobStatus.READY_TO_PRINT.getCode())
                        .isNotNull(PrintJob::getPrinterId)
        );

        for (PrintJob job : jobs) {
            PrintPrinter printer = printPrinterRepository.selectById(job.getPrinterId());
            if (printer == null || !StringUtils.hasText(printer.getBaseUrl())) {
                continue;
            }
            try {
                Integer previousStatus = job.getStatus();
                OctoPrintService.OctoPrintStatus status = octoPrintService.fetchStatus(
                        printer.getBaseUrl(),
                        printer.getAuthHeaderKey(),
                        printer.getAuthHeaderValue()
                );
                applyOctoStatus(job, status);
                printJobRepository.updateById(job);
                if (!Objects.equals(previousStatus, PrintJobStatus.DONE.getCode())
                        && Objects.equals(job.getStatus(), PrintJobStatus.DONE.getCode())) {
                    log.info("打印任务完成，发布PrintJobDoneEvent: jobId={}, orderId={}, orderSn={}",
                            job.getId(), job.getOrderId(), job.getOrderSn());
                    applicationEventPublisher.publishEvent(new PrintJobDoneEvent(job.getId(), job.getOrderId(), job.getOrderSn()));
                }
                if (Objects.equals(job.getStatus(), PrintJobStatus.DONE.getCode()) || Objects.equals(job.getStatus(), PrintJobStatus.FAILED.getCode())) {
                    releasePrinter(job.getPrinterId());
                }
                pushProgress(job);
            } catch (Exception ex) {
                log.warn("同步打印状态失败 jobId={}", job.getId(), ex);
            }
        }

        dispatchQueuedJobs();
    }

    @Override
    public List<PrintJobEventVO> listJobEvents(Long jobId, Integer limit) {
        if (jobId == null) {
            throw new BusinessException("任务ID不能为空");
        }
        int safeLimit = (limit == null || limit <= 0) ? 30 : Math.min(limit, 200);
        List<PrintJobEvent> list = printJobEventRepository.selectList(
                new LambdaQueryWrapper<PrintJobEvent>()
                        .eq(PrintJobEvent::getJobId, jobId)
                        .orderByDesc(PrintJobEvent::getCreateTime)
                        .last("limit " + safeLimit)
        );
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(item -> {
            PrintJobEventVO vo = new PrintJobEventVO();
            vo.setId(item.getId());
            vo.setJobId(item.getJobId());
            vo.setEventType(item.getEventType());
            vo.setEventMessage(item.getEventMessage());
            vo.setEventPayload(item.getEventPayload());
            vo.setCreateTime(item.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private void dispatchQueuedJobs() {
        List<PrintPrinter> idlePrinters = printPrinterRepository.selectList(
                new LambdaQueryWrapper<PrintPrinter>()
                        .eq(PrintPrinter::getIsDelete, 0)
                        .eq(PrintPrinter::getStatus, PrintPrinterStatus.IDLE.getCode())
                        .orderByAsc(PrintPrinter::getSort)
                        .orderByAsc(PrintPrinter::getId)
        );
        if (idlePrinters == null || idlePrinters.isEmpty()) {
            return;
        }

        for (PrintPrinter printer : idlePrinters) {
            PrintJob queued = printJobRepository.selectOne(
                    new LambdaQueryWrapper<PrintJob>()
                            .eq(PrintJob::getIsDelete, 0)
                            .eq(PrintJob::getStatus, PrintJobStatus.QUEUED.getCode())
                            .and(w -> w.isNull(PrintJob::getPrinterId).or().eq(PrintJob::getPrinterId, printer.getId()))
                            .orderByDesc(PrintJob::getPriority)
                            .orderByAsc(PrintJob::getCreateTime)
                            .last("limit 1")
            );
            if (queued == null) {
                continue;
            }
            runPipeline(queued.getId(), printer.getId());
        }
    }

    @Override
    public void runPipeline(Long jobId, Long preferredPrinterId) {
        // ── Phase 1: 短事务（< 100ms）— 预留打印机 + 标记 SLICING ──
        PipelineContext ctx = transactionTemplate.execute(status -> {
            PrintJob job = getJobOrThrow(jobId);
            Long reservedPrinterId = reservePrinter(preferredPrinterId);
            if (reservedPrinterId == null) {
                saveEvent(job.getId(), "WAIT_PRINTER", "暂无空闲打印机，等待排产", null);
                pushProgress(job);
                return null;
            }

            PrintPrinter printer = printPrinterRepository.selectById(reservedPrinterId);
            if (printer == null) {
                throw new BusinessException("打印机不存在");
            }

            printPrinterRepository.update(null,
                new LambdaUpdateWrapper<PrintPrinter>()
                    .eq(PrintPrinter::getId, reservedPrinterId)
                    .set(PrintPrinter::getCurrentJobId, jobId));

            updateJobStatus(job.getId(), PrintJobStatus.SLICING.getCode(), null);
            saveEvent(job.getId(), "SLICING", "开始切片", null);

            return new PipelineContext(
                reservedPrinterId,
                job.getLayerHeight(),
                job.getFillDensity(),
                job.getFilamentDiameter(),
                printer.getBaseUrl(),
                printer.getAuthHeaderKey(),
                printer.getAuthHeaderValue()
            );
        });

        if (ctx == null) {
            return;
        }

        pushProgress(getJobOrThrow(jobId));

        // ── Phase 2: 无事务（10-30s）— 文件准备 + Slic3r 切片 ──
        String gcode;
        try {
            PrintJob fresh = getJobOrThrow(jobId);
            String localModelFileName = ensureModelLocalFile(fresh);
            gcode = slicerService.executeSlice(
                localModelFileName,
                ctx.layerHeight().doubleValue(),
                ctx.fillDensity(),
                ctx.filamentDiameter().doubleValue()
            );
        } catch (Exception sliceEx) {
            log.error("切片失败 jobId={}", jobId, sliceEx);
            transactionTemplate.executeWithoutResult(status -> {
                releasePrinter(ctx.printerId());
                updateJobStatus(jobId, PrintJobStatus.SLICE_FAILED.getCode(), sliceEx.getMessage());
                saveEvent(jobId, "SLICE_FAILED", "切片失败", sliceEx.getMessage());
            });
            pushProgress(getJobOrThrow(jobId));
            return;
        }

        // ── Phase 3: 短事务（< 100ms）— 更新切片结果 + 提交打印 ──
        try {
            transactionTemplate.executeWithoutResult(status -> {
                PrintJob latest = getJobOrThrow(jobId);
                latest.setGcodeFileName(gcode);
                latest.setPrinterId(ctx.printerId());
                latest.setStatus(PrintJobStatus.READY_TO_PRINT.getCode());
                latest.setStartedAt(LocalDateTime.now());
                printJobRepository.updateById(latest);
                saveEvent(latest.getId(), "SLICED", "切片完成", gcode);
            });
            pushProgress(getJobOrThrow(jobId));

            // OctoPrint 上传也在事务外执行，避免网络延迟持锁
            octoPrintService.uploadAndStartPrint(
                ctx.printerBaseUrl(),
                gcode,
                buildWorkPath(gcode),
                ctx.printerAuthKey(),
                ctx.printerAuthValue()
            );

            transactionTemplate.executeWithoutResult(status -> {
                PrintJob latest = getJobOrThrow(jobId);
                latest.setStatus(PrintJobStatus.PRINTING.getCode());
                latest.setProgress(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                printJobRepository.updateById(latest);
                saveEvent(latest.getId(), "PRINTING", "已提交 OctoPrint 打印", null);
            });
            pushProgress(getJobOrThrow(jobId));
        } catch (Exception ex) {
            log.error("提交打印失败 jobId={}", jobId, ex);
            transactionTemplate.executeWithoutResult(status -> {
                releasePrinter(ctx.printerId());
                updateJobStatus(jobId, PrintJobStatus.FAILED.getCode(), ex.getMessage());
                saveEvent(jobId, "FAILED", "任务失败", ex.getMessage());
            });
            pushProgress(getJobOrThrow(jobId));
        }
    }

    private void applyOctoStatus(PrintJob job, OctoPrintService.OctoPrintStatus status) {
        job.setProgress(status.progress() == null ? BigDecimal.ZERO : status.progress());
        job.setEstimatedSecondsLeft(status.estimatedSecondsLeft());
        job.setToolTempActual(status.toolActual());
        job.setToolTempTarget(status.toolTarget());
        job.setBedTempActual(status.bedActual());
        job.setBedTempTarget(status.bedTarget());

        String state = status.state() == null ? "" : status.state().toLowerCase();
        if (state.contains("printing")) {
            job.setStatus(PrintJobStatus.PRINTING.getCode());
        } else if (state.contains("paused")) {
            job.setStatus(PrintJobStatus.PAUSED.getCode());
        } else if (state.contains("error") || state.contains("offline")) {
            job.setStatus(PrintJobStatus.FAILED.getCode());
            job.setErrorMessage(status.state());
            job.setFinishedAt(LocalDateTime.now());
        } else if (state.contains("operational") && status.progress() != null && status.progress().compareTo(BigDecimal.valueOf(99.9)) >= 0) {
            job.setStatus(PrintJobStatus.DONE.getCode());
            job.setProgress(BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP));
            job.setFinishedAt(LocalDateTime.now());
            saveEvent(job.getId(), "DONE", "打印完成", null);
        }
    }

    private Long reservePrinter(Long preferredPrinterId) {
        if (preferredPrinterId != null) {
            int updated = printPrinterRepository.update(null,
                    new LambdaUpdateWrapper<PrintPrinter>()
                            .eq(PrintPrinter::getId, preferredPrinterId)
                            .eq(PrintPrinter::getIsDelete, 0)
                            .eq(PrintPrinter::getStatus, PrintPrinterStatus.IDLE.getCode())
                            .set(PrintPrinter::getStatus, PrintPrinterStatus.BUSY.getCode())
            );
            if (updated > 0) {
                return preferredPrinterId;
            }
        }

        List<PrintPrinter> printers = printPrinterRepository.selectList(
                new LambdaQueryWrapper<PrintPrinter>()
                        .eq(PrintPrinter::getIsDelete, 0)
                        .eq(PrintPrinter::getStatus, PrintPrinterStatus.IDLE.getCode())
                        .orderByAsc(PrintPrinter::getSort)
                        .orderByAsc(PrintPrinter::getId)
        );

        for (PrintPrinter printer : printers) {
            int updated = printPrinterRepository.update(null,
                    new LambdaUpdateWrapper<PrintPrinter>()
                            .eq(PrintPrinter::getId, printer.getId())
                            .eq(PrintPrinter::getStatus, PrintPrinterStatus.IDLE.getCode())
                            .set(PrintPrinter::getStatus, PrintPrinterStatus.BUSY.getCode())
            );
            if (updated > 0) {
                return printer.getId();
            }
        }
        return null;
    }

    private void releasePrinter(Long printerId) {
        if (printerId == null) {
            return;
        }
        printPrinterRepository.update(null,
                new LambdaUpdateWrapper<PrintPrinter>()
                        .eq(PrintPrinter::getId, printerId)
                        .set(PrintPrinter::getStatus, PrintPrinterStatus.IDLE.getCode())
                        .set(PrintPrinter::getCurrentJobId, null)
        );
    }

    private void updateJobStatus(Long jobId, Integer status, String errorMessage) {
        LambdaUpdateWrapper<PrintJob> updateWrapper = new LambdaUpdateWrapper<PrintJob>()
                .eq(PrintJob::getId, jobId)
                .set(PrintJob::getStatus, status)
                .set(PrintJob::getErrorMessage, errorMessage);
        if (Objects.equals(status, PrintJobStatus.SLICE_FAILED.getCode())
                || Objects.equals(status, PrintJobStatus.FAILED.getCode())
                || Objects.equals(status, PrintJobStatus.CANCELED.getCode())) {
            updateWrapper.set(PrintJob::getFinishedAt, LocalDateTime.now());
        }
        printJobRepository.update(null, updateWrapper);
    }

    private void updateJobStatusAndPush(Long jobId, Integer status, String errorMessage) {
        updateJobStatus(jobId, status, errorMessage);
        PrintJob latest = getJobOrThrow(jobId);
        pushProgress(latest);
    }

    private String buildWorkPath(String fileName) {
        String workDir = slicerService.getWorkdir();
        if (!StringUtils.hasText(workDir)) {
            return fileName;
        }
        if (workDir.endsWith("/") || workDir.endsWith("\\")) {
            return workDir + fileName;
        }
        return workDir + "/" + fileName;
    }

    private SliceParams extractSliceParams(String customParams) {
        BigDecimal layerHeight = BigDecimal.valueOf(0.2).setScale(3, RoundingMode.HALF_UP);
        Integer fillDensity = 15;
        BigDecimal filamentDiameter = BigDecimal.valueOf(1.75).setScale(3, RoundingMode.HALF_UP);
        Integer priority = 1;

        if (!StringUtils.hasText(customParams)) {
            return new SliceParams(layerHeight, fillDensity, filamentDiameter, priority);
        }

        try {
            JsonNode node = objectMapper.readTree(customParams);
            if (node.hasNonNull("layerHeight")) {
                layerHeight = BigDecimal.valueOf(node.get("layerHeight").asDouble()).setScale(3, RoundingMode.HALF_UP);
            }
            if (node.hasNonNull("fillDensity")) {
                fillDensity = node.get("fillDensity").asInt();
            } else if (node.hasNonNull("fillPercent")) {
                fillDensity = BigDecimal.valueOf(node.get("fillPercent").asDouble()).setScale(0, RoundingMode.HALF_UP).intValue();
            }
            if (node.hasNonNull("filamentDiameter")) {
                filamentDiameter = BigDecimal.valueOf(node.get("filamentDiameter").asDouble()).setScale(3, RoundingMode.HALF_UP);
            }
            if (node.hasNonNull("priority")) {
                priority = node.get("priority").asInt();
            }
        } catch (Exception ex) {
            log.warn("解析订单切片参数失败，将使用默认值", ex);
        }

        if (fillDensity < 0) {
            fillDensity = 0;
        }
        if (fillDensity > 100) {
            fillDensity = 100;
        }

        return new SliceParams(layerHeight, fillDensity, filamentDiameter, priority);
    }

    private ModelSource extractModelSource(String customParams, String modelFilePath) {
        if (StringUtils.hasText(customParams)) {
            try {
                JsonNode node = objectMapper.readTree(customParams);
                String[] sourcePathKeys = new String[]{
                        "modelFilePath",
                        "modelFileUrl",
                        "sourceFilePath",
                        "sourceFileUrl",
                        "filePath",
                        "fileUrl",
                        "modelUrl",
                        "sourceUrl",
                        "downloadUrl",
                        "stlUrl",
                        "objUrl",
                        "gltfUrl",
                        "glbUrl",
                        "fbxUrl",
                        "3mfUrl"
                };
                for (String key : sourcePathKeys) {
                    if (node.hasNonNull(key)) {
                        String value = node.get(key).asText();
                        if (StringUtils.hasText(value)) {
                            String fileName = fileNameOnly(value);
                            return new ModelSource(fileName, extensionOf(fileName), value);
                        }
                    }
                }

                String[] keys = new String[]{
                        "modelFileName",
                        "sourceFileName",
                        "sliceFileName",
                        "stlFileName",
                        "objFileName",
                        "gltfFileName",
                        "glbFileName",
                        "fbxFileName",
                        "3mfFileName",
                        "sourceStl"
                };
                for (String key : keys) {
                    if (node.hasNonNull(key)) {
                        String value = node.get(key).asText();
                        if (StringUtils.hasText(value)) {
                            String fileName = fileNameOnly(value);
                            return new ModelSource(fileName, extensionOf(fileName), modelFilePath);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (StringUtils.hasText(modelFilePath)) {
            String fileName = fileNameOnly(modelFilePath);
            if (StringUtils.hasText(fileName)) {
                return new ModelSource(fileName, extensionOf(fileName), modelFilePath);
            }
        }

        throw new BusinessException("无法确定源模型文件，请在订单 customParams 中提供模型文件名或文件地址");
    }

    private String fileNameOnly(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String raw = path.trim();
        int queryIdx = raw.indexOf('?');
        if (queryIdx >= 0) {
            raw = raw.substring(0, queryIdx);
        }
        int fragmentIdx = raw.indexOf('#');
        if (fragmentIdx >= 0) {
            raw = raw.substring(0, fragmentIdx);
        }
        String normalized = raw.replace("\\", "/");
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }

    private String ensureModelLocalFile(PrintJob job) {
        SysOrder order = orderRepository.selectById(job.getOrderId());
        SysModel model = modelRepository.selectById(job.getModelId());
        String modelPath = model == null ? null : model.getFilePath();
        String customParams = order == null ? null : order.getCustomParams();

        ModelSource source = extractModelSource(customParams, modelPath);
        String ext = StringUtils.hasText(source.fileType()) ? source.fileType() : extensionOf(source.fileName());
        if (!StringUtils.hasText(ext)) {
            throw new BusinessException("模型文件扩展名缺失，无法切片");
        }

        String targetFileName = buildOrderModelFileName(job.getOrderSn(), ext);
        String targetPathText = buildWorkPath(targetFileName);
        Path targetPath = Paths.get(targetPathText);

        try {
            Path parent = targetPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            String sourcePath = source.sourcePath();
            if (isHttpUrl(sourcePath)) {
                // 将公网 URL 转换为内网 URL，后端在 WSL 内部只能访问 127.0.0.1:9000
                String internalUrl = minioUtil.toInternalUrl(sourcePath);
                downloadToLocal(internalUrl, targetPath);
            } else if (StringUtils.hasText(sourcePath)) {
                Path sourceLocalPath = Paths.get(sourcePath.trim());
                if (!Files.exists(sourceLocalPath)) {
                    throw new BusinessException("模型源文件不存在: " + sourcePath);
                }
                Files.copy(sourceLocalPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                String fallbackName = job.getModelFileName();
                if (!StringUtils.hasText(fallbackName)) {
                    throw new BusinessException("未找到模型源文件地址，无法下载到切片目录");
                }
                Path fallback = Paths.get(buildWorkPath(fallbackName));
                if (!Files.exists(fallback)) {
                    throw new BusinessException("未找到模型源文件地址，且切片目录内不存在源文件: " + fallback);
                }
                if (!fallback.equals(targetPath)) {
                    Files.copy(fallback, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("准备切片模型文件失败: " + ex.getMessage(), ex);
        }

        if (!Objects.equals(job.getModelFileName(), targetFileName) || !Objects.equals(job.getModelFileType(), ext)) {
            job.setModelFileName(targetFileName);
            job.setModelFileType(ext);
            printJobRepository.updateById(job);
        }
        return targetFileName;
    }

    private void downloadToLocal(String sourceUrl, Path targetPath) throws Exception {
        URI uri = URI.create(sourceUrl.trim());
        try (InputStream inputStream = uri.toURL().openStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean isHttpUrl(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String value = text.trim().toLowerCase();
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private String buildOrderModelFileName(String orderSn, String extension) {
        String safeOrderSn = StringUtils.hasText(orderSn) ? orderSn.replaceAll("[^a-zA-Z0-9_-]", "") : "ORDER";
        if (!StringUtils.hasText(safeOrderSn)) {
            safeOrderSn = "ORDER";
        }
        String safeExt = extensionOf("x." + extension);
        if (!StringUtils.hasText(safeExt)) {
            throw new BusinessException("模型扩展名不合法: " + extension);
        }
        return safeOrderSn + "." + safeExt;
    }

    private String extensionOf(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase();
    }

    private String buildBaseUrl(String ip, Integer port, Boolean https) {
        String protocol = Boolean.TRUE.equals(https) ? "https" : "http";
        return protocol + "://" + ip + ":" + port;
    }

    private boolean isValidIp(String ip) {
        String[] arr = ip.split("\\.");
        if (arr.length != 4) {
            return false;
        }
        try {
            for (String part : arr) {
                if (part.isEmpty() || part.length() > 3) {
                    return false;
                }
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String generatePrinterCode(String ip, Integer port) {
        String normalized = ip.replace('.', '-');
        return "P-" + normalized + "-" + port + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private Map<Long, String> loadPrinterNames(List<Long> printerIds) {
        if (printerIds == null || printerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return printPrinterRepository.selectBatchIds(printerIds).stream()
                .collect(Collectors.toMap(PrintPrinter::getId, PrintPrinter::getPrinterName, (a, b) -> a));
    }

    private PrintJobVO toVo(PrintJob job, String printerName) {
        PrintJobVO vo = new PrintJobVO();
        vo.setId(job.getId());
        vo.setOrderId(job.getOrderId());
        vo.setOrderSn(job.getOrderSn());
        vo.setModelId(job.getModelId());
        vo.setModelName(job.getModelName());
        vo.setModelFileName(job.getModelFileName());
        vo.setModelFileType(job.getModelFileType());
        vo.setStatus(job.getStatus());
        vo.setStatusDesc(printJobStatusText(job.getStatus()));
        vo.setPriority(job.getPriority());
        vo.setPrinterId(job.getPrinterId());
        vo.setPrinterName(printerName);
        vo.setProgress(job.getProgress());
        vo.setToolTempActual(job.getToolTempActual());
        vo.setToolTempTarget(job.getToolTempTarget());
        vo.setBedTempActual(job.getBedTempActual());
        vo.setBedTempTarget(job.getBedTempTarget());
        vo.setEstimatedSecondsLeft(job.getEstimatedSecondsLeft());
        vo.setErrorMessage(job.getErrorMessage());
        vo.setCreateTime(job.getCreateTime());
        vo.setUpdateTime(job.getUpdateTime());
        return vo;
    }

    private void pushProgress(PrintJob job) {
        printWebSocketService.broadcast(PrintJobProgressVO.builder()
                .jobId(job.getId())
                .orderId(job.getOrderId())
                .printerId(job.getPrinterId())
                .status(job.getStatus())
                .statusDesc(printJobStatusText(job.getStatus()))
                .progress(job.getProgress())
                .toolTempActual(job.getToolTempActual())
                .toolTempTarget(job.getToolTempTarget())
                .bedTempActual(job.getBedTempActual())
                .bedTempTarget(job.getBedTempTarget())
                .estimatedSecondsLeft(job.getEstimatedSecondsLeft())
                .errorMessage(job.getErrorMessage())
                .timestamp(System.currentTimeMillis())
                .build());
    }

    private void saveEvent(Long jobId, String eventType, String message, String payload) {
        PrintJobEvent event = new PrintJobEvent();
        event.setJobId(jobId);
        event.setEventType(eventType);
        event.setEventMessage(message);
        event.setEventPayload(payload);
        printJobEventRepository.insert(event);
    }

    private PrintJob getJobOrThrow(Long jobId) {
        PrintJob job = printJobRepository.selectById(jobId);
        if (job == null || Objects.equals(job.getIsDelete(), 1)) {
            throw new BusinessException("打印任务不存在");
        }
        return job;
    }

    private String printJobStatusText(Integer status) {
        PrintJobStatus enumValue = PrintJobStatus.fromCode(status);
        return enumValue == null ? "Unknown" : enumValue.getDescription();
    }

    private String printPrinterStatusText(Integer status) {
        if (Objects.equals(status, PrintPrinterStatus.IDLE.getCode())) {
            return PrintPrinterStatus.IDLE.getDescription();
        }
        if (Objects.equals(status, PrintPrinterStatus.BUSY.getCode())) {
            return PrintPrinterStatus.BUSY.getDescription();
        }
        if (Objects.equals(status, PrintPrinterStatus.OFFLINE.getCode())) {
            return PrintPrinterStatus.OFFLINE.getDescription();
        }
        if (Objects.equals(status, PrintPrinterStatus.ERROR.getCode())) {
            return PrintPrinterStatus.ERROR.getDescription();
        }
        return "Unknown";
    }

    private record PipelineContext(
        Long printerId,
        BigDecimal layerHeight,
        Integer fillDensity,
        BigDecimal filamentDiameter,
        String printerBaseUrl,
        String printerAuthKey,
        String printerAuthValue
    ) {}

    private record SliceParams(BigDecimal layerHeight, Integer fillDensity, BigDecimal filamentDiameter, Integer priority) {
    }

    private record ModelSource(String fileName, String fileType, String sourcePath) {
    }
}
