package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.majun.backend.dto.ModelExportRequest;
import org.majun.backend.dto.OrderExportRequest;
import org.majun.backend.dto.UserExportRequest;
import org.majun.backend.entity.SysModel;
import org.majun.backend.entity.SysUser;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.repository.SysModelRepository;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.repository.SysOrderRepository;
import org.majun.backend.service.DataExportService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 数据导出服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportServiceImpl implements DataExportService {

    private final SysOrderRepository orderRepository;
    private final SysUserRepository userRepository;
    private final SysModelRepository modelRepository;

    @Override
    public void exportOrders(OrderExportRequest request, HttpServletResponse response) {
        String filename = "orders_" + formatNow() + ".xlsx";
        setResponseHeaders(response, filename);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {
            Sheet sheet = workbook.createSheet("订单数据");

            // 创建表头
            Row header = sheet.createRow(0);
            String[] headers = {"订单ID", "订单编号", "用户ID", "订单金额", "订单状态", "模型ID", "打印数量", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            // 查询数据
            LambdaQueryWrapper<SysOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysOrder::getIsDelete, 0);

            // 优先使用订单ID列表
            if (request.getOrderIds() != null && !request.getOrderIds().isEmpty()) {
                wrapper.in(SysOrder::getId, request.getOrderIds());
            } else {
                // 没有指定ID列表时，使用筛选条件
                if (request.getOrderSn() != null && !request.getOrderSn().isBlank()) {
                    wrapper.like(SysOrder::getOrderSn, request.getOrderSn());
                }
                if (request.getUserId() != null) {
                    wrapper.eq(SysOrder::getUserId, request.getUserId());
                }
                if (request.getOrderStatuses() != null && !request.getOrderStatuses().isEmpty()) {
                    wrapper.in(SysOrder::getOrderStatus, request.getOrderStatuses());
                }
                if (request.getStartDate() != null) {
                    wrapper.ge(SysOrder::getCreateTime, request.getStartDate().atStartOfDay());
                }
                if (request.getEndDate() != null) {
                    wrapper.lt(SysOrder::getCreateTime, request.getEndDate().plusDays(1).atStartOfDay());
                }
            }
            wrapper.orderByDesc(SysOrder::getCreateTime);

            List<SysOrder> orders = orderRepository.selectList(wrapper);

            // 写入数据
            int rowNum = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (SysOrder order : orders) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(order.getId() != null ? order.getId().toString() : "");
                row.createCell(1).setCellValue(order.getOrderSn() != null ? order.getOrderSn() : "");
                row.createCell(2).setCellValue(order.getUserId() != null ? order.getUserId().toString() : "");
                row.createCell(3).setCellValue(order.getOrderPrice() != null ? order.getOrderPrice().doubleValue() : 0);
                row.createCell(4).setCellValue(getOrderStatusName(order.getOrderStatus()));
                row.createCell(5).setCellValue(order.getModelId() != null ? order.getModelId().toString() : "");
                row.createCell(6).setCellValue(order.getBatchQuantity() != null ? order.getBatchQuantity() : 1);
                row.createCell(7).setCellValue(order.getCreateTime() != null ? order.getCreateTime().format(formatter) : "");
            }

            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("导出订单数据失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportUsers(UserExportRequest request, HttpServletResponse response) {
        String filename = "users_" + formatNow() + ".xlsx";
        setResponseHeaders(response, filename);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {
            Sheet sheet = workbook.createSheet("用户数据");

            // 创建表头
            Row header = sheet.createRow(0);
            String[] headers = {"用户ID", "用户名", "昵称", "手机号", "邮箱", "性别", "状态", "注册时间"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            // 查询数据
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getIsDelete, 0);
            if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
                wrapper.in(SysUser::getStatus, request.getStatuses());
            }
            if (request.getRegisterStartDate() != null) {
                wrapper.ge(SysUser::getCreateTime, request.getRegisterStartDate().atStartOfDay());
            }
            if (request.getRegisterEndDate() != null) {
                wrapper.lt(SysUser::getCreateTime, request.getRegisterEndDate().plusDays(1).atStartOfDay());
            }
            wrapper.orderByDesc(SysUser::getCreateTime);

            List<SysUser> users = userRepository.selectList(wrapper);

            // 写入数据
            int rowNum = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (SysUser user : users) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(user.getId() != null ? user.getId().toString() : "");
                row.createCell(1).setCellValue(user.getUserName() != null ? user.getUserName() : "");
                row.createCell(2).setCellValue(user.getNickname() != null ? user.getNickname() : "");
                row.createCell(3).setCellValue(user.getMobile() != null ? user.getMobile() : "");
                row.createCell(4).setCellValue(user.getEmail() != null ? user.getEmail() : "");
                row.createCell(5).setCellValue(user.getSex() != null ? (user.getSex() == 1 ? "男" : "女") : "");
                row.createCell(6).setCellValue(user.getStatus() != null ? (user.getStatus() == 1 ? "正常" : "禁用") : "");
                row.createCell(7).setCellValue(user.getCreateTime() != null ? user.getCreateTime().format(formatter) : "");
            }

            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("导出用户数据失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportModels(ModelExportRequest request, HttpServletResponse response) {
        String filename = "models_" + formatNow() + ".xlsx";
        setResponseHeaders(response, filename);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {
            Sheet sheet = workbook.createSheet("模型数据");

            // 创建表头
            Row header = sheet.createRow(0);
            String[] headers = {"模型ID", "模型名称", "设计师ID", "分类ID", "基础价格", "状态", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            // 查询数据
            LambdaQueryWrapper<SysModel> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysModel::getIsDelete, 0);

            // 优先使用模型ID列表
            if (request.getModelIds() != null && !request.getModelIds().isEmpty()) {
                wrapper.in(SysModel::getId, request.getModelIds());
            } else {
                // 没有指定ID列表时，使用筛选条件
                if (request.getModelName() != null && !request.getModelName().isBlank()) {
                    wrapper.like(SysModel::getModelName, request.getModelName());
                }
                if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
                    wrapper.in(SysModel::getStatus, request.getStatuses());
                }
                if (request.getCategoryId() != null) {
                    wrapper.eq(SysModel::getCategoryId, request.getCategoryId());
                }
                if (request.getDesignerId() != null) {
                    wrapper.eq(SysModel::getDesignerId, request.getDesignerId());
                }
                if (request.getStartDate() != null) {
                    wrapper.ge(SysModel::getCreateTime, request.getStartDate().atStartOfDay());
                }
                if (request.getEndDate() != null) {
                    wrapper.lt(SysModel::getCreateTime, request.getEndDate().plusDays(1).atStartOfDay());
                }
            }
            wrapper.orderByDesc(SysModel::getCreateTime);

            List<SysModel> models = modelRepository.selectList(wrapper);

            // 写入数据
            int rowNum = 1;
            for (SysModel model : models) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(model.getId() != null ? model.getId().toString() : "");
                row.createCell(1).setCellValue(model.getModelName() != null ? model.getModelName() : "");
                row.createCell(2).setCellValue(model.getDesignerId() != null ? model.getDesignerId().toString() : "");
                row.createCell(3).setCellValue(model.getCategoryId() != null ? model.getCategoryId().toString() : "");
                row.createCell(4).setCellValue(model.getBasePrice() != null ? model.getBasePrice().doubleValue() : 0);
                row.createCell(5).setCellValue(getModelStatusName(model.getStatus()));
                row.createCell(6).setCellValue(model.getCreateTime() != null ? model.getCreateTime() : "");
            }

            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("导出模型数据失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    private void setResponseHeaders(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
    }

    private String formatDate(java.time.LocalDate date) {
        return date != null ? date.toString() : "all";
    }

    private String formatNow() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private String getOrderStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "生产中";
            case 2 -> "待发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

    private String getModelStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "审核中";
            case 1 -> "上架";
            case 2 -> "下架";
            default -> "未知";
        };
    }
}
