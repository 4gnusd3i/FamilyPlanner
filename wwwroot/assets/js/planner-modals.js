function updateOwnerSelects() {
  const options = ['<option value="">Ansvarlig...</option>']
    .concat(familyMembers.map((member) => `<option value="${member.id}">${escapeHtml(member.name)}</option>`))
    .join("");
  ["eventOwner", "mealOwner", "medicineOwner", "noteOwner", "shoppingOwner", "expenseOwner"].forEach((id) => {
    const element = document.getElementById(id);
    if (element) element.innerHTML = options;
  });
}

function initColorOptions() {
  const eventColors = document.getElementById("colorOptions");
  const memberColors = document.getElementById("memberColorOptions");
  if (eventColors) {
    eventColors.innerHTML = colors.map((color) => `<div class="color-opt ${color === selectedColor ? "selected" : ""}" style="background:${color}" onclick="selColor('${color}')"></div>`).join("");
  }
  if (memberColors) {
    memberColors.innerHTML = colors.map((color) => `<div class="color-opt ${color === selectedColor ? "selected" : ""}" style="background:${color}" onclick="selMemberColor('${color}')"></div>`).join("");
  }
}

function selColor(color) { selectedColor = color; initColorOptions(); }
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

function applyEventTimeDefaults(startInput, endInput) {
  const normalizedStart = normalizeTimeValue(startInput.value);
  if (normalizedStart) {
    startInput.value = normalizedStart;
  } else {
    startInput.value = formatTimeForInput(new Date());
  }

  const normalizedEnd = normalizeTimeValue(endInput.value);
  if (normalizedEnd) {
    endInput.value = normalizedEnd;
  } else {
    endInput.value = addHoursToTimeValue(startInput.value, 1);
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
      startInput.value = formatTimeForInput(new Date());
    }
  };

  const ensureEndDefault = () => {
    ensureStartDefault();
    const normalizedStart = normalizeTimeValue(startInput.value);
    const normalizedEnd = normalizeTimeValue(endInput.value);
    if (!normalizedEnd && normalizedStart) {
      endInput.value = addHoursToTimeValue(normalizedStart, 1);
    }
  };

  const syncEndFromStart = () => {
    const normalizedStart = normalizeTimeValue(startInput.value);
    startInput.value = normalizedStart;
    endInput.value = normalizedStart ? addHoursToTimeValue(normalizedStart, 1) : "";
  };

  startInput.addEventListener("focus", ensureStartDefault);
  startInput.addEventListener("click", ensureStartDefault);
  endInput.addEventListener("focus", ensureEndDefault);
  endInput.addEventListener("click", ensureEndDefault);
  startInput.addEventListener("input", syncEndFromStart);
  startInput.addEventListener("change", syncEndFromStart);
}

function applyMedicineTimeDefault(timeInput) {
  const normalized = normalizeTimeValue(timeInput.value);
  if (normalized) {
    timeInput.value = normalized;
  } else {
    timeInput.value = formatTimeForInput(new Date());
  }
}

