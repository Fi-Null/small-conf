package com.small.config.admin.service.impl;

import com.small.config.admin.dao.*;
import com.small.config.admin.domain.*;
import com.small.config.admin.result.ReturnT;
import com.small.config.admin.service.SmallConfNodeService;
import com.small.config.admin.util.RegexUtil;
import com.small.config.util.PropUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName SmallConfNodeServiceImpl
 * @Description TODO
 * @Author xiangke
 * @Date 2020/1/2 23:12
 * @Version 1.0
 **/
@Service
public class SmallConfNodeServiceImpl implements SmallConfNodeService, InitializingBean, DisposableBean {

    private static Logger logger = LoggerFactory.getLogger(SmallConfNodeServiceImpl.class);


    @Resource
    private SmallConfNodeDao smallConfNodeDao;
    @Resource
    private SmallConfProjectDao smallConfProjectDao;
    @Resource
    private SmallConfNodeLogDao smallConfNodeLogDao;
    @Resource
    private SmallConfEnvDao smallConfEnvDao;
    @Resource
    private SmallConfNodeMsgDao smallConfNodeMsgDao;


    @Value("${small.conf.confdata.filepath}")
    private String confDataFilePath;
    @Value("${small.conf.access.token}")
    private String accessToken;

    private int confBeatTime = 30;


