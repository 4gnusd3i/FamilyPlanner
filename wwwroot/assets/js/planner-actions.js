async function submitEventForm(event) {
  event.preventDefault();
  const formData = new FormData();
  formData.append("title", document.getElementById("eventTitle").value);
  formData.append("event_date", document.getElementById("eventDate").value);
  formData.append("start_time", document.getElementById("eventStartTime").value);
  formData.append("end_time", document.getElementById("eventEndTime").value);
  formData.append("recurrence_type", document.getElementById("eventRecurrenceType").value);
  formData.append("recurrence_until", document.getElementById("eventRecurrenceUntil").value);
  formData.append("owner_id", document.getElementById("eventOwner").value);
  formData.append("note", document.getElementById("eventNote").value);
  const id = document.getElementById("eventId").value;
  if (id) formData.append("id", id);
  await apiFetch("/api/events", { method: "POST", body: formData });
  closeModal("eventModal");
  await Promise.all([loadEvents(), loadUpcoming()]);
}

async function deleteEvent() {
  const recurrenceType = document.getElementById("eventRecurrenceType").value;
  if (recurrenceType && !confirm(t("events.delete_series_confirm"))) return;
  await apiFetch("/api/events", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ delete: true, id: Number(document.getElementById("eventId").value) }),
  });
  closeModal("eventModal");
  await Promise.all([loadEvents(), loadUpcoming()]);
}

async function submitMealForm(event) {
  event.preventDefault();
  const formData = new FormData();
  formData.append("day_of_week", document.getElementById("mealDay").value);
  formData.append("meal_type", document.getElementById("mealType").value);
  formData.append("meal", document.getElementById("mealName").value);
  formData.append("owner_id", document.getElementById("mealOwner").value);
  formData.append("note", document.getElementById("mealNote").value);
  const id = document.getElementById("mealId").value;
  if (id) formData.append("id", id);
  await apiFetch("/api/meals", { method: "POST", body: formData });
  closeModal("mealModal");
  await loadMeals();
}

async function deleteMeal() {
  await apiFetch("/api/meals", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ delete: true, id: Number(document.getElementById("mealId").value) }),
  });
  closeModal("mealModal");
  await loadMeals();
}

async function submitExpenseForm(event) {
  event.preventDefault();
  await apiFetch("/api/budget", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      amount: Number(document.getElementById("expenseAmount").value || 0),
      category: document.getElementById("expenseCategory").value || t("budget.category_other"),
      expense_date: document.getElementById("expenseDate").value,
      owner_id: toNullableNumber(document.getElementById("expenseOwner").value),
      description: document.getElementById("expenseDesc").value,
    }),
  });
  closeModal("budgetModal");
  await loadBudget();
}

async function submitBudgetForm(event) {
  event.preventDefault();
  await apiFetch("/api/budget", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      set_budget: true,
      limit: Number(document.getElementById("budgetLimitInput").value || 0),
      income: Number(document.getElementById("budgetIncomeInput").value || 0),
      month: formatDate(new Date()).slice(0, 7),
    }),
  });
  closeModal("budgetModal");
  await loadBudget();
}

async function submitNoteForm(event) {
  event.preventDefault();
  const formData = new FormData();
  formData.append("title", document.getElementById("noteTitle").value);
  formData.append("owner_id", document.getElementById("noteOwner").value);
  formData.append("content", document.getElementById("noteContent").value);
  const id = document.getElementById("noteId").value;
  if (id) formData.append("id", id);
  await apiFetch("/api/notes", { method: "POST", body: formData });
  closeModal("noteModal");
  await loadNotes();
}

async function deleteNote() {
  await apiFetch("/api/notes", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ delete: true, id: Number(document.getElementById("noteId").value) }),
  });
  closeModal("noteModal");
  await loadNotes();
}

async function submitShoppingForm(event) {
  event.preventDefault();
  const formData = new FormData();
  formData.append("item", document.getElementById("shoppingItem").value);
  formData.append("quantity", document.getElementById("shoppingQty").value);
  formData.append("owner_id", document.getElementById("shoppingOwner").value);
  const id = document.getElementById("shoppingId").value;
  if (id) formData.append("id", id);
  await apiFetch("/api/shopping", { method: "POST", body: formData });
  closeModal("shoppingModal");
  await loadShopping();
}

async function toggleShop(id) {
  clearShoppingDeletion(id);
  await apiFetch("/api/shopping", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ toggle: true, id }),
  });
  await loadShopping();
}

