package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.CouponTemplateCreateRequest;
import org.majun.backend.dto.CouponTemplateQueryRequest;
import org.majun.backend.dto.UserCouponQueryRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.CouponService;
import org.majun.backend.vo.CouponTemplateVO;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.UserCouponVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券接口
 */
@Tag(name = "Coupon", description = "优惠券接口")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ========== 用户端接口 ==========

    @GetMapping("/templates/available")
    @Operation(summary = "可兑换优惠券列表")
    public Result<PageResult<CouponTemplateVO>> listAvailableTemplates(
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(couponService.listAvailableTemplates(loginUser.getId()));
    }

    @PostMapping("/exchange/{templateId}")
    @Operation(summary = "积分兑换优惠券")
    public Result<Void> exchangeCoupon(
            @PathVariable Long templateId,
            @AuthenticationPrincipal LoginUser loginUser) {
        couponService.exchangeCoupon(templateId, loginUser.getId());
        return Result.success("兑换成功");
    }

    @PostMapping("/my/list")
    @Operation(summary = "我的优惠券列表")
    public Result<PageResult<UserCouponVO>> listMyCoupons(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestBody(required = false) UserCouponQueryRequest request) {
        UserCouponQueryRequest req = request == null ? new UserCouponQueryRequest() : request;
        return Result.success(couponService.listMyCoupons(req, loginUser.getId()));
    }

    @GetMapping("/available-for-order")
    @Operation(summary = "订单可用优惠券列表")
    public Result<List<UserCouponVO>> listAvailableCouponsForOrder(
            @RequestParam(required = false, defaultValue = "0") BigDecimal orderAmount,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(couponService.listAvailableCouponsForOrder(orderAmount, loginUser.getId()));
    }

    @PostMapping("/calculate-discount")
    @Operation(summary = "计算优惠券折扣金额")
    public Result<BigDecimal> calculateCouponDiscount(
            @RequestParam Long couponId,
            @RequestParam BigDecimal orderAmount,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(couponService.calculateCouponDiscount(couponId, orderAmount, loginUser.getId()));
    }

    // ========== 管理端接口 ==========

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/templates")
    @Operation(summary = "创建优惠券模板")
    public Result<Long> createTemplate(@Valid @RequestBody CouponTemplateCreateRequest request) {
        return Result.success(couponService.createTemplate(request));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/templates/{templateId}/status")
    @Operation(summary = "更新优惠券模板状态")
    public Result<Void> updateTemplateStatus(
            @PathVariable Long templateId,
            @RequestParam Integer status) {
        couponService.updateTemplateStatus(templateId, status);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/templates/list")
    @Operation(summary = "管理端优惠券模板列表")
    public Result<PageResult<CouponTemplateVO>> listTemplatesForAdmin(
            @RequestBody(required = false) CouponTemplateQueryRequest request) {
        CouponTemplateQueryRequest req = request == null ? new CouponTemplateQueryRequest() : request;
        return Result.success(couponService.listTemplatesForAdmin(req));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/templates/{templateId}")
    @Operation(summary = "获取优惠券模板详情")
    public Result<CouponTemplateVO> getTemplateDetail(@PathVariable Long templateId) {
        return Result.success(couponService.getTemplateDetail(templateId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/templates/{templateId}")
    @Operation(summary = "更新优惠券模板")
    public Result<Void> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody CouponTemplateCreateRequest request) {
        couponService.updateTemplate(templateId, request);
        return Result.success("更新成功");
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/admin/templates/{templateId}")
    @Operation(summary = "删除优惠券模板")
    public Result<Void> deleteTemplate(@PathVariable Long templateId) {
        couponService.deleteTemplate(templateId);
        return Result.success("删除成功");
    }
}
