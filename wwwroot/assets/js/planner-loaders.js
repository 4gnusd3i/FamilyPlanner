async function loadAll() {
  try {
    await loadFamily();
    await Promise.all([loadEvents(), loadMeals(), loadBudget(), loadNotes(), loadShopping(), loadUpcoming()]);
  } catch (error) {
    handleError(error);
  } finally {
    scheduleKioskColumnSizing();
  }
}

async function loadFamily() {
  const response = await apiFetch("/api/family");
  familyMembers = await response.json();
  updateOwnerSelects();
  const board = document.getElementById("familyBoard");
  const addMemberButton = `
    <button type="button" class="family-tile add-family-btn" onclick="openMemberModal()" aria-label="Nytt familiemedlem">
      <span class="family-avatar-shell family-avatar-add" aria-hidden="true">+</span>
      <span class="family-name">Legg til</span>
    </button>`;

  if (!familyMembers.length) {
    board.innerHTML = addMemberButton;
  } else {
    board.innerHTML = familyMembers.map((member, index) => {
      const avatar = member.avatar_url
        ? `<img src="${member.avatar_url}" alt="${escapeHtml(member.name)}" class="member-avatar family-member-avatar">`
        : `<span class="avatar-emoji">${memberEmojis[index % memberEmojis.length]}</span>`;

      return `<div class="family-tile family-avatar" role="button" tabindex="0" aria-label="Vis profil for ${escapeHtml(member.name)}" title="${escapeHtml(member.name)}" style="--member-color:${member.color || "#fefce8"}" draggable="true" data-id="${member.id}" data-name="${escapeHtml(member.name)}" onclick="showProfile(${member.id})">
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
    avatar.addEventListener("keydown", (event) => {
      if (event.key !== "Enter" && event.key !== " ") return;
      event.preventDefault();
      showProfile(Number(avatar.dataset.id));
    });
  });
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
    const timeDisplay = formatEventTimeRange(entry, false);
    const generatedClass = entry.source_type ? ` source-${entry.source_type}` : "";
    const timeClass = timeDisplay ? " has-time" : "";
    const eventAccent = owner?.color || entry.color || "#eaf4ff";
    const interaction = entry.source_type === "birthday"
      ? `aria-label="${escapeHtml(entry.title)}"`
      : `onclick="openEventModalFromJson('${payload}')"`;

    container.insertAdjacentHTML("beforeend", `<div class="event-item${timeClass}${generatedClass}" style="--event-accent:${eventAccent}" ${interaction}>
      ${ownerLine}
      <span class="event-title">${escapeHtml(entry.title)}</span>
      ${timeDisplay ? `<span class="event-time">${escapeHtml(timeDisplay)}</span>` : ""}
      ${entry.note ? `<span class="event-description">${escapeHtml(entry.note)}</span>` : ""}
    </div>`);
  });
}

async function loadUpcoming() {
  const response = await apiFetch("/api/events?upcoming=1");
  const events = await response.json();
  const container = document.getElementById("upcomingList");
  if (!events.length) {
    container.innerHTML = '<div class="empty-state">Ingen kommende avtaler</div>';
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
      : `onclick="openEventModalFromJson('${payload}')"`;
    const timeDisplay = formatEventTimeRange(entry, false);
    return `<div class="upcoming-item${generatedClass}" ${interaction}>
      <div class="upcoming-date">
        <span class="upcoming-dayname">${weekdayShort[normalizeWeekday(date)]}</span>
        <span class="upcoming-daynum">${date.getDate()}</span>
        <span class="upcoming-month">${date.toLocaleDateString("no-NO", { month: "short" })}</span>
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
      const ownerBadge = owner ? `<span class="meal-owner" style="background:${owner.color}">${escapeHtml(owner.name.charAt(0))}</span>` : "";
      return `<div class="meal-entry meal-${mealType.key}" onclick="openMealModal(${dayIndex}, ${meal.id})">
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
  document.getElementById("budgetIncomeDisplay").textContent = Number(data.income || 0).toLocaleString("no-NO");
  document.getElementById("budgetSpent").textContent = Number(data.spent || 0).toLocaleString("no-NO");
  document.getElementById("budgetLimit").textContent = Number(data.limit || 0).toLocaleString("no-NO");

  const percent = data.limit > 0 ? (data.spent / data.limit) * 100 : 0;
  const fill = document.getElementById("budgetFill");
  fill.style.width = `${Math.min(percent, 100)}%`;
  fill.className = percent > 100 ? "budget-fill danger" : percent > 80 ? "budget-fill warning" : "budget-fill";

  if (data.income === 0) incomeContainer.classList.add("empty");
  else incomeContainer.classList.remove("empty");

  const remaining = Number(data.remaining || 0);
  const remainingEl = document.getElementById("budgetRemaining");
  remainingEl.textContent = `${remaining.toLocaleString("no-NO")} kr gjenstar`;
  remainingEl.className = remaining >= 0 ? "budget-remaining positive" : "budget-remaining negative";
  scheduleKioskColumnSizing();
}

async function loadExpenseHistory() {
  const response = await apiFetch("/api/budget");
  const data = await response.json();
  const container = document.getElementById("expenseList");
  if (!data.expenses || data.expenses.length === 0) {
    container.innerHTML = '<div class="empty-state">Ingen utgifter</div>';
    return;
  }

  container.innerHTML = data.expenses.map((expense) => {
    const owner = familyMembers.find((member) => member.id === expense.owner_id);
    const ownerAvatar = owner ? getMemberAvatar(owner, "small") : "";
    const date = expense.expense_date ? parseDate(expense.expense_date).toLocaleDateString("no-NO", { day: "numeric", month: "short" }) : "";
    const label = expense.description || expense.category || "Utgift";
    return `<div class="shop-item expense-history-item">
      <span class="item-name">${escapeHtml(label)}</span>
      ${ownerAvatar}
      <span class="expense-meta">${escapeHtml(date)}</span>
      <span class="expense-amount">${Number(expense.amount).toLocaleString("no-NO")} kr</span>
      <button type="button" class="btn-small btn-danger expense-delete-btn" aria-label="Slett utgift ${escapeHtml(label)}" onclick="deleteExpense(${expense.id})">Slett</button>
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
    container.innerHTML = '<div class="empty-state empty-state-collapsible">Ingen notater</div>';
    notesCard?.classList.add("is-empty");
    scheduleKioskColumnSizing();
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
  scheduleKioskColumnSizing();
}

async function loadShopping() {
  const response = await apiFetch("/api/shopping");
  shoppingCache = await response.json();
  syncShoppingDeletionTimers(shoppingCache);
  const container = document.getElementById("shoppingList");
  if (!shoppingCache.length) {
    container.innerHTML = '<div class="empty-state">Vi har det vi trenger!</div>';
    scheduleKioskColumnSizing();
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
    return `<div class="shop-item${pendingClass}"${progressStyle}>
      <button type="button" class="shop-check ${item.done ? "checked" : ""}" aria-label="${item.done ? "Behold vare" : "Marker vare som handlet"}" aria-pressed="${item.done ? "true" : "false"}" onclick="toggleShop(${item.id})">${item.done ? "&#10003;" : ""}</button>
      ${ownerAvatar}
      <span class="item-name shop-name ${item.done ? "done" : ""}" onclick="openShoppingModalFromJson('${payload}')">${escapeHtml(item.item)}</span>
      <span class="shop-qty">${item.quantity}</span>
    </div>`;
  }).join("");
  shoppingCache.filter((item) => item.done).forEach(scheduleShoppingDeletion);
  scheduleKioskColumnSizing();
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

let kioskSizingFrame = null;

function scheduleKioskColumnSizing() {
  if (kioskSizingFrame !== null) {
    cancelAnimationFrame(kioskSizingFrame);
  }

  kioskSizingFrame = requestAnimationFrame(() => {
    kioskSizingFrame = null;
    syncKioskLeftColumnSizing();
  });
}

function syncKioskLeftColumnSizing() {
  const leftPanel = document.querySelector(".planner-page .side-panel:not(.side-panel-right)");
  if (!leftPanel) return;

  if (window.matchMedia("(max-width: 899px)").matches) {
    leftPanel.style.removeProperty("--left-notes-max");
    return;
  }

  const budgetCard = leftPanel.querySelector(".budget-card");
  const notesCard = leftPanel.querySelector(".notes-card");
  const notesList = document.getElementById("notesList");
  if (!budgetCard || !notesCard || !notesList) return;

  const panelHeight = leftPanel.getBoundingClientRect().height;
  const budgetHeight = budgetCard.getBoundingClientRect().height;
  const panelStyles = getComputedStyle(leftPanel);
  const gap = Number.parseFloat(panelStyles.rowGap || panelStyles.gap || "0") || 0;
  const remainingAfterBudget = Math.max(0, panelHeight - budgetHeight - (gap * 2));
  const notesMax = Math.max(0, remainingAfterBudget / 2);

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
  return allDayFallback ? "Hele dagen" : "";
}
