// ============================
//  共用：取得購物車的 <tbody>（支援 #cart tbody 與 #cart-tbody）
// ============================
function getCartTbody() {
  return document.querySelector("#cart tbody, #cart-tbody");
}

// 商品 ID -> 單品頁檔名
const DETAIL_PAGE_MAP = {
  1: "zbanana_1.html",
  4: "zhamimelon_1.html",
  5: "zpapaya_1.html",
  6: "zpeach_1.html",
  9: "zlongan_1.html",
  10: "zwatermelon_1.html",
  12: "zmango_1.html",
  11: "zapple_1.html",
  14: "zpassion_1.html",
  15: "zgrape_1.html",
  18: "zguava_1.html",
};

function buildDetailUrl(item) {
  const pid = item?.productId ?? item?.id;
  if (!pid) return "#";
  return DETAIL_PAGE_MAP[pid] || `product_${pid}.html`;
}

// ============================
//  漢堡選單開合
// ============================
const bar = document.getElementById("bar");
const close = document.getElementById("close");
const nav = document.getElementById("navbar");

if (bar && nav) {
  bar.addEventListener("click", () => nav.classList.add("active"));
}
if (close && nav) {
  close.addEventListener("click", () => nav.classList.remove("active"));
}

// ============================
//  所有網站 LOGO 通用 JS + 空狀態
// ============================
document.addEventListener("DOMContentLoaded", function () {
  // 左上 LOGO：點擊回到頂部（平滑滾動）
  const logoLink = document.getElementById("logo-link");
  const logoImg = document.getElementById("fixed-logo");

  function scrollTopSmooth(e) {
    if (e) e.preventDefault();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  if (logoLink) logoLink.addEventListener("click", scrollTopSmooth);
  if (logoImg) logoImg.addEventListener("click", scrollTopSmooth);

  // 購物車空狀態初始化
  const theadRow = document.querySelector("#cart thead tr");

  function ensureEmptyRow() {
    const cartBody = getCartTbody();
    if (!cartBody || !theadRow) return;

    const productRows = cartBody.querySelectorAll("tr[data-product-id]");
    const emptyExists = !!document.getElementById("empty-row");

    if (productRows.length === 0) {
      // 徽章歸零
      try {
        localStorage.setItem("cart_count", "0");
        window.updateCartBadge?.(0);
      } catch {}
      if (!emptyExists) {
        const emptyMessage = document.createElement("tr");
        emptyMessage.innerHTML = `<td colspan="7" id="empty-row" style="text-align:center; padding:40px;">購物車內無商品</td>`;
        cartBody.appendChild(emptyMessage);
      }
      const removeHeader = theadRow.querySelector("th, td");
      if (removeHeader && removeHeader.textContent.includes("移除")) {
        removeHeader.style.visibility = "hidden";
      }
    } else {
      const empty = document.getElementById("empty-row");
      if (empty) empty.remove();
      const removeHeader = theadRow.querySelector("th, td");
      if (removeHeader && removeHeader.style.visibility === "hidden") {
        removeHeader.style.visibility = "visible";
      }
      // 更新徽章為目前列數
      try {
        const count = productRows.length;
        localStorage.setItem("cart_count", String(count));
        window.updateCartBadge?.(count);
      } catch {}
    }
  }

  ensureEmptyRow();

  // 優惠券按鈕
  const applyBtn = document.getElementById("applyBtn");
  if (applyBtn) {
    applyBtn.addEventListener("click", function () {
      alert("對不起，現在尚無優惠可用");
    });
  }

  // 讓其他地方可呼叫
  window.__ensureEmptyRow = ensureEmptyRow;

  // ====== Navbar 購物車徽章 ======
  function updateCartBadge(count) {
    const badge = document.getElementById("cart-badge");
    if (!badge) return;
    const n = Number(count || 0);
    if (n > 0) {
      badge.textContent = String(n);
      badge.style.display = "inline-block";
    } else {
      badge.textContent = "0";
      badge.style.display = "none";
    }
  }
  window.updateCartBadge = updateCartBadge;

  // 從 localStorage 讀取先前數量（若你有存），否則先隱藏
  try {
    const cached = Number(localStorage.getItem("cart_count") || 0);
    updateCartBadge(cached);
  } catch {}
});

// ============================
//  輪播
// ============================
window.onload = function () {
  let slideIndex = 0;
  const slidesWrap = document.querySelector(".slides");
  const slides = document.querySelectorAll(".slides .slide__item");
  const prev = document.getElementById("prev");
  const next = document.getElementById("next");
  const dots = document.getElementsByClassName("dot");
  const total = slides.length;

  if (!slidesWrap || !total) return;

  function goTo(i) {
    slideIndex = (i + total) % total; // 迴圈
    slidesWrap.style.transform = `translateX(-${slideIndex * 100}%)`;

    for (let d = 0; d < dots.length; d++) dots[d].classList.remove("active");
    if (dots[slideIndex]) dots[slideIndex].classList.add("active");

    const pn = slides[slideIndex].querySelector(".slide__pagenumber");
    if (pn) pn.textContent = `${slideIndex + 1} / ${total}`;
  }

  function nextSlide() {
    goTo(slideIndex + 1);
  }
  function prevSlide() {
    goTo(slideIndex - 1);
  }

  if (prev) prev.addEventListener("click", prevSlide);
  if (next) next.addEventListener("click", nextSlide);

  for (let i = 0; i < dots.length; i++) {
    dots[i].addEventListener("click", () => goTo(i));
  }

  goTo(0);

  let timer = setInterval(nextSlide, 4000);
  ["click", "touchstart", "mouseenter"].forEach((evt) => {
    slidesWrap.addEventListener(
        evt,
        () => {
          clearInterval(timer);
          timer = setInterval(nextSlide, 3000);
        },
        { passive: true }
    );
  });
};

// ============================
//  購物車頁面功能
// ============================

// === 配置：後端 API（同源最穩） ===
const onBackend = location.port === "8080";
const API_BASE = onBackend ? "" : "http://localhost:8080";
const FIXED_USER_ID = 65; // 僅作為前端顯示用途，後端以 JWT 判斷使用者

const API_ADD_TO_CART = `${API_BASE}/api/cart/add`;
const API_CART_REMOVE = `${API_BASE}/api/cart/remove`;
const API_CART_GET = `${API_BASE}/api/cart/me`;
const API_CART_UPDATE_QTY = `${API_BASE}/api/cart/update`; // PUT with JSON body

// ============================
//  同步數量到後端（PUT + JSON Body，含 DEBUG）
// ============================
async function syncQty(row, newQty) {
  const cartItemId = row.dataset.cartItemId;
  const userId = row.dataset.userId || FIXED_USER_ID;

  console.log(
      "[DEBUG] syncQty() start => cartItemId:",
      cartItemId,
      "userId:",
      userId,
      "newQty:",
      newQty
  );

  try {
    const res = await Auth.authFetch(`${API_CART_UPDATE_QTY}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        cartItemId: Number(cartItemId),
        quantity: Number(newQty),
      }),
    });

    let data = {};
    try {
      data = await res.json();
    } catch {
      // 若後端沒回 JSON，也不要炸
      data = {};
    }

    console.log("[DEBUG] syncQty() response:", res.status, data);

    if (!res.ok) {
      throw new Error(data.message || "更新失敗");
    }

    // 成功就不 reload，UI 已經先樂觀更新
    // 如需嚴格一致，可改成：await loadCart();
  } catch (err) {
    console.error("[DEBUG] syncQty() error:", err);
    alert(err.message || "更新數量時發生錯誤，將還原畫面");
    // 以重新載入確保與後端一致
    await loadCart();
  }
}

// ============================
//  綁定 ± 按鈕（事件委派）：綁在 #cart + DEBUG，只綁一次
// ============================
window.__qtyBound = false;
function bindQtyControls() {
  const cart = document.getElementById("cart");
  if (!cart) return;
  if (window.__qtyBound) return;
  window.__qtyBound = true;

  cart.addEventListener("click", async (e) => {

    const btn = e.target.closest(".qty-btn");
    if (!btn) return;
    e.preventDefault();

    console.log("[DEBUG] 點擊的按鈕:", btn.dataset.action);

    const row = btn.closest("tr");
    const box = btn.closest(".qty");
    const input = box?.querySelector(".qty-input");
    if (!row || !box || !input) {
      console.warn("[DEBUG] 找不到 row/box/input，事件沒有綁到正確元素");
      return;
    }

    const min = Number(box.dataset.min || 1);
    const max = Number(box.dataset.max || 99);

    let val = Number(input.value || 1);
    const action = btn.dataset.action;

    console.log("[DEBUG] 原本數量:", val, "min:", min, "max:", max);

    if (action === "inc") val = Math.min(max, val + 1);
    if (action === "dec") val = Math.max(min, val - 1);

    console.log("[DEBUG] 更新後數量:", val);

    input.value = String(val);

    await updateRowPriceForQty(row, val);

    recalcRowSubtotal(row);
    recalcTotals();

    // 同步到後端
    console.log("[DEBUG] 呼叫 syncQty()", row.dataset.cartItemId, val);
    syncQty(row, val);
  });
}

// ============================
//  綁定刪除（×）按鈕：只綁一次
// ============================
window.__removeBound = false;
function bindRemoveRows() {
  const cart = document.getElementById("cart");
  if (!cart) return;
  if (window.__removeBound) return;
  window.__removeBound = true;

  cart.addEventListener("click", async (e) => {
    const icon = e.target.closest(".fa-times-circle");
    if (!icon) return;
    e.preventDefault();

    const row = icon.closest("tr");
    if (!row) return;

    const cartId = row.dataset.cartItemId;
    const name =
        row.querySelector("td:nth-child(3)")?.textContent?.trim() || "此商品";

    if (!cartId) {
      alert("缺少 cartItemId，請在 <tr> 上加 data-cart-item-id。");
      return;
    }
    if (!confirm(`要從購物車移除「${name}」嗎？`)) return;

    icon.style.pointerEvents = "none";

    try {
      const res = await Auth.authFetch(
          `${API_CART_REMOVE}?cartItemId=${encodeURIComponent(cartId)}`,
          { method: "GET" }
      );
      const text = await res.text();
      let data = {};
      try {
        data = JSON.parse(text);
      } catch {}
      if (!res.ok) throw new Error(data.message || text || res.statusText);

      row.remove();
      recalcTotals();
      window.__ensureEmptyRow?.();
    } catch (err) {
      console.error("Remove cart item error:", err);
      alert("移除失敗：" + (err.message || "請稍後再試"));
    } finally {
      icon.style.pointerEvents = "";
    }
  });
}
function numOrNull(v) {
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

// 依回傳欄位自動推導單價：
// 1) 直接有 item.price 就用它
// 2) 有 subtotal 且有 quantity → subtotal / quantity
// 3) 有市場價（market1_*、market2_*）→ 依數量選價階（1–9 上價、10–20 中價、21+ 下價；上/中取高、下取低）
// 4) 都沒有 → 0
function deriveUnitPriceFromItem(item) {
  const qty = Number(item?.quantity ?? 1);

  // 嘗試抓不同欄位的 productId
  const pid = item?.productId ?? item?.id ?? item?.product_id;

  // ★ 1. 優先從 localStorage 拿當時加入購物車的單價
  if (pid) {
    const stored = localStorage.getItem(`price:${pid}`);
    if (stored && !isNaN(stored)) {
      return Number(stored);
    }
  }

  // 2. 後端直接回傳的價格
  const direct = numOrNull(item?.price);
  if (direct != null) return direct;

  // 3. 後端有小計
  const subtotal = numOrNull(item?.subtotal);
  if (subtotal != null && qty > 0) return subtotal / qty;

  // 4. 市場行情價
  const m1 = {
    high: numOrNull(item?.market1_high_price),
    mid: numOrNull(item?.market1_mid_price),
    low: numOrNull(item?.market1_low_price),
  };
  const m2 = {
    high: numOrNull(item?.market2_high_price),
    mid: numOrNull(item?.market2_mid_price),
    low: numOrNull(item?.market2_low_price),
  };

  const tier = qty < 10 ? "high" : qty < 21 ? "mid" : "low";
  const a = m1[tier],
      b = m2[tier];

  if (a == null && b == null) return 0;
  return tier === "low" ? Math.min(a ?? b, b ?? a) : Math.max(a ?? b, b ?? a);
}

// ============================
//  根據後端資料渲染一列（兩顆按鈕加 type="button"）
// ============================
function renderCartRow(item) {
  // ① 先推導出單價（若後端沒給 price，也會用 subtotal/quantity 或市場價算）
  const unit = deriveUnitPriceFromItem(item);
  const qty = Number(item.quantity || 1);

  // ② 建列，並把單價放到 data-price，讓小計/合計運算一律用 unit
  const tr = document.createElement("tr");
  tr.dataset.cartItemId = item.cartItemId;
  tr.dataset.userId =
      typeof item.userId !== "undefined" ? item.userId : FIXED_USER_ID;
  tr.dataset.productId = item.productId;
  tr.dataset.price = unit;

  // ③ 產出 6 欄，對齊 cart.html 的表頭（移除/圖片/名稱/單價/數量/小計）
  tr.innerHTML = `
    <td><a href="#" class="btn-remove"><i class="far fa-times-circle"></i></a></td>
    <td>
      <a class="detail-link" href="${buildDetailUrl(item)}" data-pid="${
      item.productId
  }">
        <img src="${item.imageUrl || "img/FruitsImg/null/G2_0.jpg"}" alt="${
      item.name || ""
  }"/>
      </a>
    </td>
    <td>
      <a class="detail-link" href="${buildDetailUrl(item)}" data-pid="${
      item.productId
  }">
        ${item.name || "—"}
      </a>
    </td>
    <td class="unit-price">${formatCurrency(unit)}</td>
    <td>
      <div class="qty" data-min="1" data-max="99">
        <button type="button" class="qty-btn" data-action="dec" aria-label="減少一個">−</button>
        <input class="qty-input" type="text" inputmode="numeric" aria-label="數量" readonly value="${qty}">
        <button type="button" class="qty-btn" data-action="inc" aria-label="增加一個">＋</button>
      </div>
    </td>
    <td class="line-subtotal">${formatCurrency(unit * qty)}</td>
  `;
  return tr;
}

// ============================
//  載入購物車（打後端 GET）
// ============================
async function loadCart() {
  const tbody = getCartTbody();
  if (!tbody) return;

  tbody.innerHTML = ""; // 先清空
  console.log("[DEBUG] loadCart() start =>", API_CART_GET);

  try {
    const res = await Auth.authFetch(API_CART_GET, { method: "GET" });
    const text = await res.text();
    let data = {};
    try {
      data = JSON.parse(text);
    } catch {}

    console.log("[DEBUG] loadCart() response:", res.status, data);

    if (!res.ok) throw new Error(data.message || text || res.statusText);

    (data.items || []).forEach((item) => {
      tbody.appendChild(renderCartRow(item));
    });

    recalcTotals();
    bindQtyControls(); // 只綁一次（有旗標）
    bindRemoveRows(); // 只綁一次（有旗標）
    window.__ensureEmptyRow?.();
  } catch (err) {
    console.error("loadCart error:", err);
    window.__ensureEmptyRow?.();
    alert("載入購物車失敗，請稍後再試");
  }
}

// ============================
//  單一商品頁：數量 + / - 與加入購物車（含 DEBUG）
// ============================
(function bindQtyControlsForSingle() {
  const box = document.querySelector(".single-pro-details");
  if (!box) return;

  box.addEventListener("click", (e) => {
    const btn = e.target.closest(".qty-btn");
    if (!btn) return;

    const wrap = btn.closest(".qty");
    const input = wrap.querySelector(".qty-input");
    const min = Number(wrap.dataset.min || 1);
    const max = Number(wrap.dataset.max || 99);

    let val = Number(input.value || 1);
    const action = btn.dataset.action;

    if (action === "inc") val = Math.min(max, val + 1);
    if (action === "dec") val = Math.max(min, val - 1);

    input.value = String(val);
    // ★ 通知價格模組：數量變了，請重算（initSingleProductPrice 監聽 input/change）
    input.dispatchEvent(new Event("input", { bubbles: true }));
    input.dispatchEvent(new Event("change", { bubbles: true }));

    console.log("[DEBUG][single] action:", action, "qty:", val);
  });
})();

(function initSingleProductAddToCart() {
  const btn = document.getElementById("btnAddSingle");
  const qtyInput = document.getElementById("sp-qty");
  if (!btn || !qtyInput) return; // 不是單一商品頁就略過

  function getProductIdFromBody() {
    const pid = document.body?.dataset?.productId;
    return pid ? Number(pid) : NaN;
  }

  btn.addEventListener("click", async () => {
    const productId = getProductIdFromBody();
    const qty = Math.max(1, Number(qtyInput.value || 1));

    if (!productId || Number.isNaN(productId)) {
      alert('找不到商品編號，請檢查 <body data-product-id="..."> 是否正確。');
      return;
    }

    // ★ 讀取當前頁面顯示的單價
    const currentUnitPrice =
        document.querySelector(".price")?.textContent || "";
    const numericPrice = Number(currentUnitPrice.replace(/[^\d.]/g, "")) || 0;

    btn.disabled = true;
    const oldText = btn.textContent;
    btn.textContent = "加入中…";

    try {
      console.log("[DEBUG] addToCart =>", {
        userId: FIXED_USER_ID,
        productId,
        quantity: qty,
        price: numericPrice,
      });

      const res = await fetch(API_ADD_TO_CART, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: FIXED_USER_ID,
          productId: productId,
          quantity: qty,
          price: numericPrice, // ★ 把當下的單價傳給後端
        }),
      });

      const text = await res.text();
      let data = {};
      try {
        data = JSON.parse(text);
      } catch {}

      console.log("[DEBUG] addToCart response:", res.status, data);

      if (!res.ok) throw new Error(data.message || text || res.statusText);
      localStorage.setItem(`price:${productId}`, numericPrice);

      alert(data.message || "已加入購物車");
      // 更新徽章數：以後端目前購物車「不同品項數」為準，避免同品項數量增加造成誤加
      try {
        let count = NaN;
        try {
          const resp = await Auth.authFetch(API_CART_GET, { method: "GET" });
          const txt = await resp.text();
          let j = {};
          try {
            j = JSON.parse(txt);
          } catch {}
          if (Array.isArray(j.items)) count = j.items.length;
          if (!Number.isFinite(count) && Number.isFinite(j.count))
            count = Number(j.count);
        } catch {}

        // 後端若無提供，使用前端集合（以 productId 去重）當作後援
        if (!Number.isFinite(count)) {
          const set = new Set(
              JSON.parse(localStorage.getItem("cart_products") || "[]")
          );
          set.add(productId);
          count = set.size;
          localStorage.setItem("cart_products", JSON.stringify([...set]));
        }

        localStorage.setItem("cart_count", String(count));
        window.updateCartBadge?.(count);
      } catch {}
    } catch (err) {
      console.error("AddToCart(single) error:", err);
      alert("加入失敗：" + (err.message || "請稍後再試"));
    } finally {
      btn.disabled = false;
      btn.textContent = oldText;
    }
  });
})();

// ============================
//  頁面載入時初始化（含重編號）
// ============================
document.addEventListener("DOMContentLoaded", () => {
  loadCart(); // 先向後端拉購物車並渲染（含數量）

  const tbody = getCartTbody();
  if (tbody) {
    resetCartRowNumbers();
  }
});

function resetCartRowNumbers() {
  const tbody = getCartTbody();
  if (!tbody) return;

  const rows = tbody.querySelectorAll("tr[data-cart-item-id]");
  let index = 1;
  rows.forEach((row) => {
    row.dataset.displayId = index; // 顯示用
    const numberCell = row.querySelector(".cart-index");
    if (numberCell) numberCell.textContent = index;
    index++;
  });
}

window.formatCurrency =
    window.formatCurrency ||
    ((n) =>
        new Intl.NumberFormat("zh-TW", {
          style: "currency",
          currency: "TWD",
        }).format(Number(n || 0)));

/* =========================
   做法A：依商品ID決定圖片路徑
   規則：img/products/{productId}.jpg
========================= */
function ruleImageUrlById(productId) {
  return `img/products/${productId}.jpg`;
}

/* =========================
   解析購物車圖片來源的優先順序
   1) 後端回傳的 item.imageUrl
   2) 單品頁暫存的主圖 localStorage: img:{productId}
   3) 檔名=商品ID 規則圖（img/products/{id}.jpg）
   4) placeholder
========================= */
function resolveImageUrl(item) {
  if (item && item.imageUrl) return item.imageUrl;
  const cached = localStorage.getItem(`img:${item.productId || item.id}`);
  if (cached) return cached;
  const byRule = ruleImageUrlById(item.productId || item.id);
  return byRule || "img/placeholder.png";
}

/* =========================
   單品頁：加入購物車前，把主圖暫存下來
   支援兩種按鈕ID：#btnAddSingle（你原本的）與 #addToCartBtn
   支援數量輸入：#sp-qty（若沒有就當 1）
========================= */
(function initSingleProductAddToCart_ImageCapture() {
  const body = document.querySelector("body[data-product-id]");
  if (!body) return; // 不是單品頁就略過

  const getProductId = () => Number(body.dataset.productId);
  const mainImg =
      document.getElementById("MainImg") ||
      document.querySelector(".main-img,.product-main img");

  // 綁定兩種可能的加入按鈕
  const btns = [
    document.getElementById("btnAddSingle"),
    document.getElementById("addToCartBtn"),
  ].filter(Boolean);

  if (!btns.length) return;

  btns.forEach((btn) => {
    btn.addEventListener("click", () => {
      const pid = getProductId();
      if (!pid || Number.isNaN(pid)) return;

      // ✅ 暫存主圖 URL（之後購物車頁會優先使用）
      if (mainImg && mainImg.src) {
        localStorage.setItem(`img:${pid}`, mainImg.src);
      } else {
        // 如果找不到主圖，嘗試抓頁面第一張商品圖
        const anyImg = document.querySelector(".pro img, .gallery img, img");
        if (anyImg?.src) localStorage.setItem(`img:${pid}`, anyImg.src);
      }
    });
  });
})();

/* =========================
   商品清單/推薦卡片：自動帶圖（做法A）
   條件：.pro[data-product-id] 底下有 <img class="product-img">
========================= */
(function applyListImagesByRule() {
  const cards = document.querySelectorAll(".pro[data-product-id]");
  if (!cards.length) return;

  cards.forEach((card) => {
    const id = card.dataset.productId;
    const img =
        card.querySelector("img.product-img") || card.querySelector("img");
    if (!img) return;

    // 若已有 src 就不覆蓋；沒有就依規則補上
    if (!img.getAttribute("src")) {
      img.src = ruleImageUrlById(id);
    }
    img.loading = img.loading || "lazy";
    img.decoding = img.decoding || "async";

    // 破圖備援：退到 jpg 或 placeholder
    img.addEventListener(
        "error",
        () => {
          const fallback = ruleImageUrlById(id);
          if (
              img.src !== location.origin + "/" + fallback &&
              !img.src.endsWith(fallback)
          ) {
            img.src = fallback;
          } else {
            img.src = "img/placeholder.png";
          }
        },
        { once: true }
    );
  });
})();

/* =========================
   購物車：增強 renderCartRow
   - 若你已有 window.renderCartRow：會先呼叫原本的，再把 <img> 換成正確來源
   - 若沒有：提供預設版 renderCartRow
========================= */
(function enhanceRenderCartRow() {
  const original = window.renderCartRow;

  function ensureImgOnRow(tr, item) {
    // 嘗試尋找圖片節點
    let img = tr.querySelector("td img, .cart-row img, img");
    const url = resolveImageUrl(item);

    if (img) {
      img.src = url;
      img.alt = item.name || "";
      img.loading = img.loading || "lazy";
      img.decoding = img.decoding || "async";
    } else {
      // 若原本沒有圖片欄位，補一個（放在第二欄）
      const td = document.createElement("td");
      td.innerHTML = `<img src="${url}" alt="${
          item.name || ""
      }" width="80" height="80" style="object-fit:cover">`;
      const first = tr.children[0];
      if (first && first.nextSibling) {
        tr.insertBefore(td, first.nextSibling);
      } else {
        tr.appendChild(td);
      }
    }
  }

  // 預設版（當專案沒有 renderCartRow 時使用）
  function renderCartRow(item) {
    const unitPrice = deriveUnitPriceFromItem(item);
    const subtotal = unitPrice * (item.quantity ?? 0);

    const tr = document.createElement("tr");

    tr.innerHTML = `
        <td><a href="#" class="remove-item" data-id="${
        item.productId
    }"><i class="far fa-times-circle"></i></a></td>
        <td><img src="${item.imageUrl || "img/default.png"}" alt="${
        item.name
    }" /></td>
        <td>${item.name || "未命名商品"}</td>
        <td>
            ${formatCurrency(unitPrice)}<br>
            <small class="note">(當時購買價)</small>
        </td>
        <td>
            <input type="number" class="cart-qty" data-id="${
        item.productId
    }" value="${item.quantity ?? 1}" min="1">
        </td>
        <td>${formatCurrency(subtotal)}</td>
    `;

    return tr;
  }

  window.renderCartRow = function (item) {
    const tr = original ? original(item) : defaultRenderCartRow(item);
    // 把圖片設為我們的優先序來源
    try {
      ensureImgOnRow(tr, item);
    } catch (e) {
      console.warn("ensureImgOnRow failed:", e);
    }
    return tr;
  };
})();

