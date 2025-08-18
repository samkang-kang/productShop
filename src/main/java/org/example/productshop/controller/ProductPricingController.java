package org.example.productshop.controller;

import org.example.productshop.dao.ProductReadMarketDao;
import org.example.productshop.service.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pricing")
@CrossOrigin(origins = "*")
public class ProductPricingController {
    private final ProductReadMarketDao productReadMarketDao;
    private final PricingService pricing;

    public ProductPricingController(ProductReadMarketDao productReadMarketDao,
                                    PricingService pricing) {
        this.productReadMarketDao = productReadMarketDao;
        this.pricing = pricing;
    }

    /** 前端在「加入購物車之前」用這支先把 T1/T2 的三價顯示出來 */
    @GetMapping("/{productId}/options")
    public ResponseEntity<?> getOptions(@PathVariable long productId,
                                        @RequestParam(value = "market", required = false) String market) {
        var p = productReadMarketDao.findById(productId);
        if (p == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND", "message", "找不到商品 id=" + productId
            ));
        }

        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("productId", p.getId());
        payload.put("name", p.getName());
        payload.put("rule", Map.of("HIGH","1-9","MID","10-20","LOW","21+"));

        // 小工具：允許 null 的價格區塊 + 休市狀態
        java.util.function.Function<String, Map<String,Object>> t1 = k -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("code",      p.getM1Code());
            m.put("name",      p.getM1Name());
            m.put("unit",      p.getM1Unit());
            m.put("high",      p.getM1High());
            m.put("mid",       p.getM1Mid());
            m.put("low",       p.getM1Low());
            m.put("tradeDate", p.getM1Date());
            m.put("status",    (p.getM1High()==null && p.getM1Mid()==null && p.getM1Low()==null) ? "CLOSED" : "OPEN");
            return m;
        };
        java.util.function.Function<String, Map<String,Object>> t2 = k -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("code",      p.getM2Code());
            m.put("name",      p.getM2Name());
            m.put("unit",      p.getM2Unit());
            m.put("high",      p.getM2High());
            m.put("mid",       p.getM2Mid());
            m.put("low",       p.getM2Low());
            m.put("tradeDate", p.getM2Date());
            m.put("status",    (p.getM2High()==null && p.getM2Mid()==null && p.getM2Low()==null) ? "CLOSED" : "OPEN");
            return m;
        };

        // 不指定市場 → 一次回 T1/T2
        if (market == null || market.isBlank()) {
            payload.put("T1", t1.apply("T1"));
            payload.put("T2", t2.apply("T2"));
            return ResponseEntity.ok(payload);
        }

        boolean useT2 = "T2".equalsIgnoreCase(market);
        payload.put("market", useT2 ? "T2" : "T1");

        Map<String,Object> price = new LinkedHashMap<>();
        if (useT2) {
            price.put("high",      p.getM2High());
            price.put("mid",       p.getM2Mid());
            price.put("low",       p.getM2Low());
            price.put("unit",      p.getM2Unit());
            price.put("tradeDate", p.getM2Date());
            price.put("status",    (p.getM2High()==null && p.getM2Mid()==null && p.getM2Low()==null) ? "CLOSED" : "OPEN");
        } else {
            price.put("high",      p.getM1High());
            price.put("mid",       p.getM1Mid());
            price.put("low",       p.getM1Low());
            price.put("unit",      p.getM1Unit());
            price.put("tradeDate", p.getM1Date());
            price.put("status",    (p.getM1High()==null && p.getM1Mid()==null && p.getM1Low()==null) ? "CLOSED" : "OPEN");
        }
        payload.put("price", price);
        return ResponseEntity.ok(payload);
    }

    /** 依數量自動選價；也可 forceTier=HIGH|MID|LOW 強制價層（A 方案，不鎖價） */
    @PostMapping("/quote")
    public ResponseEntity<?> quote(@RequestBody QuoteReq req) {
        if (req.productId == null || req.quantity == null || req.quantity <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error","BAD_REQUEST","message","productId/quantity 必填，且 quantity > 0"
            ));
        }
        var p = productReadMarketDao.findById(req.productId);
        if (p == null) {
            return ResponseEntity.status(404).body(Map.of("error","NOT_FOUND","message","找不到商品"));
        }

        boolean useT2 = "T2".equalsIgnoreCase(req.market);
        BigDecimal high = useT2 ? p.getM2High() : p.getM1High();
        BigDecimal mid  = useT2 ? p.getM2Mid()  : p.getM1Mid();
        BigDecimal low  = useT2 ? p.getM2Low()  : p.getM1Low();

        String tierUsed = (req.forceTier == null || req.forceTier.isBlank())
                ? pricing.resolveTierByQuantity(req.quantity)
                : req.forceTier.toUpperCase();

        BigDecimal unit = (req.forceTier == null || req.forceTier.isBlank())
                ? pricing.pickUnitPrice(high, mid, low, req.quantity)
                : pricing.pickByTier(high, mid, low, req.forceTier);

        BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(req.quantity));

        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("productId", p.getId());
        payload.put("name", p.getName());
        payload.put("market", useT2 ? "T2" : "T1");
        payload.put("tierUsed", tierUsed);
        payload.put("unitPrice", unit);
        payload.put("quantity", req.quantity);
        payload.put("subtotal", subtotal);
        payload.put("allPrices", Map.of("high", high, "mid", mid, "low", low));
        return ResponseEntity.ok(payload);
    }

    // ---- DTO ----
    public static class QuoteReq {
        public Long productId;
        public String market;     // "T1" | "T2"
        public Integer quantity;  // > 0
        public String forceTier;  // 可選 "HIGH" | "MID" | "LOW"
    }
}
