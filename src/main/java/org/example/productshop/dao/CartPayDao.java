package org.example.productshop.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CartPayDao {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getCartItems(long userId) {
        String sql = """
            SELECT 
                c.id AS cart_item_id,
                p.name,
                c.quantity,
                c.price
            FROM shopping_cart_items c
            JOIN products p ON c.product_id = p.id
            WHERE c.user_id = :userId
        """;
        return jdbcTemplate.queryForList(sql, Map.of("userId", userId));
    }
}
