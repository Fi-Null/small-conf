package com.small.config.admin.controller;

import com.small.config.admin.controller.annotation.PermissionLimit;
import com.small.config.admin.dao.SmallConfEnvDao;
import com.small.config.admin.dao.SmallConfNodeDao;
import com.small.config.admin.domain.SmallConfEnv;
import com.small.config.admin.result.ReturnT;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/3/20 10:10 AM
 */
@Controller
@RequestMapping("/env")
public class EnvController {

    @Resource
    private SmallConfEnvDao smallConfEnvDao;
    @Resource
    private SmallConfNodeDao smallConfNodeDao;

    public EnvController() {
    }


    @RequestMapping
    @PermissionLimit(adminuser = true)
    public String index(Model model) {

        List<SmallConfEnv> list = smallConfEnvDao.findAll();
        model.addAttribute("list", list);

        return "env/env.index";
    }

    @RequestMapping("/save")
    @PermissionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> save(SmallConfEnv smallConfEnv) {

        // valid
        if (StringUtils.isBlank(smallConfEnv.getEnv())) {
            return new ReturnT<String>(500, "Env不可为空");
        }
        if (smallConfEnv.getEnv().length() < 3 || smallConfEnv.getEnv().length() > 50) {
            return new ReturnT<String>(500, "Env长度限制为4~50");
        }
        if (StringUtils.isBlank(smallConfEnv.getTitle())) {
            return new ReturnT<String>(500, "请输入Env名称");
        }

        // valid repeat
        SmallConfEnv existEnv = smallConfEnvDao.load(smallConfEnv.getEnv());
        if (existEnv != null) {
            return new ReturnT<String>(500, "Env已存在，请勿重复添加");
        }

        int ret = smallConfEnvDao.save(smallConfEnv);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @RequestMapping("/update")
    @PermissionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> update(SmallConfEnv smallConfEnv) {

        // valid
        if (StringUtils.isBlank(smallConfEnv.getEnv())) {
            return new ReturnT<String>(500, "Env不可为空");
        }
        if (StringUtils.isBlank(smallConfEnv.getTitle())) {
            return new ReturnT<String>(500, "请输入Env名称");
        }

        int ret = smallConfEnvDao.update(smallConfEnv);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @RequestMapping("/remove")
    @PermissionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> remove(String env) {

        if (StringUtils.isBlank(env)) {
            return new ReturnT<String>(500, "参数Env非法");
        }

        // valid
        int list_count = smallConfNodeDao.pageListCount(0, 10, env, null, null);
        if (list_count > 0) {
            return new ReturnT<String>(500, "拒绝删除，该Env下存在配置数据");
        }

        // valid can not be empty
        List<SmallConfEnv> allList = smallConfEnvDao.findAll();
        if (allList.size() == 1) {
            return new ReturnT<String>(500, "拒绝删除, 需要至少预留一个Env");
        }

        int ret = smallConfEnvDao.delete(env);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }


}
