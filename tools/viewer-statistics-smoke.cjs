const autarpieControls=require('./autarpie-test-controls.cjs');
/* Actual native encrypted-TCP calculation results; invented measurements only. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const wait=require('./browser-wait.cjs'),C=require('../app/src/main/assets/dashboard-core.js');
const fixture=JSON.parse(fs.readFileSync(process.env.WATTPIT_VIEWER_FIXTURE,'utf8')),board=C.defaults();
// The mock returns one first result for this shared request family. Independent
// tile() calls otherwise give it millisecond-dependent reset timestamps/scopes.
const period={...C.tile('solarEnergy').period,mode:'custom',from:fixture.from,to:fixture.to,aggregate:'last',resetAt:fixture.from};
board.tiles=['soc','solarEnergy','consumptionEnergy','energyBalance','mobileSolarEnergy','lan'].map((metric,i)=>{
 const t=C.tile(metric,'statistics-'+metric);if(!['soc','lan'].includes(metric)){t.type='chart';t.period={...period};}
 for(const profile of ['compact','wide'])t.layouts[profile]={x:i%2*6,y:Math.floor(i/2)*7,w:6,h:7};return t;
});
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox'],...(process.env.WATTPIT_CHROMIUM?{executablePath:process.env.WATTPIT_CHROMIUM}:{})});
 try{
  const page=await browser.newPage({viewport:{width:430,height:1080},deviceScaleFactor:1.5,timezoneId:'Europe/Berlin'}),errors=[];page.on('pageerror',error=>errors.push(error.message));await page.clock.setFixedTime(new Date(fixture.to+60000));
  await page.addInitScript(({fixture,board})=>{
   window.statTest={calls:[],held:[],first:false,revision:1,learning:[],theme:'dark'};
   const now=Date.now(),plant=fixture.first.analysis.plant,state=()=>({role:'viewer',id:plant,name:'Beispielanlage',connected:true,analysisPending:!!statTest.held.length});
   const snapshot=()=>({running:true,historyRevision:statTest.revision,lan:state(),devices:[{kind:'solar',mac:'solar'},{kind:'shunt',mac:'shunt'}],readings:[{kind:'solar',device:'solar',t:now,solar:140,state:3},{kind:'shunt',device:'shunt',t:now,soc:66.6,voltage:13.2,current:40/13.2,power:40}],found:[],stats:{count:12000},settings:{theme:statTest.theme,palette:'classic',capacity:100,sampleSeconds:10},weather:{}});
   window.WattPit={appVersion:()=>'2.0.3',isLanViewer:()=>true,lanState:()=>JSON.stringify(state()),snapshot:()=>JSON.stringify(snapshot()),syncState:()=>JSON.stringify({busy:false,cockpitNoticeAccepted:true,theme:statTest.theme,palette:'classic'}),dashboard:()=>JSON.stringify(board),saveDashboard:()=>'',settings:()=>'',solarSetup:()=>'{}',rollerSnapshot:()=>'{}',dailyEnergy:()=>'{"days":[]}',energyHistoryWindow:()=>{throw Error('Blocking statistics read');},requestHistory:json=>{
    const q=JSON.parse(json);statTest.calls.push(q);
    if(q.method.startsWith('energy')){
     if(!statTest.first)setTimeout(()=>{statTest.first=true;window.onHistoryResult({id:q.id,data:fixture.first});},250);
     else statTest.held.push(()=>window.onHistoryResult({id:q.id,data:fixture.fresh}));
    }else setTimeout(()=>window.onHistoryResult({id:q.id,data:q.method==='dailyEnergy'?{days:[]}:{points:[],summary:{}}}),20);
   },requestSolarData:()=>{statTest.learning.push({cold:reads.initialPending()});setTimeout(()=>window.onSolarPlanData({source:'test',observations:[],forecasts:[],updated:now}),20);}};
  },{fixture,board});
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));await autarpieControls.prepare(page);
  await page.evaluate(()=>solar.render());
  assert.equal(await page.evaluate(()=>statTest.learning.some(read=>read.cold)),false,'learning must not occupy the native worker ahead of the first visible statistics');
  const number=metric=>page.locator(`[data-tile="statistics-${metric}"] .tile-number`);
  const metrics={solarEnergy:'solar',consumptionEnergy:'consumption',energyBalance:'balance',mobileSolarEnergy:'mobile'};
  await wait(page,metrics=>statTest.first&&statTest.held.length>0&&metrics.every(metric=>document.querySelector(`[data-tile="statistics-${metric}"] .tile-number`)?.textContent!=='—'),Object.keys(metrics));
  for(let i=0;i<6;i++)await page.evaluate(()=>{statTest.revision++;poll();});
  for(const [metric,kind] of Object.entries(metrics)){
   assert.notEqual(await number(metric).textContent(),'—',metric+' must retain the first real native result');
   const expected=C.convert(fixture.first[kind].summary.last,board.tiles.find(t=>t.metric===metric).unit,metric);
   assert.ok(Math.abs(Number((await number(metric).textContent()).replaceAll('.','').replace(',','.'))-expected)<.011,metric+' uses the native summary');
  }
  assert.match(await page.locator('[data-tile="statistics-solarEnergy"] .tile-hint').textContent(),/Letzter Stand/);
  await page.evaluate(()=>{for(const reply of statTest.held.splice(0))reply();});
  await page.clock.setFixedTime(new Date((await page.evaluate(()=>Date.now()))+31000));
  await wait(page,()=>{for(const reply of statTest.held.splice(0))reply();poll();return !reads.initialPending()&&!reads.inflight&&!reads.queue.size;});
  await page.evaluate(()=>solar.render());await wait(page,()=>statTest.learning.length>0);
  assert.equal(await page.evaluate(()=>statTest.learning.some(read=>read.cold)),false);
  for(const metric of Object.keys(metrics)){
   assert.doesNotMatch(await page.locator(`[data-tile="statistics-${metric}"] .tile-hint`).textContent(),/geladen|Letzter Stand|fehlgeschlagen/);
   assert.ok(await page.locator(`[data-tile="statistics-${metric}"] .tile-sparkline path`).count(),metric+' has a drawn native series');
  }
  const count=await page.evaluate(()=>statTest.calls.filter(q=>q.method.startsWith('energy')).length);
  for(let i=0;i<5;i++)await page.evaluate(()=>{dashboard.cache.clear();dashboard.update();});
  assert.equal(await page.evaluate(()=>statTest.calls.filter(q=>q.method.startsWith('energy')).length),count,'settled statistics must not enter a repaint/read loop');
  assert.deepEqual(await page.evaluate(()=>dashboard.config.tiles.map(t=>t.layouts)),board.tiles.map(t=>t.layouts),'loading never changes the saved arrangement');
  for(const theme of ['dark','light']){
   await page.evaluate(theme=>{statTest.theme=theme;poll();document.querySelector('.bottom-nav').hidden=true;},theme);
   await page.locator('#dashboardBoard').screenshot({path:path.resolve(__dirname,`../docs/images/viewer-statistics-2.0.3-${theme}.png`)});
  }
  assert.deepEqual(errors,[]);console.log('PASS native TCP statistics reach real charts during input changes, retain complete first values, settle, preserve layouts and precede learning.');
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exit(1);});
