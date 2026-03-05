package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.OrderCreateRequest;
import org.majun.backend.dto.OrderQueryRequest;
import org.majun.backend.dto.OrderStatusUpdateRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.OrderService;
import org.majun.backend.vo.OrderCreateResponse;
import org.majun.backend.vo.OrderDetailVO;
import org.majun.backend.vo.OrderListVO;
import org.majun.backend.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Order controller.
 */
@Tag(name = "Order", description = "Order APIs")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create order", description = "Create a custom order")
    @PostMapping("/create")
    public Result<OrderCreateResponse> createOrder(@AuthenticationPrincipal LoginUser loginUser,
                                                   @Valid @RequestBody OrderCreateRequest request) {
        OrderCreateResponse response = orderService.createOrder(request, loginUser.getId());
        return Result.success(response);
    }

    @Operation(summary = "List my orders", description = "List current user orders")
    @PostMapping("/list")
    public Result<PageResult<OrderListVO>> listOrders(@AuthenticationPrincipal LoginUser loginUser,
                                                      @RequestBody(required = false) OrderQueryRequest request) {
        if (request == null) {
            request = new OrderQueryRequest();
        }
        PageResult<OrderListVO> result = orderService.getUserOrders(request, loginUser.getId());
        return Result.success(result);
    }

    @Operation(summary = "Order detail", description = "Get current user order detail")
    @GetMapping("/detail/{orderId}")
    public Result<OrderDetailVO> getOrderDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                @PathVariable Long orderId) {
        return Result.success(orderService.getOrderDetail(orderId, loginUser.getId()));
    }

    @Operation(summary = "Order detail by serial number", description = "Get current user order detail by order serial number")
    @GetMapping("/detail/by-sn/{orderSn}")
    public Result<OrderDetailVO> getOrderDetailByOrderSn(@AuthenticationPrincipal LoginUser loginUser,
                                                         @PathVariable String orderSn) {
        return Result.success(orderService.getOrderDetailByOrderSn(orderSn, loginUser.getId()));
    }

    @Operation(summary = "Cancel order", description = "Cancel an order in pending payment")
    @PutMapping("/cancel/{orderId}")
    public Result<Void> cancelOrder(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long orderId) {
        orderService.cancelOrder(orderId, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "Delete order", description = "Delete current user canceled/completed order")
    @DeleteMapping("/delete/{orderId}")
    public Result<Void> deleteOrder(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long orderId) {
        orderService.deleteOrder(orderId, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "Admin list orders", description = "Admin list orders with filters")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/list")
    public Result<PageResult<OrderListVO>> listOrdersForAdmin(@Valid @RequestBody OrderQueryRequest request) {
        return Result.success(orderService.getAdminOrders(request));
    }

    @Operation(summary = "Admin order detail", description = "Admin get order detail")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/detail/{orderId}")
    public Result<OrderDetailVO> getOrderDetailForAdmin(@PathVariable Long orderId) {
        return Result.success(orderService.getAdminOrderDetail(orderId));
    }

    @Operation(summary = "Admin update status", description = "Admin update order status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/status")
    public Result<Void> updateOrderStatus(@Valid @RequestBody OrderStatusUpdateRequest request) {
        orderService.updateOrderStatus(request);
        return Result.success();
    }
}
