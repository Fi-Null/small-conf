package com.small.config.admin.controller;

import com.small.config.admin.controller.annotation.PermissionLimit;
import com.small.config.admin.dao.SmallConfProjectDao;
import com.small.config.admin.domain.SmallConfNode;
import com.small.config.admin.domain.SmallConfProject;
import com.small.config.admin.domain.SmallConfUser;
import com.small.config.admin.result.ReturnT;
import com.small.config.admin.service.SmallConfNodeService;
import com.small.config.admin.service.impl.LoginService;
import com.small.config.admin.util.JacksonUtil;
import com.small.config.core.model.SmallConfParamVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static com.small.config.admin.controller.interceptor.EnvInterceptor.CURRENT_ENV;


/**
 * @ClassName SmallConfController
 * @Description TODO
 * @Author xiangke
 * @Date 2020/1/2 23:37
 * @Version 1.0
 **/
@Controller
@RequestMapping("/conf")
public class SmallConfController {

    @Resource
    private SmallConfProjectDao smallConfProjectDao;
    @Resource
    private SmallConfNodeService smallConfNodeService;

    @RequestMapping("")
    public String index(HttpServletRequest request, Model model, String appname) {

        List<SmallConfProject> list = smallConfProjectDao.findAll();
        if (list == null || list.size() == 0) {
            throw new RuntimeException("系统异常，无可用项目");
        }

        SmallConfProject project = list.get(0);
        for (SmallConfProject item : list) {
            if (item.getAppname().equals(appname)) {
                project = item;
            }
        }

        boolean ifHasProjectPermission = smallConfNodeService.ifHasProjectPermission(
                (SmallConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY),
                (String) request.getAttribute(CURRENT_ENV),
                project.getAppname());

        model.addAttribute("ProjectList", list);
        model.addAttribute("project", project);
        model.addAttribute("ifHasProjectPermission", ifHasProjectPermission);

        return "conf/conf.index";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(HttpServletRequest request,
                                        @RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String appname,
                                        String key) {

        SmallConfUser SmallConfUser = (SmallConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
        String loginEnv = (String) request.getAttribute(CURRENT_ENV);

        return smallConfNodeService.pageList(start, length, appname, key, SmallConfUser, loginEnv);
    }

    /**
     * get
     *
     * @return
     */
    @RequestMapping("/delete")
    @ResponseBody
    public ReturnT<String> delete(HttpServletRequest request, String key) {

        SmallConfUser SmallConfUser = (SmallConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
        String loginEnv = (String) request.getAttribute(CURRENT_ENV);

        return smallConfNodeService.delete(key, SmallConfUser, loginEnv);
    }

    /**
     * create/update
     *
     * @return
     */
    @RequestMapping("/add")
    @ResponseBody
    public ReturnT<String> add(HttpServletRequest request, SmallConfNode SmallConfNode) {

        SmallConfUser SmallConfUser = (SmallConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
        String loginEnv = (String) request.getAttribute(CURRENT_ENV);

        // fill env
        SmallConfNode.setEnv(loginEnv);

        return smallConfNodeService.add(SmallConfNode, SmallConfUser, loginEnv);
    }

    /**
     * create/update
     *
     * @return
     */
    @RequestMapping("/update")
    @ResponseBody
    public ReturnT<String> update(HttpServletRequest request, SmallConfNode smallConfNode) {

        SmallConfUser SmallConfUser = (SmallConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
        String loginEnv = (String) request.getAttribute(CURRENT_ENV);

        // fill env
        smallConfNode.setEnv(loginEnv);

        return smallConfNodeService.update(smallConfNode, SmallConfUser, loginEnv);
    }


    // ---------------------- rest api ----------------------

    @Value("${small.conf.access.token}")
    private String accessToken;


    /**
     * 配置查询 API
     * <p>
     * 说明：查询配置数据；
     * <p>
     * ------
     * 地址格式：{配置中心跟地址}/find
     * <p>
     * 请求参数说明：
     * 1、accessToken：请求令牌；
     * 2、env：环境标识
     * 3、keys：配置Key列表
     * <p>
     * 请求数据格式如下，放置在 RequestBody 中，JSON格式：
     * <p>
     * {
     * "accessToken" : "xx",
     * "env" : "xx",
     * "keys" : [
     * "key01",
     * "key02"
     * ]
     * }
     *
     * @param data
     * @return
     */
    @RequestMapping("/find")
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<Map<String, String>> find(@RequestBody(required = false) String data) {

        // parse data
        SmallConfParamVO confParamVO = null;
        try {
            confParamVO = JacksonUtil.readValue(data, SmallConfParamVO.class);
        } catch (Exception e) {
        }

        // parse param
        String accessToken = null;
        String env = null;
        List<String> keys = null;
        if (confParamVO != null) {
            accessToken = confParamVO.getAccessToken();
            env = confParamVO.getEnv();
            keys = confParamVO.getKeys();
        }

        return smallConfNodeService.find(accessToken, env, keys);
    }

    /**
     * 配置监控 API
     * <p>
     * 说明：long-polling 接口，主动阻塞一段时间（默认30s）；直至阻塞超时或配置信息变动时响应；
     * <p>
     * ------
     * 地址格式：{配置中心跟地址}/monitor
     * <p>
     * 请求参数说明：
     * 1、accessToken：请求令牌；
     * 2、env：环境标识
     * 3、keys：配置Key列表
     * <p>
     * 请求数据格式如下，放置在 RequestBody 中，JSON格式：
     * <p>
     * {
     * "accessToken" : "xx",
     * "env" : "xx",
     * "keys" : [
     * "key01",
     * "key02"
     * ]
     * }
     *
     * @param data
     * @return
     */
    @RequestMapping("/monitor")
    @ResponseBody
    @PermissionLimit(limit = false)
    public DeferredResult<ReturnT<String>> monitor(@RequestBody(required = false) String data) {

        // parse data
        SmallConfParamVO confParamVO = null;
        try {
            confParamVO = JacksonUtil.readValue(data, SmallConfParamVO.class);
        } catch (Exception e) {
        }

        // parse param
        String accessToken = null;
        String env = null;
        List<String> keys = null;
        if (confParamVO != null) {
            accessToken = confParamVO.getAccessToken();
            env = confParamVO.getEnv();
            keys = confParamVO.getKeys();
        }

        return smallConfNodeService.monitor(accessToken, env, keys);
    }

}
