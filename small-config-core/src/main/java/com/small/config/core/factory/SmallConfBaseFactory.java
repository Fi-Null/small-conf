package com.small.config.core.factory;

import com.small.config.core.conf.SmallConfLocalCacheConf;
import com.small.config.core.conf.SmallConfMirrorConf;
import com.small.config.core.conf.SmallConfRemoteConf;
import com.small.config.core.listener.SmallConfListenerFactory;
import com.small.config.core.listener.impl.BeanRefreshSmallConfListener;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 7:32 PM
 */
public class SmallConfBaseFactory {

    /**
     * init
     *
     * @param adminAddress
     * @param env
     */
    public static void init(String adminAddress, String env, String accessToken, String mirrorfile) {
        // init
        SmallConfRemoteConf.init(adminAddress, env, accessToken);    // init remote util
        SmallConfMirrorConf.init(mirrorfile);            // init mirror util
        SmallConfLocalCacheConf.init();                // init cache + thread, cycle refresh + monitor

        SmallConfListenerFactory.addListener(null, new BeanRefreshSmallConfListener());    // listener all key change

    }

    /**
     * destory
     */
    public static void destroy() {
        SmallConfLocalCacheConf.destroy();    // destroy
    }

}
