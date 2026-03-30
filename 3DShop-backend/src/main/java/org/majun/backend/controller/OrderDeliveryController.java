package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.DeliveryQueryRequest;
import org.majun.backend.dto.DeliveryShipRequest;
import org.majun.backend.dto.DeliveryStatusUpdateRequest;
import org.majun.backend.dto.DeliveryTrackAddRequest;
import org.majun.backend.dto.DeliveryTrackSimulateRequest;
import org.majun.backend.dto.RetryAutoShipRequest;
import org.majun.backend.dto.UserDeliveryQueryRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.OrderDeliveryService;
import org.majun.backend.vo.DeliveryDetailVO;
import org.majun.backend.vo.DeliveryListVO;
import org.majun.backend.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Order Delivery", description = "Order delivery APIs")
@RestController
@RequestMapping("/api/orders/delivery")
@RequiredArgsConstructor
public class OrderDeliveryController {

    private final OrderDeliveryService orderDeliveryService;

    @Operation(summary = "My delivery detail", description = "Query my delivery detail by order serial number")
    @PostMapping("/my/detail")
    public Result<DeliveryDetailVO> getMyDeliveryDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                         @Valid @RequestBody UserDeliveryQueryRequest request) {
        return Result.success(orderDeliveryService.getUserDeliveryByOrderSn(request.getOrderSn(), loginUser.getId()));
    }

    @Operation(summary = "Confirm receive", description = "User confirms receive by order serial number")
    @PostMapping("/my/sign/{orderSn}")
    public Result<Void> signMyOrder(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable String orderSn) {
        orderDeliveryService.userSignByOrderSn(orderSn, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "Ship order", description = "Create delivery order and mark as shipped")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/ship")
    public Result<Long> shipOrder(@Valid @RequestBody DeliveryShipRequest request) {
        return Result.success(orderDeliveryService.shipOrder(request));
    }

    @Operation(summary = "Admin delivery list", description = "Admin query delivery list")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/list")
    public Result<PageResult<DeliveryListVO>> getAdminDeliveryList(@Valid @RequestBody DeliveryQueryRequest request) {
        return Result.success(orderDeliveryService.getDeliveryPage(request));
    }

    @Operation(summary = "Admin delivery detail", description = "Admin query delivery detail")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/detail/{deliveryId}")
    public Result<DeliveryDetailVO> getAdminDeliveryDetail(@PathVariable Long deliveryId) {
        return Result.success(orderDeliveryService.getAdminDeliveryDetail(deliveryId));
    }

    @Operation(summary = "Update delivery status", description = "Admin update delivery status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/status")
    public Result<Void> updateDeliveryStatus(@Valid @RequestBody DeliveryStatusUpdateRequest request) {
        orderDeliveryService.updateDeliveryStatus(request);
        return Result.success();
    }

    @Operation(summary = "Add delivery track", description = "Admin add manual delivery track")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/track/add")
    public Result<Long> addTrack(@Valid @RequestBody DeliveryTrackAddRequest request) {
        return Result.success(orderDeliveryService.addTrack(request));
    }

    @Operation(summary = "Simulate delivery track", description = "Simulate delivery tracks based on receiver address")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/track/simulate")
    public Result<Void> simulateTracks(@Valid @RequestBody DeliveryTrackSimulateRequest request) {
        orderDeliveryService.simulateTracks(request);
        return Result.success();
    }

    @Operation(summary = "Retry auto ship", description = "Manually trigger auto-ship for orders that failed to auto-ship")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/retry-ship")
    public Result<Long> retryAutoShip(@Valid @RequestBody RetryAutoShipRequest request) {
        return Result.success(orderDeliveryService.retryAutoShip(request));
    }
}
