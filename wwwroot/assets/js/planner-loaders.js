async function loadAll() {
  try {
    await loadFamily();
    await Promise.all([loadEvents(), loadMeals(), loadBudget(), loadMedicines(), loadNotes(), loadShopping(), loadUpcoming()]);
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
  const addMemberButton = '<button type="button" class="btn btn-primary icon-action add-family-btn" onclick="openMemberModal()" aria-label="Nytt familiemedlem">+</button>';

  if (!familyMembers.length) {
    board.innerHTML = addMemberButton;
  } else {
    board.innerHTML = familyMembers.map((member, index) => {
      const avatar = member.avatar_url
        ? `<img src="${member.avatar_url}" alt="${escapeHtml(member.name)}" class="member-avatar" style="width:36px;height:36px;">`
        : `<span class="avatar-emoji">${memberEmojis[index % memberEmojis.length]}</span>`;

      return `<div class="family-avatar" role="button" tabindex="0" aria-label="Vis profil for ${escapeHtml(member.name)}" title="${escapeHtml(member.name)}" style="background:${member.color || "#fefce8"}" draggable="true" data-id="${member.id}" data-name="${escapeHtml(member.name)}" onclick="showProfile(${member.id})">
      ${avatar}
      <span class="family-name sr-only">${escapeHtml(member.name)}</span>
    </div>`;
    }).join("") + addMemberButton;
  }

  setCount("familyCountLabel", familyMembers.length);

  initDragDrop();
  await loadAssignments();
}

async function loadAssignments() {
  const response = await apiFetch("/api/family/assignments");
  const data = await response.json();
  familyAssignments = {};
  (data.assignments || []).forEach((assignment) => {
    if (!familyAssignments[assignment.day_of_week]) familyAssignments[assignment.day_of_week] = [];
    familyAssignments[assignment.day_of_week].push(assignment);
  });
  renderAssignments();
}

function renderAssignments() {
  for (let day = 0; day < 7; day += 1) {
    const zone = document.getElementById(`tasks-${day}`);
    if (!zone) continue;
    zone.innerHTML = (familyAssignments[day] || []).map((assignment) => {
      const member = familyMembers.find((item) => item.id === assignment.family_member_id);
      if (!member) return "";
      const emoji = { medicine: "💊", doctor: "👨‍⚕️", activity: "⚽", meal: "🍽️" }[assignment.activity_type] || "📌";
      const avatar = member.avatar_url
        ? `<img src="${member.avatar_url}" class="task-avatar" alt="${escapeHtml(member.name)}">`
        : `<span class="task-avatar-text">${memberEmojis[member.id % memberEmojis.length]}</span>`;
      return `<div class="task-chip" style="background:${member.color}" onclick="openAssignModal(${day}, ${member.id})">${avatar}${emoji}</div>`;
    }).join("");
  }
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
      const day = Number(zone.dataset.day);
      if (memberId) openAssignModal(day, memberId);
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
    const ownerAvatar = owner ? getMemberAvatar(owner, "small") : "";
    const payload = encodePayload(entry);
    const timeDisplay = entry.start_time && entry.end_time
      ? `${entry.start_time.slice(0, 5)} - ${entry.end_time.slice(0, 5)}`
      : entry.start_time ? entry.start_time.slice(0, 5) : "";
    const className = timeDisplay ? " has-time" : "";

    container.insertAdjacentHTML("beforeend", `<div class="event-item${className}" style="border-left-color:${entry.color || "#4f46e5"}" onclick="openEventModalFromJson('${payload}')">
      ${ownerAvatar}
      ${timeDisplay ? `<span class="event-time">${timeDisplay}</span>` : ""}
      <span class="event-title">${escapeHtml(entry.title)}</span>
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

  container.innerHTML = events.slice(0, 4).map((entry) => {
    const date = parseDate(entry.event_date);
    const owner = familyMembers.find((member) => member.id === entry.owner_id);
    const ownerAvatar = owner ? getMemberAvatar(owner, "small") : "";
    const payload = encodePayload(entry);
    return `<div class="upcoming-item" onclick="openEventModalFromJson('${payload}')">
      <div class="upcoming-date">
        <span class="upcoming-dayname">${weekdayShort[normalizeWeekday(date)]}</span>
        <span class="upcoming-daynum">${date.getDate()}</span>
        <span class="upcoming-month">${date.toLocaleDateString("no-NO", { month: "short" })}</span>
      </div>
      <div class="upcoming-content">
        ${ownerAvatar}
        <div class="upcoming-info">
          <span class="upcoming-title">${escapeHtml(entry.title)}</span>
          ${entry.start_time ? `<span class="upcoming-time">Kl. ${entry.start_time.slice(0, 5)}</span>` : ""}
        </div>
      </div>
    </div>`;
  }).join("");
}

async function loadMeals() {
  const response = await apiFetch("/api/meals");
  mealsCache = await response.json();
  const container = document.getElementById("mealsWeekGrid");

  container.innerHTML = weekdayShort.map((dayLabel, dayIndex) => {
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
      const mealShoppingPayload = encodeURIComponent(meal.note || meal.meal || "");
      return `<div class="meal-entry meal-${mealType.key}" onclick="openMealModal(${dayIndex}, ${meal.id})">
        <span class="meal-type-label">${mealType.label}</span>
        ${ownerBadge}
        <span class="meal-text">${escapeHtml(meal.meal)}</span>
        ${meal.note ? `<span class="meal-add-btn" onclick="event.stopPropagation(); addMealToShopping(${meal.id}, decodeURIComponent('${mealShoppingPayload}'))">🛒</span>` : ""}
      </div>`;
    }).join("");

    return `<div class="meal-day">
      <div class="meal-day-header">${dayLabel}</div>
      <div class="meal-day-content">${entries}</div>
    </div>`;
  }).join("");
}

async function addMealToShopping(mealId, item) {
  if (!confirm(`Legge "${item}" til i handlelisten?`)) return;
  await apiFetch("/api/shopping", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ item, quantity: 1, source_meal_id: mealId }),
  });
  await loadShopping();
  showStatus("Handlelisten er oppdatert.", "success");
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
  remainingEl.textContent = `${remaining.toLocaleString("no-NO")} kr gjenstår`;
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
    return `<div class="shop-item">
      <span class="item-name">${escapeHtml(label)}</span>
      ${ownerAvatar}
      <span style="color:#64748b;font-size:0.75rem">${date}</span>
      <span style="font-weight:700">${Number(expense.amount).toLocaleString("no-NO")} kr</span>
      <button class="btn-small btn-danger" style="padding:0.3rem 0.6rem;color:white" onclick="deleteExpense(${expense.id})">×</button>
    </div>`;
  }).join("");
}

async function deleteExpense(id) {
  if (!confirm("Slette utgift?")) return;
  await apiFetch("/api/budget", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ delete_expense: true, id }),
  });
  await loadBudget();
  await loadExpenseHistory();
}

async function loadMedicines() {
  const response = await apiFetch("/api/medicines");
  medicinesCache = await response.json();
  const container = document.getElementById("medicineList");
  if (!medicinesCache.length) {
    container.innerHTML = '<div class="empty-state empty-state-collapsible">Ingen registrert</div>';
    return;
  }

  container.innerHTML = medicinesCache.map((medicine) => {
    const owner = familyMembers.find((member) => member.id === medicine.owner_id);
    const ownerAvatar = owner ? getMemberAvatar(owner, "small") : "";
    const payload = encodePayload(medicine);
    return `<div class="med-item" onclick="viewMedicineFromJson('${payload}')">
      <div class="med-check ${medicine.taken ? "taken" : ""}" onclick="event.stopPropagation(); toggleMed(${medicine.id})">${medicine.taken ? "✓" : ""}</div>
      ${ownerAvatar}
      <span class="item-name">${escapeHtml(medicine.name)}</span>
      <span style="color:#64748b;font-size:0.7rem">${medicine.time ? medicine.time.slice(0, 5) : ""}</span>
    </div>`;
  }).join("");
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
    return `<div class="shop-item">
      <div class="shop-check ${item.done ? "checked" : ""}" onclick="toggleShop(${item.id})"></div>
      ${ownerAvatar}
      <span class="item-name shop-name ${item.done ? "done" : ""}" onclick="openShoppingModalFromJson('${payload}')">${escapeHtml(item.item)}</span>
      <span class="shop-qty">${item.quantity}</span>
    </div>`;
  }).join("");
  scheduleKioskColumnSizing();
}

function setText(id, value) {
  const element = document.getElementById(id);
  if (element) {
    element.textContent = value;
  }
}

function setCount(id, value) {
  setText(id, String(value));
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

  notesCard.classList.toggle("is-empty", notesList.children.length === 0);
  leftPanel.style.setProperty("--left-notes-max", `${notesMax}px`);
}
