function updateOwnerSelects() {
  const options = [`<option value="">${escapeHtml(t("common.owner_placeholder"))}</option>`]
    .concat(familyMembers.map((member) => `<option value="${member.id}">${escapeHtml(member.name)}</option>`))
    .join("");
  ["eventOwner", "mealOwner", "noteOwner", "shoppingOwner", "expenseOwner"].forEach((id) => {
    const element = document.getElementById(id);
    if (element) element.innerHTML = options;
    refreshCustomSelect(id);
  });
}

function initCustomSelects() {
  CUSTOM_SELECT_IDS.forEach((id) => {
    const select = document.getElementById(id);
    if (!select || select.dataset.customSelectBound === "true") return;

    select.dataset.customSelectBound = "true";
    select.classList.add("custom-select-source");
    select.setAttribute("aria-hidden", "true");
    select.tabIndex = -1;

    const root = document.createElement("div");
    root.className = "custom-select";
    root.dataset.selectId = id;
    root.innerHTML = `
      <button type="button" class="custom-select-trigger" aria-haspopup="listbox" aria-expanded="false"></button>
      <div class="custom-select-menu" role="listbox" hidden></div>`;
    select.insertAdjacentElement("afterend", root);

    root.querySelector(".custom-select-trigger").addEventListener("click", () => toggleCustomSelect(id));
    root.querySelector(".custom-select-trigger").addEventListener("keydown", (event) => handleCustomSelectTriggerKey(id, event));
    select.addEventListener("change", () => syncCustomSelect(id));
    refreshCustomSelect(id);
  });

  if (document.documentElement.dataset.customSelectDocumentBound === "true") return;
  document.documentElement.dataset.customSelectDocumentBound = "true";
  document.addEventListener("click", (event) => {
    if (!event.target.closest(".custom-select")) closeCustomSelects();
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") closeCustomSelects();
  });
}

function refreshCustomSelect(id) {
  const select = document.getElementById(id);
  const root = getCustomSelectRoot(id);
  if (!select || !root) return;

  const menu = root.querySelector(".custom-select-menu");
  menu.innerHTML = Array.from(select.options).map((option) => `
    <button
      type="button"
      class="custom-select-option"
      role="option"
      data-value="${escapeHtml(option.value)}"
      aria-selected="${option.selected ? "true" : "false"}"
    >${escapeHtml(option.textContent || "")}</button>`).join("");

  menu.querySelectorAll(".custom-select-option").forEach((option) => {
    option.addEventListener("click", () => selectCustomOption(id, option.dataset.value ?? ""));
    option.addEventListener("keydown", (event) => handleCustomSelectOptionKey(id, event));
  });
  syncCustomSelect(id);
}

function syncCustomSelect(id) {
  const select = document.getElementById(id);
  const root = getCustomSelectRoot(id);
  if (!select || !root) return;

  const selected = select.options[select.selectedIndex];
  const trigger = root.querySelector(".custom-select-trigger");
  trigger.textContent = selected?.textContent || "";
  root.querySelectorAll(".custom-select-option").forEach((option) => {
    option.setAttribute("aria-selected", option.dataset.value === select.value ? "true" : "false");
  });
}

function syncAllCustomSelects() {
  CUSTOM_SELECT_IDS.forEach(syncCustomSelect);
}

function setSelectValue(id, value) {
  const select = document.getElementById(id);
  if (!select) return;
  select.value = value ?? "";
  select.dispatchEvent(new Event("change", { bubbles: true }));
  syncCustomSelect(id);
}

function toggleCustomSelect(id) {
  const root = getCustomSelectRoot(id);
  if (!root) return;
  const menu = root.querySelector(".custom-select-menu");
  const willOpen = menu.hidden;
  closeCustomSelects(id);
  if (!willOpen) return;

  menu.hidden = false;
  root.classList.add("is-open");
  positionCustomSelectMenu(root);
  root.querySelector(".custom-select-trigger").setAttribute("aria-expanded", "true");
  const selected = menu.querySelector('[aria-selected="true"]') || menu.querySelector(".custom-select-option");
  selected?.focus();
}

