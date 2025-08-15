package org.example.productshop.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ProductViewDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    // 全部
    public List<Map<String,Object>> findAll() {
        String sql = """
          SELECT id, name,
                 market1_code, market1_name, market1_high_price, market1_mid_price, market1_low_price, market1_unit, market1_trade_date,
                 market2_code, market2_name, market2_high_price, market2_mid_price, market2_low_price, market2_unit, market2_trade_date
          FROM products
          ORDER BY name
        """;
        return jdbc.queryForList(sql, Map.of());
    }

    // 依作物代號過濾（比對 market1_code/market2_code 其一）
    public List<Map<String,Object>> findByCodes(Set<String> codes) {
        if (codes == null || codes.isEmpty()) return findAll();
        String sql = """
          SELECT id, name,
                 market1_code, market1_name, market1_high_price, market1_mid_price, market1_low_price, market1_unit, market1_trade_date,
                 market2_code, market2_name, market2_high_price, market2_mid_price, market2_low_price, market2_unit, market2_trade_date
          FROM products
          WHERE market1_code IN (:codes) OR market2_code IN (:codes)
          ORDER BY name
        """;
        return jdbc.queryForList(sql, new MapSqlParameterSource("codes", codes));
    }
}
