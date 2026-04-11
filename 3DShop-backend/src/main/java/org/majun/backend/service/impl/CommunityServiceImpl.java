package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.majun.backend.ai.service.ContentModerationService;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.PostCreateRequest;
import org.majun.backend.dto.PostInteractionMyQueryRequest;
import org.majun.backend.dto.PostInteractionToggleRequest;
import org.majun.backend.dto.PostMediaItemRequest;
import org.majun.backend.dto.PostMyQueryRequest;
import org.majun.backend.dto.PostQueryRequest;
import org.majun.backend.dto.PostReplyAdoptRequest;
import org.majun.backend.dto.PostReplyCreateRequest;
import org.majun.backend.dto.PostReplyLikeToggleRequest;
import org.majun.backend.dto.PostUpdateRequest;
import org.majun.backend.entity.SysPost;
import org.majun.backend.entity.SysPostCategory;
import org.majun.backend.entity.SysPostInteraction;
import org.majun.backend.entity.SysPostMedia;
import org.majun.backend.entity.SysPostReply;
import org.majun.backend.entity.SysPostReplyInteraction;
import org.majun.backend.entity.SysUser;
import org.majun.backend.dto.UserNotificationCreateCommand;
import org.majun.backend.repository.SysPostCategoryRepository;
import org.majun.backend.repository.SysPostInteractionRepository;
import org.majun.backend.repository.SysPostMediaRepository;
import org.majun.backend.repository.SysPostReplyRepository;
import org.majun.backend.repository.SysPostReplyInteractionRepository;
import org.majun.backend.repository.SysPostRepository;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.service.CommunityService;
import org.majun.backend.service.PointService;
import org.majun.backend.service.UserNotificationService;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PostCategoryVO;
import org.majun.backend.vo.PostDetailVO;
import org.majun.backend.vo.PostInteractionToggleVO;
import org.majun.backend.vo.PostListVO;
import org.majun.backend.vo.PostMediaVO;
import org.majun.backend.vo.PostReplyLikeToggleVO;
import org.majun.backend.vo.PostReplyVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 社区服务实现
 */
public class CommunityServiceImpl implements CommunityService {

    private final SysPostCategoryRepository postCategoryRepository;
    private final SysPostRepository postRepository;
    private final SysPostMediaRepository postMediaRepository;
    private final SysPostReplyRepository postReplyRepository;
    private final SysPostReplyInteractionRepository postReplyInteractionRepository;
    private final SysPostInteractionRepository postInteractionRepository;
    private final SysUserRepository userRepository;
    private final UserNotificationService userNotificationService;
    private final PointService pointService;
    private final ContentModerationService contentModerationService;

