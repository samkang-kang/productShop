package org.example.productshop.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserDao {
    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    /** 由 email 查 user id；找不到回 null */
    public Long findIdByEmail(String email) {
        String sql = "SELECT id FROM members WHERE email = :email LIMIT 1";
        var list = jdbc.query(sql,
                new MapSqlParameterSource("email", email),
                (rs, i) -> rs.getLong("id"));
        return list.isEmpty() ? null : list.get(0);
    }
}
