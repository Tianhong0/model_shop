package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.Result;
import org.majun.backend.dto.*;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.UsedTradeService;
import org.majun.backend.vo.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "二手交易", description = "二手交易相关接口")
@RestController
@RequestMapping("/api/used")
@RequiredArgsConstructor
@Validated
@Slf4j
/**
 * 二手交易控制器
 */
public class UsedTradeController {

    private final UsedTradeService usedTradeService;

    @Operation(summary = "二手商品分页")
    @PostMapping("/listing/page")
    public Result<PageResult<UsedListingListVO>> pageListings(@AuthenticationPrincipal LoginUser loginUser,
                                                              @RequestBody(required = false) UsedListingQueryRequest request) {
        return Result.success(usedTradeService.pageListings(request, loginUser == null ? null : loginUser.getId(), false));
    }

    @Operation(summary = "我的二手商品")
    @PostMapping("/listing/my/page")
    public Result<PageResult<UsedListingListVO>> pageMyListings(@AuthenticationPrincipal LoginUser loginUser,
                                                                @RequestBody(required = false) UsedListingQueryRequest request) {
        if (request == null) {
            request = new UsedListingQueryRequest();
        }
        request.setOnlyMine(true);
        return Result.success(usedTradeService.pageListings(request, loginUser.getId(), false));
    }

