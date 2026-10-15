// Copyright 2026 Super David Universe LLC.
// SPDX-License-Identifier: AGPL-3.0-only
// Buy for the publication's author. Only the server's settled order means success.
export function createNativeGratitude({window, dialog, config, setBusy, onPaid}) {
  const root = dialog.querySelector('[data-gratitude-native]');
  if (!root || !/^[1-9][0-9]{0,18}$/.test(config.actor || '')) return null;
  const labels = config.labels, choices = dialog.querySelector('[data-gratitude-amounts]');
  const status = dialog.querySelector('[data-gratitude-status]'), pay = dialog.querySelector('[data-gratitude-pay]');
  const paymentLabel = pay.textContent, largest = choices.querySelector('input[value="1000"]');
  const get = selector => root.querySelector(selector);
  let active=false, revision=0, products=new Map(), capable=false, unsupported=false, catalogBusy=false, busy=false;
  let target=null, pending=null, pendingState='pending', paid=false, pollTimer=null, polls=0, checking=false, checkAgain=false;
  const selected=()=>String(Number(choices.querySelector('input:checked').value)*1000);
  const current=g=>active&&g===revision;
  const key=()=>Array.from(window.crypto.getRandomValues(new Uint8Array(16)),n=>n.toString(16).padStart(2,'0')).join('');
  const storageKey=()=>`ant-gratitude-purchase:${config.actor}:${target.targetKind}:${target.targetId}`;
  const persist=()=>{try {if(pending)window.localStorage.setItem(storageKey(),JSON.stringify(pending));else window.localStorage.removeItem(storageKey());}catch(_){}};
  const update=()=>{
    const product=products.get(selected());
    root.hidden=!target; pay.hidden=paid;
    pay.textContent=pending?(['awaiting_payment','not_started'].includes(pendingState)?paymentLabel:labels.checkPayment):(!catalogBusy&&!product?pay.dataset.retryLabel:paymentLabel);
    pay.disabled=busy||catalogBusy||!capable||(!pending&&!product);
    if(!busy&&!catalogBusy&&!unsupported&&window.antintwrkStoreRequest&&(!capable||!product))pay.disabled=false;
    if(unsupported)pay.disabled=true;
    choices.disabled=busy||pending!==null||paid;
    choices.classList.toggle('ant-gratitude-wide-prices',Array.from(products.values()).some(p=>p.price.length>9));
    choices.querySelectorAll('input').forEach(input=>{
      const option=input.closest('label'),offer=products.get(String(Number(input.value)*1000));
      option.querySelector('[data-gratitude-money]').textContent=offer?.price||'—';
      option.querySelector('[data-gratitude-currency]').hidden=true;
      input.disabled=!offer;
    });
    get('[data-gratitude-confirm-amount]').textContent=BigInt(selected()).toLocaleString(dialog.ownerDocument.documentElement.lang||'en');
    const price=get('[data-gratitude-confirm-price]');price.textContent=product?.price||price.dataset.unavailable;
  };
  const request=async data=>{
    const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),15000);
    try {
      const response=await window.fetch(config.url,{method:'POST',credentials:'same-origin',signal:controller.signal,
        headers:{'Content-Type':'application/json','X-Hero-CSRF':config.token},body:JSON.stringify(data)});
      const result=await response.json();if(!response.ok||!result.ok)throw new Error(result.error||'FAILED');return result;
    } finally {clearTimeout(timer);}
  };
  const schedule=()=>{
    clearTimeout(pollTimer);
    if(active&&pending&&!paid&&polls<20)pollTimer=setTimeout(()=>{polls++;check();},polls<5?3000:10000);
  };
  const check=async()=>{
    if(!active||!pending||paid)return null;
    if(checking){checkAgain=true;return null;}
    checking=true;const generation=revision,attempt=pending;
    try {
      const result=await request({action:'play-status',requestKey:attempt.requestKey,targetKind:target.targetKind,targetId:target.targetId});
      if(!current(generation)||pending!==attempt)return null;
      if(result.state!=='not_started'&&(result.recipientId!==target.recipient.id||result.points!==attempt.points||result.key!==attempt.key))throw new Error('MISMATCH');
      pendingState=result.state;
      if(result.state==='paid') {paid=true;pending=null;persist();status.textContent=labels.paid;clearTimeout(pollTimer);}
      else if(result.state==='refunded') {pending=null;persist();status.textContent=labels.refunded;}
      else status.textContent=labels.pending;
      update();if(paid)onPaid?.('play:'+attempt.requestKey);return result.state;
    } catch(error){if(current(generation))status.textContent=error.message==='AUTH_REQUIRED'?labels.session:labels.confirmationPending;return null;}
    finally {if(current(generation)){checking=false;update();if(checkAgain){checkAgain=false;check();}else schedule();}}
  };
  const catalog=async()=>{
    if(!active||catalogBusy||busy)return;
    catalogBusy=true;capable=false;unsupported=false;products=new Map();update();const generation=revision;
    try {
      if(!window.antintwrkStoreRequest)throw new Error('UNAVAILABLE');
      const result=await window.antintwrkStoreRequest({type:'store-catalog'});
      if(!current(generation))return;
      capable=result.gratitudePurchaseVersion===1;unsupported=!capable;
      if(!capable){status.textContent=labels.updateApp;return;}
      const keys=new Set();
      for(const p of result.products||[]) {
        if(p.purpose!=='kindness'||!['1000','10000','100000','500000'].includes(String(p.points))||
           !/^[a-z0-9][a-z0-9._-]{0,99}$/.test(p.key||'')||keys.has(p.key)||typeof p.price!=='string'||!p.price.trim())continue;
        keys.add(p.key);if(!products.has(String(p.points)))products.set(String(p.points),{key:p.key,price:p.price});
      }
      if(!pending&&!paid)status.textContent=products.size?'':labels.unavailable;
    }catch(_){if(current(generation))status.textContent=labels.failed;}
    finally{if(current(generation)){catalogBusy=false;update();}}
  };
  const launch=async()=>{
    const product=products.get(selected());if(!product||!capable)return;
    if(!pending){pending={requestKey:key(),key:product.key,points:selected()};pendingState='pending';persist();}
    if(pending.key!==product.key||pending.points!==selected())return;
    const generation=revision;busy=true;setBusy(true);update();status.textContent=labels.loading;
    try {
      await window.antintwrkStoreRequest({type:'store-purchase',key:pending.key,
        gratitude:{requestKey:pending.requestKey,targetKind:target.targetKind,targetId:target.targetId}});
      if(current(generation)){polls=0;await check();}
    } catch(error){
      if(current(generation)){
        const state=await check();
        if(!paid&&error.message==='STORE_CANCELLED'&&['not_started','awaiting_payment'].includes(state)){pending=null;persist();status.textContent='';}
        else if(!paid)status.textContent=error.message==='STORE_ALREADY_OWNED'?labels.owned:labels.confirmationPending;
      }
    } finally{if(current(generation)){busy=false;setBusy(false);update();schedule();}}
  };
  choices.addEventListener('change',()=>{if(!pending&&!paid){status.textContent='';update();}});
  window.addEventListener('antintwrk-native-ready',catalog);
  window.addEventListener('antintwrk-store-updated',()=>{polls=0;check();});
  window.addEventListener('antintwrk-native-active',()=>{polls=0;check();});
  window.addEventListener('antintwrk-store-failed',async event=>{
    if(!active||!pending||event.detail?.requestKey!==pending.requestKey)return;
    const generation=revision,attempt=pending,state=await check();
    if(!current(generation)||pending!==attempt||paid)return;
    if(event.detail.error==='STORE_CANCELLED'&&['not_started','awaiting_payment'].includes(state)){
      pending=null;persist();clearTimeout(pollTimer);status.textContent='';
    }else status.textContent=labels.confirmationPending;
    update();
  });
  return {
    open(result){
      if(result.isOwnPublication||!result.recipient||!/^[1-9][0-9]{0,18}$/.test(result.recipient.id||'')||result.recipient.id===config.actor||
         typeof result.recipient.name!=='string'||!result.recipient.name.trim()||!['publication','reel'].includes(result.targetKind)||!/^[1-9][0-9]{0,18}$/.test(result.targetId||''))return false;
      revision++;active=true;target=result;pending=null;pendingState='pending';paid=false;busy=false;checking=false;checkAgain=false;catalogBusy=false;polls=0;
      largest.value='500';const points=largest.closest('label').querySelector('[data-gratitude-option-points]');points.dataset.gratitudeOptionPoints='500000';points.textContent='500 000';
      choices.querySelector('input').checked=true;get('[data-gratitude-confirm-recipient]').textContent=result.recipient.name;
      try {
        const saved=JSON.parse(window.localStorage.getItem(storageKey()));
        if(saved&&/^[a-f0-9]{32}$/.test(saved.requestKey||'')&&/^[a-z0-9][a-z0-9._-]{0,99}$/.test(saved.key||'')&&['1000','10000','100000','500000'].includes(saved.points)){
          pending=saved;choices.querySelectorAll('input').forEach(i=>i.checked=String(Number(i.value)*1000)===saved.points);
        }
      }catch(_){}
      status.textContent=labels.loading;update();catalog();if(pending)check();return true;
    },
    close(){revision++;active=false;clearTimeout(pollTimer);busy=false;setBusy(false);root.hidden=true;pay.hidden=false;},
    async submit(){
      if(!active||busy||paid||pay.disabled)return;
      const generation=revision;
      if(pending){const state=await check();if(!current(generation)||paid||!['not_started','awaiting_payment'].includes(state))return;}
      if(!capable||!products.get(selected())){await catalog();return;}
      await launch();
    },
  };
}
