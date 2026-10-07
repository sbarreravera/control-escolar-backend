package com.graduacionesisamar.controlescolar.security.module;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class SchoolModuleAuthorizationInterceptor
        implements HandlerInterceptor {

    private final SchoolModuleAccessService accessService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        SchoolModule module = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(),
                SchoolModule.class
        );

        if (module == null) {
            module = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(),
                    SchoolModule.class
            );
        }

        if (module == null) {
            return true;
        }

        accessService.requireAccess(module.key());
        return true;
    }
}