function openEventModal(event = null, date = null) {
  bindTimeInputDefaults();
  const startInput = document.getElementById("eventStartTime");
  const endInput = document.getElementById("eventEndTime");
  if (event) {
    document.getElementById("eventModalTitle").textContent = "Rediger avtale";
    document.getElementById("deleteEventBtn").style.display = "block";
    document.getElementById("eventId").value = event.id || "";
    document.getElementById("eventTitle").value = event.title || "";
    document.getElementById("eventDate").value = event.event_date || "";
    startInput.value = event.start_time || "";
    endInput.value = event.end_time || "";
    applyEventTimeDefaults(startInput, endInput);
    document.getElementById("eventOwner").value = event.owner_id || "";
    document.getElementById("eventNote").value = event.note || "";
    selectedColor = event.color || "#3b82f6";
  } else {
    document.getElementById("eventModalTitle").textContent = "Ny avtale";
    document.getElementById("deleteEventBtn").style.display = "none";
    document.getElementById("eventForm").reset();
    document.getElementById("eventDate").value = date || formatDate(new Date());
    startInput.value = "";
    endInput.value = "";
    applyEventTimeDefaults(startInput, endInput);
    selectedColor = "#3b82f6";
  }
  initColorOptions();
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

function openMedicineModal(medicine = null) {
  document.getElementById("medicineForm").reset();
  document.getElementById("deleteMedicineBtn").style.display = "none";
  const medicineTimeInput = document.getElementById("medicineTime");
  if (medicine) {
    document.getElementById("medicineId").value = medicine.id || "";
    document.getElementById("medicineName").value = medicine.name || "";
    medicineTimeInput.value = medicine.time || "";
    applyMedicineTimeDefault(medicineTimeInput);
    document.getElementById("medicineOwner").value = medicine.owner_id || "";
    document.getElementById("medicineNote").value = medicine.note || "";
    document.getElementById("deleteMedicineBtn").style.display = "block";
  } else {
    medicineTimeInput.value = "";
    applyMedicineTimeDefault(medicineTimeInput);
  }
  openModal("medicineModal");
  requestAnimationFrame(() => applyMedicineTimeDefault(medicineTimeInput));
}

function viewMedicine(medicine) {
  if (!medicine) return;
  currentViewMedicine = medicine;
  const owner = familyMembers.find((member) => member.id === medicine.owner_id);
  const ownerAvatar = owner ? getMemberAvatar(owner, "small") : "";
  const ownerName = owner ? owner.name : "Ingen";
  document.getElementById("medicineViewTitle").textContent = medicine.name;
  document.getElementById("medicineViewContent").innerHTML = `
    <div class="view-field"><div class="view-label">Ansvarlig</div>${ownerAvatar} ${escapeHtml(ownerName)}</div>
    <div class="view-field"><div class="view-label">Klokkeslett</div>${medicine.time ? medicine.time.slice(0, 5) : "-"}</div>
    ${medicine.note ? `<div class="view-field"><div class="view-label">Notat</div>${escapeHtml(medicine.note)}</div>` : ""}
    <div class="view-field"><div class="view-label">Status</div>${medicine.taken ? "✓ Tatt" : "Ikke tatt"}</div>`;
  openModal("medicineViewModal");
}

function editMedicineFromView() {
  if (!currentViewMedicine) return;
  closeModal("medicineViewModal");
  openMedicineModal(currentViewMedicine);
}

async function deleteMedicineFromView() {
  if (!currentViewMedicine || !confirm("Slette medisin?")) return;
  await apiFetch("/api/medicines", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ delete: true, id: currentViewMedicine.id }),
  });
  closeModal("medicineViewModal");
  await loadMedicines();
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
  if (!currentViewNote || !confirm("Slette notat?")) return;
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
  document.getElementById("deleteShoppingBtn").style.display = "none";
  if (item) {
    document.getElementById("shoppingId").value = item.id || "";
    document.getElementById("shoppingItem").value = item.item || "";
    document.getElementById("shoppingQty").value = item.quantity || 1;
    document.getElementById("shoppingOwner").value = item.owner_id || "";
    document.getElementById("deleteShoppingBtn").style.display = "block";
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
  document.getElementById("profileAge").textContent = member.birthday ? `${calculateAge(member.birthday)} år` : "";
  document.getElementById("profileBirthday").innerHTML = member.birthday
    ? `<div class="profile-section-label">Fødselsdag</div><div class="profile-section-content">${formatBirthday(member.birthday)}${isBirthdayWithinDays(member.birthday, 14) ? " 🎉" : ""}</div>`
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

function openAssignModal(day, memberId) {
  document.getElementById("assignDay").value = day;
  document.getElementById("assignMemberId").value = memberId;
  document.getElementById("assignNote").value = "";
  const existing = (familyAssignments[day] || []).find((assignment) => assignment.family_member_id === memberId);
  selectedActivityType = existing?.activity_type || "medicine";
  document.getElementById("assignNote").value = existing?.note || "";
  document.getElementById("removeAssignBtn").style.display = existing ? "block" : "none";
  document.querySelectorAll(".act-btn").forEach((button) => {
    button.classList.toggle("active", button.dataset.type === selectedActivityType);
  });
  openModal("assignModal");
}

function selectActivityType(type) {
  selectedActivityType = type;
  document.querySelectorAll(".act-btn").forEach((button) => {
    button.classList.toggle("active", button.dataset.type === type);
  });
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
