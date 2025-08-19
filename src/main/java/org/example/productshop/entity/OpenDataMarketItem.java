package org.example.productshop.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenDataMarketItem {

    @JsonAlias({"作物代號","CropCode","crop_code","cropCode"})
    private String cropCode;

    @JsonAlias({"作物名稱","CropName","crop_name","cropName"})
    private String cropName;

    @JsonAlias({"市場名稱","MarketName","market_name"})
    private String marketName;

    @JsonAlias({"上價","High_Price","high_price"})
    private BigDecimal highPrice;

    @JsonAlias({"中價","Mid_Price","mid_price"})
    private BigDecimal midPrice;

    @JsonAlias({"下價","Low_Price","low_price"})
    private BigDecimal lowPrice;

    @JsonAlias({"單位","Unit","unit"})
    private String unit;

    @JsonAlias({"交易日期","TransDate","Date","date"})
    private String transDateText;

    private transient String marketCode; // T1/T2

    // getters/setters
    public String getCropCode() { return cropCode; }
    public void setCropCode(String cropCode) { this.cropCode = trim(cropCode); }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = trim(cropName); }

    public String getMarketName() { return marketName; }
    public void setMarketName(String marketName) { this.marketName = trim(marketName); }

    public BigDecimal getHighPrice() { return highPrice; }
    public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }

    public BigDecimal getMidPrice() { return midPrice; }
    public void setMidPrice(BigDecimal midPrice) { this.midPrice = midPrice; }

    public BigDecimal getLowPrice() { return lowPrice; }
    public void setLowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = trim(unit); }

    public String getTransDateText() { return transDateText; }
    public void setTransDateText(String transDateText) { this.transDateText = trim(transDateText); }

    public String getMarketCode() { return marketCode; }
    public void setMarketCode(String marketCode) { this.marketCode = marketCode; }

    /** 民國 114.08.10 或西元 yyyy-MM-dd 轉 LocalDate */
    public LocalDate getTransLocalDate() {
        if (transDateText == null || transDateText.isBlank()) return null;
        String s = transDateText.trim();
        if (s.matches("\\d{3}\\.\\d{2}\\.\\d{2}")) {
            String[] p = s.split("\\.");
            int y = Integer.parseInt(p[0]) + 1911;
            return LocalDate.parse(y + "-" + p[1] + "-" + p[2], DateTimeFormatter.ISO_DATE);
        }
        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(s, DateTimeFormatter.ISO_DATE);
        return null;
    }

    private static String trim(String s){ return s==null?null:s.trim(); }
}