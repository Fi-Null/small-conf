package com.small.config.admin.domain;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 8:34 PM
 */
public class SmallConfEnv {
    private String env;         // Env
    private String title;       // 环境名称
    private int order;

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}