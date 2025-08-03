package org.example.productshop.service;

import org.example.productshop.dao.RegisterDao;
import org.example.productshop.dao.RegisterDaoImpel;
import org.example.productshop.entity.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RegisterServiceImpel implements RegisterService {

    @Autowired
    private RegisterDao registerDao;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private EmailService emailService;


    @Override
    public String register(Member member) {
        return registerDao.register(member);

    }


}
