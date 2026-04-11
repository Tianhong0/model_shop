package org.majun.backend.service;

import org.majun.backend.dto.AfterSaleAuditRequest;
import org.majun.backend.dto.AfterSaleCreateRequest;
import org.majun.backend.dto.AfterSaleMessageQueryRequest;
import org.majun.backend.dto.AfterSaleMessageSendRequest;
import org.majun.backend.dto.AfterSaleQueryRequest;
import org.majun.backend.dto.AfterSaleRefundRequest;
import org.majun.backend.vo.AfterSaleDetailVO;
import org.majun.backend.vo.AfterSaleListVO;
import org.majun.backend.vo.AfterSaleMessageVO;
import org.majun.backend.vo.PageResult;

/**
 * 订单售后服务接口
 */
public interface OrderAfterSaleService {

    Long createAfterSale(AfterSaleCreateRequest request, Long userId);

    PageResult<AfterSaleListVO> pageMyAfterSales(AfterSaleQueryRequest request, Long userId);

    PageResult<AfterSaleListVO> pageAdminAfterSales(AfterSaleQueryRequest request);

    AfterSaleDetailVO getMyAfterSaleDetail(Long afterSaleId, Long userId);

    AfterSaleDetailVO getMyAfterSaleDetailBySn(String afterSaleSn, Long userId);

    AfterSaleDetailVO getAdminAfterSaleDetail(Long afterSaleId);

    void cancelAfterSale(Long afterSaleId, Long userId);

    void cancelAfterSaleBySn(String afterSaleSn, Long userId);

    void auditAfterSale(AfterSaleAuditRequest request);

    void executeRefund(AfterSaleRefundRequest request);

    void sendMessage(AfterSaleMessageSendRequest request, Long senderId, String senderRole, boolean adminMode);

    PageResult<AfterSaleMessageVO> pageMessages(AfterSaleMessageQueryRequest request, Long userId, boolean adminMode);
}
