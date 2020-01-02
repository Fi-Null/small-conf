package com.small.config.core.conf;

import com.small.config.core.listener.SmallConfListenerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 7:51 PM
 */
public class SmallConfLocalCacheConf {

    private static Logger logger = LoggerFactory.getLogger(SmallConfLocalCacheConf.class);


    // ---------------------- init/destroy ----------------------

    private static ConcurrentHashMap<String, CacheNode> localCacheRepository = null;

    private static Thread refreshThread;
    private static boolean refreshThreadStop = false;

    public static void init() {

        localCacheRepository = new ConcurrentHashMap<>();

        // preload: mirror or remote
        Map<String, String> preConfData = new HashMap<>();

        Map<String, String> mirrorConfData = SmallConfMirrorConf.readConfMirror();

        Map<String, String> remoteConfData = null;
        if (mirrorConfData != null && mirrorConfData.size() > 0) {
            remoteConfData = SmallConfRemoteConf.find(mirrorConfData.keySet());
        }

        if (mirrorConfData != null && mirrorConfData.size() > 0) {
            preConfData.putAll(mirrorConfData);
        }
        if (remoteConfData != null && remoteConfData.size() > 0) {
            preConfData.putAll(remoteConfData);
        }
        if (preConfData != null && preConfData.size() > 0) {
            for (String preKey : preConfData.keySet()) {
                set(preKey, preConfData.get(preKey), SET_TYPE.PRELOAD);
            }
        }

        // refresh thread
        refreshThread = new Thread(() -> {
            while (!refreshThreadStop) {
                try {
                    refreshCacheAndMirror();
                } catch (Exception e) {
                    if (!refreshThreadStop && !(e instanceof InterruptedException)) {
                        logger.error(">>>>>>>>>> small-conf, refresh thread error.");
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info(">>>>>>>>>> small-conf, refresh thread stoped.");
        });
        refreshThread.setDaemon(true);
        refreshThread.start();

        logger.info(">>>>>>>>>> small-conf, XxlConfLocalCacheConf init success.");
    }

    public static void destroy() {
        if (refreshThread != null) {
            refreshThreadStop = true;
            refreshThread.interrupt();
        }
    }

    /**
     * local cache node
     */
    public static class CacheNode implements Serializable {
        private static final long serialVersionUID = 42L;

        private String value;

        public CacheNode() {
        }

        public CacheNode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }


    // ---------------------- util ----------------------

    /**
     * refresh Cache And Mirror, with real-time minitor
     */
    private static void refreshCacheAndMirror() throws InterruptedException {

        if (localCacheRepository.size() == 0) {
            TimeUnit.SECONDS.sleep(3);
            return;
        }

        // monitor
        boolean monitorRet = SmallConfRemoteConf.monitor(localCacheRepository.keySet());

        // avoid fail-retry request too quick
        if (!monitorRet) {
            TimeUnit.SECONDS.sleep(10);
        }

        // refresh cache: remote > cache
        Set<String> keySet = localCacheRepository.keySet();
        if (keySet.size() > 0) {

            Map<String, String> remoteDataMap = SmallConfRemoteConf.find(keySet);
            if (remoteDataMap != null && remoteDataMap.size() > 0) {
                for (String remoteKey : remoteDataMap.keySet()) {
                    String remoteData = remoteDataMap.get(remoteKey);

                    CacheNode existNode = localCacheRepository.get(remoteKey);
                    if (existNode != null && existNode.getValue() != null && existNode.getValue().equals(remoteData)) {
                        logger.debug(">>>>>>>>>> small-conf: RELOAD unchange-pass [{}].", remoteKey);
                    } else {
                        set(remoteKey, remoteData, SET_TYPE.RELOAD);
                    }

                }
            }
        }

        // refresh mirror: cache > mirror
        Map<String, String> mirrorConfData = new HashMap<>();
        for (String key : keySet) {
            CacheNode existNode = localCacheRepository.get(key);
            mirrorConfData.put(key, existNode.getValue() != null ? existNode.getValue() : "");
        }
        SmallConfMirrorConf.writeConfMirror(mirrorConfData);

        logger.debug(">>>>>>>>>> small-conf, refreshCacheAndMirror success.");
    }


    // ---------------------- inner api ----------------------

    public enum SET_TYPE {
        SET,        // first use
        RELOAD,     // value updated
        PRELOAD     // pre hot
    }

    /**
     * set conf (invoke listener)
     *
     * @param key
     * @param value
     * @return
     */
    private static void set(String key, String value, SET_TYPE optType) {
        localCacheRepository.put(key, new CacheNode(value));
        logger.info(">>>>>>>>>> small-conf: {}: [{}={}]", optType, key, value);

        // value updated, invoke listener
        if (optType == SET_TYPE.RELOAD) {
            SmallConfListenerFactory.onChange(key, value);
        }

        // new conf, new monitor
        if (optType == SET_TYPE.SET) {
            refreshThread.interrupt();
        }
    }

    /**
     * get conf
     *
     * @param key
     * @return
     */
    private static CacheNode get(String key) {
        if (localCacheRepository.containsKey(key)) {
            CacheNode cacheNode = localCacheRepository.get(key);
            return cacheNode;
        }
        return null;
    }

    /**
     * update conf  (only update exists key)  (invoke listener)
     *
     * @param key
     * @param value
     */
    /*private static void update(String key, String value) {
        if (localCacheRepository.containsKey(key)) {
            set(key, value, SET_TYPE.UPDATE );
        }
    }*/

    /**
     * remove conf
     *
     * @param key
     * @return
     */
    /*private static void remove(String key) {
        if (localCacheRepository.containsKey(key)) {
            localCacheRepository.remove(key);
        }
        logger.info(">>>>>>>>>> small-conf: REMOVE: [{}]", key);
    }*/


    // ---------------------- api ----------------------

    /**
     * get conf
     *
     * @param key
     * @param defaultVal
     * @return
     */
    public static String get(String key, String defaultVal) {

        // level 1: local cache
        SmallConfLocalCacheConf.CacheNode cacheNode = SmallConfLocalCacheConf.get(key);
        if (cacheNode != null) {
            return cacheNode.getValue();
        }

        // level 2	(get-and-watch, add-local-cache)
        String remoteData = null;
        try {
            remoteData = SmallConfRemoteConf.find(key);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        set(key, remoteData, SET_TYPE.SET);        // support cache null value
        if (remoteData != null) {
            return remoteData;
        }

        return defaultVal;
    }


}
