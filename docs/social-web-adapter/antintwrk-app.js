// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
// Only the authenticated content-only shell loads this module.
import {installAppEditor} from './antintwrk-app-editor.js?v=20261008-5';
import {installAppViewport} from './antintwrk-app-viewport.js';
if (window.top === window && document.body.classList.contains('antintwrk-app-view')) {
  installAppViewport(window, document.documentElement.style);
  const send = (payload) => {
    const message = JSON.stringify(payload);
    if (window.antintwrkNative?.postMessage) window.antintwrkNative.postMessage(message);
    else window.webkit?.messageHandlers?.antintwrkNative?.postMessage(message);
  };
  installAppEditor(window, document, send);
  const navigation = () => send({type: 'navigation', url: window.location.href});
  const waiting = new Map();
  const receive = (event) => {
    try {
      const result = JSON.parse(typeof event === 'string' ? event : event.data);
      const request = waiting.get(result.id);
      if (!request) return;
      waiting.delete(result.id);
      clearTimeout(request.timer);
      if (result.ok) request.resolve(result);
      else request.reject(new Error(/^STORE_[A-Z_]+$/.test(result.error || '') ? result.error : 'STORE_UNAVAILABLE'));
    } catch (_) {}
  };
  const native = window.antintwrkNative || window.webkit?.messageHandlers?.antintwrkNative;
  if (native) {
    if (window.antintwrkNative) window.antintwrkNative.onmessage = receive;
    window.antintwrkNativeResponse = receive;
    let lastGesture = 0;
    document.addEventListener('click', (event) => {
      if (event.isTrusted) lastGesture = Date.now();
    }, true);
    const request = (payload) => new Promise((resolve, reject) => {
      const asynchronousGesture = (payload.type === 'download' && Date.now() - lastGesture < 30000) ||
        (payload.type === 'store-purchase' && Date.now() - lastGesture < 120000) ||
        (payload.type === 'open-external' && Date.now() - lastGesture < 30000);
      if (payload.type !== 'store-catalog' && navigator.userActivation && !navigator.userActivation.isActive && !asynchronousGesture) {
        reject(new DOMException('User action required', 'NotAllowedError'));
        return;
      }
      const id = crypto.randomUUID();
      const timer = setTimeout(() => {
        waiting.delete(id);
        reject(new DOMException('Request expired', 'AbortError'));
      }, payload.type === 'store-catalog' ? 60000 : 120000);
      waiting.set(id, {resolve, reject, timer});
      send({...payload, id});
    });
    Object.defineProperty(navigator, 'share', {value: ({url, title}) => request({type: 'share', url, title}).then(() => undefined), configurable: true});
    window.antintwrkOpenExternal = url => request({type:'open-external',url});
    window.antintwrkStoreRequest = (payload) => {
      if (!['store-catalog', 'store-purchase', 'store-restore'].includes(payload?.type)) return Promise.reject(new DOMException('Unavailable', 'NotAllowedError'));
      return request(payload);
    };
    window.dispatchEvent(new Event('antintwrk-native-ready'));
    Object.defineProperty(navigator, 'clipboard', {value: {writeText: (text) => request({type: 'copy', text: String(text)}).then(() => undefined)}, configurable: true});
    document.addEventListener('click', (event) => {
      const link = event.target.closest?.('a[download]');
      if (!link?.href.startsWith(`blob:${window.location.origin}/`)) return;
      event.preventDefault();
      fetch(link.href).then(async (response) => {
        const content = await response.text();
        await request({type: 'download', filename: link.download, content});
      }).catch(() => {});
    }, true);
  }
  const preserveApp = (value) => {
    if (value == null) return value;
    const url = new URL(value, window.location.href);
    if (url.origin !== window.location.origin || /^\/(?:[a-z]{2}\/)?(?:go|goto)(?:\/|$)/.test(url.pathname)) return value;
    url.searchParams.set('app', '1');
    return `${url.pathname}${url.search}${url.hash}`;
  };
  for (const name of ['pushState', 'replaceState']) {
    const original = window.history[name];
    window.history[name] = function (state, unused, url) {
      const result = original.call(this, state, unused, preserveApp(url));
      navigation();
      return result;
    };
  }
  document.addEventListener('submit', async event => {
    const form=event.target;
    if (!form?.matches?.('[data-app-support]')) return;
    event.preventDefault();
    const button=form.querySelector('button[type="submit"]');
    if (!button || button.disabled) return;
    const status=form.querySelector('[data-app-support-status]') || document.querySelector('[data-kindness-load-status]');
    button.disabled=true;
    try {
      const endpoint=new URL(form.action,window.location.href);
      if (endpoint.origin!==window.location.origin || !/^\/(en|ru|uk|de|es|fr|it|ar|zh)\/(ukraine|foundation|david)-start$/.test(endpoint.pathname)) throw new Error('UNAVAILABLE');
      const body=new FormData(form);body.set('native','1');
      const response=await fetch(endpoint.href,{method:'POST',credentials:'same-origin',body});
      const result=await response.json();
      if(!response.ok||!result.ok||!window.antintwrkOpenExternal)throw new Error('UNAVAILABLE');
      let target=result.url;
      if(result.ref){
        if(result.publicOrigin!=='https://antintwrk.com'||!/^\/(en|ru|uk|de|es|fr|it|ar|zh)\/ukraine$/.test(result.path))throw new Error('UNAVAILABLE');
        const publicPage=new URL(result.path,result.publicOrigin);publicPage.searchParams.set('app_ref',result.ref);target=publicPage.href;
      }
      await window.antintwrkOpenExternal(target);
      if(status)status.textContent='';
    }catch(_){if(status)status.textContent=status.dataset.error||'';}
    finally{button.disabled=false;}
  });
  window.addEventListener('popstate', navigation);
  window.addEventListener('hashchange', navigation);
  document.addEventListener('antintwrk:auth-open', (event) => {
    if (window.antintwrkNative?.postMessage || window.webkit?.messageHandlers?.antintwrkNative) {
      event.stopImmediatePropagation();
      send({type: 'auth-required'});
    }
  }, true);
  let timer = 0;
  const originalFetch = window.fetch;
  window.fetch = function (...args) {
    // App WebAuthn uses the verified APK origin; the ordinary web endpoint stays HTTPS-only.
    const requested = new URL(args[0] instanceof Request ? args[0].url : String(args[0]), window.location.href);
    if (window.antintwrkNative && requested.origin === window.location.origin && requested.pathname === '/passkey.php' && args[1]?.body instanceof URLSearchParams) {
      const fields = new URLSearchParams(args[1].body);
      if (['create-options', 'create'].includes(fields.get('action'))) {
        fields.set('action', `passkey-${fields.get('action')}`);
        args = ['/antintwrk_mobile.php', {...args[1], body: fields, headers: {...args[1].headers, 'X-Antintwrk-Client': 'native'}}];
      }
    }
    return originalFetch.apply(this, args).then((response) => {
      const request = args[0];
      const url = new URL(request instanceof Request ? request.url : String(request), window.location.href);
      if (url.origin === window.location.origin) {
        if (response.status === 401) send({type: 'auth-required'});
        else if (response.ok && !timer) timer = window.setTimeout(() => {
          timer = 0;
          send({type: 'changed'});
        }, 3000);
      }
      return response;
    });
  };
  navigation();
}
