package org.example.productshop.dao;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class ProductReadMarketDao {
    private final NamedParameterJdbcTemplate jdbc;

    public ProductReadMarketDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ProductRow findById(long id) {
        String sql = """
            SELECT id,
                   name,
                   market1_code, market1_name, market1_unit,
                   market1_high_price, market1_mid_price, market1_low_price, market1_trade_date,
                   market2_code, market2_name, market2_unit,
                   market2_high_price, market2_mid_price, market2_low_price, market2_trade_date
            FROM products
            WHERE id = :id
            """;
        var params = new MapSqlParameterSource("id", id);
        var list = jdbc.query(sql, params, mapper);
        return list.isEmpty() ? null : list.get(0);
    }

    private static final RowMapper<ProductRow> mapper = (rs, i) -> {
        ProductRow r = new ProductRow();
        r.setId(rs.getLong("id"));
        r.setName(rs.getString("name"));

        r.setM1Code(rs.getString("market1_code"));
        r.setM1Name(rs.getString("market1_name"));
        r.setM1Unit(rs.getString("market1_unit"));
        r.setM1High(rs.getBigDecimal("market1_high_price"));
        r.setM1Mid(rs.getBigDecimal("market1_mid_price"));
        r.setM1Low(rs.getBigDecimal("market1_low_price"));
        r.setM1Date(rs.getString("market1_trade_date"));

        r.setM2Code(rs.getString("market2_code"));
        r.setM2Name(rs.getString("market2_name"));
        r.setM2Unit(rs.getString("market2_unit"));
        r.setM2High(rs.getBigDecimal("market2_high_price"));
        r.setM2Mid(rs.getBigDecimal("market2_mid_price"));
        r.setM2Low(rs.getBigDecimal("market2_low_price"));
        r.setM2Date(rs.getString("market2_trade_date"));
        return r;
    };

    /** JDBC DTO（不扯到 JPA） */
    public static class ProductRow {
        private long id; private String name;

        private String m1Code; private String m1Name; private String m1Unit;
        private BigDecimal m1High; private BigDecimal m1Mid; private BigDecimal m1Low; private String m1Date;

        private String m2Code; private String m2Name; private String m2Unit;
        private BigDecimal m2High; private BigDecimal m2Mid; private BigDecimal m2Low; private String m2Date;

        // ---- getters / setters（全給你，免得你又說少一個就編不過） ----
        public long getId() { return id; } public void setId(long id) { this.id = id; }
        public String getName() { return name; } public void setName(String name) { this.name = name; }

        public String getM1Code() { return m1Code; } public void setM1Code(String m1Code) { this.m1Code = m1Code; }
        public String getM1Name() { return m1Name; } public void setM1Name(String m1Name) { this.m1Name = m1Name; }
        public String getM1Unit() { return m1Unit; } public void setM1Unit(String m1Unit) { this.m1Unit = m1Unit; }
        public BigDecimal getM1High() { return m1High; } public void setM1High(BigDecimal m1High) { this.m1High = m1High; }
        public BigDecimal getM1Mid() { return m1Mid; } public void setM1Mid(BigDecimal m1Mid) { this.m1Mid = m1Mid; }
        public BigDecimal getM1Low() { return m1Low; } public void setM1Low(BigDecimal m1Low) { this.m1Low = m1Low; }
        public String getM1Date() { return m1Date; } public void setM1Date(String m1Date) { this.m1Date = m1Date; }

        public String getM2Code() { return m2Code; } public void setM2Code(String m2Code) { this.m2Code = m2Code; }
        public String getM2Name() { return m2Name; } public void setM2Name(String m2Name) { this.m2Name = m2Name; }
        public String getM2Unit() { return m2Unit; } public void setM2Unit(String m2Unit) { this.m2Unit = m2Unit; }
        public BigDecimal getM2High() { return m2High; } public void setM2High(BigDecimal m2High) { this.m2High = m2High; }
        public BigDecimal getM2Mid() { return m2Mid; } public void setM2Mid(BigDecimal m2Mid) { this.m2Mid = m2Mid; }
        public BigDecimal getM2Low() { return m2Low; } public void setM2Low(BigDecimal m2Low) { this.m2Low = m2Low; }
        public String getM2Date() { return m2Date; } public void setM2Date(String m2Date) { this.m2Date = m2Date; }
    }
}
