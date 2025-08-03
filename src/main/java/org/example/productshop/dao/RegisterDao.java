package org.example.productshop.dao;

import org.example.productshop.entity.Member;
import org.springframework.stereotype.Component;

@Component
public interface RegisterDao {
    String register(Member member);
}
