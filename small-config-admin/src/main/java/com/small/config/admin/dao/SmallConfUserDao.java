package com.small.config.admin.dao;

import com.small.config.admin.domain.SmallConfUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 8:50 PM
 */
@Mapper
public interface SmallConfUserDao {
    List<SmallConfUser> pageList(@Param("offset") int offset,
                                 @Param("pagesize") int pagesize,
                                 @Param("username") String username,
                                 @Param("permission") int permission);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("username") String username,
                      @Param("permission") int permission);

    int add(SmallConfUser smallConfUser);

    int update(SmallConfUser smallConfUser);

    int delete(@Param("username") String username);

    SmallConfUser load(@Param("username") String username);

}
