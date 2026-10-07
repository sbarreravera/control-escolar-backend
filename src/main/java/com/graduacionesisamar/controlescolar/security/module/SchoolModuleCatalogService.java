package com.graduacionesisamar.controlescolar.security.module;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Service;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Discovers assignable school modules from controller annotations.
 */
@Service
@RequiredArgsConstructor
public class SchoolModuleCatalogService {

    private final ListableBeanFactory beanFactory;

    public List<SchoolModuleDescriptor> findAll() {
        Map<String, SchoolModuleDescriptor> modules = new LinkedHashMap<>();

        for (String beanName :
                beanFactory.getBeanNamesForAnnotation(RestController.class)) {
            Class<?> beanType = beanFactory.getType(beanName);

            if (beanType == null) {
                continue;
            }

            register(
                    modules,
                    AnnotatedElementUtils.findMergedAnnotation(
                            beanType,
                            SchoolModule.class
                    )
            );

            for (Method method :
                    ReflectionUtils.getUniqueDeclaredMethods(beanType)) {
                register(
                        modules,
                        AnnotatedElementUtils.findMergedAnnotation(
                                method,
                                SchoolModule.class
                        )
                );
            }
        }

        return modules.values()
                .stream()
                .sorted(
                        Comparator.comparingInt(SchoolModuleDescriptor::order)
                                .thenComparing(SchoolModuleDescriptor::name)
                                .thenComparing(SchoolModuleDescriptor::key)
                )
                .toList();
    }

    public Set<String> findAllKeys() {
        return findAll()
                .stream()
                .map(SchoolModuleDescriptor::key)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void register(
            Map<String, SchoolModuleDescriptor> modules,
            SchoolModule annotation
    ) {
        if (annotation == null) {
            return;
        }

        SchoolModuleDescriptor descriptor = new SchoolModuleDescriptor(
                annotation.key().trim().toUpperCase(),
                annotation.name().trim(),
                annotation.description().trim(),
                annotation.defaultGranted(),
                annotation.order()
        );

        SchoolModuleDescriptor existing = modules.putIfAbsent(
                descriptor.key(),
                descriptor
        );

        if (existing != null && !existing.equals(descriptor)) {
            throw new IllegalStateException(
                    "School module metadata mismatch for key "
                            + descriptor.key()
            );
        }
    }
}
