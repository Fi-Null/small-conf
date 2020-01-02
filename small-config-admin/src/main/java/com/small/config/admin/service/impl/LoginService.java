package com.small.config.admin.service.impl;

import com.small.config.admin.dao.SmallConfUserDao;
import com.small.config.admin.domain.SmallConfUser;
import com.small.config.admin.result.ReturnT;
import com.small.config.admin.util.CookieUtil;
import com.small.config.admin.util.JacksonUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

/**
 * @ClassName LoginService
 * @Description TODO
 * @Author xiangke
 * @Date 2020/1/2 23:30
 * @Version 1.0
 **/
@Configuration
public class LoginService {

    public static final String LOGIN_IDENTITY = "SMALL_CONF_LOGIN_IDENTITY";

    @Resource
    private SmallConfUserDao smallConfUserDao;

    private String makeToken(SmallConfUser SmallConfUser) {
        String tokenJson = JacksonUtil.writeValueAsString(SmallConfUser);
        String tokenHex = new BigInteger(tokenJson.getBytes()).toString(16);
        return tokenHex;
    }

    private SmallConfUser parseToken(String tokenHex) {
        SmallConfUser SmallConfUser = null;
        if (tokenHex != null) {
            String tokenJson = new String(new BigInteger(tokenHex, 16).toByteArray());      // username_password(md5)
            SmallConfUser = JacksonUtil.readValue(tokenJson, SmallConfUser.class);
        }
        return SmallConfUser;
    }

    /**
     * login
     *
     * @param response
     * @param usernameParam
     * @param passwordParam
     * @param ifRemember
     * @return
     */
    public ReturnT<String> login(HttpServletResponse response, String usernameParam, String passwordParam, boolean ifRemember) {

        SmallConfUser SmallConfUser = smallConfUserDao.load(usernameParam);
        if (SmallConfUser == null) {
            return new ReturnT<String>(500, "账号或密码错误");
        }

        String passwordParamMd5 = DigestUtils.md5DigestAsHex(passwordParam.getBytes());
        if (!SmallConfUser.getPassword().equals(passwordParamMd5)) {
            return new ReturnT<String>(500, "账号或密码错误");
        }

        String loginToken = makeToken(SmallConfUser);

        // do login
        CookieUtil.set(response, LOGIN_IDENTITY, loginToken, ifRemember);
        return ReturnT.SUCCESS;
    }

    /**
     * logout
     *
     * @param request
     * @param response
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.remove(request, response, LOGIN_IDENTITY);
    }

    /**
     * logout
     *
     * @param request
     * @return
     */
    public SmallConfUser ifLogin(HttpServletRequest request) {
        String cookieToken = CookieUtil.getValue(request, LOGIN_IDENTITY);
        if (cookieToken != null) {
            SmallConfUser cookieUser = parseToken(cookieToken);
            if (cookieUser != null) {
                SmallConfUser dbUser = smallConfUserDao.load(cookieUser.getUsername());
                if (dbUser != null) {
                    if (cookieUser.getPassword().equals(dbUser.getPassword())) {
                        return dbUser;
                    }
                }
            }
        }
        return null;
    }

}
