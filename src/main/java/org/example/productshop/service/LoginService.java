package org.example.productshop.service;

import org.example.productshop.api.BCrypt;
import org.example.productshop.api.JwtUtil;
import org.example.productshop.dao.LoginDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginService {

    @Autowired
    private LoginDao loginDao ;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private BCrypt bcrypt ;

    public String  login(String email, String password){
        String dbHash = loginDao.getHashpassword(email);
        if(dbHash!=null && BCrypt.checkpw(password, dbHash)){
            return jwtUtil.generateToken(email);
        }
        return null;


    }





}
