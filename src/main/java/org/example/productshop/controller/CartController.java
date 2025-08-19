package org.example.productshop.controller;

import org.example.productshop.entity.CartRequest;
import org.example.productshop.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired
    private CartService cartService;

    // 加入購物車
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody CartRequest request) {
        try {
            String msg = cartService.addToCart(request.getUserId(), request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error","RUNTIME_ERROR",
                    "message","執行期間發生錯誤"
            ));
        }
    }

    // 以 GET 移除（相容既有前端）
    @GetMapping("/remove")
    public ResponseEntity<?> removeByGet(
            @RequestParam("cartItemId") long cartItemId,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        if (cartItemId <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_PARAM", "message", "cartItemId 無效"
            ));
        }
        try {
            int rows = cartService.removeCartItem(userId != null ? userId : 0L, cartItemId);
            if (rows <= 0) {
                return ResponseEntity.status(404).body(Map.of(
                        "error", "CART_ITEM_NOT_FOUND", "message", "找不到購物車商品或無權限"
                ));
            }
            return ResponseEntity.ok(Map.of("message", "商品已從購物車刪除"));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "CONSTRAINT_VIOLATION", "message", "受資料表關聯限制，無法刪除"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "INTERNAL_ERROR", "message", "執行期間發生錯誤"
            ));
        }
    }

    // 以 DELETE 移除（RESTful）
    @DeleteMapping("/remove/{userId}")
    public ResponseEntity<?> remove(
            @PathVariable long userId,
            @RequestParam("cartItemId") long cartItemId
    ) {
        int rows = cartService.removeCartItem(userId, cartItemId);
        return ResponseEntity.ok(Map.of("message", "商品已從購物車刪除"));
    }

    // 購物車商品數量增減
    @PutMapping("/update")
    public ResponseEntity<?> updateQuantity(@RequestBody Map<String, Object> body) {
        long cartItemId = ((Number) body.get("cartItemId")).longValue();
        int quantity = ((Number) body.get("quantity")).intValue();

        try {
            cartService.updateQuantity(cartItemId, quantity);
            return ResponseEntity.ok(Map.of("message", "商品數量已更新"));
        } catch (RuntimeException e) {
            if ("OUT_OF_STOCK".equals(e.getMessage())) {
                return ResponseEntity.status(400).body(Map.of(
                        "error", "OUT_OF_STOCK", "message", "商品庫存不足"
                ));
            } else if ("ITEM_NOT_FOUND".equals(e.getMessage())) {
                return ResponseEntity.status(404).body(Map.of(
                        "error", "ITEM_NOT_FOUND", "message", "購物車中沒有此商品"
                ));
            }
            throw e;
        }
    }

    // 顯示已加入購物車商品
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable long userId) {
        try {
            var summary = cartService.getCart(userId);
            return ResponseEntity.ok(Map.of(
                    "items", summary.getItems(),
                    "totalPrice", summary.getTotalPrice()
                    // 如果你的 CartSummary 用的是 getTotal()，改用下面這行：
                    // "totalPrice", summary.getTotal()
            ));
        } catch (Exception e) {
            e.printStackTrace(); // 看真正 SQL/欄位錯
            return ResponseEntity.status(500).body(Map.of(
                    "error","INTERNAL_ERROR",
                    "message", e.getMessage()
            ));
        }
    }
}