function closeCustomSelects(exceptId = null) {
  document.querySelectorAll(".custom-select").forEach((root) => {
    if (exceptId && root.dataset.selectId === exceptId) return;
    root.classList.remove("is-open");
    root.classList.remove("open-up");
    root.style.removeProperty("--custom-select-max-height");
    root.querySelector(".custom-select-menu").hidden = true;
    root.querySelector(".custom-select-trigger").setAttribute("aria-expanded", "false");
  });
}

function positionCustomSelectMenu(root) {
  const trigger = root.querySelector(".custom-select-trigger");
  if (!trigger) return;

  const rect = trigger.getBoundingClientRect();
  const viewportPadding = 16;
  const spaceBelow = window.innerHeight - rect.bottom - viewportPadding;
  const spaceAbove = rect.top - viewportPadding;
  const openUp = spaceBelow < 160 && spaceAbove > spaceBelow;
  const available = Math.max(120, Math.min(224, (openUp ? spaceAbove : spaceBelow) - 8));

  root.classList.toggle("open-up", openUp);
  root.style.setProperty("--custom-select-max-height", `${available}px`);
}

function selectCustomOption(id, value) {
  const select = document.getElementById(id);
  const root = getCustomSelectRoot(id);
  if (!select || !root) return;
  select.value = value;
  select.dispatchEvent(new Event("change", { bubbles: true }));
  syncCustomSelect(id);
  closeCustomSelects();
  root.querySelector(".custom-select-trigger").focus();
}

function handleCustomSelectTriggerKey(id, event) {
  if (event.key !== "ArrowDown" && event.key !== "Enter" && event.key !== " ") return;
  event.preventDefault();
  toggleCustomSelect(id);
}

function handleCustomSelectOptionKey(id, event) {
  const options = Array.from(getCustomSelectRoot(id)?.querySelectorAll(".custom-select-option") || []);
  const currentIndex = options.indexOf(event.currentTarget);
  if (event.key === "ArrowDown") {
    event.preventDefault();
    options[Math.min(currentIndex + 1, options.length - 1)]?.focus();
  } else if (event.key === "ArrowUp") {
    event.preventDefault();
    options[Math.max(currentIndex - 1, 0)]?.focus();
  } else if (event.key === "Home") {
    event.preventDefault();
    options[0]?.focus();
  } else if (event.key === "End") {
    event.preventDefault();
    options[options.length - 1]?.focus();
  } else if (event.key === "Enter" || event.key === " ") {
    event.preventDefault();
    selectCustomOption(id, event.currentTarget.dataset.value ?? "");
  }
}

function getCustomSelectRoot(id) {
  return document.querySelector(`.custom-select[data-select-id='${id}']`);
}

function initColorOptions() {
  const memberColors = document.getElementById("memberColorOptions");
  if (memberColors) {
    memberColors.innerHTML = colors.map((color) => `<div class="color-opt ${color === selectedColor ? "selected" : ""}" style="background:${color}" onclick="selMemberColor('${color}')"></div>`).join("");
  }
}

function selMemberColor(color) { selectedColor = color; initColorOptions(); }

function formatTimeForInput(date) {
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes}`;
}

function normalizeTimeValue(timeValue) {
  if (!timeValue || !timeValue.includes(":")) return "";
  const [hoursText, minutesText] = timeValue.split(":");
  const hours = Number.parseInt(hoursText, 10);
  const minutes = Number.parseInt(minutesText, 10);
  if (!Number.isFinite(hours) || !Number.isFinite(minutes)) return "";
  if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) return "";
  return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}`;
}

function addHoursToTimeValue(timeValue, hoursToAdd) {
  const normalized = normalizeTimeValue(timeValue);
  if (!normalized) return "";
  const [hoursText, minutesText] = normalized.split(":");
  const hours = Number.parseInt(hoursText, 10);
  const minutes = Number.parseInt(minutesText, 10);
  const totalMinutes = ((hours * 60) + minutes + (hoursToAdd * 60)) % (24 * 60);
  const normalizedMinutes = totalMinutes < 0 ? totalMinutes + (24 * 60) : totalMinutes;
  const nextHours = String(Math.floor(normalizedMinutes / 60)).padStart(2, "0");
  const nextMinutes = String(normalizedMinutes % 60).padStart(2, "0");
  return `${nextHours}:${nextMinutes}`;
}

