package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.entity.ModelDownloadRecord;
import org.majun.backend.entity.SysModel;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.enums.OrderStatus;
import org.majun.backend.repository.ModelDownloadRecordRepository;
import org.majun.backend.repository.SysModelRepository;
import org.majun.backend.repository.SysOrderRepository;
import org.majun.backend.service.ImageWatermarkService;
import org.majun.backend.service.ModelDownloadService;
import org.majun.backend.util.MinioUtil;
import org.majun.backend.vo.ModelDownloadUrlVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 模型下载服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 模型下载服务实现
 */
public class ModelDownloadServiceImpl implements ModelDownloadService {

    private static final int DEFAULT_PRESIGNED_EXPIRE_SECONDS = 3600; // 1小时

    private final SysModelRepository modelRepository;
    private final SysOrderRepository orderRepository;
    private final ModelDownloadRecordRepository downloadRecordRepository;
    private final MinioUtil minioUtil;
    private final ImageWatermarkService imageWatermarkService;

    @Override
    public boolean canDownloadModel(Long modelId, Long userId) {
        if (modelId == null || userId == null) {
            return false;
        }

        // 检查用户是否有已支付/已完成的订单购买该模型
        Long orderCount = orderRepository.selectCount(
                new LambdaQueryWrapper<SysOrder>()
                        .eq(SysOrder::getModelId, modelId)
                        .eq(SysOrder::getUserId, userId)
                        .eq(SysOrder::getIsDelete, 0)
                        .in(SysOrder::getOrderStatus,
                                OrderStatus.IN_PRODUCTION.getCode(),
                                OrderStatus.WAIT_SHIPMENT.getCode(),
                                OrderStatus.COMPLETED.getCode())
        );

        return orderCount != null && orderCount > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelDownloadUrlVO generateDownloadUrl(Long modelId, Long userId, String ipAddress) {
        // 验证模型存在
        SysModel model = modelRepository.selectById(modelId);
        if (model == null || Objects.equals(model.getIsDelete(), 1)) {
            throw new BusinessException("模型不存在");
        }

        // 验证购买权限
        if (!canDownloadModel(modelId, userId)) {
            throw new BusinessException("您尚未购买该模型，无法下载");
        }

        // 获取模型文件路径
        String filePath = model.getFilePath();
        if (!StringUtils.hasText(filePath)) {
            throw new BusinessException("模型文件不存在");
        }

        // 提取对象名称
        String objectName = minioUtil.extractObjectName(filePath);
        if (!StringUtils.hasText(objectName)) {
            throw new BusinessException("模型文件路径无效");
        }

        // 生成预签名URL
        String presignedUrl = minioUtil.generatePresignedDownloadUrl(objectName, DEFAULT_PRESIGNED_EXPIRE_SECONDS);
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(DEFAULT_PRESIGNED_EXPIRE_SECONDS);

        // 查找关联订单
        SysOrder order = orderRepository.selectOne(
                new LambdaQueryWrapper<SysOrder>()
                        .eq(SysOrder::getModelId, modelId)
                        .eq(SysOrder::getUserId, userId)
                        .eq(SysOrder::getIsDelete, 0)
                        .in(SysOrder::getOrderStatus,
                                OrderStatus.IN_PRODUCTION.getCode(),
                                OrderStatus.WAIT_SHIPMENT.getCode(),
                                OrderStatus.COMPLETED.getCode())
                        .orderByDesc(SysOrder::getCreateTime)
                        .last("LIMIT 1")
        );

        // 记录下载
        ModelDownloadRecord record = new ModelDownloadRecord();
        record.setModelId(modelId);
        record.setUserId(userId);
        record.setOrderId(order != null ? order.getId() : null);
        record.setDownloadTime(LocalDateTime.now());
        record.setIpAddress(ipAddress);
        record.setDownloadUrl(presignedUrl);
        record.setExpireTime(expireTime);
        downloadRecordRepository.insert(record);

        // 更新模型下载次数
        model.setDownloadCount(model.getDownloadCount() != null ? model.getDownloadCount() + 1 : 1);
        modelRepository.updateById(model);

        // 构建响应
        ModelDownloadUrlVO vo = new ModelDownloadUrlVO();
        vo.setDownloadUrl(presignedUrl);
        vo.setExpireTime(expireTime);
        vo.setExpireSeconds(DEFAULT_PRESIGNED_EXPIRE_SECONDS);
        vo.setModelId(modelId);
        vo.setModelName(model.getModelName());

        log.info("模型下载URL已生成: modelId={}, userId={}, orderId={}", modelId, userId, record.getOrderId());
        return vo;
    }

    @Override
    public String generatePreviewUrl(Long modelId) {
        // 验证模型存在
        SysModel model = modelRepository.selectById(modelId);
        if (model == null || Objects.equals(model.getIsDelete(), 1)) {
            throw new BusinessException("模型不存在");
        }

        // 优先使用预览文件路径，否则使用原文件路径
        String filePath = StringUtils.hasText(model.getPreviewFilePath())
                ? model.getPreviewFilePath()
                : model.getFilePath();

        if (!StringUtils.hasText(filePath)) {
            return null;
        }

        // 提取对象名称
        String objectName = minioUtil.extractObjectName(filePath);
        if (!StringUtils.hasText(objectName)) {
            return null;
        }

        // 生成预签名URL（预览URL有效期较短）
        return minioUtil.generatePresignedDownloadUrl(objectName, 1800); // 30分钟
    }

    @Override
    public String getWatermarkedImageUrl(Long modelId, Long imageId) {
        if (modelId == null || imageId == null) {
            return null;
        }
        return imageWatermarkService.getWatermarkedUrl(modelId, imageId);
    }
}
