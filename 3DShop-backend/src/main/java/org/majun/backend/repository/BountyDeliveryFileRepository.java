package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.BountyDeliveryFile;

/**
 * 悬赏交付文件Mapper
 */
@Mapper
public interface BountyDeliveryFileRepository extends BaseMapper<BountyDeliveryFile> {
}
