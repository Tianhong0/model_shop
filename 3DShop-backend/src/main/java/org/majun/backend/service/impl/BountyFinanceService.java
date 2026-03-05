package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.entity.BountyEscrow;
import org.majun.backend.entity.BountyPriceChange;
import org.majun.backend.entity.BountyTask;
import org.majun.backend.entity.WalletAccount;
import org.majun.backend.entity.WalletLedger;
import org.majun.backend.enums.BountyEscrowStatus;
import org.majun.backend.enums.WalletLedgerDirection;
import org.majun.backend.repository.BountyEscrowRepository;
import org.majun.backend.repository.WalletAccountRepository;
import org.majun.backend.repository.WalletLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BountyFinanceService {

    private final BountyEscrowRepository bountyEscrowRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletLedgerRepository walletLedgerRepository;
    private final PointService pointService;

    public boolean hasAvailableBalance(Long userId, BigDecimal requiredAmount) {
        BigDecimal required = amount(requiredAmount);
        if (required.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        WalletAccount wallet = walletAccountRepository.selectOne(new LambdaQueryWrapper<WalletAccount>()
                .eq(WalletAccount::getUserId, userId)
                .eq(WalletAccount::getIsDelete, 0)
                .last("limit 1"));
        if (wallet == null) {
            return false;
        }
        return amount(wallet.getAvailableBalance()).compareTo(required) >= 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public void releaseToWinner(BountyTask task) {
        if (task == null || task.getId() == null) {
            throw new BusinessException("任务信息无效");
        }
        if (task.getWinnerDesignerId() == null) {
            throw new BusinessException("任务未选择中标设计者");
        }

        BountyEscrow escrow = getEscrowOrThrow(task.getId());
        BigDecimal finalAmount = amount(task.getFinalAmount());
        BigDecimal releasedAmount = amount(escrow.getReleasedAmount());
        BigDecimal toRelease = finalAmount.subtract(releasedAmount);

        if (toRelease.compareTo(BigDecimal.ZERO) <= 0) {
            refreshEscrowStatus(escrow);
            bountyEscrowRepository.updateById(escrow);
            return;
        }

        WalletAccount winnerWallet = getOrCreateWallet(task.getWinnerDesignerId());
        WalletAccount updatedWinnerWallet = credit(
                winnerWallet,
                toRelease,
                "BOUNTY_RELEASE",
                task.getTaskSn(),
                task.getId(),
                "悬赏验收通过入账"
        );

        escrow.setReleasedAmount(releasedAmount.add(toRelease));
        refreshEscrowStatus(escrow);
        bountyEscrowRepository.updateById(escrow);
        walletAccountRepository.updateById(updatedWinnerWallet);
        pointService.rewardBountyRelease(task.getWinnerDesignerId(), task.getId(), task.getTaskSn(), toRelease);
    }

    @Transactional(rollbackFor = Exception.class)
    public void settlePriceChange(BountyTask task, BountyPriceChange change) {
        if (task == null || change == null) {
            throw new BusinessException("改价结算参数无效");
        }
        BountyEscrow escrow = getEscrowOrThrow(task.getId());

        BigDecimal currentAmount = amount(change.getCurrentAmount());
        BigDecimal targetAmount = amount(change.getTargetAmount());
        BigDecimal delta = targetAmount.subtract(currentAmount);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String bizNo = String.valueOf(change.getId());
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            WalletAccount publisherWallet = getOrCreateWallet(task.getPublisherId());
            WalletAccount updatedPublisherWallet = debit(
                    publisherWallet,
                    delta,
                    "BOUNTY_PRICE_INCREASE",
                    bizNo,
                    task.getId(),
                    "悬赏改价补差"
            );
            escrow.setEscrowAmount(amount(escrow.getEscrowAmount()).add(delta));
            walletAccountRepository.updateById(updatedPublisherWallet);
        } else {
            BigDecimal refundAmount = delta.abs();
            WalletAccount publisherWallet = getOrCreateWallet(task.getPublisherId());
            WalletAccount updatedPublisherWallet = credit(
                    publisherWallet,
                    refundAmount,
                    "BOUNTY_PRICE_REFUND",
                    bizNo,
                    task.getId(),
                    "悬赏改价退差"
            );
            escrow.setEscrowAmount(amount(escrow.getEscrowAmount()).subtract(refundAmount));
            escrow.setRefundAmount(amount(escrow.getRefundAmount()).add(refundAmount));
            walletAccountRepository.updateById(updatedPublisherWallet);
        }

        refreshEscrowStatus(escrow);
        bountyEscrowRepository.updateById(escrow);
    }

    private BountyEscrow getEscrowOrThrow(Long taskId) {
        BountyEscrow escrow = bountyEscrowRepository.selectOne(new LambdaQueryWrapper<BountyEscrow>()
                .eq(BountyEscrow::getTaskId, taskId)
                .eq(BountyEscrow::getIsDelete, 0)
                .last("limit 1"));
        if (escrow == null) {
            throw new BusinessException("托管记录不存在");
        }
        return escrow;
    }

    private WalletAccount getOrCreateWallet(Long userId) {
        WalletAccount wallet = walletAccountRepository.selectOne(new LambdaQueryWrapper<WalletAccount>()
                .eq(WalletAccount::getUserId, userId)
                .eq(WalletAccount::getIsDelete, 0)
                .last("limit 1"));
        if (wallet != null) {
            return wallet;
        }
        WalletAccount created = new WalletAccount();
        created.setUserId(userId);
        created.setAvailableBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        created.setFrozenBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        created.setStatus(1);
        created.setVersion(0);
        created.setIsDelete(0);
        walletAccountRepository.insert(created);
        return created;
    }

    private WalletAccount credit(WalletAccount account,
                                 BigDecimal amount,
                                 String bizType,
                                 String bizNo,
                                 Long refId,
                                 String remark) {
        if (existsLedger(bizType, bizNo)) {
            return account;
        }
        BigDecimal beforeAvailable = amount(account.getAvailableBalance());
        BigDecimal beforeFrozen = amount(account.getFrozenBalance());
        BigDecimal afterAvailable = beforeAvailable.add(amount);

        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(account.getUserId());
        ledger.setAccountId(account.getId());
        ledger.setDirection(WalletLedgerDirection.INCOME.getCode());
        ledger.setBizType(bizType);
        ledger.setBizNo(bizNo);
        ledger.setRefId(refId);
        ledger.setAmount(amount);
        ledger.setBeforeAvailable(beforeAvailable);
        ledger.setAfterAvailable(afterAvailable);
        ledger.setBeforeFrozen(beforeFrozen);
        ledger.setAfterFrozen(beforeFrozen);
        ledger.setRemark(remark);
        walletLedgerRepository.insert(ledger);

        account.setAvailableBalance(afterAvailable);
        return account;
    }

    private WalletAccount debit(WalletAccount account,
                                BigDecimal amount,
                                String bizType,
                                String bizNo,
                                Long refId,
                                String remark) {
        if (existsLedger(bizType, bizNo)) {
            return account;
        }
        BigDecimal beforeAvailable = amount(account.getAvailableBalance());
        BigDecimal beforeFrozen = amount(account.getFrozenBalance());
        if (beforeAvailable.compareTo(amount) < 0) {
            throw new BusinessException("发布者钱包余额不足，无法完成改价补差");
        }
        BigDecimal afterAvailable = beforeAvailable.subtract(amount);

        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(account.getUserId());
        ledger.setAccountId(account.getId());
        ledger.setDirection(WalletLedgerDirection.EXPENSE.getCode());
        ledger.setBizType(bizType);
        ledger.setBizNo(bizNo);
        ledger.setRefId(refId);
        ledger.setAmount(amount);
        ledger.setBeforeAvailable(beforeAvailable);
        ledger.setAfterAvailable(afterAvailable);
        ledger.setBeforeFrozen(beforeFrozen);
        ledger.setAfterFrozen(beforeFrozen);
        ledger.setRemark(remark);
        walletLedgerRepository.insert(ledger);

        account.setAvailableBalance(afterAvailable);
        return account;
    }

    private boolean existsLedger(String bizType, String bizNo) {
        Long count = walletLedgerRepository.selectCount(new LambdaQueryWrapper<WalletLedger>()
                .eq(WalletLedger::getBizType, bizType)
                .eq(WalletLedger::getBizNo, bizNo));
        return count != null && count > 0;
    }

    private void refreshEscrowStatus(BountyEscrow escrow) {
        BigDecimal escrowAmount = amount(escrow.getEscrowAmount());
        BigDecimal releasedAmount = amount(escrow.getReleasedAmount());
        BigDecimal refundedAmount = amount(escrow.getRefundAmount());

        BigDecimal expectedRelease = escrowAmount.subtract(refundedAmount);
        if (expectedRelease.compareTo(BigDecimal.ZERO) < 0) {
            expectedRelease = BigDecimal.ZERO;
        }

        if (releasedAmount.compareTo(BigDecimal.ZERO) <= 0 && refundedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            escrow.setStatus(BountyEscrowStatus.ESCROWED.getCode());
            return;
        }
        if (expectedRelease.compareTo(BigDecimal.ZERO) == 0 && refundedAmount.compareTo(BigDecimal.ZERO) > 0) {
            escrow.setStatus(BountyEscrowStatus.REFUNDED.getCode());
            return;
        }
        if (releasedAmount.compareTo(expectedRelease) >= 0) {
            escrow.setStatus(BountyEscrowStatus.RELEASED.getCode());
            return;
        }
        escrow.setStatus(BountyEscrowStatus.PARTIAL_RELEASED.getCode());
    }

    private BigDecimal amount(BigDecimal value) {
        if (Objects.isNull(value)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}