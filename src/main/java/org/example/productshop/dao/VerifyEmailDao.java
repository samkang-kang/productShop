package org.example.productshop.dao;

import org.example.productshop.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class VerifyEmailDao {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private EmailService emailService;

    public String verifyEmail(String token) {
        String sql = "SELECT verify_token_created_at FROM members WHERE verify_token = :token";
        Map<String, Object> param = new HashMap<>();
        param.put("token", token);

        LocalDateTime createdAt = namedParameterJdbcTemplate.queryForObject(sql, param, LocalDateTime.class);

        if (createdAt == null) {
            return "驗證失敗：連結無效或已過期";
        }

        // 限時 10 分鐘內有效
        if (createdAt.plusMinutes(10).isBefore(LocalDateTime.now())) {
            return "驗證失敗：連結已過期，請重新發送驗證信";
        }

        // 驗證成功
        String updateSql = "UPDATE members SET status = 'active', verify_token = NULL, verify_token_created_at = NULL WHERE verify_token = :token";
        namedParameterJdbcTemplate.update(updateSql, param);

        return "驗證成功！您的帳號已啟用";
    }

    public String resendVerification(String email) {
        String sql = "SELECT status FROM members WHERE email = :email";
        Map<String, Object> param = new HashMap<>();
        param.put("email", email);

        List<String> result = namedParameterJdbcTemplate.query(sql, param, (rs, rowNum) -> rs.getString("status"));
        if (result.isEmpty()) {
            return "查無此帳號";
        }

        if ("active".equals(result.get(0))) {
            return "此帳號已驗證過，無需重發";
        }

        // 產生新的 token 並更新資料
        String newToken = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        param.put("token", newToken);
        param.put("token_created_at", now);

        String updateSql = "UPDATE members SET verify_token = :token, verify_token_created_at = :token_created_at WHERE email = :email";
        namedParameterJdbcTemplate.update(updateSql, param);

        // 寄送新的驗證信
        emailService.sendVerificationEmail(email, newToken);

        return "已重新發送驗證信";
    }
}
