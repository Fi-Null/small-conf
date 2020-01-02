package com.small.config.admin.domain;

import java.util.Date;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 8:38 PM
 */
public class SmallConfNodeMsg {

    private int id;
    private Date addtime;
    private String env;
    private String key;
    private String value;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getAddtime() {
        return addtime;
    }

    public void setAddtime(Date addtime) {
        this.addtime = addtime;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
