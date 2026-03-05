package org.majun.backend.service;

import org.majun.backend.dto.BountyAcceptRequest;
import org.majun.backend.dto.BountyBidCreateRequest;
import org.majun.backend.dto.BountyBidUpdateRequest;
import org.majun.backend.dto.BountyDeliverySubmitRequest;
import org.majun.backend.dto.BountyMessageSendRequest;
import org.majun.backend.dto.BountyPickBidRequest;
import org.majun.backend.dto.BountyPriceChangeConfirmRequest;
import org.majun.backend.dto.BountyPriceChangeRequest;
import org.majun.backend.dto.BountyTaskCreateRequest;
import org.majun.backend.dto.BountyTaskReviewRequest;
import org.majun.backend.dto.BountyTaskQueryRequest;
import org.majun.backend.vo.BountyMessageVO;
import org.majun.backend.vo.BountyTaskDetailVO;
import org.majun.backend.vo.BountyTaskListVO;
import org.majun.backend.vo.PageResult;

public interface BountyService {

    Long createTask(BountyTaskCreateRequest request, Long userId);

    PageResult<BountyTaskListVO> pageTasks(BountyTaskQueryRequest request, Long userId);

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

    Long sendMessage(BountyMessageSendRequest request, Long userId, String role);

    PageResult<BountyMessageVO> pageMessages(Long taskId, Integer pageNum, Integer pageSize, Long userId);
}
