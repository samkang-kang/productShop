const express = require("express");
const router = express.Router();

// 專案的標準起始模板檔案
router.get("/", (req, res, next) => {
  res.send("respond with a resource");
});

module.exports = router;
