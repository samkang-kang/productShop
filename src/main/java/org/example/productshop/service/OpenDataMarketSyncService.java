package org.example.productshop.service;

import org.example.productshop.dao.ProductMarketDao;
import org.example.productshop.entity.OpenDataMarketItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 規則：
 * 1) 僅台北一/台北二 (normalize T1/T2)
 * 2) 只用作物代號做白名單過濾 (精準相等)，空=不過濾
 * 3) 有價格的品項永遠寫入（即使當天同時有休市公告）
 * 4) 若市場當天完全沒正常品但出現休市公告 → 對該市場的目標代號做「休市占位」（三價 NULL）
 */
@Service
public class OpenDataMarketSyncService {

    private static final String SOURCE =
            "https://data.moa.gov.tw/Service/OpenData/FromM/FarmTransData.aspx";

    @Autowired private RestTemplate rt;
    @Autowired private ProductMarketDao dao;

    @Value("${app.crop-codes:}")
    private String codeWhitelist;

    /** 市場名稱正規化：回 T1/T2/null */
    private static String normalizeMarket(String raw) {
        if (raw == null) return null;
        String s = raw.replace("臺", "台").trim();
        String ns = s.replaceAll("\\s+", "");
        if (Pattern.compile("台北.*(第一|一).*果菜").matcher(ns).find()) return "T1";
        if (s.contains("台北一") || s.contains("台北第一")) return "T1";
        if (Pattern.compile("台北.*(第二|二).*果菜").matcher(ns).find()) return "T2";
        if (s.contains("台北二") || s.contains("台北第二")) return "T2";
        if (s.contains("第一果菜")) return "T1";
        if (s.contains("第二果菜")) return "T2";
        return null;
    }

