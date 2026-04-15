(() => {
  const page = document.body.dataset.authPage;
  const pageMessages = {
    setup: {
      targetId: "setupMessage",
      errors: {
        missing: "Fyll inn familienavn og første medlem.",
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
