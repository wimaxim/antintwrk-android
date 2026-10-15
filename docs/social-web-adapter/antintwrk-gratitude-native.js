// Copyright 2026 Super David Universe LLC.
// SPDX-License-Identifier: AGPL-3.0-only
// Store purchases replenish the sender. A gift always needs separate explicit confirmation.
export function createNativeGratitude({window, dialog, config, setBusy}) {
  const root = dialog.querySelector('[data-gratitude-native]');
  if (!root || !/^[1-9][0-9]{0,18}$/.test(config.actor || '') ||
      !/^\/(?:en|ru|uk|de|es|fr|it|ar|zh)\/hero-selection\?view=kindness&app=1$/.test(config.kindnessUrl || '')) return null;
  const labels = config.labels;
  const get = selector => root.querySelector(selector);
  const choices = get('[data-gratitude-gift-amounts]');
  const products = get('[data-gratitude-products]');
  const storeStatus = get('[data-gratitude-store-status]');
  const storeRetry = get('[data-gratitude-store-retry]');
  const status = dialog.querySelector('[data-gratitude-status]');
  const pay = dialog.querySelector('[data-gratitude-pay]');
  const review = get('[data-gratitude-review]');
  const number = value => typeof value === 'string' && /^(?:0|[1-9][0-9]{0,18})$/.test(value) && BigInt(value) <= 9223372036854775807n;
  const format = value => BigInt(value).toLocaleString(dialog.ownerDocument.documentElement.lang || 'en');
  const selected = () => choices.querySelector('input:checked').value;
  const key = () => Array.from(window.crypto.getRandomValues(new Uint8Array(16)), n => n.toString(16).padStart(2, '0')).join('');
  let revision = 0, active = false, recipient = null, balance = null, confirmed = false;
  let busy = false, catalogBusy = false, balanceBusy = false, balanceAgain = false, pending = null;
  const storageKey = () => `ant-gratitude-gift:${config.actor}:${recipient.id}`;
  const persist = () => {
    try { if (pending) window.sessionStorage.setItem(storageKey(), JSON.stringify(pending)); else window.sessionStorage.removeItem(storageKey()); } catch (_) {}
  };
  const current = generation => active && generation === revision;
  const update = () => {
    pay.textContent = confirmed ? labels.send : labels.review;
    pay.disabled = busy || balance === null || BigInt(selected()) > balance;
    choices.disabled = busy || pending !== null;
    review.hidden = !confirmed;
    get('[data-gratitude-confirm-amount]').textContent = format(selected());
    root.querySelectorAll('button').forEach(button => { button.disabled = busy; });
  };
  const request = async data => {
    const controller = new AbortController(), timer = setTimeout(() => controller.abort(), 15000);
    try {
      const response = await window.fetch(config.kindnessUrl, {method:'POST', credentials:'same-origin', signal:controller.signal,
        headers:{'Content-Type':'application/json','X-Hero-CSRF':config.token}, body:JSON.stringify(data)});
      const result = await response.json();
      if (!response.ok || !result.ok) throw new Error(result.error || 'FAILED');
      return result;
    } finally { clearTimeout(timer); }
  };
  const display = result => {
    if (!number(result.available)) throw new Error('FAILED');
    balance = BigInt(result.available);
    get('[data-gratitude-balance]').textContent = format(result.available);
  };
  const load = async () => {
    if (!active) return;
    if (balanceBusy) { balanceAgain = true; return; }
    balanceBusy = true; const generation = revision;
    try {
      if (pending) {
        const result = await request({...pending, action:'gift_status'});
        if (!current(generation)) return;
        if (result.giftFound) { pending = null; persist(); confirmed = false; status.textContent = labels.sent; }
      }
      const result = await request({action:'balance'});
      if (!current(generation)) return;
      display(result);
      if (BigInt(selected()) > balance) status.textContent = labels.insufficient;
      else if ([labels.loading,labels.insufficient,labels.unavailable,labels.giftFailed].includes(status.textContent)) status.textContent = '';
    } catch (error) { if (current(generation)) { balance = null; status.textContent = error.message === 'AUTH_REQUIRED' ? labels.session : labels.giftFailed; } }
    finally { if (current(generation)) { balanceBusy = false; update(); if (balanceAgain) { balanceAgain = false; load(); } } }
  };
  const storeError = error => error?.message === 'STORE_CANCELLED' ? '' : error?.message === 'STORE_ALREADY_OWNED' ? labels.owned : labels.failed;
  const catalog = async () => {
    if (!active || busy || catalogBusy) return;
    products.replaceChildren(); storeRetry.hidden = true; storeStatus.textContent = labels.loading;
    if (!window.antintwrkStoreRequest) { storeStatus.textContent = labels.failed; storeRetry.hidden = false; return; }
    catalogBusy = true; const generation = revision;
    try {
      const result = await window.antintwrkStoreRequest({type:'store-catalog'});
      if (!current(generation)) return;
      const keys = new Set();
      for (const product of result.products || []) {
        if (product.purpose !== 'kindness' || !/^[a-z0-9][a-z0-9._-]{0,99}$/.test(product.key || '') || keys.has(product.key) ||
            !/^[1-9][0-9]{0,6}$/.test(String(product.points)) || Number(product.points) > 1000000 || typeof product.price !== 'string' || !product.price.trim()) continue;
        keys.add(product.key);
        const button = dialog.ownerDocument.createElement('button');
        button.type = 'button'; button.className = 'hw-button hw-button-secondary';
        button.textContent = `${format(String(product.points))} · ${product.price}`;
        button.addEventListener('click', () => purchase({type:'store-purchase', key:product.key}));
        products.appendChild(button);
      }
      storeStatus.textContent = products.childElementCount ? '' : labels.failed;
      storeRetry.hidden = products.childElementCount > 0;
    } catch (error) { if (current(generation)) { storeStatus.textContent = storeError(error); storeRetry.hidden = false; } }
    finally { if (current(generation)) catalogBusy = false; }
  };
  const purchase = async payload => {
    if (!active || busy || !window.antintwrkStoreRequest) return;
    const generation = revision;
    busy = true; setBusy(true); update(); storeStatus.textContent = labels.loading;
    try {
      await window.antintwrkStoreRequest(payload);
      if (!current(generation)) return;
      storeStatus.textContent = payload.type === 'store-purchase' ? labels.pending : '';
      await load(); // Never send a gift from a purchase callback or store-updated event.
    } catch (error) { if (current(generation)) storeStatus.textContent = storeError(error); }
    finally { if (current(generation)) { busy = false; setBusy(false); update(); } }
  };
  choices.addEventListener('change', () => {
    if (pending) return;
    confirmed = false;
    status.textContent = balance !== null && BigInt(selected()) > balance ? labels.insufficient : '';
    update();
  });
  get('[data-gratitude-store-retry]').addEventListener('click', () => { load(); catalog(); });
  window.addEventListener('antintwrk-native-ready', catalog);
  window.addEventListener('antintwrk-store-updated', load);
  window.addEventListener('antintwrk-native-active', load);
  return {
    open(result) {
      const target = result.recipient;
      if (result.isOwnPublication === true || !target || !/^[1-9][0-9]{0,18}$/.test(target.id || '') || target.id === config.actor || typeof target.name !== 'string' || !target.name.trim()) return false;
      revision++; active = true; recipient = target; balance = null; confirmed = false; pending = null;
      balanceBusy = false; balanceAgain = false; catalogBusy = false; busy = false; root.hidden = false; status.textContent = labels.loading;
      choices.querySelector('input').checked = true;
      get('[data-gratitude-balance]').textContent = '—';
      get('[data-gratitude-recipient]').textContent = target.name;
      get('[data-gratitude-confirm-recipient]').textContent = target.name;
      try {
        const saved = JSON.parse(window.sessionStorage.getItem(storageKey()));
        if (saved?.action === 'gift' && saved.recipientId === target.id && /^[a-f0-9]{32}$/.test(saved.requestKey) &&
            ['1000','10000','100000','1000000'].includes(saved.amount)) {
          pending = saved; confirmed = true;
          choices.querySelectorAll('input').forEach(input => { input.checked = input.value === saved.amount; });
        }
      } catch (_) {}
      update(); load(); catalog(); return true;
    },
    close() { revision++; active = false; busy = false; setBusy(false); root.hidden = true; },
    async submit() {
      if (!active || busy || pay.disabled) return;
      if (!confirmed) { confirmed = true; status.textContent = ''; update(); return; }
      const generation = revision;
      if (!pending) pending = {action:'gift', recipientId:recipient.id, amount:selected(), requestKey:key()};
      persist(); busy = true; setBusy(true); update(); status.textContent = labels.loading;
      try {
        const result = await request(pending);
        if (!current(generation)) return;
        display(result); pending = null; persist(); confirmed = false; status.textContent = labels.sent;
      } catch (error) {
        if (current(generation)) {
          if (['INSUFFICIENT_KINDNESS','AMOUNT_INVALID','BALANCE_LIMIT'].includes(error.message)) { pending = null; persist(); confirmed = false; }
          status.textContent = error.message === 'AUTH_REQUIRED' ? labels.session : error.message === 'INSUFFICIENT_KINDNESS' ? labels.insufficient : labels.giftFailed;
        }
      } finally { if (current(generation)) { busy = false; setBusy(false); update(); } }
    },
  };
}
