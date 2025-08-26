package org.example.productshop.controller;

import org.example.productshop.dao.UserDao;
import org.example.productshop.entity.CartRequest;
import org.example.productshop.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserDao userDao; // 新增

    /** 取當前 userId：優先 token，其次 body/path/param，最後 email 轉 id（應對白名單） */
    private Long resolveUserId(HttpServletRequest req, Integer userIdInBodyOrPath) {
        Object uidAttr = req.getAttribute("userId"); // 非白名單時 JwtFilter 會塞
        if (uidAttr instanceof Long) return (Long) uidAttr;
        if (userIdInBodyOrPath != null) return userIdInBodyOrPath.longValue();

        // 白名單路由可能沒塞 userId；若仍帶了 Authorization（但白名單不解 token），就嘗試 userEmail
        Object emailAttr = req.getAttribute("userEmail");
        if (emailAttr instanceof String email) {
            Long uid = userDao.findIdByEmail(email);
            if (uid != null) return uid;
        }
        return null; // 真的沒有任何線索
    }

    // 加入購物車（白名單相容：可 body 帶 userId；若有 token 的受保護情境則直接用 token）
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody CartRequest request, HttpServletRequest req) {
        Long uid = resolveUserId(req, request.getUserId());
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error","AUTH_REQUIRED_OR_USERID_MISSING",
                    "message","請登入或於 body 提供 userId"
            ));
        }
        try {
            String msg = cartService.addToCart(uid.intValue(), request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error","RUNTIME_ERROR","message","執行期間發生錯誤"));
        }
    }

    // 取得目前登入者購物車（受保護：需帶 Authorization）
    @GetMapping("/me")
    public ResponseEntity<?> getMyCart(HttpServletRequest req) {
        Long uid = resolveUserId(req, null);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("error","AUTH_REQUIRED","message","請登入"));
        }
        try {
            var summary = cartService.getCart(uid);
            return ResponseEntity.ok(Map.of("items", summary.getItems(), "totalPrice", summary.getTotalPrice()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error","INTERNAL_ERROR","message", e.getMessage()));
        }
    }

    // 相容舊前端：GET 移除（白名單，相容 ?userId=；若有 token 也能用）
    @GetMapping("/remove")
    public ResponseEntity<?> removeByGet(@RequestParam("cartItemId") long cartItemId,
                                         @RequestParam(value = "userId", required = false) Integer userIdParam,
                                         HttpServletRequest req) {
        Long uid = resolveUserId(req, userIdParam);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("error","AUTH_REQUIRED_OR_USERID_MISSING","message","請登入或提供 userId"));
        }
        try {
            int rows = cartService.removeCartItem(uid, cartItemId);
            if (rows <= 0) {
                return ResponseEntity.status(404).body(Map.of("error","CART_ITEM_NOT_FOUND","message","找不到購物車商品或無權限"));
            }
            return ResponseEntity.ok(Map.of("message","商品已從購物車刪除","affected", rows));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(Map.of("error","CONSTRAINT_VIOLATION","message","受資料表關聯限制，無法刪除"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error","INTERNAL_ERROR","message","執行期間發生錯誤"));
        }
    }

    // RESTful 刪除（也相容舊版 path userId）
    @DeleteMapping("/remove/{userId}")
    public ResponseEntity<?> remove(@PathVariable Integer userId,
                                    @RequestParam("cartItemId") long cartItemId,
                                    HttpServletRequest req) {
        Long uid = resolveUserId(req, userId);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("error","AUTH_REQUIRED_OR_USERID_MISSING","message","請登入或提供 userId"));
        }
        int rows = cartService.removeCartItem(uid, cartItemId);
        return ResponseEntity.ok(Map.of("message","商品已從購物車刪除","affected", rows));
    }

    // 購物車商品數量增減（受保護：需帶 Authorization）
    @PutMapping("/update")
    public ResponseEntity<?> updateQuantity(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long uid = resolveUserId(req, null);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("error","AUTH_REQUIRED","message","請登入"));
        }
        long cartItemId = ((Number) body.get("cartItemId")).longValue();
        int quantity = ((Number) body.get("quantity")).intValue();

        try {
            cartService.updateQuantity(uid, cartItemId, quantity); // 改簽名（帶 userId）
            return ResponseEntity.ok(Map.of("message","商品數量已更新"));
        } catch (RuntimeException e) {
            if ("OUT_OF_STOCK".equals(e.getMessage())) {
                return ResponseEntity.status(400).body(Map.of("error","OUT_OF_STOCK","message","商品庫存不足"));
            } else if ("ITEM_NOT_FOUND".equals(e.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("error","ITEM_NOT_FOUND","message","購物車中沒有此商品"));
            }
            throw e;
        }
    }

    // （相容舊前端）/api/cart/{userId}
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Integer userId, HttpServletRequest req) {
        Long uid = resolveUserId(req, userId);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("error","AUTH_REQUIRED_OR_USERID_MISSING","message","請登入或提供 userId"));
        }
        try {
            var summary = cartService.getCart(uid);
            return ResponseEntity.ok(Map.of("items", summary.getItems(), "totalPrice", summary.getTotalPrice()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error","INTERNAL_ERROR","message", e.getMessage()));
        }
    }
}