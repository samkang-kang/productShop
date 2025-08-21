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

    public List<Map<String, Object>> getCartItems(int userId) {
        String sql = """
            SELECT 
                c.id AS cart_item_id,
                p.id AS product_id,
                p.name,
                p.market1_mid_price,
                p.market2_mid_price,
                c.quantity
            FROM cart c
            JOIN products p ON c.product_id = p.id
            WHERE c.user_id = :userId
        """;
        Map<String, Object> params = Map.of("userId", userId);
        return jdbcTemplate.queryForList(sql, params);
    }
}