async function submitMemberForm(event) {
  event.preventDefault();
  const formData = new FormData();
  formData.append("name", document.getElementById("memberName").value);
  formData.append("birthday", document.getElementById("memberBirthday").value);
  formData.append("bio", document.getElementById("memberBio").value);
  formData.append("color", selectedColor);
  const file = document.getElementById("memberAvatar").files?.[0];
  if (file) formData.append("avatar", file);
  const id = document.getElementById("memberId").value;
  if (id) formData.append("id", id);
  await apiFetch("/api/family", { method: "POST", body: formData });
  closeModal("memberModal");
  await loadAll();
}

async function deleteMember() {
  if (!confirm(t("family.delete_confirm"))) return;
  await apiFetch("/api/family", {
    method: "POST",
    body: new URLSearchParams({
      delete: "1",
      id: document.getElementById("memberId").value,
    }),
  });
  closeModal("memberModal");
  await loadAll();
}

function getMemberAvatar(member, size = "small") {
  const dimension = size === "small" ? 18 : 24;
  if (member.avatar_url) {
    return `<img src="${member.avatar_url}" class="member-avatar" style="width:${dimension}px;height:${dimension}px;" alt="${escapeHtml(member.name)}">`;
  }
  const emoji = memberEmojis[member.id % memberEmojis.length] || "👤";
  return `<span class="member-avatar" style="background:${member.color};width:${dimension}px;height:${dimension}px;font-size:${size === "small" ? "0.7rem" : "0.85rem"}">${emoji}</span>`;
}

function showStatus(message, kind = "info") {
  const banner = document.getElementById("statusBanner");
  banner.hidden = false;
  banner.className = `status-banner ${kind}`;
  banner.textContent = message;
  clearTimeout(statusTimer);
  statusTimer = setTimeout(() => {
    banner.hidden = true;
  }, 5000);
}

function handleError(error) {
  console.error(error);
  showStatus(error.message || t("errors.generic"), "error");
}

function calculateAge(birthday) {
  const today = new Date();
  const birth = parseDate(birthday);
  let age = today.getFullYear() - birth.getFullYear();
  const month = today.getMonth() - birth.getMonth();
  if (month < 0 || (month === 0 && today.getDate() < birth.getDate())) age -= 1;
  return age;
}

function formatBirthday(birthday) {
  return parseDate(birthday).toLocaleDateString(getCurrentLocale(), { day: "numeric", month: "short" });
}

function isBirthdayWithinDays(birthday, days) {
  if (!birthday) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const nextBirthday = parseDate(birthday);
  nextBirthday.setFullYear(today.getFullYear());
  if (nextBirthday < today) nextBirthday.setFullYear(today.getFullYear() + 1);
  const diff = Math.ceil((nextBirthday - today) / 86400000);
  return diff <= days;
}

function getIsoWeekNumber(date) {
  const temp = new Date(date);
  temp.setHours(0, 0, 0, 0);
  temp.setDate(temp.getDate() + 3 - ((temp.getDay() + 6) % 7));
  const firstThursday = new Date(temp.getFullYear(), 0, 4);
  return 1 + Math.round(((temp - firstThursday) / 86400000 - 3 + ((firstThursday.getDay() + 6) % 7)) / 7);
}

function addDays(date, amount) {
  const result = new Date(date);
  result.setDate(result.getDate() + amount);
  return result;
}

function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function parseDate(value) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}

function formatShortDisplayDate(date) {
  return `${date.getDate()}. ${date.toLocaleDateString(getCurrentLocale(), { month: "short" })}`;
}

function normalizeWeekday(date) {
  return (date.getDay() + 6) % 7;
}

function encodePayload(value) {
  return encodeURIComponent(JSON.stringify(value));
}

function decodePayload(payload) {
  return JSON.parse(decodeURIComponent(payload));
}

function escapeHtml(value) {
  if (!value) return "";
  const div = document.createElement("div");
  div.textContent = value;
  return div.innerHTML;
}

function toNullableNumber(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && value !== "" ? parsed : null;
}

function viewEvent(event) {
  openEntryView("event", event);
}

function viewEventFromJson(payload) {
  viewEvent(decodePayload(payload));
}

function viewMeal(meal) {
  openEntryView("meal", meal);
}

function viewMealFromJson(payload) {
  viewMeal(decodePayload(payload));
}

function viewNoteFromJson(payload) {
  viewNote(decodePayload(payload));
}

function viewShopping(item) {
  openEntryView("shopping", item);
}

function viewShoppingFromJson(payload) {
  viewShopping(decodePayload(payload));
}
