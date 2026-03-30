package org.majun.backend.service;

import org.majun.backend.dto.BountyAcceptRequest;
import org.majun.backend.dto.BountyBidCreateRequest;
import org.majun.backend.dto.BountyBidUpdateRequest;
import org.majun.backend.dto.BountyCancelRequest;
import org.majun.backend.dto.BountyCancelReviewRequest;
import org.majun.backend.dto.BountyDeliverySubmitRequest;
import org.majun.backend.dto.BountyMessageSendRequest;
import org.majun.backend.dto.BountyPickBidRequest;
import org.majun.backend.dto.BountyPriceChangeConfirmRequest;
import org.majun.backend.dto.BountyPriceChangeRequest;
import org.majun.backend.dto.BountyRatingAppealCreateRequest;
import org.majun.backend.dto.BountyRatingAppealReviewRequest;
import org.majun.backend.dto.BountyRatingCreateRequest;
import org.majun.backend.dto.BountyTaskResubmitRequest;
import org.majun.backend.dto.BountyTaskCreateRequest;
import org.majun.backend.dto.BountyTaskReviewRequest;
import org.majun.backend.dto.BountyTaskQueryRequest;
import org.majun.backend.vo.BountyMessageVO;
import org.majun.backend.vo.BountyRatingAppealVO;
import org.majun.backend.vo.BountyRatingVO;
import org.majun.backend.vo.BountyTaskDetailVO;
import org.majun.backend.vo.BountyTaskListVO;
import org.majun.backend.vo.DesignerReputationVO;
import org.majun.backend.vo.PageResult;

public interface BountyService {

    Long createTask(BountyTaskCreateRequest request, Long userId);

    PageResult<BountyTaskListVO> pageTasks(BountyTaskQueryRequest request, Long userId);

    PageResult<BountyTaskListVO> pageTasksForAdmin(BountyTaskQueryRequest request);

    BountyTaskDetailVO getTaskDetail(Long taskId, Long userId);

    BountyTaskDetailVO getTaskDetailForAdmin(Long taskId);

    Long createBid(BountyBidCreateRequest request, Long userId);

    void updateBid(BountyBidUpdateRequest request, Long userId);

    void withdrawBid(Long bidId, Long userId);

    void pickBid(BountyPickBidRequest request, Long userId);

    Long submitDelivery(BountyDeliverySubmitRequest request, Long userId);

    void acceptDelivery(BountyAcceptRequest request, Long userId);

    Long applyPriceChange(BountyPriceChangeRequest request, Long userId);

    void confirmPriceChange(BountyPriceChangeConfirmRequest request, Long userId);

    void reviewTask(BountyTaskReviewRequest request, Long adminId);

    void resubmitTask(BountyTaskResubmitRequest request, Long userId);

    void requestCancelTask(BountyCancelRequest request, Long userId);

    void reviewCancelTask(BountyCancelReviewRequest request, Long adminId);

    Long sendMessage(BountyMessageSendRequest request, Long userId, String role);

    PageResult<BountyMessageVO> pageMessages(Long taskId, Integer pageNum, Integer pageSize, Long userId);

    // ==================== 评价相关 ====================

    Long createRating(BountyRatingCreateRequest request, Long userId);

    BountyRatingVO getRatingByTask(Long taskId, Long userId);

    PageResult<BountyRatingVO> getDesignerRatings(Long designerId, Integer pageNum, Integer pageSize);

    // ==================== 申诉相关 ====================

    Long createRatingAppeal(BountyRatingAppealCreateRequest request, Long userId);

    PageResult<BountyRatingAppealVO> getMyAppeals(Long userId, Integer pageNum, Integer pageSize);

    PageResult<BountyRatingAppealVO> getAllAppeals(Integer pageNum, Integer pageSize, Integer status);

    void reviewRatingAppeal(BountyRatingAppealReviewRequest request, Long adminId);

    // ==================== 信誉相关 ====================

    DesignerReputationVO getDesignerReputation(Long designerId);

    void updateReputationForQualityAnswer(Long designerId);
}
