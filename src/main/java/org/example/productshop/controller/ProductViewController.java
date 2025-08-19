package org.example.productshop.controller;

import org.example.productshop.dao.ProductViewDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductViewController {

    @Autowired private ProductViewDao viewDao;

    // GET /api/products/view
    // GET /api/products/view?codes=A1,31
    @GetMapping("/view")
    public List<Map<String,Object>> view(@RequestParam(value="codes", required=false) String codes){
        Set<String> codeSet = (codes==null||codes.isBlank())
                ? Collections.emptySet()
                : new LinkedHashSet<>(Arrays.asList(codes.split(",")));
        return codeSet.isEmpty() ? viewDao.findAll() : viewDao.findByCodes(codeSet);
    }
}