function setTimeInputValue(input, value) {
  const normalized = normalizeTimeValue(value);
  input.value = normalized;
  input.defaultValue = normalized;
  if (normalized) {
    input.setAttribute("value", normalized);
  } else {
    input.removeAttribute("value");
  }
}

function applyEventTimeDefaults(startInput, endInput) {
  const normalizedStart = normalizeTimeValue(startInput.value);
  if (normalizedStart) {
    setTimeInputValue(startInput, normalizedStart);
  } else {
    setTimeInputValue(startInput, formatTimeForInput(new Date()));
  }

  const normalizedEnd = normalizeTimeValue(endInput.value);
  if (normalizedEnd) {
    setTimeInputValue(endInput, normalizedEnd);
  } else {
    setTimeInputValue(endInput, addHoursToTimeValue(startInput.value, 1));
  }
}

function bindTimeInputDefaults() {
  const startInput = document.getElementById("eventStartTime");
  const endInput = document.getElementById("eventEndTime");
  if (!startInput || !endInput || startInput.dataset.autosyncBound === "true") return;

  startInput.dataset.autosyncBound = "true";

  const ensureStartDefault = () => {
    const normalizedStart = normalizeTimeValue(startInput.value);
    if (!normalizedStart) {
      setTimeInputValue(startInput, formatTimeForInput(new Date()));
    }
  };

  const ensureEndDefault = () => {
    ensureStartDefault();
    const normalizedStart = normalizeTimeValue(startInput.value);
    const normalizedEnd = normalizeTimeValue(endInput.value);
    if (!normalizedEnd && normalizedStart) {
      setTimeInputValue(endInput, addHoursToTimeValue(normalizedStart, 1));
    }
  };

  const syncEndFromStart = () => {
    const normalizedStart = normalizeTimeValue(startInput.value);
    setTimeInputValue(startInput, normalizedStart);
    setTimeInputValue(endInput, normalizedStart ? addHoursToTimeValue(normalizedStart, 1) : "");
  };

  startInput.addEventListener("pointerdown", ensureStartDefault);
  startInput.addEventListener("mousedown", ensureStartDefault);
  startInput.addEventListener("focus", ensureStartDefault);
  startInput.addEventListener("click", ensureStartDefault);
  endInput.addEventListener("pointerdown", ensureEndDefault);
  endInput.addEventListener("mousedown", ensureEndDefault);
  endInput.addEventListener("focus", ensureEndDefault);
  endInput.addEventListener("click", ensureEndDefault);
  startInput.addEventListener("input", syncEndFromStart);
  startInput.addEventListener("change", syncEndFromStart);
}

function bindRecurrenceControls() {
  const recurrenceType = document.getElementById("eventRecurrenceType");
  const recurrenceUntil = document.getElementById("eventRecurrenceUntil");
  if (!recurrenceType || !recurrenceUntil || recurrenceType.dataset.bound === "true") return;

  const syncRecurrenceState = () => {
    const hasRecurrence = recurrenceType.value !== "";
    recurrenceUntil.disabled = !hasRecurrence;
    recurrenceUntil.hidden = !hasRecurrence;
    if (!hasRecurrence) {
      recurrenceUntil.value = "";
    }
  };

  recurrenceType.dataset.bound = "true";
  recurrenceType.addEventListener("change", syncRecurrenceState);
  syncRecurrenceState();
}

