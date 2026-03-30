package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.ModelListAdminQueryRequest;
import org.majun.backend.dto.ModelListBatchRequest;
import org.majun.backend.dto.ModelListCreateRequest;
import org.majun.backend.dto.ModelListInteractionMyQueryRequest;
import org.majun.backend.dto.ModelListInteractionToggleRequest;
import org.majun.backend.dto.ModelListItemAddRequest;
import org.majun.backend.dto.ModelListItemRemoveRequest;
import org.majun.backend.dto.ModelListItemSortRequest;
import org.majun.backend.dto.ModelListMyQueryRequest;
import org.majun.backend.dto.ModelListQueryRequest;
import org.majun.backend.dto.ModelListUpdateRequest;
import org.majun.backend.dto.ModelListStatusUpdateRequest;
import org.majun.backend.entity.SysModel;
import org.majun.backend.entity.SysModelCategory;
import org.majun.backend.entity.SysModelImage;
import org.majun.backend.entity.SysModelList;
import org.majun.backend.entity.SysModelListInteraction;
import org.majun.backend.entity.SysModelListItem;
import org.majun.backend.entity.SysUser;
import org.majun.backend.repository.SysModelCategoryRepository;
import org.majun.backend.repository.SysModelImageRepository;
import org.majun.backend.repository.SysModelListInteractionRepository;
import org.majun.backend.repository.SysModelListItemRepository;
import org.majun.backend.repository.SysModelListRepository;
import org.majun.backend.repository.SysModelRepository;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.service.ModelListService;
import org.majun.backend.vo.ModelListAdminVO;
import org.majun.backend.vo.ModelListInteractionToggleVO;
import org.majun.backend.vo.ModelListShareDetailVO;
import org.majun.backend.vo.ModelListShareItemVO;
import org.majun.backend.vo.ModelListShareListVO;
import org.majun.backend.vo.ModelListStatisticsVO;
import org.majun.backend.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModelListServiceImpl implements ModelListService {

    private final SysModelListRepository listRepository;
    private final SysModelListItemRepository itemRepository;
    private final SysModelListInteractionRepository interactionRepository;
    private final SysModelRepository modelRepository;
    private final SysModelImageRepository modelImageRepository;
    private final SysModelCategoryRepository modelCategoryRepository;
    private final SysUserRepository userRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createList(ModelListCreateRequest request, Long userId) {
        if (request.getStatus() != null && request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态仅支持草稿(0)或发布(1)");
        }

        SysModelList list = new SysModelList();
        list.setUserId(userId);
        list.setTitle(request.getTitle());
        list.setDescription(request.getDescription());
        list.setCoverImage(request.getCoverImage());
        list.setStatus(request.getStatus() == null ? 0 : request.getStatus());
        list.setModelCount(0);
        list.setViewCount(0);
        list.setLikeCount(0);
        list.setCollectCount(0);
        list.setIsDelete(0);

        listRepository.insert(list);

        if (!CollectionUtils.isEmpty(request.getItems())) {
            int count = 0;
            for (ModelListCreateRequest.ModelListItemEntry entry : request.getItems()) {
                if (entry.getModelId() == null) continue;
                SysModelListItem item = new SysModelListItem();
                item.setListId(list.getId());
                item.setModelId(entry.getModelId());
                item.setRemark(entry.getRemark());
                item.setSortNo(entry.getSortNo() == null ? 0 : entry.getSortNo());
                itemRepository.insert(item);
                count++;
            }
            if (count > 0) {
                list.setModelCount(count);
                listRepository.updateById(list);
            }
        }

        return list.getId();
    }

    @Override
    public void updateList(ModelListUpdateRequest request, Long userId) {
        SysModelList list = getListOrThrow(request.getId());
        if (!Objects.equals(list.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只能编辑自己的清单");
        }

        if (StringUtils.hasText(request.getTitle())) {
            list.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            list.setDescription(request.getDescription());
        }
        if (request.getCoverImage() != null) {
            list.setCoverImage(request.getCoverImage());
        }

        listRepository.updateById(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteList(Long listId, Long userId) {
        SysModelList list = getListOrThrow(listId);
        if (!Objects.equals(list.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只能删除自己的清单");
        }

        listRepository.deleteById(listId);
        itemRepository.delete(new LambdaQueryWrapper<SysModelListItem>()
                .eq(SysModelListItem::getListId, listId));
        interactionRepository.delete(new LambdaQueryWrapper<SysModelListInteraction>()
                .eq(SysModelListInteraction::getListId, listId));
    }

    @Override
    public void publishList(Long listId, Long userId) {
        SysModelList list = getListOrThrow(listId);
        if (!Objects.equals(list.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只能发布自己的清单");
        }
        if (Objects.equals(list.getStatus(), 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "清单已经是发布状态");
        }

        list.setStatus(1);
        listRepository.updateById(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItems(ModelListItemAddRequest request, Long userId) {
        SysModelList list = getListOrThrow(request.getListId());
        if (!Objects.equals(list.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只能管理自己的清单");
        }

        Set<Long> existingModelIds = itemRepository.selectList(new LambdaQueryWrapper<SysModelListItem>()
                        .eq(SysModelListItem::getListId, request.getListId())
                        .select(SysModelListItem::getModelId))
                .stream().map(SysModelListItem::getModelId).collect(Collectors.toSet());

        int added = 0;
        for (ModelListCreateRequest.ModelListItemEntry entry : request.getItems()) {
            if (entry.getModelId() == null || existingModelIds.contains(entry.getModelId())) {
                continue;
            }
            SysModelListItem item = new SysModelListItem();
            item.setListId(request.getListId());
            item.setModelId(entry.getModelId());
            item.setRemark(entry.getRemark());
            item.setSortNo(entry.getSortNo() == null ? 0 : entry.getSortNo());
            itemRepository.insert(item);
            existingModelIds.add(entry.getModelId());
            added++;
        }

        if (added > 0) {
            listRepository.update(null, new UpdateWrapper<SysModelList>()
                    .eq("id", request.getListId())
                    .setSql("model_count = model_count + " + added));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(ModelListItemRemoveRequest request, Long userId) {
        SysModelList list = getListOrThrow(request.getListId());
        if (!Objects.equals(list.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只能管理自己的清单");
        }

        int deleted = itemRepository.delete(new LambdaQueryWrapper<SysModelListItem>()
                .eq(SysModelListItem::getListId, request.getListId())
                .eq(SysModelListItem::getModelId, request.getModelId()));

        if (deleted > 0) {
            listRepository.update(null, new UpdateWrapper<SysModelList>()
                    .eq("id", request.getListId())
                    .setSql("model_count = IF(model_count > 0, model_count - 1, 0)"));
        }
    }

    @Override
    public void sortItems(ModelListItemSortRequest request, Long userId) {
        SysModelList list = getListOrThrow(request.getListId());
        if (!Objects.equals(list.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只能管理自己的清单");
        }

        for (ModelListItemSortRequest.SortEntry entry : request.getItems()) {
            if (entry.getId() == null || entry.getSortNo() == null) continue;
            itemRepository.update(null, new UpdateWrapper<SysModelListItem>()
                    .eq("id", entry.getId())
                    .eq("list_id", request.getListId())
                    .set("sort_no", entry.getSortNo()));
        }
    }

    @Override
    public PageResult<ModelListShareListVO> getPublicPage(ModelListQueryRequest request, Long currentUserId) {
        LambdaQueryWrapper<SysModelList> wrapper = new LambdaQueryWrapper<SysModelList>()
                .eq(SysModelList::getStatus, 1);

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(SysModelList::getTitle, request.getKeyword())
                    .or().like(SysModelList::getDescription, request.getKeyword()));
        }

        if ("hot".equalsIgnoreCase(request.getOrderBy())) {
            wrapper.orderByDesc(SysModelList::getLikeCount)
                    .orderByDesc(SysModelList::getViewCount)
                    .orderByDesc(SysModelList::getCreateTime);
        } else {
            wrapper.orderByDesc(SysModelList::getCreateTime);
        }

        Page<SysModelList> page = new Page<>(request.getPageNum(), request.getPageSize());
        listRepository.selectPage(page, wrapper);

        List<ModelListShareListVO> records = buildListVOs(page.getRecords(), currentUserId);
        return PageResult.<ModelListShareListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public ModelListShareDetailVO getDetail(Long listId, Long currentUserId) {
        SysModelList list = getListOrThrow(listId);

        if (!Objects.equals(list.getStatus(), 1) && !Objects.equals(list.getUserId(), currentUserId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该清单");
        }

        if (Objects.equals(list.getStatus(), 1)) {
            listRepository.update(null, new UpdateWrapper<SysModelList>()
                    .eq("id", listId)
                    .setSql("view_count = view_count + 1"));
            list.setViewCount(list.getViewCount() == null ? 1 : list.getViewCount() + 1);
        }

        ModelListShareListVO listVO = buildListVOs(List.of(list), currentUserId).stream().findFirst().orElseThrow();

        List<SysModelListItem> items = itemRepository.selectList(new LambdaQueryWrapper<SysModelListItem>()
                .eq(SysModelListItem::getListId, listId)
                .orderByAsc(SysModelListItem::getSortNo)
                .orderByDesc(SysModelListItem::getCreateTime));

        List<ModelListShareItemVO> itemVOs = buildItemVOs(items);

        ModelListShareDetailVO detailVO = new ModelListShareDetailVO();
        detailVO.setListInfo(listVO);
        detailVO.setItems(itemVOs);
        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelListInteractionToggleVO toggleInteraction(ModelListInteractionToggleRequest request, Long userId) {
        if (request.getInteractType() == null || (request.getInteractType() != 1 && request.getInteractType() != 2)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "交互类型仅支持1(点赞)或2(收藏)");
        }

        SysModelList list = getListOrThrow(request.getListId());
        if (!Objects.equals(list.getStatus(), 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅已发布清单可互动");
        }

        SysModelListInteraction existed = interactionRepository.selectOne(new LambdaQueryWrapper<SysModelListInteraction>()
                .eq(SysModelListInteraction::getUserId, userId)
                .eq(SysModelListInteraction::getListId, request.getListId())
                .eq(SysModelListInteraction::getInteractType, request.getInteractType()));

        boolean active;
        if (existed != null) {
            interactionRepository.deleteById(existed.getId());
            decrementCounter(request.getListId(), request.getInteractType());
            active = false;
        } else {
            SysModelListInteraction interaction = new SysModelListInteraction();
            interaction.setUserId(userId);
            interaction.setListId(request.getListId());
            interaction.setInteractType(request.getInteractType());
            interactionRepository.insert(interaction);
            incrementCounter(request.getListId(), request.getInteractType());
            active = true;
        }

        return new ModelListInteractionToggleVO(request.getListId(), request.getInteractType(), active);
    }

    @Override
    public PageResult<ModelListShareListVO> getMyLists(ModelListMyQueryRequest request, Long userId) {
        LambdaQueryWrapper<SysModelList> wrapper = new LambdaQueryWrapper<SysModelList>()
                .eq(SysModelList::getUserId, userId);

        if (request.getStatus() != null) {
            wrapper.eq(SysModelList::getStatus, request.getStatus());
        }

        wrapper.orderByDesc(SysModelList::getCreateTime);
        Page<SysModelList> page = new Page<>(request.getPageNum(), request.getPageSize());
        listRepository.selectPage(page, wrapper);

        List<ModelListShareListVO> records = buildListVOs(page.getRecords(), userId);
        return PageResult.<ModelListShareListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public PageResult<ModelListShareListVO> getMyInteractionLists(ModelListInteractionMyQueryRequest request, Long userId) {
        if (request.getInteractType() != 1 && request.getInteractType() != 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "交互类型仅支持1(点赞)或2(收藏)");
        }

        Page<SysModelListInteraction> page = new Page<>(request.getPageNum(), request.getPageSize());
        interactionRepository.selectPage(page, new LambdaQueryWrapper<SysModelListInteraction>()
                .eq(SysModelListInteraction::getUserId, userId)
                .eq(SysModelListInteraction::getInteractType, request.getInteractType())
                .orderByDesc(SysModelListInteraction::getCreateTime));

        List<Long> listIds = page.getRecords().stream().map(SysModelListInteraction::getListId).toList();
        if (CollectionUtils.isEmpty(listIds)) {
            return PageResult.<ModelListShareListVO>builder()
                    .records(List.of())
                    .total(page.getTotal())
                    .pageNum((int) page.getCurrent())
                    .pageSize((int) page.getSize())
                    .pages((int) page.getPages())
                    .build();
        }

        Map<Long, SysModelList> listMap = listRepository.selectList(new LambdaQueryWrapper<SysModelList>()
                        .in(SysModelList::getId, listIds)
                        .eq(SysModelList::getStatus, 1))
                .stream()
                .collect(Collectors.toMap(SysModelList::getId, Function.identity()));

        List<SysModelList> orderedLists = new ArrayList<>();
        for (Long listId : listIds) {
            SysModelList ml = listMap.get(listId);
            if (ml != null) {
                orderedLists.add(ml);
            }
        }

        List<ModelListShareListVO> records = buildListVOs(orderedLists, userId);
        return PageResult.<ModelListShareListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    // ==================== private methods ====================

    private SysModelList getListOrThrow(Long listId) {
        SysModelList list = listRepository.selectById(listId);
        if (list == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "清单不存在");
        }
        return list;
    }

    private List<ModelListShareListVO> buildListVOs(List<SysModelList> lists, Long currentUserId) {
        if (CollectionUtils.isEmpty(lists)) {
            return List.of();
        }

        List<Long> listIds = lists.stream().map(SysModelList::getId).toList();
        Set<Long> userIds = lists.stream().map(SysModelList::getUserId).collect(Collectors.toSet());

        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of() :
                userRepository.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        Set<Long> likedIds = currentUserId == null ? Set.of() : interactionRepository.selectList(
                        new LambdaQueryWrapper<SysModelListInteraction>()
                                .eq(SysModelListInteraction::getUserId, currentUserId)
                                .eq(SysModelListInteraction::getInteractType, 1)
                                .in(SysModelListInteraction::getListId, listIds))
                .stream().map(SysModelListInteraction::getListId).collect(Collectors.toSet());

        Set<Long> collectedIds = currentUserId == null ? Set.of() : interactionRepository.selectList(
                        new LambdaQueryWrapper<SysModelListInteraction>()
                                .eq(SysModelListInteraction::getUserId, currentUserId)
                                .eq(SysModelListInteraction::getInteractType, 2)
                                .in(SysModelListInteraction::getListId, listIds))
                .stream().map(SysModelListInteraction::getListId).collect(Collectors.toSet());

        return lists.stream().map(ml -> {
            ModelListShareListVO vo = new ModelListShareListVO();
            vo.setId(ml.getId());
            vo.setUserId(ml.getUserId());
            vo.setTitle(ml.getTitle());
            vo.setDescription(ml.getDescription());
            vo.setCoverImage(ml.getCoverImage());
            vo.setModelCount(defaultZero(ml.getModelCount()));
            vo.setViewCount(defaultZero(ml.getViewCount()));
            vo.setLikeCount(defaultZero(ml.getLikeCount()));
            vo.setCollectCount(defaultZero(ml.getCollectCount()));
            vo.setStatus(ml.getStatus());
            vo.setLiked(likedIds.contains(ml.getId()));
            vo.setCollected(collectedIds.contains(ml.getId()));
            vo.setCreateTime(ml.getCreateTime());

            SysUser user = userMap.get(ml.getUserId());
            vo.setUserNickname(user == null ? "用户" : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUserName()));
            vo.setUserAvatar(user == null ? null : user.getAvatar());
            return vo;
        }).toList();
    }

    private List<ModelListShareItemVO> buildItemVOs(List<SysModelListItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return List.of();
        }

        List<Long> modelIds = items.stream().map(SysModelListItem::getModelId).toList();

        Map<Long, SysModel> modelMap = modelRepository.selectList(new LambdaQueryWrapper<SysModel>()
                        .in(SysModel::getId, modelIds))
                .stream()
                .collect(Collectors.toMap(SysModel::getId, Function.identity()));

        Set<Long> designerIds = modelMap.values().stream()
                .map(SysModel::getDesignerId).collect(Collectors.toSet());
        Map<Long, SysUser> designerMap = designerIds.isEmpty() ? Map.of() :
                userRepository.selectBatchIds(designerIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        Set<Long> categoryIds = modelMap.values().stream()
                .map(SysModel::getCategoryId).collect(Collectors.toSet());
        Map<Long, SysModelCategory> categoryMap = categoryIds.isEmpty() ? Map.of() :
                modelCategoryRepository.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(SysModelCategory::getId, Function.identity()));

        Map<Long, String> mainImageMap = modelImageRepository.selectList(new LambdaQueryWrapper<SysModelImage>()
                        .in(SysModelImage::getModelId, modelIds)
                        .eq(SysModelImage::getIsMain, 1))
                .stream()
                .collect(Collectors.toMap(SysModelImage::getModelId, SysModelImage::getImageUrl, (a, b) -> a));

        return items.stream().map(item -> {
            ModelListShareItemVO vo = new ModelListShareItemVO();
            vo.setId(item.getId());
            vo.setModelId(item.getModelId());
            vo.setRemark(item.getRemark());
            vo.setSortNo(item.getSortNo());

            SysModel model = modelMap.get(item.getModelId());
            if (model != null) {
                vo.setModelName(model.getModelName());
                vo.setBasePrice(model.getBasePrice());
                vo.setMainImageUrl(mainImageMap.get(model.getId()));

                SysUser designer = designerMap.get(model.getDesignerId());
                vo.setDesignerName(designer == null ? null :
                        (StringUtils.hasText(designer.getNickname()) ? designer.getNickname() : designer.getUserName()));

                SysModelCategory category = categoryMap.get(model.getCategoryId());
                vo.setCategoryName(category == null ? null : category.getCategoryName());
            }
            return vo;
        }).toList();
    }

    private void incrementCounter(Long listId, Integer interactType) {
        if (interactType == 1) {
            listRepository.update(null, new UpdateWrapper<SysModelList>()
                    .eq("id", listId).setSql("like_count = like_count + 1"));
        } else {
            listRepository.update(null, new UpdateWrapper<SysModelList>()
                    .eq("id", listId).setSql("collect_count = collect_count + 1"));
        }
    }

    private void decrementCounter(Long listId, Integer interactType) {
        if (interactType == 1) {
            listRepository.update(null, new UpdateWrapper<SysModelList>()
                    .eq("id", listId).setSql("like_count = IF(like_count > 0, like_count - 1, 0)"));
        } else {
            listRepository.update(null, new UpdateWrapper<SysModelList>()
                    .eq("id", listId).setSql("collect_count = IF(collect_count > 0, collect_count - 1, 0)"));
        }
    }

    private Integer defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    // ===== 管理端方法实现 =====

    @Override
    public PageResult<ModelListAdminVO> getAdminPage(ModelListAdminQueryRequest request) {
        int pageNum = request.getPageNum() == null ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        pageSize = Math.min(pageSize, 100);

        LambdaQueryWrapper<SysModelList> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysModelList::getIsDelete, 0);

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(SysModelList::getTitle, request.getKeyword());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysModelList::getStatus, request.getStatus());
        }
        if (request.getUserId() != null) {
            wrapper.eq(SysModelList::getUserId, request.getUserId());
        }
        if (request.getCreateTimeStart() != null) {
            wrapper.ge(SysModelList::getCreateTime, request.getCreateTimeStart());
        }
        if (request.getCreateTimeEnd() != null) {
            wrapper.le(SysModelList::getCreateTime, request.getCreateTimeEnd());
        }

        // 排序
        String orderBy = request.getOrderBy();
        if ("viewCount".equals(orderBy)) {
            wrapper.orderByDesc(SysModelList::getViewCount);
        } else if ("likeCount".equals(orderBy)) {
            wrapper.orderByDesc(SysModelList::getLikeCount);
        } else {
            wrapper.orderByDesc(SysModelList::getCreateTime);
        }

        Page<SysModelList> page = listRepository.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<SysModelList> records = page.getRecords();
        List<ModelListAdminVO> voList = new ArrayList<>();

        if (!records.isEmpty()) {
            Set<Long> userIds = records.stream().map(SysModelList::getUserId).collect(Collectors.toSet());
            Map<Long, SysUser> userMap = userRepository.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, u -> u));

            for (SysModelList list : records) {
                ModelListAdminVO vo = new ModelListAdminVO();
                vo.setId(list.getId());
                vo.setUserId(list.getUserId());
                vo.setTitle(list.getTitle());
                vo.setDescription(list.getDescription());
                vo.setCoverImage(list.getCoverImage());
                vo.setModelCount(list.getModelCount());
                vo.setViewCount(list.getViewCount());
                vo.setLikeCount(list.getLikeCount());
                vo.setCollectCount(list.getCollectCount());
                vo.setStatus(list.getStatus());
                vo.setCreateTime(list.getCreateTime());
                vo.setUpdateTime(list.getUpdateTime());

                SysUser user = userMap.get(list.getUserId());
                if (user != null) {
                    vo.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUserName());
                    vo.setUserAvatar(user.getAvatar());
                }
                voList.add(vo);
            }
        }

        PageResult<ModelListAdminVO> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPages((int) page.getPages());
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setRecords(voList);
        return result;
    }

    @Override
    public ModelListAdminVO getAdminDetail(Long listId) {
        SysModelList list = listRepository.selectById(listId);
        if (list == null || list.getIsDelete() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "清单不存在");
        }

        ModelListAdminVO vo = new ModelListAdminVO();
        vo.setId(list.getId());
        vo.setUserId(list.getUserId());
        vo.setTitle(list.getTitle());
        vo.setDescription(list.getDescription());
        vo.setCoverImage(list.getCoverImage());
        vo.setModelCount(list.getModelCount());
        vo.setViewCount(list.getViewCount());
        vo.setLikeCount(list.getLikeCount());
        vo.setCollectCount(list.getCollectCount());
        vo.setStatus(list.getStatus());
        vo.setCreateTime(list.getCreateTime());
        vo.setUpdateTime(list.getUpdateTime());

        SysUser user = userRepository.selectById(list.getUserId());
        if (user != null) {
            vo.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUserName());
            vo.setUserAvatar(user.getAvatar());
        }

        return vo;
    }

    @Override
    public void updateStatus(ModelListStatusUpdateRequest request) {
        SysModelList list = listRepository.selectById(request.getListId());
        if (list == null || list.getIsDelete() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "清单不存在");
        }

        Integer currentStatus = list.getStatus();
        Integer targetStatus = request.getStatus();

        // 状态流转校验
        if (currentStatus.equals(targetStatus)) {
            return; // 状态相同，无需操作
        }

        if (currentStatus == 0 && targetStatus == 1) {
            // 草稿 -> 发布，允许
        } else if (currentStatus == 1 && targetStatus == 2) {
            // 已发布 -> 下架，允许
        } else if (currentStatus == 2 && targetStatus == 1) {
            // 下架 -> 发布，允许
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的状态转换");
        }

        list.setStatus(targetStatus);
        listRepository.updateById(list);
    }

    @Override
    public void adminDeleteList(Long listId) {
        SysModelList list = listRepository.selectById(listId);
        if (list == null || list.getIsDelete() == 1) {
            return; // 不存在则忽略
        }

        // 逻辑删除清单
        list.setIsDelete(1);
        listRepository.updateById(list);

        // 删除关联的清单项
        itemRepository.delete(new LambdaQueryWrapper<SysModelListItem>().eq(SysModelListItem::getListId, listId));

        // 删除关联的互动记录
        interactionRepository.delete(new LambdaQueryWrapper<SysModelListInteraction>().eq(SysModelListInteraction::getListId, listId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(ModelListBatchRequest request) {
        if (CollectionUtils.isEmpty(request.getListIds()) || request.getStatus() == null) {
            return;
        }

        for (Long listId : request.getListIds()) {
            try {
                ModelListStatusUpdateRequest statusRequest = new ModelListStatusUpdateRequest();
                statusRequest.setListId(listId);
                statusRequest.setStatus(request.getStatus());
                updateStatus(statusRequest);
            } catch (Exception ignored) {
                // 忽略单个失败，继续处理其他
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(ModelListBatchRequest request) {
        if (CollectionUtils.isEmpty(request.getListIds())) {
            return;
        }

        for (Long listId : request.getListIds()) {
            try {
                adminDeleteList(listId);
            } catch (Exception ignored) {
                // 忽略单个失败，继续处理其他
            }
        }
    }

    @Override
    public ModelListStatisticsVO getStatistics() {
        ModelListStatisticsVO vo = new ModelListStatisticsVO();

        Long totalCount = listRepository.selectCount(new LambdaQueryWrapper<SysModelList>()
                .eq(SysModelList::getIsDelete, 0));
        vo.setTotalCount(totalCount);

        Long draftCount = listRepository.selectCount(new LambdaQueryWrapper<SysModelList>()
                .eq(SysModelList::getIsDelete, 0)
                .eq(SysModelList::getStatus, 0));
        vo.setDraftCount(draftCount);

        Long publishedCount = listRepository.selectCount(new LambdaQueryWrapper<SysModelList>()
                .eq(SysModelList::getIsDelete, 0)
                .eq(SysModelList::getStatus, 1));
        vo.setPublishedCount(publishedCount);

        Long offlineCount = listRepository.selectCount(new LambdaQueryWrapper<SysModelList>()
                .eq(SysModelList::getIsDelete, 0)
                .eq(SysModelList::getStatus, 2));
        vo.setOfflineCount(offlineCount);

        Long totalInteractions = interactionRepository.selectCount(null);
        vo.setTotalInteractions(totalInteractions);

        List<SysModelList> allLists = listRepository.selectList(new LambdaQueryWrapper<SysModelList>()
                .eq(SysModelList::getIsDelete, 0));
        long totalViews = allLists.stream().mapToLong(l -> defaultZero(l.getViewCount())).sum();
        vo.setTotalViews(totalViews);

        // 找出创建最多清单的用户
        Map<Long, Long> userCountMap = allLists.stream()
                .collect(Collectors.groupingBy(SysModelList::getUserId, Collectors.counting()));
        if (!userCountMap.isEmpty()) {
            Map.Entry<Long, Long> topEntry = userCountMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
            if (topEntry != null) {
                vo.setTopCreatorId(topEntry.getKey());
                vo.setTopCreatorCount(topEntry.getValue());
                SysUser topUser = userRepository.selectById(topEntry.getKey());
                if (topUser != null) {
                    vo.setTopCreatorNickname(StringUtils.hasText(topUser.getNickname()) ? topUser.getNickname() : topUser.getUserName());
                }
            }
        }

        return vo;
    }
}
