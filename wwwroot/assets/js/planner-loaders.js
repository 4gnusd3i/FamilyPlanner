async function loadAll() {
  try {
    await loadFamily();
    await Promise.all([loadEvents(), loadMeals(), loadBudget(), loadNotes(), loadShopping(), loadUpcoming()]);
  } catch (error) {
    handleError(error);
  } finally {
    scheduleKioskLayout();
  }
}

async function loadFamily() {
  const response = await apiFetch("/api/family");
  familyMembers = await response.json();
  updateOwnerSelects();
  const board = document.getElementById("familyBoard");
  const addMemberButton = `
    <button type="button" class="family-tile add-family-btn" onclick="openMemberModal()" aria-label="${escapeHtml(t("family.new_member_aria"))}">
      <span class="family-avatar-shell family-avatar-add" aria-hidden="true">+</span>
      <span class="family-name">${escapeHtml(t("family.add_member"))}</span>
    </button>`;

  if (!familyMembers.length) {
    board.innerHTML = addMemberButton;
  } else {
    board.innerHTML = familyMembers.map((member, index) => {
      const avatar = member.avatar_url
        ? `<img src="${escapeHtml(member.avatar_url)}" alt="${escapeHtml(member.name)}" class="member-avatar family-member-avatar">`
        : `<span class="avatar-emoji">${memberEmojis[index % memberEmojis.length]}</span>`;

      return `<div class="family-tile family-avatar" role="button" tabindex="0" aria-label="${escapeHtml(t("family.view_profile_for", { name: member.name }))}" title="${escapeHtml(member.name)}" style="--member-color:${safeColor(member.color, "#fefce8")}" draggable="true" data-id="${member.id}" data-name="${escapeHtml(member.name)}" onclick="showProfile(${member.id})">
        <span class="family-avatar-shell">
          ${avatar}
        </span>
        <span class="family-name">${escapeHtml(member.name)}</span>
      </div>`;
    }).join("") + addMemberButton;
  }

  initDragDrop();
}

function initDragDrop() {
  document.querySelectorAll(".day-box").forEach((zone) => {
    if (zone.dataset.dropBound === "true") return;
    zone.dataset.dropBound = "true";
    zone.addEventListener("dragenter", (event) => {
      event.preventDefault();
      zone.classList.add("is-drop-target");
    });
    zone.addEventListener("dragover", (event) => {
      event.preventDefault();
      zone.classList.add("is-drop-target");
    });
    zone.addEventListener("dragleave", () => {
      zone.classList.remove("is-drop-target");
    });
    zone.addEventListener("drop", (event) => {
      event.preventDefault();
      zone.classList.remove("is-drop-target");
      const memberId = Number(event.dataTransfer.getData("text/plain"));
      if (memberId) openEventModal(null, zone.dataset.date, memberId);
    });
  });

  document.querySelectorAll(".family-avatar").forEach((avatar) => {
    if (avatar.dataset.dragBound === "true") return;
    avatar.dataset.dragBound = "true";
    avatar.addEventListener("dragstart", (event) => {
      event.dataTransfer.setData("text/plain", avatar.dataset.id);
      document.body.classList.add("is-dragging-family");
    });
    avatar.addEventListener("dragend", () => {
      document.body.classList.remove("is-dragging-family");
    });
    avatar.addEventListener("click", (event) => {
      if (avatar.dataset.suppressClick !== "true") return;
      event.preventDefault();
      event.stopImmediatePropagation();
      avatar.dataset.suppressClick = "false";
    }, true);
    avatar.addEventListener("pointerdown", beginTouchFamilyDrag);
    avatar.addEventListener("touchstart", beginNativeFamilyTouchDrag, { passive: false });
    avatar.addEventListener("keydown", (event) => {
      if (event.key !== "Enter" && event.key !== " ") return;
      event.preventDefault();
      showProfile(Number(avatar.dataset.id));
    });
  });
}

