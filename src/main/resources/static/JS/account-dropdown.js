// 帳戶選單功能
(function () {
  "use strict";

  // 獲取 DOM 元素
  const accountBtn = document.querySelector(".account-btn");
  const accountDropdown = document.querySelector(".account-dropdown");
  const menuItems = document.querySelectorAll(".menu-item");

  // 狀態管理
  let isOpen = false;
  let currentFocusIndex = -1;

  // 初始化
  function init() {
    if (!accountBtn || !accountDropdown) return;

    bindEvents();
    setupAccessibility();
  }

  // 綁定事件
  function bindEvents() {
    // 點擊頭像按鈕
    accountBtn.addEventListener("click", toggleDropdown);

    // 點擊外部區域關閉
    document.addEventListener("click", handleOutsideClick);

    // ESC 鍵關閉
    document.addEventListener("keydown", handleKeydown);

    // 選單項目點擊
    menuItems.forEach((item) => {
      item.addEventListener("click", handleMenuItemClick);
      item.addEventListener("keydown", handleMenuItemKeydown);
    });
  }

  // 設置無障礙功能
  function setupAccessibility() {
    accountBtn.setAttribute("aria-haspopup", "menu");
    accountBtn.setAttribute("aria-expanded", "false");

    accountDropdown.setAttribute("role", "menu");
    accountDropdown.setAttribute("aria-label", "帳戶選單");

    menuItems.forEach((item, index) => {
      item.setAttribute("role", "menuitem");
      item.setAttribute("tabindex", "-1");
      item.setAttribute("data-index", index);
    });
  }

  // 切換選單顯示/隱藏
  function toggleDropdown() {
    isOpen = !isOpen;

    if (isOpen) {
      showDropdown();
    } else {
      hideDropdown();
    }
  }

  // 顯示選單
  function showDropdown() {
    accountDropdown.classList.add("show");
    accountDropdown.removeAttribute("hidden");
    accountBtn.setAttribute("aria-expanded", "true");
    currentFocusIndex = -1;

    // 聚焦到第一個選單項目
    setTimeout(() => {
      focusMenuItem(0);
    }, 100);
  }

  // 隱藏選單
  function hideDropdown() {
    accountDropdown.classList.remove("show");
    accountDropdown.setAttribute("hidden", "");
    accountBtn.setAttribute("aria-expanded", "false");
    currentFocusIndex = -1;

    // 將焦點返回到按鈕
    accountBtn.focus();
  }

  // 處理外部點擊
  function handleOutsideClick(event) {
    if (
      !accountDropdown.contains(event.target) &&
      !accountBtn.contains(event.target)
    ) {
      if (isOpen) {
        hideDropdown();
        isOpen = false;
      }
    }
  }

  // 處理鍵盤事件
  function handleKeydown(event) {
    if (!isOpen) return;

    switch (event.key) {
      case "Escape":
        event.preventDefault();
        hideDropdown();
        isOpen = false;
        break;
      case "ArrowDown":
        event.preventDefault();
        navigateMenu("down");
        break;
      case "ArrowUp":
        event.preventDefault();
        navigateMenu("up");
        break;
    }
  }

  // 處理選單項目鍵盤事件
  function handleMenuItemKeydown(event) {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      event.target.click();
    }
  }

  // 導航選單
  function navigateMenu(direction) {
    const itemCount = menuItems.length;

    if (direction === "down") {
      currentFocusIndex = (currentFocusIndex + 1) % itemCount;
    } else {
      currentFocusIndex =
        currentFocusIndex <= 0 ? itemCount - 1 : currentFocusIndex - 1;
    }

    focusMenuItem(currentFocusIndex);
  }

  // 聚焦選單項目
  function focusMenuItem(index) {
    // 移除之前的焦點
    menuItems.forEach((item) => {
      item.setAttribute("tabindex", "-1");
    });

    // 設置新的焦點
    if (index >= 0 && index < menuItems.length) {
      const targetItem = menuItems[index];
      targetItem.setAttribute("tabindex", "0");
      targetItem.focus();
      currentFocusIndex = index;
    }
  }

  // 處理選單項目點擊
  function handleMenuItemClick(event) {
    const action = event.currentTarget.textContent.trim();

    // 這裡可以根據不同動作執行相應功能
    console.log("執行動作:", action);

    // 登出特殊處理
    if (action === "登出") {
      if (confirm("確定要登出嗎？")) {
        // 執行登出邏輯
        console.log("執行登出");
      }
    }

    // 關閉選單
    hideDropdown();
    isOpen = false;
  }

  // 當 DOM 加載完成後初始化
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
