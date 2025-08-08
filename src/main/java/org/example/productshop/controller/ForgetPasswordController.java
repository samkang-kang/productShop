package org.example.productshop.controller;

import org.example.productshop.dao.ForgetPasswordDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")

public class ForgetPasswordController {

    @Autowired
    private ForgetPasswordDao forgetPasswordDao;

    // 申請重設（body: { "email": "...", "newPassword": "..." }）
    @PostMapping("users/password/reset/request")
    public Map<String, Object> requestReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String newPassword = body.get("newPassword");
        String msg = forgetPasswordDao.requestReset(email, newPassword);
        return Map.of("message", msg);
    }

    // 確認重設（使用者點信中的連結）
    // 你可以用 GET /password/reset/confirm?token=...（前端導頁即可）
    @GetMapping("users/password/reset/confirm")
    public ResponseEntity<String> confirm(@RequestParam("token") String token) {
        String msg = forgetPasswordDao.confirmReset(token);
        // 可視訊息決定回傳 200 或 400
        if ("密碼已更新，請以新密碼登入。".equals(msg)) {
            return ResponseEntity.ok(msg);
        }
        return ResponseEntity.badRequest().body(msg);
    }
}

