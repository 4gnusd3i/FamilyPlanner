const fallbackLanguagePack = {
  id: "no-NB",
  locale: "nb-NO",
  htmlLang: "nb",
  weekdayNames: ["Mandag", "Tirsdag", "Onsdag", "Torsdag", "Fredag", "Lørdag", "Søndag"],
  weekdayShort: ["Man", "Tir", "Ons", "Tor", "Fre", "Lør", "Søn"],
  mealTypes: [
    { key: "breakfast", label: "🌄", name: "Frokost", optionLabel: "🌄 Frokost" },
    { key: "lunch", label: "☀️", name: "Lunsj", optionLabel: "☀️ Lunsj" },
    { key: "dinner", label: "🍽️", name: "Middag", optionLabel: "🍽️ Middag" },
  ],
  strings: {},
};

function getLanguagePack() {
  return window.appLanguagePack || fallbackLanguagePack;
}

function getCurrentLocale() {
  return getLanguagePack().locale || fallbackLanguagePack.locale;
}

function getWeekdayNames() {
  const pack = getLanguagePack();
  return pack.weekdayNames || pack.weekday_names || fallbackLanguagePack.weekdayNames;
}

function getWeekdayShort() {
  const pack = getLanguagePack();
  return pack.weekdayShort || pack.weekday_short || fallbackLanguagePack.weekdayShort;
}

function getLocalizedMealTypes() {
  const pack = getLanguagePack();
  const mealTypes = pack.mealTypes || pack.meal_types || fallbackLanguagePack.mealTypes;
  return mealTypes.map((mealType) => ({
    key: mealType.key,
    label: mealType.label,
    name: mealType.name,
    optionLabel: mealType.optionLabel || mealType.option_label || `${mealType.label} ${mealType.name}`,
  }));
}

function t(key, params = null) {
  const strings = getLanguagePack().strings || {};
  let value = strings[key] || key;
  if (!params) return value;

  Object.entries(params).forEach(([name, replacement]) => {
    value = value.replaceAll(`{${name}}`, replacement ?? "");
  });
  return value;
}

function extractApiErrorMessage(text) {
  if (!text) return "";

  try {
    const payload = JSON.parse(text);
    if (payload && typeof payload.error === "string") {
      return payload.error;
    }
  } catch {
    // Ignore invalid JSON payloads and use the raw response text.
  }

  return text;
}