function openEventModal(event = null, date = null, ownerId = null) {
  bindTimeInputDefaults();
  bindRecurrenceControls();
  const startInput = document.getElementById("eventStartTime");
  const endInput = document.getElementById("eventEndTime");
  const recurrenceType = document.getElementById("eventRecurrenceType");
  const recurrenceUntil = document.getElementById("eventRecurrenceUntil");
  if (event) {
    document.getElementById("eventModalTitle").textContent = t("events.edit_title");
    document.getElementById("deleteEventBtn").style.display = "block";
    document.getElementById("eventId").value = event.id || "";
    document.getElementById("eventTitle").value = event.title || "";
    document.getElementById("eventDate").value = event.series_start_date || event.event_date || "";
    setTimeInputValue(startInput, event.start_time || "");
    setTimeInputValue(endInput, event.end_time || "");
    applyEventTimeDefaults(startInput, endInput);
    setSelectValue("eventRecurrenceType", event.recurrence_type || "");
    recurrenceUntil.value = event.recurrence_until || "";
    setSelectValue("eventOwner", event.owner_id || "");
    document.getElementById("eventNote").value = event.note || "";
  } else {
    document.getElementById("eventModalTitle").textContent = t("events.new_title");
    document.getElementById("deleteEventBtn").style.display = "none";
    document.getElementById("eventForm").reset();
    document.getElementById("eventDate").value = date || formatDate(new Date());
    setTimeInputValue(startInput, "");
    setTimeInputValue(endInput, "");
    applyEventTimeDefaults(startInput, endInput);
    setSelectValue("eventRecurrenceType", "");
    recurrenceUntil.value = "";
    setSelectValue("eventOwner", ownerId || "");
  }
  recurrenceUntil.disabled = recurrenceType.value === "";
  recurrenceUntil.hidden = recurrenceType.value === "";
  syncAllCustomSelects();
  openModal("eventModal");
  requestAnimationFrame(() => applyEventTimeDefaults(startInput, endInput));
}

function openMealModal(day = null, mealId = null) {
  document.getElementById("mealForm").reset();
  document.getElementById("deleteMealBtn").style.display = "none";
  setSelectValue("mealDay", day ?? normalizeWeekday(new Date()));
  setSelectValue("mealType", "dinner");
  if (mealId) {
    const meal = mealsCache.find((item) => item.id === mealId);
    if (meal) {
      document.getElementById("mealId").value = meal.id;
      setSelectValue("mealDay", meal.day_of_week);
      setSelectValue("mealType", meal.meal_type || "dinner");
      document.getElementById("mealName").value = meal.meal || "";
      setSelectValue("mealOwner", meal.owner_id || "");
      document.getElementById("mealNote").value = meal.note || "";
      document.getElementById("deleteMealBtn").style.display = "block";
    }
  }
  syncAllCustomSelects();
  openModal("mealModal");
}

function openMealModalForType(day, mealType) {
  openMealModal(day, null);
  setSelectValue("mealType", mealType);
}

function openBudgetModal() {
  document.querySelectorAll("#budgetModal .tabs button").forEach((button) => {
    button.classList.toggle("active", button.dataset.tab === "expense");
  });
  showBudgetTab("expense");
  document.getElementById("expenseForm").reset();
  document.getElementById("expenseDate").value = formatDate(new Date());
  setSelectValue("expenseOwner", "");
  syncAllCustomSelects();
  openModal("budgetModal");
}

function openNoteModal(note = null) {
  document.getElementById("noteForm").reset();
  document.getElementById("deleteNoteBtn").style.display = "none";
  if (note) {
    document.getElementById("noteId").value = note.id || "";
    document.getElementById("noteTitle").value = note.title || "";
    setSelectValue("noteOwner", note.owner_id || "");
    document.getElementById("noteContent").value = note.content || "";
    document.getElementById("deleteNoteBtn").style.display = "block";
  }
  syncAllCustomSelects();
  openModal("noteModal");
}

function viewNote(note) {
  openEntryView("note", note);
}

function openShoppingModal(item = null) {
  document.getElementById("shoppingForm").reset();
  document.getElementById("shoppingId").value = "";
  document.getElementById("shoppingQty").value = "1";
  if (item) {
    document.getElementById("shoppingId").value = item.id || "";
    document.getElementById("shoppingItem").value = item.item || "";
    document.getElementById("shoppingQty").value = item.quantity || 1;
    setSelectValue("shoppingOwner", item.owner_id || "");
  }
  syncAllCustomSelects();
  openModal("shoppingModal");
}

