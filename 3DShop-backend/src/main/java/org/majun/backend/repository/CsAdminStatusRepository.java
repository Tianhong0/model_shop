package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.CsAdminStatus;

import java.util.List;

@Mapper
public interface CsAdminStatusRepository extends BaseMapper<CsAdminStatus> {

    /**
     * 获取所有在线的客服ID列表
     */
    List<Long> findOnlineAdminIds();

    /**
     * 根据ID列表查询客服状态
     */
    List<CsAdminStatus> findByAdminIdIn(List<Long> adminIds);

    /**
     * 通过adminId查询状态
     */
    CsAdminStatus findByAdminId(Long adminId);
}
