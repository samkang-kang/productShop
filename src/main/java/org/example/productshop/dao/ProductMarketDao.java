package org.example.productshop.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public class ProductMarketDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    /** 依 name 找 id；沒有就 INSERT 並回傳自增 id */
    public long insertProductIfNotExists(String name) {
        String q = "SELECT id FROM products WHERE name=:name LIMIT 1";
        Long id = jdbc.query(q, new MapSqlParameterSource("name", name),
                rs -> rs.next()? rs.getLong(1): null);
        if (id != null) return id;

        String ins = "INSERT INTO products(name, stock_quantity) VALUES(:name, 0)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(ins, new MapSqlParameterSource("name", name), kh, new String[]{"id"});
        Number key = kh.getKey();
        if (key != null) return key.longValue();

        Long again = jdbc.query(q, new MapSqlParameterSource("name", name),
                rs -> rs.next()? rs.getLong(1): null);
        if (again != null) return again;
        throw new IllegalStateException("Insert products 失敗：取不到 id (name="+name+")");
    }

    /** 占位時找既有商品 */
    public Long findExistingIdByMarketCodeOrName(String code, String fallbackName) {
        String sql = """
          SELECT id FROM products 
          WHERE market1_code=:code OR market2_code=:code OR name=:name
          LIMIT 1
        """;
        return jdbc.query(sql, new MapSqlParameterSource()
                        .addValue("code", code).addValue("name", fallbackName),
                rs -> rs.next()? rs.getLong(1) : null);
    }

    public String findNameById(long id) {
        String sql = "SELECT name FROM products WHERE id=:id";
        return jdbc.query(sql, new MapSqlParameterSource("id", id),
                rs -> rs.next()? rs.getString(1) : null);
    }

    public int updateMarket1(long id, String code, String marketName,
                             BigDecimal high, BigDecimal mid, BigDecimal low,
                             String unit, LocalDate tradeDate, LocalDateTime now) {
        String sql = """
          UPDATE products SET
            market1_code=:code,
            market1_name=:marketName,
            market1_high_price=:high,
            market1_mid_price=:mid,
            market1_low_price=:low,
            market1_unit=:unit,
            market1_trade_date=:tradeDate,
            market1_last_synced_at=:now
          WHERE id=:id
        """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id).addValue("code", code).addValue("marketName", marketName)
                .addValue("high", high).addValue("mid", mid).addValue("low", low)
                .addValue("unit", unit).addValue("tradeDate", tradeDate).addValue("now", now));
    }

    public int updateMarket2(long id, String code, String marketName,
                             BigDecimal high, BigDecimal mid, BigDecimal low,
                             String unit, LocalDate tradeDate, LocalDateTime now) {
        String sql = """
          UPDATE products SET
            market2_code=:code,
            market2_name=:marketName,
            market2_high_price=:high,
            market2_mid_price=:mid,
            market2_low_price=:low,
            market2_unit=:unit,
            market2_trade_date=:tradeDate,
            market2_last_synced_at=:now
          WHERE id=:id
        """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id).addValue("code", code).addValue("marketName", marketName)
                .addValue("high", high).addValue("mid", mid).addValue("low", low)
                .addValue("unit", unit).addValue("tradeDate", tradeDate).addValue("now", now));
    }
}