function beginTouchFamilyDrag(event) {
  if (event.pointerType === "mouse") return;
  const avatar = event.currentTarget;
  const memberId = Number(avatar.dataset.id);
  if (!memberId) return;

  let dragging = false;
  let currentZone = null;
  const pointerId = event.pointerId;
  const startX = event.clientX;
  const startY = event.clientY;
  const longPressTimer = window.setTimeout(() => {
    if (avatar.dataset.longPressActive === "true") return;
    avatar.dataset.longPressActive = "true";
    dragging = true;
    avatar.dataset.suppressClick = "true";
    document.body.classList.add("is-dragging-family");
    try {
      avatar.setPointerCapture?.(pointerId);
    } catch {
      // Synthetic touch events used by regression tests do not always own a browser pointer capture.
    }
    updateTouchDropTarget(startX, startY);
  }, 360);

  const clearCurrentZone = () => {
    currentZone?.classList.remove("is-drop-target");
    currentZone = null;
  };

  const updateTouchDropTarget = (clientX, clientY) => {
    const target = document.elementFromPoint(clientX, clientY)?.closest(".day-box");
    if (target === currentZone) return;
    clearCurrentZone();
    currentZone = target;
    currentZone?.classList.add("is-drop-target");
  };

  const cleanup = () => {
    window.clearTimeout(longPressTimer);
    clearCurrentZone();
    document.body.classList.remove("is-dragging-family");
    avatar.dataset.longPressActive = "false";
    try {
      avatar.releasePointerCapture?.(pointerId);
    } catch {
      // Ignore missing capture for canceled or synthetic touch drags.
    }
    window.setTimeout(() => {
      avatar.dataset.suppressClick = "false";
    }, 500);
    window.removeEventListener("pointermove", onMove, true);
    window.removeEventListener("pointerup", onEnd, true);
    window.removeEventListener("pointercancel", cleanup, true);
  };

  const onMove = (moveEvent) => {
    if (moveEvent.pointerId !== pointerId) return;
    const moved = Math.hypot(moveEvent.clientX - startX, moveEvent.clientY - startY);
    if (!dragging && moved > 14) {
      cleanup();
      return;
    }

    if (!dragging) return;
    moveEvent.preventDefault();
    updateTouchDropTarget(moveEvent.clientX, moveEvent.clientY);
  };

  const onEnd = (endEvent) => {
    if (endEvent.pointerId !== pointerId) return;
    window.clearTimeout(longPressTimer);
    if (dragging && currentZone) {
      endEvent.preventDefault();
      openEventModal(null, currentZone.dataset.date, memberId);
    }
    cleanup();
  };

  window.addEventListener("pointermove", onMove, true);
  window.addEventListener("pointerup", onEnd, true);
  window.addEventListener("pointercancel", cleanup, true);
}

function beginNativeFamilyTouchDrag(event) {
  const touch = event.changedTouches?.[0];
  if (!touch) return;

  const avatar = event.currentTarget;
  const memberId = Number(avatar.dataset.id);
  if (!memberId) return;
  if (avatar.dataset.longPressActive === "true") return;
  avatar.dataset.longPressActive = "true";

  let dragging = false;
  let currentZone = null;
  const touchId = touch.identifier;
  const startX = touch.clientX;
  const startY = touch.clientY;
  const longPressTimer = window.setTimeout(() => {
    dragging = true;
    avatar.dataset.suppressClick = "true";
    document.body.classList.add("is-dragging-family");
    updateTouchDropTarget(startX, startY);
  }, 360);

  const getTrackedTouch = (touchEvent) =>
    Array.from(touchEvent.changedTouches || []).find((item) => item.identifier === touchId) ||
    Array.from(touchEvent.touches || []).find((item) => item.identifier === touchId);

  const clearCurrentZone = () => {
    currentZone?.classList.remove("is-drop-target");
    currentZone = null;
  };

  const updateTouchDropTarget = (clientX, clientY) => {
    const target = document.elementFromPoint(clientX, clientY)?.closest(".day-box");
    if (target === currentZone) return;
    clearCurrentZone();
    currentZone = target;
    currentZone?.classList.add("is-drop-target");
  };

  const cleanup = () => {
    window.clearTimeout(longPressTimer);
    clearCurrentZone();
    document.body.classList.remove("is-dragging-family");
    avatar.dataset.longPressActive = "false";
    window.setTimeout(() => {
      avatar.dataset.suppressClick = "false";
    }, 500);
    document.removeEventListener("touchmove", onMove, true);
    document.removeEventListener("touchend", onEnd, true);
    document.removeEventListener("touchcancel", cleanup, true);
  };

  const onMove = (moveEvent) => {
    const activeTouch = getTrackedTouch(moveEvent);
    if (!activeTouch) return;
    const moved = Math.hypot(activeTouch.clientX - startX, activeTouch.clientY - startY);
    if (!dragging && moved > 14) {
      cleanup();
      return;
    }

    if (!dragging) return;
    moveEvent.preventDefault();
    updateTouchDropTarget(activeTouch.clientX, activeTouch.clientY);
  };

  const onEnd = (endEvent) => {
    const endedTouch = getTrackedTouch(endEvent);
    if (!endedTouch) return;
    window.clearTimeout(longPressTimer);
    if (dragging && currentZone) {
      endEvent.preventDefault();
      openEventModal(null, currentZone.dataset.date, memberId);
    }
    cleanup();
  };

  document.addEventListener("touchmove", onMove, { capture: true, passive: false });
  document.addEventListener("touchend", onEnd, { capture: true, passive: false });
  document.addEventListener("touchcancel", cleanup, true);
}

