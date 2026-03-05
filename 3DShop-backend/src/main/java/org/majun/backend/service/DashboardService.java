package org.majun.backend.service;

import org.majun.backend.vo.DashboardOverviewVO;
import org.majun.backend.vo.DashboardMessageVO;

import java.util.List;

public interface DashboardService {

    DashboardOverviewVO getAdminOverview();

    List<DashboardMessageVO> getAdminMessages(Long adminUserId);

    void markAllAdminMessagesRead(Long adminUserId);
}