function openMemberModal(member = null) {
  document.getElementById("memberForm").reset();
  document.getElementById("avatarPreview").innerHTML = "👤";
  if (typeof syncLocalizedFileInput === "function") {
    syncLocalizedFileInput("memberAvatar");
  }
  document.getElementById("deleteMemberBtn").style.display = "none";
  if (member) {
    document.getElementById("memberId").value = member.id || "";
    document.getElementById("memberName").value = member.name || "";
    document.getElementById("memberBirthday").value = member.birthday || "";
    document.getElementById("memberBio").value = member.bio || "";
    selectedColor = member.color || "#3b82f6";
    document.getElementById("deleteMemberBtn").style.display = "block";
    if (member.avatar_url) {
      document.getElementById("avatarPreview").innerHTML = `<img src="${member.avatar_url}" alt="${escapeHtml(member.name)}">`;
    }
  } else {
    selectedColor = colors[Math.floor(Math.random() * colors.length)];
  }
  initColorOptions();
  openModal("memberModal");
}

function showProfile(memberId) {
  const member = familyMembers.find((item) => item.id === memberId);
  if (!member) return;
  currentProfileMemberId = memberId;
  document.getElementById("profileAvatar").innerHTML = member.avatar_url
    ? `<img src="${member.avatar_url}" alt="${escapeHtml(member.name)}">`
    : memberEmojis[member.id % memberEmojis.length];
  document.getElementById("profileName").textContent = member.name;
  document.getElementById("profileAge").textContent = member.birthday ? t("profile.age_years", { age: String(calculateAge(member.birthday)) }) : "";
  document.getElementById("profileBirthday").innerHTML = member.birthday
    ? `<div class="profile-section-label">${escapeHtml(t("profile.birthday_label"))}</div><div class="profile-section-content">${formatBirthday(member.birthday)}${isBirthdayWithinDays(member.birthday, 14) ? " \ud83c\udf89" : ""}</div>`
    : "";
  document.getElementById("profileBio").innerHTML = member.bio
    ? `<div class="profile-section-label">${escapeHtml(t("profile.about_label"))}</div><div class="profile-section-content">${escapeHtml(member.bio)}</div>`
    : "";
  openModal("profileModal");
}

function editProfileFromModal() {
  const member = familyMembers.find((item) => item.id === currentProfileMemberId);
  if (member) {
    closeModal("profileModal");
    openMemberModal(member);
  }
}

function openModal(id) {
  syncAllCustomSelects();
  document.getElementById(id).classList.add("active");
}

function closeModal(id) {
  closeCustomSelects();
  document.getElementById(id).classList.remove("active");
}

function showBudgetTab(tab, event) {
  if (event?.target) {
    document.querySelectorAll("#budgetModal .tabs button").forEach((button) => button.classList.remove("active"));
    event.target.classList.add("active");
  }
  document.getElementById("expenseForm").style.display = tab === "expense" ? "block" : "none";
  document.getElementById("budgetForm").style.display = tab === "limit" ? "block" : "none";
  document.getElementById("expenseHistory").style.display = tab === "history" ? "block" : "none";
  if (tab === "history") loadExpenseHistory().catch(handleError);
}

function openEntryView(type, item) {
  if (!item) return;
  currentViewEntry = { type, item };
  const title = getEntryViewTitle(type, item);
  document.getElementById("entryViewTitle").textContent = title;
  document.getElementById("entryViewContent").innerHTML = renderEntryViewFields(type, item);
  openModal("entryViewModal");
}

function getEntryViewTitle(type, item) {
  if (type === "event") return item.title || t("events.default_title");
  if (type === "meal") return item.meal || t("entry.meal_default_title");
  if (type === "shopping") return item.item || t("entry.shopping_default_title");
  return item.title || t("entry.note_default_title");
}

