package org.example.productshop.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.productshop.api.JwtUtil;
import org.example.productshop.dao.MemberDao;
import org.example.productshop.entity.Address;
import org.example.productshop.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin(origins = "*")
public class AddressController {

    private final AddressService addressService;
    private final MemberDao memberDao;
    private final JwtUtil jwtUtil; // ✅ 方案A關鍵：自行解析 Authorization

    public AddressController(AddressService addressService, MemberDao memberDao, JwtUtil jwtUtil) {
        this.addressService = addressService;
        this.memberDao = memberDao;
        this.jwtUtil = jwtUtil;
    }

    // ====== 共用：從 request attribute 或 Authorization header 解析 memberId ======
    private Integer resolveMemberId(HttpServletRequest request, String authorizationHeader) {
        // 1) 先用 JwtFilter 放進來的 userEmail（若走白名單可能為 null）
        String email = (String) request.getAttribute("userEmail");

        // 2) 若為 null，自己從 Authorization: Bearer <token> 解析一次
        if (email == null && authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    email = jwtUtil.getEmailFromToken(token);
                }
            } catch (Exception ignored) {}
        }

        if (email == null) return null;
        return memberDao.findIdByEmail(email);
    }

    // ====== 新增地址 ======
    @PostMapping("/add")
    public ResponseEntity<?> addAddress(
            @RequestBody Address address,
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Integer memberId = resolveMemberId(request, authorization);
        if (memberId == null) {
            return ResponseEntity.status(401).body("{\"error\":\"Unauthorized\"}");
        }

        address.setMemberId(memberId);
        long id = addressService.create(address);
        return ResponseEntity.ok("{\"message\":\"地址新增成功\",\"id\":" + id + "}");
    }

    // ====== 取得我的地址列表 ======
    @GetMapping("/my")
    public ResponseEntity<?> myAddresses(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Integer memberId = resolveMemberId(request, authorization);
        if (memberId == null) {
            return ResponseEntity.status(401).body("{\"error\":\"Unauthorized\"}");
        }
        return ResponseEntity.ok(addressService.listMy(memberId));
    }

    // ====== 設為預設地址 ======
    @PutMapping("/{id}/default")
    public ResponseEntity<?> setDefault(
            @PathVariable("id") long id,
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Integer memberId = resolveMemberId(request, authorization);
        if (memberId == null) {
            return ResponseEntity.status(401).body("{\"error\":\"Unauthorized\"}");
        }
        boolean ok = addressService.setDefault(memberId, id);
        if (!ok) return ResponseEntity.status(404).body("{\"error\":\"NOT_FOUND\"}");
        return ResponseEntity.ok("{\"message\":\"已設為預設地址\"}");
    }

    // ====== 刪除我的地址 ======
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable("id") long id,
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Integer memberId = resolveMemberId(request, authorization);
        if (memberId == null) {
            return ResponseEntity.status(401).body("{\"error\":\"Unauthorized\"}");
        }
        boolean ok = addressService.deleteMy(memberId, id);
        if (!ok) return ResponseEntity.status(404).body("{\"error\":\"NOT_FOUND\"}");
        return ResponseEntity.ok("{\"message\":\"已刪除\"}");
    }
}