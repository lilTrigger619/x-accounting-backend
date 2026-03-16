package com.unionsg.xaccounting.security.interceptor;

import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.security.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import com.unionsg.xaccounting.exception.AccessDeniedException;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionInterceptor {

    private final PermissionService permissionService;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(
            ProceedingJoinPoint joinPoint,
            RequirePermission requirePermission
    ) throws Throwable {

        String permission = requirePermission.value();

        if (!permissionService.currentUserHasPermission(permission)) {
            throw new AccessDeniedException(permission);
        }

        return joinPoint.proceed();
    }
}
