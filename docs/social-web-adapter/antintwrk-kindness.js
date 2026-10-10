// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
const root = document.querySelector('[data-kindness-root]');
if (root) {
  const config = JSON.parse(root.querySelector('[data-kindness-config]').textContent);
  const labels = config.labels;
  const form = root.querySelector('[data-kindness-form]');
  const amount = form?.querySelector('input');
  const review = root.querySelector('[data-kindness-review]');
  const dialog = root.querySelector('[data-kindness-dialog]');
  const send = root.querySelector('[data-kindness-send]');
  const cancel = root.querySelector('[data-kindness-cancel]');
  const status = root.querySelector('[data-kindness-load-status]');
  const retry = root.querySelector('[data-kindness-retry]');
  let available = null;
  let pending = null;
  let sending = false;
  let pendingWasSent = false;
  const storageKey = `ant-kindness-pending:${root.dataset.kindnessActor}:${config.recipientId}`;
  // Persist only the retry identity, never credentials or an authoritative balance.
  const persistPending = () => {
    try { if (pending) sessionStorage.setItem(storageKey, JSON.stringify(pending)); else sessionStorage.removeItem(storageKey); } catch (_) { /* In-memory retry remains available. */ }
  };
  try {
    const saved = JSON.parse(sessionStorage.getItem(storageKey));
    if (saved && saved.action === 'gift' && String(saved.recipientId) === String(config.recipientId) && /^[a-f0-9]{32}$/.test(saved.requestKey) && /^[1-9][0-9]{0,18}$/.test(saved.amount)) {
      pending = saved;
      pendingWasSent = true;
      if (amount) amount.value = saved.amount;
    }
  } catch (_) { /* Missing/disabled browser storage must not break the page. */ }
  const format = (value) => new Intl.NumberFormat(document.documentElement.lang || 'en').format(BigInt(value));
  const key = () => Array.from(crypto.getRandomValues(new Uint8Array(16)), v => v.toString(16).padStart(2, '0')).join('');
  const request = async (data) => {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 15000);
    try {
      const response = await fetch(config.url, {method:'POST', credentials:'same-origin', signal:controller.signal, headers:{'Content-Type':'application/json','X-Hero-CSRF':config.token}, body:JSON.stringify(data)});
      const result = await response.json();
      if (!response.ok || !result.ok) throw new Error(result.error || 'FAILED');
      return result;
    } finally { clearTimeout(timer); }
  };
  const display = (result) => {
    available = BigInt(result.available);
    root.querySelector('[data-kindness-available]').textContent = format(result.available);
    root.querySelector('[data-kindness-reserved]').textContent = format(result.reserved);
    root.querySelector('[data-kindness-reserved-wrap]').hidden = BigInt(result.reserved) === 0n;
    if (review) review.disabled = false;
    if (typeof result.historyHtml === 'string') {
      const history = root.querySelector('[data-kindness-history]');
      if (history) history.outerHTML = result.historyHtml;
    }
  };
  const load = async () => {
    retry.hidden = true; status.textContent = '';
    try {
      if (pending) {
        const recovered = await request({...pending,action:'gift_status'});
        if (recovered.giftFound) {
          pending = null; persistPending();
          if (amount) amount.value = '';
          root.querySelector('[data-kindness-gift-status]').textContent = labels.sent;
        }
      }
      display(await request({action:'balance',before:new URL(location.href).searchParams.get('before') || '0'}));
    }
    catch (error) { status.textContent = error.message === 'AUTH_REQUIRED' ? labels.session : labels.failed; retry.hidden = false; }
  };
  retry.addEventListener('click', load);
  form?.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!form.reportValidity() || available === null) return;
    const value = amount.value;
    const errorNode = root.querySelector('[data-kindness-gift-status]');
    if (!/^[1-9][0-9]{0,18}$/.test(value) || BigInt(value) > 9223372036854775807n) { errorNode.textContent = labels.invalid; return; }
    review.disabled = true;
    try { display(await request({action:'balance'})); }
    catch (_) { errorNode.textContent = labels.failed; review.disabled = false; return; }
    errorNode.textContent = '';
    if (pending && pendingWasSent) {
      try {
        const recovered = await request({...pending,action:'gift_status'});
        if (recovered.giftFound) { pending = null; persistPending(); amount.value = ''; errorNode.textContent = labels.sent; display(recovered); return; }
      } catch (_) { errorNode.textContent = labels.failed; return; }
      if (pending.amount !== value) { amount.value = pending.amount; errorNode.textContent = labels.giftFailed; return; }
    }
    if (BigInt(value) > available) { errorNode.textContent = labels.insufficient; return; }
    if (!pending || pending.amount !== value) { pending = {action:'gift', recipientId:config.recipientId, amount:value, requestKey:key()}; pendingWasSent = false; }
    root.querySelector('[data-kindness-confirm-amount]').textContent = format(value);
    root.querySelector('[data-kindness-confirm-status]').textContent = '';
    dialog.setAttribute('role','alertdialog');
    send.disabled = false;
    dialog.showModal(); document.documentElement.style.overflow = 'hidden'; cancel.focus();
  });
  const close = () => { if (!sending) { dialog.close(); review?.focus(); } };
  cancel.addEventListener('click', close);
  dialog.addEventListener('cancel', event => { if (sending) event.preventDefault(); });
  dialog.addEventListener('close', () => { document.documentElement.style.overflow = ''; review?.focus(); });
  send.addEventListener('click', async () => {
    if (sending || !pending) return;
    sending = true; send.disabled = true; cancel.disabled = true;
    pendingWasSent = true;
    persistPending();
    const feedback = root.querySelector('[data-kindness-confirm-status]');
    feedback.textContent = labels.sending;
    try {
      const result = await request(pending);
      display(result); pending = null; persistPending(); amount.value = '';
      dialog.close(); root.querySelector('[data-kindness-gift-status]').textContent = labels.sent; amount.focus();
    } catch (error) {
      feedback.textContent = error.message === 'INSUFFICIENT_KINDNESS' ? labels.insufficient : error.message === 'TOO_MANY_REQUESTS' ? labels.rate : error.message === 'AUTH_REQUIRED' ? labels.session : labels.giftFailed;
      // Keep the same operation key so retry cannot charge twice after a timeout.
      // These transactional failures explicitly confirm that nothing was transferred.
      if (['INSUFFICIENT_KINDNESS','AMOUNT_INVALID','BALANCE_LIMIT'].includes(error.message)) { pending = null; pendingWasSent = false; persistPending(); }
    } finally { sending = false; send.disabled = !pending; cancel.disabled = false; }
  });
  if (config.play) {
    const storeStatus = root.querySelector('[data-store-status]');
    const productsNode = root.querySelector('[data-store-products]');
    let storeBusy = false;
    const storeError = error => error?.message === 'STORE_CANCELLED' ? '' : error?.message === 'STORE_ALREADY_OWNED' ? labels.owned : labels.unavailable;
    const prices = async () => {
      if (!storeStatus || !productsNode || storeBusy) return;
      if (!config.playReady) { storeStatus.textContent = labels.unavailable; return; }
      if (!window.antintwrkStoreRequest) return;
      storeBusy = true; productsNode.replaceChildren();
      try {
        const catalog = await window.antintwrkStoreRequest({type:'store-catalog'});
        for (const product of catalog.products.filter(item => item.purpose === 'kindness')) {
          const button = document.createElement('button');
          button.type = 'button'; button.className = 'hw-button hw-button-primary';
          button.textContent = `${format(product.points)} · ${product.price}`;
          button.addEventListener('click', async () => {
            if (storeBusy) return; storeBusy = true;
            productsNode.querySelectorAll('button').forEach(item => item.disabled = true);
            try { await window.antintwrkStoreRequest({type:'store-purchase',key:product.key}); storeStatus.textContent = labels.pending; }
            catch (error) { storeStatus.textContent = storeError(error); }
            finally { storeBusy = false; productsNode.querySelectorAll('button').forEach(item => item.disabled = false); }
          });
          productsNode.appendChild(button);
        }
        storeStatus.textContent = productsNode.childElementCount ? '' : labels.unavailable;
      } catch (error) { storeStatus.textContent = storeError(error); }
      finally { storeBusy = false; }
    };
    root.querySelector('[data-store-retry]')?.addEventListener('click', prices);
    root.querySelector('[data-store-restore]')?.addEventListener('click', async () => {
      if(storeBusy)return;storeBusy=true;
      try { await window.antintwrkStoreRequest({type:'store-restore'}); await load();storeStatus.textContent=''; }
      catch(error){storeStatus.textContent=storeError(error);}finally{storeBusy=false;}
    });
    window.addEventListener('antintwrk-native-ready',prices);
    window.addEventListener('antintwrk-store-updated',()=>{load();if(storeStatus)storeStatus.textContent='';});
    prices();
    root.querySelectorAll('[data-app-support]').forEach(support => support.addEventListener('submit', async event => {
      event.preventDefault();const button=support.querySelector('button');if(button.disabled)return;button.disabled=true;
      try {
        const body=new FormData(support);body.set('native','1');
        const response=await fetch(support.action,{method:'POST',credentials:'same-origin',body});const result=await response.json();
        if(!response.ok||!result.ok||!window.antintwrkOpenExternal)throw new Error('UNAVAILABLE');
        await window.antintwrkOpenExternal(result.url);
      } catch(_){status.textContent=labels.failed;}finally{button.disabled=false;}
    }));
  }
  load();
}
