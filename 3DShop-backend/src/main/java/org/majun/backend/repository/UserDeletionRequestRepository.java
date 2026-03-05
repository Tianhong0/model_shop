package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.UserDeletionRequest;
import org.majun.backend.enums.DeletionStatus;

/**
 * 用户注销申请 Mapper
 */
@Mapper
public interface UserDeletionRequestRepository extends BaseMapper<UserDeletionRequest> {

    /**
     * 根据用户ID和状态查询注销申请
     */
    default UserDeletionRequest findByUserIdAndStatus(Long userId, DeletionStatus status) {
        return selectOne(new LambdaQueryWrapper<UserDeletionRequest>()
                .eq(UserDeletionRequest::getUserId, userId)
                .eq(UserDeletionRequest::getStatus, status.getCode())
        );
    }
}
