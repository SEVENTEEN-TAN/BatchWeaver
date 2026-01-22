package com.batchweaver.demo.service.impl;

import com.batchweaver.demo.entity.DemoUser;
import com.batchweaver.demo.service.Db4BusinessService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Db4 业务服务实现
 * <p>
 * 使用 @Transactional(transactionManager = "tm4")
 */
@Service
public class Db4BusinessServiceImpl implements Db4BusinessService {

    private final NamedParameterJdbcTemplate namedJdbcTemplate4;

    public Db4BusinessServiceImpl(@Qualifier("namedJdbcTemplate4") NamedParameterJdbcTemplate namedJdbcTemplate4) {
        this.namedJdbcTemplate4 = namedJdbcTemplate4;
    }

    @Override
    @Transactional(transactionManager = "tm4", propagation = Propagation.REQUIRED)
    public void batchInsertUsers(List<DemoUser> users) {
        String sql = "INSERT INTO DEMO_USER ( name, email, birth_date) " +
                "VALUES ( :name, :email, :birthDate)";

        SqlParameterSource[] batchParams = users.stream()
                .map(user -> new MapSqlParameterSource()
                        .addValue("id", user.getId())
                        .addValue("name", user.getName())
                        .addValue("email", user.getEmail())
                        .addValue("birthDate", user.getBirthDate()))
                .toArray(SqlParameterSource[]::new);

        namedJdbcTemplate4.batchUpdate(sql, batchParams);
    }

    /**
     * 获取所有用户（用于同步）
     */
    @Override
    public List<DemoUser> getAllUsers() {
        String sql = "SELECT id, name, email, birth_date FROM DEMO_USER";
        return namedJdbcTemplate4.query(sql, (rs, rowNum) -> {
            DemoUser user = new DemoUser();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setBirthDate(rs.getDate("birth_date"));
            return user;
        });
    }
}