    @Override
    public boolean ifHasProjectPermission(SmallConfUser loginUser, String loginEnv, String appname) {
        if (loginUser.getPermission() == 1) {
            return true;
        }
        if (ArrayUtils.contains(StringUtils.split(loginUser.getPermissionData(), ","), (appname.concat("#").concat(loginEnv)))) {
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Object> pageList(int offset,
                                        int pagesize,
                                        String appname,
                                        String key,
                                        SmallConfUser loginUser,
                                        String loginEnv) {

        // project permission
        if (StringUtils.isBlank(loginEnv) || StringUtils.isBlank(appname) || !ifHasProjectPermission(loginUser, loginEnv, appname)) {
            //return new ReturnT<String>(500, "您没有该项目的配置权限,请联系管理员开通");
            Map<String, Object> emptyMap = new HashMap<String, Object>();
            emptyMap.put("data", new ArrayList<>());
            emptyMap.put("recordsTotal", 0);
            emptyMap.put("recordsFiltered", 0);
            return emptyMap;
        }

        // SmallConfNode in mysql
        List<SmallConfNode> data = smallConfNodeDao.pageList(offset, pagesize, loginEnv, appname, key);
        int list_count = smallConfNodeDao.pageListCount(offset, pagesize, loginEnv, appname, key);

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("data", data);
        maps.put("recordsTotal", list_count);        // 总记录数
        maps.put("recordsFiltered", list_count);    // 过滤后的总记录数
        return maps;

    }

    @Override
    public ReturnT<String> delete(String key, SmallConfUser loginUser, String loginEnv) {
        if (StringUtils.isBlank(key)) {
            return new ReturnT<String>(500, "参数缺失");
        }
        SmallConfNode existNode = smallConfNodeDao.load(loginEnv, key);
        if (existNode == null) {
            return new ReturnT<String>(500, "参数非法");
        }

        // project permission
        if (!ifHasProjectPermission(loginUser, loginEnv, existNode.getAppname())) {
            return new ReturnT<String>(500, "您没有该项目的配置权限,请联系管理员开通");
        }

        smallConfNodeDao.delete(loginEnv, key);
        smallConfNodeLogDao.deleteTimeout(loginEnv, key, 0);

        // conf msg
        sendConfMsg(loginEnv, key, null);

        return ReturnT.SUCCESS;
    }

    // conf broadcast msg
    private void sendConfMsg(String env, String key, String value) {

        SmallConfNodeMsg confNodeMsg = new SmallConfNodeMsg();
        confNodeMsg.setEnv(env);
        confNodeMsg.setKey(key);
        confNodeMsg.setValue(value);

        smallConfNodeMsgDao.add(confNodeMsg);
    }

    @Override
    public ReturnT<String> add(SmallConfNode SmallConfNode, SmallConfUser loginUser, String loginEnv) {

        // valid
        if (StringUtils.isBlank(SmallConfNode.getAppname())) {
            return new ReturnT<String>(500, "AppName不可为空");
        }

        // project permission
        if (!ifHasProjectPermission(loginUser, loginEnv, SmallConfNode.getAppname())) {
            return new ReturnT<String>(500, "您没有该项目的配置权限,请联系管理员开通");
        }

        // valid group
        SmallConfProject group = smallConfProjectDao.load(SmallConfNode.getAppname());
        if (group == null) {
            return new ReturnT<String>(500, "AppName非法");
        }

        // valid env
        if (StringUtils.isBlank(SmallConfNode.getEnv())) {
            return new ReturnT<String>(500, "配置Env不可为空");
        }
        SmallConfEnv smallConfEnv = smallConfEnvDao.load(SmallConfNode.getEnv());
        if (smallConfEnv == null) {
            return new ReturnT<String>(500, "配置Env非法");
        }

        // valid key
        if (StringUtils.isBlank(SmallConfNode.getKey())) {
            return new ReturnT<String>(500, "配置Key不可为空");
        }
        SmallConfNode.setKey(SmallConfNode.getKey().trim());

        SmallConfNode existNode = smallConfNodeDao.load(SmallConfNode.getEnv(), SmallConfNode.getKey());
        if (existNode != null) {
            return new ReturnT<String>(500, "配置Key已存在，不可重复添加");
        }
        if (!SmallConfNode.getKey().startsWith(SmallConfNode.getAppname())) {
            return new ReturnT<String>(500, "配置Key格式非法");
        }

        // valid title
        if (StringUtils.isBlank(SmallConfNode.getTitle())) {
            return new ReturnT<String>(500, "配置描述不可为空");
        }

        // value force null to ""
        if (SmallConfNode.getValue() == null) {
            SmallConfNode.setValue("");
        }

        smallConfNodeDao.insert(SmallConfNode);

        // node log
        SmallConfNodeLog nodeLog = new SmallConfNodeLog();
        nodeLog.setEnv(SmallConfNode.getEnv());
        nodeLog.setKey(SmallConfNode.getKey());
        nodeLog.setTitle(SmallConfNode.getTitle() + "(配置新增)");
        nodeLog.setValue(SmallConfNode.getValue());
        nodeLog.setOptuser(loginUser.getUsername());
        smallConfNodeLogDao.add(nodeLog);

        // conf msg
        sendConfMsg(SmallConfNode.getEnv(), SmallConfNode.getKey(), SmallConfNode.getValue());

        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> update(SmallConfNode SmallConfNode, SmallConfUser loginUser, String loginEnv) {

        // valid
        if (StringUtils.isBlank(SmallConfNode.getKey())) {
            return new ReturnT<String>(500, "配置Key不可为空");
        }
        SmallConfNode existNode = smallConfNodeDao.load(SmallConfNode.getEnv(), SmallConfNode.getKey());
        if (existNode == null) {
            return new ReturnT<String>(500, "配置Key非法");
        }

        // project permission
        if (!ifHasProjectPermission(loginUser, loginEnv, existNode.getAppname())) {
            return new ReturnT<String>(500, "您没有该项目的配置权限,请联系管理员开通");
        }

        if (StringUtils.isBlank(SmallConfNode.getTitle())) {
            return new ReturnT<String>(500, "配置描述不可为空");
        }

        // value force null to ""
        if (SmallConfNode.getValue() == null) {
            SmallConfNode.setValue("");
        }

        existNode.setTitle(SmallConfNode.getTitle());
        existNode.setValue(SmallConfNode.getValue());
        int ret = smallConfNodeDao.update(existNode);
        if (ret < 1) {
            return ReturnT.FAIL;
        }

        // node log
        SmallConfNodeLog nodeLog = new SmallConfNodeLog();
        nodeLog.setEnv(existNode.getEnv());
        nodeLog.setKey(existNode.getKey());
        nodeLog.setTitle(existNode.getTitle() + "(配置更新)");
        nodeLog.setValue(existNode.getValue());
        nodeLog.setOptuser(loginUser.getUsername());
        smallConfNodeLogDao.add(nodeLog);
        smallConfNodeLogDao.deleteTimeout(existNode.getEnv(), existNode.getKey(), 10);

        // conf msg
        sendConfMsg(SmallConfNode.getEnv(), SmallConfNode.getKey(), SmallConfNode.getValue());

        return ReturnT.SUCCESS;
    }


    // ---------------------- rest api ----------------------

    @Override
    public ReturnT<Map<String, String>> find(String accessToken, String env, List<String> keys) {

        // valid
        if (this.accessToken != null && this.accessToken.trim().length() > 0 && !this.accessToken.equals(accessToken)) {
            return new ReturnT<Map<String, String>>(ReturnT.FAIL.getCode(), "AccessToken Invalid.");
        }
        if (env == null || env.trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), "env Invalid.");
        }
        if (keys == null || keys.size() == 0) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), "keys Invalid.");
        }

        // result
        Map<String, String> result = new HashMap<String, String>();
        for (String key : keys) {

            // get val
            String value = null;
            if (key == null || key.trim().length() < 4 || key.trim().length() > 100
                    || !RegexUtil.matches(RegexUtil.abc_number_line_point_pattern, key)) {
                // invalid key, pass
            } else {
                value = getFileConfData(env, key);
            }

            // parse null
            if (value == null) {
                value = "";
            }

            // put
            result.put(key, value);
        }

        return new ReturnT<Map<String, String>>(result);
    }

    @Override
    public DeferredResult<ReturnT<String>> monitor(String accessToken, String env, List<String> keys) {

        // init
        DeferredResult deferredResult = new DeferredResult(confBeatTime * 1000L, new ReturnT<>(ReturnT.SUCCESS_CODE, "Monitor timeout, no key updated."));

        // valid
        if (this.accessToken != null && this.accessToken.trim().length() > 0 && !this.accessToken.equals(accessToken)) {
            deferredResult.setResult(new ReturnT<>(ReturnT.FAIL.getCode(), "AccessToken Invalid."));
            return deferredResult;
        }
        if (env == null || env.trim().length() == 0) {
            deferredResult.setResult(new ReturnT<>(ReturnT.FAIL.getCode(), "env Invalid."));
            return deferredResult;
        }
        if (keys == null || keys.size() == 0) {
            deferredResult.setResult(new ReturnT<>(ReturnT.FAIL.getCode(), "keys Invalid."));
            return deferredResult;
        }

        // monitor by client
        for (String key : keys) {


            // invalid key, pass
            if (key == null || key.trim().length() < 4 || key.trim().length() > 100
                    || !RegexUtil.matches(RegexUtil.abc_number_line_point_pattern, key)) {
                continue;
            }

            // monitor each key
            String fileName = parseConfDataFileName(env, key);

            List<DeferredResult> deferredResultList = confDeferredResultMap.get(fileName);
            if (deferredResultList == null) {
                deferredResultList = new ArrayList<>();
                confDeferredResultMap.put(fileName, deferredResultList);
            }

            deferredResultList.add(deferredResult);
        }

        return deferredResult;
    }


    // ---------------------- start stop ----------------------

    @Override
    public void afterPropertiesSet() throws Exception {
        startThead();
    }

    @Override
    public void destroy() throws Exception {
        stopThread();
    }


    // ---------------------- thread ----------------------

    private ExecutorService executorService = Executors.newCachedThreadPool();
    private volatile boolean executorStoped = false;

    private volatile List<Integer> readedMessageIds = Collections.synchronizedList(new ArrayList<Integer>());

    private Map<String, List<DeferredResult>> confDeferredResultMap = new ConcurrentHashMap<>();

    public void startThead() throws Exception {

        /**
         * brocast conf-data msg, sync to file, for "add、update、delete"
         */
        executorService.execute(() -> {
            while (!executorStoped) {
                try {
                    // new message, filter readed
                    List<SmallConfNodeMsg> messageList = smallConfNodeMsgDao.findMsg(readedMessageIds);
                    if (messageList != null && messageList.size() > 0) {
                        for (SmallConfNodeMsg message : messageList) {
                            readedMessageIds.add(message.getId());


                            // sync file
                            setFileConfData(message.getEnv(), message.getKey(), message.getValue());
                        }
                    }

                    // clean old message;
                    if ((System.currentTimeMillis() / 1000) % confBeatTime == 0) {
                        smallConfNodeMsgDao.cleanMessage(confBeatTime);
                        readedMessageIds.clear();
                    }
                } catch (Exception e) {
                    if (!executorStoped) {
                        logger.error(e.getMessage(), e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                    if (!executorStoped) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });


        /**
         *  sync total conf-data, db + file      (1+N/30s)
         *
         *  clean deleted conf-data file
         */
        executorService.execute(() -> {
            while (!executorStoped) {

                // align to beattime
                try {
                    long sleepSecond = confBeatTime - (System.currentTimeMillis() / 1000) % confBeatTime;
                    if (sleepSecond > 0 && sleepSecond < confBeatTime) {
                        TimeUnit.SECONDS.sleep(sleepSecond);
                    }
                } catch (Exception e) {
                    if (!executorStoped) {
                        logger.error(e.getMessage(), e);
                    }
                }

                try {

                    // sync registry-data, db + file
                    int offset = 0;
                    int pagesize = 1000;
                    List<String> confDataFileList = new ArrayList<>();

                    List<SmallConfNode> confNodeList = smallConfNodeDao.pageList(offset, pagesize, null, null, null);
                    while (confNodeList != null && confNodeList.size() > 0) {

                        for (SmallConfNode confNoteItem : confNodeList) {

                            // sync file
                            String confDataFile = setFileConfData(confNoteItem.getEnv(), confNoteItem.getKey(), confNoteItem.getValue());

                            // collect confDataFile
                            confDataFileList.add(confDataFile);
                        }


                        offset += 1000;
                        confNodeList = smallConfNodeDao.pageList(offset, pagesize, null, null, null);
                    }

                    // clean old registry-data file
                    cleanFileConfData(confDataFileList);

                    logger.debug(">>>>>>>>>>> small-conf, sync totel conf data success, sync conf count = {}", confDataFileList.size());
                } catch (Exception e) {
                    if (!executorStoped) {
                        logger.error(e.getMessage(), e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(confBeatTime);
                } catch (Exception e) {
                    if (!executorStoped) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });


    }

    private void stopThread() {
        executorStoped = true;
        executorService.shutdownNow();
    }


    // ---------------------- file opt ----------------------

    // get
    public String getFileConfData(String env, String key) {

        // fileName
        String confFileName = parseConfDataFileName(env, key);

        // read
        Properties existProp = PropUtil.loadFileProp(confFileName);
        if (existProp != null && existProp.containsKey("value")) {
            return existProp.getProperty("value");
        }
        return null;
    }

    private String parseConfDataFileName(String env, String key) {
        // fileName
        String fileName = confDataFilePath
                .concat(File.separator).concat(env)
                .concat(File.separator).concat(key)
                .concat(".properties");
        return fileName;
    }

    // set
    private String setFileConfData(String env, String key, String value) {

        // fileName
        String confFileName = parseConfDataFileName(env, key);

        // valid repeat update
        Properties existProp = PropUtil.loadFileProp(confFileName);
        if (existProp != null
                && value != null
                && value.equals(existProp.getProperty("value"))
                ) {
            return new File(confFileName).getPath();
        }

        // write
        Properties prop = new Properties();
        if (value == null) {
            prop.setProperty("value-deleted", "true");
        } else {
            prop.setProperty("value", value);
        }

        PropUtil.writeFileProp(prop, confFileName);
        logger.info(">>>>>>>>>>> small-conf, setFileConfData: confFileName={}, value={}", confFileName, value);

        // brocast monitor client
        List<DeferredResult> deferredResultList = confDeferredResultMap.get(confFileName);
        if (deferredResultList != null) {
            confDeferredResultMap.remove(confFileName);
            for (DeferredResult deferredResult : deferredResultList) {
                deferredResult.setResult(new ReturnT<>(ReturnT.SUCCESS_CODE, "Monitor key update."));
            }
        }

        return new File(confFileName).getPath();
    }

    // clean
    public void cleanFileConfData(List<String> confDataFileList) {
        filterChildPath(new File(confDataFilePath), confDataFileList);
    }

    public void filterChildPath(File parentPath, final List<String> confDataFileList) {
        if (!parentPath.exists() || parentPath.list() == null || parentPath.list().length == 0) {
            return;
        }
        File[] childFileList = parentPath.listFiles();
        for (File childFile : childFileList) {
            if (childFile.isFile() && !confDataFileList.contains(childFile.getPath())) {
                childFile.delete();

                logger.info(">>>>>>>>>>> small-conf, cleanFileConfData, ConfDataFile={}", childFile.getPath());
            }
            if (childFile.isDirectory()) {
                if (parentPath.listFiles() != null && parentPath.listFiles().length > 0) {
                    filterChildPath(childFile, confDataFileList);
                } else {
                    childFile.delete();
                }

            }
        }

    }

}
