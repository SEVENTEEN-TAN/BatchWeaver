package com.batchweaver.demo.service.impl;

import com.batchweaver.demo.entity.DemoUser;
import com.batchweaver.demo.service.Db2BusinessService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Db2 业务服务实现
 * <p>
 * 关键：使用 @Transactional(transactionManager = "tm2")
 * 确保业务事务由 tm2 管理，与元数据事务（tm1）隔离
 */
@Service
public class Db2BusinessServiceImpl implements Db2BusinessService {

    private final NamedParameterJdbcTemplate namedJdbcTemplate2;

    public Db2BusinessServiceImpl(@Qualifier("namedJdbcTemplate2") NamedParameterJdbcTemplate namedJdbcTemplate2) {
        this.namedJdbcTemplate2 = namedJdbcTemplate2;
    }

    /**
     * 批量插入用户数据到 db2
     * <p>
     * 关键：显式指定 transactionManager = "tm2"
     */
    @Override
    @Transactional(transactionManager = "tm2", propagation = Propagation.REQUIRED)
    public void batchInsertUsers(List<DemoUser> users) {
        String sql = "INSERT INTO DEMO_USER (name, email, birth_date) " +
                "VALUES (:name, :email, :birthDate)";

        SqlParameterSource[] batchParams = users.stream()
                .map(user -> new MapSqlParameterSource()
                        .addValue("name", user.getName())
                        .addValue("email", user.getEmail())
                        .addValue("birthDate", user.getBirthDate()))
                .toArray(SqlParameterSource[]::new);

        namedJdbcTemplate2.batchUpdate(sql, batchParams);
    }

    /**
     * 获取所有用户（用于同步）
     */
    @Override
    @Transactional(transactionManager = "tm2", propagation = Propagation.SUPPORTS, readOnly = true)
    public List<DemoUser> getAllUsers() {
        String sql = "SELECT id, name, email, birth_date FROM DEMO_USER";
        return namedJdbcTemplate2.query(sql, (rs, rowNum) -> {
            DemoUser user = new DemoUser();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setBirthDate(rs.getDate("birth_date"));
            return user;
        });
    }
}