    @Operation(summary = "二手商品详情")
    @GetMapping("/listing/detail/{listingId}")
    public Result<UsedListingDetailVO> getListingDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                        @PathVariable Long listingId) {
        return Result.success(usedTradeService.getListingDetail(listingId, loginUser == null ? null : loginUser.getId(), false));
    }

    @Operation(summary = "发布二手商品")
    @PostMapping("/listing/create")
    public Result<Long> createListing(@AuthenticationPrincipal LoginUser loginUser,
                                      @Valid @RequestBody UsedListingCreateRequest request) {
        return Result.success(usedTradeService.createListing(request, loginUser.getId()));
    }

    @Operation(summary = "编辑二手商品")
    @PostMapping("/listing/update")
    public Result<Void> updateListing(@AuthenticationPrincipal LoginUser loginUser,
                                      @Valid @RequestBody UsedListingUpdateRequest request) {
        usedTradeService.updateListing(request, loginUser.getId(), false);
        return Result.success();
    }

    @Operation(summary = "修改二手商品状态")
    @PostMapping("/listing/status")
    public Result<Void> updateListingStatus(@AuthenticationPrincipal LoginUser loginUser,
                                            @Valid @RequestBody UsedListingStatusRequest request) {
        usedTradeService.updateListingStatus(request, loginUser.getId(), false);
        return Result.success();
    }

    @Operation(summary = "创建议价")
    @PostMapping("/offer/create")
    public Result<Long> createOffer(@AuthenticationPrincipal LoginUser loginUser,
                                    @Valid @RequestBody UsedOfferCreateRequest request) {
        return Result.success(usedTradeService.createOffer(request, loginUser.getId()));
    }

    @Operation(summary = "处理议价")
    @PostMapping("/offer/respond")
    public Result<Void> respondOffer(@AuthenticationPrincipal LoginUser loginUser,
                                     @Valid @RequestBody UsedOfferRespondRequest request) {
        usedTradeService.respondOffer(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "创建二手订单")
    @PostMapping("/order/create")
    public Result<Long> createOrder(@AuthenticationPrincipal LoginUser loginUser,
                                    @Valid @RequestBody UsedOrderCreateRequest request) {
        return Result.success(usedTradeService.createOrder(request, loginUser.getId()));
    }

    @Operation(summary = "支付二手订单")
    @PostMapping("/order/pay/{orderId}")
    public Result<OrderPayCreateResponse> payOrder(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable Long orderId) {
        return Result.success(usedTradeService.payOrder(orderId, loginUser.getId()));
    }

    @Operation(summary = "查询二手订单支付状态")
    @GetMapping("/order/pay/status/{orderId}")
    public Result<OrderPayStatusVO> queryPayStatus(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable Long orderId) {
        return Result.success(usedTradeService.queryPayStatus(orderId, loginUser.getId()));
    }

    @Operation(summary = "余额支付二手订单")
    @PostMapping("/order/pay/wallet/{orderId}")
    public Result<OrderPayStatusVO> payOrderByWallet(@AuthenticationPrincipal LoginUser loginUser,
                                                     @PathVariable Long orderId) {
        return Result.success(usedTradeService.payOrderByWallet(orderId, loginUser.getId()));
    }

    @Operation(summary = "同步二手订单支付状态")
    @PostMapping("/order/pay/sync/{orderId}")
    public Result<OrderPayStatusVO> syncPayStatus(@AuthenticationPrincipal LoginUser loginUser,
                                                  @PathVariable Long orderId) {
        return Result.success(usedTradeService.syncPayStatus(orderId, loginUser.getId()));
    }

    @Operation(summary = "二手订单支付宝回调")
    @PostMapping("/order/pay/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    params.put(key, values[0]);
                }
            });
            boolean success = usedTradeService.handleAlipayNotify(params);
            return success ? "success" : "failure";
        } catch (Exception ex) {
            log.error("二手订单支付宝回调处理失败", ex);
            return "failure";
        }
    }

    @Operation(summary = "取消二手订单")
    @PostMapping("/order/cancel/{orderId}")
    public Result<Void> cancelOrder(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long orderId) {
        usedTradeService.cancelOrder(orderId, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "我的购买订单")
    @PostMapping("/order/buy/page")
    public Result<PageResult<UsedOrderListVO>> pageBuyerOrders(@AuthenticationPrincipal LoginUser loginUser,
                                                               @RequestBody(required = false) UsedOrderQueryRequest request) {
        return Result.success(usedTradeService.pageBuyerOrders(request, loginUser.getId()));
    }

    @Operation(summary = "我的出售订单")
    @PostMapping("/order/sell/page")
    public Result<PageResult<UsedOrderListVO>> pageSellerOrders(@AuthenticationPrincipal LoginUser loginUser,
                                                                @RequestBody(required = false) UsedOrderQueryRequest request) {
        return Result.success(usedTradeService.pageSellerOrders(request, loginUser.getId()));
    }

    @Operation(summary = "二手订单详情")
    @GetMapping("/order/detail/{orderId}")
    public Result<UsedOrderDetailVO> getOrderDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                    @PathVariable Long orderId) {
        return Result.success(usedTradeService.getOrderDetail(orderId, loginUser.getId(), false));
    }

    @Operation(summary = "卖家发货")
    @PostMapping("/order/ship")
    public Result<Void> shipOrder(@AuthenticationPrincipal LoginUser loginUser,
                                  @Valid @RequestBody UsedOrderShipRequest request) {
        usedTradeService.shipOrder(request, loginUser.getId(), false);
        return Result.success();
    }

    @Operation(summary = "确认收货")
    @PostMapping("/order/confirm/{orderId}")
    public Result<Void> confirmReceive(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable Long orderId) {
        usedTradeService.confirmReceive(orderId, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "修改收货地址")
    @PostMapping("/order/address/update")
    public Result<Void> updateOrderAddress(@AuthenticationPrincipal LoginUser loginUser,
                                           @Valid @RequestBody UsedOrderAddressUpdateRequest request) {
        usedTradeService.updateOrderAddress(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "发送二手消息")
    @PostMapping("/message/send")
    public Result<Long> sendMessage(@AuthenticationPrincipal LoginUser loginUser,
                                    @Valid @RequestBody UsedMessageSendRequest request) {
        return Result.success(usedTradeService.sendMessage(request, loginUser.getId()));
    }

    @Operation(summary = "二手消息会话列表")
    @GetMapping("/message/session/list")
    public Result<java.util.List<UsedMessageSessionVO>> listMessageSessions(@AuthenticationPrincipal LoginUser loginUser,
                                                                            @RequestParam Long listingId) {
        return Result.success(usedTradeService.listMessageSessions(listingId, loginUser.getId()));
    }

    @Operation(summary = "二手消息分页")
    @GetMapping("/message/page")
    public Result<PageResult<UsedMessageVO>> pageMessages(@AuthenticationPrincipal LoginUser loginUser,
                                                          @RequestParam Long listingId,
                                                          @RequestParam Long counterpartId,
                                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(usedTradeService.pageMessages(listingId, counterpartId, pageNum, pageSize, loginUser.getId()));
    }

    @Operation(summary = "申请二手售后")
    @PostMapping("/after-sale/create")
    public Result<Long> createAfterSale(@AuthenticationPrincipal LoginUser loginUser,
                                        @Valid @RequestBody UsedAfterSaleCreateRequest request) {
        return Result.success(usedTradeService.createAfterSale(request, loginUser.getId()));
    }

    @Operation(summary = "我的售后列表")
    @PostMapping("/after-sale/buy/page")
    public Result<PageResult<UsedAfterSaleVO>> pageBuyerAfterSales(@AuthenticationPrincipal LoginUser loginUser,
                                                                   @RequestBody(required = false) UsedOrderQueryRequest request) {
        return Result.success(usedTradeService.pageBuyerAfterSales(request, loginUser.getId()));
    }

    @Operation(summary = "卖家售后列表")
    @PostMapping("/after-sale/sell/page")
    public Result<PageResult<UsedAfterSaleVO>> pageSellerAfterSales(@AuthenticationPrincipal LoginUser loginUser,
                                                                    @RequestBody(required = false) UsedOrderQueryRequest request) {
        return Result.success(usedTradeService.pageSellerAfterSales(request, loginUser.getId()));
    }

    @Operation(summary = "售后详情")
    @GetMapping("/after-sale/detail/{afterSaleId}")
    public Result<UsedAfterSaleVO> getAfterSaleDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                      @PathVariable Long afterSaleId) {
        return Result.success(usedTradeService.getAfterSaleDetail(afterSaleId, loginUser.getId(), false));
    }

    @Operation(summary = "卖家处理售后")
    @PostMapping("/after-sale/seller/audit")
    public Result<Void> auditAfterSaleBySeller(@AuthenticationPrincipal LoginUser loginUser,
                                               @Valid @RequestBody UsedAfterSaleAuditRequest request) {
        usedTradeService.auditAfterSaleBySeller(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "提交举报")
    @PostMapping("/report/create")
    public Result<Long> createReport(@AuthenticationPrincipal LoginUser loginUser,
                                     @Valid @RequestBody UsedReportCreateRequest request) {
        return Result.success(usedTradeService.createReport(request, loginUser.getId()));
    }

    @Operation(summary = "后台二手商品分页")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/listing/page")
    public Result<PageResult<UsedListingListVO>> pageAdminListings(@RequestBody(required = false) UsedListingQueryRequest request) {
        return Result.success(usedTradeService.pageListings(request, null, true));
    }

    @Operation(summary = "后台二手商品详情")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/listing/detail/{listingId}")
    public Result<UsedListingDetailVO> getAdminListingDetail(@PathVariable Long listingId) {
        return Result.success(usedTradeService.getListingDetail(listingId, null, true));
    }

    @Operation(summary = "后台二手商品状态")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/listing/status")
    public Result<Void> updateAdminListingStatus(@Valid @RequestBody UsedListingStatusRequest request) {
        usedTradeService.updateListingStatus(request, null, true);
        return Result.success();
    }

    @Operation(summary = "后台二手订单分页")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/order/page")
    public Result<PageResult<UsedOrderListVO>> pageAdminOrders(@RequestBody(required = false) UsedOrderQueryRequest request) {
        return Result.success(usedTradeService.pageAdminOrders(request));
    }

    @Operation(summary = "后台二手订单详情")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/order/detail/{orderId}")
    public Result<UsedOrderDetailVO> getAdminOrderDetail(@PathVariable Long orderId) {
        return Result.success(usedTradeService.getOrderDetail(orderId, null, true));
    }

    @Operation(summary = "后台代发货")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/order/ship")
    public Result<Void> adminShipOrder(@Valid @RequestBody UsedOrderShipRequest request) {
        usedTradeService.shipOrder(request, null, true);
        return Result.success();
    }

    @Operation(summary = "后台举报分页")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/report/page")
    public Result<PageResult<UsedReportVO>> pageAdminReports(@RequestBody(required = false) UsedReportQueryRequest request) {
        return Result.success(usedTradeService.pageAdminReports(request));
    }

    @Operation(summary = "后台处理举报")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/report/handle")
    public Result<Void> handleReport(@AuthenticationPrincipal LoginUser loginUser,
                                     @Valid @RequestBody UsedReportHandleRequest request) {
        usedTradeService.handleReport(request, loginUser.getId());
        return Result.success();
    }
}
