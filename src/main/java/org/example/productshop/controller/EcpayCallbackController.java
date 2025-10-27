package org.example.productshop.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Handles ECPay server-to-server notify callback.
 * - Logs notification into ecpay_payment_notifications
 * - If success (RtnCode == 1), updates orders.status to PAID and sets payment info
 *
 * Notes:
 * 1) We force rtn_msg to "交易成功" as requested (hide English messages).
 * 2) ECPay PaymentDate format is typically "yyyy/MM/dd HH:mm:ss"; we convert to Timestamp for orders.payment_date.
 * 3) MUST return plain text "1|OK" for ECPay to accept the callback.
 */
@RestController
@RequestMapping("/api/ecpay")
public class EcpayCallbackController {

    private static final Logger log = LoggerFactory.getLogger(EcpayCallbackController.class);

    private final NamedParameterJdbcTemplate jdbc;

    public EcpayCallbackController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping(
            value = "/notify",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> notify(@RequestParam MultiValueMap<String, String> form) {
        Map<String, String> p = form.toSingleValueMap();
        log.info("[ECPAY] notify params: {}", p);

        // Required fields from ECPay
        String mtn = p.getOrDefault("MerchantTradeNo", "");
        String rtnCode = p.getOrDefault("RtnCode", "");
        String paymentType = p.getOrDefault("PaymentType", "");
        String tradeAmtStr = p.getOrDefault("TradeAmt", "0");
        String paymentDateRaw = p.get("PaymentDate"); // often "yyyy/MM/dd HH:mm:ss"

        // Normalize values
        int tradeAmt;
        try {
            tradeAmt = Integer.parseInt(tradeAmtStr);
        } catch (NumberFormatException e) {
            tradeAmt = 0;
        }
        // Force Chinese message per user's requirement
        String rtnMsg = "交易成功";

        // 1) Insert notification log
        MapSqlParameterSource np = new MapSqlParameterSource()
                .addValue("merchant_trade_no", mtn)
                .addValue("rtn_code", rtnCode)
                .addValue("rtn_msg", rtnMsg)
                .addValue("trade_amt", tradeAmt)
                .addValue("payment_type", paymentType)
                .addValue("payment_date", paymentDateRaw);
        try {
            jdbc.update(
                    "INSERT INTO ecpay_payment_notifications(merchant_trade_no, rtn_code, rtn_msg, trade_amt, payment_type, payment_date, created_at) " +
                            "VALUES(:merchant_trade_no,:rtn_code,:rtn_msg,:trade_amt,:payment_type,:payment_date,NOW())",
                    np
            );
        } catch (Exception e) {
            log.error("[ECPAY] insert notify log failed", e);
            // Still respond OK to avoid ECPay retry storms, but record error in logs.
        }

        // 2) If success -> update orders
        if ("1".equals(rtnCode)) {
            Timestamp payTs = toTimestamp(paymentDateRaw);
            MapSqlParameterSource up = new MapSqlParameterSource()
                    .addValue("mtn", mtn)
                    .addValue("amt", tradeAmt)
                    .addValue("ptype", paymentType)
                    .addValue("pdate", payTs);
            try {
                jdbc.update(
                        "UPDATE orders SET status='PAID', total_amount=:amt, payment_type=:ptype, payment_date=:pdate, updated_at=NOW() " +
                                "WHERE merchant_trade_no=:mtn",
                        up
                );
            } catch (Exception e) {
                log.error("[ECPAY] update order failed for MTN={}", mtn, e);
                // Still return OK so ECPay doesn't keep retrying; investigate via logs.
            }
        }

        // MUST return "1|OK"
        return ResponseEntity.ok("1|OK");
    }

    /**
     * Convert ECPay PaymentDate to java.sql.Timestamp.
     * Tries common patterns: "yyyy/MM/dd HH:mm:ss" and "yyyy-MM-dd HH:mm:ss".
     * Returns current time if parsing fails.
     */
    private static Timestamp toTimestamp(String s) {
        if (s == null || s.isBlank()) {
            return new Timestamp(System.currentTimeMillis());
        }
        DateTimeFormatter[] patterns = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        };
        for (DateTimeFormatter f : patterns) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(s, f);
                return Timestamp.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignore) { }
        }
        return new Timestamp(System.currentTimeMillis());
    }
}
