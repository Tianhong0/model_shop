package org.majun.backend.service;

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
import org.majun.backend.vo.OrderCommentDetailVO;
import org.majun.backend.vo.OrderCommentLikeToggleVO;
import org.majun.backend.vo.OrderCommentListVO;
import org.majun.backend.vo.OrderCommentReplyLikeToggleVO;
import org.majun.backend.vo.OrderCommentReplyVO;
import org.majun.backend.vo.OrderCommentStatsVO;
import org.majun.backend.vo.PageResult;

public interface OrderCommentService {

    Long createComment(OrderCommentCreateRequest request, Long userId);

    PageResult<OrderCommentListVO> getMyComments(OrderCommentMyQueryRequest request, Long userId);

    PageResult<OrderCommentListVO> getModelComments(OrderCommentModelQueryRequest request);

    PageResult<OrderCommentListVO> getAdminComments(OrderCommentAdminQueryRequest request);

    OrderCommentDetailVO getCommentDetail(Long commentId, Long currentUserId, boolean adminView);

    void replyComment(OrderCommentReplyRequest request, Long designerId);

    void updateCommentStatus(OrderCommentStatusUpdateRequest request);

    OrderCommentStatsVO getModelCommentStats(Long modelId);

    OrderCommentLikeToggleVO toggleCommentLike(OrderCommentLikeToggleRequest request, Long userId);

    Long createCommentReply(OrderCommentReplyCreateRequest request, Long userId);

    PageResult<OrderCommentReplyVO> getCommentReplies(OrderCommentReplyQueryRequest request, boolean adminView);

    OrderCommentReplyLikeToggleVO toggleCommentReplyLike(OrderCommentReplyLikeToggleRequest request, Long userId);
}
