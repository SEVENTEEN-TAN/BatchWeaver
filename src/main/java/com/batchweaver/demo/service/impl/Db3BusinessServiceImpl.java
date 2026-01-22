package com.batchweaver.demo.service.impl;

import com.batchweaver.demo.entity.DemoUser;
import com.batchweaver.demo.service.Db3BusinessService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Db3 业务服务实现
 * <p>
 * 使用 @Transactional(transactionManager = "tm3")
 */
@Service
public class Db3BusinessServiceImpl implements Db3BusinessService {

    private final NamedParameterJdbcTemplate namedJdbcTemplate3;

    public Db3BusinessServiceImpl(@Qualifier("namedJdbcTemplate3") NamedParameterJdbcTemplate namedJdbcTemplate3) {
        this.namedJdbcTemplate3 = namedJdbcTemplate3;
    }

    @Override
    @Transactional(transactionManager = "tm3", propagation = Propagation.REQUIRED)
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

        namedJdbcTemplate3.batchUpdate(sql, batchParams);
    }

    /**
     * 获取所有用户（用于同步）
     */
    @Override
    public List<DemoUser> getAllUsers() {
        String sql = "SELECT id, name, email, birth_date FROM DEMO_USER";
        return namedJdbcTemplate3.query(sql, (rs, rowNum) -> {
            DemoUser user = new DemoUser();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setBirthDate(rs.getDate("birth_date"));
            return user;
        });
    }
}
