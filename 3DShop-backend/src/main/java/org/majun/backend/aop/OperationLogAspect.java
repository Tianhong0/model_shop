package org.majun.backend.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.majun.backend.annotation.OperationLog;
import org.majun.backend.context.OperationLogContext;
import org.majun.backend.entity.SysOperationLog;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.OperationLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(org.majun.backend.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        SysOperationLog logEntity = new SysOperationLog();
        logEntity.setOperationType(annotation.type());
        logEntity.setModule(annotation.module());
        logEntity.setDescription(annotation.description());
        logEntity.setTargetType(annotation.targetType());
        logEntity.setCreateTime(LocalDateTime.now());

        // 设置请求信息
        setRequestInfo(logEntity);

        // 设置操作人信息
        setOperatorInfo(logEntity);

        // 记录请求参数
        if (annotation.recordParams()) {
            try {
                Object[] args = point.getArgs();
                String params = getParams(args, signature.getParameterNames());
                logEntity.setContent(params);
            } catch (Exception e) {
                log.warn("记录请求参数失败", e);
            }
        }

        // 放入上下文
        OperationLogContext.setLog(logEntity);

        Object result = null;
        Exception exception = null;

        try {
            result = point.proceed();
            logEntity.setSuccess(1);

            // 记录返回结果
            if (annotation.recordResult() && result != null) {
                try {
                    logEntity.setAfterData(objectMapper.writeValueAsString(result));
                } catch (Exception e) {
                    log.warn("记录返回结果失败", e);
                }
            }
        } catch (Exception e) {
            exception = e;
            logEntity.setSuccess(0);
            logEntity.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            logEntity.setDuration(endTime - startTime);

            // 从上下文中获取可能被修改的数据
            SysOperationLog contextLog = OperationLogContext.getLog();
            if (contextLog != null) {
                if (contextLog.getBeforeData() != null) {
                    logEntity.setBeforeData(contextLog.getBeforeData());
                }
                if (contextLog.getAfterData() != null) {
                    logEntity.setAfterData(contextLog.getAfterData());
                }
                if (contextLog.getTargetId() != null) {
                    logEntity.setTargetId(contextLog.getTargetId());
                }
                if (contextLog.getContent() != null && logEntity.getContent() == null) {
                    logEntity.setContent(contextLog.getContent());
                }
            }

            // 异步保存日志
            try {
                operationLogService.logAsync(logEntity);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }

            OperationLogContext.clear();
        }

        return result;
    }

    private void setRequestInfo(SysOperationLog logEntity) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logEntity.setRequestUrl(request.getRequestURI());
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setIp(getClientIp(request));
        }
    }

    private void setOperatorInfo(SysOperationLog logEntity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            logEntity.setOperatorId(loginUser.getId());
            String nickname = loginUser.getUser() != null ? loginUser.getUser().getNickname() : null;
            logEntity.setOperatorName(nickname != null ? nickname : loginUser.getUsername());
        }
    }

    private String getParams(Object[] args, String[] paramNames) {
        if (args == null || args.length == 0) {
            return null;
        }

        try {
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                if (paramNames != null && i < paramNames.length) {
                    Object arg = args[i];
                    // 跳过不能序列化的参数
                    if (arg instanceof ServletRequest || arg instanceof ServletResponse || arg instanceof MultipartFile) {
                        continue;
                    }
                    params.put(paramNames[i], arg);
                }
            }
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "参数序列化失败: " + e.getMessage();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
