function updateOwnerSelects() {
  const options = ['<option value="">Ansvarlig...</option>']
    .concat(familyMembers.map((member) => `<option value="${member.id}">${escapeHtml(member.name)}</option>`))
    .join("");
  ["eventOwner", "mealOwner", "noteOwner", "shoppingOwner", "expenseOwner"].forEach((id) => {
    const element = document.getElementById(id);
    if (element) element.innerHTML = options;
  });
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
    document.getElementById("eventModalTitle").textContent = "Rediger avtale";
    document.getElementById("deleteEventBtn").style.display = "block";
    document.getElementById("eventId").value = event.id || "";
    document.getElementById("eventTitle").value = event.title || "";
    document.getElementById("eventDate").value = event.series_start_date || event.event_date || "";
    setTimeInputValue(startInput, event.start_time || "");
    setTimeInputValue(endInput, event.end_time || "");
    applyEventTimeDefaults(startInput, endInput);
    recurrenceType.value = event.recurrence_type || "";
    recurrenceUntil.value = event.recurrence_until || "";
    document.getElementById("eventOwner").value = event.owner_id || "";
    document.getElementById("eventNote").value = event.note || "";
  } else {
    document.getElementById("eventModalTitle").textContent = "Ny avtale";
    document.getElementById("deleteEventBtn").style.display = "none";
    document.getElementById("eventForm").reset();
    document.getElementById("eventDate").value = date || formatDate(new Date());
    setTimeInputValue(startInput, "");
    setTimeInputValue(endInput, "");
    applyEventTimeDefaults(startInput, endInput);
    recurrenceType.value = "";
    recurrenceUntil.value = "";
    document.getElementById("eventOwner").value = ownerId || "";
  }
  recurrenceUntil.disabled = recurrenceType.value === "";
  recurrenceUntil.hidden = recurrenceType.value === "";
  openModal("eventModal");
  requestAnimationFrame(() => applyEventTimeDefaults(startInput, endInput));
}

function openMealModal(day = null, mealId = null) {
  document.getElementById("mealForm").reset();
  document.getElementById("deleteMealBtn").style.display = "none";
  document.getElementById("mealDay").value = day ?? normalizeWeekday(new Date());
  document.getElementById("mealType").value = "dinner";
  if (mealId) {
    const meal = mealsCache.find((item) => item.id === mealId);
    if (meal) {
      document.getElementById("mealId").value = meal.id;
      document.getElementById("mealDay").value = meal.day_of_week;
      document.getElementById("mealType").value = meal.meal_type || "dinner";
      document.getElementById("mealName").value = meal.meal || "";
      document.getElementById("mealOwner").value = meal.owner_id || "";
      document.getElementById("mealNote").value = meal.note || "";
      document.getElementById("deleteMealBtn").style.display = "block";
    }
  }
  openModal("mealModal");
}

function openMealModalForType(day, mealType) {
  openMealModal(day, null);
  document.getElementById("mealType").value = mealType;
}

function openBudgetModal() {
  document.querySelectorAll("#budgetModal .tabs button").forEach((button) => {
    button.classList.toggle("active", button.dataset.tab === "expense");
  });
  showBudgetTab("expense");
  document.getElementById("expenseDate").value = formatDate(new Date());
  openModal("budgetModal");
}

function openNoteModal(note = null) {
  document.getElementById("noteForm").reset();
  document.getElementById("deleteNoteBtn").style.display = "none";
  if (note) {
    document.getElementById("noteId").value = note.id || "";
    document.getElementById("noteTitle").value = note.title || "";
    document.getElementById("noteOwner").value = note.owner_id || "";
    document.getElementById("noteContent").value = note.content || "";
    document.getElementById("deleteNoteBtn").style.display = "block";
  }
  openModal("noteModal");
}

function viewNote(note) {
  if (!note) return;
  currentViewNote = note;
  const owner = familyMembers.find((member) => member.id === note.owner_id);
  const ownerAvatar = owner ? getMemberAvatar(owner, "small") : "";
  const ownerName = owner ? owner.name : "Ingen";
  document.getElementById("noteViewTitle").textContent = note.title;
  document.getElementById("noteViewContent").innerHTML = `
    <div class="view-field"><div class="view-label">Ansvarlig</div>${ownerAvatar} ${escapeHtml(ownerName)}</div>
    <div class="view-field view-content-text"><div class="view-label">Innhold</div><div style="white-space:pre-wrap">${note.content ? escapeHtml(note.content) : "-"}</div></div>`;
  openModal("noteViewModal");
}

function editNoteFromView() {
  if (!currentViewNote) return;
  closeModal("noteViewModal");
  openNoteModal(currentViewNote);
}

async function deleteNoteFromView() {
  if (!currentViewNote) return;
  await apiFetch("/api/notes", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ delete: true, id: currentViewNote.id }),
  });
  closeModal("noteViewModal");
  await loadNotes();
}

function openShoppingModal(item = null) {
  document.getElementById("shoppingForm").reset();
  document.getElementById("shoppingId").value = "";
  document.getElementById("shoppingQty").value = "1";
  if (item) {
    document.getElementById("shoppingId").value = item.id || "";
    document.getElementById("shoppingItem").value = item.item || "";
    document.getElementById("shoppingQty").value = item.quantity || 1;
    document.getElementById("shoppingOwner").value = item.owner_id || "";
  }
  openModal("shoppingModal");
}

function openMemberModal(member = null) {
  document.getElementById("memberForm").reset();
  document.getElementById("avatarPreview").innerHTML = "👤";
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
  document.getElementById("profileAge").textContent = member.birthday ? `${calculateAge(member.birthday)} \u00e5r` : "";
  document.getElementById("profileBirthday").innerHTML = member.birthday
    ? `<div class="profile-section-label">F\u00f8dselsdag</div><div class="profile-section-content">${formatBirthday(member.birthday)}${isBirthdayWithinDays(member.birthday, 14) ? " \ud83c\udf89" : ""}</div>`
    : "";
  document.getElementById("profileBio").innerHTML = member.bio
    ? `<div class="profile-section-label">Om meg</div><div class="profile-section-content">${escapeHtml(member.bio)}</div>`
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
  document.getElementById(id).classList.add("active");
}

function closeModal(id) {
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
