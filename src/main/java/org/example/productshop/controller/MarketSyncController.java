package org.example.productshop.controller;

import org.example.productshop.dao.ProductViewDao;
import org.example.productshop.service.OpenDataMarketSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class MarketSyncController {

    @Autowired private OpenDataMarketSyncService service;
    @Autowired private ProductViewDao viewDao;

    // 單純同步（保留）
    // GET /api/products/markets/sync
    // GET /api/products/markets/sync?codes=A1,31
    @GetMapping("/markets/sync")
    public ResponseEntity<?> sync(@RequestParam(value="codes", required=false) String codes) {
        int n = service.sync(codes);
        return ResponseEntity.ok(Map.of("message","市場價同步完成","updated", n));
    }

    // 合併：先同步，再回傳最新資料
    // GET /api/products/markets/sync-view
    // GET /api/products/markets/sync-view?codes=A1,31
    @GetMapping("/markets/sync-view")
    public ResponseEntity<?> syncAndView(@RequestParam(value="codes", required=false) String codes) {
        int updated = service.sync(codes);
        Set<String> codeSet = splitCodes(codes);
        var items = codeSet.isEmpty() ? viewDao.findAll() : viewDao.findByCodes(codeSet);
        return ResponseEntity.ok(Map.of(
                "updated", updated,
                "count", items.size(),
                "items", items
        ));
    }

    private static Set<String> splitCodes(String codes) {
        if (codes == null || codes.isBlank()) return Collections.emptySet();
        return Arrays.stream(codes.split(","))
                .map(String::trim).filter(s->!s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}