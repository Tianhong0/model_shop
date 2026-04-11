package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.OrderCreateRequest;
import org.majun.backend.dto.OrderQueryRequest;
import org.majun.backend.dto.OrderStatusUpdateRequest;
import org.majun.backend.entity.CouponTemplate;
import org.majun.backend.entity.ModelMaterial;
import org.majun.backend.entity.PointLedger;
import org.majun.backend.entity.PrintJob;
import org.majun.backend.entity.SysModel;
import org.majun.backend.entity.SysModelImage;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.entity.UserCoupon;
import org.majun.backend.enums.PrintJobStatus;
import org.majun.backend.enums.OrderStatus;
import org.majun.backend.repository.CouponTemplateRepository;
import org.majun.backend.repository.ModelMaterialRepository;
import org.majun.backend.repository.PointLedgerRepository;
import org.majun.backend.repository.PrintJobRepository;
import org.majun.backend.repository.SysModelImageRepository;
import org.majun.backend.repository.SysModelRepository;
import org.majun.backend.repository.SysOrderRepository;
import org.majun.backend.repository.UserCouponRepository;
import org.majun.backend.service.CouponService;
import org.majun.backend.service.OrderService;
import org.majun.backend.service.PointService;
import org.majun.backend.vo.OrderCreateResponse;
import org.majun.backend.vo.OrderDetailVO;
import org.majun.backend.vo.OrderListVO;
import org.majun.backend.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Order service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 订单服务实现
 */
public class OrderServiceImpl extends ServiceImpl<SysOrderRepository, SysOrder> implements OrderService {

    private static final int ORDER_SN_RANDOM_BOUND = 10000;
    private static final int ORDER_SN_RETRY_LIMIT = 5;
    private static final BigDecimal MAX_CUSTOM_PREMIUM = BigDecimal.TEN.setScale(2, RoundingMode.HALF_UP);
    private static final int POINTS_PER_YUAN = 100;
    private static final BigDecimal MIN_PAY_AMOUNT = new BigDecimal("0.01");

    private final SysOrderRepository orderRepository;
    private final SysModelRepository modelRepository;
    private final ModelMaterialRepository modelMaterialRepository;
    private final SysModelImageRepository modelImageRepository;
    private final PointService pointService;
    private final CouponService couponService;
    private final PointLedgerRepository pointLedgerRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponTemplateRepository couponTemplateRepository;
    private final PrintJobRepository printJobRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponse createOrder(OrderCreateRequest request, Long userId) {
        SysModel model = modelRepository.selectById(request.getModelId());
        if (model == null || Objects.equals(model.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Model not found");
        }

        ModelMaterial material = null;
        if (request.getMaterialId() != null) {
            material = findModelMaterial(request.getModelId(), request.getMaterialId());
            if (material == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "Material not found");
            }
        }

        PriceBreakdown priceBreakdown = calculatePriceBreakdown(model, material, request, userId);
        String orderSn = generateOrderSn();
        String customParams = buildCustomParams(request, priceBreakdown.getUsedPoints(), priceBreakdown.getPointDiscountAmount(), priceBreakdown.getCouponId(), priceBreakdown.getCouponDiscountAmount());

        SysOrder order = new SysOrder();
        order.setOrderSn(orderSn);
        order.setUserId(userId);
        order.setModelId(request.getModelId());
        order.setMaterialId(request.getMaterialId());
        order.setOrderPrice(priceBreakdown.getPayAmount());
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setCustomParams(customParams);
        order.setIsDelete(0);

        orderRepository.insert(order);

        if (priceBreakdown.getUsedPoints() > 0) {
            pointService.consumeOrderPoints(userId, order.getId(), orderSn, priceBreakdown.getUsedPoints());
        }

        OrderCreateResponse response = new OrderCreateResponse();
        response.setOrderId(order.getId());
        response.setOrderSn(orderSn);
        response.setOrderPrice(priceBreakdown.getPayAmount());
        response.setBasePrice(priceBreakdown.getBasePrice());
        response.setMaterialCost(priceBreakdown.getMaterialCost());
        response.setGoodsAmount(priceBreakdown.getGoodsAmount());
        response.setShippingFee(priceBreakdown.getShippingFee());
        response.setDiscountAmount(priceBreakdown.getDiscountAmount());
        response.setPayAmount(priceBreakdown.getPayAmount());
        response.setUsedPoints(priceBreakdown.getUsedPoints());
        response.setPointDiscountAmount(priceBreakdown.getPointDiscountAmount());
        return response;
    }

