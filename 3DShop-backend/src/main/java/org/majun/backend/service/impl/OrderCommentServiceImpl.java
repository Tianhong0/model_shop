package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.UserNotificationCreateCommand;
import org.majun.backend.dto.OrderCommentAdminQueryRequest;
import org.majun.backend.dto.OrderCommentCreateRequest;
import org.majun.backend.dto.OrderCommentLikeToggleRequest;
import org.majun.backend.dto.OrderCommentModelQueryRequest;
import org.majun.backend.dto.OrderCommentMyQueryRequest;
import org.majun.backend.dto.OrderCommentReplyCreateRequest;
import org.majun.backend.dto.OrderCommentReplyLikeToggleRequest;
import org.majun.backend.dto.OrderCommentReplyQueryRequest;
import org.majun.backend.dto.OrderCommentReplyRequest;
import org.majun.backend.dto.OrderCommentStatusUpdateRequest;
import org.majun.backend.entity.SysModel;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.entity.SysOrderComment;
import org.majun.backend.entity.SysOrderCommentLike;
import org.majun.backend.entity.SysOrderCommentReply;
import org.majun.backend.entity.SysOrderCommentReplyLike;
import org.majun.backend.entity.SysUser;
import org.majun.backend.enums.OrderStatus;
import org.majun.backend.repository.SysModelRepository;
import org.majun.backend.repository.SysOrderCommentRepository;
import org.majun.backend.repository.SysOrderCommentLikeRepository;
import org.majun.backend.repository.SysOrderCommentReplyLikeRepository;
import org.majun.backend.repository.SysOrderCommentReplyRepository;
import org.majun.backend.repository.SysOrderRepository;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.service.OrderCommentService;
import org.majun.backend.service.UserNotificationService;
import org.majun.backend.vo.OrderCommentDetailVO;
import org.majun.backend.vo.OrderCommentLikeToggleVO;
import org.majun.backend.vo.OrderCommentListVO;
import org.majun.backend.vo.OrderCommentReplyLikeToggleVO;
import org.majun.backend.vo.OrderCommentReplyVO;
import org.majun.backend.vo.OrderCommentStatsVO;
import org.majun.backend.vo.PageResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommentServiceImpl implements OrderCommentService {

    private final SysOrderCommentRepository orderCommentRepository;
    private final SysOrderCommentLikeRepository orderCommentLikeRepository;
    private final SysOrderCommentReplyRepository orderCommentReplyRepository;
    private final SysOrderCommentReplyLikeRepository orderCommentReplyLikeRepository;
    private final SysOrderRepository orderRepository;
    private final SysModelRepository modelRepository;
    private final SysUserRepository userRepository;
    private final UserNotificationService userNotificationService;

    @Override
    public Long createComment(OrderCommentCreateRequest request, Long userId) {
        SysOrder order = orderRepository.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限评价该订单");
        }
        if (!Integer.valueOf(OrderStatus.COMPLETED.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException("仅已完成订单可评价");
        }

        SysOrderComment existed = orderCommentRepository.selectOne(new LambdaQueryWrapper<SysOrderComment>()
                .eq(SysOrderComment::getOrderId, request.getOrderId()));
        if (existed != null) {
            throw new BusinessException("该订单已评价");
        }

        SysOrderComment comment = new SysOrderComment();
        comment.setOrderId(order.getId());
        comment.setUserId(userId);
        comment.setModelId(order.getModelId());
        comment.setModelScore(request.getModelScore());
        comment.setPrintScore(request.getPrintScore());
        comment.setServiceScore(request.getServiceScore());
        comment.setCommentText(request.getCommentText());
        comment.setCommentImages(request.getCommentImages());
        comment.setIsAnonymous(request.getIsAnonymous() != null && request.getIsAnonymous() == 1 ? 1 : 0);
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(1);
        comment.setCreateTime(LocalDateTime.now());

        orderCommentRepository.insert(comment);
        log.info("创建订单评价成功, commentId: {}, orderId: {}, userId: {}", comment.getId(), order.getId(), userId);
        return comment.getId();
    }

    @Override
    public PageResult<OrderCommentListVO> getMyComments(OrderCommentMyQueryRequest request, Long userId) {
        LambdaQueryWrapper<SysOrderComment> wrapper = new LambdaQueryWrapper<SysOrderComment>()
                .eq(SysOrderComment::getUserId, userId)
                .orderByDesc(SysOrderComment::getCreateTime);

        if (request.getOrderId() != null) {
            wrapper.eq(SysOrderComment::getOrderId, request.getOrderId());
        }
        if (request.getModelId() != null) {
            wrapper.eq(SysOrderComment::getModelId, request.getModelId());
        }

        Page<SysOrderComment> page = new Page<>(request.getPageNum(), request.getPageSize());
        orderCommentRepository.selectPage(page, wrapper);

        List<OrderCommentListVO> records = page.getRecords().stream()
                .map(comment -> buildListVO(comment, true, false))
                .toList();

        return PageResult.<OrderCommentListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public PageResult<OrderCommentListVO> getModelComments(OrderCommentModelQueryRequest request) {
        LambdaQueryWrapper<SysOrderComment> wrapper = new LambdaQueryWrapper<SysOrderComment>()
                .eq(SysOrderComment::getModelId, request.getModelId())
                .eq(SysOrderComment::getStatus, 1);

        if ("hot".equalsIgnoreCase(request.getSortType())) {
            wrapper.orderByDesc(SysOrderComment::getLikeCount)
                    .orderByDesc(SysOrderComment::getCreateTime);
        } else {
            wrapper.orderByDesc(SysOrderComment::getCreateTime);
        }

        Page<SysOrderComment> page = new Page<>(request.getPageNum(), request.getPageSize());
        orderCommentRepository.selectPage(page, wrapper);

        List<OrderCommentListVO> records = page.getRecords().stream()
                .map(comment -> buildListVO(comment, false, false))
                .toList();

        return PageResult.<OrderCommentListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public PageResult<OrderCommentListVO> getAdminComments(OrderCommentAdminQueryRequest request) {
        LambdaQueryWrapper<SysOrderComment> wrapper = new LambdaQueryWrapper<SysOrderComment>()
                .orderByDesc(SysOrderComment::getCreateTime);

        if (request.getOrderId() != null) {
            wrapper.eq(SysOrderComment::getOrderId, request.getOrderId());
        }
        if (request.getModelId() != null) {
            wrapper.eq(SysOrderComment::getModelId, request.getModelId());
        }
        if (request.getUserId() != null) {
            wrapper.eq(SysOrderComment::getUserId, request.getUserId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysOrderComment::getStatus, request.getStatus());
        }

        Page<SysOrderComment> page = new Page<>(request.getPageNum(), request.getPageSize());
        orderCommentRepository.selectPage(page, wrapper);

        List<OrderCommentListVO> records = page.getRecords().stream()
                .map(comment -> buildListVO(comment, false, true))
                .toList();

        return PageResult.<OrderCommentListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public OrderCommentDetailVO getCommentDetail(Long commentId, Long currentUserId, boolean adminView) {
        SysOrderComment comment = orderCommentRepository.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }

        if (!adminView) {
            if (currentUserId == null || !comment.getUserId().equals(currentUserId)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看该评价");
            }
        }

        OrderCommentListVO listVO = buildListVO(comment, !adminView, adminView);
        OrderCommentDetailVO detailVO = new OrderCommentDetailVO();
        copyListToDetail(listVO, detailVO);
        return detailVO;
    }

    @Override
    public void replyComment(OrderCommentReplyRequest request, Long designerId) {
        SysOrderComment comment = orderCommentRepository.selectById(request.getCommentId());
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }

        SysModel model = modelRepository.selectById(comment.getModelId());
        if (model == null || model.getIsDelete() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "模型不存在");
        }
        if (!designerId.equals(model.getDesignerId())) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限回复该评价");
        }

        comment.setReplyContent(request.getReplyContent());
        comment.setReplyTime(LocalDateTime.now());
        orderCommentRepository.updateById(comment);
        log.info("设计者回复评价成功, commentId: {}, designerId: {}", comment.getId(), designerId);
    }

    @Override
    public void updateCommentStatus(OrderCommentStatusUpdateRequest request) {
        if (request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "评价状态仅支持0或1");
        }

        SysOrderComment comment = orderCommentRepository.selectById(request.getCommentId());
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }

        comment.setStatus(request.getStatus());
        orderCommentRepository.updateById(comment);
        log.info("更新评价状态成功, commentId: {}, status: {}", comment.getId(), request.getStatus());
    }

    @Override
    public OrderCommentStatsVO getModelCommentStats(Long modelId) {
        List<SysOrderComment> comments = orderCommentRepository.selectList(new LambdaQueryWrapper<SysOrderComment>()
                .eq(SysOrderComment::getModelId, modelId)
                .eq(SysOrderComment::getStatus, 1));

        OrderCommentStatsVO stats = new OrderCommentStatsVO();
        stats.setModelId(modelId);
        stats.setTotalCount((long) comments.size());

        if (comments.isEmpty()) {
            stats.setAvgModelScore(0.0);
            stats.setAvgPrintScore(0.0);
            stats.setAvgServiceScore(0.0);
            stats.setAvgOverallScore(0.0);
            return stats;
        }

        double avgModel = comments.stream().mapToInt(SysOrderComment::getModelScore).average().orElse(0.0);
        double avgPrint = comments.stream().mapToInt(SysOrderComment::getPrintScore).average().orElse(0.0);
        double avgService = comments.stream().mapToInt(SysOrderComment::getServiceScore).average().orElse(0.0);

        stats.setAvgModelScore(round2(avgModel));
        stats.setAvgPrintScore(round2(avgPrint));
        stats.setAvgServiceScore(round2(avgService));
        stats.setAvgOverallScore(round2((avgModel + avgPrint + avgService) / 3));
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCommentLikeToggleVO toggleCommentLike(OrderCommentLikeToggleRequest request, Long userId) {
        SysOrderComment comment = orderCommentRepository.selectById(request.getCommentId());
        if (comment == null || !Objects.equals(comment.getStatus(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }

        SysOrderCommentLike existed = orderCommentLikeRepository.selectOne(new LambdaQueryWrapper<SysOrderCommentLike>()
                .eq(SysOrderCommentLike::getUserId, userId)
                .eq(SysOrderCommentLike::getCommentId, request.getCommentId()));

        boolean active;
        if (existed != null) {
            orderCommentLikeRepository.deleteById(existed.getId());
            orderCommentRepository.update(null, new UpdateWrapper<SysOrderComment>().eq("id", request.getCommentId())
                    .setSql("like_count = IF(IFNULL(like_count, 0) > 0, IFNULL(like_count, 0) - 1, 0)"));
            active = false;
        } else {
            SysOrderCommentLike like = new SysOrderCommentLike();
            like.setUserId(userId);
            like.setCommentId(request.getCommentId());
            orderCommentLikeRepository.insert(like);
            orderCommentRepository.update(null, new UpdateWrapper<SysOrderComment>().eq("id", request.getCommentId())
                    .setSql("like_count = IFNULL(like_count, 0) + 1"));
            active = true;
            notifyCommentLike(comment, userId);
        }

        return new OrderCommentLikeToggleVO(request.getCommentId(), active);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCommentReply(OrderCommentReplyCreateRequest request, Long userId) {
        SysOrderComment comment = orderCommentRepository.selectById(request.getCommentId());
        if (comment == null || !Objects.equals(comment.getStatus(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }

        SysOrderCommentReply reply = new SysOrderCommentReply();
        reply.setCommentId(request.getCommentId());
        reply.setUserId(userId);
        reply.setContent(request.getContent());
        reply.setStatus(1);
        reply.setLikeCount(0);
        reply.setCreateTime(LocalDateTime.now());
        orderCommentReplyRepository.insert(reply);

        orderCommentRepository.update(null, new UpdateWrapper<SysOrderComment>().eq("id", request.getCommentId())
            .setSql("reply_count = IFNULL(reply_count, 0) + 1"));
        return reply.getId();
    }

    @Override
    public PageResult<OrderCommentReplyVO> getCommentReplies(OrderCommentReplyQueryRequest request, boolean adminView) {
        SysOrderComment comment = orderCommentRepository.selectById(request.getCommentId());
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        if (!adminView && !Objects.equals(comment.getStatus(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }

        LambdaQueryWrapper<SysOrderCommentReply> wrapper = new LambdaQueryWrapper<SysOrderCommentReply>()
                .eq(SysOrderCommentReply::getCommentId, request.getCommentId())
                .eq(!adminView, SysOrderCommentReply::getStatus, 1)
                .orderByDesc(SysOrderCommentReply::getCreateTime);

        Page<SysOrderCommentReply> page = new Page<>(request.getPageNum(), request.getPageSize());
        orderCommentReplyRepository.selectPage(page, wrapper);

        List<OrderCommentReplyVO> records = page.getRecords().stream()
                .map(this::buildReplyVO)
                .toList();

        return PageResult.<OrderCommentReplyVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCommentReplyLikeToggleVO toggleCommentReplyLike(OrderCommentReplyLikeToggleRequest request, Long userId) {
        SysOrderCommentReply reply = orderCommentReplyRepository.selectById(request.getReplyId());
        if (reply == null || !Objects.equals(reply.getStatus(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "追评不存在");
        }

        SysOrderComment comment = orderCommentRepository.selectById(reply.getCommentId());
        if (comment == null || !Objects.equals(comment.getStatus(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }

        SysOrderCommentReplyLike existed = orderCommentReplyLikeRepository.selectOne(new LambdaQueryWrapper<SysOrderCommentReplyLike>()
                .eq(SysOrderCommentReplyLike::getUserId, userId)
                .eq(SysOrderCommentReplyLike::getReplyId, request.getReplyId()));

        boolean active;
        if (existed != null) {
            orderCommentReplyLikeRepository.deleteById(existed.getId());
            orderCommentReplyRepository.update(null, new UpdateWrapper<SysOrderCommentReply>().eq("id", request.getReplyId())
                    .setSql("like_count = IF(IFNULL(like_count, 0) > 0, IFNULL(like_count, 0) - 1, 0)"));
            active = false;
        } else {
            SysOrderCommentReplyLike like = new SysOrderCommentReplyLike();
            like.setUserId(userId);
            like.setReplyId(request.getReplyId());
            orderCommentReplyLikeRepository.insert(like);
            orderCommentReplyRepository.update(null, new UpdateWrapper<SysOrderCommentReply>().eq("id", request.getReplyId())
                    .setSql("like_count = IFNULL(like_count, 0) + 1"));
            active = true;
            notifyCommentReplyLike(comment, reply, userId);
        }

        return new OrderCommentReplyLikeToggleVO(request.getReplyId(), active);
    }

    private OrderCommentListVO buildListVO(SysOrderComment comment, boolean ownerView, boolean adminView) {
        OrderCommentListVO vo = new OrderCommentListVO();
        vo.setId(comment.getId());
        vo.setOrderId(comment.getOrderId());
        vo.setModelId(comment.getModelId());
        vo.setModelScore(comment.getModelScore());
        vo.setPrintScore(comment.getPrintScore());
        vo.setServiceScore(comment.getServiceScore());
        vo.setAvgScore(round2((comment.getModelScore() + comment.getPrintScore() + comment.getServiceScore()) / 3.0));
        vo.setCommentText(comment.getCommentText());
        vo.setCommentImages(comment.getCommentImages());
        vo.setIsAnonymous(comment.getIsAnonymous());
        vo.setReplyContent(comment.getReplyContent());
        vo.setReplyTime(comment.getReplyTime());
        vo.setLikeCount(comment.getLikeCount() == null ? 0 : comment.getLikeCount());
        vo.setReplyCount(comment.getReplyCount() == null ? 0 : comment.getReplyCount());
        vo.setStatus(comment.getStatus());
        vo.setCreateTime(comment.getCreateTime());

        SysModel model = modelRepository.selectById(comment.getModelId());
        if (model != null) {
            vo.setModelName(model.getModelName());
        }

        boolean masked = comment.getIsAnonymous() != null && comment.getIsAnonymous() == 1 && !ownerView && !adminView;
        if (masked) {
            vo.setUserId(null);
            vo.setUserNickname("匿名用户");
            vo.setUserAvatar(null);
            return vo;
        }

        SysUser user = userRepository.selectById(comment.getUserId());
        vo.setUserId(comment.getUserId());
        if (user != null) {
            vo.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUserName());
            vo.setUserAvatar(user.getAvatar());
        } else {
            vo.setUserNickname("用户已不存在");
        }
        return vo;
    }

    private OrderCommentReplyVO buildReplyVO(SysOrderCommentReply reply) {
        OrderCommentReplyVO vo = new OrderCommentReplyVO();
        vo.setId(reply.getId());
        vo.setCommentId(reply.getCommentId());
        vo.setUserId(reply.getUserId());
        vo.setContent(reply.getContent());
        vo.setLikeCount(reply.getLikeCount() == null ? 0 : reply.getLikeCount());
        vo.setStatus(reply.getStatus());
        vo.setCreateTime(reply.getCreateTime());

        SysUser user = userRepository.selectById(reply.getUserId());
        if (user != null) {
            vo.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUserName());
            vo.setUserAvatar(user.getAvatar());
        } else {
            vo.setUserNickname("用户已不存在");
        }
        return vo;
    }

    private void copyListToDetail(OrderCommentListVO source, OrderCommentDetailVO target) {
        target.setId(source.getId());
        target.setOrderId(source.getOrderId());
        target.setModelId(source.getModelId());
        target.setModelName(source.getModelName());
        target.setUserId(source.getUserId());
        target.setUserNickname(source.getUserNickname());
        target.setUserAvatar(source.getUserAvatar());
        target.setModelScore(source.getModelScore());
        target.setPrintScore(source.getPrintScore());
        target.setServiceScore(source.getServiceScore());
        target.setAvgScore(source.getAvgScore());
        target.setCommentText(source.getCommentText());
        target.setCommentImages(source.getCommentImages());
        target.setIsAnonymous(source.getIsAnonymous());
        target.setReplyContent(source.getReplyContent());
        target.setReplyTime(source.getReplyTime());
        target.setLikeCount(source.getLikeCount());
        target.setReplyCount(source.getReplyCount());
        target.setStatus(source.getStatus());
        target.setCreateTime(source.getCreateTime());
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void notifyCommentLike(SysOrderComment comment, Long actorUserId) {
        if (comment == null || Objects.equals(comment.getUserId(), actorUserId)) {
            return;
        }
        SysUser actor = userRepository.selectById(actorUserId);
        UserNotificationCreateCommand command = new UserNotificationCreateCommand();
        command.setUserId(comment.getUserId());
        command.setCategory(UserNotificationServiceImpl.CATEGORY_LIKE);
        command.setNotificationType(UserNotificationServiceImpl.TYPE_ORDER_COMMENT_LIKE);
        command.setTitle("商品评价收到新点赞");
        command.setContent(resolveUserName(actor) + "点赞了你的商品评价");
        command.setSenderId(actorUserId);
        command.setSenderName(resolveUserName(actor));
        command.setBizId(comment.getId());
        command.setRedirectUrl("/pages/custom/comment-list?modelId=" + comment.getModelId());
        userNotificationService.createNotification(command);
    }

    private void notifyCommentReplyLike(SysOrderComment comment, SysOrderCommentReply reply, Long actorUserId) {
        if (reply == null || Objects.equals(reply.getUserId(), actorUserId)) {
            return;
        }
        SysUser actor = userRepository.selectById(actorUserId);
        UserNotificationCreateCommand command = new UserNotificationCreateCommand();
        command.setUserId(reply.getUserId());
        command.setCategory(UserNotificationServiceImpl.CATEGORY_LIKE);
        command.setNotificationType(UserNotificationServiceImpl.TYPE_ORDER_COMMENT_REPLY_LIKE);
        command.setTitle("商品追评收到新点赞");
        command.setContent(resolveUserName(actor) + "点赞了你在商品评价下的追评");
        command.setSenderId(actorUserId);
        command.setSenderName(resolveUserName(actor));
        command.setBizId(reply.getId());
        command.setRedirectUrl("/pages/custom/comment-list?modelId=" + (comment == null ? "" : comment.getModelId()));
        userNotificationService.createNotification(command);
    }

    private String resolveUserName(SysUser user) {
        if (user == null) {
            return "用户";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (StringUtils.hasText(user.getUserName())) {
            return user.getUserName();
        }
        return "用户";
    }
}
