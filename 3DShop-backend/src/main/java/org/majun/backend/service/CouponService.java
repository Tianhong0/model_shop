package org.majun.backend.service;

import org.majun.backend.dto.CouponTemplateCreateRequest;
import org.majun.backend.dto.CouponTemplateQueryRequest;
import org.majun.backend.dto.UserCouponQueryRequest;
import org.majun.backend.vo.CouponTemplateVO;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.UserCouponVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券服务接口
 */
public interface CouponService {

    // ========== 用户端接口 ==========

    /**
     * 获取可兑换优惠券列表
     */
    PageResult<CouponTemplateVO> listAvailableTemplates(Long userId);

    /**
     * 积分兑换优惠券
     */
    void exchangeCoupon(Long templateId, Long userId);

    /**
     * 获取我的优惠券列表
     */
    PageResult<UserCouponVO> listMyCoupons(UserCouponQueryRequest request, Long userId);

    /**
     * 获取订单可用优惠券列表
     */
    List<UserCouponVO> listAvailableCouponsForOrder(BigDecimal orderAmount, Long userId);

    /**
     * 计算优惠券折扣金额
     */
    BigDecimal calculateCouponDiscount(Long couponId, BigDecimal orderAmount, Long userId);

    /**
     * 使用优惠券
     */
    void useCoupon(Long couponId, Long orderId, Long userId);

    /**
     * 返还优惠券
     */
    void returnCoupon(Long couponId, Long userId);

    // ========== 管理端接口 ==========

    /**
     * 创建优惠券模板
     */
    Long createTemplate(CouponTemplateCreateRequest request);

    /**
     * 更新优惠券模板状态
     */
    void updateTemplateStatus(Long templateId, Integer status);

    /**
     * 管理端优惠券模板列表
     */
    PageResult<CouponTemplateVO> listTemplatesForAdmin(CouponTemplateQueryRequest request);

    /**
     * 获取优惠券模板详情
     */
    CouponTemplateVO getTemplateDetail(Long templateId);

    /**
     * 更新优惠券模板
     */
    void updateTemplate(Long templateId, CouponTemplateCreateRequest request);

    /**
     * 删除优惠券模板
     */
    void deleteTemplate(Long templateId);
}
