const colors = [
  "#2563eb", "#059669", "#d97706", "#dc2626", "#7c3aed",
  "#db2777", "#0891b2", "#65a30d", "#9333ea", "#ea580c",
  "#0f766e", "#be123c", "#4f46e5", "#15803d", "#b45309",
  "#c026d3", "#0284c7", "#ca8a04", "#16a34a", "#e11d48",
];
const memberEmojis = ["\ud83d\udc68", "\ud83d\udc69", "\ud83d\udc66", "\ud83d\udc67", "\ud83e\uddd2", "\ud83d\udc74", "\ud83d\udc75", "\ud83e\uddd1"];
const weekdayNames = getWeekdayNames();
const weekdayShort = getWeekdayShort();
const mealTypes = getLocalizedMealTypes();
let selectedColor = "#3b82f6";
let currentWeekStart = "";
let currentWeekEnd = "";
let familyMembers = [];
let eventsCache = [];
let mealsCache = [];
let notesCache = [];
let shoppingCache = [];
let currentProfileMemberId = null;
let statusTimer = null;
const SHOPPING_DELETE_DELAY_MS = 15000;
const KIOSK_BASE_WIDTH = 1180;
const KIOSK_BASE_HEIGHT = 820;
const KIOSK_BREAKPOINT = 1000;
const CUSTOM_SELECT_IDS = [
  "eventRecurrenceType",
  "eventOwner",
  "mealDay",
  "mealType",
  "mealOwner",
  "expenseOwner",
  "noteOwner",
  "shoppingOwner",
];
const shoppingDeleteTimers = new Map();
let currentViewEntry = null;

document.addEventListener("DOMContentLoaded", async () => {
  initWeek();
  renderWeekShell();
  renderMealDayOptions();
  initCustomSelects();
  initColorOptions();
  bindForms();
  bindAvatarPreview();
  await loadAll();
  if (typeof scheduleKioskLayout === "function") {
    window.addEventListener("resize", scheduleKioskLayout, { passive: true });
    scheduleKioskLayout();
  }
});

async function apiFetch(url, options = {}) {
  const response = await fetch(url, { credentials: "same-origin", ...options });
  if (response.status === 409) {
    location.href = "/setup";
    throw new Error(t("errors.setup_required"));
  }
  if (!response.ok) {
    const text = await response.text();
    throw new Error(extractApiErrorMessage(text) || t("errors.request_failed"));
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
    const dayLabel = `${weekdayNames[index]} ${date.getDate()}.${isToday ? ` ${t("calendar.today_suffix")}` : ""}`;

    return `<div class="day-box ${isToday ? "today" : ""}" data-day="${index}" data-date="${dateText}" aria-label="${dayLabel}">
      <div class="day-header">
        <span class="day-name">${weekdayNames[index]}</span>
        <span class="day-num">${date.getDate()}.</span>
      </div>
      <div class="day-body">
        <div class="day-content" id="events-${dateText}"></div>
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
  Promise.all([loadEvents(), loadUpcoming()]).catch(handleError);
}

function goToCurrentWeek() {
  initWeek();
  renderWeekShell();
  initDragDrop();
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
}

function bindAvatarPreview() {
  document.getElementById("memberAvatar").addEventListener("change", (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      document.getElementById("avatarPreview").innerHTML = `<img src="${reader.result}" alt="${escapeHtml(t("file_input.selected_profile_image_alt"))}">`;
    };
    reader.readAsDataURL(file);
  });
}
