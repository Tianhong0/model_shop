package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.majun.backend.entity.BountyTask;
import org.majun.backend.entity.DesignerApplyRequest;
import org.majun.backend.entity.PrintJob;
import org.majun.backend.entity.SysAdminMessageRead;
import org.majun.backend.entity.SysModel;
import org.majun.backend.entity.SysModelList;
import org.majun.backend.entity.SysModelListInteraction;
import org.majun.backend.entity.SysNotice;
import org.majun.backend.entity.SysEvent;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.entity.SysOrderAfterSale;
import org.majun.backend.entity.SysUser;
import org.majun.backend.entity.UserDeletionRequest;
import org.majun.backend.entity.UsedReport;
import org.majun.backend.entity.WalletWithdraw;
import org.majun.backend.entity.AdminRegisterRequest;
import org.majun.backend.enums.AfterSaleStatus;
import org.majun.backend.enums.AdminRegisterStatus;
import org.majun.backend.enums.BountyTaskStatus;
import org.majun.backend.enums.DeletionStatus;
import org.majun.backend.enums.DesignerApplyStatus;
import org.majun.backend.enums.OrderStatus;
import org.majun.backend.enums.PrintJobStatus;
import org.majun.backend.enums.UsedReportStatus;
import org.majun.backend.enums.WalletWithdrawStatus;
import org.majun.backend.repository.BountyTaskRepository;
import org.majun.backend.repository.DesignerApplyRequestRepository;
import org.majun.backend.repository.PrintJobRepository;
import org.majun.backend.repository.SysAdminMessageReadRepository;
import org.majun.backend.repository.SysModelRepository;
import org.majun.backend.repository.SysModelListRepository;
import org.majun.backend.repository.SysModelListInteractionRepository;
import org.majun.backend.repository.SysNoticeRepository;
import org.majun.backend.repository.SysEventRepository;
import org.majun.backend.repository.SysOrderAfterSaleRepository;
import org.majun.backend.repository.SysOrderRepository;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.repository.UserDeletionRequestRepository;
import org.majun.backend.repository.UsedReportRepository;
import org.majun.backend.repository.WalletWithdrawRepository;
import org.majun.backend.repository.AdminRegisterRequestRepository;
import org.majun.backend.service.DashboardService;
import org.majun.backend.vo.DashboardMessageVO;
import org.majun.backend.vo.DashboardOverviewVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SysUserRepository userRepository;
    private final SysOrderRepository orderRepository;
    private final SysModelRepository modelRepository;
    private final SysModelListRepository modelListRepository;
    private final SysModelListInteractionRepository modelListInteractionRepository;
    private final SysOrderAfterSaleRepository orderAfterSaleRepository;
    private final WalletWithdrawRepository walletWithdrawRepository;
    private final PrintJobRepository printJobRepository;
    private final BountyTaskRepository bountyTaskRepository;
    private final UserDeletionRequestRepository userDeletionRequestRepository;
    private final SysNoticeRepository noticeRepository;
    private final SysAdminMessageReadRepository adminMessageReadRepository;
    private final AdminRegisterRequestRepository adminRegisterRequestRepository;
    private final DesignerApplyRequestRepository designerApplyRequestRepository;
    private final SysEventRepository eventRepository;
    private final UsedReportRepository usedReportRepository;

    @Override
    public DashboardOverviewVO getAdminOverview() {
        DashboardOverviewVO overview = new DashboardOverviewVO();
        List<DashboardOverviewVO.TrendPointVO> trend7d = buildTrend7d();
        DashboardOverviewVO.TodoVO todo = buildTodo();
        overview.setKpi(buildKpi(trend7d, todo));
        overview.setTrend7d(trend7d);
        overview.setTodo(todo);
        overview.setRecentOrders(buildRecentOrders());
        overview.setGeneratedAt(LocalDateTime.now());
        return overview;
    }

    @Override
    public List<DashboardMessageVO> getAdminMessages(Long adminUserId) {
        List<DashboardMessageVO> currentMessages = buildCurrentMessages();
        if (adminUserId == null || adminUserId <= 0) {
            currentMessages.forEach(item -> item.setUnread(true));
            return currentMessages;
        }

        Set<String> readKeys = adminMessageReadRepository.selectList(new LambdaQueryWrapper<SysAdminMessageRead>()
                        .eq(SysAdminMessageRead::getUserId, adminUserId))
                .stream()
                .map(SysAdminMessageRead::getMessageKey)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        currentMessages.forEach(item -> item.setUnread(!readKeys.contains(item.getKey())));
        return currentMessages;
    }

    @Override
    public void markAllAdminMessagesRead(Long adminUserId) {
        if (adminUserId == null || adminUserId <= 0) {
            return;
        }

        List<DashboardMessageVO> messages = buildCurrentMessages();

        adminMessageReadRepository.delete(new LambdaQueryWrapper<SysAdminMessageRead>()
                .eq(SysAdminMessageRead::getUserId, adminUserId));

        if (messages.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (DashboardMessageVO message : messages) {
            if (!StringUtils.hasText(message.getKey())) {
                continue;
            }
            SysAdminMessageRead read = new SysAdminMessageRead();
            read.setUserId(adminUserId);
            read.setMessageKey(message.getKey());
            read.setReadTime(now);
            adminMessageReadRepository.insert(read);
        }
    }

    private DashboardOverviewVO.KpiVO buildKpi(List<DashboardOverviewVO.TrendPointVO> trend7d,
                                               DashboardOverviewVO.TodoVO todo) {
        DashboardOverviewVO.KpiVO kpi = new DashboardOverviewVO.KpiVO();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Long totalUsers = userRepository.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1));

        Long totalOrders = orderRepository.selectCount(new LambdaQueryWrapper<SysOrder>());

        Long totalModels = modelRepository.selectCount(new LambdaQueryWrapper<SysModel>()
                .eq(SysModel::getIsDelete, 0));

        Long totalModelLists = modelListRepository.selectCount(new LambdaQueryWrapper<SysModelList>()
                .eq(SysModelList::getStatus, 1)
                .eq(SysModelList::getIsDelete, 0));

        Long totalModelListInteractions = modelListInteractionRepository.selectCount(null);

        BigDecimal orderAmount7d = trend7d.stream()
                .map(DashboardOverviewVO.TrendPointVO::getOrderAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        kpi.setTotalUsers(defaultLong(totalUsers));
        kpi.setTotalOrders(defaultLong(totalOrders));
        kpi.setTotalModels(defaultLong(totalModels));
        kpi.setTotalModelLists(defaultLong(totalModelLists));
        kpi.setTotalModelListInteractions(defaultLong(totalModelListInteractions));
        kpi.setTotalTodos(defaultLong(todo.getTotal()));
        kpi.setOrderAmount7d(orderAmount7d.setScale(2, RoundingMode.HALF_UP));

        long usersToday = countActiveUsersByDay(today);
        long usersYesterday = countActiveUsersByDay(yesterday);
        long ordersToday = countValidOrdersByDay(today);
        long ordersYesterday = countValidOrdersByDay(yesterday);
        long modelsToday = countModelsByDay(today);
        long modelsYesterday = countModelsByDay(yesterday);
        long modelListsToday = countModelListsByDay(today);
        long modelListsYesterday = countModelListsByDay(yesterday);
        long todosToday = countTodosCreatedByDay(today);
        long todosYesterday = countTodosCreatedByDay(yesterday);

        BigDecimal current7dAmount = sumOrderAmountBetween(today.minusDays(6), today);
        BigDecimal previous7dAmount = sumOrderAmountBetween(today.minusDays(13), today.minusDays(7));

        kpi.setUsersTrendPct(calcTrendPct(usersToday, usersYesterday));
        kpi.setOrdersTrendPct(calcTrendPct(ordersToday, ordersYesterday));
        kpi.setModelsTrendPct(calcTrendPct(modelsToday, modelsYesterday));
        kpi.setModelListsTrendPct(calcTrendPct(modelListsToday, modelListsYesterday));
        kpi.setTodosTrendPct(calcTrendPct(todosToday, todosYesterday));
        kpi.setOrderAmount7dTrendPct(calcTrendPct(current7dAmount, previous7dAmount));
        return kpi;
    }

    private DashboardOverviewVO.TodoVO buildTodo() {
        DashboardOverviewVO.TodoVO todo = new DashboardOverviewVO.TodoVO();

        Long afterSalePending = orderAfterSaleRepository.selectCount(new LambdaQueryWrapper<SysOrderAfterSale>()
                .in(SysOrderAfterSale::getStatus, AfterSaleStatus.APPLIED.getCode(), AfterSaleStatus.REVIEWING.getCode()));

        Long withdrawPending = walletWithdrawRepository.selectCount(new LambdaQueryWrapper<WalletWithdraw>()
                .eq(WalletWithdraw::getStatus, WalletWithdrawStatus.APPLIED.getCode())
                .eq(WalletWithdraw::getIsDelete, 0));

        Long deletionPending = userDeletionRequestRepository.selectCount(new LambdaQueryWrapper<UserDeletionRequest>()
            .eq(UserDeletionRequest::getStatus, DeletionStatus.PENDING.getCode()));

        Long modelReviewPending = modelRepository.selectCount(new LambdaQueryWrapper<SysModel>()
            .eq(SysModel::getIsDelete, 0)
            .eq(SysModel::getStatus, 0));

        Long bountyReviewPending = bountyTaskRepository.selectCount(new LambdaQueryWrapper<BountyTask>()
            .eq(BountyTask::getStatus, BountyTaskStatus.WAIT_ESCROW_PAYMENT.getCode()));

        Long adminRegisterPending = adminRegisterRequestRepository.selectCount(new LambdaQueryWrapper<AdminRegisterRequest>()
            .eq(AdminRegisterRequest::getStatus, AdminRegisterStatus.PENDING.getCode()));

        Long designerApplyPending = designerApplyRequestRepository.selectCount(new LambdaQueryWrapper<DesignerApplyRequest>()
            .eq(DesignerApplyRequest::getStatus, DesignerApplyStatus.PENDING.getCode()));

        Long printException = printJobRepository.selectCount(new LambdaQueryWrapper<PrintJob>()
                .in(PrintJob::getStatus, PrintJobStatus.SLICE_FAILED.getCode(), PrintJobStatus.FAILED.getCode()));

        Long bountyDisputed = bountyTaskRepository.selectCount(new LambdaQueryWrapper<BountyTask>()
                .eq(BountyTask::getStatus, BountyTaskStatus.DISPUTED.getCode()));

        Long eventReviewing = eventRepository.selectCount(new LambdaQueryWrapper<SysEvent>()
                .eq(SysEvent::getStatus, 3)
                .eq(SysEvent::getIsDelete, 0));

        Long usedReportPending = usedReportRepository.selectCount(new LambdaQueryWrapper<UsedReport>()
                .eq(UsedReport::getStatus, UsedReportStatus.PENDING.getCode()));

        long total = defaultLong(afterSalePending) + defaultLong(withdrawPending)
            + defaultLong(deletionPending) + defaultLong(modelReviewPending) + defaultLong(bountyReviewPending)
            + defaultLong(adminRegisterPending)
            + defaultLong(designerApplyPending)
            + defaultLong(printException) + defaultLong(bountyDisputed)
            + defaultLong(eventReviewing) + defaultLong(usedReportPending);

        todo.setAfterSalePending(defaultLong(afterSalePending));
        todo.setWithdrawPending(defaultLong(withdrawPending));
        todo.setDeletionPending(defaultLong(deletionPending));
        todo.setModelReviewPending(defaultLong(modelReviewPending));
        todo.setBountyReviewPending(defaultLong(bountyReviewPending));
        todo.setAdminRegisterPending(defaultLong(adminRegisterPending));
        todo.setDesignerApplyPending(defaultLong(designerApplyPending));
        todo.setPrintException(defaultLong(printException));
        todo.setBountyDisputed(defaultLong(bountyDisputed));
        todo.setEventReviewing(defaultLong(eventReviewing));
        todo.setUsedReportPending(defaultLong(usedReportPending));
        todo.setTotal(total);
        return todo;
    }

    private List<DashboardOverviewVO.TrendPointVO> buildTrend7d() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();

        List<SysOrder> orders = orderRepository.selectList(new LambdaQueryWrapper<SysOrder>()
                .ge(SysOrder::getCreateTime, startTime)
                .lt(SysOrder::getCreateTime, endTime)
                .orderByAsc(SysOrder::getCreateTime));

        Map<LocalDate, DashboardOverviewVO.TrendPointVO> trendMap = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = startDate.plusDays(i);
            DashboardOverviewVO.TrendPointVO point = new DashboardOverviewVO.TrendPointVO();
            point.setDate(day.format(DAY_FORMATTER));
            point.setOrderCount(0);
            point.setOrderAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            trendMap.put(day, point);
        }

        for (SysOrder order : orders) {
            if (order.getCreateTime() == null) {
                continue;
            }
            LocalDate day = order.getCreateTime().toLocalDate();
            DashboardOverviewVO.TrendPointVO point = trendMap.get(day);
            if (point == null) {
                continue;
            }
            if (order.getOrderStatus() != null && order.getOrderStatus().equals(OrderStatus.CANCELED.getCode())) {
                continue;
            }
            point.setOrderCount(point.getOrderCount() + 1);
            BigDecimal amount = order.getOrderPrice() == null ? BigDecimal.ZERO : order.getOrderPrice();
            point.setOrderAmount(point.getOrderAmount().add(amount).setScale(2, RoundingMode.HALF_UP));
        }

        return new ArrayList<>(trendMap.values());
    }

    private List<DashboardOverviewVO.RecentOrderVO> buildRecentOrders() {
        List<SysOrder> orders = orderRepository.selectList(new LambdaQueryWrapper<SysOrder>()
                .orderByDesc(SysOrder::getCreateTime)
                .last("LIMIT 10"));

        if (orders.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = orders.stream()
                .map(SysOrder::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, SysUser> userMap = userIds.isEmpty()
                ? Map.of()
                : userRepository.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user));

        return orders.stream().map(order -> {
            DashboardOverviewVO.RecentOrderVO vo = new DashboardOverviewVO.RecentOrderVO();
            vo.setOrderId(order.getId());
            vo.setOrderSn(order.getOrderSn());
            vo.setUserId(order.getUserId());
            vo.setOrderStatus(order.getOrderStatus());
            vo.setOrderPrice(order.getOrderPrice() == null ? BigDecimal.ZERO : order.getOrderPrice());
            vo.setCreateTime(order.getCreateTime());

            SysUser user = userMap.get(order.getUserId());
            if (user != null) {
                vo.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUserName());
            } else {
                vo.setUserNickname("-");
            }
            return vo;
        }).toList();
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private long countActiveUsersByDay(LocalDate day) {
        return defaultLong(userRepository.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .ge(SysUser::getCreateTime, day.atStartOfDay())
                .lt(SysUser::getCreateTime, day.plusDays(1).atStartOfDay())));
    }

    private long countValidOrdersByDay(LocalDate day) {
        return defaultLong(orderRepository.selectCount(new LambdaQueryWrapper<SysOrder>()
                .ne(SysOrder::getOrderStatus, OrderStatus.CANCELED.getCode())
                .ge(SysOrder::getCreateTime, day.atStartOfDay())
                .lt(SysOrder::getCreateTime, day.plusDays(1).atStartOfDay())));
    }

    private long countModelsByDay(LocalDate day) {
        return defaultLong(modelRepository.selectCount(new LambdaQueryWrapper<SysModel>()
                .eq(SysModel::getIsDelete, 0)
                .apply("date(create_time) = {0}", day.format(DAY_FORMATTER))));
    }

    private long countModelListsByDay(LocalDate day) {
        return defaultLong(modelListRepository.selectCount(new LambdaQueryWrapper<SysModelList>()
                .eq(SysModelList::getStatus, 1)
                .eq(SysModelList::getIsDelete, 0)
                .apply("date(create_time) = {0}", day.format(DAY_FORMATTER))));
    }

    private long countTodosCreatedByDay(LocalDate day) {
        long afterSale = defaultLong(orderAfterSaleRepository.selectCount(new LambdaQueryWrapper<SysOrderAfterSale>()
                .in(SysOrderAfterSale::getStatus, AfterSaleStatus.APPLIED.getCode(), AfterSaleStatus.REVIEWING.getCode())
                .ge(SysOrderAfterSale::getCreateTime, day.atStartOfDay())
                .lt(SysOrderAfterSale::getCreateTime, day.plusDays(1).atStartOfDay())));

        long withdraw = defaultLong(walletWithdrawRepository.selectCount(new LambdaQueryWrapper<WalletWithdraw>()
                .eq(WalletWithdraw::getStatus, WalletWithdrawStatus.APPLIED.getCode())
                .eq(WalletWithdraw::getIsDelete, 0)
                .ge(WalletWithdraw::getCreateTime, day.atStartOfDay())
                .lt(WalletWithdraw::getCreateTime, day.plusDays(1).atStartOfDay())));

        long deletion = defaultLong(userDeletionRequestRepository.selectCount(new LambdaQueryWrapper<UserDeletionRequest>()
            .eq(UserDeletionRequest::getStatus, DeletionStatus.PENDING.getCode())
            .ge(UserDeletionRequest::getRequestTime, day.atStartOfDay())
            .lt(UserDeletionRequest::getRequestTime, day.plusDays(1).atStartOfDay())));

        long modelReview = defaultLong(modelRepository.selectCount(new LambdaQueryWrapper<SysModel>()
            .eq(SysModel::getIsDelete, 0)
            .eq(SysModel::getStatus, 0)
            .apply("date(create_time) = {0}", day.format(DAY_FORMATTER))));

        long bountyReview = defaultLong(bountyTaskRepository.selectCount(new LambdaQueryWrapper<BountyTask>()
            .eq(BountyTask::getStatus, BountyTaskStatus.WAIT_ESCROW_PAYMENT.getCode())
            .ge(BountyTask::getCreateTime, day.atStartOfDay())
            .lt(BountyTask::getCreateTime, day.plusDays(1).atStartOfDay())));

        long adminRegister = defaultLong(adminRegisterRequestRepository.selectCount(new LambdaQueryWrapper<AdminRegisterRequest>()
            .eq(AdminRegisterRequest::getStatus, AdminRegisterStatus.PENDING.getCode())
            .ge(AdminRegisterRequest::getRequestTime, day.atStartOfDay())
            .lt(AdminRegisterRequest::getRequestTime, day.plusDays(1).atStartOfDay())));

        long designerApply = defaultLong(designerApplyRequestRepository.selectCount(new LambdaQueryWrapper<DesignerApplyRequest>()
            .eq(DesignerApplyRequest::getStatus, DesignerApplyStatus.PENDING.getCode())
            .ge(DesignerApplyRequest::getRequestTime, day.atStartOfDay())
            .lt(DesignerApplyRequest::getRequestTime, day.plusDays(1).atStartOfDay())));

        long printException = defaultLong(printJobRepository.selectCount(new LambdaQueryWrapper<PrintJob>()
                .in(PrintJob::getStatus, PrintJobStatus.SLICE_FAILED.getCode(), PrintJobStatus.FAILED.getCode())
                .ge(PrintJob::getCreateTime, day.atStartOfDay())
                .lt(PrintJob::getCreateTime, day.plusDays(1).atStartOfDay())));

        long bounty = defaultLong(bountyTaskRepository.selectCount(new LambdaQueryWrapper<BountyTask>()
                .eq(BountyTask::getStatus, BountyTaskStatus.DISPUTED.getCode())
                .ge(BountyTask::getCreateTime, day.atStartOfDay())
                .lt(BountyTask::getCreateTime, day.plusDays(1).atStartOfDay())));

        long eventReview = defaultLong(eventRepository.selectCount(new LambdaQueryWrapper<SysEvent>()
                .eq(SysEvent::getStatus, 3)
                .eq(SysEvent::getIsDelete, 0)
                .ge(SysEvent::getCreateTime, day.atStartOfDay())
                .lt(SysEvent::getCreateTime, day.plusDays(1).atStartOfDay())));

        long usedReport = defaultLong(usedReportRepository.selectCount(new LambdaQueryWrapper<UsedReport>()
                .eq(UsedReport::getStatus, UsedReportStatus.PENDING.getCode())
                .ge(UsedReport::getCreateTime, day.atStartOfDay())
                .lt(UsedReport::getCreateTime, day.plusDays(1).atStartOfDay())));

        return afterSale + withdraw + deletion + modelReview + bountyReview + adminRegister + designerApply + printException + bounty + eventReview + usedReport;
    }

    private BigDecimal sumOrderAmountBetween(LocalDate startDay, LocalDate endDay) {
        LocalDateTime startTime = startDay.atStartOfDay();
        LocalDateTime endTime = endDay.plusDays(1).atStartOfDay();
        List<SysOrder> orders = orderRepository.selectList(new LambdaQueryWrapper<SysOrder>()
                .ne(SysOrder::getOrderStatus, OrderStatus.CANCELED.getCode())
                .ge(SysOrder::getCreateTime, startTime)
                .lt(SysOrder::getCreateTime, endTime));

        return orders.stream()
                .map(item -> item.getOrderPrice() == null ? BigDecimal.ZERO : item.getOrderPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcTrendPct(long current, long previous) {
        return calcTrendPct(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }

    private BigDecimal calcTrendPct(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = current == null ? BigDecimal.ZERO : current;
        BigDecimal safePrevious = previous == null ? BigDecimal.ZERO : previous;

        if (safePrevious.compareTo(BigDecimal.ZERO) == 0) {
            if (safeCurrent.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }

        return safeCurrent.subtract(safePrevious)
                .multiply(BigDecimal.valueOf(100))
                .divide(safePrevious, 2, RoundingMode.HALF_UP);
    }

    private List<DashboardMessageVO> buildCurrentMessages() {
        DashboardOverviewVO.TodoVO todo = buildTodo();
        List<DashboardMessageVO> messages = new ArrayList<>();

        messages.add(buildTodoMessage("todo-after-sale-" + defaultLong(todo.getAfterSalePending()), "售后", "warning",
                "售后待处理", defaultLong(todo.getAfterSalePending()), "/orders/after-sales"));
        messages.add(buildTodoMessage("todo-withdraw-" + defaultLong(todo.getWithdrawPending()), "资金", "warning",
                "提现待审核", defaultLong(todo.getWithdrawPending()), "/finance/withdraws"));
        messages.add(buildTodoMessage("todo-deletion-" + defaultLong(todo.getDeletionPending()), "用户", "danger",
                "注销申请待审核", defaultLong(todo.getDeletionPending()), "/users/deletion-requests"));
        messages.add(buildTodoMessage("todo-model-review-" + defaultLong(todo.getModelReviewPending()), "模型", "primary",
                "模型待审核", defaultLong(todo.getModelReviewPending()), "/models/list"));
        messages.add(buildTodoMessage("todo-bounty-review-" + defaultLong(todo.getBountyReviewPending()), "悬赏", "primary",
                "悬赏待审核", defaultLong(todo.getBountyReviewPending()), "/bounty"));
        messages.add(buildTodoMessage("todo-admin-register-" + defaultLong(todo.getAdminRegisterPending()), "账号", "warning",
            "管理员注册待审核", defaultLong(todo.getAdminRegisterPending()), "/users/admin-register-requests"));
        messages.add(buildTodoMessage("todo-designer-apply-" + defaultLong(todo.getDesignerApplyPending()), "设计者", "warning",
            "设计者申请待审核", defaultLong(todo.getDesignerApplyPending()), "/users/designer-apply-requests"));
        messages.add(buildTodoMessage("todo-print-exception-" + defaultLong(todo.getPrintException()), "打印", "danger",
                "打印异常", defaultLong(todo.getPrintException()), "/print-queue"));
        messages.add(buildTodoMessage("todo-bounty-disputed-" + defaultLong(todo.getBountyDisputed()), "悬赏", "danger",
                "悬赏争议", defaultLong(todo.getBountyDisputed()), "/bounty"));
        messages.add(buildTodoMessage("todo-event-review-" + defaultLong(todo.getEventReviewing()), "活动", "primary",
                "活动评审中", defaultLong(todo.getEventReviewing()), "/events"));
        messages.add(buildTodoMessage("todo-used-report-" + defaultLong(todo.getUsedReportPending()), "二手", "warning",
                "二手举报待处理", defaultLong(todo.getUsedReportPending()), "/used/reports"));

        // 模型清单统计消息
        Long totalModelLists = modelListRepository.selectCount(new LambdaQueryWrapper<SysModelList>()
                .eq(SysModelList::getStatus, 1)
                .eq(SysModelList::getIsDelete, 0));
        Long totalInteractions = modelListInteractionRepository.selectCount(null);
        DashboardMessageVO modelListMsg = new DashboardMessageVO();
        modelListMsg.setKey("stat-model-list-" + defaultLong(totalModelLists));
        modelListMsg.setTag("清单");
        modelListMsg.setType("info");
        modelListMsg.setContent("清单总数：" + defaultLong(totalModelLists) + "，互动总数：" + defaultLong(totalInteractions));
        modelListMsg.setTime("实时");
        modelListMsg.setRoute("/model-lists");
        messages.add(modelListMsg);

        List<SysNotice> notices = noticeRepository.selectList(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, 1)
                .orderByDesc(SysNotice::getCreateTime)
                .last("LIMIT 5"));

        for (SysNotice notice : notices) {
            DashboardMessageVO vo = new DashboardMessageVO();
            vo.setKey("notice-" + notice.getId());
            vo.setTag("公告");
            vo.setType("info");
            vo.setContent(StringUtils.hasText(notice.getTitle()) ? notice.getTitle() : "系统公告");
            vo.setTime(formatRelativeTime(notice.getCreateTime()));
            vo.setRoute("/operation/notices");
            messages.add(vo);
        }

        messages.sort(Comparator.comparing((DashboardMessageVO item) -> {
            String key = item.getKey();
            if (!StringUtils.hasText(key) || !key.startsWith("todo-")) {
                return 0L;
            }
            int idx = key.lastIndexOf('-');
            if (idx < 0 || idx == key.length() - 1) {
                return 0L;
            }
            try {
                return Long.parseLong(key.substring(idx + 1));
            } catch (Exception ignored) {
                return 0L;
            }
        }).reversed());

        return messages;
    }

    private DashboardMessageVO buildTodoMessage(String key,
                                                String tag,
                                                String type,
                                                String title,
                                                long count,
                                                String route) {
        DashboardMessageVO vo = new DashboardMessageVO();
        vo.setKey(key);
        vo.setTag(tag);
        vo.setType(type);
        vo.setContent(count > 0 ? title + "：" + count + "条" : title + "：当前无待处理");
        vo.setTime("实时");
        vo.setRoute(route);
        return vo;
    }

    private String formatRelativeTime(LocalDateTime time) {
        if (time == null) {
            return "-";
        }
        long diffSeconds = java.time.Duration.between(time, LocalDateTime.now()).getSeconds();
        if (diffSeconds < 60) {
            return "刚刚";
        }
        if (diffSeconds < 3600) {
            return (diffSeconds / 60) + "分钟前";
        }
        if (diffSeconds < 86400) {
            return (diffSeconds / 3600) + "小时前";
        }
        return (diffSeconds / 86400) + "天前";
    }
}
