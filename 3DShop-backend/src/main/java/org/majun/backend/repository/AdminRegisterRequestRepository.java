package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.AdminRegisterRequest;
import org.majun.backend.enums.AdminRegisterStatus;

@Mapper
public interface AdminRegisterRequestRepository extends BaseMapper<AdminRegisterRequest> {

    default AdminRegisterRequest findPendingByUserNameOrEmail(String userName, String email) {
        return selectOne(new LambdaQueryWrapper<AdminRegisterRequest>()
                .eq(AdminRegisterRequest::getStatus, AdminRegisterStatus.PENDING.getCode())
                .and(wrapper -> wrapper.eq(AdminRegisterRequest::getUserName, userName)
                        .or()
                        .eq(AdminRegisterRequest::getEmail, email))
                .last("limit 1"));
    }

    default AdminRegisterRequest findLatestByUserNameOrEmail(String userName, String email) {
        return selectOne(new LambdaQueryWrapper<AdminRegisterRequest>()
                .and(wrapper -> wrapper.eq(AdminRegisterRequest::getUserName, userName)
                        .or()
                        .eq(AdminRegisterRequest::getEmail, email))
                .orderByDesc(AdminRegisterRequest::getRequestTime)
                .last("limit 1"));
    }
}
