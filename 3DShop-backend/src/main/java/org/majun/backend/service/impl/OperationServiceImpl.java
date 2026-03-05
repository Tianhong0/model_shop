package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.BannerCreateRequest;
import org.majun.backend.dto.BannerQueryRequest;
import org.majun.backend.dto.BannerStatusUpdateRequest;
import org.majun.backend.dto.BannerUpdateRequest;
import org.majun.backend.dto.NoticeCreateRequest;
import org.majun.backend.dto.NoticeQueryRequest;
import org.majun.backend.dto.NoticeStatusUpdateRequest;
import org.majun.backend.dto.NoticeUpdateRequest;
import org.majun.backend.entity.SysBanner;
import org.majun.backend.entity.SysNotice;
import org.majun.backend.repository.SysBannerRepository;
import org.majun.backend.repository.SysNoticeRepository;
import org.majun.backend.service.OperationService;
import org.majun.backend.vo.AdminOperationStatusVO;
import org.majun.backend.vo.BannerVO;
import org.majun.backend.vo.HomeConfigVO;
import org.majun.backend.vo.NoticeVO;
import org.majun.backend.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 运营管理服务实现
 */
@Service
@RequiredArgsConstructor
public class OperationServiceImpl implements OperationService {

    private final SysBannerRepository bannerRepository;
    private final SysNoticeRepository noticeRepository;
    private final AtomicBoolean operating = new AtomicBoolean(true);
    private volatile LocalDateTime operationStatusUpdatedAt = LocalDateTime.now();