    @Override
    public PageResult<OrderListVO> getUserOrders(OrderQueryRequest request, Long userId) {
        return queryOrders(request, userId);
    }

    @Override
    public PageResult<OrderListVO> getAdminOrders(OrderQueryRequest request) {
        return queryOrders(request, request.getUserId());
    }

    @Override
    public OrderDetailVO getOrderDetail(Long orderId, Long userId) {
        SysOrder order = getOrderOrThrow(orderId);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "No permission to access this order");
        }
        return buildOrderDetail(order);
    }

    @Override
    public OrderDetailVO getOrderDetailByOrderSn(String orderSn, Long userId) {
        if (!StringUtils.hasText(orderSn)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "Order serial number is required");
        }
        SysOrder order = orderRepository.selectOne(new LambdaQueryWrapper<SysOrder>()
                .eq(SysOrder::getOrderSn, orderSn.trim())
                .eq(SysOrder::getIsDelete, 0)
                .last("limit 1"));
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Order not found");
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "No permission to access this order");
        }
        return buildOrderDetail(order);
    }

    @Override
    public OrderDetailVO getAdminOrderDetail(Long orderId) {
        SysOrder order = getOrderOrThrow(orderId);
        return buildOrderDetail(order);
    }

    @Override
    public void cancelOrder(Long orderId, Long userId) {
        SysOrder order = getOrderOrThrow(orderId);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "No permission to cancel this order");
        }
        if (!Objects.equals(order.getOrderStatus(), OrderStatus.PENDING_PAYMENT.getCode())) {
            throw new BusinessException("Only pending payment orders can be canceled");
        }

        // 返还积分
        int usedPoints = parseUsedPointsFromCustomParams(order.getCustomParams());
        if (usedPoints > 0) {
            pointService.refundOrderPoints(order.getUserId(), order.getId(), order.getOrderSn(), usedPoints);
        }

        // 返还优惠券
        Long couponId = parseCouponIdFromCustomParams(order.getCustomParams());
        if (couponId != null) {
            try {
                couponService.returnCoupon(couponId, userId);
            } catch (Exception e) {
                log.warn("返还优惠券失败: couponId={}, error={}", couponId, e.getMessage());
            }
        }

        order.setOrderStatus(OrderStatus.CANCELED.getCode());
        orderRepository.updateById(order);
    }

    @Override
    public void deleteOrder(Long orderId, Long userId) {
        SysOrder order = getOrderOrThrow(orderId);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "No permission to delete this order");
        }
        if (!Objects.equals(order.getOrderStatus(), OrderStatus.COMPLETED.getCode())
                && !Objects.equals(order.getOrderStatus(), OrderStatus.CANCELED.getCode())) {
            throw new BusinessException("Only completed or canceled orders can be deleted");
        }
        int affected = orderRepository.deleteById(orderId);
        if (affected <= 0) {
            throw new BusinessException("Delete order failed");
        }
    }

    @Override
    public void updateOrderStatus(OrderStatusUpdateRequest request) {
        SysOrder order = getOrderOrThrow(request.getOrderId());
        OrderStatus targetStatus = OrderStatus.fromCode(request.getStatus());
        if (targetStatus == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "Invalid order status");
        }

        if (Objects.equals(order.getOrderStatus(), OrderStatus.COMPLETED.getCode())
                || Objects.equals(order.getOrderStatus(), OrderStatus.CANCELED.getCode())) {
            throw new BusinessException("Order status cannot be changed");
        }

        order.setOrderStatus(targetStatus.getCode());
        if (request.getPrinterId() != null) {
            order.setPrinterId(request.getPrinterId());
        }
        orderRepository.updateById(order);
    }

    private PageResult<OrderListVO> queryOrders(OrderQueryRequest request, Long userId) {
        LambdaQueryWrapper<SysOrder> queryWrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            queryWrapper.eq(SysOrder::getUserId, userId);
        }
        if (request.getOrderStatus() != null) {
            queryWrapper.eq(SysOrder::getOrderStatus, request.getOrderStatus());
        }
        if (request.getModelId() != null) {
            queryWrapper.eq(SysOrder::getModelId, request.getModelId());
        }
        if (StringUtils.hasText(request.getOrderSn())) {
            queryWrapper.like(SysOrder::getOrderSn, request.getOrderSn());
        }
        queryWrapper.eq(SysOrder::getIsDelete, 0);
        queryWrapper.orderByDesc(SysOrder::getCreateTime);

        Page<SysOrder> page = new Page<>(request.getPageNum(), request.getPageSize());
        orderRepository.selectPage(page, queryWrapper);

        List<SysOrder> orders = page.getRecords();
        if (CollectionUtils.isEmpty(orders)) {
            return PageResult.<OrderListVO>builder()
                    .records(Collections.emptyList())
                    .total(0L)
                    .pageNum(request.getPageNum())
                    .pageSize(request.getPageSize())
                    .pages(0)
                    .build();
        }

        List<Long> modelIds = orders.stream()
                .map(SysOrder::getModelId)
                .distinct()
                .toList();

        Map<Long, SysModel> modelMap = loadModelMap(modelIds);
        Map<Long, String> mainImageMap = loadMainImageMap(modelIds);

        List<OrderListVO> records = orders.stream()
                .map(order -> buildOrderListItem(order, modelMap, mainImageMap))
                .collect(Collectors.toList());

        return PageResult.<OrderListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    private SysOrder getOrderOrThrow(Long orderId) {
        SysOrder order = orderRepository.selectById(orderId);
        if (order == null || Objects.equals(order.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Order not found");
        }
        return order;
    }

    private OrderListVO buildOrderListItem(SysOrder order, Map<Long, SysModel> modelMap, Map<Long, String> mainImageMap) {
        SysModel model = modelMap.get(order.getModelId());

        OrderListVO vo = new OrderListVO();
        vo.setId(order.getId());
        vo.setOrderSn(order.getOrderSn());
        vo.setModelId(order.getModelId());
        vo.setModelName(model != null ? model.getModelName() : null);
        vo.setMainImageUrl(resolveMainImage(order.getModelId(), model, mainImageMap));
        vo.setOrderPrice(order.getOrderPrice());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    private OrderDetailVO buildOrderDetail(SysOrder order) {
        SysModel model = modelRepository.selectById(order.getModelId());
        ModelMaterial material = null;
        if (order.getMaterialId() != null) {
            material = findModelMaterial(order.getModelId(), order.getMaterialId());
        }

        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderSn(order.getOrderSn());
        vo.setUserId(order.getUserId());
        vo.setModelId(order.getModelId());
        vo.setModelName(model != null ? model.getModelName() : null);
        vo.setMainImageUrl(resolveMainImage(order.getModelId(), model, loadMainImageMap(List.of(order.getModelId()))));
        vo.setMaterialId(order.getMaterialId());
        vo.setMaterialName(material != null ? material.getMaterialName() : null);
        vo.setMaterialColor(parseColorFromCustomParams(order.getCustomParams()));
        vo.setOrderPrice(order.getOrderPrice());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setPrinterId(order.getPrinterId());
        vo.setCustomParams(order.getCustomParams());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());

        // 解析积分和优惠券信息
        parsePointsAndCouponInfo(order, vo);

        // 查询打印任务状态
        fillPrintJobStatus(order.getId(), vo);

        return vo;
    }

    private void parsePointsAndCouponInfo(SysOrder order, OrderDetailVO vo) {
        String customParams = order.getCustomParams();
        if (!StringUtils.hasText(customParams)) {
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(customParams);

            // 积分信息
            JsonNode usedPointsNode = node.get("usedPoints");
            if (usedPointsNode != null && !usedPointsNode.isNull()) {
                vo.setUsedPoints(usedPointsNode.asInt(0));
            }
            JsonNode pointDiscountNode = node.get("pointDiscountAmount");
            if (pointDiscountNode != null && !pointDiscountNode.isNull()) {
                vo.setPointDiscountAmount(new BigDecimal(pointDiscountNode.asText("0")));
            }

            // 优惠券信息
            JsonNode couponIdNode = node.get("couponId");
            if (couponIdNode != null && !couponIdNode.isNull()) {
                Long couponId = couponIdNode.asLong();
                vo.setCouponId(couponId);
                // 查询优惠券名称
                if (couponId != null && couponId > 0) {
                    UserCoupon userCoupon = userCouponRepository.selectById(couponId);
                    if (userCoupon != null) {
                        CouponTemplate template = couponTemplateRepository.selectById(userCoupon.getTemplateId());
                        if (template != null) {
                            vo.setCouponName(template.getName());
                        }
                    }
                }
            }
            JsonNode couponDiscountNode = node.get("couponDiscountAmount");
            if (couponDiscountNode != null && !couponDiscountNode.isNull()) {
                vo.setCouponDiscountAmount(new BigDecimal(couponDiscountNode.asText("0")));
            }

            // 价格明细
            JsonNode basePriceNode = node.get("basePrice");
            if (basePriceNode != null && !basePriceNode.isNull()) {
                vo.setBasePrice(new BigDecimal(basePriceNode.asText("0")));
            }
            JsonNode materialCostNode = node.get("materialCost");
            if (materialCostNode != null && !materialCostNode.isNull()) {
                vo.setMaterialCost(new BigDecimal(materialCostNode.asText("0")));
            }

            // 查询订单获得的积分
            PointLedger rewardLedger = pointLedgerRepository.selectOne(
                new LambdaQueryWrapper<PointLedger>()
                    .eq(PointLedger::getRefId, order.getId())
                    .eq(PointLedger::getBizType, PointService.BIZ_ORDER_PAY)
                    .last("LIMIT 1")
            );
            if (rewardLedger != null) {
                vo.setEarnedPoints(rewardLedger.getPoints());
            }

        } catch (Exception e) {
            log.warn("解析订单积分优惠券信息失败: orderId={}, error={}", order.getId(), e.getMessage());
        }
    }

    private Map<Long, SysModel> loadModelMap(List<Long> modelIds) {
        if (CollectionUtils.isEmpty(modelIds)) {
            return Collections.emptyMap();
        }
        List<SysModel> models = modelRepository.selectBatchIds(modelIds);
        return models.stream()
                .filter(model -> !Objects.equals(model.getIsDelete(), 1))
                .collect(Collectors.toMap(SysModel::getId, model -> model));
    }

    private Map<Long, String> loadMainImageMap(List<Long> modelIds) {
        if (CollectionUtils.isEmpty(modelIds)) {
            return Collections.emptyMap();
        }
        List<SysModelImage> images = modelImageRepository.selectList(
                new LambdaQueryWrapper<SysModelImage>()
                        .in(SysModelImage::getModelId, modelIds)
                        .eq(SysModelImage::getIsMain, 1)
        );
        Map<Long, String> map = new HashMap<>();
        for (SysModelImage image : images) {
            map.putIfAbsent(image.getModelId(), image.getImageUrl());
        }
        return map;
    }

    private String resolveMainImage(Long modelId, SysModel model, Map<Long, String> mainImageMap) {
        String image = mainImageMap.get(modelId);
        if (StringUtils.hasText(image)) {
            return image;
        }
        return model != null ? model.getFilePath() : null;
    }

    private PriceBreakdown calculatePriceBreakdown(SysModel model, ModelMaterial material, OrderCreateRequest request, Long userId) {
        BigDecimal basePrice = model.getBasePrice() != null ? model.getBasePrice() : BigDecimal.ZERO;
        BigDecimal volume = model.getBaseVolume() != null ? model.getBaseVolume() : BigDecimal.ZERO;
        BigDecimal scale = request.getScale() != null ? request.getScale() : BigDecimal.ONE;
        BigDecimal fillPercent = request.getFillPercent() != null ? request.getFillPercent() : BigDecimal.valueOf(100);
        BigDecimal fillFactor = fillPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal m = material != null && material.getPrice() != null
                ? BigDecimal.valueOf(material.getPrice())
                : BigDecimal.ZERO;

        BigDecimal rawMaterialCost = volume.multiply(scale).multiply(fillFactor).multiply(materialPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal materialCost = rawMaterialCost.max(BigDecimal.ZERO).min(MAX_CUSTOM_PREMIUM).setScale(2, RoundingMode.HALF_UP);
        BigDecimal goodsAmount = basePrice.add(materialCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // 积分抵扣
        int requestedPoints = request.getUsePoints() == null ? 0 : Math.max(request.getUsePoints(), 0);
        int availablePoints = 0;
        if (requestedPoints > 0) {
            availablePoints = Math.max(pointService.getAccount(userId).getAvailablePoints(), 0);
        }
        int candidatePoints = Math.min(requestedPoints, availablePoints);
        BigDecimal maxDiscountByPoints = new BigDecimal(candidatePoints)
                .divide(new BigDecimal(POINTS_PER_YUAN), 2, RoundingMode.DOWN);

        BigDecimal maxAllowDiscount = goodsAmount.add(shippingFee).subtract(MIN_PAY_AMOUNT).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pointDiscountAmount = maxDiscountByPoints.min(maxAllowDiscount).setScale(2, RoundingMode.HALF_UP);
        int usedPoints = pointDiscountAmount.multiply(new BigDecimal(POINTS_PER_YUAN)).setScale(0, RoundingMode.DOWN).intValue();
        if (usedPoints <= 0) {
            pointDiscountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        discountAmount = discountAmount.add(pointDiscountAmount);

        // 优惠券抵扣
        Long couponId = request.getCouponId();
        BigDecimal couponDiscountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountAfterPointDiscount = goodsAmount.add(shippingFee).subtract(discountAmount);
        if (couponId != null) {
            try {
                couponDiscountAmount = couponService.calculateCouponDiscount(couponId, amountAfterPointDiscount, userId);
                if (couponDiscountAmount != null && couponDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // 优惠券折扣不能超过剩余金额减去最低支付金额
                    BigDecimal maxCouponDiscount = amountAfterPointDiscount.subtract(MIN_PAY_AMOUNT).max(BigDecimal.ZERO);
                    if (couponDiscountAmount.compareTo(maxCouponDiscount) > 0) {
                        couponDiscountAmount = maxCouponDiscount;
                    }
                    discountAmount = discountAmount.add(couponDiscountAmount);
                } else {
                    couponDiscountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    couponId = null;
                }
            } catch (Exception e) {
                log.warn("优惠券计算失败: {}", e.getMessage());
                couponId = null;
                couponDiscountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal payAmount = goodsAmount.add(shippingFee).subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
        if (payAmount.compareTo(MIN_PAY_AMOUNT) < 0) {
            payAmount = MIN_PAY_AMOUNT;
        }

        PriceBreakdown breakdown = new PriceBreakdown();
        breakdown.setBasePrice(basePrice.setScale(2, RoundingMode.HALF_UP));
        breakdown.setMaterialCost(materialCost);
        breakdown.setGoodsAmount(goodsAmount);
        breakdown.setShippingFee(shippingFee);
        breakdown.setDiscountAmount(discountAmount);
        breakdown.setPayAmount(payAmount);
        breakdown.setUsedPoints(usedPoints);
        breakdown.setPointDiscountAmount(pointDiscountAmount);
        breakdown.setCouponId(couponId);
        breakdown.setCouponDiscountAmount(couponDiscountAmount);
        return breakdown;
    }

    @lombok.Data
    private static class PriceBreakdown {
        private BigDecimal basePrice;
        private BigDecimal materialCost;
        private BigDecimal goodsAmount;
        private BigDecimal shippingFee;
        private BigDecimal discountAmount;
        private BigDecimal payAmount;
        private Integer usedPoints;
        private BigDecimal pointDiscountAmount;
        private Long couponId;
        private BigDecimal couponDiscountAmount;
    }

    private String buildCustomParams(OrderCreateRequest request, Integer usedPoints, BigDecimal pointDiscountAmount, Long couponId, BigDecimal couponDiscountAmount) {
        Map<String, Object> params = new LinkedHashMap<>();

        if (StringUtils.hasText(request.getCustomParams())) {
            try {
                JsonNode customNode = objectMapper.readTree(request.getCustomParams());
                if (customNode != null && customNode.isObject()) {
                    customNode.fields().forEachRemaining(entry -> params.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class)));
                } else {
                    params.put("rawCustomParams", request.getCustomParams());
                }
            } catch (Exception ex) {
                params.put("rawCustomParams", request.getCustomParams());
            }
        }

        params.put("materialId", request.getMaterialId());
        params.put("color", request.getColor());
        params.put("scale", request.getScale());
        params.put("fillPercent", request.getFillPercent());
        params.put("note", request.getNote());
        params.put("usedPoints", usedPoints == null ? 0 : Math.max(usedPoints, 0));
        params.put("pointDiscountAmount", pointDiscountAmount == null ? BigDecimal.ZERO : pointDiscountAmount);
        params.put("couponId", couponId);
        params.put("couponDiscountAmount", couponDiscountAmount == null ? BigDecimal.ZERO : couponDiscountAmount);

        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize custom params", e);
        }
    }

    private int parseUsedPointsFromCustomParams(String customParams) {
        if (!StringUtils.hasText(customParams)) {
            return 0;
        }
        try {
            JsonNode node = objectMapper.readTree(customParams);
            JsonNode pointsNode = node.get("usedPoints");
            if (pointsNode == null || pointsNode.isNull()) {
                return 0;
            }
            int points = pointsNode.asInt(0);
            return Math.max(points, 0);
        } catch (Exception ex) {
            return 0;
        }
    }

    private String parseColorFromCustomParams(String customParams) {
        if (!StringUtils.hasText(customParams)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(customParams);
            JsonNode colorNode = node.get("color");
            if (colorNode == null || colorNode.isNull()) {
                return null;
            }
            String color = colorNode.asText();
            return StringUtils.hasText(color) ? color : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private Long parseCouponIdFromCustomParams(String customParams) {
        if (!StringUtils.hasText(customParams)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(customParams);
            JsonNode couponIdNode = node.get("couponId");
            if (couponIdNode == null || couponIdNode.isNull()) {
                return null;
            }
            return couponIdNode.asLong();
        } catch (Exception ex) {
            return null;
        }
    }

    private String generateOrderSn() {
        String prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        for (int i = 0; i < ORDER_SN_RETRY_LIMIT; i++) {
            int suffix = ThreadLocalRandom.current().nextInt(ORDER_SN_RANDOM_BOUND);
            String orderSn = prefix + String.format("%04d", suffix);
            if (!orderSnExists(orderSn)) {
                return orderSn;
            }
        }
        throw new BusinessException("Failed to generate order serial number");
    }

    private boolean orderSnExists(String orderSn) {
        return orderRepository.selectCount(
                new LambdaQueryWrapper<SysOrder>()
                        .eq(SysOrder::getOrderSn, orderSn)
        ) > 0;
    }

    private ModelMaterial findModelMaterial(Long modelId, Long materialId) {
        return modelMaterialRepository.selectOne(
                new LambdaQueryWrapper<ModelMaterial>()
                        .eq(ModelMaterial::getModelId, modelId)
                        .eq(ModelMaterial::getMaterialId, materialId)
                        .last("LIMIT 1")
        );
    }

    private void fillPrintJobStatus(Long orderId, OrderDetailVO vo) {
        if (orderId == null) {
            return;
        }
        PrintJob printJob = printJobRepository.selectOne(
                new LambdaQueryWrapper<PrintJob>()
                        .eq(PrintJob::getOrderId, orderId)
                        .eq(PrintJob::getIsDelete, 0)
                        .last("LIMIT 1")
        );
        if (printJob == null) {
            return;
        }
        vo.setPrintJobStatus(printJob.getStatus());
        vo.setPrintJobStatusDesc(getPrintJobStatusDesc(printJob.getStatus()));
        vo.setPrintProgress(printJob.getProgress());
        vo.setPrintErrorMessage(printJob.getErrorMessage());
    }

    private String getPrintJobStatusDesc(Integer status) {
        if (status == null) {
            return null;
        }
        PrintJobStatus jobStatus = PrintJobStatus.fromCode(status);
        return jobStatus != null ? jobStatus.getDescription() : null;
    }
}
