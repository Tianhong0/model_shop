package org.majun.backend.service;

import org.majun.backend.dto.BannerCreateRequest;
import org.majun.backend.dto.BannerQueryRequest;
import org.majun.backend.dto.BannerStatusUpdateRequest;
import org.majun.backend.dto.BannerUpdateRequest;
import org.majun.backend.dto.NoticeCreateRequest;
import org.majun.backend.dto.NoticeQueryRequest;
import org.majun.backend.dto.NoticeStatusUpdateRequest;
import org.majun.backend.dto.NoticeUpdateRequest;
import org.majun.backend.vo.AdminOperationStatusVO;
import org.majun.backend.vo.BannerVO;
import org.majun.backend.vo.HomeConfigVO;
import org.majun.backend.vo.NoticeVO;
import org.majun.backend.vo.PageResult;

/**
 * 操作服务接口
 */
public interface OperationService {

    PageResult<BannerVO> getBannerAdminList(BannerQueryRequest request);

    BannerVO getBannerDetail(Long id);

    Long createBanner(BannerCreateRequest request);

    void updateBanner(BannerUpdateRequest request);

    void updateBannerStatus(BannerStatusUpdateRequest request);

    void deleteBanner(Long id);

    PageResult<NoticeVO> getNoticeAdminList(NoticeQueryRequest request);

    NoticeVO getNoticeDetail(Long id);

    Long createNotice(NoticeCreateRequest request, Long createBy);

    void updateNotice(NoticeUpdateRequest request);

    void updateNoticeStatus(NoticeStatusUpdateRequest request);

    void deleteNotice(Long id);

    HomeConfigVO getHomeConfig();

    AdminOperationStatusVO getAdminOperationStatus();

    void updateAdminOperationStatus(Boolean operating);
}
