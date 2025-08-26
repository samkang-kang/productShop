package org.example.productshop.controller;

import org.example.productshop.service.CarPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class CarPayController {

    @Autowired
    private CarPayService carPayService;

    // 兩種方式擇一：
    // 1) 受保護情境（建議）：帶 Authorization，JwtFilter 會塞 userId
    // 2) 相容舊前端：?userId=123
    @GetMapping("/api/cart/checkout")
    public Map<String, Object> checkout(@RequestParam(value = "userId", required = false) Integer userIdParam,
                                        HttpServletRequest req) {
        Long uid = (Long) req.getAttribute("userId");
        if (uid == null && userIdParam != null) uid = userIdParam.longValue();
        if (uid == null) throw new RuntimeException("AUTH_REQUIRED_OR_USERID_MISSING");
        return carPayService.checkout(uid);
    }
}

