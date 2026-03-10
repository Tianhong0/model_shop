package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.DeliveryQueryRequest;
import org.majun.backend.dto.DeliveryShipRequest;
import org.majun.backend.dto.DeliveryStatusUpdateRequest;
import org.majun.backend.dto.DeliveryTrackAddRequest;
import org.majun.backend.dto.DeliveryTrackSimulateRequest;
import org.majun.backend.dto.UserNotificationCreateCommand;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.entity.SysOrderDelivery;
import org.majun.backend.entity.SysOrderDeliveryTrack;
import org.majun.backend.enums.DeliveryStatus;
import org.majun.backend.enums.OrderStatus;
import org.majun.backend.repository.SysOrderDeliveryRepository;
import org.majun.backend.repository.SysOrderDeliveryTrackRepository;
import org.majun.backend.repository.SysOrderRepository;
import org.majun.backend.service.OrderDeliveryService;
import org.majun.backend.service.UserNotificationService;
import org.majun.backend.vo.DeliveryDetailVO;
import org.majun.backend.vo.DeliveryListVO;
import org.majun.backend.vo.DeliveryTrackVO;
import org.majun.backend.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDeliveryServiceImpl implements OrderDeliveryService {

    private static final List<String> AUTO_SIM_COMPANY_LIST = List.of("顺丰速运", "中通快递", "圆通速递", "韵达快递", "申通快递", "京东快递", "EMS");
    private static final DateTimeFormatter DELIVERY_SN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SysOrderRepository orderRepository;
    private final SysOrderDeliveryRepository deliveryRepository;
    private final SysOrderDeliveryTrackRepository trackRepository;
    private final ObjectMapper objectMapper;
    private final UserNotificationService userNotificationService;

    @Override
    public Long shipOrder(DeliveryShipRequest request) {
        SysOrder order = getOrderByOrderSn(request.getOrderSn());
        validateOrderShippable(order);

        SysOrderDelivery existed = deliveryRepository.selectOne(new LambdaQueryWrapper<SysOrderDelivery>()
                .eq(SysOrderDelivery::getOrderId, order.getId())
                .eq(SysOrderDelivery::getIsDelete, 0));
        if (existed != null) {
            throw new BusinessException("该订单已存在物流单");
        }

        SysOrderDelivery delivery = new SysOrderDelivery();
        delivery.setOrderId(order.getId());
        delivery.setOrderSn(order.getOrderSn());
        delivery.setDeliveryCompany(request.getDeliveryCompany());
        delivery.setDeliverySn(request.getDeliverySn());
        delivery.setReceiverName(request.getReceiverName());
        delivery.setReceiverPhone(request.getReceiverPhone());
        delivery.setReceiverAddress(request.getReceiverAddress());
        delivery.setStatus(DeliveryStatus.SHIPPED.getCode());
        delivery.setDeliveryTime(LocalDateTime.now());
        delivery.setIsDelete(0);
        deliveryRepository.insert(delivery);

        syncOrderStatus(order, DeliveryStatus.SHIPPED.getCode());
        insertTrack(delivery.getId(), "包裹已由" + request.getDeliveryCompany() + "揽收", delivery.getDeliveryTime(), "system");
        notifyMallDelivery(order, delivery);

        log.info("发货成功, orderId: {}, deliveryId: {}", order.getId(), delivery.getId());
        return delivery.getId();
    }

    @Override
    public PageResult<DeliveryListVO> getDeliveryPage(DeliveryQueryRequest request) {
        LambdaQueryWrapper<SysOrderDelivery> wrapper = new LambdaQueryWrapper<SysOrderDelivery>()
                .eq(SysOrderDelivery::getIsDelete, 0)
                .orderByDesc(SysOrderDelivery::getCreateTime);

        if (StringUtils.hasText(request.getOrderSn())) {
            wrapper.like(SysOrderDelivery::getOrderSn, request.getOrderSn());
        }
        if (StringUtils.hasText(request.getDeliverySn())) {
            wrapper.like(SysOrderDelivery::getDeliverySn, request.getDeliverySn());
        }
        if (StringUtils.hasText(request.getDeliveryCompany())) {
            wrapper.like(SysOrderDelivery::getDeliveryCompany, request.getDeliveryCompany());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysOrderDelivery::getStatus, request.getStatus());
        }

        Page<SysOrderDelivery> page = new Page<>(request.getPageNum(), request.getPageSize());
        deliveryRepository.selectPage(page, wrapper);

        List<DeliveryListVO> records = page.getRecords().stream().map(delivery -> {
            DeliveryListVO vo = new DeliveryListVO();
            vo.setId(delivery.getId());
            vo.setOrderId(delivery.getOrderId());
            vo.setOrderSn(delivery.getOrderSn());
            vo.setDeliveryCompany(delivery.getDeliveryCompany());
            vo.setDeliverySn(delivery.getDeliverySn());
            vo.setStatus(delivery.getStatus());
            vo.setDeliveryTime(delivery.getDeliveryTime());
            vo.setReceiveTime(delivery.getReceiveTime());
            vo.setCreateTime(delivery.getCreateTime());

            SysOrder order = orderRepository.selectById(delivery.getOrderId());
            if (order != null) {
                vo.setUserId(order.getUserId());
            }
            return vo;
        }).toList();

        return PageResult.<DeliveryListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public DeliveryDetailVO getAdminDeliveryDetail(Long deliveryId) {
        SysOrderDelivery delivery = getDeliveryOrThrow(deliveryId);
        SysOrder order = orderRepository.selectById(delivery.getOrderId());
        return buildDeliveryDetail(delivery, order);
    }

    @Override
    public DeliveryDetailVO getUserDeliveryByOrderSn(String orderSn, Long userId) {
        SysOrder order = orderRepository.selectOne(new LambdaQueryWrapper<SysOrder>()
                .eq(SysOrder::getOrderSn, orderSn)
                .eq(SysOrder::getUserId, userId)
                .eq(SysOrder::getIsDelete, 0));
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        SysOrderDelivery delivery = deliveryRepository.selectOne(new LambdaQueryWrapper<SysOrderDelivery>()
                .eq(SysOrderDelivery::getOrderId, order.getId())
                .eq(SysOrderDelivery::getIsDelete, 0));
        if (delivery == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "物流信息暂未生成");
        }

        return buildDeliveryDetail(delivery, order);
    }

    @Override
    public void userSignByOrderSn(String orderSn, Long userId) {
        SysOrder order = orderRepository.selectOne(new LambdaQueryWrapper<SysOrder>()
                .eq(SysOrder::getOrderSn, orderSn)
                .eq(SysOrder::getUserId, userId)
                .eq(SysOrder::getIsDelete, 0)
                .last("limit 1"));
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        SysOrderDelivery delivery = deliveryRepository.selectOne(new LambdaQueryWrapper<SysOrderDelivery>()
                .eq(SysOrderDelivery::getOrderId, order.getId())
                .eq(SysOrderDelivery::getIsDelete, 0)
                .last("limit 1"));
        if (delivery == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "物流信息暂未生成");
        }

        Integer status = delivery.getStatus();
        if (Objects.equals(status, DeliveryStatus.SIGNED.getCode())) {
            return;
        }
        if (!Objects.equals(status, DeliveryStatus.SHIPPED.getCode())
                && !Objects.equals(status, DeliveryStatus.IN_TRANSIT.getCode())) {
            throw new BusinessException("当前物流状态不支持确认收货");
        }

        LocalDateTime now = LocalDateTime.now();
        delivery.setStatus(DeliveryStatus.SIGNED.getCode());
        delivery.setReceiveTime(now);
        if (delivery.getDeliveryTime() == null) {
            delivery.setDeliveryTime(now);
        }
        deliveryRepository.updateById(delivery);

        syncOrderStatus(order, DeliveryStatus.SIGNED.getCode());
        insertTrackIfAbsent(delivery.getId(), "用户已确认签收", now, "user");
    }

    @Override
    public void updateDeliveryStatus(DeliveryStatusUpdateRequest request) {
        SysOrderDelivery delivery = getDeliveryOrThrow(request.getDeliveryId());
        DeliveryStatus status = DeliveryStatus.fromCode(request.getStatus());
        if (status == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "物流状态无效");
        }

        delivery.setStatus(status.getCode());
        if (status == DeliveryStatus.SHIPPED && delivery.getDeliveryTime() == null) {
            delivery.setDeliveryTime(LocalDateTime.now());
        }
        if (status == DeliveryStatus.SIGNED) {
            delivery.setReceiveTime(LocalDateTime.now());
        }
        deliveryRepository.updateById(delivery);

        SysOrder order = orderRepository.selectById(delivery.getOrderId());
        if (order != null) {
            syncOrderStatus(order, status.getCode());
        }

        String trackContent = StringUtils.hasText(request.getTrackContent())
                ? request.getTrackContent()
                : buildDefaultTrackContent(status, delivery);
        insertTrack(delivery.getId(), trackContent, LocalDateTime.now(), request.getOperatorInfo());

        log.info("更新物流状态成功, deliveryId: {}, status: {}", delivery.getId(), status.getCode());
    }

    @Override
    public Long addTrack(DeliveryTrackAddRequest request) {
        SysOrderDelivery delivery = getDeliveryOrThrow(request.getDeliveryId());
        Long trackId = insertTrack(
                delivery.getId(),
                request.getTrackContent(),
                request.getTrackTime() != null ? request.getTrackTime() : LocalDateTime.now(),
                request.getOperatorInfo()
        );
        log.info("新增物流轨迹成功, deliveryId: {}, trackId: {}", delivery.getId(), trackId);
        return trackId;
    }

    @Override
    public void simulateTracks(DeliveryTrackSimulateRequest request) {
        SysOrderDelivery delivery = getDeliveryOrThrow(request.getDeliveryId());
        if (delivery.getStatus() != null && delivery.getStatus().equals(DeliveryStatus.SIGNED.getCode())) {
            throw new BusinessException("该物流已签收，无需重复仿真");
        }

        LocalDateTime baseTime = request.getStartTime() != null
                ? request.getStartTime()
                : (delivery.getDeliveryTime() != null ? delivery.getDeliveryTime() : LocalDateTime.now());

        String destination = briefAddress(delivery.getReceiverAddress());
        insertTrack(delivery.getId(), "快件已揽收，离开始发分拨中心", baseTime, "simulator");
        insertTrack(delivery.getId(), "快件运输中，预计发往" + destination, baseTime.plusHours(12), "simulator");
        insertTrack(delivery.getId(), "快件到达目的城市转运中心", baseTime.plusHours(24), "simulator");
        insertTrack(delivery.getId(), "快件正在派送，请保持电话畅通", baseTime.plusHours(36), "simulator");
        insertTrack(delivery.getId(), "快件已签收", baseTime.plusHours(48), "simulator");

        delivery.setStatus(DeliveryStatus.SIGNED.getCode());
        if (delivery.getDeliveryTime() == null) {
            delivery.setDeliveryTime(baseTime);
        }
        delivery.setReceiveTime(baseTime.plusHours(48));
        deliveryRepository.updateById(delivery);

        SysOrder order = orderRepository.selectById(delivery.getOrderId());
        if (order != null) {
            syncOrderStatus(order, DeliveryStatus.SIGNED.getCode());
        }

        log.info("物流轨迹仿真完成, deliveryId: {}", delivery.getId());
    }

    @Override
    public Long autoShipByOrderId(Long orderId, Long printJobId) {
        if (orderId == null) {
            return null;
        }
        SysOrder order = orderRepository.selectOne(new LambdaQueryWrapper<SysOrder>()
                .eq(SysOrder::getId, orderId)
                .eq(SysOrder::getIsDelete, 0)
                .last("limit 1"));
        if (order == null) {
            return null;
        }

        validateOrderShippable(order);

        SysOrderDelivery existed = deliveryRepository.selectOne(new LambdaQueryWrapper<SysOrderDelivery>()
                .eq(SysOrderDelivery::getOrderId, order.getId())
                .eq(SysOrderDelivery::getIsDelete, 0)
                .last("limit 1"));
        if (existed != null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        ReceiverInfo receiverInfo = extractReceiverInfo(order.getCustomParams());
        SysOrderDelivery delivery = new SysOrderDelivery();
        delivery.setOrderId(order.getId());
        delivery.setOrderSn(order.getOrderSn());
        delivery.setDeliveryCompany(randomDomesticCompany());
        delivery.setDeliverySn(generateAutoDeliverySn(order.getId()));
        delivery.setReceiverName(receiverInfo.name());
        delivery.setReceiverPhone(receiverInfo.phone());
        delivery.setReceiverAddress(receiverInfo.address());
        delivery.setStatus(DeliveryStatus.SHIPPED.getCode());
        delivery.setDeliveryTime(now);
        delivery.setIsDelete(0);
        deliveryRepository.insert(delivery);

        syncOrderStatus(order, DeliveryStatus.SHIPPED.getCode());
        insertTrackIfAbsent(delivery.getId(), "包裹已由" + delivery.getDeliveryCompany() + "揽收", now, "auto-ship");
        if (printJobId != null) {
            insertTrackIfAbsent(delivery.getId(), "3D打印任务已完成，系统自动发货", now.plusMinutes(1), "auto-ship");
        }

        log.info("自动发货成功, orderId: {}, jobId: {}, deliveryId: {}", orderId, printJobId, delivery.getId());
        return delivery.getId();
    }

    @Override
    public void autoAdvanceTracks() {
        LocalDateTime now = LocalDateTime.now();
        List<SysOrderDelivery> deliveries = deliveryRepository.selectList(new LambdaQueryWrapper<SysOrderDelivery>()
                .eq(SysOrderDelivery::getIsDelete, 0)
                .in(SysOrderDelivery::getStatus, DeliveryStatus.SHIPPED.getCode(), DeliveryStatus.IN_TRANSIT.getCode())
                .isNotNull(SysOrderDelivery::getDeliveryTime)
                .orderByAsc(SysOrderDelivery::getDeliveryTime));

        for (SysOrderDelivery delivery : deliveries) {
            if (delivery.getDeliveryTime() == null) {
                continue;
            }
            long hours = Duration.between(delivery.getDeliveryTime(), now).toHours();
            boolean changed = false;

            if (hours >= 6) {
                changed |= insertTrackIfAbsent(delivery.getId(), "快件运输中，正在发往目的地", delivery.getDeliveryTime().plusHours(6), "auto-track");
            }
            if (hours >= 18) {
                changed |= insertTrackIfAbsent(delivery.getId(), "快件已到达目的城市转运中心", delivery.getDeliveryTime().plusHours(18), "auto-track");
            }
            if (hours >= 30) {
                changed |= insertTrackIfAbsent(delivery.getId(), "快件正在派送，请保持电话畅通", delivery.getDeliveryTime().plusHours(30), "auto-track");
            }
            if (hours >= 42) {
                changed |= insertTrackIfAbsent(delivery.getId(), "快件已送达，待用户签收", delivery.getDeliveryTime().plusHours(42), "auto-track");
            }

            if (hours >= 6 && !Objects.equals(delivery.getStatus(), DeliveryStatus.IN_TRANSIT.getCode())) {
                delivery.setStatus(DeliveryStatus.IN_TRANSIT.getCode());
                deliveryRepository.updateById(delivery);
                SysOrder order = orderRepository.selectById(delivery.getOrderId());
                if (order != null) {
                    syncOrderStatus(order, DeliveryStatus.IN_TRANSIT.getCode());
                }
                changed = true;
            }

            if (changed) {
                log.info("自动推进物流轨迹成功, deliveryId: {}, status: {}", delivery.getId(), delivery.getStatus());
            }
        }
    }

    private SysOrder getOrderByOrderSn(String orderSn) {
        SysOrder order = orderRepository.selectOne(new LambdaQueryWrapper<SysOrder>()
                .eq(SysOrder::getOrderSn, orderSn)
                .eq(SysOrder::getIsDelete, 0));
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private void notifyMallDelivery(SysOrder order, SysOrderDelivery delivery) {
        if (order == null || delivery == null || order.getUserId() == null) {
            return;
        }
        UserNotificationCreateCommand command = new UserNotificationCreateCommand();
        command.setUserId(order.getUserId());
        command.setCategory(UserNotificationServiceImpl.CATEGORY_LOGISTICS);
        command.setNotificationType(UserNotificationServiceImpl.TYPE_MALL_DELIVERY);
        command.setTitle("商城商品已发货");
        command.setContent("订单" + order.getOrderSn() + "已由" + delivery.getDeliveryCompany() + "发出，可查看物流进度");
        command.setBizId(order.getId());
        command.setBizNo(order.getOrderSn());
        command.setRedirectUrl("/pages/user/logistics-detail?orderSn=" + order.getOrderSn());
        command.setPopupRequired(true);
        userNotificationService.createNotification(command);
    }

    private void validateOrderShippable(SysOrder order) {
        if (order.getOrderStatus() != null
                && (order.getOrderStatus().equals(OrderStatus.CANCELED.getCode())
                || order.getOrderStatus().equals(OrderStatus.COMPLETED.getCode()))) {
            throw new BusinessException("当前订单状态不允许发货");
        }
    }

    private SysOrderDelivery getDeliveryOrThrow(Long deliveryId) {
        SysOrderDelivery delivery = deliveryRepository.selectOne(new LambdaQueryWrapper<SysOrderDelivery>()
                .eq(SysOrderDelivery::getId, deliveryId)
                .eq(SysOrderDelivery::getIsDelete, 0));
        if (delivery == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "物流单不存在");
        }
        return delivery;
    }

    private DeliveryDetailVO buildDeliveryDetail(SysOrderDelivery delivery, SysOrder order) {
        DeliveryDetailVO vo = new DeliveryDetailVO();
        vo.setId(delivery.getId());
        vo.setOrderId(delivery.getOrderId());
        vo.setOrderSn(delivery.getOrderSn());
        vo.setDeliveryCompany(delivery.getDeliveryCompany());
        vo.setDeliverySn(delivery.getDeliverySn());
        vo.setReceiverName(delivery.getReceiverName());
        vo.setReceiverPhone(delivery.getReceiverPhone());
        vo.setReceiverAddress(delivery.getReceiverAddress());
        vo.setStatus(delivery.getStatus());
        vo.setDeliveryTime(delivery.getDeliveryTime());
        vo.setReceiveTime(delivery.getReceiveTime());
        if (order != null) {
            vo.setUserId(order.getUserId());
        }

        List<DeliveryTrackVO> trackVOList = trackRepository.selectList(new LambdaQueryWrapper<SysOrderDeliveryTrack>()
                        .eq(SysOrderDeliveryTrack::getDeliveryId, delivery.getId())
                        .orderByAsc(SysOrderDeliveryTrack::getTrackTime))
                .stream()
                .map(track -> {
                    DeliveryTrackVO trackVO = new DeliveryTrackVO();
                    trackVO.setId(track.getId());
                    trackVO.setTrackContent(track.getTrackContent());
                    trackVO.setTrackTime(track.getTrackTime());
                    trackVO.setOperatorInfo(track.getOperatorInfo());
                    return trackVO;
                })
                .toList();
        vo.setTracks(trackVOList);
        return vo;
    }

    private Long insertTrack(Long deliveryId, String content, LocalDateTime trackTime, String operatorInfo) {
        SysOrderDeliveryTrack track = new SysOrderDeliveryTrack();
        track.setDeliveryId(deliveryId);
        track.setTrackContent(content);
        track.setTrackTime(trackTime);
        track.setOperatorInfo(StringUtils.hasText(operatorInfo) ? operatorInfo : "system");
        trackRepository.insert(track);
        return track.getId();
    }

    private boolean insertTrackIfAbsent(Long deliveryId, String content, LocalDateTime trackTime, String operatorInfo) {
        Long count = trackRepository.selectCount(new LambdaQueryWrapper<SysOrderDeliveryTrack>()
                .eq(SysOrderDeliveryTrack::getDeliveryId, deliveryId)
                .eq(SysOrderDeliveryTrack::getTrackContent, content));
        if (count != null && count > 0) {
            return false;
        }
        insertTrack(deliveryId, content, trackTime, operatorInfo);
        return true;
    }

    private void syncOrderStatus(SysOrder order, Integer deliveryStatus) {
        if (order == null || order.getId() == null) {
            return;
        }
        DeliveryStatus status = DeliveryStatus.fromCode(deliveryStatus);
        if (status == null) {
            return;
        }

        if (status == DeliveryStatus.SIGNED) {
            orderRepository.update(null,
                    new LambdaUpdateWrapper<SysOrder>()
                            .eq(SysOrder::getId, order.getId())
                            .ne(SysOrder::getOrderStatus, OrderStatus.CANCELED.getCode())
                            .set(SysOrder::getOrderStatus, OrderStatus.COMPLETED.getCode()));
            return;
        }

        if (status == DeliveryStatus.SHIPPED || status == DeliveryStatus.IN_TRANSIT || status == DeliveryStatus.PENDING) {
            orderRepository.update(null,
                    new LambdaUpdateWrapper<SysOrder>()
                            .eq(SysOrder::getId, order.getId())
                            .ne(SysOrder::getOrderStatus, OrderStatus.COMPLETED.getCode())
                            .ne(SysOrder::getOrderStatus, OrderStatus.CANCELED.getCode())
                            .set(SysOrder::getOrderStatus, OrderStatus.WAIT_SHIPMENT.getCode()));
            order.setOrderStatus(OrderStatus.WAIT_SHIPMENT.getCode());
        }
    }

    private String generateAutoDeliverySn(Long orderId) {
        String suffix = String.valueOf(orderId == null ? 0L : orderId);
        if (suffix.length() > 6) {
            suffix = suffix.substring(suffix.length() - 6);
        }
        return "SIM" + LocalDateTime.now().format(DELIVERY_SN_TIME_FORMATTER) + suffix;
    }

    private String randomDomesticCompany() {
        if (AUTO_SIM_COMPANY_LIST.isEmpty()) {
            return "模拟快递-3DShop";
        }
        int index = ThreadLocalRandom.current().nextInt(AUTO_SIM_COMPANY_LIST.size());
        return AUTO_SIM_COMPANY_LIST.get(index);
    }

    private ReceiverInfo extractReceiverInfo(String customParams) {
        if (!StringUtils.hasText(customParams)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单缺少收件信息，无法自动发货");
        }

        String receiverName = null;
        String receiverPhone = null;
        String receiverAddress = null;

        try {
            JsonNode root = objectMapper.readTree(customParams);
            String directName = pickText(root, "receiverName", "consigneeName");
            String directPhone = pickText(root, "receiverPhone", "consigneePhone", "receiverMobile", "consigneeMobile");
            String directAddress = pickText(root, "receiverAddress", "consigneeAddress");

            if (StringUtils.hasText(directName)) {
                receiverName = directName;
            }
            if (StringUtils.hasText(directPhone)) {
                receiverPhone = directPhone;
            }
            if (StringUtils.hasText(directAddress)) {
                receiverAddress = directAddress;
            }

            JsonNode addressNode = root.path("shippingAddress");
            if (addressNode == null || addressNode.isMissingNode() || addressNode.isNull()) {
                addressNode = root.path("address");
            }
            if (addressNode != null && !addressNode.isMissingNode() && !addressNode.isNull()) {
                String nestedName = pickText(addressNode, "name", "receiverName", "consigneeName");
                String nestedPhone = pickText(addressNode, "phone", "mobile", "receiverPhone", "consigneePhone");
                if (StringUtils.hasText(nestedName)) {
                    receiverName = nestedName;
                }
                if (StringUtils.hasText(nestedPhone)) {
                    receiverPhone = nestedPhone;
                }

                String nestedFullAddress = pickText(addressNode, "fullAddress", "full", "address", "detailAddress");
                if (StringUtils.hasText(nestedFullAddress)) {
                    receiverAddress = nestedFullAddress;
                } else {
                    List<String> parts = new ArrayList<>();
                    String province = pickText(addressNode, "province", "provinceName");
                    String city = pickText(addressNode, "city", "cityName");
                    String district = pickText(addressNode, "district", "districtName", "area");
                    String detail = pickText(addressNode, "detail", "detailAddress", "street", "addressDetail");
                    if (StringUtils.hasText(province)) {
                        parts.add(province);
                    }
                    if (StringUtils.hasText(city)) {
                        parts.add(city);
                    }
                    if (StringUtils.hasText(district)) {
                        parts.add(district);
                    }
                    if (StringUtils.hasText(detail)) {
                        parts.add(detail);
                    }
                    if (!parts.isEmpty()) {
                        receiverAddress = String.join("", parts);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("解析订单收件信息失败", ex);
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单收件信息格式错误，无法自动发货");
        }

        if (!StringUtils.hasText(receiverName) || !StringUtils.hasText(receiverPhone) || !StringUtils.hasText(receiverAddress)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单收件信息不完整，无法自动发货");
        }

        return new ReceiverInfo(receiverName, receiverPhone, receiverAddress);
    }

    private String pickText(JsonNode node, String... fields) {
        if (node == null || fields == null || fields.length == 0) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value == null || value.isMissingNode() || value.isNull()) {
                continue;
            }
            String text = value.asText();
            if (StringUtils.hasText(text)) {
                return text.trim();
            }
        }
        return null;
    }

    private String buildDefaultTrackContent(DeliveryStatus status, SysOrderDelivery delivery) {
        return switch (status) {
            case PENDING -> "物流单已创建，等待发货";
            case SHIPPED -> "快件已由" + delivery.getDeliveryCompany() + "发出";
            case IN_TRANSIT -> "快件运输中";
            case SIGNED -> "快件已签收";
            case EXCEPTION -> "物流状态异常，请人工处理";
        };
    }

    private String briefAddress(String address) {
        if (!StringUtils.hasText(address)) {
            return "目的地";
        }
        return address.length() <= 12 ? address : address.substring(0, 12) + "...";
    }

    private record ReceiverInfo(String name, String phone, String address) {
    }
}
