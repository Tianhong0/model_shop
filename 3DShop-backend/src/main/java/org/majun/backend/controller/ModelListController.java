package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.ModelListCreateRequest;
import org.majun.backend.dto.ModelListInteractionMyQueryRequest;
import org.majun.backend.dto.ModelListInteractionToggleRequest;
import org.majun.backend.dto.ModelListItemAddRequest;
import org.majun.backend.dto.ModelListItemRemoveRequest;
import org.majun.backend.dto.ModelListItemSortRequest;
import org.majun.backend.dto.ModelListMyQueryRequest;
import org.majun.backend.dto.ModelListQueryRequest;
import org.majun.backend.dto.ModelListUpdateRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.ModelListService;
import org.majun.backend.vo.ModelListInteractionToggleVO;
import org.majun.backend.vo.ModelListShareDetailVO;
import org.majun.backend.vo.ModelListShareListVO;
import org.majun.backend.vo.PageResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ModelList", description = "清单分享接口")
@RestController
@RequestMapping("/api/model-list")
@RequiredArgsConstructor
public class ModelListController {

    private final ModelListService modelListService;

    @Operation(summary = "创建清单", description = "创建草稿或发布清单，可带初始模型")
    @PostMapping("/create")
    public Result<Long> createList(@AuthenticationPrincipal LoginUser loginUser,
                                   @Valid @RequestBody ModelListCreateRequest request) {
        return Result.success(modelListService.createList(request, loginUser.getId()));
    }

    @Operation(summary = "更新清单", description = "更新自己的清单信息")
    @PostMapping("/update")
    public Result<Void> updateList(@AuthenticationPrincipal LoginUser loginUser,
                                   @Valid @RequestBody ModelListUpdateRequest request) {
        modelListService.updateList(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "删除清单", description = "删除自己的清单")
    @PostMapping("/delete/{listId}")
    public Result<Void> deleteList(@AuthenticationPrincipal LoginUser loginUser,
                                   @PathVariable Long listId) {
        modelListService.deleteList(listId, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "发布清单", description = "将草稿清单发布")
    @PostMapping("/publish/{listId}")
    public Result<Void> publishList(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long listId) {
        modelListService.publishList(listId, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "添加模型到清单", description = "向清单中添加一个或多个模型")
    @PostMapping("/item/add")
    public Result<Void> addItems(@AuthenticationPrincipal LoginUser loginUser,
                                 @Valid @RequestBody ModelListItemAddRequest request) {
        modelListService.addItems(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "从清单移除模型", description = "从清单中移除一个模型")
    @PostMapping("/item/remove")
    public Result<Void> removeItem(@AuthenticationPrincipal LoginUser loginUser,
                                   @Valid @RequestBody ModelListItemRemoveRequest request) {
        modelListService.removeItem(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "调整清单项排序", description = "批量更新清单内模型的排序")
    @PostMapping("/item/sort")
    public Result<Void> sortItems(@AuthenticationPrincipal LoginUser loginUser,
                                  @Valid @RequestBody ModelListItemSortRequest request) {
        modelListService.sortItems(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "公开清单分页", description = "浏览已发布的模型清单")
    @PostMapping("/page")
    public Result<PageResult<ModelListShareListVO>> getPublicPage(@AuthenticationPrincipal LoginUser loginUser,
                                                                   @Valid @RequestBody ModelListQueryRequest request) {
        return Result.success(modelListService.getPublicPage(request, loginUser.getId()));
    }

    @Operation(summary = "清单详情", description = "查看清单详情及包含的模型")
    @GetMapping("/detail/{listId}")
    public Result<ModelListShareDetailVO> getDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                    @PathVariable Long listId) {
        return Result.success(modelListService.getDetail(listId, loginUser.getId()));
    }

    @Operation(summary = "点赞/收藏切换", description = "再次点击取消")
    @PostMapping("/interaction/toggle")
    public Result<ModelListInteractionToggleVO> toggleInteraction(@AuthenticationPrincipal LoginUser loginUser,
                                                                  @Valid @RequestBody ModelListInteractionToggleRequest request) {
        return Result.success(modelListService.toggleInteraction(request, loginUser.getId()));
    }

    @Operation(summary = "我的清单", description = "查询当前用户创建的清单")
    @PostMapping("/my/page")
    public Result<PageResult<ModelListShareListVO>> getMyLists(@AuthenticationPrincipal LoginUser loginUser,
                                                               @Valid @RequestBody ModelListMyQueryRequest request) {
        return Result.success(modelListService.getMyLists(request, loginUser.getId()));
    }

    @Operation(summary = "我的点赞/收藏清单", description = "查询当前用户互动过的清单")
    @PostMapping("/interaction/my/page")
    public Result<PageResult<ModelListShareListVO>> getMyInteractionLists(@AuthenticationPrincipal LoginUser loginUser,
                                                                          @Valid @RequestBody ModelListInteractionMyQueryRequest request) {
        return Result.success(modelListService.getMyInteractionLists(request, loginUser.getId()));
    }
}