async function loadEvents() {
  const response = await apiFetch(`/api/events?start=${currentWeekStart}&end=${currentWeekEnd}`);
  eventsCache = await response.json();
  document.querySelectorAll(".day-content").forEach((container) => {
    container.innerHTML = "";
  });

  eventsCache.forEach((entry) => {
    const container = document.getElementById(`events-${entry.event_date}`);
    if (!container) return;
    const owner = familyMembers.find((member) => member.id === entry.owner_id);
    const ownerLine = owner
      ? `<div class="event-owner-line">${getMemberAvatar(owner, "small")}<span class="event-owner-name">${escapeHtml(owner.name)}</span></div>`
      : "";
    const payload = encodePayload(entry);
    const startDisplay = formatEventStart(entry);
    const endDisplay = formatEventEnd(entry);
    const generatedClass = entry.source_type ? ` source-${entry.source_type}` : "";
    const timeClass = startDisplay || endDisplay ? " has-time" : "";
    const eventAccent = safeColor(owner?.color, safeColor(entry.color, "#eaf4ff"));
    const interaction = entry.source_type === "birthday"
      ? `aria-label="${escapeHtml(entry.title)}"`
      : `onclick="viewEventFromJson('${payload}')"`;

    container.insertAdjacentHTML("beforeend", `<div class="event-item${timeClass}${generatedClass}" style="--event-accent:${eventAccent}" ${interaction}>
      ${ownerLine}
      <span class="event-title">${escapeHtml(entry.title)}</span>
      ${startDisplay ? `<span class="event-time event-start">${escapeHtml(startDisplay)}</span>` : ""}
      ${entry.note ? `<span class="event-more">${escapeHtml(t("events.more_hint"))}</span>` : ""}
      ${endDisplay ? `<span class="event-time event-end">${escapeHtml(endDisplay)}</span>` : ""}
    </div>`);
  });
}

async function loadUpcoming() {
  const response = await apiFetch("/api/events?upcoming=1");
  const events = await response.json();
  const container = document.getElementById("upcomingList");
  if (!events.length) {
    container.innerHTML = `<div class="empty-state">${escapeHtml(t("events.no_upcoming"))}</div>`;
    return;
  }

  container.innerHTML = events.map((entry) => {
    const date = parseDate(entry.event_date);
    const owner = familyMembers.find((member) => member.id === entry.owner_id);
    const ownerAvatar = owner ? getMemberAvatar(owner, "small") : "";
    const ownerLine = owner
      ? `<div class="upcoming-owner-line">${ownerAvatar}<span class="upcoming-owner-name">${escapeHtml(owner.name)}</span></div>`
      : "";
    const payload = encodePayload(entry);
    const generatedClass = entry.source_type ? ` source-${entry.source_type}` : "";
    const interaction = entry.source_type === "birthday"
      ? `aria-label="${escapeHtml(entry.title)}"`
      : `onclick="viewEventFromJson('${payload}')"`;
    const timeDisplay = formatEventTimeRange(entry, false);
    return `<div class="upcoming-item${generatedClass}" ${interaction}>
      <div class="upcoming-date">
        <span class="upcoming-dayname">${weekdayShort[normalizeWeekday(date)]}</span>
        <span class="upcoming-daynum">${date.getDate()}</span>
        <span class="upcoming-month">${date.toLocaleDateString(getCurrentLocale(), { month: "short" })}</span>
      </div>
      <div class="upcoming-content">
        ${ownerLine}
        <div class="upcoming-info">
          <span class="upcoming-title">${escapeHtml(entry.title)}</span>
          ${timeDisplay ? `<span class="upcoming-time">${escapeHtml(timeDisplay)}</span>` : ""}
        </div>
      </div>
    </div>`;
  }).join("");
}

