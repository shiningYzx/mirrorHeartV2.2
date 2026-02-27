package org.shiningyang.mirrorheart_v2_2.common.aspect;

import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.shiningyang.mirrorheart_v2_2.common.annotation.AuditLog;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.admin.entity.AdminAuditLog;
import org.shiningyang.mirrorheart_v2_2.module.admin.mapper.AdminAuditLogMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端操作审计日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AdminAuditLogMapper adminAuditLogMapper;

    // 配置织入点：拦截所有标有 @AuditLog 注解的方法
    @Pointcut("@annotation(org.shiningyang.mirrorheart_v2_2.common.annotation.AuditLog)")
    public void logPointCut() {}

    // 正常返回后执行
    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null);
    }

    // 抛出异常后执行 (虽然操作失败了，但作为审计也要记录尝试过的痕迹)
    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e);
    }

    protected void handleLog(final JoinPoint joinPoint, final Exception e) {
        try {
            // 获取方法上的注解
            AuditLog annotationLog = getAnnotationLog(joinPoint);
            if (annotationLog == null) {
                return;
            }

            // 1. 尝试获取当前操作的管理员 ID
            Long userId = 0L;
            try {
                userId = SecurityUtils.getUserId();
            } catch (Exception ignored) {
                // 如果出现未登录异常，保留 userId = 0
            }

            AdminAuditLog auditLog = new AdminAuditLog();
            auditLog.setUserId(userId);
            auditLog.setModule(annotationLog.module());
            auditLog.setOperation(annotationLog.operation());

            // 2. 获取 Request 上下文，提取 IP 地址
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIp(getIpAddress(request));
            }

            // 3. 获取方法的入参，转换为 JSON 存入 params 字段
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                List<Object> filteredArgs = new ArrayList<>();
                for (Object arg : args) {
                    // 过滤掉不可序列化的对象 (如 ServletRequest, MultipartFile 等)
                    if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse || arg instanceof MultipartFile) {
                        continue;
                    }
                    filteredArgs.add(arg);
                }
                
                if (!filteredArgs.isEmpty()) {
                    String paramsJson = JSONUtil.toJsonStr(filteredArgs.size() == 1 ? filteredArgs.get(0) : filteredArgs);
                    // 防止入参过大撑爆数据库
                    if (paramsJson.length() > 2000) {
                        paramsJson = paramsJson.substring(0, 2000) + "...";
                    }
                    auditLog.setParams(paramsJson);
                }
            }

            // 4. 将日志对象存入数据库
            adminAuditLogMapper.insert(auditLog);
            
        } catch (Exception exp) {
            log.error("==AOP 记录审计日志异常==", exp);
        }
    }

    private AuditLog getAnnotationLog(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method != null) {
            return method.getAnnotation(AuditLog.class);
        }
        return null;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip : "127.0.0.1";
    }
}