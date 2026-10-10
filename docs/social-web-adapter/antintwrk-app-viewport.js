// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
// Embedded browsers can resolve modal viewport units to zero before layout settles.
export function installAppViewport(window, style) {
  const update = () => {
    const visual = window.visualViewport?.height;
    const height = Number.isFinite(visual) && visual > 0 ? visual : window.innerHeight;
    if (Number.isFinite(height) && height > 0 && height <= 20000) {
      style.setProperty('--ant-app-viewport-height', `${height}px`);
    }
  };
  update();
  window.addEventListener('resize', update);
  window.addEventListener('pageshow', update);
  window.visualViewport?.addEventListener('resize', update);
}
