package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.BountyTaskAttachment;

@Mapper
public interface BountyTaskAttachmentRepository extends BaseMapper<BountyTaskAttachment> {
}
