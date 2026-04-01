package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.dto.OperationLogQueryRequest;
import org.majun.backend.entity.SysOperationLog;
import org.majun.backend.repository.SysOperationLogRepository;
import org.majun.backend.service.OperationLogService;
import org.majun.backend.vo.OperationLogVO;
import org.majun.backend.vo.PageResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final SysOperationLogRepository operationLogRepository;

    @Override
    public void log(SysOperationLog log) {
        operationLogRepository.insert(log);
    }

    @Override
    @Async("logTaskExecutor")
    public void logAsync(SysOperationLog logEntity) {
        try {
            operationLogRepository.insert(logEntity);
        } catch (Exception e) {
            log.error("异步记录操作日志失败", e);
        }
    }

    @Override
    public PageResult<OperationLogVO> queryPage(OperationLogQueryRequest request) {
        Page<SysOperationLog> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getOperatorName())) {
            wrapper.like(SysOperationLog::getOperatorName, request.getOperatorName());
        }
        if (request.getOperatorId() != null) {
            wrapper.eq(SysOperationLog::getOperatorId, request.getOperatorId());
        }
        if (StringUtils.hasText(request.getOperationType())) {
            wrapper.eq(SysOperationLog::getOperationType, request.getOperationType());
        }
        if (StringUtils.hasText(request.getModule())) {
            wrapper.eq(SysOperationLog::getModule, request.getModule());
        }
        if (StringUtils.hasText(request.getTargetType())) {
            wrapper.eq(SysOperationLog::getTargetType, request.getTargetType());
        }
        if (request.getSuccess() != null) {
            wrapper.eq(SysOperationLog::getSuccess, request.getSuccess());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(SysOperationLog::getCreateTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(SysOperationLog::getCreateTime, request.getEndTime());
        }

        wrapper.orderByDesc(SysOperationLog::getCreateTime);

        Page<SysOperationLog> result = operationLogRepository.selectPage(page, wrapper);

        List<OperationLogVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .toList();

        int pages = (int) ((result.getTotal() + result.getSize() - 1) / result.getSize());

        return PageResult.<OperationLogVO>builder()
                .records(voList)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages(pages)
                .build();
    }

    @Override
    public OperationLogVO getDetail(Long id) {
        SysOperationLog log = operationLogRepository.selectById(id);
        return log != null ? toVO(log) : null;
    }

    @Override
    public int cleanOldLogs(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SysOperationLog::getCreateTime, threshold);
        return operationLogRepository.delete(wrapper);
    }

    private OperationLogVO toVO(SysOperationLog log) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorName(log.getOperatorName());
        vo.setOperationType(log.getOperationType());
        vo.setModule(log.getModule());
        vo.setDescription(log.getDescription());
        vo.setTargetType(log.getTargetType());
        vo.setTargetId(log.getTargetId());
        vo.setContent(log.getContent());
        vo.setBeforeData(log.getBeforeData());
        vo.setAfterData(log.getAfterData());
        vo.setIp(log.getIp());
        vo.setRequestUrl(log.getRequestUrl());
        vo.setRequestMethod(log.getRequestMethod());
        vo.setSuccess(log.getSuccess());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setDuration(log.getDuration());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }
}
