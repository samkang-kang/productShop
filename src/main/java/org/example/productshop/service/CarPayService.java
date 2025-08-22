package org.example.productshop.service;

import org.example.productshop.dao.CartPayDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CarPayService {

    @Autowired
    private CartPayDao carpaydao;

    public Map<String, Object> checkout(int userId) {
        List<Map<String, Object>> items = carpaydao.getCartItems(userId);

        // itemName 格式：蘋果 100元X1#香蕉 150元X2
        String itemName = items.stream()
                .map(i -> i.get("name") + " " + i.get("price") + "元X" + i.get("quantity"))
                .collect(Collectors.joining("#"));

        int total = items.stream()
                .mapToInt(i -> ((Number) i.get("price")).intValue() * ((Number) i.get("quantity")).intValue())
                .sum();

        return Map.of(
                "itemName", itemName,
                "amount", total
        );
    }
}