package org.example.productshop.dao;

import org.example.productshop.api.BCrypt;
import org.example.productshop.entity.Member;
import org.example.productshop.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

@Component
public class RegisterDaoImpel implements RegisterDao {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private EmailService emailService;

    @Override
    public String register(Member member) {
        String sql = "INSERT INTO members (email, password, name, phone, created_at, last_login, status, verify_token, verify_token_created_at) " +
                "VALUES (:email, :password, :name, :phone, NOW(), NULL, :status, :token, :token_created_at)";

        HashMap<String, Object> map = new HashMap<>();
        String token = UUID.randomUUID().toString();

        map.put("email", member.getEmail());
        map.put("password", BCrypt.hashpw(member.getPassword(), BCrypt.gensalt()));
        map.put("name", member.getName());
        map.put("phone", member.getPhone());
        map.put("status", "pending");
        map.put("token", token);
        map.put("token_created_at", LocalDateTime.now());

        namedParameterJdbcTemplate.update(sql, map);

        // 發送驗證信
        emailService.sendVerificationEmail(member.getEmail(), token);

        return "註冊成功，請至信箱點擊驗證連結完成啟用";
    }



}
