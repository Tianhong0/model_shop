package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysModelCategory;

/**
 * 模型分类Mapper接口
 */
@Mapper
public interface SysModelCategoryRepository extends BaseMapper<SysModelCategory> {

}
