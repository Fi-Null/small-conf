package com.small.config.admin.controller;

import com.small.config.admin.controller.annotation.PermissionLimit;
import com.small.config.admin.dao.SmallConfNodeDao;
import com.small.config.admin.dao.SmallConfProjectDao;
import com.small.config.admin.domain.SmallConfProject;
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
 * @createDate 1/3/20 10:17 AM
 */
@Controller
@RequestMapping("/project")
public class ProjectController {

    @Resource
    private SmallConfProjectDao smallConfProjectDao;
    @Resource
    private SmallConfNodeDao smallConfNodeDao;

    @RequestMapping
    @PermissionLimit(adminuser = true)
    public String index(Model model) {

        List<SmallConfProject> list = smallConfProjectDao.findAll();
        model.addAttribute("list", list);

        return "project/project.index";
    }

    @RequestMapping("/save")
    @PermissionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> save(SmallConfProject smallConfProject) {

        // valid
        if (StringUtils.isBlank(smallConfProject.getAppname())) {
            return new ReturnT<String>(500, "AppName不可为空");
        }
        if (smallConfProject.getAppname().length() < 4 || smallConfProject.getAppname().length() > 100) {
            return new ReturnT<String>(500, "Appname长度限制为4~100");
        }
        if (StringUtils.isBlank(smallConfProject.getTitle())) {
            return new ReturnT<String>(500, "请输入项目名称");
        }

        // valid repeat
        SmallConfProject existProject = smallConfProjectDao.load(smallConfProject.getAppname());
        if (existProject != null) {
            return new ReturnT<String>(500, "Appname已存在，请勿重复添加");
        }

        int ret = smallConfProjectDao.save(smallConfProject);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @RequestMapping("/update")
    @PermissionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> update(SmallConfProject SmallConfProject) {

        // valid
        if (StringUtils.isBlank(SmallConfProject.getAppname())) {
            return new ReturnT<String>(500, "AppName不可为空");
        }
        if (StringUtils.isBlank(SmallConfProject.getTitle())) {
            return new ReturnT<String>(500, "请输入项目名称");
        }

        int ret = smallConfProjectDao.update(SmallConfProject);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @RequestMapping("/remove")
    @PermissionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> remove(String appname) {

        if (StringUtils.isBlank(appname)) {
            return new ReturnT<String>(500, "参数AppName非法");
        }

        // valid
        int list_count = smallConfNodeDao.pageListCount(0, 10, null, appname, null);
        if (list_count > 0) {
            return new ReturnT<String>(500, "拒绝删除，该项目下存在配置数据");
        }

        List<SmallConfProject> allList = smallConfProjectDao.findAll();
        if (allList.size() == 1) {
            return new ReturnT<String>(500, "拒绝删除, 需要至少预留一个项目");
        }

        int ret = smallConfProjectDao.delete(appname);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

}