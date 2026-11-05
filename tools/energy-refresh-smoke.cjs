const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs');
const root=path.resolve(__dirname,'..'),C=require(root+'/app/src/main/assets/dashboard-core.js');
const board=C.defaults();
board.tiles=['solarEnergy','mobileSolarEnergy','consumptionEnergy','energyBalance','lan'].map((metric,i)=>{
 const t=C.tile(metric,'check-'+metric);if(metric!=='lan')t.type='chart';
 t.period.mode=i<2?'sunrise':'today';t.period.aggregate='last';t.period.amount=6;t.period.unit='h';t.period.resetAt=0;
 for(const p of ['compact','wide'])t.layouts[p]={x:metric==='lan'?8:i%2*4,y:metric==='lan'?0:Math.floor(i/2)*6,w:4,h:metric==='lan'?12:6};
 return t;
});
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.WATTPIT_CHROMIUM,args:['--no-sandbox','--disable-gpu']});
 try{
  const page=await browser.newPage({viewport:{width:390,height:1000},timezoneId:'Europe/Berlin'}),errors=[];page.on('pageerror',e=>errors.push(e.message));page.setDefaultTimeout(10000);
  await page.clock.setFixedTime(new Date('2026-11-04T18:14:00Z'));
  await page.addInitScript(board=>{
   const now=Date.now(),day=Date.parse('2026-11-03T23:00:00Z'),rise=day+8.5*3600000,set=day+18*3600000;
   window.fixture={day,rise,set,calls:[],hold:false,held:[],pending:false,theme:'dark',revision:7,delta:0};
   const state=()=>({role:'viewer',id:'test-plant',name:'Testzentrale',connected:true,analysisPending:fixture.pending,analysisAt:now,receivedAt:now});
   const snapshot=()=>({running:true,historyRevision:fixture.revision,lan:state(),devices:[{kind:'solar',mac:'solar'},{kind:'shunt',mac:'shunt'}],readings:[{device:'solar',kind:'solar',t:now,solar:0,state:0},{device:'shunt',kind:'shunt',t:now,soc:65.9,voltage:13.24,power:-25,current:-25/13.24}],found:[],stats:{count:100},settings:{theme:fixture.theme,palette:'classic',capacity:100,sampleSeconds:10},weather:{timezone:'Europe/Berlin',days:[{day:'2026-11-04',sunrise:rise,sunset:set}]}});
   const response=q=>{
    if(q.method==='dailyEnergy')return {days:[]};
    if(!q.method.startsWith('energy'))return {points:[],summary:{}};
    const [from,to]=q.args,light=from===rise,totals={solar:39.4,mobile:(light?42.84:48.12)+fixture.delta,balance:light?-20:-25.22};totals.consumption=totals.solar+totals.mobile-totals.balance;
    const data={accountingVersion:2,analysis:{revision:fixture.revision,at:now,tables:{model_weather:'test'}}};
    for(const [kind,last] of Object.entries(totals)){
     const estimated=kind==='mobile'||kind==='consumption';
     data[kind]={analysis:data.analysis,points:[{t:from,last:from,v:0,min:0,max:0,n:1,gap:true,kind},{t:to,last:to,v:last,min:last,max:last,n:1,gap:false,kind}],summary:{from,to,last,min:Math.min(0,last),max:Math.max(0,last),coverage:1,measuredCoverage:1,modelledMillis:estimated?3600000:0}};
    }
    return data;
   };
   window.WattPit={appVersion:()=>'1.8.4',isLanViewer:()=>true,lanState:()=>JSON.stringify(state()),snapshot:()=>JSON.stringify(snapshot()),syncState:()=>JSON.stringify({busy:false,cockpitNoticeAccepted:true,theme:fixture.theme,palette:'classic'}),dashboard:()=>JSON.stringify(board),saveDashboard:()=>'',settings:()=>'',solarSetup:()=>'{}',rollerSnapshot:()=>'{}',history:()=>' {"points":[]}',dashboardHistory:()=>' {"points":[],"summary":{}}',energyHistoryWindow:()=>{throw Error('Unexpected blocking energy query');},dailyEnergy:()=>' {"days":[]}',requestHistory:json=>{
    const q=JSON.parse(json);fixture.calls.push(q);
    const reply=error=>window.onHistoryResult({id:q.id,data:error?{error:'Synthetic refresh failure'}:response(q)});
    if(fixture.hold)fixture.held.push(reply);else setTimeout(reply,20);
   }};
  },board);
  await page.route('http://wattpit.test/**',route=>route.fulfill({path:path.resolve(root,'app/src/main/assets',new URL(route.request().url()).pathname.slice(1))}));
  await page.goto('http://wattpit.test/index.html');
  const number=metric=>page.locator(`[data-tile="check-${metric}"] .tile-number`),hint=metric=>page.locator(`[data-tile="check-${metric}"] .tile-hint`);
  // Poll from Node: older Playwright waitForFunction implementations use eval,
  // which newer Chromium correctly blocks under the application's strict CSP.
  const waitForText=async(locator,expected)=>{
   const deadline=Date.now()+10000;let value;
   do{value=await locator.textContent();if(typeof expected==='string'?value===expected:expected.test(value))return;await new Promise(resolve=>setTimeout(resolve,25));}while(Date.now()<deadline);
   assert.fail(`Expected ${expected}, received ${value}`);
  };
  await waitForText(number('consumptionEnergy'),'112,74');
  assert.equal(await number('solarEnergy').textContent(),'39,40');assert.equal(await number('mobileSolarEnergy').textContent(),'42,84');assert.equal(await number('energyBalance').textContent(),'-25,22');
  const initialCalls=await page.evaluate(()=>fixture.calls.length);
  for(let i=0;i<10;i++){
   await page.evaluate(()=>{dashboard.clearCache();dashboard.update(snapshot);});
   assert.equal(await number('mobileSolarEnergy').textContent(),'42,84');
  }
  assert.equal(await page.evaluate(()=>fixture.calls.length),initialCalls,'unchanged windows must not repeatedly request or borrow each other');
  assert.deepEqual(await page.evaluate(()=>dashboard.config.tiles.filter(t=>t.metric!=='lan').map(t=>t.period.mode)),['sunrise','sunrise','today','today']);
  await page.evaluate(()=>{fixture.hold=true;fixture.revision++;poll();});
  assert.equal(await number('mobileSolarEnergy').textContent(),'42,84');assert.match(await hint('mobileSolarEnergy').textContent(),/Geschätzt · Letzter Stand · neue Daten werden geladen/);
  // Drain every queued refresh as an error; the original estimates remain visible.
  for(let i=0;i<8;i++)await page.evaluate(()=>{for(const reply of fixture.held.splice(0))reply(true);});
  await waitForText(hint('mobileSolarEnergy'),/Aktualisierung fehlgeschlagen/);
  assert.equal(await number('mobileSolarEnergy').textContent(),'42,84');assert.match(await hint('mobileSolarEnergy').textContent(),/Geschätzt · Letzter Stand/);
  await page.evaluate(()=>{fixture.hold=false;fixture.delta=2;fixture.revision++;poll();});
  await waitForText(number('mobileSolarEnergy'),'44,84');
  await waitForText(number('consumptionEnergy'),'114,74');
  assert.doesNotMatch(await hint('mobileSolarEnergy').textContent(),/Letzter Stand|geladen|fehlgeschlagen/,'a complete revised estimate must replace the old value');
  await page.evaluate(()=>{fixture.hold=true;reads.setActive(false);window.wattDisplayActive=false;clearTimeout(readRepaint);});
  const cases=[];
  for(const width of [320,390,844])for(const theme of ['dark','light'])for(const scale of [50,100,150]){
   await page.setViewportSize({width,height:1000});
   await page.evaluate(({theme,scale})=>{fixture.theme=theme;document.documentElement.dataset.theme=theme;WattDisplay.apply(scale);},{theme,scale});
   await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));
   const geometry=await page.evaluate(()=>{
    const el=dashboard.elements.get('check-solarEnergy'),hint=el.querySelector('.tile-hint'),original=hint.textContent;
    const measure=()=>{const rect=s=>{const r=el.querySelector(s).getBoundingClientRect();return {top:r.top,height:r.height};};return {hint:rect('.tile-hint'),curve:rect('.tile-sparkline'),axis:rect('.tile-chart-range'),cssHeight:parseFloat(getComputedStyle(hint).height),lineHeight:parseFloat(getComputedStyle(hint).lineHeight)};};
    hint.textContent='Heute';const short=measure();
    hint.textContent='Letzter Stand · neue Daten werden geladen · gemessen und kurz interpoliert · weitere Erläuterung zur vollständigen Datengrundlage';const long=measure();hint.textContent=original;
    const status=()=>({button:document.getElementById('lanOpen').getBoundingClientRect().top,visual:dashboard.elements.get('check-lan').querySelector('.tile-visual').getBoundingClientRect().bottom});
    fixture.pending=true;lanUi.render();const pending=status();fixture.pending=false;lanUi.render();const complete=status();
    return {short,long,pending,complete};
   });
   for(const area of ['hint','curve','axis']){
    assert.ok(Math.abs(geometry.short[area].height-geometry.long[area].height)<.1,`${width}/${theme}/${scale}: ${area} height changes with text`);
    assert.ok(Math.abs(geometry.short[area].top-geometry.long[area].top)<.1,`${width}/${theme}/${scale}: ${area} position changes with text`);
   }
   assert.ok(Math.abs(geometry.short.cssHeight-2*geometry.short.lineHeight)<.1,`${width}/${theme}/${scale}: reserve exactly two hint lines`);
   assert.ok(Math.abs(geometry.pending.button-geometry.complete.button)<.1,`${width}/${theme}/${scale}: the plant link moves`);
   assert.ok(geometry.complete.button<geometry.complete.visual,`${width}/${theme}/${scale}: the plant link is clipped`);
   cases.push({width,theme,scale});
  }
  if(process.env.WATTPIT_SCREENSHOT_DIR){
   fs.mkdirSync(process.env.WATTPIT_SCREENSHOT_DIR,{recursive:true});
   for(const theme of ['dark','light']){
    await page.setViewportSize({width:390,height:1000});await page.evaluate(theme=>{fixture.theme=theme;document.documentElement.dataset.theme=theme;WattDisplay.apply(100);},theme);
    await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));
    await page.screenshot({path:path.join(process.env.WATTPIT_SCREENSHOT_DIR,`energy-refresh-${theme}.png`),fullPage:true});
   }
  }
  assert.deepEqual(errors,[]);console.log(`Energy refresh: independent windows, pending/error estimates, real revision changes and ${cases.length} stable layouts passed.`);
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
