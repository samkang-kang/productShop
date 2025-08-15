const express = require("express");
const router = express.Router();
const crypto = require("crypto");
require("dotenv").config();

// 綠界提供的 SDK
const ecpay_payment = require("ecpay_aio_nodejs");

const { MERCHANTID, HASHKEY, HASHIV, HOST } = process.env;

// 檢查必要的環境變數
if (!MERCHANTID || !HASHKEY || !HASHIV || !HOST) {
  console.error("缺少必要的環境變數：MERCHANTID, HASHKEY, HASHIV, HOST");
  process.exit(1);
}

// 綠界支付服務類
class ECPayService {
  constructor() {
    this.options = {
      OperationMode: "Test", // Test or Production
      MercProfile: {
        MerchantID: MERCHANTID,
        HashKey: HASHKEY,
        HashIV: HASHIV,
      },
      IgnorePayment: [
        // "Credit", "WebATM", "ATM", "CVS", "BARCODE", "AndroidPay"
      ],
      IsProjectContractor: false,
    };

    this.ecpayInstance = new ecpay_payment(this.options);
  }

  // 生成交易編號
  generateTradeNo() {
    return `test${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  // 格式化日期為綠界要求的格式
  formatTradeDate() {
    return new Date().toLocaleString("zh-TW", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
      timeZone: "UTC",
    });
  }

  // 生成支付參數
  generatePaymentParams(
    amount = "100",
    description = "測試交易描述",
    itemName = "測試商品等"
  ) {
    return {
      MerchantTradeNo: this.generateTradeNo(),
      MerchantTradeDate: this.formatTradeDate(),
      TotalAmount: amount.toString(),
      TradeDesc: description,
      ItemName: itemName,
      ReturnURL: `${HOST}/return`,
      ClientBackURL: `${HOST}/clientReturn`,
    };
  }

  // 創建支付表單
  createPaymentForm(params) {
    try {
      return this.ecpayInstance.payment_client.aio_check_out_all(params);
    } catch (error) {
      console.error("創建支付表單失敗:", error);
      throw new Error("支付表單創建失敗");
    }
  }

  // 驗證 CheckMacValue
  verifyCheckMacValue(data) {
    try {
      const { CheckMacValue, ...verificationData } = data;
      const checkValue =
        this.ecpayInstance.payment_client.helper.gen_chk_mac_value(
          verificationData
        );

      return {
        isValid: CheckMacValue === checkValue,
        received: CheckMacValue,
        calculated: checkValue,
      };
    } catch (error) {
      console.error("驗證 CheckMacValue 失敗:", error);
      throw new Error("簽章驗證失敗");
    }
  }
}

// 創建綠界服務實例
const ecpayService = new ECPayService();

// 錯誤處理中間件
const errorHandler = (error, req, res, next) => {
  console.error("路由錯誤:", error);
  res.status(500).json({
    success: false,
    message: error.message || "內部伺服器錯誤",
    timestamp: new Date().toISOString(),
  });
};

// 主頁路由
router.get("/", async (req, res, next) => {
  try {
    const paymentParams = ecpayService.generatePaymentParams();
    const paymentForm = ecpayService.createPaymentForm(paymentParams);

    console.log("支付表單創建成功，交易編號:", paymentParams.MerchantTradeNo);

    res.render("index", {
      title: "綠界支付測試",
      html: paymentForm,
    });
  } catch (error) {
    next(error);
  }
});

// 綠界回調處理
router.post("/return", async (req, res, next) => {
  try {
    console.log("收到綠界回調:", req.body);

    if (!req.body || Object.keys(req.body).length === 0) {
      return res.status(400).send("缺少回調數據");
    }

    const verificationResult = ecpayService.verifyCheckMacValue(req.body);

    console.log("簽章驗證結果:", {
      isValid: verificationResult.isValid,
      received: verificationResult.received,
      calculated: verificationResult.calculated,
    });

    if (verificationResult.isValid) {
      console.log("交易驗證成功，交易編號:", req.body.MerchantTradeNo);
      // 這裡可以添加交易記錄到資料庫的邏輯
      res.send("1|OK");
    } else {
      console.warn("交易驗證失敗，可能是偽造的回調");
      res.status(400).send("簽章驗證失敗");
    }
  } catch (error) {
    next(error);
  }
});

// 用戶支付完成後的轉址
router.get("/clientReturn", (req, res, next) => {
  try {
    console.log("用戶支付完成，查詢參數:", req.query);

    if (!req.query || Object.keys(req.query).length === 0) {
      return res.status(400).send("缺少查詢參數");
    }

    res.render("return", {
      query: req.query,
      success: req.query.RtnCode === "1",
      message: req.query.RtnMsg || "支付完成",
    });
  } catch (error) {
    next(error);
  }
});

// 健康檢查端點
router.get("/health", (req, res) => {
  res.json({
    status: "OK",
    timestamp: new Date().toISOString(),
    service: "ECPay Payment Service",
    environment: process.env.NODE_ENV || "development",
  });
});

// 使用錯誤處理中間件
router.use(errorHandler);

module.exports = router;
