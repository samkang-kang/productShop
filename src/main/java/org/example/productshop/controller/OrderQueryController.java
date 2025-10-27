package org.example.productshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {

    private final NamedParameterJdbcTemplate jdbc;

    @Autowired
    public OrderQueryController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/by-mtn")
    public ResponseEntity<?> getByMtn(@RequestParam("mtn") String mtn) {
        Map<String,Object> o;
        try {
            o = jdbc.queryForMap(
                    // 只取「大多數 schema 都有」的欄位，避免 Unknown column
                    "SELECT id, user_id, total_amount, status, payment_type, merchant_trade_no, created_at, updated_at " +
                            "FROM orders WHERE merchant_trade_no=:mtn LIMIT 1",
                    new MapSqlParameterSource("mtn", mtn)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "order not found", "mtn", mtn));
        }

        Number userId = (Number) o.get("user_id");

        // Email 從 members 補
        String email = null;
        try {
            email = jdbc.queryForObject(
                    "SELECT email FROM members WHERE id=:uid",
                    new MapSqlParameterSource("uid", userId),
                    String.class
            );
        } catch (Exception ignore) {}

        // 收件資訊從 addresses 預設地址補
        Map<String,Object> addr = Collections.emptyMap();
        try {
            addr = jdbc.queryForMap(
                    "SELECT recipient_name, phone, city, district, postal_code, detail_address " +
                            "FROM addresses WHERE member_id=:uid ORDER BY is_default DESC, id ASC LIMIT 1",
                    new MapSqlParameterSource("uid", userId)
            );
        } catch (Exception ignore) {}

        String recipientName = s(addr.get("recipient_name"));
        String phone         = s(addr.get("phone"));
        String shippingAddr  = (s(addr.get("postal_code")) + s(addr.get("city")) +
                s(addr.get("district")) + " " + s(addr.get("detail_address"))).trim();

        // 明細
        List<Map<String,Object>> items = jdbc.queryForList(
                "SELECT product_id, name, quantity, unit_price, subtotal, '' AS spec " +
                        "FROM order_items WHERE order_id=:oid",
                new MapSqlParameterSource("oid", (Number)o.get("id"))
        );

        // 組回傳
        Map<String,Object> resp = new LinkedHashMap<>();
        resp.put("id", o.get("id"));
        resp.put("orderNo", o.get("id"));
        resp.put("merchantTradeNo", o.get("merchant_trade_no"));
        resp.put("recipientName", recipientName);
        resp.put("phone", phone);
        resp.put("email", email);
        resp.put("shippingAddr", shippingAddr);
        resp.put("totalAmount", o.get("total_amount"));
        resp.put("status", o.get("status"));
        resp.put("paymentType", o.get("payment_type"));
        resp.put("createdAt", o.get("created_at"));
        resp.put("updatedAt", o.get("updated_at"));
        resp.put("items", items);
        return ResponseEntity.ok(resp);
    }

    private static String s(Object v) { return v == null ? "" : String.valueOf(v); }
}
