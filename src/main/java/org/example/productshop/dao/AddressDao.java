package org.example.productshop.dao;

import org.example.productshop.entity.Address;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class AddressDao {

    private final JdbcTemplate jdbc;

    public AddressDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(Address a) {
        String sql = """
            INSERT INTO addresses
            (member_id, recipient_name, phone, city, district, postal_code, detail_address, is_default)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[]{"id"});
            ps.setInt(1, a.getMemberId());
            ps.setString(2, a.getRecipientName());
            ps.setString(3, a.getPhone());
            ps.setString(4, a.getCity());
            ps.setString(5, a.getDistrict());
            ps.setString(6, a.getPostalCode());
            ps.setString(7, a.getDetailAddress());
            ps.setInt(8, (a.getIsDefault() != null && a.getIsDefault()) ? 1 : 0);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? 0L : key.longValue();
    }

    public List<Address> listByMemberId(int memberId) {
        String sql = """
            SELECT id, member_id, recipient_name, phone, city, district, postal_code, detail_address, is_default
            FROM addresses
            WHERE member_id = ?
            ORDER BY is_default DESC, id DESC
        """;
        return jdbc.query(sql, (rs, i) -> {
            Address a = new Address();
            a.setId(rs.getLong("id"));
            a.setMemberId(rs.getInt("member_id"));
            a.setRecipientName(rs.getString("recipient_name"));
            a.setPhone(rs.getString("phone"));
            a.setCity(rs.getString("city"));
            a.setDistrict(rs.getString("district"));
            a.setPostalCode(rs.getString("postal_code"));
            a.setDetailAddress(rs.getString("detail_address"));
            a.setIsDefault(rs.getInt("is_default") == 1);
            return a;
        }, memberId);
    }

    public void clearDefaultByMemberId(int memberId) {
        jdbc.update("UPDATE addresses SET is_default = 0 WHERE member_id = ?", memberId);
    }

    public int setDefaultById(int memberId, long addressId) {
        // 先全部歸零，再把指定 id 設為預設
        clearDefaultByMemberId(memberId);
        return jdbc.update("UPDATE addresses SET is_default = 1 WHERE id = ? AND member_id = ?", addressId, memberId);
    }

    public int deleteByIdAndMember(long addressId, int memberId) {
        return jdbc.update("DELETE FROM addresses WHERE id = ? AND member_id = ?", addressId, memberId);
    }

    public Address findByIdAndMember(long id, int memberId) {
        String sql = """
            SELECT id, member_id, recipient_name, phone, city, district, postal_code, detail_address, is_default
            FROM addresses WHERE id = ? AND member_id = ?
        """;
        try {
            return jdbc.queryForObject(sql, (rs, i) -> {
                Address a = new Address();
                a.setId(rs.getLong("id"));
                a.setMemberId(rs.getInt("member_id"));
                a.setRecipientName(rs.getString("recipient_name"));
                a.setPhone(rs.getString("phone"));
                a.setCity(rs.getString("city"));
                a.setDistrict(rs.getString("district"));
                a.setPostalCode(rs.getString("postal_code"));
                a.setDetailAddress(rs.getString("detail_address"));
                a.setIsDefault(rs.getInt("is_default") == 1);
                return a;
            }, id, memberId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}

