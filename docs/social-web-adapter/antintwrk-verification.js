// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
const page=document.querySelector('[data-verification-page]');
const config=JSON.parse(page.querySelector('[data-verification-config]').textContent);
const message=page.querySelector('[data-verification-message]');
const form=page.querySelector('[data-verification-form]');
form?.elements.legal_name.addEventListener('input',()=>form.elements.legal_name.setCustomValidity(''));
form?.elements.document.addEventListener('change',()=>form.elements.document.setCustomValidity(''));
const nameKey=name=>name.normalize('NFC').trim().replace(/\s+/gu,' ').toLocaleLowerCase();
let busy=false;
let storeProducts=[];
let pendingApplicationId=null;
let catalogLoading=false;
async function storePrices(){
 if(!config.play||catalogLoading)return;
 if(!config.playReady){document.querySelectorAll('[data-store-plan]').forEach(node=>node.textContent='—');message.textContent=config.labels.unavailable;return;}
 if(!window.antintwrkStoreRequest)return;
 catalogLoading=true;
 try{
  const catalog=await window.antintwrkStoreRequest({type:'store-catalog'});
  storeProducts=catalog.products.filter(product=>product.purpose==='verification');
  document.querySelectorAll('[data-store-plan]').forEach(node=>{const item=storeProducts.find(product=>product.plan===node.dataset.storePlan);node.textContent=item?.price||'—';});
  document.querySelectorAll('[data-store-submit]').forEach(button=>button.disabled=storeProducts.length===0);
  form?.querySelectorAll('[name="plan"]').forEach(input=>input.disabled=!storeProducts.some(product=>product.plan===input.value));
  const selected=form?.querySelector('[name="plan"]:checked');if(selected?.disabled)form.querySelector('[name="plan"]:not(:disabled)')?.click();
  if(!storeProducts.length)message.textContent=config.labels.unavailable;
 }catch(_){message.textContent=config.labels.unavailable;}finally{catalogLoading=false;}
}
window.addEventListener('antintwrk-native-ready',storePrices);storePrices();
async function refreshStore(){const button=page.querySelector('[data-verification-action="refresh"]'),id=pendingApplicationId||Number(button?.dataset.applicationId);if(!id)return;try{const result=await request({action:'refresh',applicationId:id});if(!busy&&(result.changed||!button||result.status!==button.dataset.observedStatus))location.reload();}catch(_){}}
window.addEventListener('antintwrk-store-updated',()=>{setTimeout(refreshStore,300);});
document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible'&&config.play)refreshStore();});
function errorText(error){if(error==='STORE_CANCELLED')return '';if(error==='STORE_ALREADY_OWNED')return config.labels.owned;return error==='VERIFICATION_NAME_MISMATCH'?config.labels.mismatch:error==='VERIFICATION_DOCUMENT_INVALID'||error==='VERIFICATION_DOCUMENT_REQUIRED'?config.labels.document:error==='AUTH_REQUIRED'?config.labels.auth:config.labels.failed;}
async function request(body,formData=false,retry=true){
 const response=await fetch(config.url,{method:'POST',credentials:'same-origin',headers:{'X-Hero-CSRF':config.token,...(formData?{}:{'Content-Type':'application/json'})},body:formData?body:JSON.stringify(body)});
 const result=await response.json();
 if(!result.ok&&result.error==='TOKEN_INVALID'&&retry){const session=await request({action:'session'},false,false);if(session.userId!==config.userId)throw new Error('AUTH_REQUIRED');config.token=session.token;return request(body,formData,false);}
 if(!response.ok||!result.ok)throw new Error(result.error||'UNAVAILABLE');return result;
}
async function perform(button,body,isForm=false){
 if(busy)return;busy=true;button.disabled=true;message.textContent=config.labels.saving;
 try{const result=await request(body,isForm);if(result.storeKey){
  if(!config.play||!storeProducts.some(product=>product.key===result.storeKey)||!window.antintwrkStoreRequest)throw new Error('UNAVAILABLE');
  await window.antintwrkStoreRequest({type:'store-purchase',key:result.storeKey,applicationId:result.applicationId});pendingApplicationId=result.applicationId;message.textContent=config.labels.pending;
 }else if(result.url){const url=new URL(result.url);if(config.play&&url.hostname!=='play.google.com')throw new Error('UNAVAILABLE');if(url.protocol!=='https:'||!['checkout.stripe.com','billing.stripe.com','www.paypal.com','paypal.com','play.google.com'].includes(url.hostname))throw new Error('UNAVAILABLE');if(config.play){if(!window.antintwrkOpenExternal)throw new Error('UNAVAILABLE');await window.antintwrkOpenExternal(url.href);}else location.assign(url.href);}else location.reload();}
 catch(error){message.textContent=errorText(error.message);message.scrollIntoView({block:'nearest'});}
 finally{busy=false;button.disabled=false;}
}
async function prepareDocument(file){
 const url=URL.createObjectURL(file),image=new Image();
 try{
  image.src=url;await image.decode();
  if(!image.naturalWidth||!image.naturalHeight)throw new Error('VERIFICATION_DOCUMENT_INVALID');
  const scale=Math.min(1,2600/image.naturalWidth,2600/image.naturalHeight),canvas=document.createElement('canvas');
  canvas.width=Math.max(1,Math.round(image.naturalWidth*scale));canvas.height=Math.max(1,Math.round(image.naturalHeight*scale));
  const context=canvas.getContext('2d');context.fillStyle='#fff';context.fillRect(0,0,canvas.width,canvas.height);context.drawImage(image,0,0,canvas.width,canvas.height);
  // Preserve legibility while fitting the existing engine's multipart-upload allowance.
  for(const quality of [.95,.9,.85,.8]){const blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/jpeg',quality));if(blob&&blob.size<=1800*1024){canvas.width=canvas.height=1;return blob;}}
  throw new Error('VERIFICATION_DOCUMENT_INVALID');
 }finally{URL.revokeObjectURL(url);image.src='';}
}
form?.addEventListener('submit',async event=>{
 event.preventDefault();const name=form.elements.legal_name;
 if(busy)return;
 if(nameKey(name.value)!==nameKey(name.dataset.profileName)){message.textContent=config.labels.mismatch;name.setCustomValidity(config.labels.mismatch);name.reportValidity();return;}
 const file=form.elements.document.files[0];if(!file||file.size>10*1024*1024||!['image/jpeg','image/png'].includes(file.type)){message.textContent=config.labels.document;form.elements.document.setCustomValidity(config.labels.document);form.elements.document.reportValidity();return;}
 const button=form.querySelector('[type=submit]'),body=new FormData(form),controls=[...form.elements].map(field=>[field,field.disabled]);busy=true;controls.forEach(([field])=>field.disabled=true);form.setAttribute('aria-busy','true');message.textContent=config.labels.saving;
 try{body.set('document',await prepareDocument(file),'identity.jpg');busy=false;await perform(button,body,true);}
 catch(error){message.textContent=errorText(error.message);}
 finally{busy=false;controls.forEach(([field,disabled])=>field.disabled=disabled);form.removeAttribute('aria-busy');}
});
page.addEventListener('click',event=>{const button=event.target.closest('[data-verification-action]');if(button)perform(button,{action:button.dataset.verificationAction,applicationId:Number(button.dataset.applicationId)});});
// Returning from a provider needs no extra confirmation: a protected POST reads payment truth.
const statusButton=page.querySelector('[data-verification-action="refresh"]');
if(statusButton)request({action:'refresh',applicationId:Number(statusButton.dataset.applicationId)}).then(result=>{if((result.changed||result.status!==statusButton.dataset.observedStatus)&&!busy)location.reload();}).catch(()=>{});
