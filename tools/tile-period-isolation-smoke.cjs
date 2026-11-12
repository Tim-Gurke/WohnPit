const autarpieControls=require('./autarpie-test-controls.cjs');
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path');
const C=require('../app/src/main/assets/dashboard-core.js');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.WATTPIT_CHROMIUM,args:['--no-sandbox','--disable-gpu']});
 try{
  const page=await browser.newPage({viewport:{width:390,height:1000},timezoneId:'Europe/Berlin'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.clock.setFixedTime(new Date('2026-11-02T11:44:00Z'));
  const board=C.defaults();board.tiles.push(C.tile('mobileSolarEnergy','mobile-test'),C.tile('energyBalance','second-balance'));
  board.tiles.at(-1).period={...board.tiles.at(-1).period,mode:'last',amount:2};
  // A saved 1.8.0 board still contains this former shared setting.
  board.energyPeriod={...C.tile('energyBalance').period};
  await page.addInitScript(board=>{
   window.board=JSON.parse(localStorage.getItem('period-test-board')||JSON.stringify(board));window.calls=[];
   const rise=Date.parse('2026-11-02T06:00:00Z'),sunset=Date.parse('2026-11-02T16:00:00Z');
   window.fixture={running:true,status:'Live',devices:[],readings:[],found:[],stats:{count:20},historyRevision:1,equipment:{version:1,groups:[]},settings:{theme:'dark',palette:'classic',capacity:100,sampleSeconds:10,miniRange:'today',weatherPlace:{name:'Testort',latitude:49,longitude:8}},weather:{timezone:'Europe/Berlin',days:[{day:'2026-11-02',sunrise:rise,sunset,hours:8}],updated:Date.now()}};
   window.WattPit={appVersion:()=> '1.8.1',snapshot:()=>JSON.stringify(fixture),dashboard:()=>JSON.stringify(window.board),saveDashboard:json=>{window.board=JSON.parse(json);localStorage.setItem('period-test-board',json);return '';},settings:()=>'',history:()=>'{"points":[]}',dashboardHistory:()=>'{"points":[],"summary":{}}',energyHistoryWindow:(from,to,source,solarFrom,solarTo)=>{
    calls.push({from,to,source,solarFrom,solarTo});
    const series=(kind,rate,start=from,end=to)=>{const value=rate*(end-start)/3600000;return {points:[{t:start,last:start,v:0,min:0,max:0,n:1,gap:true,kind,origin:'calculated'},{t:end,last:end,v:value,min:value,max:value,n:1,gap:false,kind,origin:'calculated'}],summary:{last:value,min:0,max:value,from:start,to:end,coverage:1,measuredCoverage:1}};};
    return JSON.stringify({accountingVersion:2,solar:series('solar',70,Math.max(from,solarFrom),Math.min(to,solarTo)),mobile:series('mobile',20),consumption:series('consumption',30),balance:series('balance',60)});
   }};
  },board);
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));await autarpieControls.prepare(page);
  const snapshot=()=>page.evaluate(()=>({config:structuredClone(dashboard.config),tiles:Object.fromEntries(dashboard.config.tiles.filter(t=>WattDashboardCore.CATALOG[t.metric].flow).map(t=>{const el=dashboard.elements.get(t.id);return [t.id,{period:structuredClone(t.period),number:el.querySelector('.tile-number').textContent,hint:el.querySelector('.tile-hint').textContent,chart:el.querySelector('.tile-sparkline')?.outerHTML}];}))}));
  const initial=await snapshot();assert.equal(initial.config.energyPeriod,undefined);
  assert.notEqual(initial.tiles['default-energyBalance'].number,initial.tiles['second-balance'].number,'duplicate metrics must query their own periods');
  for(const [id,mode] of [['mobile-test','sunrise'],['default-consumptionEnergy','sunrise'],['default-energyBalance','last'],['default-solarEnergy','today']]){
   const before=await snapshot();
   await page.evaluate(id=>dashboard.editor(id),id);await page.locator('#tilePeriod').selectOption(mode);
   if(mode==='last'){await page.locator('#tileAmount').fill('3');await page.locator('#tileTimeUnit').selectOption('h');}
   await page.locator('#saveTile').click();const after=await snapshot();
   assert.equal(after.tiles[id].period.mode,mode);
   if(id!=='default-solarEnergy')assert.notEqual(after.tiles[id].number,before.tiles[id].number,'the changed tile must actually requery its selected interval');
   for(const key of Object.keys(before.tiles))if(key!==id)assert.deepEqual(after.tiles[key],before.tiles[key],id+' changed '+key);
   assert.deepEqual(after.config.tiles.map(t=>t.layouts),before.config.tiles.map(t=>t.layouts));
   await page.evaluate(()=>{fixture.historyRevision++;dashboard.clearCache();poll();});assert.deepEqual(await snapshot(),after,'data refresh changed a local tile period');
   await page.reload();assert.deepEqual(await snapshot(),after,'restart changed a local tile period');
  }
  const dayQuery=await page.evaluate(()=>calls.find(c=>c.from===Date.parse('2026-11-01T23:00:00Z')&&c.solarFrom===Date.parse('2026-11-02T06:00:00Z')&&c.solarTo===Date.parse('2026-11-02T16:00:00Z')));
  assert.ok(dayQuery,'full-day energy queries must keep the physical solar window so observed night consumption is included');
  // A temporary detail period uses its real boundaries, then leaves all tiles alone.
  const saved=await snapshot();await page.evaluate(()=>dashboard.choose('mobile-test'));await page.locator('#tileActionDetail').click();
  await page.locator('#detailPeriod').selectOption('today');const today=await page.locator('.detail-number').textContent();
  await page.locator('#detailPeriod').selectOption('sunrise');assert.notEqual(await page.locator('.detail-number').textContent(),today);
  const detailFrom=await page.evaluate(()=>dashboard.detail.series[0].data.summary.from);assert.equal(detailFrom,Date.parse('2026-11-02T06:00:00Z'));
  const query=await page.evaluate(from=>calls.find(c=>c.from===from&&c.solarFrom===from),detailFrom);assert.ok(query,'the mobile sunrise interval must be requested independently');
  await page.locator('#detailClose').click();assert.deepEqual(await snapshot(),saved);
  // Portable backups must not reconnect the periods when restored.
  await page.evaluate(()=>{const backup=WattSettingsCore.create(fixture,dashboard.config,fixture.equipment);dashboard.config=WattSettingsCore.normalize(JSON.parse(JSON.stringify(backup))).dashboard;dashboard.commit();});
  assert.deepEqual(await snapshot(),saved);await page.reload();assert.deepEqual(await snapshot(),saved);
  assert.deepEqual(errors,[]);await page.close();console.log('PASS independent tile periods: mobile/consumption/balance/fixed solar, distinct native query windows, duplicate metrics, data refresh, restart, temporary detail and backup round trip.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
