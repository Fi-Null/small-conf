package com.small.config.admin.dao;

import com.small.config.admin.domain.SmallConfEnv;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 8:39 PM
 */
@Mapper
public interface SmallConfEnvDao {

    List<SmallConfEnv> findAll();

    int save(SmallConfEnv smallConfEnv);

    int update(SmallConfEnv smallConfEnv);

    int delete(@Param("env") String env);

    SmallConfEnv load(@Param("env") String env);
}