    @Override
    public PageResult<BannerVO> getBannerAdminList(BannerQueryRequest request) {
        LambdaQueryWrapper<SysBanner> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getTitle())) {
            wrapper.like(SysBanner::getTitle, request.getTitle());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysBanner::getStatus, request.getStatus());
        }
        wrapper.orderByAsc(SysBanner::getSortNo)
                .orderByDesc(SysBanner::getCreateTime);

        Page<SysBanner> page = new Page<>(request.getPageNum(), request.getPageSize());
        bannerRepository.selectPage(page, wrapper);

        return PageResult.<BannerVO>builder()
                .records(page.getRecords().stream().map(this::toBannerVO).collect(Collectors.toList()))
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public BannerVO getBannerDetail(Long id) {
        return toBannerVO(getBannerOrThrow(id));
    }

    @Override
    public Long createBanner(BannerCreateRequest request) {
        validateBannerTimeRange(request.getStartTime(), request.getEndTime());

        SysBanner banner = new SysBanner();
        banner.setTitle(request.getTitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setLinkType(request.getLinkType() != null ? request.getLinkType() : 0);
        banner.setLinkValue(request.getLinkValue());
        banner.setSortNo(request.getSortNo() != null ? request.getSortNo() : 0);
        banner.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        banner.setStartTime(request.getStartTime());
        banner.setEndTime(request.getEndTime());
        banner.setIsDelete(0);

        bannerRepository.insert(banner);
        return banner.getId();
    }

    @Override
    public void updateBanner(BannerUpdateRequest request) {
        SysBanner banner = getBannerOrThrow(request.getId());

        LocalDateTime startTime = request.getStartTime() != null ? request.getStartTime() : banner.getStartTime();
        LocalDateTime endTime = request.getEndTime() != null ? request.getEndTime() : banner.getEndTime();
        validateBannerTimeRange(startTime, endTime);

        if (StringUtils.hasText(request.getTitle())) {
            banner.setTitle(request.getTitle());
        }
        if (StringUtils.hasText(request.getImageUrl())) {
            banner.setImageUrl(request.getImageUrl());
        }
        if (request.getLinkType() != null) {
            banner.setLinkType(request.getLinkType());
        }
        if (request.getLinkValue() != null) {
            banner.setLinkValue(request.getLinkValue());
        }
        if (request.getSortNo() != null) {
            banner.setSortNo(request.getSortNo());
        }
        if (request.getStatus() != null) {
            banner.setStatus(request.getStatus());
        }
        if (request.getStartTime() != null) {
            banner.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            banner.setEndTime(request.getEndTime());
        }

        bannerRepository.updateById(banner);
    }

    @Override
    public void updateBannerStatus(BannerStatusUpdateRequest request) {
        SysBanner banner = getBannerOrThrow(request.getId());
        banner.setStatus(request.getStatus());
        bannerRepository.updateById(banner);
    }

    @Override
    public void deleteBanner(Long id) {
        getBannerOrThrow(id);
        bannerRepository.deleteById(id);
    }

    @Override
    public PageResult<NoticeVO> getNoticeAdminList(NoticeQueryRequest request) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getTitle())) {
            wrapper.like(SysNotice::getTitle, request.getTitle());
        }
        if (request.getNoticeType() != null) {
            wrapper.eq(SysNotice::getNoticeType, request.getNoticeType());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysNotice::getStatus, request.getStatus());
        }
        wrapper.orderByDesc(SysNotice::getCreateTime);

        Page<SysNotice> page = new Page<>(request.getPageNum(), request.getPageSize());
        noticeRepository.selectPage(page, wrapper);

        return PageResult.<NoticeVO>builder()
                .records(page.getRecords().stream().map(this::toNoticeVO).collect(Collectors.toList()))
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public NoticeVO getNoticeDetail(Long id) {
        return toNoticeVO(getNoticeOrThrow(id));
    }

    @Override
    public Long createNotice(NoticeCreateRequest request, Long createBy) {
        SysNotice notice = new SysNotice();
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setNoticeType(request.getNoticeType() != null ? request.getNoticeType() : 1);
        notice.setLevel(StringUtils.hasText(request.getLevel()) ? request.getLevel() : "NORMAL");
        notice.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        notice.setCreateBy(createBy);
        notice.setIsDelete(0);

        noticeRepository.insert(notice);
        return notice.getId();
    }

    @Override
    public void updateNotice(NoticeUpdateRequest request) {
        SysNotice notice = getNoticeOrThrow(request.getId());

        if (StringUtils.hasText(request.getTitle())) {
            notice.setTitle(request.getTitle());
        }
        if (StringUtils.hasText(request.getContent())) {
            notice.setContent(request.getContent());
        }
        if (request.getNoticeType() != null) {
            notice.setNoticeType(request.getNoticeType());
        }
        if (StringUtils.hasText(request.getLevel())) {
            notice.setLevel(request.getLevel());
        }
        if (request.getStatus() != null) {
            notice.setStatus(request.getStatus());
        }

        noticeRepository.updateById(notice);
    }

    @Override
    public void updateNoticeStatus(NoticeStatusUpdateRequest request) {
        SysNotice notice = getNoticeOrThrow(request.getId());
        notice.setStatus(request.getStatus());
        noticeRepository.updateById(notice);
    }

    @Override
    public void deleteNotice(Long id) {
        getNoticeOrThrow(id);
        noticeRepository.deleteById(id);
    }

    @Override
    public HomeConfigVO getHomeConfig() {
        LocalDateTime now = LocalDateTime.now();

        List<BannerVO> banners = bannerRepository.selectList(
                        new LambdaQueryWrapper<SysBanner>()
                                .eq(SysBanner::getStatus, 1)
                                .and(wrapper -> wrapper
                                        .isNull(SysBanner::getStartTime)
                                        .or().le(SysBanner::getStartTime, now))
                                .and(wrapper -> wrapper
                                        .isNull(SysBanner::getEndTime)
                                        .or().ge(SysBanner::getEndTime, now))
                                .orderByAsc(SysBanner::getSortNo)
                                .last("LIMIT 5")
                ).stream()
                .map(this::toBannerVO)
                .collect(Collectors.toList());

        List<NoticeVO> notices = noticeRepository.selectList(
                        new LambdaQueryWrapper<SysNotice>()
                                .eq(SysNotice::getStatus, 1)
                                .orderByDesc(SysNotice::getCreateTime)
                                .last("LIMIT 5")
                ).stream()
                .map(this::toNoticeVO)
                .collect(Collectors.toList());

        return new HomeConfigVO(banners, notices);
    }

    @Override
    public AdminOperationStatusVO getAdminOperationStatus() {
        AdminOperationStatusVO vo = new AdminOperationStatusVO();
        vo.setOperating(operating.get());
        vo.setUpdatedAt(operationStatusUpdatedAt);
        return vo;
    }

    @Override
    public void updateAdminOperationStatus(Boolean operatingValue) {
        if (operatingValue == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "运营状态不能为空");
        }
        operating.set(Boolean.TRUE.equals(operatingValue));
        operationStatusUpdatedAt = LocalDateTime.now();
    }

    private SysBanner getBannerOrThrow(Long id) {
        SysBanner banner = bannerRepository.selectById(id);
        if (banner == null || Objects.equals(banner.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "轮播图不存在");
        }
        return banner;
    }

    private SysNotice getNoticeOrThrow(Long id) {
        SysNotice notice = noticeRepository.selectById(id);
        if (notice == null || Objects.equals(notice.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        return notice;
    }

    private BannerVO toBannerVO(SysBanner banner) {
        BannerVO vo = new BannerVO();
        vo.setId(banner.getId());
        vo.setTitle(banner.getTitle());
        vo.setImageUrl(banner.getImageUrl());
        vo.setLinkType(banner.getLinkType());
        vo.setLinkValue(banner.getLinkValue());
        vo.setSortNo(banner.getSortNo());
        vo.setStatus(banner.getStatus());
        vo.setStartTime(banner.getStartTime());
        vo.setEndTime(banner.getEndTime());
        vo.setCreateTime(banner.getCreateTime());
        return vo;
    }

    private NoticeVO toNoticeVO(SysNotice notice) {
        NoticeVO vo = new NoticeVO();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setContent(notice.getContent());
        vo.setNoticeType(notice.getNoticeType());
        vo.setLevel(notice.getLevel());
        vo.setStatus(notice.getStatus());
        vo.setCreateBy(notice.getCreateBy());
        vo.setCreateTime(notice.getCreateTime());
        return vo;
    }

    private void validateBannerTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "生效开始时间不能晚于结束时间");
        }
    }
}
