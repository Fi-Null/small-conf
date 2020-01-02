package com.small.config.core.listener;

public interface SmallConfListener {

    /**
     * invoke when first-use or conf-change
     *
     * @param key
     */
    public void onChange(String key, String value) throws Exception;

}