async function loadMeals() {
  const response = await apiFetch("/api/meals");
  mealsCache = await response.json();
  const container = document.getElementById("mealsWeekGrid");

  container.innerHTML = weekdayShort.map((_, dayIndex) => {
    const entries = mealTypes.map((mealType) => {
      const meal = mealsCache.find((item) => item.day_of_week === dayIndex && item.meal_type === mealType.key);
      if (!meal) {
        return `<div class="meal-entry meal-empty" onclick="openMealModalForType(${dayIndex}, '${mealType.key}')">
          <span class="meal-type-label">${mealType.label}</span>
          <span class="meal-empty-text">+</span>
        </div>`;
      }

      const owner = familyMembers.find((member) => member.id === meal.owner_id);
      const ownerBadge = owner ? `<span class="meal-owner" style="background:${safeColor(owner.color)}">${escapeHtml(owner.name.charAt(0))}</span>` : "";
      const payload = encodePayload(meal);
      return `<div class="meal-entry meal-${mealType.key}" onclick="viewMealFromJson('${payload}')">
        <span class="meal-type-label">${mealType.label}</span>
        ${ownerBadge}
        <span class="meal-text">${escapeHtml(meal.meal)}</span>
      </div>`;
    }).join("");

    return `<div class="meal-day">
      <div class="meal-day-content">${entries}</div>
    </div>`;
  }).join("");
}

async function loadBudget() {
  const response = await apiFetch("/api/budget");
  const data = await response.json();
  const incomeContainer = document.getElementById("budgetIncomeDisplay").parentElement;
  const locale = getCurrentLocale();
  document.getElementById("budgetIncomeDisplay").textContent = Number(data.income || 0).toLocaleString(locale);
  document.getElementById("budgetSpent").textContent = Number(data.spent || 0).toLocaleString(locale);
  document.getElementById("budgetLimit").textContent = Number(data.limit || 0).toLocaleString(locale);

  const percent = data.limit > 0 ? (data.spent / data.limit) * 100 : 0;
  const fill = document.getElementById("budgetFill");
  fill.style.width = `${Math.min(percent, 100)}%`;
  fill.className = percent > 100 ? "budget-fill danger" : percent > 80 ? "budget-fill warning" : "budget-fill";

  if (data.income === 0) incomeContainer.classList.add("empty");
  else incomeContainer.classList.remove("empty");

  const remaining = Number(data.remaining || 0);
  const remainingEl = document.getElementById("budgetRemaining");
  remainingEl.textContent = t("budget.remaining_format", { amount: remaining.toLocaleString(locale) });
  remainingEl.className = remaining >= 0 ? "budget-remaining positive" : "budget-remaining negative";
  scheduleKioskLayout();
}

async function loadExpenseHistory() {
  const response = await apiFetch("/api/budget");
  const data = await response.json();
  const container = document.getElementById("expenseList");
  if (!data.expenses || data.expenses.length === 0) {
    container.innerHTML = `<div class="empty-state">${escapeHtml(t("budget.no_expenses"))}</div>`;
    return;
  }

  container.innerHTML = data.expenses.map((expense) => {
    const owner = familyMembers.find((member) => member.id === expense.owner_id);
    const ownerAvatar = owner ? getMemberAvatar(owner, "small") : "";
    const locale = getCurrentLocale();
    const date = expense.expense_date ? parseDate(expense.expense_date).toLocaleDateString(locale, { day: "numeric", month: "short" }) : "";
    const label = expense.description || expense.category || t("budget.expense_default_label");
    return `<div class="shop-item expense-history-item">
      <span class="item-name">${escapeHtml(label)}</span>
      ${ownerAvatar}
      <span class="expense-meta">${escapeHtml(date)}</span>
      <span class="expense-amount">${Number(expense.amount).toLocaleString(locale)} kr</span>
      <button type="button" class="btn-small btn-danger" aria-label="${escapeHtml(t("budget.delete_expense_aria", { label }))}" onclick="deleteExpense(${expense.id})">${escapeHtml(t("common.delete"))}</button>
    </div>`;
  }).join("");
}

