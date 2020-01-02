package com.small.config.admin.dao;

import com.small.config.admin.domain.SmallConfNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 8:40 PM
 */
@Mapper
public interface SmallConfNodeDao {

    List<SmallConfNode> pageList(@Param("offset") int offset,
                                 @Param("pagesize") int pagesize,
                                 @Param("env") String env,
                                 @Param("appname") String appname,
                                 @Param("key") String key);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("env") String env,
                      @Param("appname") String appname,
                      @Param("key") String key);

    int delete(@Param("env") String env, @Param("key") String key);

    void insert(SmallConfNode smallConfNode);

    SmallConfNode load(@Param("env") String env, @Param("key") String key);

    int update(SmallConfNode smallConfNode);
}
