package org.example.productshop.controller;

import org.example.productshop.dao.ProductViewDao;
import org.example.productshop.service.OpenDataMarketSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;   // ★ 新增
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;                    // ★ 新增
import java.util.*;                             // 你的原有 import
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class MarketSyncController {

    @Autowired private OpenDataMarketSyncService service;
    @Autowired private ProductViewDao viewDao;

    @Autowired private JdbcTemplate jdbc; // ★ 新增：最小查詢用

    // === 你原本的兩支 ===

    @GetMapping("/markets/sync")
    public ResponseEntity<?> sync(@RequestParam(value="codes", required=false) String codes) {
        int n = service.sync(codes);
        return ResponseEntity.ok(Map.of("message","市場價同步完成","updated", n));
    }

    @GetMapping("/markets/sync-view")
    public ResponseEntity<?> syncAndView(@RequestParam(value="codes", required=false) String codes) {
        int updated = service.sync(codes);
        Set<String> codeSet = splitCodes(codes);
        var items = codeSet.isEmpty() ? viewDao.findAll() : viewDao.findByCodes(codeSet);
        return ResponseEntity.ok(Map.of(
                "updated", updated,
                "count", items.size(),
                "items", items
        ));
    }

    // === ★ 新增：前端要用的 /{id}/market-tiers ===
    // 回傳格式：
    // { "productId": 11,
    //   "m1": {"high": 120.00, "mid": 100.00, "low": 90.00},
    //   "m2": {"high": 118.00, "mid": 101.00, "low": 88.00}
    // }
    @GetMapping("/{id}/market-tiers")
    public ResponseEntity<?> getMarketTiers(@PathVariable long id) {
        final String sql = """
            SELECT id,
                   market1_high_price, market1_mid_price, market1_low_price,
                   market2_high_price, market2_mid_price, market2_low_price
            FROM products
            WHERE id = ?
            """;
        Map<String, Object> body = jdbc.query(sql, rs -> {
            if (!rs.next()) return null;

            // Map.of 不接受 null，所以用 LinkedHashMap
            Map<String, BigDecimal> m1 = new LinkedHashMap<>();
            m1.put("high", rs.getBigDecimal("market1_high_price"));
            m1.put("mid",  rs.getBigDecimal("market1_mid_price"));
            m1.put("low",  rs.getBigDecimal("market1_low_price"));

            Map<String, BigDecimal> m2 = new LinkedHashMap<>();
            m2.put("high", rs.getBigDecimal("market2_high_price"));
            m2.put("mid",  rs.getBigDecimal("market2_mid_price"));
            m2.put("low",  rs.getBigDecimal("market2_low_price"));

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("productId", rs.getLong("id"));
            map.put("m1", m1);
            map.put("m2", m2);
            return map;
        }, id);

        if (body == null) {
            return ResponseEntity.status(404).body(Map.of("error", "PRODUCT_NOT_FOUND"));
        }
        return ResponseEntity.ok(body);
    }

    private static Set<String> splitCodes(String codes) {
        if (codes == null || codes.isBlank()) return Collections.emptySet();
        return Arrays.stream(codes.split(","))
                .map(String::trim).filter(s->!s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