async function deleteExpense(id) {
  await apiFetch("/api/budget", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ delete_expense: true, id }),
  });
  await loadBudget();
  await loadExpenseHistory();
}

async function loadNotes() {
  const response = await apiFetch("/api/notes");
  notesCache = await response.json();
  const container = document.getElementById("notesList");
  const notesCard = container?.closest(".notes-card");
  if (!notesCache.length) {
    container.innerHTML = `<div class="empty-state empty-state-collapsible">${escapeHtml(t("notes.no_notes"))}</div>`;
    notesCard?.classList.add("is-empty");
    scheduleKioskLayout();
    return;
  }

  notesCard?.classList.remove("is-empty");
  container.innerHTML = notesCache.slice(0, 5).map((note) => {
    const owner = familyMembers.find((member) => member.id === note.owner_id);
    const ownerAvatar = owner ? getMemberAvatar(owner, "small") : "";
    const payload = encodePayload(note);
    return `<div class="note-item" onclick="viewNoteFromJson('${payload}')">
      ${ownerAvatar}
      <span class="item-name">${escapeHtml(note.title)}</span>
    </div>`;
  }).join("");
  scheduleKioskLayout();
}

async function loadShopping() {
  const response = await apiFetch("/api/shopping");
  shoppingCache = await response.json();
  syncShoppingDeletionTimers(shoppingCache);
  const container = document.getElementById("shoppingList");
  if (!shoppingCache.length) {
    container.innerHTML = `<div class="empty-state">${escapeHtml(t("shopping.no_items"))}</div>`;
    scheduleKioskLayout();
    return;
  }

  container.innerHTML = shoppingCache.slice(0, 8).map((item) => {
    const owner = familyMembers.find((member) => member.id === item.owner_id);
    const ownerAvatar = owner ? getMemberAvatar(owner, "small") : "";
    const payload = encodePayload(item);
    const progress = getShoppingDeleteProgress(item);
    const pendingClass = item.done ? " is-delete-pending" : "";
    const progressStyle = item.done
      ? ` style="--delete-duration:${SHOPPING_DELETE_DELAY_MS}ms;--delete-delay-offset:-${progress.elapsed}ms"`
      : "";
    return `<div class="shop-item${pendingClass}"${progressStyle} onclick="viewShoppingFromJson('${payload}')">
      <button type="button" class="shop-check ${item.done ? "checked" : ""}" aria-label="${escapeHtml(item.done ? t("shopping.keep_item") : t("shopping.mark_purchased"))}" aria-pressed="${item.done ? "true" : "false"}" onclick="event.stopPropagation(); toggleShop(${item.id})">${item.done ? "&#10003;" : ""}</button>
      ${ownerAvatar}
      <span class="item-name shop-name ${item.done ? "done" : ""}">${escapeHtml(item.item)}</span>
      <span class="shop-qty">${item.quantity}</span>
    </div>`;
  }).join("");
  shoppingCache.filter((item) => item.done).forEach(scheduleShoppingDeletion);
  scheduleKioskLayout();
}

function syncShoppingDeletionTimers(items) {
  const pendingIds = new Set(items.filter((item) => item.done).map((item) => item.id));
  Array.from(shoppingDeleteTimers.keys()).forEach((id) => {
    if (!pendingIds.has(id)) {
      clearShoppingDeletion(id);
    }
  });
}

function scheduleShoppingDeletion(item) {
  clearShoppingDeletion(item.id);
  const progress = getShoppingDeleteProgress(item);
  const remaining = Math.max(0, SHOPPING_DELETE_DELAY_MS - progress.elapsed);
  const timer = setTimeout(() => {
    shoppingDeleteTimers.delete(item.id);
    loadShopping().catch(handleError);
  }, remaining + 100);
  shoppingDeleteTimers.set(item.id, timer);
}

