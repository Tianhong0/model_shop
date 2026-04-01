package org.majun.backend.service;

import jakarta.servlet.http.HttpServletResponse;
import org.majun.backend.dto.ModelExportRequest;
import org.majun.backend.dto.OrderExportRequest;
import org.majun.backend.dto.UserExportRequest;

/**
 * 数据导出服务接口
 */
public interface DataExportService {

    /**
     * 导出订单数据
     *
     * @param request  导出请求
     * @param response HTTP响应
     */
    void exportOrders(OrderExportRequest request, HttpServletResponse response);

    /**
     * 导出用户数据
     *
     * @param request  导出请求
     * @param response HTTP响应
     */
    void exportUsers(UserExportRequest request, HttpServletResponse response);

    /**
     * 导出模型数据
     *
     * @param request  导出请求
     * @param response HTTP响应
     */
    void exportModels(ModelExportRequest request, HttpServletResponse response);
}