/* =========================
   若你的購物車是自己渲染（沒有 renderCart 函式）
   這段會找 #cart-tbody，將 cartItems 渲染成列：
   - 若你的專案已有 loadCart/renderCart，這段不會干擾
========================= */
(async function fallbackCartRenderIfNeeded() {
  const tbody =
      document.getElementById("cart-tbody") ||
      document.getElementById("cartTableBody");
  if (!tbody) return;

  // 若專案已有全功能的 loadCart，就不介入
  if (typeof window.loadCart === "function") return;

  // 假設你把購物車清單存在 localStorage（沒有後端時）
  const local = JSON.parse(localStorage.getItem("cart") || "[]");
  if (!local.length) return;

  tbody.innerHTML = "";
  local.forEach((item) => {
    const tr = window.renderCartRow({
      productId: item.id || item.productId,
      name: item.name,
      price: item.price,
      quantity: item.qty || item.quantity || 1,
      imageUrl: item.imageUrl, // 若有
    });
    tbody.appendChild(tr);
  });
})();

/* ======== Pricing Module (auto-appended) ======== */
// Display decimals: 0 for integer, 2 for two decimals
const PRICE_DECIMALS = 0;

// API endpoint for market tiers
const API_MARKET_TIERS = (id) => `${API_BASE}/api/products/${id}/market-tiers`;

