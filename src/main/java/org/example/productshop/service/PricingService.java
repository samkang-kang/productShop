package org.example.productshop.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
@Service
public class PricingService {
    /** A 方案：1~9 上價、10~20 中價、21+ 下價；若該價位為 null，會容錯回退到其它價位 */
    public BigDecimal pickUnitPrice(BigDecimal high, BigDecimal mid, BigDecimal low, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        if (quantity <= 9) {
            return firstNonNull(high, mid, low);
        } else if (quantity <= 20) {
            return firstNonNull(mid, high, low);
        } else {
            return firstNonNull(low, mid, high);
        }
    }

    /** 可選：若你要讓前端「強制選擇」某價層（HIGH/MID/LOW） */
    public BigDecimal pickByTier(BigDecimal high, BigDecimal mid, BigDecimal low, String tier) {
        if (tier == null) return firstNonNull(high, mid, low);
        return switch (tier.toUpperCase()) {
            case "HIGH" -> firstNonNull(high, mid, low);
            case "MID"  -> firstNonNull(mid, high, low);
            case "LOW"  -> firstNonNull(low, mid, high);
            default -> firstNonNull(high, mid, low);
        };
    }

    private static BigDecimal firstNonNull(BigDecimal a, BigDecimal b, BigDecimal c) {
        if (a != null) return a;
        if (b != null) return b;
        if (c != null) return c;
        return BigDecimal.ZERO; // 三價都沒有，避免 NPE（你也可以改成丟例外）
    }

    /** 回傳價層名稱（依數量規則） */
    public String resolveTierByQuantity(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        if (quantity <= 9)  return "HIGH";
        if (quantity <= 20) return "MID";
        return "LOW";
    }
}