    /** GET /api/products/markets/sync?codes=A1,31 */
    public int sync(String codesOverride) {
        // ★ 解析代號白名單（精準相等）
        String codesRaw = (codesOverride != null) ? codesOverride : codeWhitelist;
        Set<String> codeSet = Arrays.stream(Objects.toString(codesRaw, "").split(","))
                .map(String::trim).filter(s->!s.isEmpty()).collect(Collectors.toSet());
        System.out.println("[OpenData][cfg] codes=" + codeSet);

        // 抓資料（防快取）
        String url = SOURCE + (SOURCE.contains("?") ? "&" : "?") + "_ts=" + System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        headers.add("Pragma","no-cache");
        ResponseEntity<OpenDataMarketItem[]> resp =
                rt.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), OpenDataMarketItem[].class);
        OpenDataMarketItem[] arr = resp.getBody();
        int raw = (arr==null?0:arr.length);
        System.out.println("[OpenData] raw size = " + raw);
        if (raw == 0) return 0;

        // 先拆：台北一/二正常品清單 + 是否有休市公告 + 公告日
        List<OpenDataMarketItem> t1List = new ArrayList<>();
        List<OpenDataMarketItem> t2List = new ArrayList<>();
        boolean t1HasClosed = false, t2HasClosed = false;
        LocalDate closedDate = null;

        for (OpenDataMarketItem i : arr) {
            if (i == null) continue;
            String tag = normalizeMarket(i.getMarketName());
            if (tag == null) continue;
            i.setMarketCode(tag);
            String nm = Objects.toString(i.getCropName(), "").trim();
            if ("休市".equals(nm)) {
                if ("T1".equals(tag)) t1HasClosed = true;
                if ("T2".equals(tag)) t2HasClosed = true;
                if (closedDate == null) closedDate = i.getTransLocalDate();
                continue; // 休市筆不進正常清單
            }
            if ("T1".equals(tag)) t1List.add(i); else t2List.add(i);
        }

        // 基本欄位檢核 + 只用作物代號白名單過濾
        t1List = t1List.stream()
                .filter(i -> i.getCropCode()!=null && i.getCropName()!=null)
                .filter(i -> i.getHighPrice()!=null || i.getMidPrice()!=null || i.getLowPrice()!=null)
                .filter(i -> codeSet.isEmpty() || codeSet.contains(i.getCropCode().trim()))
                .toList();

        t2List = t2List.stream()
                .filter(i -> i.getCropCode()!=null && i.getCropName()!=null)
                .filter(i -> i.getHighPrice()!=null || i.getMidPrice()!=null || i.getLowPrice()!=null)
                .filter(i -> codeSet.isEmpty() || codeSet.contains(i.getCropCode().trim()))
                .toList();

        // 正常品存在 → 「不是休市」；只有在該市場完全沒有正常品且有休市公告才算休市
        boolean t1Closed = t1List.isEmpty() && t1HasClosed;
        boolean t2Closed = t2List.isEmpty() && t2HasClosed;

        // 每市場各自 (作物名稱) 取「最新交易日」一筆
        Map<String, OpenDataMarketItem> latestT1 = t1List.stream()
                .collect(Collectors.groupingBy(i -> i.getCropName().trim()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .max(Comparator.comparing(i ->
                                        Optional.ofNullable(i.getTransLocalDate()).orElse(LocalDate.MIN)))
                                .orElse(null)
                ));

        Map<String, OpenDataMarketItem> latestT2 = t2List.stream()
                .collect(Collectors.groupingBy(i -> i.getCropName().trim()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .max(Comparator.comparing(i ->
                                        Optional.ofNullable(i.getTransLocalDate()).orElse(LocalDate.MIN)))
                                .orElse(null)
                ));

        int affected = 0;
        LocalDateTime now = LocalDateTime.now();

        // 先寫正常品（不管今天是否也有休市公告）
        Set<String> wroteT1Codes = new HashSet<>();
        Set<String> wroteT2Codes = new HashSet<>();

        for (OpenDataMarketItem item : latestT1.values()) {
            if (item == null) continue;
            long pid = dao.insertProductIfNotExists(item.getCropName().trim());
            affected += dao.updateMarket1(
                    pid, item.getCropCode().trim(), "台北一",
                    item.getHighPrice(), item.getMidPrice(), item.getLowPrice(),
                    item.getUnit(), item.getTransLocalDate(), now
            );
            wroteT1Codes.add(item.getCropCode().trim());
        }
        for (OpenDataMarketItem item : latestT2.values()) {
            if (item == null) continue;
            long pid = dao.insertProductIfNotExists(item.getCropName().trim());
            affected += dao.updateMarket2(
                    pid, item.getCropCode().trim(), "台北二",
                    item.getHighPrice(), item.getMidPrice(), item.getLowPrice(),
                    item.getUnit(), item.getTransLocalDate(), now
            );
            wroteT2Codes.add(item.getCropCode().trim());
        }

        // 若該市場當天「沒有任何正常品」但有休市公告 → 對「目標代號集」做占位（三價 NULL）
        LocalDate closedUseDate = (closedDate != null) ? closedDate : LocalDate.now();
        Set<String> targetCodes = codeSet.isEmpty()
                ? unionCodes(t1List, t2List)   // 若沒白名單，就拿今日有出現的代號集合（避免亂全表）
                : codeSet;                     // 有白名單 → 就照你設定的代號

        if (t1Closed) {
            for (String code : targetCodes) {
                if (wroteT1Codes.contains(code)) continue; // 已有正常品就不要占位
                String name = pickExistingNameOrCode(code);
                long pid = dao.insertProductIfNotExists(name);
                affected += dao.updateMarket1(
                        pid, code, "台北一",
                        null, null, null,
                        null, closedUseDate, now
                );
            }
        }
        if (t2Closed) {
            for (String code : targetCodes) {
                if (wroteT2Codes.contains(code)) continue;
                String name = pickExistingNameOrCode(code);
                long pid = dao.insertProductIfNotExists(name);
                affected += dao.updateMarket2(
                        pid, code, "台北二",
                        null, null, null,
                        null, closedUseDate, now
                );
            }
        }

        System.out.println("[OpenData] upsert updated = " + affected);
        return affected;
    }

    private static Set<String> unionCodes(List<OpenDataMarketItem> a, List<OpenDataMarketItem> b){
        Set<String> s = a.stream().map(i->i.getCropCode().trim()).collect(Collectors.toSet());
        s.addAll(b.stream().map(i->i.getCropCode().trim()).collect(Collectors.toSet()));
        return s;
    }

    /** 找既有商品名稱；找不到就用代號當暫名（占位時避免空 name） */
    private String pickExistingNameOrCode(String code) {
        Long id = dao.findExistingIdByMarketCodeOrName(code, code);
        if (id == null) return code;
        String n = dao.findNameById(id);
        return (n == null || n.isBlank()) ? code : n;
    }
}