// safe rounding
function roundTo(n, d = PRICE_DECIMALS) {
  const x = Number(n);
  if (!isFinite(x)) return null;
  const p = 10 ** d;
  return Math.round((x + Number.EPSILON) * p) / p;
}

// currency formatter
function formatCurrency(n, decimals = PRICE_DECIMALS) {
  const v = roundTo(n, decimals);
  if (v == null) return "—";
  return v.toLocaleString("zh-TW", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

// normalize server payload into {m1:{high,mid,low}, m2:{high,mid,low}}
function normalizeTiers(d) {
  if (!d) return { m1: {}, m2: {} };
  if (d.m1 && d.m2) return d;
  return {
    m1: {
      high: d.market1_high_price ?? d.market1HighPrice ?? null,
      mid: d.market1_mid_price ?? d.market1MidPrice ?? null,
      low: d.market1_low_price ?? d.market1LowPrice ?? null,
    },
    m2: {
      high: d.market2_high_price ?? d.market2HighPrice ?? null,
      mid: d.market2_mid_price ?? d.market2MidPrice ?? null,
      low: d.market2_low_price ?? d.market2LowPrice ?? null,
    },
  };
}

// 1) 門檻（1–9 上價、10–20 中價、21+ 下價）
const DEFAULT_T1 = 10;
const DEFAULT_T2 = 21;

// 2) 決定價階
function chooseTier(qty, t1 = DEFAULT_T1, t2 = DEFAULT_T2) {
  qty = Number(qty || 0);
  if (qty < t1) return "high";
  if (qty < t2) return "mid";
  return "low";
}

// 3) 依數量 → 取價（小/中量取高，大量取低）+ 可指定 preferMarket
function pickPriceForQty(
    data,
    qty,
    t1 = DEFAULT_T1,
    t2 = DEFAULT_T2,
    {
      smallPolicy = "highest",
      midPolicy   = "highest",
      largePolicy = "lowest",
      preferMarket = "auto",   // ★ 新增：可 "M1"、"M2"、"auto"
    } = {}
) {
  const tier = chooseTier(qty, t1, t2);
  const m1 = data?.m1?.[tier];
  const m2 = data?.m2?.[tier];

  // 若強制指定市場：先用指定市場，沒有再後援另一個
  if (preferMarket === "M1") {
    const price = (m1 ?? m2 ?? null);
    return { unitPrice: price, sourceMarket: price == null ? null : (m1 != null ? "M1" : "M2"), tier };
  }
  if (preferMarket === "M2") {
    const price = (m2 ?? m1 ?? null);
    return { unitPrice: price, sourceMarket: price == null ? null : (m2 != null ? "M2" : "M1"), tier };
  }
  const picked =
      m1 == null && m2 == null
          ? { price: null, source: null }
          : m1 == null
              ? { price: m2, source: "M2" }
              : m2 == null
                  ? { price: m1, source: "M1" }
                  : tier === "low"
                      ? m1 <= m2
                          ? { price: m1, source: "M1" }
                          : { price: m2, source: "M2" }
                      : m1 >= m2
                          ? { price: m1, source: "M1" }
                          : { price: m2, source: "M2" };
  return { unitPrice: picked.price, sourceMarket: picked.source, tier };
}

function renderPrice(root, unitPrice, qty) {
  const priceEl = root.querySelector?.(".price");
  const totalEl = root.querySelector?.(".total-price");
  const unit = roundTo(unitPrice);
  const total = roundTo(unitPrice * Number(qty || 1));
  if (priceEl) priceEl.textContent = unit == null ? "—" : formatCurrency(unit);
  if (totalEl)
    totalEl.textContent = total == null ? "—" : formatCurrency(total);
}

// Ensure a card has a .price element; if missing, create one in .des or append to card
function ensureCardHasPriceEl(card) {
  let priceEl = card.querySelector(".price");
  if (!priceEl) {
    const des = card.querySelector(".des") || card;
    priceEl = document.createElement("h4");
    priceEl.className = "price";
    priceEl.textContent = "載入中…";
    // insert before cart button if exists, else append
    const cart = card.querySelector(".cart");
    if (cart && cart.parentElement === card) {
      card.insertBefore(priceEl, cart);
    } else {
      des.appendChild(priceEl);
    }
  }
  return priceEl;
}

// Listing / home card prices: treat qty=1 => tier=high, pick highest of market1/2
async function hydrateListingCardPrices() {
  const cards = document.querySelectorAll(".pro[data-product-id]");
  if (!cards.length) {
    console.debug("[pricing] no cards with data-product-id");
    return;
  }

  for (const card of cards) {
    const pid = Number(card.dataset.productId);
    const priceEl = ensureCardHasPriceEl(card);
    if (!pid) continue;

    priceEl.textContent = "載入中…";
    try {
      const res = await fetch(API_MARKET_TIERS(pid));
      if (!res.ok) throw new Error("server error" + (await res.text()));
      const tiers = normalizeTiers(await res.json());
      const { unitPrice, sourceMarket } = pickPriceForQty(
          tiers,
          1,
          DEFAULT_T1,
          DEFAULT_T2,
          {
            smallPolicy: "highest",
            midPolicy: "highest",
            largePolicy: "lowest",
          }
      );
      card.dataset.market = sourceMarket || "";
      renderPrice(card, unitPrice, 1);
    } catch (e) {
      console.warn("[pricing] card price failed:", e);
      // Fallback: 使用本地快取價格（若有）
      const cached = Number(localStorage.getItem(`price:${pid}`));
      if (Number.isFinite(cached) && cached > 0) {
        renderPrice(card, cached, 1);
      } else {
        priceEl.textContent = "—";
      }
    }
  }
}

// Single product page: body[data-product-id], quantity input (#sp-qty or common ids)
async function initSingleProductPrice() {
  const body = document.querySelector("body[data-product-id]");
  if (!body) return;
  const pid = Number(body.dataset.productId);
  const t1 = Number(body.dataset.tier1 || DEFAULT_T1);
  const t2 = Number(body.dataset.tier2 || DEFAULT_T2);
  const qtyInput = document.querySelector('#sp-qty, input.qty, input[name="quantity"], #quantity');

  // ★ 新增：當前市場（預設 M1=台北一）
  let currentMarket = "M1";

  function apply(tiers) {
    const qty = Number(qtyInput?.value || 1);
    // ★ 帶入 preferMarket: currentMarket
    const { unitPrice, tier } = pickPriceForQty(
        tiers, qty, t1, t2,
        { smallPolicy: "highest", midPolicy: "highest", largePolicy: "lowest", preferMarket: currentMarket }
    );
    document.body.dataset.market = currentMarket; // 標記目前市場
    document.body.dataset.tier = tier || "";
    renderPrice(document, unitPrice, qty);
  }

  try {
    const res = await fetch(API_MARKET_TIERS(pid));
    if (!res.ok) throw new Error("server error " + (await res.text()));
    const tiers = normalizeTiers(await res.json());

    // ★ 新增：綁定「台北一 / 台北二」切換
    document.querySelectorAll(".market-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        const mk = (btn.dataset.market || "").toLowerCase(); // taipei1 / taipei2
        currentMarket = mk.includes("2") ? "M2" : "M1";
        // 樣式切換（可選）
        document.querySelectorAll(".market-btn").forEach(b => b.classList.toggle("active", b === btn));
        apply(tiers); // 依目前數量重算
      });
    });

    // 初始化價格（預設 M1，qty=1 會走上價）
    apply(tiers);
    qtyInput?.addEventListener("input", () => apply(tiers));
    qtyInput?.addEventListener("change", () => apply(tiers));
  } catch (e) {
    console.warn("[single price] failed", e);
    // Fallback: 若 API 失敗，改用本地快取價格顯示
    const container =
        document.querySelector(".single-pro-details") || document.body;
    if (!document.querySelector(".price")) {
      const h = document.createElement("h2");
      h.innerHTML =
          '單價：<span class="price" aria-live="polite">—</span> <small class="unit"></small>';
      container.prepend(h);
      const subtotalP = document.createElement("p");
      subtotalP.className = "subtotal";
      subtotalP.innerHTML = '小計：<span class="total-price">—</span>';
      container.appendChild(subtotalP);
    }
    const cached = Number(localStorage.getItem(`price:${pid}`));
    const qty = Number(qtyInput?.value || 1);
    if (Number.isFinite(cached) && cached > 0) {
      renderPrice(document, cached, qty);
    } else {
      // 沒快取就維持破折號
      renderPrice(document, null, qty);
    }
  }
}

