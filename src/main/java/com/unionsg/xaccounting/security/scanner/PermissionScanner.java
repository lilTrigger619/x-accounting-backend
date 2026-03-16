package com.unionsg.xaccounting.security.scanner;

import com.unionsg.xaccounting.entity.User.Permission;
import com.unionsg.xaccounting.repository.PermissionRepository;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionScanner implements ApplicationRunner {

    private final ApplicationContext context;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(ApplicationArguments args) {

        log.info("Scanning application for permissions...");

        String[] beanNames = context.getBeanDefinitionNames();

        for (String beanName : beanNames) {

            Object bean = context.getBean(beanName);

            Class<?> targetClass = AopUtils.getTargetClass(bean);

            Method[] methods = targetClass.getDeclaredMethods();

            for (Method method : methods) {

                RequirePermission annotation =
                        AnnotationUtils.findAnnotation(method, RequirePermission.class);

                if (annotation != null) {

                    String permissionName = annotation.value();
                    String group = annotation.group();

                    boolean exists = permissionRepository
                            .existsByNameAndGuardName(permissionName, group);

                    if (!exists) {

                        Permission permission = new Permission();
                        permission.setName(permissionName);
                        permission.setGuardName(group);

                        permissionRepository.save(permission);

                        log.info(
                            "Created permission {} in group {}",
                            permissionName,
                            group
                        );
                    }
                }
            }
        }

        log.info("Permission scanning finished.");
    }
}