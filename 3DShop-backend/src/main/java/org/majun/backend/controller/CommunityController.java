package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.PostCreateRequest;
import org.majun.backend.dto.PostInteractionMyQueryRequest;
import org.majun.backend.dto.PostInteractionToggleRequest;
import org.majun.backend.dto.PostMyQueryRequest;
import org.majun.backend.dto.PostQueryRequest;
import org.majun.backend.dto.PostReplyAdoptRequest;
import org.majun.backend.dto.PostReplyCreateRequest;
import org.majun.backend.dto.PostReplyLikeToggleRequest;
import org.majun.backend.dto.PostUpdateRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.CommunityService;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PostCategoryVO;
import org.majun.backend.vo.PostDetailVO;
import org.majun.backend.vo.PostInteractionToggleVO;
import org.majun.backend.vo.PostListVO;
import org.majun.backend.vo.PostReplyLikeToggleVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Community", description = "社区用户端接口")
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
/**
 * 社区前台控制器
 */
public class CommunityController {

    private final CommunityService communityService;

    /** 分类列表 */
    @Operation(summary = "分类列表", description = "查询已启用社区分类")
    @GetMapping("/category/list")
    public Result<List<PostCategoryVO>> getCategoryList() {
        return Result.success(communityService.getCategoryList());
    }

    /** 创建帖子 */
    @Operation(summary = "创建帖子", description = "创建草稿或发布帖子")
    @PostMapping("/post/create")
    public Result<Long> createPost(@AuthenticationPrincipal LoginUser loginUser,
                                   @Valid @RequestBody PostCreateRequest request) {
        return Result.success(communityService.createPost(request, loginUser.getId()));
    }

    /** 更新帖子 */
    @Operation(summary = "更新帖子", description = "更新自己的帖子")
    @PostMapping("/post/update")
    public Result<Void> updatePost(@AuthenticationPrincipal LoginUser loginUser,
                                   @Valid @RequestBody PostUpdateRequest request) {
        communityService.updatePost(request, loginUser.getId());
        return Result.success();
    }

    /** 删除帖子 */
    @Operation(summary = "删除帖子", description = "删除自己的帖子")
    @PostMapping("/post/delete/{postId}")
    public Result<Void> deletePost(@AuthenticationPrincipal LoginUser loginUser,
                                   @PathVariable Long postId) {
        communityService.deletePost(postId, loginUser.getId());
        return Result.success();
    }

    /** 帖子分页 */
    @Operation(summary = "帖子分页", description = "社区帖子列表")
    @PostMapping("/post/page")
    public Result<PageResult<PostListVO>> getPostPage(@AuthenticationPrincipal LoginUser loginUser,
                                                       @Valid @RequestBody PostQueryRequest request) {
        return Result.success(communityService.getPostPage(request, loginUser.getId()));
    }

    /** 帖子详情 */
    @Operation(summary = "帖子详情", description = "查询帖子详情和回复")
    @GetMapping("/post/detail/{postId}")
    public Result<PostDetailVO> getPostDetail(@AuthenticationPrincipal LoginUser loginUser,
                                              @PathVariable Long postId) {
        return Result.success(communityService.getPostDetail(postId, loginUser.getId()));
    }

    /** 创建回复 */
    @Operation(summary = "创建回复", description = "对帖子或回复进行回复")
    @PostMapping("/reply/create")
    public Result<Long> createReply(@AuthenticationPrincipal LoginUser loginUser,
                                    @Valid @RequestBody PostReplyCreateRequest request) {
        return Result.success(communityService.createReply(request, loginUser.getId()));
    }

    /** 删除回复 */
    @Operation(summary = "删除回复", description = "删除自己的回复或自己帖子下的回复")
    @PostMapping("/reply/delete/{replyId}")
    public Result<Void> deleteReply(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long replyId) {
        communityService.deleteReply(replyId, loginUser.getId());
        return Result.success();
    }

    /** 采纳回复 */
    @Operation(summary = "采纳回复", description = "帖子作者采纳一条回复")
    @PostMapping("/reply/adopt")
    public Result<Void> adoptReply(@AuthenticationPrincipal LoginUser loginUser,
                                   @Valid @RequestBody PostReplyAdoptRequest request) {
        communityService.adoptReply(request, loginUser.getId());
        return Result.success();
    }

    /** 回复点赞切换 */
    @Operation(summary = "回复点赞切换", description = "再次点击取消点赞")
    @PostMapping("/reply/like/toggle")
    public Result<PostReplyLikeToggleVO> toggleReplyLike(@AuthenticationPrincipal LoginUser loginUser,
                                                         @Valid @RequestBody PostReplyLikeToggleRequest request) {
        return Result.success(communityService.toggleReplyLike(request, loginUser.getId()));
    }

    /** 点赞/收藏切换 */
    @Operation(summary = "点赞/收藏切换", description = "再次点击取消")
    @PostMapping("/interaction/toggle")
    public Result<PostInteractionToggleVO> toggleInteraction(@AuthenticationPrincipal LoginUser loginUser,
                                                             @Valid @RequestBody PostInteractionToggleRequest request) {
        return Result.success(communityService.toggleInteraction(request, loginUser.getId()));
    }

    /** 我的点赞/收藏帖子 */
    @Operation(summary = "我的点赞/收藏帖子", description = "查询当前用户互动过的帖子")
    @PostMapping("/interaction/my/page")
    public Result<PageResult<PostListVO>> getMyInteractionPosts(@AuthenticationPrincipal LoginUser loginUser,
                                                                @Valid @RequestBody PostInteractionMyQueryRequest request) {
        return Result.success(communityService.getMyInteractionPosts(request, loginUser.getId()));
    }

    /** 我的帖子分页 */
    @Operation(summary = "我的帖子分页", description = "查询当前用户发布的帖子")
    @PostMapping("/post/my/page")
    public Result<PageResult<PostListVO>> getMyPostPage(@AuthenticationPrincipal LoginUser loginUser,
                                                        @Valid @RequestBody PostMyQueryRequest request) {
        return Result.success(communityService.getMyPostPage(request, loginUser.getId()));
    }
}
