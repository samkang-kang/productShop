package org.example.productshop.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.productshop.api.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtFilter implements Filter {

    @Autowired
    private JwtUtil jwtUtil;

    // 白名單路徑（不需要驗證 JWT）
    private static final List<String> WHITELIST = Arrays.asList(
            // 使用者相關
            "/users/register",
            "/users/login",
            "/users/verify",
            "/users/resend-verification",
            "/users/password/reset/request",
            "/users/password/reset/confirm",

            // 靜態資源
            "/**/*.html",
            "/favicon.ico",
            "/CSS/",
            "/JS/",
            "/img/",

            // 公開 API
            "/api/products/search",
            "/api/products/markets/sync", // 價格自動更新
            "/api/pricing/",              // 報價根路徑
            "/api/pricing",               // 單一請求 (避免不一致)
            "/api/pricing/quote",
            "/api/pricing/**",            // 通配所有 pricing API
            "/api/cart/add",              // 測試用新增
            "/api/cart/remove",
            "/api/products/",
            "/api/products/*/market-tiers",// 移除購物車
            "/api/cart/",
            "/api/cart/add",
            "/api/cart/remove",
            "/api/cart/update",

            // 其他
            "/error"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // CORS 設定
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:63342");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        res.setHeader("Access-Control-Allow-Credentials", "true");

        // OPTIONS 預檢請求直接放行
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String path = req.getRequestURI();

        // 白名單比對（精準 or 前綴）
        boolean isWhitelisted = WHITELIST.stream().anyMatch(whitelistPath -> {
            if (whitelistPath.endsWith("/")) {
                return path.startsWith(whitelistPath); // 目錄前綴
            } else {
                return path.equals(whitelistPath) || path.startsWith(whitelistPath + "/");
            }
        });

        if (isWhitelisted) {
            chain.doFilter(request, response);
            return;
        }

        // 驗證 Token
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                if (jwtUtil.validateToken(token)) {
                    String email = jwtUtil.getEmailFromToken(token);
                    req.setAttribute("userEmail", email);
                    chain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"Invalid token\"}");
                return;
            }
        }

        // 沒有 Token 或驗證失敗
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"Authorization header required\"}");
    }
}
