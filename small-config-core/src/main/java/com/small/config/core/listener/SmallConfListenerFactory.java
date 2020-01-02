package com.small.config.core.listener;

import com.small.config.SmallConfClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 7:39 PM
 */
public class SmallConfListenerFactory {

    private static Logger logger = LoggerFactory.getLogger(SmallConfListenerFactory.class);

    /**
     * small conf listener repository
     */
    private static ConcurrentHashMap<String, List<SmallConfListener>> keyListenerRepository = new ConcurrentHashMap<>();
    private static List<SmallConfListener> noKeyConfListener = Collections.synchronizedList(new ArrayList<SmallConfListener>());

    /**
     * add listener and first invoke + watch
     *
     * @param key               empty will listener all key
     * @param SmallConfListener
     * @return
     */
    public static boolean addListener(String key, SmallConfListener SmallConfListener) {
        if (SmallConfListener == null) {
            return false;
        }
        if (key == null || key.trim().length() == 0) {
            // listene all key used
            noKeyConfListener.add(SmallConfListener);
            return true;
        } else {

            // first use, invoke and watch this key
            try {
                String value = SmallConfClient.get(key);
                SmallConfListener.onChange(key, value);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            // listene this key
            List<SmallConfListener> listeners = keyListenerRepository.get(key);
            if (listeners == null) {
                listeners = new ArrayList<>();
                keyListenerRepository.put(key, listeners);
            }
            listeners.add(SmallConfListener);
            return true;
        }
    }

    /**
     * invoke listener on small conf change
     *
     * @param key
     */
    public static void onChange(String key, String value) {
        if (key == null || key.trim().length() == 0) {
            return;
        }
        List<SmallConfListener> keyListeners = keyListenerRepository.get(key);
        if (keyListeners != null && keyListeners.size() > 0) {
            for (SmallConfListener listener : keyListeners) {
                try {
                    listener.onChange(key, value);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        if (noKeyConfListener.size() > 0) {
            for (SmallConfListener confListener : noKeyConfListener) {
                try {
                    confListener.onChange(key, value);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

}
