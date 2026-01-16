package com.example.batch.demo.infrastructure.mapper;

import com.example.batch.demo.infrastructure.entity.DemoUserEntity;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * DB4 数据源 Mapper
 * 负责操作 DB4_Business 中的 DEMO_USER 表
 */
@Mapper
@UseDataSource("db4")
public interface Db4UserMapper extends BaseMapper<DemoUserEntity> {

    /**
     * 查询所有待处理的用户
     */
    @Select("SELECT * FROM DEMO_USER WHERE STATUS = 'PENDING'")
    List<DemoUserEntity> findPending();

    /**
     * 批量更新用户状态
     */
    @Update("<script>" +
            "UPDATE DEMO_USER SET STATUS = #{status}, UPDATE_TIME = GETDATE() " +
            "WHERE ID IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int bulkUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 清空表（用于 Demo 演示）
     */
    @Update("TRUNCATE TABLE DEMO_USER")
    void truncate();
}
