package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.InviteRelationQueryRequest;
import org.majun.backend.dto.PromotionRewardQueryRequest;
import org.majun.backend.dto.PromotionShareRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.PromotionService;
import org.majun.backend.vo.InviteCodeVO;
import org.majun.backend.vo.InviteeVO;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PosterConfigVO;
import org.majun.backend.vo.PromotionCenterVO;
import org.majun.backend.vo.PromotionRankVO;
import org.majun.backend.vo.PromotionRewardVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Promotion", description = "推广分享接口")
@RestController
@RequestMapping("/api/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/center")
    @Operation(summary = "推广中心首页")
    public Result<PromotionCenterVO> getPromotionCenter(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(promotionService.getPromotionCenter(loginUser.getId()));
    }

    @GetMapping("/invite-code")
    @Operation(summary = "获取我的邀请码")
    public Result<InviteCodeVO> getInviteCode(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(promotionService.getOrCreateInviteCode(loginUser.getId()));
    }

    @PostMapping("/share")
    @Operation(summary = "记录分享行为")
    public Result<String> recordShare(@AuthenticationPrincipal LoginUser loginUser,
                                      @Valid @RequestBody PromotionShareRequest request) {
        Long shareId = promotionService.recordShare(request, loginUser.getId());
        return Result.success(String.valueOf(shareId));
    }

    @GetMapping("/poster")
    @Operation(summary = "生成推广海报")
    public Result<String> generatePoster(@AuthenticationPrincipal LoginUser loginUser) {
        String posterUrl = promotionService.generatePoster(loginUser.getId());
        return Result.success(posterUrl);
    }

    @PostMapping("/invitees/page")
    @Operation(summary = "被邀请人列表分页")
    public Result<PageResult<InviteeVO>> pageInvitees(@AuthenticationPrincipal LoginUser loginUser,
                                                       @RequestBody(required = false) InviteRelationQueryRequest request) {
        InviteRelationQueryRequest req = request == null ? new InviteRelationQueryRequest() : request;
        return Result.success(promotionService.pageInvitees(req, loginUser.getId()));
    }

    @PostMapping("/rewards/page")
    @Operation(summary = "推广奖励记录分页")
    public Result<PageResult<PromotionRewardVO>> pageRewards(@AuthenticationPrincipal LoginUser loginUser,
                                                             @RequestBody(required = false) PromotionRewardQueryRequest request) {
        PromotionRewardQueryRequest req = request == null ? new PromotionRewardQueryRequest() : request;
        return Result.success(promotionService.pageRewards(req, loginUser.getId()));
    }

    @GetMapping("/rank")
    @Operation(summary = "推广排行榜")
    public Result<List<PromotionRankVO>> getRankList(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "week") String period) {
        return Result.success(promotionService.getRankList(limit, period));
    }

    @GetMapping("/poster/config")
    @Operation(summary = "获取海报配置（公开接口）")
    public Result<PosterConfigVO> getPosterConfig() {
        return Result.success(promotionService.getPosterConfig());
    }
}
