package org.example.productshop.service;

import org.example.productshop.dao.ShoppingCartItemDao;
import org.example.productshop.entity.CartItemView;
import org.example.productshop.entity.CartSummary;
import org.example.productshop.entity.ShoppingCartItem;
import org.example.productshop.exception.ItemNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    @Autowired
    private ShoppingCartItemDao cartDao; // 合併成一個欄位，統一使用

    // 加入購物車（保留你的原邏輯）
    public String addToCart(Integer userId, Integer productId, Integer quantity) {
        ShoppingCartItem item = cartDao.findByUserIdAndProductId(userId, productId);
        if (item == null) {
            cartDao.insertCartItem(userId, productId, quantity);
            return "已加入購物車: 產品 " + productId + " x" + quantity;
        } else {
            int newQuantity = item.getQuantity() + quantity;
            cartDao.updateCartItem(userId, productId, newQuantity);
            return "購物車已更新數量: 產品 " + productId + " x" + newQuantity;
        }
    }

    // 移除購物車
    public int removeCartItem(long userId, long cartItemId) {
        int rows = cartDao.deleteCartItem(userId, cartItemId);
        if (rows == 0) throw new ItemNotFoundException("購物車中沒有此商品");
        return rows;
    }

    // 更新數量（保留你的庫存檢查）
    public void updateQuantity(long cartItemId, int quantity) {
        Integer stock = cartDao.getStockByCartItemId(cartItemId);
        if (stock == null) throw new RuntimeException("ITEM_NOT_FOUND");
        if (quantity > stock) throw new RuntimeException("OUT_OF_STOCK");

        int rows = cartDao.updateCartItemQuantity(cartItemId, quantity);
        if (rows == 0) throw new RuntimeException("ITEM_NOT_FOUND");
    }

    // ★★ 關鍵：不要拋 CartEmptyException；回空清單＋總額 0，避免 /api/cart/{userId} 變 400
    public CartSummary getCart(long userId) {
        List<CartItemView> items = cartDao.listCartItemsByUserId(userId);
        if (items == null || items.isEmpty()) {
            return new CartSummary(List.of(), BigDecimal.ZERO);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItemView it : items) {
            // 小計優先用 DAO 提供的 subtotal；若沒有就用 price*quantity 計算
            BigDecimal line = it.getSubtotal();
            if (line == null) {
                BigDecimal price = it.getPrice() != null ? it.getPrice() : BigDecimal.ZERO;
                int qty = it.getQuantity() != null ? it.getQuantity() : 0;
                line = price.multiply(BigDecimal.valueOf(qty));
            }
            total = total.add(line);
        }
        return new CartSummary(items, total);
    }
}