// Auto-run after DOM ready (won't interfere with your existing listeners)
document.addEventListener("DOMContentLoaded", () => {
  try {
    hydrateListingCardPrices();
  } catch (e) {
    console.warn(e);
  }
  try {
    initSingleProductPrice();
  } catch (e) {
    console.warn(e);
  }
});
/* ======== End Pricing Module ======== */

// 金額格式（四捨五入到整數，TWD）
function formatCurrency(n) {
  if (n == null || isNaN(Number(n))) return "—"; // 沒有金額時顯示破折號
  const rounded = Math.round(Number(n));
  return `NT$ ${rounded.toLocaleString("zh-TW")}`;
}

// 單列小計：用 <tr data-price="單價"> * .qty-input
function recalcRowSubtotal(row) {
  const unit = Number(row.dataset.price || 0);
  const qtyInput = row.querySelector(".qty-input");
  const qty = Number(qtyInput?.value || 1);
  const subtotal = unit * qty;

  const cell = row.querySelector(".line-subtotal");
  if (cell) cell.textContent = formatCurrency(subtotal);
  return subtotal;
}

// 統一更新總金額到三個位置
function renderTotals(sum) {
  const sumEl = document.getElementById("summary-subtotal");
  const totalEl = document.getElementById("summary-total");
  const bottom = document.getElementById("bottom-total");
  if (sumEl) sumEl.textContent = formatCurrency(sum);
  if (totalEl) totalEl.textContent = formatCurrency(sum);
  if (bottom) bottom.textContent = `總金額：${formatCurrency(sum)}`;
}

