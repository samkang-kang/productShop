package org.example.productshop.entity;

import java.math.BigDecimal;

public class CartItemView {
    private Long cartItemId;
    private Long userId;      // 方便除錯（DAO 有 SELECT 就填，沒有可不填）
    private Long productId;
    private String name;
    private Integer quantity;

    // 兩市場的上/中/下價（DAO 查出，Service 用來計價）
    private BigDecimal market1High;
    private BigDecimal market1Mid;
    private BigDecimal market1Low;
    private BigDecimal market2High;
    private BigDecimal market2Mid;
    private BigDecimal market2Low;

    // 由 Service 計算後回填給前端
    private BigDecimal price;     // 單價（依區間＋兩市場取高/低）
    private BigDecimal subtotal;  // 小計 = price * quantity

    // ===== getters / setters =====
    public Long getCartItemId() { return cartItemId; }
    public void setCartItemId(Long cartItemId) { this.cartItemId = cartItemId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getMarket1High() { return market1High; }
    public void setMarket1High(BigDecimal market1High) { this.market1High = market1High; }

    public BigDecimal getMarket1Mid() { return market1Mid; }
    public void setMarket1Mid(BigDecimal market1Mid) { this.market1Mid = market1Mid; }

    public BigDecimal getMarket1Low() { return market1Low; }
    public void setMarket1Low(BigDecimal market1Low) { this.market1Low = market1Low; }

    public BigDecimal getMarket2High() { return market2High; }
    public void setMarket2High(BigDecimal market2High) { this.market2High = market2High; }

    public BigDecimal getMarket2Mid() { return market2Mid; }
    public void setMarket2Mid(BigDecimal market2Mid) { this.market2Mid = market2Mid; }

    public BigDecimal getMarket2Low() { return market2Low; }
    public void setMarket2Low(BigDecimal market2Low) { this.market2Low = market2Low; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
