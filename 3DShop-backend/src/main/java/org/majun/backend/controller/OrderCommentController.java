package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
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
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.OrderCommentService;
import org.majun.backend.util.MinioUtil;
import org.majun.backend.vo.OrderCommentDetailVO;
import org.majun.backend.vo.OrderCommentLikeToggleVO;
import org.majun.backend.vo.OrderCommentListVO;
import org.majun.backend.vo.OrderCommentReplyLikeToggleVO;
import org.majun.backend.vo.OrderCommentReplyVO;
import org.majun.backend.vo.OrderCommentStatsVO;
import org.majun.backend.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Order Comment", description = "Order comment APIs")
@RestController
@RequestMapping("/api/orders/comment")
@RequiredArgsConstructor
public class OrderCommentController {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024L;

    private final OrderCommentService orderCommentService;
    private final MinioUtil minioUtil;

    @Operation(summary = "Create comment", description = "User creates comment for completed order")
    @PostMapping("/create")
    public Result<Long> createComment(@AuthenticationPrincipal LoginUser loginUser,
                                      @Valid @RequestBody OrderCommentCreateRequest request) {
        return Result.success(orderCommentService.createComment(request, loginUser.getId()));
    }

    @Operation(summary = "Upload order comment media", description = "type: image/video")
    @PostMapping("/my/media/upload")
    public Result<String> uploadCommentMedia(@AuthenticationPrincipal LoginUser loginUser,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam("type") String type) {
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        validateCommentMedia(file, type);
        try {
            String folder = "video".equalsIgnoreCase(type) ? "order-comment/videos" : "order-comment/images";
            return Result.success(minioUtil.uploadFile(file, folder));
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.FAIL, "上传失败: " + ex.getMessage());
        }
    }

    @Operation(summary = "My comment list", description = "Current user comment list")
    @PostMapping("/my/list")
    public Result<PageResult<OrderCommentListVO>> getMyComments(@AuthenticationPrincipal LoginUser loginUser,
                                                                 @Valid @RequestBody OrderCommentMyQueryRequest request) {
        return Result.success(orderCommentService.getMyComments(request, loginUser.getId()));
    }

    @Operation(summary = "Model comment list", description = "Query model comments")
    @PostMapping("/model/list")
    public Result<PageResult<OrderCommentListVO>> getModelComments(@Valid @RequestBody OrderCommentModelQueryRequest request) {
        return Result.success(orderCommentService.getModelComments(request));
    }

    @Operation(summary = "Model comment stats", description = "Get model score statistics")
    @GetMapping("/model/stats/{modelId}")
    public Result<OrderCommentStatsVO> getModelCommentStats(@PathVariable Long modelId) {
        return Result.success(orderCommentService.getModelCommentStats(modelId));
    }

    @Operation(summary = "Toggle comment like", description = "Current user toggles like on comment")
    @PostMapping("/model/like/toggle")
    public Result<OrderCommentLikeToggleVO> toggleCommentLike(@AuthenticationPrincipal LoginUser loginUser,
                                                               @Valid @RequestBody OrderCommentLikeToggleRequest request) {
        return Result.success(orderCommentService.toggleCommentLike(request, loginUser.getId()));
    }

    @Operation(summary = "Create comment reply", description = "Current user creates reply under comment")
    @PostMapping("/reply/create")
    public Result<Long> createCommentReply(@AuthenticationPrincipal LoginUser loginUser,
                                           @Valid @RequestBody OrderCommentReplyCreateRequest request) {
        return Result.success(orderCommentService.createCommentReply(request, loginUser.getId()));
    }

    @Operation(summary = "Comment reply list", description = "Query replies under a comment")
    @PostMapping("/reply/list")
    public Result<PageResult<OrderCommentReplyVO>> getCommentReplies(@Valid @RequestBody OrderCommentReplyQueryRequest request) {
        return Result.success(orderCommentService.getCommentReplies(request, false));
    }

    @Operation(summary = "Toggle comment reply like", description = "Current user toggles like on comment reply")
    @PostMapping("/reply/like/toggle")
    public Result<OrderCommentReplyLikeToggleVO> toggleCommentReplyLike(@AuthenticationPrincipal LoginUser loginUser,
                                                                         @Valid @RequestBody OrderCommentReplyLikeToggleRequest request) {
        return Result.success(orderCommentService.toggleCommentReplyLike(request, loginUser.getId()));
    }

    @Operation(summary = "My comment detail", description = "Current user query own comment detail")
    @GetMapping("/my/detail/{commentId}")
    public Result<OrderCommentDetailVO> getMyCommentDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                            @PathVariable Long commentId) {
        return Result.success(orderCommentService.getCommentDetail(commentId, loginUser.getId(), false));
    }

    @Operation(summary = "Designer reply comment", description = "Designer replies a comment of own model")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    @PostMapping("/designer/reply")
    public Result<Void> replyComment(@AuthenticationPrincipal LoginUser loginUser,
                                     @Valid @RequestBody OrderCommentReplyRequest request) {
        orderCommentService.replyComment(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "Admin comment list", description = "Admin query all comments")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/list")
    public Result<PageResult<OrderCommentListVO>> getAdminComments(@Valid @RequestBody OrderCommentAdminQueryRequest request) {
        return Result.success(orderCommentService.getAdminComments(request));
    }

    @Operation(summary = "Admin comment detail", description = "Admin query comment detail")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/detail/{commentId}")
    public Result<OrderCommentDetailVO> getAdminCommentDetail(@PathVariable Long commentId) {
        return Result.success(orderCommentService.getCommentDetail(commentId, null, true));
    }

    @Operation(summary = "Admin comment reply list", description = "Admin query replies under comment")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/reply/list/{commentId}")
    public Result<PageResult<OrderCommentReplyVO>> getAdminCommentReplies(@PathVariable Long commentId,
                                                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                                                           @RequestParam(defaultValue = "20") Integer pageSize) {
        OrderCommentReplyQueryRequest request = new OrderCommentReplyQueryRequest();
        request.setCommentId(commentId);
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        return Result.success(orderCommentService.getCommentReplies(request, true));
    }

    @Operation(summary = "Admin update comment status", description = "Admin hide or show comment")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/status")
    public Result<Void> updateCommentStatus(@Valid @RequestBody OrderCommentStatusUpdateRequest request) {
        orderCommentService.updateCommentStatus(request);
        return Result.success();
    }

    private void validateCommentMedia(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件不能为空");
        }
        if (!"image".equalsIgnoreCase(type) && !"video".equalsIgnoreCase(type)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "type仅支持image或video");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if ("image".equalsIgnoreCase(type)) {
            if (!contentType.startsWith("image/")) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持图片文件");
            }
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "图片大小不能超过10MB");
            }
            return;
        }

        if (!contentType.startsWith("video/")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持视频文件");
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "视频大小不能超过100MB");
        }
    }
}
