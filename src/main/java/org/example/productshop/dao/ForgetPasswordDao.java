package org.example.productshop.dao;

import org.example.productshop.api.BCrypt;
import org.example.productshop.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Component
public class ForgetPasswordDao {

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    EmailService emailService;

    //先寄信驗證重設密碼
    public String requestReset(String email, String newPasswordPlain){
    String sql = "SELECT COUNT(1) FROM members WHERE email=:email" ;
    HashMap<String, Object> map = new HashMap<>();
    map.put("email", email);
    Integer count = namedParameterJdbcTemplate.queryForObject(sql, map, Integer.class);
    if(count == 0 || count == null){
        return "若此 Email 存在，我們已寄出重設密碼信" ;
    }

    String token = UUID.randomUUID().toString();
    String pendingHash = BCrypt.hashpw(newPasswordPlain, BCrypt.gensalt());

        String sql1 = " UPDATE members\n" +
                "               SET verify_token = :verify_token,\n" +
                "                   verify_token_created_at = :created_at,\n" +
                "                   reset_password_hash_pending = :pending_hash\n" +
                "             WHERE email = :email" ;

        HashMap<String, Object> map1= new HashMap();
        map1.put("email", email);
        map1.put("verify_token", token);
        map1.put("created_at", LocalDateTime.now());
        map1.put("pending_hash", pendingHash);

        namedParameterJdbcTemplate.update(sql1, map1);

        emailService.sendRestPasswordEmail(email,token);

        return "若此 Email 存在，我們已寄出重設密碼驗證信";


    }




    //點擊URL 才會重設
    public String confirmReset(String token){
        String sql =" SELECT id, reset_password_hash_pending\n" +
                "              FROM members\n" +
                "             WHERE verify_token = :token\n" +
                "               AND verify_token_created_at > (NOW() - INTERVAL 10 MINUTE)\n" +
                "               AND reset_password_hash_pending IS NOT NULL";

        HashMap<String, Object> map = new HashMap<>();
        map.put("token", token);

        List<Object[]>  list = namedParameterJdbcTemplate.query(sql,map,(rs, i) -> new Object[]{
                rs.getLong("id"),
                rs.getString("reset_password_hash_pending")
        });


        if (list.isEmpty()) {
            return "連結無效或已過期。";
        }

        Long memberId = (Long) list.get(0)[0];
        String pendingHash = (String) list.get(0)[1];

        HashMap<String, Object> map1 = new HashMap<>();
        map1.put("id", memberId);
        map1.put("hash", pendingHash);

        int rows = namedParameterJdbcTemplate.update("""
            UPDATE members
               SET password = :hash,
                   verify_token = NULL,
                   verify_token_created_at = NULL,
                   reset_password_hash_pending = NULL
             WHERE id = :id
        """, map1);

        return rows > 0 ? "密碼已更新，請以新密碼登入。" : "更新失敗，請稍後再試。";

    }










}