    @Override
    public List<PostCategoryVO> getCategoryList() {
        return postCategoryRepository.selectList(new LambdaQueryWrapper<SysPostCategory>()
                        .eq(SysPostCategory::getStatus, 1)
                        .orderByAsc(SysPostCategory::getSortNo)
                        .orderByDesc(SysPostCategory::getCreateTime))
                .stream()
                .map(this::toCategoryVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(PostCreateRequest request, Long userId) {
        validateCategory(request.getCategoryId());
        validatePostEditableStatus(request.getStatus());

        // AI内容审核
        String title = request.getTitle();
        String content = request.getContent();
        String[] moderated = contentModerationService.moderateTexts(title, content);
        title = moderated[0];
        content = moderated[1];

        SysPost post = new SysPost();
        post.setUserId(userId);
        post.setCategoryId(request.getCategoryId());
        post.setTitle(title);
        post.setContent(content);
        post.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        post.setIsTop(0);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCollectCount(0);
        post.setIsDelete(0);

        postRepository.insert(post);

        if (!CollectionUtils.isEmpty(request.getMediaList())) {
            replacePostMedia(post.getId(), request.getMediaList());
        }
        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(PostUpdateRequest request, Long userId) {
        SysPost post = getPostOrThrow(request.getId());
        if (!Objects.equals(post.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只能编辑自己的帖子");
        }

        if (request.getCategoryId() != null) {
            validateCategory(request.getCategoryId());
            post.setCategoryId(request.getCategoryId());
        }
        if (StringUtils.hasText(request.getTitle())) {
            // AI内容审核标题
            post.setTitle(contentModerationService.moderateText(request.getTitle()));
        }
        if (StringUtils.hasText(request.getContent())) {
            // AI内容审核内容
            post.setContent(contentModerationService.moderateText(request.getContent()));
        }
        if (request.getStatus() != null) {
            validatePostEditableStatus(request.getStatus());
            post.setStatus(request.getStatus());
        }

        postRepository.updateById(post);

        if (request.getMediaList() != null) {
            replacePostMedia(post.getId(), request.getMediaList());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId, Long userId) {
        SysPost post = getPostOrThrow(postId);
        if (!Objects.equals(post.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只能删除自己的帖子");
        }
        List<Long> replyIds = postReplyRepository.selectList(new LambdaQueryWrapper<SysPostReply>()
                        .select(SysPostReply::getId)
                        .eq(SysPostReply::getPostId, postId))
                .stream().map(SysPostReply::getId).toList();
        postRepository.deleteById(postId);
        postMediaRepository.delete(new LambdaQueryWrapper<SysPostMedia>().eq(SysPostMedia::getPostId, postId));
        postReplyRepository.delete(new LambdaQueryWrapper<SysPostReply>().eq(SysPostReply::getPostId, postId));
        if (!CollectionUtils.isEmpty(replyIds)) {
            postReplyInteractionRepository.delete(new LambdaQueryWrapper<SysPostReplyInteraction>()
                    .in(SysPostReplyInteraction::getReplyId, replyIds));
        }
        postInteractionRepository.delete(new LambdaQueryWrapper<SysPostInteraction>().eq(SysPostInteraction::getPostId, postId));
    }

    @Override
    public PageResult<PostListVO> getPostPage(PostQueryRequest request, Long currentUserId) {
        LambdaQueryWrapper<SysPost> wrapper = new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getStatus, 1);

        if (request.getCategoryId() != null) {
            wrapper.eq(SysPost::getCategoryId, request.getCategoryId());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(SysPost::getTitle, request.getKeyword())
                    .or().like(SysPost::getContent, request.getKeyword()));
        }

        wrapper.orderByDesc(SysPost::getIsTop);
        if ("hot".equalsIgnoreCase(request.getOrderBy())) {
            wrapper.orderByDesc(SysPost::getLikeCount)
                    .orderByDesc(SysPost::getViewCount)
                    .orderByDesc(SysPost::getCreateTime);
        } else {
            wrapper.orderByDesc(SysPost::getCreateTime);
        }

        Page<SysPost> page = new Page<>(request.getPageNum(), request.getPageSize());
        postRepository.selectPage(page, wrapper);

        List<PostListVO> records = buildPostList(page.getRecords(), currentUserId);
        return PageResult.<PostListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public PostDetailVO getPostDetail(Long postId, Long currentUserId) {
        SysPost post = getPostOrThrow(postId);

        if (!Objects.equals(post.getStatus(), 1) && !Objects.equals(post.getUserId(), currentUserId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该帖子");
        }

        if (Objects.equals(post.getStatus(), 1)) {
            postRepository.update(null, new UpdateWrapper<SysPost>()
                    .eq("id", postId)
                    .setSql("view_count = view_count + 1"));
            post.setViewCount(post.getViewCount() == null ? 1 : post.getViewCount() + 1);
        }

        PostListVO postVO = buildPostList(List.of(post), currentUserId).stream().findFirst().orElseThrow();

        List<SysPostReply> replies = postReplyRepository.selectList(new LambdaQueryWrapper<SysPostReply>()
                .eq(SysPostReply::getPostId, postId)
                .eq(SysPostReply::getStatus, 1)
                .orderByDesc(SysPostReply::getIsAdopted)
                .orderByDesc(SysPostReply::getIsExcellent)
                .orderByAsc(SysPostReply::getCreateTime));

        PostDetailVO detailVO = new PostDetailVO();
        detailVO.setPost(postVO);
        detailVO.setContent(post.getContent());
        detailVO.setReplies(buildReplyVOList(replies, currentUserId));
        return detailVO;
    }

    @Override
    public Long createReply(PostReplyCreateRequest request, Long userId) {
        SysPost post = getPostOrThrow(request.getPostId());
        if (!Objects.equals(post.getStatus(), 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅已发布帖子支持回复");
        }

        Long parentId = request.getParentId() == null ? 0L : request.getParentId();
        if (parentId > 0) {
            SysPostReply parentReply = postReplyRepository.selectById(parentId);
            if (parentReply == null || !Objects.equals(parentReply.getPostId(), request.getPostId())) {
                throw new BusinessException(ResultCode.NOT_FOUND, "父回复不存在");
            }
        }

        // AI内容审核回复内容
        String moderatedContent = contentModerationService.moderateText(request.getContent());

        SysPostReply reply = new SysPostReply();
        reply.setPostId(request.getPostId());
        reply.setUserId(userId);
        reply.setParentId(parentId);
        reply.setContent(moderatedContent);
        reply.setIsAdopted(0);
        reply.setIsExcellent(0);
        reply.setLikeCount(0);
        reply.setStatus(1);

        postReplyRepository.insert(reply);
        return reply.getId();
    }

    @Override
    public void deleteReply(Long replyId, Long userId) {
        SysPostReply reply = postReplyRepository.selectById(replyId);
        if (reply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "回复不存在");
        }

        SysPost post = getPostOrThrow(reply.getPostId());
        boolean isReplyOwner = Objects.equals(reply.getUserId(), userId);
        boolean isPostOwner = Objects.equals(post.getUserId(), userId);
        if (!isReplyOwner && !isPostOwner) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限删除该回复");
        }

        postReplyRepository.deleteById(replyId);
    postReplyInteractionRepository.delete(new LambdaQueryWrapper<SysPostReplyInteraction>()
        .eq(SysPostReplyInteraction::getReplyId, replyId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adoptReply(PostReplyAdoptRequest request, Long userId) {
        SysPostReply reply = postReplyRepository.selectById(request.getReplyId());
        if (reply == null || !Objects.equals(reply.getStatus(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "回复不存在");
        }

        SysPost post = getPostOrThrow(reply.getPostId());
        if (!Objects.equals(post.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只有帖子作者可采纳回复");
        }

        SysPostReply adopted = postReplyRepository.selectOne(new LambdaQueryWrapper<SysPostReply>()
                .eq(SysPostReply::getPostId, reply.getPostId())
                .eq(SysPostReply::getIsAdopted, 1));
        if (adopted != null && !Objects.equals(adopted.getId(), reply.getId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "每个帖子只能采纳一条回复");
        }

        reply.setIsAdopted(1);
        postReplyRepository.updateById(reply);

        // 奖励积分给回答者
        if (!Objects.equals(reply.getUserId(), userId)) {
            pointService.rewardReplyAdopted(reply.getUserId(), reply.getId(), reply.getPostId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostReplyLikeToggleVO toggleReplyLike(PostReplyLikeToggleRequest request, Long userId) {
        SysPostReply reply = postReplyRepository.selectById(request.getReplyId());
        if (reply == null || !Objects.equals(reply.getStatus(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "回复不存在");
        }

        SysPost post = getPostOrThrow(reply.getPostId());
        if (!Objects.equals(post.getStatus(), 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅已发布帖子下的回复支持点赞");
        }

        SysPostReplyInteraction existed = postReplyInteractionRepository.selectOne(new LambdaQueryWrapper<SysPostReplyInteraction>()
                .eq(SysPostReplyInteraction::getUserId, userId)
                .eq(SysPostReplyInteraction::getReplyId, request.getReplyId()));

        boolean active;
        if (existed != null) {
            postReplyInteractionRepository.deleteById(existed.getId());
            postReplyRepository.update(null, new UpdateWrapper<SysPostReply>().eq("id", request.getReplyId())
                    .setSql("like_count = IF(like_count > 0, like_count - 1, 0)"));
            active = false;
        } else {
            SysPostReplyInteraction interaction = new SysPostReplyInteraction();
            interaction.setUserId(userId);
            interaction.setReplyId(request.getReplyId());
            postReplyInteractionRepository.insert(interaction);
            postReplyRepository.update(null, new UpdateWrapper<SysPostReply>().eq("id", request.getReplyId())
                    .setSql("like_count = like_count + 1"));
            active = true;
            notifyReplyLike(reply, post, userId);
        }

        return new PostReplyLikeToggleVO(request.getReplyId(), active);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostInteractionToggleVO toggleInteraction(PostInteractionToggleRequest request, Long userId) {
        if (request.getInteractType() == null || (request.getInteractType() != 1 && request.getInteractType() != 2)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "交互类型仅支持1(点赞)或2(收藏)");
        }

        SysPost post = getPostOrThrow(request.getPostId());
        if (!Objects.equals(post.getStatus(), 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅已发布帖子可互动");
        }

        SysPostInteraction existed = postInteractionRepository.selectOne(new LambdaQueryWrapper<SysPostInteraction>()
                .eq(SysPostInteraction::getUserId, userId)
                .eq(SysPostInteraction::getPostId, request.getPostId())
                .eq(SysPostInteraction::getInteractType, request.getInteractType()));

        boolean active;
        if (existed != null) {
            postInteractionRepository.deleteById(existed.getId());
            decrementCounter(request.getPostId(), request.getInteractType());
            active = false;
        } else {
            SysPostInteraction interaction = new SysPostInteraction();
            interaction.setUserId(userId);
            interaction.setPostId(request.getPostId());
            interaction.setInteractType(request.getInteractType());
            postInteractionRepository.insert(interaction);
            incrementCounter(request.getPostId(), request.getInteractType());
            active = true;
            notifyPostLike(post, request.getInteractType(), userId);
        }

        return new PostInteractionToggleVO(request.getPostId(), request.getInteractType(), active);
    }

    @Override
    public PageResult<PostListVO> getMyInteractionPosts(PostInteractionMyQueryRequest request, Long userId) {
        if (request.getInteractType() != 1 && request.getInteractType() != 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "交互类型仅支持1(点赞)或2(收藏)");
        }

        Page<SysPostInteraction> page = new Page<>(request.getPageNum(), request.getPageSize());
        postInteractionRepository.selectPage(page, new LambdaQueryWrapper<SysPostInteraction>()
                .eq(SysPostInteraction::getUserId, userId)
                .eq(SysPostInteraction::getInteractType, request.getInteractType())
                .orderByDesc(SysPostInteraction::getCreateTime));

        List<Long> postIds = page.getRecords().stream().map(SysPostInteraction::getPostId).toList();
        if (CollectionUtils.isEmpty(postIds)) {
            return PageResult.<PostListVO>builder()
                    .records(List.of())
                    .total(page.getTotal())
                    .pageNum((int) page.getCurrent())
                    .pageSize((int) page.getSize())
                    .pages((int) page.getPages())
                    .build();
        }

        Map<Long, SysPost> postMap = postRepository.selectList(new LambdaQueryWrapper<SysPost>()
                        .in(SysPost::getId, postIds)
                        .eq(SysPost::getStatus, 1))
                .stream()
                .collect(Collectors.toMap(SysPost::getId, Function.identity()));

        List<SysPost> orderedPosts = new ArrayList<>();
        for (Long postId : postIds) {
            SysPost post = postMap.get(postId);
            if (post != null) {
                orderedPosts.add(post);
            }
        }

        List<PostListVO> records = buildPostList(orderedPosts, userId);
        return PageResult.<PostListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public PageResult<PostListVO> getMyPostPage(PostMyQueryRequest request, Long userId) {
        LambdaQueryWrapper<SysPost> wrapper = new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getUserId, userId);

        if (request.getStatus() != null) {
            wrapper.eq(SysPost::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(SysPost::getTitle, request.getKeyword())
                    .or().like(SysPost::getContent, request.getKeyword()));
        }

        wrapper.orderByDesc(SysPost::getCreateTime);
        Page<SysPost> page = new Page<>(request.getPageNum(), request.getPageSize());
        postRepository.selectPage(page, wrapper);

        List<PostListVO> records = buildPostList(page.getRecords(), userId);
        return PageResult.<PostListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    private SysPost getPostOrThrow(Long postId) {
        SysPost post = postRepository.selectById(postId);
        if (post == null || Objects.equals(post.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        return post;
    }

    private void validateCategory(Long categoryId) {
        SysPostCategory category = postCategoryRepository.selectById(categoryId);
        if (category == null || !Objects.equals(category.getStatus(), 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类不存在或已禁用");
        }
    }

    private void validatePostEditableStatus(Integer status) {
        if (status == null) {
            return;
        }
        if (status != 0 && status != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "帖子仅支持草稿(0)或发布(1)");
        }
    }

    private void replacePostMedia(Long postId, List<PostMediaItemRequest> mediaList) {
        postMediaRepository.delete(new LambdaQueryWrapper<SysPostMedia>().eq(SysPostMedia::getPostId, postId));
        if (CollectionUtils.isEmpty(mediaList)) {
            return;
        }

        for (PostMediaItemRequest item : mediaList) {
            if (item.getMediaType() == null || (item.getMediaType() != 1 && item.getMediaType() != 2)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "媒体类型仅支持1(图片)或2(视频)");
            }
            SysPostMedia media = new SysPostMedia();
            media.setPostId(postId);
            media.setMediaUrl(item.getMediaUrl());
            media.setMediaType(item.getMediaType());
            media.setSortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder());
            postMediaRepository.insert(media);
        }
    }

    private List<PostListVO> buildPostList(List<SysPost> posts, Long currentUserId) {
        if (CollectionUtils.isEmpty(posts)) {
            return List.of();
        }

        List<Long> postIds = posts.stream().map(SysPost::getId).toList();
        Set<Long> userIds = posts.stream().map(SysPost::getUserId).collect(Collectors.toSet());
        Set<Long> categoryIds = posts.stream().map(SysPost::getCategoryId).collect(Collectors.toSet());

        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of() :
                userRepository.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, SysPostCategory> categoryMap = categoryIds.isEmpty() ? Map.of() :
                postCategoryRepository.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(SysPostCategory::getId, Function.identity()));

        Map<Long, List<SysPostMedia>> mediaMap = postMediaRepository.selectList(new LambdaQueryWrapper<SysPostMedia>()
                        .in(SysPostMedia::getPostId, postIds)
                        .orderByAsc(SysPostMedia::getSortOrder))
                .stream()
                .collect(Collectors.groupingBy(SysPostMedia::getPostId));

        Map<Long, Long> replyCountMap = postReplyRepository.selectList(new LambdaQueryWrapper<SysPostReply>()
                        .in(SysPostReply::getPostId, postIds)
                        .eq(SysPostReply::getStatus, 1))
                .stream()
                .collect(Collectors.groupingBy(SysPostReply::getPostId, Collectors.counting()));

        Set<Long> likedPostIds = currentUserId == null ? Set.of() : postInteractionRepository.selectList(new LambdaQueryWrapper<SysPostInteraction>()
                        .eq(SysPostInteraction::getUserId, currentUserId)
                        .eq(SysPostInteraction::getInteractType, 1)
                        .in(SysPostInteraction::getPostId, postIds))
                .stream().map(SysPostInteraction::getPostId).collect(Collectors.toSet());

        Set<Long> collectedPostIds = currentUserId == null ? Set.of() : postInteractionRepository.selectList(new LambdaQueryWrapper<SysPostInteraction>()
                        .eq(SysPostInteraction::getUserId, currentUserId)
                        .eq(SysPostInteraction::getInteractType, 2)
                        .in(SysPostInteraction::getPostId, postIds))
                .stream().map(SysPostInteraction::getPostId).collect(Collectors.toSet());

        return posts.stream().map(post -> {
            PostListVO vo = new PostListVO();
            vo.setId(post.getId());
            vo.setUserId(post.getUserId());
            vo.setCategoryId(post.getCategoryId());
            vo.setTitle(post.getTitle());
            vo.setSummary(buildSummary(post.getContent()));
            vo.setViewCount(defaultZero(post.getViewCount()));
            vo.setLikeCount(defaultZero(post.getLikeCount()));
            vo.setCollectCount(defaultZero(post.getCollectCount()));
            vo.setReplyCount(replyCountMap.getOrDefault(post.getId(), 0L).intValue());
            vo.setIsTop(post.getIsTop());
            vo.setStatus(post.getStatus());
            vo.setLiked(likedPostIds.contains(post.getId()));
            vo.setCollected(collectedPostIds.contains(post.getId()));
            vo.setCreateTime(post.getCreateTime());

            SysUser user = userMap.get(post.getUserId());
            vo.setUserNickname(user == null ? "用户" : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUserName()));
            vo.setUserAvatar(user == null ? null : user.getAvatar());

            SysPostCategory category = categoryMap.get(post.getCategoryId());
            vo.setCategoryName(category == null ? null : category.getName());

            List<PostMediaVO> mediaVOS = mediaMap.getOrDefault(post.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(m -> m.getSortOrder() == null ? 0 : m.getSortOrder()))
                    .map(this::toMediaVO)
                    .toList();
            vo.setMediaList(mediaVOS);
            return vo;
        }).toList();
    }

    private List<PostReplyVO> buildReplyVOList(List<SysPostReply> replies, Long currentUserId) {
        if (CollectionUtils.isEmpty(replies)) {
            return List.of();
        }
        List<Long> replyIds = replies.stream().map(SysPostReply::getId).toList();
        Set<Long> userIds = replies.stream().map(SysPostReply::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of() :
                userRepository.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Set<Long> likedReplyIds = currentUserId == null ? Set.of() : postReplyInteractionRepository.selectList(new LambdaQueryWrapper<SysPostReplyInteraction>()
                        .eq(SysPostReplyInteraction::getUserId, currentUserId)
                        .in(SysPostReplyInteraction::getReplyId, replyIds))
                .stream().map(SysPostReplyInteraction::getReplyId).collect(Collectors.toSet());

        Map<Long, SysPostReply> replyMap = replies.stream()
                .collect(Collectors.toMap(SysPostReply::getId, Function.identity()));

        return replies.stream().map(reply -> {
            PostReplyVO vo = new PostReplyVO();
            vo.setId(reply.getId());
            vo.setPostId(reply.getPostId());
            vo.setUserId(reply.getUserId());
            vo.setParentId(reply.getParentId());
            vo.setContent(reply.getContent());
            vo.setIsAdopted(reply.getIsAdopted());
            vo.setIsExcellent(reply.getIsExcellent());
            vo.setLikeCount(defaultZero(reply.getLikeCount()));
            vo.setLiked(likedReplyIds.contains(reply.getId()));
            vo.setStatus(reply.getStatus());
            vo.setCreateTime(reply.getCreateTime());

            SysUser user = userMap.get(reply.getUserId());
            vo.setUserNickname(user == null ? "用户" : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUserName()));
            vo.setUserAvatar(user == null ? null : user.getAvatar());

            if (reply.getParentId() != null && reply.getParentId() > 0) {
                SysPostReply parentReply = replyMap.get(reply.getParentId());
                if (parentReply != null) {
                    SysUser parentUser = userMap.get(parentReply.getUserId());
                    vo.setParentUserNickname(parentUser == null ? "用户" : (StringUtils.hasText(parentUser.getNickname()) ? parentUser.getNickname() : parentUser.getUserName()));
                }
            }
            return vo;
        }).toList();
    }

    private void incrementCounter(Long postId, Integer interactType) {
        if (interactType == 1) {
            postRepository.update(null, new UpdateWrapper<SysPost>().eq("id", postId).setSql("like_count = like_count + 1"));
            return;
        }
        postRepository.update(null, new UpdateWrapper<SysPost>().eq("id", postId).setSql("collect_count = collect_count + 1"));
    }

    private void decrementCounter(Long postId, Integer interactType) {
        if (interactType == 1) {
            postRepository.update(null, new UpdateWrapper<SysPost>().eq("id", postId)
                    .setSql("like_count = IF(like_count > 0, like_count - 1, 0)"));
            return;
        }
        postRepository.update(null, new UpdateWrapper<SysPost>().eq("id", postId)
                .setSql("collect_count = IF(collect_count > 0, collect_count - 1, 0)"));
    }

    private Integer defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String buildSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String trimmed = content.trim();
        return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 80) + "...";
    }

    private PostCategoryVO toCategoryVO(SysPostCategory category) {
        PostCategoryVO vo = new PostCategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setSortNo(category.getSortNo());
        vo.setStatus(category.getStatus());
        vo.setCreateTime(category.getCreateTime());
        return vo;
    }

    private PostMediaVO toMediaVO(SysPostMedia media) {
        PostMediaVO vo = new PostMediaVO();
        vo.setId(media.getId());
        vo.setMediaUrl(media.getMediaUrl());
        vo.setMediaType(media.getMediaType());
        vo.setSortOrder(media.getSortOrder());
        return vo;
    }

    private void notifyPostLike(SysPost post, Integer interactType, Long actorUserId) {
        if (!Objects.equals(interactType, 1) || post == null || Objects.equals(post.getUserId(), actorUserId)) {
            return;
        }
        SysUser actor = userRepository.selectById(actorUserId);
        UserNotificationCreateCommand command = new UserNotificationCreateCommand();
        command.setUserId(post.getUserId());
        command.setCategory(UserNotificationServiceImpl.CATEGORY_LIKE);
        command.setNotificationType(UserNotificationServiceImpl.TYPE_COMMUNITY_POST_LIKE);
        command.setTitle("社区帖子收到新点赞");
        command.setContent(resolveUserName(actor) + "点赞了你的帖子《" + post.getTitle() + "》");
        command.setSenderId(actorUserId);
        command.setSenderName(resolveUserName(actor));
        command.setBizId(post.getId());
        command.setRedirectUrl("/pages/community/post-detail?id=" + post.getId());
        userNotificationService.createNotification(command);
    }

    private void notifyReplyLike(SysPostReply reply, SysPost post, Long actorUserId) {
        if (reply == null || Objects.equals(reply.getUserId(), actorUserId)) {
            return;
        }
        SysUser actor = userRepository.selectById(actorUserId);
        UserNotificationCreateCommand command = new UserNotificationCreateCommand();
        command.setUserId(reply.getUserId());
        command.setCategory(UserNotificationServiceImpl.CATEGORY_LIKE);
        command.setNotificationType(UserNotificationServiceImpl.TYPE_COMMUNITY_REPLY_LIKE);
        command.setTitle("社区回复收到新点赞");
        command.setContent(resolveUserName(actor) + "点赞了你在《" + (post == null ? "社区帖子" : post.getTitle()) + "》下的回复");
        command.setSenderId(actorUserId);
        command.setSenderName(resolveUserName(actor));
        command.setBizId(reply.getId());
        command.setRedirectUrl("/pages/community/post-detail?id=" + reply.getPostId());
        userNotificationService.createNotification(command);
    }

    private String resolveUserName(SysUser user) {
        if (user == null) {
            return "用户";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : (StringUtils.hasText(user.getUserName()) ? user.getUserName() : "用户");
    }
}
