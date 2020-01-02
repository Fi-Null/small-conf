package com.small.config.admin.dao;

import com.small.config.admin.domain.SmallConfNodeMsg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SmallConfNodeMsgDao {

    void add(SmallConfNodeMsg smallConfNode);

    List<SmallConfNodeMsg> findMsg(@Param("readedMsgIds") List<Integer> readedMsgIds);

    int cleanMessage(@Param("messageTimeout") int messageTimeout);

}
