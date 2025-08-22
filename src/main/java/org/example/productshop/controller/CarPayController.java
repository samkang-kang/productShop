package org.example.productshop.controller;

import org.example.productshop.service.CarPayService;
import org.example.productshop.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CarPayController {

    @Autowired
    private CarPayService carPayService;

    @GetMapping("/api/cart/checkout")
    public Map<String, Object> checkout(@RequestParam int userId) {
        return carPayService.checkout(userId);
    }






}
