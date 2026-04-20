const colors = ["#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899", "#06b6d4", "#84cc16"];
const memberEmojis = ["\ud83d\udc68", "\ud83d\udc69", "\ud83d\udc66", "\ud83d\udc67", "\ud83e\uddd2", "\ud83d\udc74", "\ud83d\udc75", "\ud83e\uddd1"];
const weekdayNames = ["Mandag", "Tirsdag", "Onsdag", "Torsdag", "Fredag", "L\u00f8rdag", "S\u00f8ndag"];
const weekdayShort = ["Man", "Tir", "Ons", "Tor", "Fre", "L\u00f8r", "S\u00f8n"];
const mealTypes = [
  { key: "breakfast", label: "\ud83c\udf04", name: "Frokost" },
  { key: "lunch", label: "\u2600\ufe0f", name: "Lunsj" },
  { key: "dinner", label: "\ud83c\udf7d\ufe0f", name: "Middag" },
];
let selectedColor = "#3b82f6";
let selectedActivityType = "activity";
let currentWeekStart = "";
let currentWeekEnd = "";
let familyMembers = [];
let familyAssignments = {};
let eventsCache = [];
let mealsCache = [];
let notesCache = [];
let shoppingCache = [];
let currentViewNote = null;
let currentProfileMemberId = null;
let statusTimer = null;
const SHOPPING_DELETE_DELAY_MS = 15000;
const shoppingDeleteTimers = new Map();

document.addEventListener("DOMContentLoaded", async () => {
  initWeek();
  renderWeekShell();
  renderMealDayOptions();
  initColorOptions();
  bindForms();
  bindAvatarPreview();
  removeLegacyAssignmentOptions();
  await loadAll();
  if (typeof scheduleKioskColumnSizing === "function") {
    window.addEventListener("resize", scheduleKioskColumnSizing, { passive: true });
    scheduleKioskColumnSizing();
  }
});

async function apiFetch(url, options = {}) {
  const response = await fetch(url, { credentials: "same-origin", ...options });
  if (response.status === 409) {
    location.href = "/setup";
    throw new Error("Oppsett mangler.");
  }
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || "Foresp\u00f8rselen feilet.");
  }
  return response;
}

function initWeek() {
  const today = new Date();
  const monday = new Date(today);
  const weekday = today.getDay() || 7;
  monday.setDate(today.getDate() - weekday + 1);
  monday.setHours(0, 0, 0, 0);
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);
  currentWeekStart = formatDate(monday);
  currentWeekEnd = formatDate(sunday);
}

function renderWeekShell() {
  const grid = document.getElementById("weekDaysGrid");
  const weekStart = parseDate(currentWeekStart);
  const todayText = formatDate(new Date());
  const weekNumber = String(getIsoWeekNumber(weekStart));
  const weekRangeText = `${formatShortDisplayDate(weekStart)} - ${formatShortDisplayDate(addDays(weekStart, 6))}`;

  document.getElementById("weekNumber").textContent = weekNumber;
  document.getElementById("weekRange").textContent = weekRangeText;

  grid.innerHTML = Array.from({ length: 7 }, (_, index) => {
    const date = addDays(weekStart, index);
    const dateText = formatDate(date);
    const isToday = dateText === todayText;
    const dayLabel = `${weekdayNames[index]} ${date.getDate()}.${isToday ? " i dag" : ""}`;

    return `<div class="day-box ${isToday ? "today" : ""}" data-day="${index}" aria-label="${dayLabel}">
      <div class="day-header">
        <span class="day-name">${weekdayNames[index]}</span>
        <span class="day-num">${date.getDate()}.</span>
      </div>
      <div class="day-body">
        <div class="day-content" id="events-${dateText}"></div>
        <div class="day-tasks" id="tasks-${index}"></div>
      </div>
    </div>`;
  }).join("");
}

function shiftWeek(direction) {
  const start = addDays(parseDate(currentWeekStart), direction * 7);
  const end = addDays(start, 6);
  currentWeekStart = formatDate(start);
  currentWeekEnd = formatDate(end);
  renderWeekShell();
  initDragDrop();
  renderAssignments();
  Promise.all([loadEvents(), loadUpcoming()]).catch(handleError);
}

function goToCurrentWeek() {
  initWeek();
  renderWeekShell();
  initDragDrop();
  renderAssignments();
  Promise.all([loadEvents(), loadUpcoming()]).catch(handleError);
}

function renderMealDayOptions() {
  document.getElementById("mealDay").innerHTML = weekdayNames
    .map((day, index) => `<option value="${index}">${day}</option>`)
    .join("");
}

function bindForms() {
  document.getElementById("eventForm").addEventListener("submit", submitEventForm);
  document.getElementById("mealForm").addEventListener("submit", submitMealForm);
  document.getElementById("expenseForm").addEventListener("submit", submitExpenseForm);
  document.getElementById("budgetForm").addEventListener("submit", submitBudgetForm);
  document.getElementById("noteForm").addEventListener("submit", submitNoteForm);
  document.getElementById("shoppingForm").addEventListener("submit", submitShoppingForm);
  document.getElementById("memberForm").addEventListener("submit", submitMemberForm);
  document.getElementById("assignForm").addEventListener("submit", submitAssignmentForm);
}

function bindAvatarPreview() {
  document.getElementById("memberAvatar").addEventListener("change", (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      document.getElementById("avatarPreview").innerHTML = `<img src="${reader.result}" alt="Avatar">`;
    };
    reader.readAsDataURL(file);
  });
}

function removeLegacyAssignmentOptions() {
  document.querySelectorAll('.act-btn[data-type="legacy"]').forEach((button) => button.remove());
}
