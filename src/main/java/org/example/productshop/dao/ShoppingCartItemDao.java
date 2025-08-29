package org.example.productshop.dao;

import org.example.productshop.entity.CartItemView;
import org.example.productshop.entity.ShoppingCartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ShoppingCartItemDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    // 檢查有沒有該筆購物車紀錄（不用更改）
    public ShoppingCartItem findByUserIdAndProductId(Integer userId, Integer productId) {
        String sql = "SELECT * FROM shopping_cart_items WHERE user_id = :userId AND product_id = :productId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("productId", productId);

        return jdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(ShoppingCartItem.class))
                .stream().findFirst().orElse(null);
    }

//    // 新增購物車（不用更改）
//    public void insertCartItem(Integer userId, Integer productId, Integer quantity) {
//        String sql = "INSERT INTO shopping_cart_items(user_id, product_id, quantity) " +
//                "VALUES(:userId, :productId, :quantity)";
//        MapSqlParameterSource params = new MapSqlParameterSource();
//        params.addValue("userId", userId);
//        params.addValue("productId", productId);
//        params.addValue("quantity", quantity);
//        jdbcTemplate.update(sql, params);
//    }

    // ✨ 新增購物車（含 price）
    public void insertCartItem(Integer userId, Integer productId, Integer quantity, java.math.BigDecimal price) {
                String sql = "INSERT INTO shopping_cart_items(user_id, product_id, quantity, price) " +
                                "VALUES(:userId, :productId, :quantity, :price)";
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("userId", userId);
                params.addValue("productId", productId);
                params.addValue("quantity", quantity);
                params.addValue("price", price);
                jdbcTemplate.update(sql, params);
            }

    // 更新購物車數量（以 userId + productId 定位）（不用更改）
    public void updateCartItem(Integer userId, Integer productId, Integer quantity) {
        String sql = "UPDATE shopping_cart_items SET quantity = :quantity " +
                "WHERE user_id = :userId AND product_id = :productId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("quantity", quantity);
        params.addValue("userId", userId);
        params.addValue("productId", productId);
        jdbcTemplate.update(sql, params);
    }

    // 放在 ShoppingCartItemDao 裡（和其他方法同一層）
    public int updateCartItemQuantity(long userId, long cartItemId, int quantity) {
        String sql = "UPDATE shopping_cart_items SET quantity = :quantity " +
                "WHERE id = :id AND user_id = :userId";
        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("quantity", quantity)
                .addValue("id", cartItemId)
                .addValue("userId", userId));
    }

    // 刪除購物車商品（已有 userId 條件，不用更改）
    public int deleteCartItem(long userId, long cartItemId) {
        String sql = """
            DELETE FROM shopping_cart_items
            WHERE id = :id AND user_id = :userId
        """;
        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", cartItemId)
                .addValue("userId", userId));
    }

    // ✅ 更新購物車商品數量：加入 userId 條件（避免誤改他人商品）
//    public int updateCartItemQuantity(long userId, long cartItemId, int quantity) {
//        String sql = "UPDATE shopping_cart_items SET quantity = :quantity WHERE id = :id AND user_id = :userId";
//        return jdbcTemplate.update(sql, new MapSqlParameterSource()
//                .addValue("quantity", quantity)
//                .addValue("id", cartItemId)
//                .addValue("userId", userId));
//    }

    public void updateCartItem(Integer userId, Integer productId, Integer quantity, java.math.BigDecimal price) {
                String sql = "UPDATE shopping_cart_items SET quantity = :quantity, price = :price " +
                                "WHERE user_id = :userId AND product_id = :productId";
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("quantity", quantity);
                params.addValue("price", price);
                params.addValue("userId", userId);
                params.addValue("productId", productId);
                jdbcTemplate.update(sql, params);
            }

    // ✅ 讀庫存：加入 userId 條件（更安全）
    public Integer getStockByCartItemId(long userId, long cartItemId) {
        String sql = """
            SELECT p.stock_quantity
            FROM shopping_cart_items c
            JOIN products p ON c.product_id = p.id
            WHERE c.id = :id AND c.user_id = :userId
        """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource()
                        .addValue("id", cartItemId)
                        .addValue("userId", userId),
                rs -> rs.next() ? (Integer) rs.getObject("stock_quantity") : null);
    }

    // ★ 顯示購物車已加入商品（不用更改）
    public List<CartItemView> listCartItemsByUserId(long userId) {
        String sql = """
            SELECT
                c.id           AS cart_item_id,
                c.user_id      AS user_id,
                c.product_id   AS product_id,
                c.quantity     AS quantity,
                p.name         AS name,
                p.market1_high_price, p.market1_mid_price, p.market1_low_price,
                p.market2_high_price, p.market2_mid_price, p.market2_low_price
            FROM shopping_cart_items c
            JOIN products p ON p.id = c.product_id
            WHERE c.user_id = :userId
            ORDER BY c.id
        """;

        RowMapper<CartItemView> mapper = (rs, i) -> {
            CartItemView v = new CartItemView();
            v.setCartItemId(rs.getLong("cart_item_id"));
            v.setUserId(rs.getLong("user_id"));
            v.setProductId(rs.getLong("product_id"));
            v.setName(rs.getString("name"));
            v.setQuantity(rs.getInt("quantity"));

            v.setMarket1High(rs.getBigDecimal("market1_high_price"));
            v.setMarket1Mid (rs.getBigDecimal("market1_mid_price"));
            v.setMarket1Low (rs.getBigDecimal("market1_low_price"));
            v.setMarket2High(rs.getBigDecimal("market2_high_price"));
            v.setMarket2Mid (rs.getBigDecimal("market2_mid_price"));
            v.setMarket2Low (rs.getBigDecimal("market2_low_price"));
            return v;
        };

        return jdbcTemplate.query(sql, new MapSqlParameterSource("userId", userId), mapper);
    }
}
