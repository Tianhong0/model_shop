package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.entity.PrintFaultDiagnosis;
import org.majun.backend.entity.PrintFaultType;
import org.majun.backend.entity.PrintJob;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.enums.PrintJobStatus;
import org.majun.backend.event.PrintJobRetryEvent;
import org.majun.backend.repository.PrintFaultDiagnosisRepository;
import org.majun.backend.repository.PrintFaultTypeRepository;
import org.majun.backend.repository.PrintJobRepository;
import org.majun.backend.repository.SysOrderRepository;
import org.majun.backend.service.PrintFaultService;
import org.majun.backend.vo.PrintFaultDiagnosisVO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 打印故障诊断服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrintFaultServiceImpl implements PrintFaultService {

    private static final int MAX_RETRY_COUNT = 3;

    private static final Map<String, String> CATEGORY_NAMES = Map.of(
            "MODEL", "模型问题",
            "PARAM", "参数设置",
            "MATERIAL", "材料异常",
            "DEVICE", "设备故障",
            "UNKNOWN", "其他问题"
    );

    private final PrintJobRepository printJobRepository;
    private final PrintFaultTypeRepository faultTypeRepository;
    private final PrintFaultDiagnosisRepository diagnosisRepository;
    private final SysOrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public PrintFaultDiagnosisVO diagnoseByOrderId(Long orderId, Long userId) {
        PrintJob job = printJobRepository.selectOne(
                new LambdaQueryWrapper<PrintJob>()
                        .eq(PrintJob::getOrderId, orderId)
                        .eq(PrintJob::getIsDelete, 0)
                        .last("limit 1")
        );

        if (job == null) {
            return buildNoFaultResult();
        }

        if (!isFailedStatus(job.getStatus())) {
            return buildNoFaultResult();
        }

        // 验证订单归属
        SysOrder order = orderRepository.selectById(orderId);
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            return buildNoFaultResult();
        }

        return diagnose(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userRetryPrint(Long orderId, Long userId) {
        SysOrder order = orderRepository.selectById(orderId);
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException("订单不存在");
        }

        PrintJob job = printJobRepository.selectOne(
                new LambdaQueryWrapper<PrintJob>()
                        .eq(PrintJob::getOrderId, orderId)
                        .eq(PrintJob::getIsDelete, 0)
                        .last("limit 1")
        );

        if (job == null) {
            throw new BusinessException("打印任务不存在");
        }

        if (!isFailedStatus(job.getStatus())) {
            throw new BusinessException("当前任务状态不允许重试");
        }

        // 检查重试次数
        PrintFaultDiagnosis record = diagnosisRepository.selectOne(
                new LambdaQueryWrapper<PrintFaultDiagnosis>()
                        .eq(PrintFaultDiagnosis::getJobId, job.getId())
                        .orderByDesc(PrintFaultDiagnosis::getCreateTime)
                        .last("limit 1")
        );

        int currentRetryCount = record != null ? (record.getRetryCount() != null ? record.getRetryCount() : 0) : 0;
        if (currentRetryCount >= MAX_RETRY_COUNT) {
            throw new BusinessException("已达到最大重试次数(" + MAX_RETRY_COUNT + "次)，请联系客服处理");
        }

        // 重置任务状态
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

        // 更新诊断记录
        if (record != null) {
            record.setRetryCount(currentRetryCount + 1);
            record.setStatus(1); // 已重试
            diagnosisRepository.updateById(record);
        } else {
            // 创建诊断记录
            PrintFaultDiagnosis newRecord = new PrintFaultDiagnosis();
            newRecord.setJobId(job.getId());
            newRecord.setOrderId(orderId);
            newRecord.setUserId(userId);
            newRecord.setFaultCode("RETRY");
            newRecord.setFaultName("用户重试");
            newRecord.setRetryCount(1);
            newRecord.setStatus(1);
            diagnosisRepository.insert(newRecord);
        }

        // 发布重试事件
        eventPublisher.publishEvent(new PrintJobRetryEvent(job.getId(), job.getPrinterId()));

        log.info("用户 {} 重试打印任务 {}，第 {} 次重试", userId, job.getId(), currentRetryCount + 1);
    }

    private PrintFaultDiagnosisVO diagnose(PrintJob job) {
        String errorMessage = job.getErrorMessage();
        Integer status = job.getStatus();

        // 获取所有启用的故障类型，按优先级排序
        List<PrintFaultType> faultTypes = faultTypeRepository.selectList(
                new LambdaQueryWrapper<PrintFaultType>()
                        .eq(PrintFaultType::getIsActive, 1)
                        .orderByDesc(PrintFaultType::getPriority)
        );

        // 匹配故障类型
        PrintFaultType matchedType = matchFaultType(errorMessage, status, faultTypes);

        // 构建诊断结果
        PrintFaultDiagnosisVO vo = buildDiagnosisVO(matchedType, errorMessage, status);

        // 获取已重试次数
        PrintFaultDiagnosis record = diagnosisRepository.selectOne(
                new LambdaQueryWrapper<PrintFaultDiagnosis>()
                        .eq(PrintFaultDiagnosis::getJobId, job.getId())
                        .orderByDesc(PrintFaultDiagnosis::getCreateTime)
                        .last("limit 1")
        );
        if (record != null && record.getRetryCount() != null) {
            vo.setRetryCount(record.getRetryCount());
        } else {
            vo.setRetryCount(0);
        }
        vo.setMaxRetryCount(MAX_RETRY_COUNT);

        // 保存诊断记录
        saveDiagnosisRecord(job, matchedType, errorMessage, vo);

        return vo;
    }

    private PrintFaultType matchFaultType(String errorMessage, Integer status, List<PrintFaultType> faultTypes) {
        if (!StringUtils.hasText(errorMessage)) {
            return findDefaultFaultType(status, faultTypes);
        }

        String lowerError = errorMessage.toLowerCase();

        for (PrintFaultType type : faultTypes) {
            List<String> keywords = parseJsonArray(type.getErrorKeywords());
            for (String keyword : keywords) {
                if (StringUtils.hasText(keyword) && lowerError.contains(keyword.toLowerCase())) {
                    return type;
                }
            }
        }

        return findDefaultFaultType(status, faultTypes);
    }

    private PrintFaultType findDefaultFaultType(Integer status, List<PrintFaultType> faultTypes) {
        String defaultCode;
        if (status != null && status == PrintJobStatus.SLICE_FAILED.getCode()) {
            defaultCode = "SLICE_FAILED";
        } else {
            defaultCode = "UNKNOWN_ERROR";
        }

        return faultTypes.stream()
                .filter(t -> defaultCode.equals(t.getFaultCode()))
                .findFirst()
                .orElse(null);
    }

    private PrintFaultDiagnosisVO buildDiagnosisVO(PrintFaultType type, String errorMessage, Integer status) {
        PrintFaultDiagnosisVO vo = new PrintFaultDiagnosisVO();
        vo.setDiagnoseTime(LocalDateTime.now());

        if (type != null) {
            vo.setFaultCode(type.getFaultCode());
            vo.setFaultCategory(type.getFaultCategory());
            vo.setFaultCategoryName(CATEGORY_NAMES.getOrDefault(type.getFaultCategory(), "其他问题"));
            vo.setFaultName(type.getFaultName());
            vo.setDescription(type.getDescription());
            vo.setSuggestions(parseJsonArray(type.getSuggestion()));
        } else {
            vo.setFaultCode("UNKNOWN_ERROR");
            vo.setFaultCategory("UNKNOWN");
            vo.setFaultCategoryName("其他问题");
            vo.setFaultName("未知错误");
            vo.setDescription("发生未知类型的错误");
            vo.setSuggestions(List.of("尝试重新打印", "如问题持续请联系客服"));
        }

        vo.setErrorMessage(errorMessage);
        vo.setCanRetry(status != null && status != PrintJobStatus.CANCELED.getCode());

        return vo;
    }

    private PrintFaultDiagnosisVO buildNoFaultResult() {
        PrintFaultDiagnosisVO vo = new PrintFaultDiagnosisVO();
        vo.setFaultCode("NO_FAULT");
        vo.setFaultCategory("NONE");
        vo.setFaultCategoryName("无故障");
        vo.setFaultName("无故障");
        vo.setDescription("当前打印任务正常，无需诊断");
        vo.setSuggestions(Collections.emptyList());
        vo.setCanRetry(false);
        vo.setDiagnoseTime(LocalDateTime.now());
        vo.setRetryCount(0);
        vo.setMaxRetryCount(MAX_RETRY_COUNT);
        return vo;
    }

    private void saveDiagnosisRecord(PrintJob job, PrintFaultType type, String errorMessage, PrintFaultDiagnosisVO vo) {
        try {
            PrintFaultDiagnosis record = new PrintFaultDiagnosis();
            record.setJobId(job.getId());
            record.setOrderId(job.getOrderId());
            record.setErrorMessage(errorMessage);
            record.setStatus(0);

            if (type != null) {
                record.setFaultTypeId(type.getId());
                record.setFaultCode(type.getFaultCode());
                record.setFaultCategory(type.getFaultCategory());
                record.setFaultName(type.getFaultName());
            }

            record.setAnalysisResult(objectMapper.writeValueAsString(vo));
            diagnosisRepository.insert(record);
        } catch (Exception e) {
            log.warn("保存故障诊断记录失败: jobId={}, error={}", job.getId(), e.getMessage());
        }
    }

    private boolean isFailedStatus(Integer status) {
        return status != null && (
                status == PrintJobStatus.SLICE_FAILED.getCode() ||
                status == PrintJobStatus.FAILED.getCode()
        );
    }

    private List<String> parseJsonArray(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
