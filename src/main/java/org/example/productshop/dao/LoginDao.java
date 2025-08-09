package org.example.productshop.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class LoginDao {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public String checkEmail(String email){
        String sql ="SELECT email FROM members WHERE email = :email";
        Map<String, Object> map = new HashMap();
        map.put("email", email);
        try {
            return namedParameterJdbcTemplate.queryForObject(sql, map, String.class);
        } catch (Exception e) {

            return null;
        }

    }



    public String checkStatus(String email) {
        String sql = "select status from members where email = :email";
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        return namedParameterJdbcTemplate.queryForObject(sql, map, String.class);

    }


    public String getHashpassword(String email){
        String sql = "SELECT password FROM members WHERE email = :email";
        HashMap<String, Object> map = new HashMap();
        map.put("email", email);
        try {
            return namedParameterJdbcTemplate.queryForObject(sql, map, String.class);
        } catch (Exception e) {

            return null;
        }
    }




}
