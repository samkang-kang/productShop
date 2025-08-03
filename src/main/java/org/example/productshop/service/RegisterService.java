package org.example.productshop.service;

import org.example.productshop.entity.Member;
import org.springframework.stereotype.Component;

@Component
public interface RegisterService {
    String register(Member member);
}
