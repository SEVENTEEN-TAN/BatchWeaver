package com.batchweaver.demo.service;

import com.batchweaver.demo.entity.DemoUser;

import java.util.List;

/**
 * Db3 业务服务接口
 * <p>
 * 提供 DB3 数据库的业务操作
 */
public interface Db3BusinessService {

    /**
     * 批量插入用户数据到 db3
     *
     * @param users 用户列表
     */
    void batchInsertUsers(List<DemoUser> users);

    /**
     * 获取所有用户（用于同步）
     *
     * @return 用户列表
     */
    List<DemoUser> getAllUsers();
}
