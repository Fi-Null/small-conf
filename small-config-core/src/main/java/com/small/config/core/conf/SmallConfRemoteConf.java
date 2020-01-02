package com.small.config.core.conf;

import com.small.config.core.model.SmallConfParamVO;
import com.small.config.util.HttpUtil;
import com.small.config.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 7:42 PM
 */
public class SmallConfRemoteConf {

    private static Logger logger = LoggerFactory.getLogger(SmallConfRemoteConf.class);


    private static String adminAddress;
    private static String env;
    private static String accessToken;

    private static List<String> adminAddressArr = null;

    public static void init(String adminAddress, String env, String accessToken) {

        // valid
        if (adminAddress == null || adminAddress.trim().length() == 0) {
            throw new RuntimeException("small-conf adminAddress can not be empty");
        }
        if (env == null || env.trim().length() == 0) {
            throw new RuntimeException("small-conf env can not be empty");
        }


        SmallConfRemoteConf.adminAddress = adminAddress;
        SmallConfRemoteConf.env = env;
        SmallConfRemoteConf.accessToken = accessToken;


        // parse
        SmallConfRemoteConf.adminAddressArr = new ArrayList<>();
        if (adminAddress.contains(",")) {
            SmallConfRemoteConf.adminAddressArr.add(adminAddress);
        } else {
            SmallConfRemoteConf.adminAddressArr.addAll(Arrays.asList(adminAddress.split(",")));
        }

    }


    // ---------------------- rest api ----------------------

    /**
     * get and valid
     *
     * @param url
     * @param requestBody
     * @param timeout
     * @return
     */
    private static Map<String, Object> getAndValid(String url, String requestBody, int timeout) {

        // resp json
        String respJson = HttpUtil.postBody(url, requestBody, timeout);
        if (respJson == null) {
            return null;
        }


        // parse obj
        Map<String, Object> respObj = JsonUtil.parseMap(respJson);
        int code = Integer.valueOf(String.valueOf(respObj.get("code")));
        if (code != 200) {
            logger.info("request fail, msg={}", (respObj.containsKey("msg") ? respObj.get("msg") : respJson));
            return null;
        }
        return respObj;
    }


    /**
     * find
     *
     * @param keys
     * @return
     */
    public static Map<String, String> find(Set<String> keys) {
        for (String adminAddressUrl : SmallConfRemoteConf.adminAddressArr) {

            // url + param
            String url = adminAddressUrl + "/conf/find";

            SmallConfParamVO paramVO = new SmallConfParamVO();
            paramVO.setAccessToken(accessToken);
            paramVO.setEnv(env);
            paramVO.setKeys(new ArrayList<String>(keys));

            String paramsJson = JsonUtil.toJson(paramVO);

            // get and valid
            Map<String, Object> respObj = getAndValid(url, paramsJson, 5);

            // parse
            if (respObj != null && respObj.containsKey("data")) {
                Map<String, String> data = (Map<String, String>) respObj.get("data");
                return data;
            }
        }

        return null;
    }

    public static String find(String key) {
        Map<String, String> result = find(new HashSet<String>(Arrays.asList(key)));
        if (result != null) {
            return result.get(key);
        }
        return null;
    }


    /**
     * monitor
     *
     * @param keys
     * @return
     */
    public static boolean monitor(Set<String> keys) {

        for (String adminAddressUrl : SmallConfRemoteConf.adminAddressArr) {

            // url + param
            String url = adminAddressUrl + "/conf/monitor";

            SmallConfParamVO paramVO = new SmallConfParamVO();
            paramVO.setAccessToken(accessToken);
            paramVO.setEnv(env);
            paramVO.setKeys(new ArrayList<String>(keys));

            String paramsJson = JsonUtil.toJson(paramVO);

            // get and valid
            Map<String, Object> respObj = getAndValid(url, paramsJson, 60);

            return respObj != null ? true : false;
        }
        return false;
    }

}
