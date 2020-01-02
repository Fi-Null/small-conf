package com.small.config.admin.service;

import com.small.config.admin.domain.SmallConfNode;
import com.small.config.admin.domain.SmallConfUser;
import com.small.config.admin.result.ReturnT;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;

/**
 * @ClassName SmallConfNodeService
 * @Description TODO
 * @Author xiangke
 * @Date 2020/1/2 23:06
 * @Version 1.0
 **/
public interface SmallConfNodeService {

    boolean ifHasProjectPermission(SmallConfUser loginUser, String loginEnv, String appname);

    Map<String, Object> pageList(int offset,
                                 int pagesize,
                                 String appname,
                                 String key,
                                 SmallConfUser loginUser,
                                 String loginEnv);

    ReturnT<String> delete(String key, SmallConfUser loginUser, String loginEnv);

    ReturnT<String> add(SmallConfNode smallConfNode, SmallConfUser loginUser, String loginEnv);

    ReturnT<String> update(SmallConfNode smallConfNode, SmallConfUser loginUser, String loginEnv);


    //------------------ rest api ----------------------

    ReturnT<Map<String, String>> find(String accessToken, String env, List<String> keys);

    DeferredResult<ReturnT<String>> monitor(String accessToken, String env, List<String> keys);

}
