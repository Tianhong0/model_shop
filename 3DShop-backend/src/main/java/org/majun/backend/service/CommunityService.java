package org.majun.backend.service;

import org.majun.backend.dto.PostCreateRequest;
import org.majun.backend.dto.PostInteractionMyQueryRequest;
import org.majun.backend.dto.PostInteractionToggleRequest;
import org.majun.backend.dto.PostMyQueryRequest;
import org.majun.backend.dto.PostQueryRequest;
import org.majun.backend.dto.PostReplyAdoptRequest;
import org.majun.backend.dto.PostReplyCreateRequest;
import org.majun.backend.dto.PostReplyLikeToggleRequest;
import org.majun.backend.dto.PostUpdateRequest;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PostCategoryVO;
import org.majun.backend.vo.PostDetailVO;
import org.majun.backend.vo.PostInteractionToggleVO;
import org.majun.backend.vo.PostListVO;
import org.majun.backend.vo.PostReplyLikeToggleVO;

import java.util.List;

/**
 * 社区服务接口
 */
public interface CommunityService {

    List<PostCategoryVO> getCategoryList();

    Long createPost(PostCreateRequest request, Long userId);

    void updatePost(PostUpdateRequest request, Long userId);

    void deletePost(Long postId, Long userId);

    PageResult<PostListVO> getPostPage(PostQueryRequest request, Long currentUserId);

    PostDetailVO getPostDetail(Long postId, Long currentUserId);

    Long createReply(PostReplyCreateRequest request, Long userId);

    void deleteReply(Long replyId, Long userId);

    void adoptReply(PostReplyAdoptRequest request, Long userId);

    PostReplyLikeToggleVO toggleReplyLike(PostReplyLikeToggleRequest request, Long userId);

    PostInteractionToggleVO toggleInteraction(PostInteractionToggleRequest request, Long userId);

    PageResult<PostListVO> getMyInteractionPosts(PostInteractionMyQueryRequest request, Long userId);

    PageResult<PostListVO> getMyPostPage(PostMyQueryRequest request, Long userId);
}
