// SPDX-License-Identifier: AGPL-3.0-only
const {test} = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const script = fs.readFileSync(path.join(__dirname, '../../main/assets/antintwrk-ime.js'), 'utf8');
function setup(rect, {editable = false, caret = null, hidden = false, nested = false} = {}) {
  let reveal = [], scroll = [], tasks = new Map(), next = 0;
  const handlers = {};
  const field = {
    isContentEditable: editable, parentElement: null, scrollTop: 0, scrollHeight: nested ? 1200 : 0, clientHeight: 400,
    matches: () => true, closest: () => field, contains: () => true,
    getBoundingClientRect: () => rect, scrollIntoView: options => reveal.push(options),
  };
  const range = {cloneRange: () => range, setStart(){}, collapse(){}, getBoundingClientRect: () => caret};
  const window = {
    innerHeight: 600, visualViewport: {height: 600, offsetTop: 0, addEventListener(){}}, addEventListener(){},
    getSelection: () => caret ? {rangeCount: 1, focusNode: {}, focusOffset: 0, getRangeAt: () => range} : null,
    scrollBy: options => scroll.push(options.top),
  };
  const document = {activeElement:field, visibilityState:hidden?'hidden':'visible', scrollingElement:{}, addEventListener:(event, callback) => handlers[event]=callback};
  const context = {window,document,requestAnimationFrame:fn=>{tasks.set(++next,fn);return next},cancelAnimationFrame:id=>tasks.delete(id),getComputedStyle:()=>({overflowY:nested?'auto':'visible'})};
  vm.runInNewContext(script,context);
  const flush = () => {while(tasks.size){const entries=[...tasks.entries()];tasks.clear();entries.forEach(([,fn])=>fn());}};
  return {window,field,document,handlers,reveal,scroll,flush};
}
test('keyboard appearance lifts obscured compact editor after layout settles',()=>{
  const s=setup({top:720,bottom:960,height:240});s.window.antintwrkIme.update(true);
  assert.equal(s.reveal.length,0);s.flush();assert.equal(s.reveal.length,1);assert.equal(s.reveal[0].block,'center');
});
test('visible editor keeps its scroll position',()=>{
  const s=setup({top:100,bottom:340,height:240});s.window.antintwrkIme.update(true);s.flush();assert.equal(s.reveal.length,0);
});
test('hidden keyboard and hidden document never move a page',()=>{
  const s=setup({top:720,bottom:960,height:240});s.window.antintwrkIme.update(false);s.handlers.focusin();s.flush();assert.equal(s.reveal.length,0);
  const hidden=setup({top:720,bottom:960,height:240},{hidden:true});hidden.window.antintwrkIme.update(true);hidden.flush();assert.equal(hidden.reveal.length,0);
});
test('keyboard closure cancels pending automatic scroll',()=>{
  const s=setup({top:720,bottom:960,height:240});s.window.antintwrkIme.update(true);s.window.antintwrkIme.update(false);s.flush();assert.equal(s.reveal.length,0);
});
test('long rich text follows focus caret without scrolling to editor start',()=>{
  const s=setup({top:-700,bottom:1300,height:2000},{editable:true,caret:{top:740,bottom:760,height:20}});
  s.window.antintwrkIme.update(true);s.flush();assert.deepEqual(s.scroll,[176]);assert.equal(s.reveal.length,0);
});
test('nested editor scrolling consumes caret movement before document scrolling',()=>{
  const s=setup({top:-700,bottom:1300,height:2000},{editable:true,nested:true,caret:{top:740,bottom:760,height:20}});
  s.window.antintwrkIme.update(true);s.flush();assert.equal(s.field.scrollTop,176);assert.equal(s.scroll.length,0);
});
test('ordinary large textarea aligns its top without reading its contents',()=>{
  const s=setup({top:800,bottom:1800,height:1000});s.window.antintwrkIme.update(true);s.flush();assert.equal(s.reveal[0].block,'start');
});
test('initialization is idempotent and noneditable focus is ignored',()=>{
  const s=setup({top:800,bottom:1800,height:1000});const original=s.window.antintwrkIme;
  vm.runInNewContext(script,{window:s.window});s.field.matches=()=>false;s.window.antintwrkIme.update(true);s.flush();assert.equal(s.reveal.length,0);assert.equal(s.window.antintwrkIme,original);
});