// 重新計算整張表的小計/總計（前端算）
function recalcTotals() {
  const tbody =
      getCartTbody?.() || document.querySelector("#cart tbody, #cart-tbody");
  if (!tbody) return;

  let sum = 0;
  tbody.querySelectorAll("tr[data-product-id]").forEach((row) => {
    sum += recalcRowSubtotal(row);
  });
  renderTotals(sum);
}


// ====== Helpers for cart tiered pricing on +/- in cart ======
const __tiersCache = new Map();
async function getTiers(pid) {
  if (__tiersCache.has(pid)) return __tiersCache.get(pid);
  const res = await fetch(API_MARKET_TIERS(pid));
  if (!res.ok) throw new Error(await res.text());
  const tiers = normalizeTiers(await res.json());
  __tiersCache.set(pid, tiers);
  return tiers;
}

function getPreferMarketForRow(row) {
  const pid = row?.dataset?.productId;
  const code = row?.dataset?.marketCode
      || localStorage.getItem(`market:${pid}`)
      || "M1";
  return String(code).toUpperCase() === "M2" ? "M2" : "M1";
}

async function updateRowPriceForQty(row, qty) {
  const pid = Number(row?.dataset?.productId || 0);
  if (!pid) return;
  const tiers = await getTiers(pid);
  const preferMarket = "auto";
  const picked = pickPriceForQty(tiers, qty, DEFAULT_T1, DEFAULT_T2, {
    smallPolicy: "highest",
    midPolicy:   "highest",
    largePolicy: "lowest",
    preferMarket
  });
  const unitPrice = Number(picked?.unitPrice || 0);
  row.dataset.price = String(unitPrice);

  const unitTd = row.querySelector(".unit-price");
  if (unitTd) {
    const label = preferMarket === "M2" ? "台北二" : "台北一";
    unitTd.textContent = formatCurrency(unitPrice);
  }
}
