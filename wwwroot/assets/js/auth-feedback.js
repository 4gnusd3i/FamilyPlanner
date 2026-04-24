(() => {
  const page = document.body.dataset.authPage;
  const pageMessages = {
    setup: {
      targetId: "setupMessage",
      errors: {
        missing: t("setup.error_missing"),
        invalid_avatar_format: t("errors.invalid_avatar_format"),
      },
    },
  };

  const config = pageMessages[page];
  if (!config) return;

  const error = new URLSearchParams(window.location.search).get("error");
  const message = error ? config.errors[error] : null;
  const messageEl = document.getElementById(config.targetId);

  if (!message || !messageEl) return;

  messageEl.hidden = false;
  messageEl.textContent = message;
})();
