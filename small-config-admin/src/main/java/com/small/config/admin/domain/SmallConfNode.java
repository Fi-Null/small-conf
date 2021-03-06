package com.small.config.admin.domain;

import java.util.List;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 8:35 PM
 */
public class SmallConfNode {

    private String env;
    private String key;            // 配置Key
    private String appname;    // 所属项目AppName
    private String title;        // 配置描述
    private String value;        // 配置Value

    // plugin
    /*private String zkValue; 				// ZK中配置Value	// TODO, delete*/
    private List<SmallConfNodeLog> logList;    // 配置变更Log

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

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

	/*public String getZkValue() {
		return zkValue;
	}

	public void setZkValue(String zkValue) {
		this.zkValue = zkValue;
	}*/

    public List<SmallConfNodeLog> getLogList() {
        return logList;
    }

    public void setLogList(List<SmallConfNodeLog> logList) {
        this.logList = logList;
    }

}
