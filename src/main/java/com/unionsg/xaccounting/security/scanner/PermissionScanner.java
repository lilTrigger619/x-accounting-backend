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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Runs with an explicit order (rather than the implicit "runs after every @Order-annotated
 * runner" default) so {@code SettingsPermissionBackfillSeeder} can depend on every permission
 * this scanner discovers - including brand new ones like the Settings group - already existing
 * in the database by the time it runs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(5)
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