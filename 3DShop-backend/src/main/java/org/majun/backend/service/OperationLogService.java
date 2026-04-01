package org.majun.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.majun.backend.dto.OperationLogQueryRequest;
import org.majun.backend.entity.SysOperationLog;
import org.majun.backend.vo.OperationLogVO;
import org.majun.backend.vo.PageResult;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {

    /**
     * 记录操作日志
     */
    void log(SysOperationLog log);

    /**
     * 异步记录操作日志
     */
    void logAsync(SysOperationLog log);

    /**
     * 分页查询操作日志
     */
    PageResult<OperationLogVO> queryPage(OperationLogQueryRequest request);

    /**
     * 查询日志详情
     */
    OperationLogVO getDetail(Long id);

    /**
     * 清理指定天数前的日志
     */
    int cleanOldLogs(int days);
}
