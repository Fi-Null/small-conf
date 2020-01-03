package com.small.config.admin.controller;

import com.small.config.admin.controller.annotation.PermissionLimit;
import com.small.config.admin.dao.SmallConfEnvDao;
import com.small.config.admin.dao.SmallConfProjectDao;
import com.small.config.admin.dao.SmallConfUserDao;
import com.small.config.admin.domain.SmallConfEnv;
import com.small.config.admin.domain.SmallConfProject;
import com.small.config.admin.domain.SmallConfUser;
import com.small.config.admin.result.ReturnT;
import com.small.config.admin.service.impl.LoginService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/3/20 10:30 AM
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Resource
    private SmallConfUserDao smallConfUserDao;
    @Resource
    private SmallConfProjectDao smallConfProjectDao;
    @Resource
    private SmallConfEnvDao smallConfEnvDao;

    @RequestMapping("")
    @PermissionLimit(adminuser = true)
    public String index(Model model) {

        List<SmallConfProject> projectList = smallConfProjectDao.findAll();
        model.addAttribute("projectList", projectList);

        List<SmallConfEnv> envList = smallConfEnvDao.findAll();
        model.addAttribute("envList", envList);

        return "user/user.index";
    }

    @RequestMapping("/pageList")
    @PermissionLimit(adminuser = true)
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String username,
                                        int permission) {

        // smallConfNode in mysql
        List<SmallConfUser> data = smallConfUserDao.pageList(start, length, username, permission);
        int list_count = smallConfUserDao.pageListCount(start, length, username, permission);

        // package result
        Map<String, Object> maps = new HashMap<>();
        maps.put("data", data);
        maps.put("recordsTotal", list_count);        // 总记录数
        maps.put("recordsFiltered", list_count);    // 过滤后的总记录数
        return maps;
    }

    /**
     * add
     *
     * @return
     */
    @RequestMapping("/add")
    @PermissionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> add(SmallConfUser SmallConfUser) {

        // valid
        if (StringUtils.isBlank(SmallConfUser.getUsername())) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "用户名不可为空");
        }
        if (StringUtils.isBlank(SmallConfUser.getPassword())) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码不可为空");
        }
        if (!(SmallConfUser.getPassword().length() >= 4 && SmallConfUser.getPassword().length() <= 100)) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码长度限制为4~50");
        }

        // passowrd md5
        String md5Password = DigestUtils.md5DigestAsHex(SmallConfUser.getPassword().getBytes());
        SmallConfUser.setPassword(md5Password);

        int ret = smallConfUserDao.add(SmallConfUser);
        return ret > 0 ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    /**
     * delete
     *
     * @return
     */
    @RequestMapping("/delete")
    @PermissionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> delete(HttpServletRequest request, String username) {

        SmallConfUser loginUser = (SmallConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
        if (loginUser.getUsername().equals(username)) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "禁止操作当前登录账号");
        }

        smallConfUserDao.delete(username);
        return ReturnT.SUCCESS;
    }

    /**
     * update
     *
     * @return
     */
    @RequestMapping("/update")
    @PermissionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> update(HttpServletRequest request, SmallConfUser SmallConfUser) {

        SmallConfUser loginUser = (SmallConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
        if (loginUser.getUsername().equals(SmallConfUser.getUsername())) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "禁止操作当前登录账号");
        }

        // valid
        if (StringUtils.isBlank(SmallConfUser.getUsername())) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "用户名不可为空");
        }

        SmallConfUser existUser = smallConfUserDao.load(SmallConfUser.getUsername());
        if (existUser == null) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "用户名非法");
        }

        if (StringUtils.isNotBlank(SmallConfUser.getPassword())) {
            if (!(SmallConfUser.getPassword().length() >= 4 && SmallConfUser.getPassword().length() <= 50)) {
                return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码长度限制为4~50");
            }
            // passowrd md5
            String md5Password = DigestUtils.md5DigestAsHex(SmallConfUser.getPassword().getBytes());
            existUser.setPassword(md5Password);
        }
        existUser.setPermission(SmallConfUser.getPermission());

        int ret = smallConfUserDao.update(existUser);
        return ret > 0 ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @RequestMapping("/updatePermissionData")
    @PermissionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> updatePermissionData(HttpServletRequest request,
                                                String username,
                                                @RequestParam(required = false) String[] permissionData) {

        SmallConfUser existUser = smallConfUserDao.load(username);
        if (existUser == null) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "参数非法");
        }

        String permissionDataArrStr = permissionData != null ? StringUtils.join(permissionData, ",") : "";
        existUser.setPermissionData(permissionDataArrStr);
        smallConfUserDao.update(existUser);

        return ReturnT.SUCCESS;
    }

    @RequestMapping("/updatePwd")
    @ResponseBody
    public ReturnT<String> updatePwd(HttpServletRequest request, String password) {

        // new password(md5)
        if (StringUtils.isBlank(password)) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码不可为空");
        }
        if (!(password.length() >= 4 && password.length() <= 100)) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码长度限制为4~50");
        }
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        // update pwd
        SmallConfUser loginUser = (SmallConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);

        SmallConfUser existUser = smallConfUserDao.load(loginUser.getUsername());
        existUser.setPassword(md5Password);
        smallConfUserDao.update(existUser);

        return ReturnT.SUCCESS;
    }

}