function clearShoppingDeletion(id) {
  const timer = shoppingDeleteTimers.get(id);
  if (timer) {
    clearTimeout(timer);
    shoppingDeleteTimers.delete(id);
  }
}

function getShoppingDeleteProgress(item) {
  const doneAt = item.done_at ? Date.parse(item.done_at) : Date.now();
  const elapsed = Number.isFinite(doneAt) ? Date.now() - doneAt : 0;
  return { elapsed: Math.min(Math.max(elapsed, 0), SHOPPING_DELETE_DELAY_MS) };
}

function setText(id, value) {
  const element = document.getElementById(id);
  if (element) {
    element.textContent = value;
  }
}

let kioskLayoutFrame = null;

function scheduleKioskLayout() {
  if (kioskLayoutFrame !== null) {
    cancelAnimationFrame(kioskLayoutFrame);
  }

  kioskLayoutFrame = requestAnimationFrame(() => {
    kioskLayoutFrame = null;
    syncKioskScale();
    syncKioskLeftColumnSizing();
  });
}

function scheduleKioskColumnSizing() {
  scheduleKioskLayout();
}

function syncKioskScale() {
  const isKiosk = window.innerWidth >= KIOSK_BREAKPOINT;
  const scale = isKiosk
    ? Math.min(window.innerWidth / KIOSK_BASE_WIDTH, window.innerHeight / KIOSK_BASE_HEIGHT)
    : 1;
  const canvasWidth = isKiosk ? window.innerWidth / scale : KIOSK_BASE_WIDTH;
  const canvasHeight = isKiosk ? window.innerHeight / scale : KIOSK_BASE_HEIGHT;

  document.documentElement.style.setProperty("--kiosk-scale", scale.toFixed(4));
  document.documentElement.style.setProperty("--kiosk-canvas-width", `${canvasWidth.toFixed(2)}px`);
  document.documentElement.style.setProperty("--kiosk-canvas-height", `${canvasHeight.toFixed(2)}px`);
}

function syncKioskLeftColumnSizing() {
  const leftPanel = document.querySelector(".planner-page .side-panel:not(.side-panel-right)");
  if (!leftPanel) return;

  if (window.matchMedia(`(max-width: ${KIOSK_BREAKPOINT - 1}px)`).matches) {
    leftPanel.style.removeProperty("--left-notes-max");
    return;
  }

  const budgetCard = leftPanel.querySelector(".budget-card");
  const notesCard = leftPanel.querySelector(".notes-card");
  const notesList = document.getElementById("notesList");
  if (!budgetCard || !notesCard || !notesList) return;

  const panelHeight = leftPanel.clientHeight;
  const budgetHeight = budgetCard.offsetHeight;
  const panelStyles = getComputedStyle(leftPanel);
  const gap = Number.parseFloat(panelStyles.rowGap || panelStyles.gap || "0") || 0;
  const shoppingMinimum = Number.parseFloat(panelStyles.getPropertyValue("--left-shopping-min")) || 0;
  const remainingAfterBudget = Math.max(0, panelHeight - budgetHeight - (gap * 2));
  const notesMax = Math.max(0, Math.min(remainingAfterBudget / 2, shoppingMinimum || remainingAfterBudget));

  const hasNoteItems = notesList.querySelector(".note-item") !== null;
  notesCard.classList.toggle("is-empty", !hasNoteItems);
  leftPanel.style.setProperty("--left-notes-max", `${notesMax}px`);
}

function formatEventTimeRange(entry, allDayFallback = true) {
  if (entry.start_time && entry.end_time) {
    return `${entry.start_time.slice(0, 5)} - ${entry.end_time.slice(0, 5)}`;
  }
  if (entry.start_time) {
    return entry.start_time.slice(0, 5);
  }
  return allDayFallback ? t("events.all_day") : "";
}

function formatEventStart(entry) {
  return entry.start_time ? entry.start_time.slice(0, 5) : "";
}

function formatEventEnd(entry) {
  return entry.end_time ? entry.end_time.slice(0, 5) : "";
}
