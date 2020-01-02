package com.small.config.admin.controller.interceptor;

import com.small.config.admin.controller.annotation.PermissionLimit;
import com.small.config.admin.domain.SmallConfUser;
import com.small.config.admin.service.impl.LoginService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @ClassName PermissionInterceptor
 * @Description TODO
 * @Author xiangke
 * @Date 2020/1/1 23:28
 * @Version 1.0
 **/
@Component
public class PermissionInterceptor extends HandlerInterceptorAdapter {

    @Resource
    private LoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return super.preHandle(request, response, handler);
        }

        // if need login
        boolean needLogin = true;
        boolean needAdminuser = false;
        HandlerMethod method = (HandlerMethod) handler;
        PermissionLimit permission = method.getMethodAnnotation(PermissionLimit.class);
        if (permission != null) {
            needLogin = permission.limit();
            needAdminuser = permission.adminuser();
        }

        if (needLogin) {
            SmallConfUser loginUser = loginService.ifLogin(request);
            if (loginUser == null) {
                response.sendRedirect(request.getContextPath() + "/toLogin");
                return false;
            }
            if (needAdminuser && loginUser.getPermission() != 1) {
                throw new RuntimeException("权限拦截");
            }
            request.setAttribute(LoginService.LOGIN_IDENTITY, loginUser);
        }

        return super.preHandle(request, response, handler);
    }

}
