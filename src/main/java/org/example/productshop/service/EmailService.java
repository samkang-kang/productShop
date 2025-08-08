package org.example.productshop.service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String token) {
        String verifyUrl = "http://localhost:8080/users/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("會員驗證信");
        message.setText("請點擊以下連結完成帳號啟用：\n" + verifyUrl);

        mailSender.send(message);
    }
}
