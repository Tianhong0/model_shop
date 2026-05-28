package org.majun.backend.service;

import org.majun.backend.dto.DesignerSettlementQueryRequest;
import org.majun.backend.vo.DesignerSettlementVO;
import org.majun.backend.vo.PageResult;

/**
 * 设计师分润结算服务接口
 */
public interface DesignerSettlementService {

    /**
     * 打印完成后触发分润结算
     */
    void settleOnPrintDone(Long orderId);

    /**
     * 管理员查询分润列表
     */
    PageResult<DesignerSettlementVO> querySettlements(DesignerSettlementQueryRequest request);

    /**
     * 设计者查询自己的分润列表
     */
    PageResult<DesignerSettlementVO> queryMySettlements(DesignerSettlementQueryRequest request, Long designerId);

    /**
     * 管理员重试失败的结算
     */
    void retrySettlement(Long settlementId);
}
