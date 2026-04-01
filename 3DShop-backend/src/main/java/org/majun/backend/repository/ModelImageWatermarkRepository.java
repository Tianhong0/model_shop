package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.majun.backend.entity.ModelImageWatermark;

@Mapper
public interface ModelImageWatermarkRepository extends BaseMapper<ModelImageWatermark> {

    /**
     * 物理删除模型的所有水印记录（绕过逻辑删除）
     */
    @Delete("DELETE FROM model_image_watermark WHERE model_id = #{modelId}")
    int physicalDeleteByModelId(@Param("modelId") Long modelId);

    /**
     * 物理删除所有水印记录（绕过逻辑删除）
     */
    @Delete("DELETE FROM model_image_watermark WHERE 1=1")
    int physicalDeleteAll();
}
