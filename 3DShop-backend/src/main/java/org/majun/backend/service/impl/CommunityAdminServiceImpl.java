package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
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
import org.majun.backend.entity.SysPost;
import org.majun.backend.entity.SysPostCategory;
import org.majun.backend.entity.SysPostInteraction;
import org.majun.backend.entity.SysPostMedia;
import org.majun.backend.entity.SysPostReply;
import org.majun.backend.entity.SysPostReplyInteraction;
import org.majun.backend.entity.SysUser;
import org.majun.backend.repository.SysPostCategoryRepository;
import org.majun.backend.repository.SysPostInteractionRepository;
import org.majun.backend.repository.SysPostMediaRepository;
import org.majun.backend.repository.SysPostReplyRepository;
import org.majun.backend.repository.SysPostReplyInteractionRepository;
import org.majun.backend.repository.SysPostRepository;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.service.CommunityAdminService;
import org.majun.backend.service.PointService;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PostCategoryVO;
import org.majun.backend.vo.PostDetailVO;
import org.majun.backend.vo.PostListVO;
import org.majun.backend.vo.PostMediaVO;
import org.majun.backend.vo.PostReplyVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityAdminServiceImpl implements CommunityAdminService {

    private final SysPostRepository postRepository;
    private final SysPostReplyRepository postReplyRepository;
    private final SysPostMediaRepository postMediaRepository;
    private final SysPostInteractionRepository postInteractionRepository;
    private final SysPostCategoryRepository postCategoryRepository;
    private final SysPostReplyInteractionRepository postReplyInteractionRepository;
    private final SysUserRepository userRepository;
    private final PointService pointService;

    @Override
    public PageResult<PostListVO> getAdminPostPage(PostAdminQueryRequest request) {
        LambdaQueryWrapper<SysPost> wrapper = new LambdaQueryWrapper<SysPost>()
                .orderByDesc(SysPost::getIsTop)
                .orderByDesc(SysPost::getCreateTime);

        if (request.getCategoryId() != null) {
            wrapper.eq(SysPost::getCategoryId, request.getCategoryId());
        }
        if (request.getUserId() != null) {
            wrapper.eq(SysPost::getUserId, request.getUserId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysPost::getStatus, request.getStatus());
        }
        if (request.getIsTop() != null) {
            wrapper.eq(SysPost::getIsTop, request.getIsTop());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(SysPost::getTitle, request.getKeyword())
                    .or().like(SysPost::getContent, request.getKeyword()));
        }

        Page<SysPost> page = new Page<>(request.getPageNum(), request.getPageSize());
        postRepository.selectPage(page, wrapper);

        List<PostListVO> records = toPostListVO(page.getRecords());
        return PageResult.<PostListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public PostDetailVO getAdminPostDetail(Long postId) {
        SysPost post = postRepository.selectById(postId);
        if (post == null || Objects.equals(post.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        PostListVO postVO = toPostListVO(List.of(post)).stream().findFirst().orElseThrow();
        List<SysPostReply> replies = postReplyRepository.selectList(new LambdaQueryWrapper<SysPostReply>()
                .eq(SysPostReply::getPostId, postId)
                .orderByDesc(SysPostReply::getCreateTime));

        PostDetailVO detailVO = new PostDetailVO();
        detailVO.setPost(postVO);
        detailVO.setContent(post.getContent());
        detailVO.setReplies(toReplyVO(replies));
        return detailVO;
    }

    @Override
    public void updatePostStatus(PostStatusUpdateRequest request) {
        if (request.getStatus() < 0 || request.getStatus() > 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "帖子状态仅支持0/1/2");
        }

        SysPost post = postRepository.selectById(request.getPostId());
        if (post == null || Objects.equals(post.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        post.setStatus(request.getStatus());
        postRepository.updateById(post);
    }

    @Override
    public void updatePostTop(PostTopUpdateRequest request) {
        if (request.getIsTop() != 0 && request.getIsTop() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "置顶状态仅支持0/1");
        }

        SysPost post = postRepository.selectById(request.getPostId());
        if (post == null || Objects.equals(post.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        post.setIsTop(request.getIsTop());
        postRepository.updateById(post);
    }

    @Override
    public void updatePostCategory(PostCategoryAssignRequest request) {
        SysPost post = postRepository.selectById(request.getPostId());
        if (post == null || Objects.equals(post.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        SysPostCategory category = postCategoryRepository.selectById(request.getCategoryId());
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        if (category.getStatus() != null && category.getStatus() == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类已禁用");
        }

        post.setCategoryId(request.getCategoryId());
        postRepository.updateById(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId) {
        SysPost post = postRepository.selectById(postId);
        if (post == null || Objects.equals(post.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        List<Long> replyIds = postReplyRepository.selectList(new LambdaQueryWrapper<SysPostReply>()
                        .select(SysPostReply::getId)
                        .eq(SysPostReply::getPostId, postId))
                .stream().map(SysPostReply::getId).toList();
        postRepository.deleteById(postId);
        postMediaRepository.delete(new LambdaQueryWrapper<SysPostMedia>().eq(SysPostMedia::getPostId, postId));
        postReplyRepository.delete(new LambdaQueryWrapper<SysPostReply>().eq(SysPostReply::getPostId, postId));
        if (!replyIds.isEmpty()) {
            postReplyInteractionRepository.delete(new LambdaQueryWrapper<SysPostReplyInteraction>()
                    .in(SysPostReplyInteraction::getReplyId, replyIds));
        }
        postInteractionRepository.delete(new LambdaQueryWrapper<SysPostInteraction>().eq(SysPostInteraction::getPostId, postId));
    }

    @Override
    public PageResult<PostReplyVO> getAdminReplyPage(PostReplyAdminQueryRequest request) {
        LambdaQueryWrapper<SysPostReply> wrapper = new LambdaQueryWrapper<SysPostReply>()
                .orderByDesc(SysPostReply::getCreateTime);
        if (request.getPostId() != null) {
            wrapper.eq(SysPostReply::getPostId, request.getPostId());
        }
        if (request.getUserId() != null) {
            wrapper.eq(SysPostReply::getUserId, request.getUserId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysPostReply::getStatus, request.getStatus());
        }
        if (request.getIsAdopted() != null) {
            wrapper.eq(SysPostReply::getIsAdopted, request.getIsAdopted());
        }

        Page<SysPostReply> page = new Page<>(request.getPageNum(), request.getPageSize());
        postReplyRepository.selectPage(page, wrapper);

        List<PostReplyVO> records = toReplyVO(page.getRecords());
        return PageResult.<PostReplyVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public void deleteReply(Long replyId) {
        SysPostReply reply = postReplyRepository.selectById(replyId);
        if (reply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "回复不存在");
        }
        postReplyRepository.deleteById(replyId);
        postReplyInteractionRepository.delete(new LambdaQueryWrapper<SysPostReplyInteraction>()
                .eq(SysPostReplyInteraction::getReplyId, replyId));
    }

    @Override
    public void updateReplyStatus(PostReplyStatusUpdateRequest request) {
        if (request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "回复状态仅支持0/1");
        }
        SysPostReply reply = postReplyRepository.selectById(request.getReplyId());
        if (reply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "回复不存在");
        }
        reply.setStatus(request.getStatus());
        postReplyRepository.updateById(reply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReplyExcellent(PostReplyExcellentRequest request) {
        if (request.getIsExcellent() != 0 && request.getIsExcellent() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "优质状态仅支持0/1");
        }
        SysPostReply reply = postReplyRepository.selectById(request.getReplyId());
        if (reply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "回复不存在");
        }

        // 只有从未标记为优质变为优质时才奖励积分
        boolean wasExcellent = Objects.equals(reply.getIsExcellent(), 1);
        boolean willBeExcellent = Objects.equals(request.getIsExcellent(), 1);

        reply.setIsExcellent(request.getIsExcellent());
        postReplyRepository.updateById(reply);

        // 设置为优质回答时奖励积分
        if (!wasExcellent && willBeExcellent) {
            pointService.rewardReplyExcellent(reply.getUserId(), reply.getId(), reply.getPostId());
        }
    }

    @Override
    public List<PostCategoryVO> getCategoryAdminList() {
        return postCategoryRepository.selectList(new LambdaQueryWrapper<SysPostCategory>()
                        .orderByAsc(SysPostCategory::getSortNo)
                        .orderByDesc(SysPostCategory::getCreateTime))
                .stream()
                .map(this::toCategoryVO)
                .toList();
    }

    @Override
    public PageResult<PostCategoryVO> getCategoryAdminPage(PostCategoryAdminQueryRequest request) {
        LambdaQueryWrapper<SysPostCategory> wrapper = new LambdaQueryWrapper<SysPostCategory>()
                .orderByAsc(SysPostCategory::getSortNo)
                .orderByDesc(SysPostCategory::getCreateTime);

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(SysPostCategory::getName, request.getKeyword().trim());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysPostCategory::getStatus, request.getStatus());
        }

        Page<SysPostCategory> page = new Page<>(request.getPageNum(), request.getPageSize());
        postCategoryRepository.selectPage(page, wrapper);

        List<PostCategoryVO> records = page.getRecords().stream().map(this::toCategoryVO).toList();
        return PageResult.<PostCategoryVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public Long createCategory(PostCategoryCreateRequest request) {
        SysPostCategory existed = postCategoryRepository.selectOne(new LambdaQueryWrapper<SysPostCategory>()
                .eq(SysPostCategory::getName, request.getName()));
        if (existed != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类名称已存在");
        }

        SysPostCategory category = new SysPostCategory();
        category.setName(request.getName());
        category.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        category.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        postCategoryRepository.insert(category);
        return category.getId();
    }

    @Override
    public void updateCategory(PostCategoryUpdateRequest request) {
        SysPostCategory category = postCategoryRepository.selectById(request.getId());
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }

        if (StringUtils.hasText(request.getName())) {
            SysPostCategory existed = postCategoryRepository.selectOne(new LambdaQueryWrapper<SysPostCategory>()
                    .eq(SysPostCategory::getName, request.getName())
                    .ne(SysPostCategory::getId, request.getId()));
            if (existed != null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "分类名称已存在");
            }
            category.setName(request.getName());
        }
        if (request.getSortNo() != null) {
            category.setSortNo(request.getSortNo());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() != 0 && request.getStatus() != 1) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "分类状态仅支持0/1");
            }
            category.setStatus(request.getStatus());
        }

        postCategoryRepository.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long categoryId) {
        SysPostCategory category = postCategoryRepository.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        Long postCount = postRepository.selectCount(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getCategoryId, categoryId)
                .eq(SysPost::getIsDelete, 0));
        if (postCount > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类下仍有帖子，不能删除");
        }
        postCategoryRepository.deleteById(categoryId);
    }

    private List<PostListVO> toPostListVO(List<SysPost> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }
        Set<Long> postIds = posts.stream().map(SysPost::getId).collect(Collectors.toSet());
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
                .orderByAsc(SysPostMedia::getSortOrder)
                .orderByAsc(SysPostMedia::getId))
            .stream()
            .collect(Collectors.groupingBy(SysPostMedia::getPostId));

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
            vo.setIsTop(post.getIsTop());
            vo.setStatus(post.getStatus());
            vo.setCreateTime(post.getCreateTime());

            SysUser user = userMap.get(post.getUserId());
            vo.setUserNickname(user == null ? "用户" : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUserName()));
            vo.setUserAvatar(user == null ? null : user.getAvatar());

            SysPostCategory category = categoryMap.get(post.getCategoryId());
            vo.setCategoryName(category == null ? null : category.getName());
            vo.setReplyCount(postReplyRepository.selectCount(new LambdaQueryWrapper<SysPostReply>()
                    .eq(SysPostReply::getPostId, post.getId())
                    .eq(SysPostReply::getStatus, 1)).intValue());

            vo.setLiked(false);
            vo.setCollected(false);
            vo.setMediaList(mediaMap.getOrDefault(post.getId(), List.of()).stream().map(this::toMediaVO).toList());
            return vo;
        }).toList();
    }

    private PostMediaVO toMediaVO(SysPostMedia media) {
        PostMediaVO vo = new PostMediaVO();
        vo.setId(media.getId());
        vo.setMediaUrl(media.getMediaUrl());
        vo.setMediaType(media.getMediaType());
        vo.setSortOrder(media.getSortOrder());
        return vo;
    }

    private List<PostReplyVO> toReplyVO(List<SysPostReply> replies) {
        if (replies.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = replies.stream().map(SysPostReply::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of() :
                userRepository.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));

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
            vo.setStatus(reply.getStatus());
            vo.setCreateTime(reply.getCreateTime());

            SysUser user = userMap.get(reply.getUserId());
            vo.setUserNickname(user == null ? "用户" : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUserName()));
            vo.setUserAvatar(user == null ? null : user.getAvatar());
            return vo;
        }).toList();
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
}
