package org.majun.backend.service;

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
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PostCategoryVO;
import org.majun.backend.vo.PostDetailVO;
import org.majun.backend.vo.PostListVO;
import org.majun.backend.vo.PostReplyVO;

import java.util.List;

/**
 * 社区管理后台服务接口
 */
public interface CommunityAdminService {

    PageResult<PostListVO> getAdminPostPage(PostAdminQueryRequest request);

    PostDetailVO getAdminPostDetail(Long postId);

    void updatePostStatus(PostStatusUpdateRequest request);

    void updatePostTop(PostTopUpdateRequest request);

    void updatePostCategory(PostCategoryAssignRequest request);

    void deletePost(Long postId);

    PageResult<PostReplyVO> getAdminReplyPage(PostReplyAdminQueryRequest request);

    void deleteReply(Long replyId);

    void updateReplyStatus(PostReplyStatusUpdateRequest request);

    void updateReplyExcellent(PostReplyExcellentRequest request);

    List<PostCategoryVO> getCategoryAdminList();

    PageResult<PostCategoryVO> getCategoryAdminPage(PostCategoryAdminQueryRequest request);

    Long createCategory(PostCategoryCreateRequest request);

    void updateCategory(PostCategoryUpdateRequest request);

    void deleteCategory(Long categoryId);
}
