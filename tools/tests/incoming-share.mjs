// SPDX-License-Identifier: AGPL-3.0-only
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../../appnav/src/main/assets/antintwrk-incoming-share.js',import.meta.url),'utf8');
function fixture({draft='',type='post',app=true,iframe=false,fetchFails=false}={}) {
  let photos=0,videos=0,inputs=0,fetches=0;
  const surface={textContent:draft,dispatchEvent(){inputs++},replaceChildren(...paragraphs){this.textContent=paragraphs.map(p=>p.textContent).join('\n')},scrollIntoView(){}};
  const imageInput={files:[],dispatchEvent(){photos++}},videoInput={files:[],dispatchEvent(){videos++}};
  const root={dataset:{},querySelector(s){return {'[data-rich-surface]':surface,'[data-publication-type]:checked':{value:type},'[data-photo-input]':imageInput,'[data-publication-video-input]':videoInput}[s]},querySelectorAll(){return []}};
  const document={body:{classList:{contains(){return app}}},querySelector(){return root},createElement(){return {style:{}}}};
  const window={};window.top=iframe?{}:window;
  class File {constructor(_data,name,{type}){this.name=name;this.type=type}}
  class DataTransfer {constructor(){this.files=[];this.items={add:file=>this.files.push(file)}}}
  vm.runInNewContext(source,{window,document,File,DataTransfer,URL,location:{origin:'https://antintwrk.com'},Event:class{},InputEvent:class{},fetch:async()=>{fetches++;return {ok:!fetchFails,blob:async()=>new Uint8Array([1])}}});
  return {run:window.antintwrkImportNativeShare,surface,counts:()=>[photos,videos,inputs,fetches]};
}
const payload={id:'abc',text:'<img src=x onerror=alert(1)>\nhttps://example.com',files:[{url:'https://antintwrk.com/_native-share/abc/0',mime:'image/jpeg',name:'0.jpg'},{url:'https://antintwrk.com/_native-share/abc/1',mime:'video/mp4',name:'1.mp4'}]};
const fresh=fixture();assert.equal(await fresh.run(payload),'ok');assert.equal(fresh.surface.textContent,payload.text);assert.deepEqual(fresh.counts(),[1,1,1,2]);
assert.equal(await fresh.run(payload),'ok');assert.deepEqual(fresh.counts(),[1,1,1,2]);
for(const options of [{draft:'Existing draft'},{type:'poll'}]){const f=fixture(options);assert.equal(await f.run(payload),'draft');assert.deepEqual(f.counts(),[0,0,0,0]);}
for(const options of [{app:false},{iframe:true}]){const f=fixture(options);assert.equal(await f.run(payload),'unavailable');assert.deepEqual(f.counts(),[0,0,0,0]);}
const denied=fixture({fetchFails:true});assert.equal(await denied.run(payload),'unavailable');assert.equal(denied.surface.textContent,'');
const foreign=fixture();assert.equal(await foreign.run({...payload,files:[{...payload.files[0],url:'https://evil.example/data'}]}),'unavailable');assert.deepEqual(foreign.counts(),[0,0,0,0]);
console.log('Native share importer: text, deduplication, streamed image/video, drafts, frames, failed/foreign fetches passed');
