package org.example.productshop.dao;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MemberDao {

    private final JdbcTemplate jdbc;

    public MemberDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Integer findIdByEmail(String email) {
        try {
            return jdbc.queryForObject("SELECT id FROM members WHERE email = ?", Integer.class, email);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}