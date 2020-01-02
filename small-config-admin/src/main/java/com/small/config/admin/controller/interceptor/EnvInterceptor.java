package com.small.config.admin.controller.interceptor;

import com.small.config.admin.dao.SmallConfEnvDao;
import com.small.config.admin.domain.SmallConfEnv;
import com.small.config.admin.util.CookieUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @ClassName EnvInterceptor
 * @Description TODO
 * @Author xiangke
 * @Date 2020/1/2 23:45
 * @Version 1.0
 **/
@Component
public class EnvInterceptor extends HandlerInterceptorAdapter {

    public static final String CURRENT_ENV = "SMALL_CONF_CURRENT_ENV";

    @Resource
    private SmallConfEnvDao smallConfEnvDao;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // env list
        List<SmallConfEnv> envList = smallConfEnvDao.findAll();
        if (envList == null || envList.size() == 0) {
            throw new RuntimeException("系统异常，获取Env数据失败");
        }

        // current env
        String currentEnv = envList.get(0).getEnv();
        String currentEnvCookie = CookieUtil.getValue(request, CURRENT_ENV);
        if (currentEnvCookie != null && currentEnvCookie.trim().length() > 0) {
            for (SmallConfEnv envItem : envList) {
                if (currentEnvCookie.equals(envItem.getEnv())) {
                    currentEnv = envItem.getEnv();
                }
            }
        }

        request.setAttribute("envList", envList);
        request.setAttribute(CURRENT_ENV, currentEnv);

        return super.preHandle(request, response, handler);
    }

}
