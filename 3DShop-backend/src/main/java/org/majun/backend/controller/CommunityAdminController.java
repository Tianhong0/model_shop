package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.PostAdminQueryRequest;
import org.majun.backend.dto.PostCategoryAssignRequest;
import org.majun.backend.dto.PostCategoryAdminQueryRequest;
import org.majun.backend.dto.PostCategoryCreateRequest;
import org.majun.backend.dto.PostCategoryUpdateRequest;
import org.majun.backend.dto.PostReplyAdminQueryRequest;
import org.majun.backend.dto.PostReplyExcellentRequest;
import org.majun.backend.dto.PostReplyStatusUpdateRequest;
import org.majun.backend.dto.PostStatusUpdateRequest;
import org.majun.backend.dto.PostTopUpdateRequest;
import org.majun.backend.service.CommunityAdminService;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PostCategoryVO;
import org.majun.backend.vo.PostDetailVO;
import org.majun.backend.vo.PostListVO;
import org.majun.backend.vo.PostReplyVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Community Admin", description = "社区管理端接口")
@RestController
@RequestMapping("/api/community/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class CommunityAdminController {

    private final CommunityAdminService communityAdminService;

    @Operation(summary = "帖子管理分页")
    @PostMapping("/post/page")
    public Result<PageResult<PostListVO>> getAdminPostPage(@Valid @RequestBody PostAdminQueryRequest request) {
        return Result.success(communityAdminService.getAdminPostPage(request));
    }

    @Operation(summary = "帖子管理详情")
    @GetMapping("/post/detail/{postId}")
    public Result<PostDetailVO> getAdminPostDetail(@PathVariable Long postId) {
        return Result.success(communityAdminService.getAdminPostDetail(postId));
    }

    @Operation(summary = "更新帖子状态")
    @PostMapping("/post/status")
    public Result<Void> updatePostStatus(@Valid @RequestBody PostStatusUpdateRequest request) {
        communityAdminService.updatePostStatus(request);
        return Result.success();
    }

    @Operation(summary = "更新帖子置顶")
    @PostMapping("/post/top")
    public Result<Void> updatePostTop(@Valid @RequestBody PostTopUpdateRequest request) {
        communityAdminService.updatePostTop(request);
        return Result.success();
    }

    @Operation(summary = "更新帖子分类")
    @PostMapping("/post/category")
    public Result<Void> updatePostCategory(@Valid @RequestBody PostCategoryAssignRequest request) {
        communityAdminService.updatePostCategory(request);
        return Result.success();
    }

    @Operation(summary = "删除帖子")
    @PostMapping("/post/delete/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        communityAdminService.deletePost(postId);
        return Result.success();
    }

    @Operation(summary = "回复管理分页")
    @PostMapping("/reply/page")
    public Result<PageResult<PostReplyVO>> getAdminReplyPage(@Valid @RequestBody PostReplyAdminQueryRequest request) {
        return Result.success(communityAdminService.getAdminReplyPage(request));
    }

    @Operation(summary = "更新回复状态")
    @PostMapping("/reply/status")
    public Result<Void> updateReplyStatus(@Valid @RequestBody PostReplyStatusUpdateRequest request) {
        communityAdminService.updateReplyStatus(request);
        return Result.success();
    }

    @Operation(summary = "更新优质回复")
    @PostMapping("/reply/excellent")
    public Result<Void> updateReplyExcellent(@Valid @RequestBody PostReplyExcellentRequest request) {
        communityAdminService.updateReplyExcellent(request);
        return Result.success();
    }

    @Operation(summary = "删除回复")
    @PostMapping("/reply/delete/{replyId}")
    public Result<Void> deleteReply(@PathVariable Long replyId) {
        communityAdminService.deleteReply(replyId);
        return Result.success();
    }

    @Operation(summary = "分类管理列表")
    @GetMapping("/category/list")
    public Result<List<PostCategoryVO>> getCategoryAdminList() {
        return Result.success(communityAdminService.getCategoryAdminList());
    }

    @Operation(summary = "分类管理分页")
    @GetMapping("/category/page")
    public Result<PageResult<PostCategoryVO>> getCategoryAdminPage(@Valid PostCategoryAdminQueryRequest request) {
        return Result.success(communityAdminService.getCategoryAdminPage(request));
    }

    @Operation(summary = "创建分类")
    @PostMapping("/category/create")
    public Result<Long> createCategory(@Valid @RequestBody PostCategoryCreateRequest request) {
        return Result.success(communityAdminService.createCategory(request));
    }

    @Operation(summary = "更新分类")
    @PostMapping("/category/update")
    public Result<Void> updateCategory(@Valid @RequestBody PostCategoryUpdateRequest request) {
        communityAdminService.updateCategory(request);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @PostMapping("/category/delete/{categoryId}")
    public Result<Void> deleteCategory(@PathVariable Long categoryId) {
        communityAdminService.deleteCategory(categoryId);
        return Result.success();
    }
}