function renderEntryViewFields(type, item) {
  if (type === "event") {
    return renderViewFields([
      { label: t("view.field_owner"), html: renderOwnerInline(item.owner_id) },
      { label: t("view.field_date"), value: formatShortDateText(item.event_date) },
      { label: t("view.field_start"), value: item.start_time?.slice(0, 5) },
      { label: t("view.field_end"), value: item.end_time?.slice(0, 5) },
      { label: t("view.field_recurrence"), value: formatRecurrenceText(item) },
      { label: t("view.field_note"), value: item.note, wide: true },
    ]);
  }

  if (type === "meal") {
    return renderViewFields([
      { label: t("view.field_day"), value: weekdayNames[item.day_of_week] },
      { label: t("view.field_type"), value: mealTypes.find((entry) => entry.key === item.meal_type)?.name },
      { label: t("view.field_owner"), html: renderOwnerInline(item.owner_id) },
      { label: t("view.field_note"), value: item.note, wide: true },
    ]);
  }

  if (type === "shopping") {
    return renderViewFields([
      { label: t("view.field_quantity"), value: String(item.quantity || 1) },
      { label: t("view.field_owner"), html: renderOwnerInline(item.owner_id) },
      { label: t("view.field_status"), value: item.done ? t("shopping.status_purchased") : t("shopping.status_not_purchased") },
    ]);
  }

  return renderViewFields([
    { label: t("view.field_owner"), html: renderOwnerInline(item.owner_id) },
    { label: t("view.field_content"), value: item.content, wide: true },
  ]);
}

function renderViewFields(fields) {
  return fields
    .filter((field) => field.html || !isBlank(field.value))
    .map((field) => {
      const body = field.html || `<div class="view-value">${escapeHtml(field.value)}</div>`;
      return `<div class="view-field ${field.wide ? "view-content-text" : ""}">
        <div class="view-label">${escapeHtml(field.label)}</div>
        ${body}
      </div>`;
    })
    .join("");
}

function renderOwnerInline(ownerId) {
  const owner = familyMembers.find((member) => member.id === ownerId);
  if (!owner) return `<div class="view-value">${escapeHtml(t("view.none"))}</div>`;
  return `<div class="view-owner">${getMemberAvatar(owner, "small")}<span>${escapeHtml(owner.name)}</span></div>`;
}

function formatShortDateText(value) {
  return value ? parseDate(value).toLocaleDateString(getCurrentLocale(), { day: "numeric", month: "short", year: "numeric" }) : "";
}

function formatRecurrenceText(item) {
  if (item.recurrence_type === "daily") return item.recurrence_until ? t("events.recurrence_daily_until", { date: formatShortDateText(item.recurrence_until) }) : t("events.recurrence_daily");
  if (item.recurrence_type === "weekly") return item.recurrence_until ? t("events.recurrence_weekly_until", { date: formatShortDateText(item.recurrence_until) }) : t("events.recurrence_weekly");
  return "";
}

function isBlank(value) {
  return value === null || value === undefined || String(value).trim() === "";
}

function editEntryFromView() {
  if (!currentViewEntry) return;
  const { type, item } = currentViewEntry;
  closeModal("entryViewModal");
  if (type === "event") openEventModal(item);
  else if (type === "meal") openMealModal(item.day_of_week, item.id);
  else if (type === "shopping") openShoppingModal(item);
  else openNoteModal(item);
}

async function deleteEntryFromView() {
  if (!currentViewEntry) return;
  const { type, item } = currentViewEntry;

  if (type === "event") {
    if (item.recurrence_type && !confirm(t("events.delete_series_confirm"))) return;
    await apiFetch("/api/events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ delete: true, id: item.id }),
    });
    closeModal("entryViewModal");
    await Promise.all([loadEvents(), loadUpcoming()]);
    return;
  }

  if (type === "meal") {
    await apiFetch("/api/meals", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ delete: true, id: item.id }),
    });
    closeModal("entryViewModal");
    await loadMeals();
    return;
  }

  if (type === "shopping") {
    clearShoppingDeletion(item.id);
    await apiFetch("/api/shopping", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ delete: true, id: item.id }),
    });
    closeModal("entryViewModal");
    await loadShopping();
    return;
  }

  await apiFetch("/api/notes", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ delete: true, id: item.id }),
  });
  closeModal("entryViewModal");
  await loadNotes();
}
