/* Regression for the reported viewer flicker and monthly day-archive reads. Synthetic data. */
const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path'),fs=require('node:fs');
const preset=require('../app/src/main/assets/dashboard-preset.json');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox'],...(process.env.WATTPIT_CHROMIUM?{executablePath:process.env.WATTPIT_CHROMIUM}:{})});
 try{for(const theme of ['dark','light']){
  const page=await browser.newPage({viewport:{width:390,height:844},timezoneId:'Europe/Berlin'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  const until=async fn=>{for(let i=0;i<200;i++){if(await page.evaluate(fn))return;await page.waitForTimeout(50);}throw Error('History retention UI did not reach the expected state');};
  await page.clock.setFixedTime(new Date('2026-10-27T19:00:00Z'));
  await page.addInitScript(({preset,theme})=>{
   window.revision=1;window.auto=false;window.pendingReads=[];window.amount=640;window.dayAmount=29.68;
   const history=q=>{
    const [from,to,metric]=q.args,points=Array.from({length:241},(_,i)=>{
     const t=from+(to-from)*i/240,v=10+i/12;
     return {t,last:t,v,min:v,max:v,n:1,kind:'shunt',device:'shunt',origin:'archive',period:Math.max(1,Math.ceil((to-from)/240000))};
    });
    if(q.method==='dailyEnergy'){window.lastDaily=q;return {accountingVersion:2,days:[{day:'2026-10-27',last:window.dayAmount,coverage:.99},{day:'2026-10-26',last:0,coverage:1}]};}
    if(q.method.startsWith('energy')){const series=last=>({points:points.map((p,i)=>({...p,v:i*last/240,min:i*last/240,max:i*last/240})),summary:{last,n:241,coverage:1,measuredCoverage:1,from,to}});return {accountingVersion:2,solar:series(410),consumption:series(window.amount),balance:series(410-window.amount)};}
    const values=points.flatMap(p=>{
     if(metric==='voltage')return [{...p,v:13.1,min:13.1,max:13.1}];
     if(q.method==='history'&&metric==='power')return [p,{...p,kind:'solar',device:'solar',v:p.v*1.5,min:p.v*1.5,max:p.v*1.5}];
     return [p];
    });
    return {points:values,summary:{last:30,min:10,max:30,n:values.length}};
   };
   window.replyOne=error=>{const q=window.pendingReads.shift();if(q)window.onHistoryResult({id:q.id,data:error?{error:'Hauptgerät nicht erreichbar'}:history(q)});};
   window.drain=error=>{let limit=0;while(window.pendingReads.length){if(++limit>100)throw Error('Unbounded read loop');window.replyOne(error);}};
   window.WattPit={appVersion:()=> '2.0.1',isLanViewer:()=>true,lanState:()=>JSON.stringify({id:'plant',role:'viewer',name:'Testanlage',connected:true}),snapshot:()=>JSON.stringify({running:true,historyRevision:window.revision,lan:{id:'plant',connected:true},devices:[{mac:'shunt',kind:'shunt',name:'Test-Shunt'},{mac:'solar',kind:'solar',name:'Test-Solar'}],readings:[{device:'shunt',kind:'shunt',t:Date.now(),soc:62,voltage:13.15,current:-2,power:-26},{device:'solar',kind:'solar',t:Date.now(),solar:0,state:0}],found:[],stats:{count:100},settings:{theme,palette:'classic',capacity:100,sampleSeconds:10,miniRange:'last6h'},solarDaily:[{day:'2026-10-27','Yield(Wh)':410},{day:'2026-10-26','Yield(Wh)':0}]}),syncState:()=>JSON.stringify({busy:false,runId:0,cockpitNoticeAccepted:true,theme,palette:'classic'}),dashboardPreset:()=>JSON.stringify(preset),dashboard:()=>JSON.stringify(preset),saveDashboard:()=>'',settings:()=>'',solarSetup:()=> '{}',rollerSnapshot:()=> '{}',history:()=>{throw Error('Blocking viewer read');},dashboardHistory:()=>{throw Error('Blocking viewer read');},energyHistoryWindow:()=>{throw Error('Blocking viewer read');},dailyEnergy:()=>{throw Error('Blocking viewer read');},requestHistory:json=>{const q=JSON.parse(json);if(window.auto)setTimeout(()=>window.onHistoryResult({id:q.id,data:history(q)}),10);else window.pendingReads.push(q);}};
  },{preset,theme});
  await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));
  const consumption=page.locator('.cockpit-tile').filter({has:page.locator('.tile-label',{hasText:'Geräteverbrauch'})});
  await until(()=>window.pendingReads.length>0);
  // Each delayed result is overtaken by another live revision. A cold viewer
  // must still install all complete first results instead of retrying tile one.
  for(let i=0;i<12;i++){
   await page.evaluate(()=>{window.revision++;poll();window.replyOne(false);});
   await page.waitForTimeout(100);
  }
  await until(()=>miniPoints.length===482&&[...document.querySelectorAll('.tile-label')].some(e=>e.textContent==='Geräteverbrauch'&&e.closest('.cockpit-tile').querySelector('.tile-number').textContent==='640,00'));
  const yieldTile=page.locator('[data-tile="default-solarEnergy"]');
  assert.equal(await yieldTile.locator('.tile-number').textContent(),'410,00');
  await page.evaluate(()=>{window.auto=true;window.drain(false);});await until(()=>!reads.inflight&&!reads.queue.size);
  for(const tile of [consumption,yieldTile])assert.ok((await tile.locator('.tile-sparkline path').last().getAttribute('d')).split('L').length>100,'a complete energy curve must be drawn');
  const colors=await page.locator('#miniChart').evaluate(canvas=>{
   const pixels=canvas.getContext('2d').getImageData(0,0,canvas.width,canvas.height).data;let solar=0,battery=0;
   for(let i=0;i<pixels.length;i+=4){const [r,g,b]=pixels.slice(i,i+3);if(r>150&&g>70&&g<200&&b<140)solar++;if(g>120&&g>r*1.2&&b>r*1.1)battery++;}
   return {solar,battery};
  });
  assert.ok(colors.solar>30&&colors.battery>30,'solar and battery curves must be painted '+JSON.stringify(colors));
  if(process.env.WATTPIT_SCREENSHOT_DIR){fs.mkdirSync(process.env.WATTPIT_SCREENSHOT_DIR,{recursive:true});await page.screenshot({path:path.join(process.env.WATTPIT_SCREENSHOT_DIR,'viewer-first-statistics-'+theme+'.png'),fullPage:true});}
  await page.evaluate(()=>{const tile=dashboard.config.tiles.find(t=>t.metric==='consumptionEnergy');if(!dashboard.detail)dashboard.detail=new WattTileDetail(dashboard);dashboard.detail.open(tile.id);});
  await until(()=>document.querySelector('#detailReading .detail-number')?.textContent==='640,00');
  await page.evaluate(()=>{window.auto=false;window.revision++;poll();dashboard.detail.refresh(false);});
  assert.equal(await page.locator('#detailReading .detail-number').textContent(),'640,00');assert.match(await page.locator('#detailNote').textContent(),/Letzter Stand/);await page.locator('#detailClose').click();
  await page.evaluate(()=>{window.auto=false;window.revision++;poll();});
  assert.equal(await consumption.locator('.tile-number').textContent(),'640,00');assert.equal(await page.evaluate(()=>miniPoints.length),482);assert.match(await consumption.locator('.tile-hint').textContent(),/Letzter Stand/);
  await page.evaluate(()=>window.drain(true));await page.waitForTimeout(120);assert.equal(await consumption.locator('.tile-number').textContent(),'640,00');assert.match(await consumption.locator('.tile-hint').textContent(),/Aktualisierung fehlgeschlagen/);
  await page.clock.setFixedTime(new Date('2026-10-27T19:10:00Z'));await page.evaluate(()=>poll());
  assert.equal(await consumption.locator('.tile-number').textContent(),'640,00');assert.equal(await page.evaluate(()=>miniPoints.length),482);
  await page.evaluate(()=>{window.amount=660;window.drain(false);});await until(()=>[...document.querySelectorAll('.tile-label')].some(e=>e.textContent==='Geräteverbrauch'&&e.closest('.cockpit-tile').querySelector('.tile-number').textContent==='660,00'));
  await page.locator('[data-page="history"]').click();await page.evaluate(()=>{poll();window.drain(false);});await page.waitForTimeout(120);
  const row=day=>page.locator('#solarDailyArchive tr').filter({has:page.locator('td').filter({hasText:day})});
  assert.match(await row('2026-10-27').locator('td').nth(2).textContent(),/29,68/);assert.match(await row('2026-10-26').locator('td').nth(2).textContent(),/0,00/);
  assert.equal(await page.evaluate(()=>window.lastDaily.args[1]>Date.now()+86400000),true,'the real UI requests the complete calendar month, which the LAN host must accept');
  await page.evaluate(()=>{window.revision++;poll();});
  assert.equal(await page.evaluate(()=>points.length),482);assert.match(await page.locator('#historyReadNote').textContent(),/Letzter Stand/);assert.match(await row('2026-10-27').locator('td').nth(2).textContent(),/29,68.*letzter Stand/);
  await page.evaluate(()=>window.drain(true));await page.waitForTimeout(120);assert.equal(await page.evaluate(()=>points.length),482);assert.match(await row('2026-10-27').locator('td').nth(2).textContent(),/29,68.*Aktualisierung fehlgeschlagen/);
  await page.locator('[data-metric="voltage"]').click();assert.deepEqual(await page.evaluate(()=>points),[],'a different metric must not inherit the old power curve');await page.evaluate(()=>window.drain(false));await page.waitForTimeout(120);assert.equal(await page.evaluate(()=>points[0].v),13.1);
  assert.deepEqual(errors,[]);await page.close();
 }
 console.log('PASS cold viewer first statistics and fair chart loading under continuous live revisions; retained curves during 10-minute delays and errors; atomic replacement; daily month-end query, zero values and explicit stale status; metric isolation; both themes.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
