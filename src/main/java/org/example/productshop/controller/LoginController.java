package org.example.productshop.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import org.example.productshop.dao.LoginDao;
import org.example.productshop.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
//@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    LoginService loginService;
    @Autowired
    LoginDao loginDao;

    @PostMapping("/users/login")
    public Map<String, Object> login(@RequestBody Map<String,String> request) {
        String email = request.get("email");
        String password = request.get("password");
        //產生token
        String token = loginService.login(email, password);
        String isemail = loginService.checkEmail(email);
        HashMap<String, Object> response = new HashMap<>();
        if(isemail == null) {
            response.put("status", "failOfEmail");
            response.put("message", "尚未註冊");
            return response;
        }

        if(!"active".equals(loginDao.checkStatus(email))){
            response.put("status", "fail");
            return response ;
        }

        if(token != null) {
            response.put("status", "success");
            response.put("message", "登入成功");
            response.put("token", token);
        }else{
            response.put("status", "fail");
            response.put("message", "帳號或密碼錯誤");

        }
        return response;

    }


//    @GetMapping("/secure/test")
//    public String testSecure(HttpServletRequest request) {
//        return "Hello, " + request.getAttribute("userEmail");
//    }

    @GetMapping("/api/me")
    public Map<String, Object> me(@RequestAttribute("userEmail") String email) {
        return Map.of("email", email);
    }

}
