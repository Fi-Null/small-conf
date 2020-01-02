package com.small.config;

import com.small.config.core.conf.SmallConfLocalCacheConf;
import com.small.config.core.listener.SmallConfListener;
import com.small.config.core.listener.SmallConfListenerFactory;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 4:02 PM
 */
public class SmallConfClient {

    public static String get(String key, String defaultVal) {
        return SmallConfLocalCacheConf.get(key, defaultVal);
    }

    /**
     * get conf (string)
     *
     * @param key
     * @return
     */
    public static String get(String key) {
        return get(key, null);
    }

    /**
     * get conf (boolean)
     *
     * @param key
     * @return
     */
    public static boolean getBoolean(String key) {
        String value = get(key, null);
        if (value == null) {
            throw new RuntimeException("config key [" + key + "] does not exist");
        }
        return Boolean.valueOf(value);
    }

    /**
     * get conf (short)
     *
     * @param key
     * @return
     */
    public static short getShort(String key) {
        String value = get(key, null);
        if (value == null) {
            throw new RuntimeException("config key [" + key + "] does not exist");
        }
        return Short.valueOf(value);
    }

    /**
     * get conf (int)
     *
     * @param key
     * @return
     */
    public static int getInt(String key) {
        String value = get(key, null);
        if (value == null) {
            throw new RuntimeException("config key [" + key + "] does not exist");
        }
        return Integer.valueOf(value);
    }

    /**
     * get conf (long)
     *
     * @param key
     * @return
     */
    public static long getLong(String key) {
        String value = get(key, null);
        if (value == null) {
            throw new RuntimeException("config key [" + key + "] does not exist");
        }
        return Long.valueOf(value);
    }

    /**
     * get conf (float)
     *
     * @param key
     * @return
     */
    public static float getFloat(String key) {
        String value = get(key, null);
        if (value == null) {
            throw new RuntimeException("config key [" + key + "] does not exist");
        }
        return Float.valueOf(value);
    }

    /**
     * get conf (double)
     *
     * @param key
     * @return
     */
    public static double getDouble(String key) {
        String value = get(key, null);
        if (value == null) {
            throw new RuntimeException("config key [" + key + "] does not exist");
        }
        return Double.valueOf(value);
    }

    /**
     * add listener with small conf change
     *
     * @param key
     * @param smallConfListener
     * @return
     */
    public static boolean addListener(String key, SmallConfListener smallConfListener) {
        return SmallConfListenerFactory.addListener(key, smallConfListener);
    }
}
