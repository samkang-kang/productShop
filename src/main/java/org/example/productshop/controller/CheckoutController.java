package org.example.productshop.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CheckoutController (no Service) — 改為 POST /api/checkout/start
 */
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);
    private final NamedParameterJdbcTemplate jdbc;

    public CheckoutController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestParam Long userId) {
        try {
            // 1) 取會員 email
            String email = jdbc.queryForObject(
                    "SELECT email FROM members WHERE id=:id LIMIT 1",
                    new MapSqlParameterSource("id", userId),
                    String.class
            );
            if (email == null) return ResponseEntity.badRequest().body(Map.of("error", "user not found"));

            // 2) 取預設地址
            List<Map<String, Object>> addrRows = jdbc.queryForList(
                    "SELECT recipient_name, phone, city, district, postal_code, detail_address " +
                            "FROM addresses WHERE member_id=:uid ORDER BY is_default DESC, id ASC LIMIT 1",
                    new MapSqlParameterSource("uid", userId)
            );
            if (addrRows.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "default address not found"));
            Map<String, Object> addr = addrRows.get(0);

            // 3) 讀購物車
            List<Map<String, Object>> items = jdbc.queryForList(
                    "SELECT sci.product_id, p.name, sci.quantity, sci.unit_price " +
                            "FROM shopping_cart_items sci JOIN products p ON p.id = sci.product_id " +
                            "WHERE sci.user_id = :uid",
                    new MapSqlParameterSource("uid", userId)
            );
            if (items.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "empty cart"));

            long amount = 0;
            for (Map<String, Object> it : items) {
                long q = ((Number) it.get("quantity")).longValue();
                long u = ((Number) it.get("unit_price")).longValue();
                amount += q * u;
            }
            String firstName = (String) items.get(0).get("name");
            String itemName = (items.size() == 1) ? firstName : (firstName + " 等");

            // 4) 產生 MTN（<=20字）
            String mtn = "MTN" + System.currentTimeMillis();

            // 5) 建立訂單（PENDING）
            String shippingAddr = String.format("%s%s%s %s",
                    nvl(addr.get("postal_code")),
                    nvl(addr.get("city")),
                    nvl(addr.get("district")),
                    nvl(addr.get("detail_address"))
            ).trim();

            MapSqlParameterSource o = new MapSqlParameterSource()
                    .addValue("uid", userId)
                    .addValue("recipient_name", addr.get("recipient_name"))
                    .addValue("phone", addr.get("phone"))
                    .addValue("email", email)
                    .addValue("shipping_addr", shippingAddr)
                    .addValue("total_amount", amount)
                    .addValue("merchant_trade_no", mtn)
                    .addValue("status", "PENDING");

            jdbc.update(
                    "INSERT INTO orders(user_id, recipient_name, phone, email, shipping_addr, total_amount, merchant_trade_no, status, created_at) " +
                            "VALUES(:uid,:recipient_name,:phone,:email,:shipping_addr,:total_amount,:merchant_trade_no,:status,NOW())", o
            );

            Long orderId = jdbc.queryForObject(
                    "SELECT id FROM orders WHERE merchant_trade_no=:mtn LIMIT 1",
                    new MapSqlParameterSource("mtn", mtn), Long.class
            );

            // 6) 寫入 order_items（快照）
            for (Map<String, Object> it : items) {
                long q = ((Number) it.get("quantity")).longValue();
                long u = ((Number) it.get("unit_price")).longValue();
                jdbc.update(
                        "INSERT INTO order_items(order_id, product_id, name, quantity, unit_price, subtotal) " +
                                "VALUES(:order_id,:product_id,:name,:quantity,:unit_price,:subtotal)",
                        new MapSqlParameterSource()
                                .addValue("order_id", orderId)
                                .addValue("product_id", it.get("product_id"))
                                .addValue("name", it.get("name"))
                                .addValue("quantity", q)
                                .addValue("unit_price", u)
                                .addValue("subtotal", q * u)
                );
            }

            // 7) 回前端
            return ResponseEntity.ok(Map.of(
                    "itemName", itemName,
                    "amount", amount,
                    "mtn", mtn
            ));
        } catch (Exception e) {
            log.error("[checkout/start] failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "checkout failed",
                    "message", e.getMessage()
            ));
        }
    }

    private static String nvl(Object o) { return (o == null) ? "" : String.valueOf(o); }
}
