package org.example.productshop.job;


import org.example.productshop.service.OpenDataMarketSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketPriceSyncJob {

    @Autowired
    private OpenDataMarketSyncService service;

    // 每小時第 10 分（可用 properties 覆寫：app.market.sync-cron）
    @Scheduled(cron = "${app.market.sync-cron:0 10 * * * *}")
    public void hourlySync() {
        try {
            int n = service.sync(null);
            System.out.println("[MarketPriceSyncJob] auto-sync updated=" + n);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}