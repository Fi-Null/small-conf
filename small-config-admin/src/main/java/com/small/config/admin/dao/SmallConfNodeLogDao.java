package com.small.config.admin.dao;

import com.small.config.admin.domain.SmallConfNodeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 8:42 PM
 */
@Mapper
public interface SmallConfNodeLogDao {
    
    List<SmallConfNodeLog> findByKey(@Param("env") String env, @Param("key") String key);

    void add(SmallConfNodeLog smallConfNode);

    int deleteTimeout(@Param("env") String env,
                             @Param("key") String key,
                             @Param("length") int length);

}
