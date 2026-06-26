package com.example.SmartLearningPlatformBackend.integration;

import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class TestAuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {

    private final UserDetailsImpl principal;

    TestAuthenticationPrincipalResolver(UserDetailsImpl principal) {
        this.principal = principal;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                && UserDetailsImpl.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        return principal;
    }
}
