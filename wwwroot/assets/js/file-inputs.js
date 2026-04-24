const DEFAULT_FILE_INPUT_EMPTY_TEXT = t("file_input.no_file_selected");

function syncLocalizedFileInput(inputOrId) {
  const input = typeof inputOrId === "string"
    ? document.getElementById(inputOrId)
    : inputOrId;

  if (!input) return;

  const fileNameTargetId = input.dataset.fileName;
  const fileNameTarget = fileNameTargetId ? document.getElementById(fileNameTargetId) : null;
  if (!fileNameTarget) return;

  const hasFile = Boolean(input.files && input.files.length > 0);
  fileNameTarget.textContent = hasFile
    ? input.files[0].name
    : (input.dataset.emptyText || DEFAULT_FILE_INPUT_EMPTY_TEXT);
  fileNameTarget.classList.toggle("is-placeholder", !hasFile);
}

function bindLocalizedFileInputs(root = document) {
  root.querySelectorAll(".file-input-native[data-file-name]").forEach((input) => {
    if (input.dataset.localizedBound === "true") return;

    input.dataset.localizedBound = "true";
    input.addEventListener("change", () => syncLocalizedFileInput(input));
    syncLocalizedFileInput(input);
  });
}

document.addEventListener("DOMContentLoaded", () => bindLocalizedFileInputs());
