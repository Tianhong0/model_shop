package org.majun.backend.service;

import org.majun.backend.vo.DashboardOverviewVO;
import org.majun.backend.vo.DashboardMessageVO;

import java.util.List;

/**
 * 仪表盘服务接口
 */
public interface DashboardService {

    DashboardOverviewVO getAdminOverview();

    List<DashboardMessageVO> getAdminMessages(Long adminUserId);

    void markAllAdminMessagesRead(Long adminUserId);
}
