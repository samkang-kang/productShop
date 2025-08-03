package org.example.productshop.controller;


import org.example.productshop.api.BCrypt;
import org.example.productshop.dao.RegisterDao;
import org.example.productshop.dao.VerifyEmailDao;
import org.example.productshop.entity.Member;
import org.example.productshop.service.EmailService;
import org.example.productshop.service.RegisterService;
import org.example.productshop.service.RegisterServiceImpel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class RegisterController {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private RegisterServiceImpel registerService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private VerifyEmailDao verifyEmailDao;


    @PostMapping("/register") //
    public String test(@RequestBody Member member) {

        return registerService.register(member);
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("token") String token) {

        return verifyEmailDao.verifyEmail(token);
    }

    @PostMapping("/resend-verification")
    public String resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        return verifyEmailDao.resendVerification(email);
    }

}





