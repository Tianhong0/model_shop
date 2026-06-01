package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.annotation.OperationLog;
import org.majun.backend.common.Result;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.AfterSaleAuditRequest;
import org.majun.backend.dto.AfterSaleCreateRequest;
import org.majun.backend.dto.AfterSaleMessageQueryRequest;
import org.majun.backend.dto.AfterSaleMessageSendRequest;
import org.majun.backend.dto.AfterSaleQueryRequest;
import org.majun.backend.dto.AfterSaleRefundRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.OrderAfterSaleService;
import org.majun.backend.util.MinioUtil;
import org.majun.backend.vo.AfterSaleDetailVO;
import org.majun.backend.vo.AfterSaleListVO;
import org.majun.backend.vo.AfterSaleMessageVO;
import org.majun.backend.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Order After-sale", description = "After-sale APIs")
@RestController
@RequestMapping("/api/orders/after-sale")
@RequiredArgsConstructor
/**
 * 订单售后控制器
 */
public class OrderAfterSaleController {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024L;

    private final OrderAfterSaleService afterSaleService;
    private final MinioUtil minioUtil;

    /** 创建售后申请 */
    @Operation(summary = "Create after-sale")
    @PostMapping("/create")
    public Result<Long> createAfterSale(@AuthenticationPrincipal LoginUser loginUser,
                                        @Valid @RequestBody AfterSaleCreateRequest request) {
        return Result.success(afterSaleService.createAfterSale(request, loginUser.getId()));
    }

    /** 我的售后列表 */
    @Operation(summary = "My after-sale list")
    @PostMapping("/my/list")
    public Result<PageResult<AfterSaleListVO>> pageMyAfterSales(@AuthenticationPrincipal LoginUser loginUser,
                                                                 @RequestBody(required = false) AfterSaleQueryRequest request) {
        if (request == null) {
            request = new AfterSaleQueryRequest();
        }
        return Result.success(afterSaleService.pageMyAfterSales(request, loginUser.getId()));
    }

    /** 我的售后详情 */
    @Operation(summary = "My after-sale detail")
    @GetMapping("/my/detail/{afterSaleId}")
    public Result<AfterSaleDetailVO> getMyDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                  @PathVariable Long afterSaleId) {
        return Result.success(afterSaleService.getMyAfterSaleDetail(afterSaleId, loginUser.getId()));
    }

    /** 根据售后编号查询我的售后详情 */
    @Operation(summary = "My after-sale detail by sn")
    @GetMapping("/my/detail/by-sn/{afterSaleSn}")
    public Result<AfterSaleDetailVO> getMyDetailBySn(@AuthenticationPrincipal LoginUser loginUser,
                                                      @PathVariable String afterSaleSn) {
        return Result.success(afterSaleService.getMyAfterSaleDetailBySn(afterSaleSn, loginUser.getId()));
    }

    /** 取消我的售后 */
    @Operation(summary = "Cancel my after-sale")
    @DeleteMapping("/my/cancel/{afterSaleId}")
    public Result<Void> cancelAfterSale(@AuthenticationPrincipal LoginUser loginUser,
                                        @PathVariable Long afterSaleId) {
        afterSaleService.cancelAfterSale(afterSaleId, loginUser.getId());
        return Result.success();
    }

    /** 根据售后编号取消我的售后 */
    @Operation(summary = "Cancel my after-sale by sn")
    @DeleteMapping("/my/cancel/by-sn/{afterSaleSn}")
    public Result<Void> cancelAfterSaleBySn(@AuthenticationPrincipal LoginUser loginUser,
                                            @PathVariable String afterSaleSn) {
        afterSaleService.cancelAfterSaleBySn(afterSaleSn, loginUser.getId());
        return Result.success();
    }

    /** 发送售后消息 */
    @Operation(summary = "Send message in after-sale")
    @PostMapping("/my/message/send")
    public Result<Void> sendMyMessage(@AuthenticationPrincipal LoginUser loginUser,
                                      @Valid @RequestBody AfterSaleMessageSendRequest request) {
        afterSaleService.sendMessage(request, loginUser.getId(), "USER", false);
        return Result.success();
    }

    /** 上传售后凭证 */
    @Operation(summary = "Upload after-sale evidence", description = "type: image/video")
    @PostMapping("/my/evidence/upload")
    public Result<String> uploadMyEvidence(@AuthenticationPrincipal LoginUser loginUser,
                                           @RequestParam("file") MultipartFile file,
                                           @RequestParam("type") String type) {
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        validateEvidenceUpload(file, type);
        try {
            String folder = "video".equalsIgnoreCase(type) ? "after-sale/evidence/videos" : "after-sale/evidence/images";
            return Result.success(minioUtil.uploadFile(file, folder));
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.FAIL, "上传失败: " + ex.getMessage());
        }
    }

    /** 分页查询售后消息 */
    @Operation(summary = "Page after-sale messages")
    @PostMapping("/my/message/page")
    public Result<PageResult<AfterSaleMessageVO>> pageMyMessages(@AuthenticationPrincipal LoginUser loginUser,
                                                                  @Valid @RequestBody AfterSaleMessageQueryRequest request) {
        return Result.success(afterSaleService.pageMessages(request, loginUser.getId(), false));
    }

    /** 管理员售后列表 */
    @Operation(summary = "Admin after-sale list")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/list")
    public Result<PageResult<AfterSaleListVO>> pageAdminAfterSales(@RequestBody(required = false) AfterSaleQueryRequest request) {
        if (request == null) {
            request = new AfterSaleQueryRequest();
        }
        return Result.success(afterSaleService.pageAdminAfterSales(request));
    }

    /** 管理员售后详情 */
    @Operation(summary = "Admin after-sale detail")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/detail/{afterSaleId}")
    public Result<AfterSaleDetailVO> getAdminDetail(@PathVariable Long afterSaleId) {
        return Result.success(afterSaleService.getAdminAfterSaleDetail(afterSaleId));
    }

    /** 审核售后申请 */
    @Operation(summary = "Admin audit after-sale")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "REVIEW", module = "售后管理", description = "审核售后申请", targetType = "AFTER_SALE")
    @PostMapping("/admin/audit")
    public Result<Void> auditAfterSale(@Valid @RequestBody AfterSaleAuditRequest request) {
        afterSaleService.auditAfterSale(request);
        return Result.success();
    }

    /** 执行退款 */
    @Operation(summary = "Admin execute refund")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "REFUND", module = "售后管理", description = "执行退款", targetType = "AFTER_SALE")
    @PostMapping("/admin/refund")
    public Result<Void> executeRefund(@Valid @RequestBody AfterSaleRefundRequest request) {
        afterSaleService.executeRefund(request);
        return Result.success();
    }

    /** 管理员发送售后消息 */
    @Operation(summary = "Admin send message")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/message/send")
    public Result<Void> sendAdminMessage(@AuthenticationPrincipal LoginUser loginUser,
                                         @Valid @RequestBody AfterSaleMessageSendRequest request) {
        afterSaleService.sendMessage(request, loginUser.getId(), "ADMIN", true);
        return Result.success();
    }

    /** 管理员分页查询售后消息 */
    @Operation(summary = "Admin page messages")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/message/page")
    public Result<PageResult<AfterSaleMessageVO>> pageAdminMessages(@Valid @RequestBody AfterSaleMessageQueryRequest request) {
        return Result.success(afterSaleService.pageMessages(request, null, true));
    }

    private void validateEvidenceUpload(MultipartFile file, String type) {
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
