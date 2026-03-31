package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.CouponTemplateCreateRequest;
import org.majun.backend.dto.CouponTemplateQueryRequest;
import org.majun.backend.dto.UserCouponQueryRequest;
import org.majun.backend.entity.CouponTemplate;
import org.majun.backend.entity.UserCoupon;
import org.majun.backend.enums.CouponType;
import org.majun.backend.enums.UserCouponStatus;
import org.majun.backend.repository.CouponTemplateRepository;
import org.majun.backend.repository.UserCouponRepository;
import org.majun.backend.service.CouponService;
import org.majun.backend.service.PointService;
import org.majun.backend.vo.CouponTemplateVO;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.UserCouponVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 优惠券服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl implements CouponService {

    private static final String BIZ_COUPON_EXCHANGE = "COUPON_EXCHANGE";
    private static final String BIZ_COUPON_REFUND = "COUPON_REFUND";

    private final CouponTemplateRepository templateRepository;
    private final UserCouponRepository userCouponRepository;
    private final PointService pointService;

    // ========== 用户端接口实现 ==========

    @Override
    public PageResult<CouponTemplateVO> listAvailableTemplates(Long userId) {
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, 1)
                .gt(CouponTemplate::getRemainingStock, 0)
                .orderByAsc(CouponTemplate::getPointCost);

        List<CouponTemplate> templates = templateRepository.selectList(wrapper);

        List<CouponTemplateVO> records = templates.stream().map(t -> {
            CouponTemplateVO vo = toTemplateVO(t);
            // 查询用户已领取数量
            Long receivedCount = userCouponRepository.selectCount(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getTemplateId, t.getId()));
            vo.setUserReceivedCount(receivedCount != null ? receivedCount.intValue() : 0);
            return vo;
        }).toList();

        return PageResult.<CouponTemplateVO>builder()
                .records(records)
                .total((long) records.size())
                .pageNum(1)
                .pageSize(records.size())
                .pages(1)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exchangeCoupon(Long templateId, Long userId) {
        // 查询模板
        CouponTemplate template = templateRepository.selectById(templateId);
        if (template == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (template.getStatus() != 1) {
            throw new BusinessException("优惠券已下架");
        }
        if (template.getRemainingStock() <= 0) {
            throw new BusinessException("优惠券已抢光");
        }

        // 检查用户限领
        Long receivedCount = userCouponRepository.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getTemplateId, templateId));
        int limit = template.getPerUserLimit() != null ? template.getPerUserLimit() : 1;
        if (receivedCount != null && receivedCount >= limit) {
            throw new BusinessException("已达到领取上限");
        }

        // 检查积分是否足够
        int pointCost = template.getPointCost();
        String bizNo = generateCouponBizNo(templateId);

        // 扣除积分
        pointService.decrease(userId, pointCost, BIZ_COUPON_EXCHANGE, bizNo, templateId, "兑换优惠券：" + template.getName());

        // 扣减库存
        LambdaUpdateWrapper<CouponTemplate> updateWrapper = new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .gt(CouponTemplate::getRemainingStock, 0)
                .setSql("remaining_stock = remaining_stock - 1");
        int updated = templateRepository.update(null, updateWrapper);
        if (updated <= 0) {
            throw new BusinessException("优惠券已抢光");
        }

        // 创建用户优惠券
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = template.getStartTime() != null ? template.getStartTime() : now;
        LocalDateTime endTime = template.getEndTime() != null ? template.getEndTime() :
                now.plusDays(template.getValidDays() != null ? template.getValidDays() : 30);

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setTemplateId(templateId);
        userCoupon.setCouponNo(generateCouponNo());
        userCoupon.setType(template.getType());
        userCoupon.setValue(template.getValue());
        userCoupon.setMinAmount(template.getMinAmount());
        userCoupon.setMaxDiscount(template.getMaxDiscount());
        userCoupon.setStatus(UserCouponStatus.UNUSED.getCode());
        userCoupon.setStartTime(startTime);
        userCoupon.setEndTime(endTime);
        userCoupon.setPointCost(pointCost);
        userCouponRepository.insert(userCoupon);

        log.info("用户 {} 成功兑换优惠券 {}", userId, templateId);
    }

    @Override
    public PageResult<UserCouponVO> listMyCoupons(UserCouponQueryRequest request, Long userId) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getCreateTime);

        if (request.getStatus() != null) {
            wrapper.eq(UserCoupon::getStatus, request.getStatus());
        }

        Page<UserCoupon> page = new Page<>(request.getPageNum(), request.getPageSize());
        userCouponRepository.selectPage(page, wrapper);

        List<UserCouponVO> records = page.getRecords().stream()
                .map(this::toUserCouponVO)
                .toList();

        return PageResult.<UserCouponVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public List<UserCouponVO> listAvailableCouponsForOrder(BigDecimal orderAmount, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // 查询用户所有未使用且在有效期内的优惠券
        List<UserCoupon> coupons = userCouponRepository.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode())
                .le(UserCoupon::getStartTime, now)
                .gt(UserCoupon::getEndTime, now));

        // 过滤出满足门槛条件的优惠券
        return coupons.stream()
                .filter(c -> {
                    BigDecimal minAmount = c.getMinAmount();
                    // 无门槛或门槛为0，都可用
                    if (minAmount == null || minAmount.compareTo(BigDecimal.ZERO) == 0) {
                        return true;
                    }
                    // 门槛 <= 订单金额，可用
                    return minAmount.compareTo(orderAmount) <= 0;
                })
                .map(c -> {
                    UserCouponVO vo = toUserCouponVO(c);
                    vo.setAvailable(true);
                    return vo;
                })
                .toList();
    }

    @Override
    public BigDecimal calculateCouponDiscount(Long couponId, BigDecimal orderAmount, Long userId) {
        UserCoupon coupon = validateAndGetCoupon(couponId, userId, orderAmount);
        return calculateDiscount(coupon, orderAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(Long couponId, Long orderId, Long userId) {
        UserCoupon coupon = userCouponRepository.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getUserId, userId));

        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (coupon.getStatus() != UserCouponStatus.UNUSED.getCode()) {
            throw new BusinessException("优惠券不可用");
        }

        LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode())
                .set(UserCoupon::getStatus, UserCouponStatus.USED.getCode())
                .set(UserCoupon::getOrderId, orderId)
                .set(UserCoupon::getUsedTime, LocalDateTime.now());

        int updated = userCouponRepository.update(null, updateWrapper);
        if (updated <= 0) {
            throw new BusinessException("优惠券使用失败");
        }

        log.info("用户 {} 使用优惠券 {} 用于订单 {}", userId, couponId, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnCoupon(Long couponId, Long userId) {
        UserCoupon coupon = userCouponRepository.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getUserId, userId));

        if (coupon == null || coupon.getStatus() != UserCouponStatus.USED.getCode()) {
            return;
        }

        LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getStatus, UserCouponStatus.USED.getCode())
                .set(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode())
                .set(UserCoupon::getOrderId, null)
                .set(UserCoupon::getUsedTime, null);

        userCouponRepository.update(null, updateWrapper);

        // 检查是否过期
        if (coupon.getEndTime().isBefore(LocalDateTime.now())) {
            userCouponRepository.update(new LambdaUpdateWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, couponId)
                    .set(UserCoupon::getStatus, UserCouponStatus.EXPIRED.getCode()));
        }

        log.info("用户 {} 优惠券 {} 已返还", userId, couponId);
    }

    // ========== 管理端接口实现 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(CouponTemplateCreateRequest request) {
        CouponTemplate template = new CouponTemplate();
        template.setName(request.getName());
        template.setType(request.getType());
        template.setValue(request.getValue());
        template.setMinAmount(request.getMinAmount() != null ? request.getMinAmount() : BigDecimal.ZERO);
        template.setMaxDiscount(request.getMaxDiscount());
        template.setPointCost(request.getPointCost());
        template.setTotalStock(request.getTotalStock());
        template.setRemainingStock(request.getTotalStock());
        template.setPerUserLimit(request.getPerUserLimit() != null ? request.getPerUserLimit() : 1);
        template.setValidDays(request.getValidDays() != null ? request.getValidDays() : 30);
        template.setStartTime(request.getStartTime());
        template.setEndTime(request.getEndTime());
        template.setDescription(request.getDescription());
        template.setStatus(1);

        templateRepository.insert(template);
        log.info("创建优惠券模板 {}", template.getId());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplateStatus(Long templateId, Integer status) {
        LambdaUpdateWrapper<CouponTemplate> updateWrapper = new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .set(CouponTemplate::getStatus, status);

        int updated = templateRepository.update(null, updateWrapper);
        if (updated <= 0) {
            throw new BusinessException("优惠券模板不存在");
        }
    }

    @Override
    public PageResult<CouponTemplateVO> listTemplatesForAdmin(CouponTemplateQueryRequest request) {
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<CouponTemplate>()
                .orderByDesc(CouponTemplate::getCreateTime);

        if (request.getStatus() != null) {
            wrapper.eq(CouponTemplate::getStatus, request.getStatus());
        }
        if (request.getType() != null) {
            wrapper.eq(CouponTemplate::getType, request.getType());
        }

        Page<CouponTemplate> page = new Page<>(request.getPageNum(), request.getPageSize());
        templateRepository.selectPage(page, wrapper);

        List<CouponTemplateVO> records = page.getRecords().stream()
                .map(this::toTemplateVO)
                .toList();

        return PageResult.<CouponTemplateVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public CouponTemplateVO getTemplateDetail(Long templateId) {
        CouponTemplate template = templateRepository.selectById(templateId);
        if (template == null) {
            throw new BusinessException("优惠券模板不存在");
        }
        return toTemplateVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long templateId, CouponTemplateCreateRequest request) {
        CouponTemplate template = templateRepository.selectById(templateId);
        if (template == null) {
            throw new BusinessException("优惠券模板不存在");
        }

        // 更新基本信息
        template.setName(request.getName());
        template.setType(request.getType());
        template.setValue(request.getValue());
        template.setMinAmount(request.getMinAmount() != null ? request.getMinAmount() : BigDecimal.ZERO);
        template.setMaxDiscount(request.getMaxDiscount());
        template.setPointCost(request.getPointCost());
        template.setPerUserLimit(request.getPerUserLimit() != null ? request.getPerUserLimit() : 1);
        template.setValidDays(request.getValidDays() != null ? request.getValidDays() : 30);
        template.setStartTime(request.getStartTime());
        template.setEndTime(request.getEndTime());
        template.setDescription(request.getDescription());

        // 如果新库存大于原总库存，则增加剩余库存
        int stockDiff = request.getTotalStock() - template.getTotalStock();
        template.setTotalStock(request.getTotalStock());
        if (stockDiff > 0) {
            template.setRemainingStock(template.getRemainingStock() + stockDiff);
        } else if (stockDiff < 0) {
            // 减少库存时，剩余库存不能小于0
            int newRemaining = template.getRemainingStock() + stockDiff;
            if (newRemaining < 0) {
                throw new BusinessException("库存已使用，无法减少到指定数量");
            }
            template.setRemainingStock(newRemaining);
        }

        templateRepository.updateById(template);
        log.info("更新优惠券模板 {}", templateId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        CouponTemplate template = templateRepository.selectById(templateId);
        if (template == null) {
            throw new BusinessException("优惠券模板不存在");
        }

        // 检查是否有用户已领取但未使用的优惠券
        Long unusedCount = userCouponRepository.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getTemplateId, templateId)
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode()));
        if (unusedCount != null && unusedCount > 0) {
            throw new BusinessException("存在未使用的优惠券，无法删除");
        }

        // 逻辑删除
        templateRepository.deleteById(templateId);
        log.info("删除优惠券模板 {}", templateId);
    }

    // ========== 私有方法 ==========

    private UserCoupon validateAndGetCoupon(Long couponId, Long userId, BigDecimal orderAmount) {
        UserCoupon coupon = userCouponRepository.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getUserId, userId));

        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (coupon.getStatus() != UserCouponStatus.UNUSED.getCode()) {
            throw new BusinessException("优惠券不可用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime().isAfter(now) || coupon.getEndTime().isBefore(now)) {
            throw new BusinessException("优惠券不在有效期内");
        }
        BigDecimal minAmount = coupon.getMinAmount() != null ? coupon.getMinAmount() : BigDecimal.ZERO;
        if (orderAmount.compareTo(minAmount) < 0) {
            throw new BusinessException("订单金额不满足优惠券使用门槛");
        }

        return coupon;
    }

    private BigDecimal calculateDiscount(UserCoupon coupon, BigDecimal orderAmount) {
        BigDecimal discount = BigDecimal.ZERO;
        int type = coupon.getType();

        if (type == CouponType.FULL_REDUCTION.getCode() || type == CouponType.CASH.getCode()) {
            // 满减券或现金券：直接减免
            discount = coupon.getValue();
        } else if (type == CouponType.DISCOUNT.getCode()) {
            // 折扣券：计算折扣金额
            // value 是折扣比例，如 0.8 表示 8 折
            BigDecimal discountRate = coupon.getValue();
            BigDecimal originalAmount = orderAmount;
            discount = originalAmount.multiply(BigDecimal.ONE.subtract(discountRate));

            // 检查最大优惠金额
            if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
        }

        // 优惠金额不能超过订单金额
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private String generateCouponNo() {
        return System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String generateCouponBizNo(Long templateId) {
        return "CP" + templateId + "_" + System.currentTimeMillis();
    }

    private CouponTemplateVO toTemplateVO(CouponTemplate entity) {
        CouponTemplateVO vo = new CouponTemplateVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setType(entity.getType());
        vo.setTypeDesc(CouponType.fromCode(entity.getType()) != null ?
                CouponType.fromCode(entity.getType()).getDescription() : "未知");
        vo.setValue(entity.getValue());
        vo.setMinAmount(entity.getMinAmount());
        vo.setMaxDiscount(entity.getMaxDiscount());
        vo.setPointCost(entity.getPointCost());
        vo.setRemainingStock(entity.getRemainingStock());
        vo.setTotalStock(entity.getTotalStock());
        vo.setPerUserLimit(entity.getPerUserLimit());
        vo.setValidDays(entity.getValidDays());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private UserCouponVO toUserCouponVO(UserCoupon entity) {
        UserCouponVO vo = new UserCouponVO();
        vo.setId(entity.getId());
        vo.setCouponNo(entity.getCouponNo());
        vo.setType(entity.getType());
        vo.setTypeDesc(CouponType.fromCode(entity.getType()) != null ?
                CouponType.fromCode(entity.getType()).getDescription() : "未知");
        vo.setValue(entity.getValue());
        vo.setMinAmount(entity.getMinAmount());
        vo.setMaxDiscount(entity.getMaxDiscount());
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(UserCouponStatus.fromCode(entity.getStatus()) != null ?
                UserCouponStatus.fromCode(entity.getStatus()).getDescription() : "未知");
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setOrderId(entity.getOrderId());
        vo.setUsedTime(entity.getUsedTime());
        vo.setPointCost(entity.getPointCost());
        vo.setCreateTime(entity.getCreateTime());

        // 获取模板名称
        CouponTemplate template = templateRepository.selectById(entity.getTemplateId());
        vo.setName(template != null ? template.getName() : "优惠券");

        // 检查是否可用
        LocalDateTime now = LocalDateTime.now();
        boolean available = entity.getStatus() == UserCouponStatus.UNUSED.getCode()
                && entity.getStartTime().isBefore(now)
                && entity.getEndTime().isAfter(now);
        vo.setAvailable(available);

        return vo;
    }
}
