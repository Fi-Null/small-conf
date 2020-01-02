package com.small.config.admin.dao;

import com.small.config.admin.domain.SmallConfProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 8:48 PM
 */
@Mapper
public interface SmallConfProjectDao {

    List<SmallConfProject> findAll();

    int save(SmallConfProject smallConfProject);

    int update(SmallConfProject smallConfProject);

    int delete(@Param("appname") String appname);

    SmallConfProject load(@Param("appname") String appname);

}
