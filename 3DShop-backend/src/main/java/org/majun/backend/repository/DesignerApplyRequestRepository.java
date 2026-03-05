package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.DesignerApplyRequest;
import org.majun.backend.enums.DesignerApplyStatus;

@Mapper
public interface DesignerApplyRequestRepository extends BaseMapper<DesignerApplyRequest> {

    default DesignerApplyRequest findPendingByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapper<DesignerApplyRequest>()
                .eq(DesignerApplyRequest::getUserId, userId)
                .eq(DesignerApplyRequest::getStatus, DesignerApplyStatus.PENDING.getCode())
                .last("limit 1"));
    }

    default DesignerApplyRequest findLatestByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapper<DesignerApplyRequest>()
                .eq(DesignerApplyRequest::getUserId, userId)
                .orderByDesc(DesignerApplyRequest::getRequestTime)
                .last("limit 1"));
    }
}
