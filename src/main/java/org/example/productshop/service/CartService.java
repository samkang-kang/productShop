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
    private ShoppingCartItemDao cartDao;

    // 加入購物車（不用更改）
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

    // 刪除（不用更改）
    public int removeCartItem(long userId, long cartItemId) {
        int rows = cartDao.deleteCartItem(userId, cartItemId);
        if (rows == 0) throw new ItemNotFoundException("購物車中沒有此商品");
        return rows;
    }

    // ✅ 更新數量：帶 userId + cartItemId
    public void updateQuantity(long userId, long cartItemId, int quantity) {
        Integer stock = cartDao.getStockByCartItemId(userId, cartItemId);
        if (stock == null) throw new RuntimeException("ITEM_NOT_FOUND");
        if (quantity > stock) throw new RuntimeException("OUT_OF_STOCK");

        int rows = cartDao.updateCartItemQuantity(userId, cartItemId, quantity);
        if (rows == 0) throw new RuntimeException("ITEM_NOT_FOUND");
    }

    // 取購物車內容（不用更改）
    public CartSummary getCart(long userId) {
        List<CartItemView> items = cartDao.listCartItemsByUserId(userId);
        if (items == null || items.isEmpty()) {
            return new CartSummary(List.of(), BigDecimal.ZERO);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItemView it : items) {
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