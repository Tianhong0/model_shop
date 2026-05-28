package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.dto.DesignerSettlementQueryRequest;
import org.majun.backend.entity.DesignerSettlement;
import org.majun.backend.entity.SysModel;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.entity.WalletLedger;
import org.majun.backend.enums.ModelSourceType;
import org.majun.backend.enums.WalletLedgerDirection;
import org.majun.backend.repository.DesignerSettlementRepository;
import org.majun.backend.repository.SysModelRepository;
import org.majun.backend.repository.SysOrderRepository;
import org.majun.backend.repository.WalletLedgerRepository;
import org.majun.backend.service.DesignerSettlementService;
import org.majun.backend.vo.DesignerSettlementVO;
import org.majun.backend.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesignerSettlementServiceImpl implements DesignerSettlementService {

    private static final String BIZ_TYPE_MODEL_PROFIT = "MODEL_PROFIT";
    private static final DateTimeFormatter SN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /** 分润冻结天数：与悬赏/二手交易保持一致，7 天后由 WalletFrozenSchedule 自动解冻到可用余额 */
    private static final int SETTLEMENT_FROZEN_DAYS = 7;

    private final DesignerSettlementRepository settlementRepository;
    private final SysOrderRepository orderRepository;
    private final SysModelRepository modelRepository;
    private final WalletLedgerRepository walletLedgerRepository;
    private final WalletService walletService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleOnPrintDone(Long orderId) {
        // 1. 查询订单
        SysOrder order = orderRepository.selectById(orderId);
        if (order == null || order.getIsDelete() == 1) {
            log.info("订单不存在或已删除，跳过分润: orderId={}", orderId);
            return;
        }

        // 2. 检查是否已存在结算记录（幂等）
        Long count = settlementRepository.selectCount(new LambdaQueryWrapper<DesignerSettlement>()
                .eq(DesignerSettlement::getOrderId, orderId));
        if (count != null && count > 0) {
            log.info("分润已结算，跳过: orderId={}", orderId);
            return;
        }

        // 3. 查询模型
        SysModel model = modelRepository.selectById(order.getModelId());
        if (model == null || model.getIsDelete() == 1) {
            log.info("模型不存在或已删除，跳过分润: modelId={}", order.getModelId());
            return;
        }

        // 4. 只对设计师模型分润
        if (!Objects.equals(model.getSourceType(), ModelSourceType.DESIGNER.getCode())) {
            log.info("非设计师模型，跳过分润: modelId={}, sourceType={}", model.getId(), model.getSourceType());
            return;
        }

        Integer shareRatio = model.getProfitShareRatio();
        if (shareRatio == null || shareRatio <= 0) {
            log.info("分润比例为0或空，跳过分润: modelId={}, ratio={}", model.getId(), shareRatio);
            return;
        }

        // 5. 计算分润金额
        BigDecimal orderPrice = order.getOrderPrice() != null ? order.getOrderPrice() : BigDecimal.ZERO;
        BigDecimal settlementAmount = orderPrice.multiply(BigDecimal.valueOf(shareRatio))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (settlementAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("分润金额为0，跳过分润: orderId={}", orderId);
            return;
        }

        String settlementSn = "DS" + LocalDateTime.now().format(SN_DATE_FORMAT) + orderId;

        // 6. 创建结算记录（冻结中）
        DesignerSettlement settlement = new DesignerSettlement();
        settlement.setSettlementSn(settlementSn);
        settlement.setOrderId(orderId);
        settlement.setOrderSn(order.getOrderSn());
        settlement.setModelId(model.getId());
        settlement.setDesignerId(model.getDesignerId());
        settlement.setOrderPrice(orderPrice);
        settlement.setProfitShareRatio(shareRatio);
        settlement.setSettlementAmount(settlementAmount);
        settlement.setBizType(BIZ_TYPE_MODEL_PROFIT);
        settlement.setStatus(0);
        settlement.setIsDelete(0);
        settlementRepository.insert(settlement);

        try {
            // 7. 调用钱包服务冻结分润金额（冻结期满后由 WalletFrozenSchedule 自动解冻到可用余额）
            String remark = "模型分润冻结" + SETTLEMENT_FROZEN_DAYS + "天，订单号:" + order.getOrderSn();
            Long frozenRecordId = walletService.freezeAmount(
                    model.getDesignerId(),
                    settlementAmount,
                    BIZ_TYPE_MODEL_PROFIT,
                    settlementSn,
                    settlement.getId(),
                    SETTLEMENT_FROZEN_DAYS,
                    remark
            );

            // 8. 反查 FREEZE 流水 ID，回填到结算记录（仅作展示用）
            WalletLedger freezeLedger = walletLedgerRepository.selectOne(new LambdaQueryWrapper<WalletLedger>()
                    .eq(WalletLedger::getBizType, BIZ_TYPE_MODEL_PROFIT)
                    .eq(WalletLedger::getBizNo, settlementSn)
                    .eq(WalletLedger::getDirection, WalletLedgerDirection.FREEZE.getCode())
                    .last("limit 1")
            );

            // 9. 更新结算状态为"已冻结"
            settlement.setStatus(1);
            if (freezeLedger != null) {
                settlement.setWalletLedgerId(freezeLedger.getId());
            }
            settlement.setRemark("分润金额已冻结，将于" + SETTLEMENT_FROZEN_DAYS + "天后自动解冻到可用余额");
            settlementRepository.updateById(settlement);

            log.info("分润结算成功(冻结中): settlementSn={}, designerId={}, amount={}, frozenRecordId={}, frozenDays={}",
                    settlementSn, model.getDesignerId(), settlementAmount, frozenRecordId, SETTLEMENT_FROZEN_DAYS);
        } catch (Exception e) {
            log.error("分润结算失败: settlementSn={}, error={}", settlementSn, e.getMessage(), e);
            settlement.setStatus(2);
            settlement.setRemark("结算失败: " + (e.getMessage() != null ? e.getMessage().substring(0, Math.min(200, e.getMessage().length())) : "未知错误"));
            settlementRepository.updateById(settlement);
        }
    }

    @Override
    public PageResult<DesignerSettlementVO> querySettlements(DesignerSettlementQueryRequest request) {
        return querySettlementsInternal(request, null);
    }

    @Override
    public PageResult<DesignerSettlementVO> queryMySettlements(DesignerSettlementQueryRequest request, Long designerId) {
        return querySettlementsInternal(request, designerId);
    }

    private PageResult<DesignerSettlementVO> querySettlementsInternal(DesignerSettlementQueryRequest request, Long designerId) {
        LambdaQueryWrapper<DesignerSettlement> qw = new LambdaQueryWrapper<>();
        qw.eq(DesignerSettlement::getIsDelete, 0);
        if (designerId != null) {
            qw.eq(DesignerSettlement::getDesignerId, designerId);
        }
        if (request.getStatus() != null) {
            qw.eq(DesignerSettlement::getStatus, request.getStatus());
        }
        if (request.getDesignerId() != null && designerId == null) {
            qw.eq(DesignerSettlement::getDesignerId, request.getDesignerId());
        }
        qw.orderByDesc(DesignerSettlement::getCreateTime);

        Page<DesignerSettlement> page = new Page<>(request.getPageNum(), request.getPageSize());
        settlementRepository.selectPage(page, qw);

        List<DesignerSettlementVO> records = page.getRecords().stream().map(s -> {
            String modelName = "-";
            SysModel model = modelRepository.selectById(s.getModelId());
            if (model != null) {
                modelName = model.getModelName();
            }
            return DesignerSettlementVO.builder()
                    .id(s.getId())
                    .settlementSn(s.getSettlementSn())
                    .orderId(s.getOrderId())
                    .orderSn(s.getOrderSn())
                    .modelId(s.getModelId())
                    .modelName(modelName)
                    .designerId(s.getDesignerId())
                    .orderPrice(s.getOrderPrice())
                    .profitShareRatio(s.getProfitShareRatio())
                    .settlementAmount(s.getSettlementAmount())
                    .status(s.getStatus())
                    .walletLedgerId(s.getWalletLedgerId())
                    .createTime(s.getCreateTime())
                    .build();
        }).collect(Collectors.toList());

        return PageResult.<DesignerSettlementVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retrySettlement(Long settlementId) {
        DesignerSettlement settlement = settlementRepository.selectById(settlementId);
        if (settlement == null || settlement.getIsDelete() == 1) {
            throw new RuntimeException("结算记录不存在或已删除");
        }
        if (settlement.getStatus() != 2) {
            throw new RuntimeException("只能重试失败的结算记录");
        }
        settleOnPrintDone(settlement.getOrderId());
    }
}
