package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.BannerCreateRequest;
import org.majun.backend.dto.BannerQueryRequest;
import org.majun.backend.dto.BannerStatusUpdateRequest;
import org.majun.backend.dto.BannerUpdateRequest;
import org.majun.backend.dto.AdminOperationStatusUpdateRequest;
import org.majun.backend.dto.NoticeCreateRequest;
import org.majun.backend.dto.NoticeQueryRequest;
import org.majun.backend.dto.NoticeStatusUpdateRequest;
import org.majun.backend.dto.NoticeUpdateRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.OperationService;
import org.majun.backend.vo.AdminOperationStatusVO;
import org.majun.backend.vo.BannerVO;
import org.majun.backend.vo.HomeConfigVO;
import org.majun.backend.vo.NoticeVO;
import org.majun.backend.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营管理控制器
 */
@Tag(name = "运营管理", description = "轮播图与公告管理接口")
@RestController
@RequestMapping("/api/operation")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;

    @Operation(summary = "管理端轮播分页")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/banner/admin/list")
    public Result<PageResult<BannerVO>> bannerAdminList(@Valid @RequestBody BannerQueryRequest request) {
        return Result.success(operationService.getBannerAdminList(request));
    }

    @Operation(summary = "轮播详情")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/banner/admin/detail/{id}")
    public Result<BannerVO> bannerDetail(@PathVariable Long id) {
        return Result.success(operationService.getBannerDetail(id));
    }

    @Operation(summary = "创建轮播")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/banner/admin/create")
    public Result<Long> createBanner(@Valid @RequestBody BannerCreateRequest request) {
        return Result.success(operationService.createBanner(request));
    }

    @Operation(summary = "更新轮播")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/banner/admin/update")
    public Result<Void> updateBanner(@Valid @RequestBody BannerUpdateRequest request) {
        operationService.updateBanner(request);
        return Result.success();
    }

    @Operation(summary = "更新轮播状态")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/banner/admin/status")
    public Result<Void> updateBannerStatus(@Valid @RequestBody BannerStatusUpdateRequest request) {
        operationService.updateBannerStatus(request);
        return Result.success();
    }

    @Operation(summary = "删除轮播")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/banner/admin/delete/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        operationService.deleteBanner(id);
        return Result.success();
    }

    @Operation(summary = "管理端公告分页")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/notice/admin/list")
    public Result<PageResult<NoticeVO>> noticeAdminList(@Valid @RequestBody NoticeQueryRequest request) {
        return Result.success(operationService.getNoticeAdminList(request));
    }

    @Operation(summary = "公告详情")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/notice/admin/detail/{id}")
    public Result<NoticeVO> noticeDetail(@PathVariable Long id) {
        return Result.success(operationService.getNoticeDetail(id));
    }

    @Operation(summary = "创建公告")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/notice/admin/create")
    public Result<Long> createNotice(@AuthenticationPrincipal LoginUser loginUser,
                                     @Valid @RequestBody NoticeCreateRequest request) {
        return Result.success(operationService.createNotice(request, loginUser.getId()));
    }

    @Operation(summary = "更新公告")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/notice/admin/update")
    public Result<Void> updateNotice(@Valid @RequestBody NoticeUpdateRequest request) {
        operationService.updateNotice(request);
        return Result.success();
    }

    @Operation(summary = "更新公告状态")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/notice/admin/status")
    public Result<Void> updateNoticeStatus(@Valid @RequestBody NoticeStatusUpdateRequest request) {
        operationService.updateNoticeStatus(request);
        return Result.success();
    }

    @Operation(summary = "删除公告")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/notice/admin/delete/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        operationService.deleteNotice(id);
        return Result.success();
    }

    @Operation(summary = "首页配置")
    @GetMapping("/home/config")
    public Result<HomeConfigVO> getHomeConfig() {
        return Result.success(operationService.getHomeConfig());
    }

    @Operation(summary = "后台运营状态")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/status")
    public Result<AdminOperationStatusVO> getAdminOperationStatus() {
        return Result.success(operationService.getAdminOperationStatus());
    }

    @Operation(summary = "更新后台运营状态")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/status")
    public Result<Void> updateAdminOperationStatus(@Valid @RequestBody AdminOperationStatusUpdateRequest request) {
        operationService.updateAdminOperationStatus(request.getOperating());
        return Result.success();
    }
}
