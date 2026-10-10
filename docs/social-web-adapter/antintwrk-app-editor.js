// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
// Only a UI mode signal; no account authority crosses this bridge.
export function appEditorActive(document, pathname) {
  if (document.querySelector('[data-composer-disclosure][open], [data-story-create-dialog][open], [data-reel-create-dialog][open]')) return true;
  if (/\/hero-compose\/?$/.test(pathname)) return true;
  return [...document.querySelectorAll('[data-hero-composer]')].some(form =>
    !form.closest('details:not([open]), dialog:not([open]), [hidden]') && form.getClientRects().length > 0);
}
export function installAppEditor(window, document, send) {
  let previous;
  let previousDialog;
  let pending = false;
  const update = () => {
    pending = false;
    const active = appEditorActive(document, window.location.pathname);
    const dialog = !!document.querySelector('[data-story-create-dialog][open], [data-reel-create-dialog][open]');
    if (active !== previous || dialog !== previousDialog) { previous = active; previousDialog = dialog; send({type:'editor', active, dialog:!!document.querySelector('[data-story-create-dialog][open], [data-reel-create-dialog][open]')}); }
  };
  const schedule = () => { if (!pending) { pending = true; window.requestAnimationFrame(update); } };
  new window.MutationObserver(schedule).observe(document.body, {subtree:true, childList:true, attributes:true, attributeFilter:['open','hidden','class','style']});
  document.addEventListener('toggle', schedule, true);
  document.addEventListener('close', schedule, true);
  window.addEventListener('popstate', schedule);
  window.addEventListener('antintwrk-close-editor', () => {
    const dialog = document.querySelector('[data-story-create-dialog][open], [data-reel-create-dialog][open]');
    if (dialog) dialog.querySelector('[data-story-create-close], [data-reel-create-close]')?.click();
    else document.querySelector('[data-composer-disclosure][open] [data-composer-collapse]')?.click();
    schedule();
  });
  window.addEventListener('pagehide', () => send({type:'editor',active:false}));
  update();
}
