package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.PrintPrinter;

@Mapper
public interface PrintPrinterRepository extends BaseMapper<PrintPrinter> {
}
